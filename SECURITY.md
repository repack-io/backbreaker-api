# Security Documentation

## Card Token Security

### Overview

The QR code links use HMAC-SHA256 signed tokens to prevent tampering and unauthorized access. This replaces the previous simple Base64 encoding which was predictable and easy to modify.

### Token Format

Tokens have the following structure before Base64 encoding:
```
{cardId}.{timestamp}.{signature}
```

- **cardId**: The series card ID
- **timestamp**: Unix timestamp (seconds) when the token was generated
- **signature**: HMAC-SHA256 hash of `{cardId}.{timestamp}` using a secret key

The entire token is then Base64 URL-encoded for safe use in URLs.

### Example Token Flow

1. **Generation** (in `CardTokenService.generateToken()`):
   - Input: `cardId = 123`
   - Payload: `123.1733079600`
   - Signature: `a3f7b2c...` (HMAC-SHA256 of payload with secret)
   - Full token: `123.1733079600.a3f7b2c...`
   - Base64 encoded: `MTIzLjE3MzMwNzk2MDAuYTNmN2IyYy4uLg==`

2. **Validation** (in `CardTokenService.validateAndExtract()`):
   - Decode Base64 token
   - Split into cardId, timestamp, signature
   - Recompute signature using secret key
   - Compare signatures - if they match, token is valid
   - Return the cardId

### Security Benefits

1. **Tamper-Proof**: Any modification to the token (changing card ID, timestamp) will cause signature verification to fail
2. **Longer Tokens**: Base64-encoded signed tokens are 80-100+ characters instead of 2-4 characters
3. **Non-Sequential**: Tokens include timestamps and cryptographic hashes, making them unpredictable
4. **Environment Isolation**: Each environment (dev, qa, prd) has its own secret key, so tokens from one environment won't work in another

### Secret Key Management

**CRITICAL**: Each environment must have a unique secret key.

#### Current Configuration

- **DEV**: `rueM1tAHr-gzQpvlZ6VbacKYRaZCVnT3d4GV5Gjv5kA`
- **QA**: `zfbZXFXdRvssc6_92rOoD4vK0zOqd3CwzXCKztS6-XQ`
- **PROD**: `577xZLdOuIh-wpZ3nBu1IPJJ6PQenO7JOv4jRa2KVcI`

These are 256-bit URL-safe secrets generated using Python's `secrets.token_urlsafe(32)`.

#### Rotating Secrets

If you need to rotate the secret key:

1. Generate a new secret:
   ```bash
   python3 -c "import secrets; print(secrets.token_urlsafe(32))"
   ```

2. Update the `card.token.secret` property in the environment's properties file:
   - DEV: `src/main/resources/application-aws-dev.properties`
   - QA: `src/main/resources/application-aws-qa.properties`
   - PRD: `src/main/resources/application-aws-prd.properties`

3. Rebuild and redeploy

**IMPORTANT**: All existing QR codes will become invalid when you rotate the secret. This is a security feature - only generate new secrets when you want to invalidate all existing links.

### Token Expiry

Currently, tokens do not expire automatically. The timestamp is included in the signature for future expiry validation if needed.

To add expiry (optional):

```java
// In CardTokenService.validateAndExtract()
long tokenTimestamp = Long.parseLong(timestamp);
long currentTimestamp = System.currentTimeMillis() / 1000;
long maxAge = 30 * 24 * 60 * 60; // 30 days in seconds

if (currentTimestamp - tokenTimestamp > maxAge) {
    throw new IllegalArgumentException("Token has expired");
}
```

### Implementation Files

- **Token Service**: [CardTokenService.java](src/main/java/com/repackio/backbreaker/services/CardTokenService.java)
- **Token Generation**: [LabelSheetService.java:212](src/main/java/com/repackio/backbreaker/services/LabelSheetService.java#L212)
- **Token Validation**: [ThisGotHitController.java:34](src/main/java/com/repackio/backbreaker/api/ThisGotHitController.java#L34)

### Testing

Test token tampering protection:

1. Generate a valid QR code and scan it
2. Try to modify any part of the token in the URL
3. The system should return: "This link appears to be invalid or has been tampered with"

### Migration from Old Format

The old simple Base64 encoding has been completely replaced. All new QR codes will use the signed token format.

**Note**: If you have existing QR codes in the wild using the old format, they will fail with "Invalid token format". You would need to maintain backwards compatibility or regenerate all labels.
