module.exports = {
    globDirectory: "build/dist/wasmJs/productionExecutable/",
    globPatterns: [
        "**/*.{html,js,wasm,css,png,json}"
    ],
    globIgnores: [
        "**/*.map"
    ],
    maximumFileSizeToCacheInBytes: 10 * 1024 * 1024,
    runtimeCaching: [{
        urlPattern: /.+/,
        handler: "StaleWhileRevalidate",
    }],
    swDest: "build/dist/wasmJs/productionExecutable/serviceWorker.js",
};
