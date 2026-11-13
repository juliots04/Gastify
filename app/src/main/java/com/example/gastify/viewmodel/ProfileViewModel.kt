package com.example.gastify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isGoogleAccount: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val userId: String
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(
        buildProfileState(
            auth.currentUser
        )
    )

    val uiState: StateFlow<ProfileUiState> =
        _uiState

    // ==========================================
    // OBTENER PERFIL
    // ==========================================

    private fun buildProfileState(
        user: FirebaseUser?
    ): ProfileUiState {

        if (
            user == null ||
            user.uid != userId
        ) {
            return ProfileUiState(
                displayName = "Usuario",
                errorMessage = "Sesión no disponible."
            )
        }

        // Obtener los datos originales de Google.

        val googleProfile = user.providerData
            .firstOrNull {
                it.providerId ==
                    GoogleAuthProvider.PROVIDER_ID
            }

        val isGoogleAccount = googleProfile != null

        // Priorizar el nombre completo de Google.

        val fullName = if (isGoogleAccount) {

            googleProfile.displayName
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }
                ?: "Nombre no disponible en Google"

        } else {

            user.displayName
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }
                ?: "Usuario"
        }

        // Priorizar el correo de Google.

        val email = googleProfile
            ?.email
            ?.takeIf {
                it.isNotBlank()
            }
            ?: user.email.orEmpty()

        // Obtener únicamente la foto de Google
        // cuando la cuenta está vinculada.

        val photoUrl = if (isGoogleAccount) {

            googleProfile.photoUrl
                ?.toString()
                ?.takeIf {
                    it.isNotBlank()
                }

        } else {

            user.photoUrl
                ?.toString()
        }

        return ProfileUiState(
            displayName = fullName,
            email = email,
            photoUrl = photoUrl,
            isGoogleAccount = isGoogleAccount,
            isLoading = false,
            errorMessage = null
        )
    }

    // ==========================================
    // ACTUALIZAR AUTOMÁTICAMENTE
    // ==========================================

    fun refreshProfile() {

        val user = auth.currentUser

        if (
            user == null ||
            user.uid != userId
        ) {

            _uiState.value = ProfileUiState(
                displayName = "Usuario",
                errorMessage = "Sesión no disponible."
            )

            return
        }

        // Mostrar datos disponibles inmediatamente.

        _uiState.value = buildProfileState(
            user
        ).copy(
            isLoading = true
        )

        // Solicitar información actualizada
        // al servidor de Firebase.

        user.reload()
            .addOnCompleteListener { task ->

                val currentUser = auth.currentUser

                val updatedState = buildProfileState(
                    currentUser
                )

                _uiState.value = updatedState.copy(

                    isLoading = false,

                    errorMessage = if (
                        task.isSuccessful
                    ) {

                        null

                    } else {

                        "No se pudo actualizar el perfil. Se muestran los datos disponibles."
                    }
                )
            }
    }

    // ==========================================
    // FACTORY
    // ==========================================

    companion object {

        fun factory(
            userId: String
        ): ViewModelProvider.Factory {

            return object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {

                    require(
                        modelClass.isAssignableFrom(
                            ProfileViewModel::class.java
                        )
                    )

                    return ProfileViewModel(
                        userId
                    ) as T
                }
            }
        }
    }
}
