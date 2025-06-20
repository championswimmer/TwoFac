This is a Kotlin Multiplatform project.

This is what each module is for:

- `sharedLib`: shared common business logic code
- `composeApp`: Android, Desktop and Web (wasm) app using Compose
- `iosApp`: iOS app that uses the `ComposeApp` as a framework
- `cliApp`: command line interface app that uses the `sharedLib` for business logic