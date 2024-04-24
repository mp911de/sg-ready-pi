import {defineConfig} from 'vite';
import vue from '@vitejs/plugin-vue';

/** @type {import('vite').UserConfig} */
export default defineConfig({
    root: 'src/main/frontend',
    build: {
        outDir: '../../../target/dist'
    },
    resolve: {
        alias: {
            vue: '@vue/compat',
        },
    },
    server: {
        proxy: {
            '/api': 'http://localhost:8080/',
            '/actuator': 'http://localhost:8080/',
        }
    },
    plugins: [vue()],
});
