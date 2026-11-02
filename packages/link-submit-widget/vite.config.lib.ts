import { fileURLToPath } from 'url';
import { copyFileSync, mkdirSync } from 'node:fs';
import { defineConfig } from 'vite';

const RUNTIME_ASSET_NAME = 'link-apply-enhance.iife.js';

function syncRuntimeAssets() {
  return {
    name: 'sync-runtime-assets',
    closeBundle() {
      const runtimeDirectory = fileURLToPath(
        new URL('../../src/main/resources/static', import.meta.url),
      );
      mkdirSync(runtimeDirectory, { recursive: true });
      copyFileSync(`dist/${RUNTIME_ASSET_NAME}`, `${runtimeDirectory}/${RUNTIME_ASSET_NAME}`);
    },
  };
}

export default defineConfig({
  build: {
    lib: {
      entry: 'src/index.ts',
      name: 'LinkApplyEnhance',
      fileName: 'link-apply-enhance',
      formats: ['iife', 'es'],
    },
    emptyOutDir: true,
    rollupOptions: {
      output: {
        extend: true,
      },
    },
  },
  plugins: [syncRuntimeAssets()],
});
