package org.opencontrol.erp.kernel.exception

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `should map EntityNotFoundException to 404 ProblemDetail`() {
        val ex = EntityNotFoundException("Plugin not found with ID: 123")
        val response = handler.handleOpenControlException(ex)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNotNull(response.body)
        assertEquals("ENTITY_NOT_FOUND", response.body?.title)
        assertEquals("Plugin not found with ID: 123", response.body?.detail)
    }

    @Test
    fun `should map PluginConflictException to 409 ProblemDetail`() {
        val ex = PluginConflictException("Plugin already installed")
        val response = handler.handleOpenControlException(ex)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("PLUGIN_CONFLICT", response.body?.title)
        assertEquals("Plugin already installed", response.body?.detail)
    }

    @Test
    fun `should map unhandled Exception to 500 ProblemDetail`() {
        val ex = RuntimeException("Unexpected internal database error")
        val response = handler.handleGenericException(ex)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("INTERNAL_SERVER_ERROR", response.body?.title)
        assertEquals("Unexpected internal database error", response.body?.detail)
    }
}
