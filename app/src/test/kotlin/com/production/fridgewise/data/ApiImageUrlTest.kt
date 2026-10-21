package com.production.fridgewise.data

import org.junit.Assert.*
import org.junit.Test

class ApiImageUrlTest {
    @Test fun resolvesPortableAssetsAgainstBackendAndKeepsEncodedNames() {
        assertEquals("http://127.0.0.1:3000/assets/thit-heo.webp", apiImageUrl("/assets/thit-heo.webp", "http://127.0.0.1:3000/"))
        assertEquals("https://api.example/assets/bap%2C%20chan_gio.webp", apiImageUrl("/assets/bap%2C%20chan_gio.webp", "https://api.example/v1/"))
        assertEquals("https://cdn.example/pork.webp", apiImageUrl("https://cdn.example/pork.webp", "http://localhost:3000/"))
        assertNull(apiImageUrl(null))
        assertNull(apiImageUrl("file:///etc/passwd"))
        assertNull(apiImageUrl("//another-host/image.webp"))
        assertNull(apiImageUrl("/private/image.webp"))
    }
}
