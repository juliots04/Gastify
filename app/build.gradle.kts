plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.gastify"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.gastify"
        minSdk = 24
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf(
                    "androidx.**",
                    "kotlin.**",
                    "kotlinx.**"
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    // ANDROID
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // JETPACK COMPOSE
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // ICONOS VECTORIALES
    implementation(
        "androidx.compose.material:material-icons-extended"
    )

    // VIEWMODEL
    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1"
    )

    // FIREBASE
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // GOOGLE SIGN-IN
    implementation(
        "androidx.credentials:credentials:1.6.0"
    )

    implementation(
        "androidx.credentials:credentials-play-services-auth:1.6.0"
    )

    implementation(
        "com.google.android.libraries.identity.googleid:googleid:1.2.1"
    )

    // TAREAS EN SEGUNDO PLANO
    implementation(
        "androidx.work:work-runtime-ktx:2.12.0"
    )

    // TESTING
    testImplementation(libs.junit)

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(libs.androidx.junit)

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}
