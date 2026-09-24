package app.wireframephoto.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionsTest {
    @Test
    fun parse_stripsTagPrefixAndBuildSuffix() {
        assertEquals(listOf(1, 4, 0), Versions.parse("v1.4.0"))
        assertEquals(listOf(1, 3, 0), Versions.parse("1.3.0-debug"))
    }

    @Test
    fun isNewer_comparesNumericallyNotAsText() {
        assertTrue(Versions.isNewer("v1.10.0", "1.9.0"))
        assertTrue(Versions.isNewer("v1.4.0", "1.3.0"))
        assertTrue(Versions.isNewer("v2.0", "1.9.9"))
        assertFalse(Versions.isNewer("v1.3.0", "1.3.0"))
        assertFalse(Versions.isNewer("v1.3.0", "1.4.0-debug"))
        assertFalse(Versions.isNewer("v1.3", "1.3.0"))
    }
}
