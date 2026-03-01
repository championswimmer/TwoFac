@file:OptIn(ExperimentalWasmJsInterop::class)
@file:JsModule("webextension-polyfill")

package tech.arnav.twofac.session.interop

@JsName("default")
external val webExtensionBrowser: JsAny?
