#!/usr/bin/env node

/** Builds the plugin inside a Capacitor-style Android host project. */

import { execFileSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const workspace = mkdtempSync(join(tmpdir(), 'capacitor-filesharer-android-consumer-'));

function gradlePath(path) {
  return path.replaceAll('\\', '\\\\').replaceAll("'", "\\'");
}

function androidSdkPath() {
  const environmentPath = process.env.ANDROID_HOME ?? process.env.ANDROID_SDK_ROOT;
  if (environmentPath) return environmentPath;

  const properties = readFileSync(join(root, 'android', 'local.properties'), 'utf8');
  const match = properties.match(/^sdk\.dir=(.+)$/m);
  if (!match) throw new Error('Set ANDROID_HOME, ANDROID_SDK_ROOT, or android/local.properties sdk.dir');
  return match[1].replaceAll('\\:', ':').replaceAll('\\\\', '\\');
}

try {
  const packOutput = execFileSync('npm', ['pack', '--json', '--pack-destination', workspace], {
    cwd: root,
    encoding: 'utf8',
  });
  const tarball = join(workspace, JSON.parse(packOutput)[0].filename);
  const packedPlugin = join(workspace, 'package');
  mkdirSync(packedPlugin);
  execFileSync('tar', ['-xzf', tarball, '-C', packedPlugin, '--strip-components=1']);

  writeFileSync(
    join(workspace, 'settings.gradle'),
    `pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = 'capacitor-filesharer-consumer'
include ':capacitor-android'
project(':capacitor-android').projectDir = new File('${gradlePath(join(root, 'node_modules', '@capacitor', 'android', 'capacitor'))}')
include ':capacitor-filesharer'
project(':capacitor-filesharer').projectDir = new File('${gradlePath(join(packedPlugin, 'android'))}')
`,
  );
  writeFileSync(
    join(workspace, 'build.gradle'),
    `buildscript {
    repositories { google(); mavenCentral() }
    dependencies { classpath 'com.android.tools.build:gradle:8.13.0' }
}
`,
  );
  writeFileSync(join(workspace, 'gradle.properties'), 'android.useAndroidX=true\n');
  writeFileSync(join(workspace, 'local.properties'), `sdk.dir=${androidSdkPath().replaceAll('\\', '\\\\')}\n`);

  execFileSync(join(root, 'android', 'gradlew'), [':capacitor-filesharer:assembleRelease', '--no-daemon'], {
    cwd: workspace,
    stdio: 'inherit',
  });
  console.log('Android Capacitor host integration verified');
} finally {
  rmSync(workspace, { recursive: true, force: true });
}
