// Make TypeScript-compiled interop modules resolvable by Webpack.
// __dirname points to build/wasm/packages/composeApp.js/ at runtime,
// so we navigate up to the project root and into composeApp/build.
const path = require("path");
const generatedDir = path.resolve(
  __dirname, "../../../../composeApp/build/generated/wasmJs/resources"
);
// Also include the Kotlin/Wasm node_modules so npm packages (e.g. jsqr)
// used by our interop modules are resolvable from the generated directory.
const kotlinNodeModules = path.resolve(__dirname, "../..", "node_modules");
config.resolve = config.resolve || {};
config.resolve.modules = (config.resolve.modules || []).concat([
  generatedDir, kotlinNodeModules,
]);
