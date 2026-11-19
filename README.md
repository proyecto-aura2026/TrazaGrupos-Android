# TrazaGrupos Android — fuente inicial

Proyecto Android nativo para abrir con Android Studio o compilar mediante GitHub Actions. Usa JDK 17, Gradle 8.9 y Android SDK 35. El APK de depuración queda en `app/build/outputs/apk/debug/app-debug.apk`; para distribuir versiones estables, crear y conservar un keystore propio y generar un APK firmado.

## Compilar desde GitHub

1. Creá un repositorio nuevo para TrazaGrupos y subí **el contenido de esta carpeta** a la raíz del repositorio. La carpeta `.github/workflows` debe quedar en la raíz junto a `settings.gradle`.
2. En la pestaña **Actions**, abrí **Compilar APK Android** y pulsá **Run workflow**. También se ejecuta al enviar cambios a `main`.
3. Al terminar, abrí la ejecución y descargá el artefacto `TrazaGrupos-APK-debug`. Descomprimí ese artefacto: dentro estará `app-debug.apk`.
4. El APK de depuración sirve para instalación y pruebas. Su firma es de depuración; no es apropiada para versiones estables o actualizaciones garantizadas entre compilaciones.

Al instalar, abrir la app, leer la explicación y habilitar manualmente el servicio en Accesibilidad. Abrir un grupo de WhatsApp. El botón flotante abre el panel; la herramienta solo procesa texto que Android expone en la pantalla actual y mensajes vistos mientras se desplaza. Los datos se mantienen en memoria durante la sesión. El borrador se copia al portapapeles tras acción explícita para que el usuario lo pegue y envíe manualmente.

## Estado

Fuente de desarrollo sin APK compilado ni prueba en dispositivo. La atribución de autor y teléfono requiere calibración con la jerarquía de accesibilidad de WhatsApp en distintos Android y versiones. La clasificación inicial es local, por palabras clave; no interpreta semántica profunda ni garantiza recuperar todo el historial. No hace lectura de chats cerrados ni de mensajes no cargados.
