import { fileURLToPath } from 'url';
import { cpSync, mkdirSync } from 'node:fs';
import { defineConfig } from 'vite';
import dts from 'vite-plugin-dts';

function syncRuntimeAssets() {
  return {
    name: 'sync-runtime-assets',
    closeBundle() {
      const runtimeDirectory = fileURLToPath(
        new URL('../../src/main/resources/static', import.meta.url),
      );
      mkdirSync(runtimeDirectory, { recursive: true });
      cpSync('dist/link-submit-widget.iife.js', `${runtimeDirectory}/link-submit-widget.iife.js`);
      cpSync('var.css', `${runtimeDirectory}/var.css`);
    },
  };
}

export default defineConfig({
  build: {
    lib: {
      entry: 'src/index.ts',
      name: 'LinkSubmitWidget',
      fileName: 'link-submit-widget',
      formats: ['iife', 'es'],
    },
    emptyOutDir: true,
    rollupOptions: {
      output: {
        extend: true,
      },
    },
  },
  plugins: [dts(), syncRuntimeAssets()],
});
