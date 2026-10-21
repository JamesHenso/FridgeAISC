package com.production.fridgewise.data

import java.security.MessageDigest
import java.security.SecureRandom

/** PKCE (RFC 7636) helpers for the browser-based OAuth flow. */
internal object OAuthPkce {
    fun verifier(): String = base64Url(SecureRandom().generateSeed(32))

    fun challenge(verifier: String): String =
        base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    // Hand-rolled because java.util.Base64 needs API 26 and this app supports API 23.
    private fun base64Url(bytes: ByteArray): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val out = StringBuilder((bytes.size + 2) / 3 * 4)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xff
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xff else 0
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xff else 0
            out.append(alphabet[b0 shr 2])
            out.append(alphabet[((b0 and 3) shl 4) or (b1 shr 4)])
            if (i + 1 < bytes.size) out.append(alphabet[((b1 and 15) shl 2) or (b2 shr 6)])
            if (i + 2 < bytes.size) out.append(alphabet[b2 and 63])
            i += 3
        }
        return out.toString()
    }
}
