package com.tefbanesco

/**
 * Utilidad para generar comandos curl equivalentes a las operaciones HTTP realizadas
 * con fines de depuración y verificación.
 */
object CurlGenerator {
    /**
     * Genera un comando curl equivalente a la solicitud HTTP que se realizaría
     *
     * @param method Método HTTP (GET, POST, PUT, DELETE, etc)
     * @param url URL completa de la solicitud
     * @param headers Mapa de cabeceras HTTP
     * @param body Cuerpo de la solicitud (para POST, PUT, etc)
     * @return String con el comando curl completo
     */
    fun generateCurlCommand(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String? = null
    ): String {
        val sb = StringBuilder()
        sb.append("curl -X $method \\\n")

        // Agregar cabeceras
        headers.forEach { (key, value) ->
            // Sanitizar valor para evitar problemas con caracteres especiales
            val sanitizedValue = value.replace("\"", "\\\"")
            sb.append("  -H \"$key: $sanitizedValue\" \\\n")
        }

        // Agregar cuerpo si existe
        if (!body.isNullOrEmpty()) {
            val sanitizedBody = body.replace("\"", "\\\"").replace("\n", "")
            sb.append("  -d \"$sanitizedBody\" \\\n")
        }

        // Agregar URL
        sb.append("  \"$url\"")

        return sb.toString()
    }

    /**
     * Oculta información sensible como claves API o tokens para la presentación
     *
     * @param curlCommand Comando curl completo
     * @return Comando curl con información sensible oculta
     */
    fun sanitizeForDisplay(curlCommand: String): String {
        var sanitized = curlCommand

        // Ocultar API Keys
        sanitized = sanitized.replace(Regex("-H \"X-API-Key: ([^\"]*)\""), "-H \"X-API-Key: ***REDACTED***\"")
        sanitized = sanitized.replace(Regex("-H \"X-Secret-Key: ([^\"]*)\""), "-H \"X-Secret-Key: ***REDACTED***\"")

        // Ocultar tokens de autorización
        sanitized = sanitized.replace(Regex("-H \"Authorization: Bearer ([^\"]*)\""), "-H \"Authorization: Bearer ***REDACTED***\"")

        return sanitized
    }
}