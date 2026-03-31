Done — wrote findings to:

`/Users/championswimmer/Development/Personal/Kotlin/TwoFac/research.md`

Key takeaway: for a 2FA app, don’t rely on `allowBackup` alone. If you want limited D2D transfer but no cloud backup, use explicit `dataExtractionRules`/`fullBackupContent` allowlists.