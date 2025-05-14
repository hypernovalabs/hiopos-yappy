package com.tefbanesco

/**
 * Códigos de error de la API de Yappy y sus descripciones para presentar al usuario
 * una información más clara y útil.
 */
object YappyErrorCodes {

    /**
     * Obtiene una descripción amigable para un código de error de Yappy
     * @param code Código de error de Yappy
     * @return Descripción del error en español
     */
    fun getErrorDescription(code: String): String {
        return when (code) {
            // Errores de autenticación
            "YP-0001" -> "Credenciales inválidas. Verifica tu API Key y Secret Key."
            "YP-0002" -> "Token de autorización inválido o expirado."
            "YP-0003" -> "No tienes permisos para esta operación."
            
            // Errores de validación
            "YP-0009" -> "Faltan campos obligatorios en la solicitud. Verifica la configuración del dispositivo."
            "YP-0010" -> "Formato de JSON inválido en la solicitud."
            "YP-0011" -> "Valor de campo inválido. Verifica los datos enviados."
            "YP-0013" -> "El ID del dispositivo ya está registrado con una sesión activa."
            
            // Errores de transacción
            "YP-0021" -> "No se encontró la transacción solicitada."
            "YP-0022" -> "El estado de la transacción no permite esta operación."
            "YP-0023" -> "La transacción fue cancelada por el usuario."
            "YP-0024" -> "El monto de la transacción es inválido o no está permitido."
            "YP-0025" -> "La moneda de la transacción no está soportada."
            
            // Errores de sesión
            "YP-0031" -> "No se encontró una sesión activa para este dispositivo."
            "YP-0032" -> "La sesión ha expirado. Intenta iniciar una nueva sesión."
            
            // Errores de QR
            "YP-0041" -> "Error al generar el código QR."
            "YP-0042" -> "El código QR ha expirado."
            
            // Errores del sistema
            "YP-0099" -> "Error interno del sistema Yappy. Intenta nuevamente más tarde."
            "YP-0098" -> "El servicio de Yappy está en mantenimiento. Intenta más tarde."
            "YAPPY-998" -> "Servicio temporalmente no disponible. Por favor intente nuevamente en unos momentos."
            
            // Código desconocido
            else -> "Error desconocido ($code). Contacta a soporte técnico."
        }
    }
    
    /**
     * Verifica si el error es recuperable (se puede reintentar)
     */
    fun isRecoverableError(code: String): Boolean {
        return when (code) {
            // Errores temporales que pueden resolverse con un reintento
            "YP-0098", "YP-0099", "YP-0032" -> true
            else -> false
        }
    }
}