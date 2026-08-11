#!/usr/bin/env node

/**
 * Verifies that the packed package is consumable.
 *
 * This exists because `pnpm build` succeeding says nothing about whether the
 * published artifact resolves. Two failures got through a green build before
 * this check existed, and both were silent rather than loud:
 *
 * - `dist/plugin.cjs.js` was parsed as ESM (the package is "type": "module"),
 *   so `require()` returned an object with no exports at all.
 * - The declarations kept extensionless relative imports, so a consumer on
 *   moduleResolution node16/nodenext resolved `export * from './definitions'`
 *   to nothing and saw none of the public types.
 *
 * The check runs against the real `npm pack` tarball, not the working tree, so
 * it also covers the `files` allowlist and the exports map.
 */

import { execFileSync } from 'node:child_process';
import { cpSync, mkdirSync, mkdtempSync, readFileSync, realpathSync, rmSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const tsc = join(repoRoot, 'node_modules', '.bin', 'tsc');

const failures = [];
let workspace;

function run(command, args, options = {}) {
  return execFileSync(command, args, { encoding: 'utf8', stdio: 'pipe', ...options });
}

function check(name, fn) {
  try {
    fn();
    console.log(`  ok    ${name}`);
  } catch (error) {
    const detail = [error.message, error.stdout, error.stderr].filter(Boolean).join('\n').trim();
    console.log(`  FAIL  ${name}\n${detail.replace(/^/gm, '        ')}`);
    failures.push(name);
  }
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

/**
 * Copies a package and its runtime dependencies into the consumer, resolving
 * every path to its real location first so nothing points back into the pnpm
 * store.
 */
function vendor(targetModules, packageName, fromPackageJson = join(repoRoot, 'package.json'), seen = new Set()) {
  if (seen.has(packageName)) {
    return;
  }
  seen.add(packageName);

  const manifestPath = createRequire(fromPackageJson).resolve(`${packageName}/package.json`);
  const sourceDir = dirname(realpathSync(manifestPath));
  const targetDir = join(targetModules, ...packageName.split('/'));

  mkdirSync(dirname(targetDir), { recursive: true });
  cpSync(sourceDir, targetDir, { recursive: true, dereference: true });

  const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
  for (const dependency of Object.keys(manifest.dependencies ?? {})) {
    vendor(targetModules, dependency, manifestPath, seen);
  }
}

try {
  workspace = mkdtempSync(join(tmpdir(), 'capacitor-filesharer-verify-'));
  const consumer = join(workspace, 'consumer');
  mkdirSync(consumer);

  console.log('packing...');
  const packOutput = run('npm', ['pack', '--json', '--pack-destination', workspace], { cwd: repoRoot });
  const tarball = join(workspace, JSON.parse(packOutput)[0].filename);

  // Extract rather than `npm install` so the check needs no registry access and
  // stays deterministic. The tarball is still the real published artifact.
  const installed = join(consumer, 'node_modules', '@byteowls', 'capacitor-filesharer');
  mkdirSync(installed, { recursive: true });
  run('tar', ['-xzf', tarball, '-C', installed, '--strip-components=1']);

  // @capacitor/core is a peer dependency, so the consumer has to provide it.
  // Copy dereferenced and resolve transitive deps explicitly: under pnpm the
  // entries in node_modules are symlinks into a content-addressed store, and a
  // plain recursive copy would produce dangling links.
  vendor(join(consumer, 'node_modules'), '@capacitor/core');

  writeFileSync(
    join(consumer, 'package.json'),
    `${JSON.stringify({ name: 'consumer', version: '1.0.0', type: 'module', private: true }, null, 2)}\n`,
  );

  writeFileSync(
    join(consumer, 'app.ts'),
    [
      "import { FileSharer } from '@byteowls/capacitor-filesharer';",
      "import type { ShareFileOptions } from '@byteowls/capacitor-filesharer';",
      '',
      "const options: ShareFileOptions = { filename: 'test.txt', contentType: 'text/plain', base64Data: 'dGVzdA==' };",
      '',
      'export async function go(): Promise<void> {',
      '  await FileSharer.share(options);',
      '}',
      '',
    ].join('\n'),
  );

  const tsconfig = (moduleSetting, moduleResolution) => ({
    compilerOptions: {
      module: moduleSetting,
      moduleResolution,
      target: 'ES2022',
      strict: true,
      noEmit: true,
      // Deliberately not skipped: the point is to typecheck our own .d.ts.
      skipLibCheck: false,
      lib: ['ES2022', 'DOM'],
    },
    include: ['app.ts'],
  });

  writeFileSync(
    join(consumer, 'tsconfig.nodenext.json'),
    `${JSON.stringify(tsconfig('NodeNext', 'NodeNext'), null, 2)}\n`,
  );
  writeFileSync(
    join(consumer, 'tsconfig.bundler.json'),
    `${JSON.stringify(tsconfig('ES2022', 'Bundler'), null, 2)}\n`,
  );

  console.log(`verifying ${tarball.split('/').pop()}`);

  check('tarball ships the bundled artifacts', () => {
    const listing = run('tar', ['-tzf', tarball]).split('\n');
    for (const entry of ['package/dist/index.js', 'package/dist/index.cjs', 'package/dist/index.d.ts']) {
      assert(listing.includes(entry), `missing ${entry}`);
    }
  });

  check('tarball does not ship the intermediate esm tree', () => {
    const listing = run('tar', ['-tzf', tarball]).split('\n');
    const leaked = listing.filter((entry) => entry.startsWith('package/dist/esm/'));
    assert(leaked.length === 0, `unexpected build input published: ${leaked.join(', ')}`);
  });

  check('tarball ships Android and Swift Package Manager sources', () => {
    const listing = run('tar', ['-tzf', tarball]).split('\n');
    for (const entry of [
      'package/android/src/main/AndroidManifest.xml',
      'package/ios/Sources/ByteowlsCapacitorFilesharer/FileSharerPlugin.swift',
      'package/Package.swift',
    ]) {
      assert(listing.includes(entry), `missing ${entry}`);
    }
    assert(!listing.some((entry) => entry.endsWith('.podspec')), 'unexpected CocoaPods specification');
  });

  check('node ESM import resolves and exposes the plugin', () => {
    const output = run(
      'node',
      [
        '--input-type=module',
        '-e',
        "import { FileSharer } from '@byteowls/capacitor-filesharer'; if (typeof FileSharer !== 'object' || FileSharer === null) { throw new Error('FileSharer missing from ESM entry'); } console.log('esm-ok');",
      ],
      { cwd: consumer },
    );
    assert(output.includes('esm-ok'), output);
  });

  check('node CommonJS require resolves and exposes the plugin', () => {
    const output = run(
      'node',
      [
        '--input-type=commonjs',
        '-e',
        "const m = require('@byteowls/capacitor-filesharer'); if (!Object.keys(m).includes('FileSharer')) { throw new Error('CommonJS entry exports: ' + JSON.stringify(Object.keys(m))); } console.log('cjs-ok');",
      ],
      { cwd: consumer },
    );
    assert(output.includes('cjs-ok'), output);
  });

  check('typechecks under moduleResolution nodenext', () => {
    run(tsc, ['-p', 'tsconfig.nodenext.json'], { cwd: consumer });
  });

  check('typechecks under moduleResolution bundler', () => {
    run(tsc, ['-p', 'tsconfig.bundler.json'], { cwd: consumer });
  });
} finally {
  if (workspace !== undefined) {
    rmSync(workspace, { recursive: true, force: true });
  }
}

if (failures.length > 0) {
  console.error(`\n${failures.length} package check(s) failed: ${failures.join(', ')}`);
  process.exit(1);
}

console.log('\nall package checks passed');
