package org.opencontrol.erp.kernel.config

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JacksonConfigTest {

    private val jacksonConfig = JacksonConfig()

    @Test
    fun `objectMapper should serialize Instant as ISO-8601 string`() {
        val mapper = jacksonConfig.objectMapper()
        assertNotNull(mapper)

        val map = mapOf("timestamp" to Instant.parse("2026-11-02T00:00:00Z"))
        val json = mapper.writeValueAsString(map)

        assertTrue(json.contains("2026-11-02T00:00:00Z"))
    }
}
