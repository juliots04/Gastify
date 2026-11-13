package com.example.gastify.data.repository

import android.app.Activity
import android.content.Context

import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest

import com.example.gastify.R

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

import kotlinx.coroutines.suspendCancellableCoroutine

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GoogleAuthRepository {

    private val firebaseAuth = FirebaseAuth.getInstance()

    suspend fun signInWithGoogle(activity: Activity) {

        // Obtener administrador de credenciales
        val credentialManager = CredentialManager.create(activity)

        // Configurar acceso mediante Google
        val googleOption = GetSignInWithGoogleOption.Builder(
            activity.getString(R.string.default_web_client_id)
        ).build()

        // Crear solicitud
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        // Mostrar selector de cuentas de Google
        val result = credentialManager.getCredential(
            context = activity,
            request = request
        )

        // Verificar tipo de credencial
        val credential = result.credential

        if (
            credential !is CustomCredential ||
            credential.type !=
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException(
                "No se recibió una credencial válida de Google."
            )
        }

        // Extraer token de Google
        val googleCredential = GoogleIdTokenCredential.createFrom(
            credential.data
        )

        // Convertir token en credencial Firebase
        val firebaseCredential = GoogleAuthProvider.getCredential(
            googleCredential.idToken,
            null
        )

        // Autenticar en Firebase
        suspendCancellableCoroutine<Unit> { continuation ->

            firebaseAuth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener { task ->

                    if (!continuation.isActive) {
                        return@addOnCompleteListener
                    }

                    if (task.isSuccessful) {

                        continuation.resume(Unit)

                    } else {

                        continuation.resumeWithException(
                            task.exception
                                ?: Exception(
                                    "No se pudo iniciar sesión."
                                )
                        )
                    }
                }
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun clearGoogleSession(context: Context) {

        val credentialManager = CredentialManager.create(context)

        credentialManager.clearCredentialState(
            ClearCredentialStateRequest()
        )
    }
}
