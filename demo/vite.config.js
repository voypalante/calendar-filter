import { defineConfig } from 'vite';
import preact from '@preact/preset-vite';
import { fileURLToPath } from 'node:url';

// The component lives in ../web, outside this folder, so point its imports at this folder's Preact.
const preactDir = fileURLToPath(new URL('./node_modules/preact', import.meta.url));

export default defineConfig({
  plugins: [preact()],
  base: './',
  resolve: { alias: [{ find: /^preact(\/.*)?$/, replacement: `${preactDir}$1` }] },
});
