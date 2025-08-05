# 🚶‍♀️ StepUpQuest 🧭  
> ¡Convierte cada paso en una aventura!

Aplicación Android que convierte tus pasos diarios en estadísticas motivadoras y notificaciones en tu wearable, con visualización en Smart TV. 🏃📱⌚📺

---

## 📱 ¿Qué es StepUpQuest?

**StepUpQuest** es una app multiplataforma que:

- 🦶 Usa el **acelerómetro** del móvil para contar tus pasos.
- 📣 Envía **notificaciones al wearable** cuando alcanzas hitos (por ejemplo, “¡Llegaste al 80% de tu meta diaria!”).
- 📊 Muestra **estadísticas motivacionales** en tu Smart TV a través de una visualización clara y atractiva.
- 🌎 Está pensada como una solución para fomentar hábitos saludables y combatir el sedentarismo en todas las edades.

---

## 💡 Solución social

StepUpQuest nace con un propósito más allá del fitness.  
### 🙌 Buscamos impactar en:
- 👵 Personas mayores que necesitan mantenerse activas y motivadas.
- 👨‍👩‍👧 Familias que desean integrar hábitos saludables mediante retos de pasos.
- 🧠 Usuarios que requieren una herramienta accesible y visual para su bienestar mental y físico.

> ¡La actividad física puede ser divertida, visual y colaborativa! 💥

---

## ⚙️ Características técnicas

| Funcionalidad | Tecnología |
|---------------|------------|
| Conteo de pasos | `Sensor.TYPE_ACCELEROMETER` de Android |
| Notificaciones al wearable | API de `Bluetooth` / `Wear OS` |
| Visualización en TV | `Google Cast SDK` / `DLNA` opcional |
| UI adaptable | `Jetpack Compose` |
| Persistencia de datos | `Room Database` |
| Estadísticas | Gráficos con `MPAndroidChart` |

---

## 🛠️ Cómo clonar y correr el proyecto en Android Studio

### 1. 🔄 Clonar el repositorio

git clone https://github.com/tuusuario/StepUpQuest.git
cd StepUpQuest

### 2. ⚙️ Abrir en Android Studio

- Abre Android Studio y selecciona **“Open an Existing Project”**  
- Navega a la carpeta clonada y ábrela

### 3. 🧱 Configura dependencias

- Asegúrate de tener instalado:
  - SDK mínimo: **API 26**
  - Kotlin versión **1.9+**
  - Android Gradle Plugin actualizado
- Sincroniza el proyecto con Gradle

### 4. 🚀 Ejecutar en tu dispositivo

- Conecta tu dispositivo Android o usa un emulador  
- Haz clic en **Run ▶️**  
- ¡Empieza a caminar y ver tu progreso en tiempo real! 🙌
