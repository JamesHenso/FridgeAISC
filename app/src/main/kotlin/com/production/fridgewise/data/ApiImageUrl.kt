package com.production.fridgewise.data

import com.production.fridgewise.BuildConfig
import java.net.URI

/** Catalog stores portable paths; resolve against the backend, not the OCR host. */
fun apiImageUrl(value: String?, baseUrl: String = BuildConfig.API_BASE_URL): String? {
    val path = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return runCatching {
        val uri = URI(path)
        val resolved = if (uri.isAbsolute) uri else {
            if (!path.startsWith("/assets/") || path.startsWith("//")) return null
            URI(baseUrl).resolve(uri)
        }
        resolved.takeIf { it.scheme in setOf("http", "https") && it.host != null }?.toASCIIString()
    }.getOrNull()
}
