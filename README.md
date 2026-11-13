# Gastify 💰

Aplicación Android para el **control de gastos e ingresos**: organiza tus cuentas, categorías y movimientos por espacios de trabajo, con recordatorios de pagos y sincronización en la nube.

---

## ✨ Funcionalidades

| Módulo | Descripción |
|---|---|
| 🔐 **Inicio de sesión** | Acceso con tu cuenta de Google (Firebase Authentication + Credential Manager) |
| 🏠 **Inicio** | Resumen general de tu actividad financiera |
| ↔️ **Movimientos** | Registra ingresos y gastos con monto, cuenta, categoría y saldo resultante |
| 🗂️ **Espacios** | Crea espacios de trabajo para separar finanzas (personal, viajes, negocio…) |
| 💳 **Cuentas** | Efectivo, banco, billetera, transporte u otro, con saldo inicial y actual |
| 🏷️ **Categorías** | Clasifica tus movimientos por tipo (ingreso/gasto), icono y color |
| 🔔 **Recordatorios** | Pagos recurrentes por día y hora, con notificaciones y botones de confirmación |
| 📊 **Estadísticas** | Sección de gráficos y reportes *(próximamente)* |

**Detalles técnicos de los recordatorios:**
- Notificaciones programadas con `WorkManager`
- Se recuperan automáticamente tras reiniciar el dispositivo o cambiar la hora
- Permiso `POST_NOTIFICATIONS` (Android 13+)

---

## 🛠️ Tecnologías

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Arquitectura:** MVVM (ViewModel + Repositorios)
- **Backend:** Firebase Authentication y Cloud Firestore
- **Google Sign-In:** Credential Manager + Google Identity Services
- **Tareas en segundo plano:** WorkManager
- **Build:** Gradle Kotlin DSL + Version Catalog (`libs.versions.toml`)

---

## 📋 Requisitos

- Android Studio (Ladybug o superior)
- JDK 11+
- Android SDK con `compileSdk 37`
- Dispositivo o emulador con **Android 7.0 (API 24)** o superior
- Proyecto de Firebase configurado (para autenticación y Firestore)

---

## 🚀 Puesta en marcha

```bash
# 1. Clonar el repositorio
git clone https://github.com/juliots04/Gastify.git
cd Gastify

# 2. Abrir en Android Studio y dejar sincronizar Gradle

# 3. Compilar e instalar en un dispositivo/emulador
./gradlew installDebug
```

> **Firebase:** el proyecto incluye `app/google-services.json`. Si usas tu propio proyecto de Firebase, reemplázalo por el tuyo desde la consola de Firebase (Ajustes del proyecto → Tus apps → Descargar).

---

## 📁 Estructura del proyecto

```
app/src/main/java/com/example/gastify/
├── MainActivity.kt          # Puerta de entrada + control de sesión
├── data/
│   ├── model/               # Account, Category, Movement, Workspace, PassageReminder
│   ├── local/               # Almacenamiento local (ReminderStore)
│   └── repository/          # Auth, Firestore (movimientos, cuentas, categorías…)
├── notifications/           # Recordatorios: receiver, worker y acciones
├── viewmodel/               # ViewModels por módulo
└── ui/
    ├── GastifyApp.kt        # Navegación principal (bottom bar)
    ├── screens/             # Home, Movimientos, Espacios, Cuentas, Categorías…
    ├── components/          # Iconos y componentes reutilizables
    └── theme/               # Colores, tipografía y tema
```

---

## 📦 Versión

- **versionName:** 1.0
- **minSdk:** 24 · **targetSdk:** 37

---

## ✍️ Autor

**juliots04** — [github.com/juliots04](https://github.com/juliots04)
