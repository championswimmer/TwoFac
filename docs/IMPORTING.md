# Importing 2FA Secrets from Other Apps

TwoFac supports importing 2FA secrets from other authenticator apps using an extensible adapter pattern.

## Supported Import Formats

### 1. **2FAS Authenticator**
- **Format**: JSON file with `.2fas` extension
- **Encryption**: Supports unencrypted exports (encrypted exports must be decrypted first)
- **Adapter**: `TwoFasImportAdapter`

#### 2FAS Export Example
```json
{
  "services": [
    {
      "name": "GitHub",
      "secret": "JBSWY3DPEHPK3PXP",
      "account": "user@example.com",
      "digits": 6,
      "period": 30,
      "algorithm": "SHA1"
    }
  ]
}
```

### 2. **Authy**
- **Format**: JSON array format
- **Encryption**: Not supported (use community export tools)
- **Adapter**: `AuthyImportAdapter`

#### Authy Export Example
```json
[
  {
    "secret": "JBSWY3DPEHPK3PXP",
    "digits": 6,
    "name": "GitHub",
    "issuer": "GitHub",
    "type": "totp",
    "period": 30
  }
]
```

### 3. **Ente Auth**
- **Format**: Plaintext (newline-separated otpauth:// URIs)
- **Encryption**: Encrypted exports not yet supported
- **Adapter**: `EnteImportAdapter`

#### Ente Auth Plaintext Export Example
```
otpauth://totp/GitHub:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub
otpauth://totp/Google:test@gmail.com?secret=GEZDGNBVGY3TQOJQ&issuer=Google
```

## Usage Example

```kotlin
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.importer.ImportResult
import tech.arnav.twofac.lib.importer.adapters.TwoFasImportAdapter
import tech.arnav.twofac.lib.importer.adapters.AuthyImportAdapter
import tech.arnav.twofac.lib.importer.adapters.EnteImportAdapter
import tech.arnav.twofac.lib.storage.MemoryStorage

suspend fun importFrom2FAS() {
    // Initialize and unlock TwoFacLib
    val lib = TwoFacLib.initialise(MemoryStorage(), "my-password")
    lib.unlock("my-password")

    // Read the export file content
    val fileContent = File("backup.2fas").readText()

    // Create the appropriate adapter
    val adapter = TwoFasImportAdapter()

    // Import accounts
    val result = lib.importAccounts(adapter, fileContent)

    when (result) {
        is ImportResult.Success -> {
            println("Successfully imported ${result.otpAuthUris.size} accounts")
        }
        is ImportResult.Failure -> {
            println("Import failed: ${result.message}")
        }
    }
}

suspend fun importFromAuthy() {
    val lib = TwoFacLib.initialise(MemoryStorage(), "my-password")
    lib.unlock("my-password")

    val fileContent = File("authy-export.json").readText()
    val adapter = AuthyImportAdapter()

    val result = lib.importAccounts(adapter, fileContent)
    // Handle result...
}

suspend fun importFromEnte() {
    val lib = TwoFacLib.initialise(MemoryStorage(), "my-password")
    lib.unlock("my-password")

    val fileContent = File("ente-plaintext.txt").readText()
    val adapter = EnteImportAdapter()

    val result = lib.importAccounts(adapter, fileContent)
    // Handle result...
}
```

## Creating Custom Import Adapters

You can create custom adapters for other authenticator apps by implementing the `ImportAdapter` interface:

```kotlin
import tech.arnav.twofac.lib.importer.ImportAdapter
import tech.arnav.twofac.lib.importer.ImportResult

class MyCustomAdapter : ImportAdapter {
    override suspend fun parse(fileContent: String, password: String?): ImportResult {
        return try {
            // Parse your custom format
            val otpAuthUris = parseCustomFormat(fileContent)
            ImportResult.Success(otpAuthUris)
        } catch (e: Exception) {
            ImportResult.Failure("Parse failed: ${e.message}", e)
        }
    }

    override fun getName(): String = "My Custom Authenticator"

    override fun requiresPassword(): Boolean = false

    private fun parseCustomFormat(content: String): List<String> {
        // Convert your format to otpauth:// URIs
        // otpauth://totp/Issuer:account?secret=XXX&issuer=Issuer&period=30
        return listOf(/* your parsed URIs */)
    }
}
```

## Important Notes

### Ente Auth Encrypted Exports
Ente Auth's encrypted export format uses **Argon2id** KDF and **XChaCha20-Poly1305** encryption, which are not currently implemented in this library. To import from Ente Auth:

1. **Option 1**: Export as plaintext (otpauth:// URIs) from Ente Auth settings
2. **Option 2**: Decrypt the encrypted export using Ente Auth first, then export as plaintext

### 2FAS Encrypted Exports
If your 2FAS export is password-protected, you'll need to decrypt it first using 2FAS before importing.

### Security Considerations
- Export files contain sensitive secrets in plaintext or weakly encrypted formats
- Always delete export files securely after importing
- Never share export files with anyone
- Use strong passwords when encrypting exports

## Troubleshooting

### Import Fails with "No valid services found"
- Check that the export file format matches the adapter you're using
- Ensure the file is not corrupted
- Verify the file is not encrypted (decrypt first if needed)

### "TwoFacLib is not unlocked" Error
- Make sure to call `lib.unlock(password)` before importing
- Verify you're using the correct password

### Individual Account Import Failures
The `importAccounts` method will attempt to import all accounts, but may skip individual accounts that have invalid data. Check the `ImportResult` for details on partial failures.
