import { existsSync, readFileSync } from 'node:fs';

import nodeResolve from '@rollup/plugin-node-resolve';
import dts from 'rollup-plugin-dts';

export default [bundleRuntime(), bundleTypes()];

function bundleRuntime() {
  return {
    input: 'dist/esm/index.js',
    external: ['@capacitor/core'],
    plugins: [loadInputSourcemaps(), nodeResolve()],
    output: [
      {
        file: 'dist/index.js',
        format: 'es',
        sourcemap: true,
        inlineDynamicImports: true,
      },
      {
        file: 'dist/index.cjs',
        format: 'cjs',
        sourcemap: true,
        inlineDynamicImports: true,
        exports: 'named',
      },
      {
        file: 'dist/plugin.js',
        format: 'iife',
        name: 'capacitorFileSharer',
        globals: { '@capacitor/core': 'capacitorExports' },
        sourcemap: true,
        inlineDynamicImports: true,
      },
    ],
  };
}

function loadInputSourcemaps() {
  return {
    name: 'load-input-sourcemaps',
    load(id) {
      const mapPath = `${id}.map`;
      if (!id.endsWith('.js') || !existsSync(mapPath)) {
        return null;
      }

      return {
        code: readFileSync(id, 'utf8'),
        map: JSON.parse(readFileSync(mapPath, 'utf8')),
      };
    },
  };
}

function bundleTypes() {
  return {
    input: 'dist/esm/index.d.ts',
    external: ['@capacitor/core'],
    plugins: [dts()],
    output: {
      file: 'dist/index.d.ts',
      format: 'es',
    },
  };
}
