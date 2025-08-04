Aplicación que utiliza el acelerómetro del móvil para contar pasos, muestra alertas en el wearable (por ejemplo: "¡Llegaste al 80% de tu meta diaria!") y visualiza estadísticas en la Smart TV.

Este documento describe la arquitectura, componentes clave, funcionalidades y flujos de datos del backend correspondiente a la aplicación móvil StepUpQuest. Está dirigido a desarrolladores, colaboradores técnicos y partes interesadas, con el objetivo de ofrecer una comprensión clara y estructurada del sistema implementado en Android.
Alcance
Incluye el desarrollo backend de la versión Android de la aplicación, abordando la interacción con sensores, persistencia local de datos, comunicación con dispositivos Wear OS, y envío de información a Smart TVs.

Glosario
Acelerómetro: Sensor que detecta aceleraciones lineales.


Giroscopio: Sensor que mide la velocidad de rotación o cambio angular.


Wear OS: Sistema operativo de Google para dispositivos portátiles (wearables).


DataLayer: API de comunicación entre dispositivos Android conectados (móvil ↔ wearable).


SharedPreferences: Almacenamiento clave-valor local en Android.


MPAndroidChart: Librería para visualización de datos mediante gráficos.


ViewModel: Componente de arquitectura que gestiona datos relacionados con la interfaz de usuario.


TECNOLOGÍAS:

Activities:
StepsActivity: Pantalla principal, gestiona el conteo de pasos y navegación.
GoalActivity: Permite al usuario establecer su meta diaria.
StatsActivity: Muestra estadísticas a través de gráficos interactivos.
Managers y Helpers:
DataStorageManager: Encargado del almacenamiento local utilizando SharedPreferences.
StepDataManager: Administra el envío de datos y alertas al dispositivo Wear OS.
TVDataSender: Se encarga del envío de estadísticas a la Smart TV.
SensorFusionHelper: Fusiona datos del acelerómetro y giroscopio para mejorar la precisión en la detección de pasos.


Modelos de Datos:
StepData: Clase de datos utilizada para representar y transferir la información de pasos.


Servicios:
DataLayerService: Servicio encargado de recibir datos enviados desde el wearable.



Funcionalidades Detalladas
Conteo de Pasos
Sensores Utilizados
Sensor.TYPE_ACCELEROMETER: Sensor principal para detectar movimientos repetitivos del dispositivo.
Sensor.TYPE_GYROSCOPE: Sensor complementario para validar los pasos detectados y mejorar la precisión general.


Algoritmo de Detección
Se registran los listeners en el método onResume. A través de onSensorChanged, se obtienen los datos de los eventos.


Se calcula la magnitud de la aceleración mediante la fórmula sqrt(x² + y² + z²).


Se aplican umbrales temporales y de variación de aceleración para identificar patrones consistentes con el acto de caminar.


La detección es validada utilizando datos del giroscopio mediante la clase SensorFusionHelper, con el objetivo de reducir falsos positivos.


(Log del procesamiento de sensores - ver Anexo 1)


Actualización de Interfaz
Cada paso detectado incrementa el contador stepCount.


Se actualiza la UI a través del método updateStepCount, que refresca tanto el TextView del contador como la ProgressBar.


Los valores actualizados se persisten inmediatamente mediante DataStorageManager.


(Log de ejecución de updateStepCount - ver Anexo 2)


Almacenamiento Local
Tecnología Utilizada
Se emplea SharedPreferences para la persistencia eficiente de datos esenciales.

Datos Almacenados
Meta diaria de pasos (dailyGoal).


Conteo actual de pasos del día (dailySteps).


Historial de pasos diarios (stepHistory), utilizado para generar estadísticas.


Clase DataStorageManager
Centraliza las operaciones de lectura y escritura sobre SharedPreferences.


Métodos principales: saveDailyGoal, getDailyGoal, saveDailySteps, getTodaySteps, addToHistory, getStepHistory, getWeeklySteps.


(Log de historial desde getWeeklySteps - ver Anexo 3)



Comunicación con Wear OS
Tecnología
Se utiliza Wearable API junto con DataLayer para establecer una comunicación bidireccional y segura con dispositivos Wear OS vinculados.


Envío de Datos
La clase StepDataManager se encarga de enviar el conteo total de pasos al wearable mediante PutDataRequest y la ruta /step-data.


La operación se ejecuta de manera asíncrona utilizando Coroutines, lo que garantiza una experiencia fluida.

Notificaciones y Alertas
Al alcanzar el 50% o el 80% de la meta diaria, se dispara una notificación desde StepDataManager.

Esta notificación se envía utilizando la ruta /notifications, incluyendo el porcentaje alcanzado y un mensaje descriptivo.


Visualización de Estadísticas
Tecnología
Se utiliza la librería MPAndroidChart para generar gráficos de barras interactivos.


Datos Visualizados
Se muestra un gráfico de barras apiladas que representa:


Pasos diarios hasta alcanzar la meta (color verde).


Pasos que exceden la meta ("Plus Ultra", color naranja).


El eje Y del gráfico se ajusta dinámicamente al mayor valor semanal registrado.


ROLES:

Líder del Proyecto - Renteria Falcón Oscar Antonio 
Coordina al equipo
Supervisa el cronograma
Gestiona riesgos
Asegura la entrega final.

Frontend Android - Carvajal Alvarez María Fernanda
Diseña y desarrolla la interfaz gráfica del móvil: perfil de usuario, pantalla de estadísticas, pantalla de configuración, navegación.

Backend Android - Macias Ramirez Ramón de Jesús
Implementa el conteo de pasos
Sensores (acelerómetro y giroscopio)
Almacenamiento local
Comunicación con wearable y TV.

Frontend Wearable - Ibarra Cruz Grecia Montserrat
Diseña y desarrolla la interfaz para Wear OS: notificaciones y alertas visuales.

Backend Wearable - Renteria Falcón Oscar Antonio
Implementa la recepción de datos desde el móvil (DataLayer),
Lógica de notificaciones
Persistencia básica.

Testeo - Hernandez Franco Cindy Elizabeth
Realiza pruebas funcionales, de integración y usabilidad en las tres plataformas.
