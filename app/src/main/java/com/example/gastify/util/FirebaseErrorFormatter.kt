package com.example.gastify.util

import com.google.firebase.firestore.FirebaseFirestoreException

import java.io.IOException

object FirebaseErrorFormatter {

    fun message(
        exception: Throwable?,
        action: String = "completar la operación"
    ): String {

        if (exception == null) {
            return "No se pudo $action."
        }

        // Buscar un posible error de Firestore
        // incluso cuando está dentro de otra excepción.

        val firestoreError = generateSequence(
            exception
        ) {
            it.cause
        }
            .take(8)
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

        if (firestoreError != null) {

            return when (firestoreError.code) {

                FirebaseFirestoreException.Code.UNAVAILABLE -> {

                    "No se pudo conectar con Firebase. " +
                        "Comprueba tu conexión a internet."
                }

                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> {

                    "La conexión tardó demasiado. " +
                        "Inténtalo nuevamente."
                }

                FirebaseFirestoreException.Code.PERMISSION_DENIED -> {

                    "Firebase rechazó la operación. " +
                        "Comprueba los permisos de tu cuenta " +
                        "y las reglas de seguridad."
                }

                FirebaseFirestoreException.Code.UNAUTHENTICATED -> {

                    "Tu sesión no está disponible. " +
                        "Inicia sesión nuevamente."
                }

                FirebaseFirestoreException.Code.ABORTED -> {

                    "Los datos cambiaron durante la operación. " +
                        "Inténtalo nuevamente."
                }

                FirebaseFirestoreException.Code.ALREADY_EXISTS -> {

                    "Este registro ya existe."
                }

                FirebaseFirestoreException.Code.NOT_FOUND -> {

                    "No se encontró la información solicitada."
                }

                FirebaseFirestoreException.Code.FAILED_PRECONDITION -> {

                    "No se cumplen las condiciones necesarias " +
                        "para completar la operación."
                }

                FirebaseFirestoreException.Code.INVALID_ARGUMENT -> {

                    "Los datos enviados no son válidos."
                }

                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> {

                    "Se alcanzó temporalmente un límite " +
                        "del servicio. Inténtalo más tarde."
                }

                FirebaseFirestoreException.Code.CANCELLED -> {

                    "La operación fue cancelada."
                }

                else -> {

                    "No se pudo $action. " +
                        "Inténtalo nuevamente."
                }
            }
        }

        // Conservar los mensajes de validación
        // generados por nuestra propia aplicación.

        if (
            exception is IllegalArgumentException ||
            exception is IllegalStateException
        ) {

            return exception.localizedMessage
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.take(200)
                ?: "Los datos de la operación no son válidos."
        }

        if (exception is SecurityException) {

            return "No tienes permiso para realizar esta acción."
        }

        if (exception is IOException) {

            return "Se produjo un problema de conexión. " +
                "Comprueba tu internet e inténtalo nuevamente."
        }

        return "No se pudo $action. Inténtalo nuevamente."
    }
}
