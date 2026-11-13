package com.example.gastify

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.lifecycle.lifecycleScope

import androidx.credentials.exceptions.GetCredentialCancellationException

import com.example.gastify.data.repository.GoogleAuthRepository

import com.example.gastify.ui.GastifyApp
import com.example.gastify.ui.screens.auth.GoogleLoginScreen
import com.example.gastify.ui.theme.GastifyTheme

import com.google.firebase.auth.FirebaseAuth

import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val firebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val authRepository by lazy {
        GoogleAuthRepository()
    }

    private var isSignedIn by mutableStateOf(false)

    private var isLoading by mutableStateOf(false)

    private var errorMessage by mutableStateOf<String?>(null)

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        isSignedIn = auth.currentUser != null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        isSignedIn = firebaseAuth.currentUser != null

        setContent {

            GastifyTheme {

                if (isSignedIn) {

                    GastifyApp(
                        onSignOut = {
                            signOut()
                        }
                    )

                } else {

                    GoogleLoginScreen(
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onSignIn = {
                            signIn()
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        firebaseAuth.addAuthStateListener(authListener)
    }

    override fun onStop() {

        firebaseAuth.removeAuthStateListener(authListener)

        super.onStop()
    }

    private fun signIn() {

        if (isLoading) return

        isLoading = true
        errorMessage = null

        lifecycleScope.launch {

            try {

                authRepository.signInWithGoogle(
                    this@MainActivity
                )

            } catch (e: GetCredentialCancellationException) {

                // El usuario cerró el selector de cuentas.

            } catch (e: Exception) {

                errorMessage =
                    e.localizedMessage
                        ?: "No se pudo iniciar sesión con Google."

            } finally {

                isLoading = false
            }
        }
    }

    private fun signOut() {

        authRepository.signOut()

        errorMessage = null

        lifecycleScope.launch {

            try {

                authRepository.clearGoogleSession(
                    this@MainActivity
                )

            } catch (_: Exception) {

                // La sesión de Firebase ya se cerró.
            }
        }
    }
}
