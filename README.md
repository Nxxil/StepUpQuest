# 🚶‍♀️ StepUpQuest 🧭  
> ¡Convierte cada paso en una aventura! Aplicación Android que convierte tus pasos diarios en estadísticas motivadoras y notificaciones en tu wearable, con visualización en Smart TV. 🏃📱⌚📺
---
## 📱 ¿Qué es StepUpQuest?
**StepUpQuest** es una app multiplataforma que:
- 🦶 Usa el **acelerómetro** del móvil para contar tus pasos.
- 📣 Envía **notificaciones al wearable** cuando alcanzas hitos (por ejemplo, “¡Llegaste al 80% de tu meta diaria!”).
- 📊 Muestra **estadísticas motivacionales** en tu Smart TV a través de una visualización clara y atractiva.
- 🌎 Está pensada como una solución para fomentar hábitos saludables y combatir el sedentarismo en todas las edades.
---
## 🦾 Propósito
Este documento describe la arquitectura, componentes clave, funcionalidades y flujos de datos del backend correspondiente a la aplicación móvil StepUpQuest. 
Está dirigido a desarrolladores, colaboradores técnicos y partes interesadas, con el objetivo de ofrecer una comprensión clara y estructurada del sistema implementado en Android.

---
## 💻 Alcance
Incluye el desarrollo backend de la versión Android de la aplicación, abordando la interacción con sensores, persistencia local de datos, comunicación con dispositivos Wear OS, y envío de información a Smart TVs.

---
## 💡 Solución social
StepUpQuest nace con un propósito más allá del fitness.  
**🙌 Buscamos impactar en:**
- 👵 Personas mayores que necesitan mantenerse activas y motivadas.
- 👨‍👩‍👧 Familias que desean integrar hábitos saludables mediante retos de pasos.
- 🧠 Usuarios que requieren una herramienta accesible y visual para su bienestar mental y físico.
> ¡La actividad física puede ser divertida, visual y colaborativa! 💥
---
## 🧾 Glosario
- **Acelerómetro:** Sensor que detecta aceleraciones lineales.

- **Giroscopio:** Sensor que mide la velocidad de rotación o cambio angular.

- **Wear OS:** Sistema operativo de Google para dispositivos portátiles (wearables).

- **DataLayer:** API de comunicación entre dispositivos Android conectados (móvil ↔ wearable).

- **SharedPreferences:** Almacenamiento clave-valor local en Android.

- **MPAndroidChart:** Librería para visualización de datos mediante gráficos.

- **ViewModel:** Componente de arquitectura que gestiona datos relacionados con la interfaz de usuario.

---
## ⚙️ Características técnicas
| Funcionalidad               | Tecnología                                          |
|----------------------------|-----------------------------------------------------|
| Conteo de pasos            | `Sensor.TYPE_ACCELEROMETER` de Android             |
| Notificaciones al wearable | API de `Bluetooth` / `Wear OS`                     |
| Visualización en TV        | `Google Cast SDK` / `DLNA` opcional                |
| UI adaptable               | `Jetpack Compose`                                  |
| Persistencia de datos      | `Room Database`                                    |
| Estadísticas               | Gráficos con `MPAndroidChart`                      |
---
# 📊 Funcionalidades Detalladas

## 🚶 Conteo de Pasos

### 🔧 Sensores Utilizados
- `Sensor.TYPE_ACCELEROMETER`: Sensor principal para detectar movimientos repetitivos del dispositivo.
- `Sensor.TYPE_GYROSCOPE`: Sensor complementario para validar los pasos detectados y mejorar la precisión general.

### 🧠 Algoritmo de Detección
1. Se registran los listeners en el método `onResume`. A través de `onSensorChanged`, se obtienen los datos de los eventos.
2. Se calcula la magnitud de la aceleración mediante la fórmula `sqrt(x² + y² + z²)`.
3. Se aplican umbrales temporales y de variación de aceleración para identificar patrones consistentes con el acto de caminar.
4. La detección es validada utilizando datos del giroscopio mediante la clase `SensorFusionHelper`, con el objetivo de reducir falsos positivos.
---
## 🖥️ Actualización de Interfaz
- Cada paso detectado incrementa el contador `stepCount`.
- Se actualiza la UI a través del método `updateStepCount`, que refresca tanto el `TextView` del contador como la `ProgressBar`.
- Los valores actualizados se persisten inmediatamente mediante `DataStorageManager`.
---
## 💾 Almacenamiento Local

### 🛠 Tecnología Utilizada
  - `SharedPreferences` para la persistencia eficiente de datos esenciales.

### 📚 Datos Almacenados  
  - Meta diaria de pasos (`dailyGoal`).
  - Conteo actual de pasos del día (`dailySteps`).
  - Historial de pasos diarios (`stepHistory`), utilizado para generar estadísticas.

### 🔧 Clase `DataStorageManager`  
  Centraliza las operaciones de lectura y escritura sobre `SharedPreferences`.

  **Métodos principales:**
  - `saveDailyGoal`, `getDailyGoal`- `saveDailySteps`, `getTodaySteps`- `addToHistory`, `getStepHistory`, `getWeeklySteps`
---
## ⌚ Comunicación con Wear OS

### 📶 Tecnología  
  - `Wearable API` junto con `DataLayer` para establecer una comunicación bidireccional y segura con dispositivos Wear OS vinculados.

### 📤 Envío de Datos  
  - La clase `StepDataManager` se encarga de enviar el conteo total de pasos al wearable mediante `PutDataRequest` y la ruta `/step-data`.
  - La operación se ejecuta de manera asíncrona utilizando `Coroutines`, lo que garantiza una experiencia fluida.

### 🔔 Notificaciones y Alertas  
  - Al alcanzar el **50%** o el **80%** de la meta diaria, se dispara una notificación desde `StepDataManager`.
  - Esta notificación se envía utilizando la ruta `/notifications`, incluyendo el porcentaje alcanzado y un mensaje descriptivo.
---
## 📈 Visualización de Estadísticas  
### 🛠 Tecnología  
- Librería `MPAndroidChart` para generar gráficos de barras interactivos.
### 📊 Datos Visualizados  
- Se muestra un gráfico de barras apiladas que representa:    
- ✅ Pasos diarios hasta alcanzar la meta (**color verde**).    
- 🟧 Pasos que exceden la meta ("**Plus Ultra**", **color naranja**). 
- El eje Y del gráfico se ajusta dinámicamente al mayor valor semanal registrado.
---
## 🛠️ Cómo clonar y correr el proyecto en Android Studio
### 1. 🔄 Clonar el repositorio
- git clone https://github.com/tuusuario/StepUpQuest.gitcd StepUpQuest
### 2. ⚙️ Abrir en Android Studio
- Abre Android Studio y selecciona **“Open an Existing Project”**
- Navega a la carpeta clonada y ábrela
### 3. 🧱 Configura dependencias
Asegúrate de tener instalado:  
- SDK mínimo: **API 26**  - Kotlin versión **1.9+**
- Android Gradle Plugin actualizado- Sincroniza el proyecto con Gradle
### 4. 🚀 Ejecutar en tu dispositivo
- Conecta tu dispositivo Android o usa un emulador
- Haz clic en **Run ▶️**
- ¡Empieza a caminar y ver tu progreso en tiempo real! 🙌
---
## 🥸 ROLES:
### Líder del Proyecto (Renteria Falcón Oscar Antonio)
- Coordina al equipo, Supervisa el cronograma, Gestiona riesgos, Asegura la entrega final.
### Frontend Android (Carvajal Alvarez María Fernanda)
- Diseña y desarrolla la interfaz gráfica del móvil: perfil de usuario, pantalla de estadísticas, pantalla de configuración, navegación.
### Backend Android (Macias Ramirez Ramón de Jesús)
- Implementa el conteo de pasos, Sensores (acelerómetro y giroscopio), Almacenamiento local, Comunicación con wearable y TV.
### Frontend Wearable (Ibarra Cruz Grecia Montserrat)
- Diseña y desarrolla la interfaz para Wear OS: notificaciones y alertas visuales.
### Backend Wearable (Renteria Falcón Oscar Antonio)
- Implementa la recepción de datos desde el móvil (DataLayer), Lógica de notificaciones, Persistencia básica.
### Testeo (Hernandez Franco Cindy Elizabeth)
- Realiza pruebas funcionales, de integración y usabilidad en las tres plataformas.

