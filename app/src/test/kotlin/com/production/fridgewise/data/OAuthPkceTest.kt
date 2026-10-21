package com.production.fridgewise.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthPkceTest {
    @Test fun challengeMatchesRfc7636Vector() {
        // Test vector from RFC 7636 appendix B.
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            OAuthPkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
        )
    }

    @Test fun verifierIsUrlSafeAndUnique() {
        val a = OAuthPkce.verifier()
        val b = OAuthPkce.verifier()
        assertNotEquals(a, b)
        assertTrue(a, a.matches(Regex("[A-Za-z0-9_-]{43}")))
    }
}
