// Kotlin copies Compose resources beside the test executable, but Karma does
// not serve them by default. Keep their runtime URLs identical to the app's.
config.files.push({
    pattern: 'kotlin/composeResources/**/*',
    included: false,
    served: true,
    watched: false,
});
config.proxies['/composeResources/'] = '/base/kotlin/composeResources/';
