# Changelog

## [1.7] - 2026-04-29
### Actual
- 🎭 **Playlists Inmersivas**: Rediseño de la interfaz de listas con cabeceras dinámicas, efectos de paralaje y escalado al hacer scroll.
- 🗑️ **Gestión de Canciones**: Añadida la capacidad de eliminar canciones individualmente de las listas de reproducción.
- 💾 **Persistencia Robusta**: Actualizado `PlaylistManager` para manejar la eliminación de pistas y sincronizar el estado visual.
- 🛠️ **Refactorización de LibraryTab**: Corregidos errores de referencia y optimizada la lógica de navegación entre artistas, álbumes y carpetas.
- 🎨 **UI de Opciones**: El menú de opciones de canción ahora muestra dinámicamente "Eliminar de la lista" solo cuando es relevante.

## [1.6] - 2026-04-27
### Actual
- 📂 **Gestión de Descargas**: Añadido nuevo historial de descargas y una guía detallada para el usuario.
- 📑 **Gesto de Cola**: Acceso intuitivo a la cola de reproducción deslizando hacia arriba desde el reproductor.
- 🛠️ **Solución de Descargas**: Corregido un error crítico que impedía bajar archivos correctamente.
- 📜 **Scroll de Letras**: Reparada la navegación y el desplazamiento en la vista de letras de canciones.

## [1.5] - 2026-04-26
### Actual
- 📊 **Visualizador Reactivo**: Nuevo sistema de procesamiento FFT con agrupación cuadrática para una respuesta dinámica en todas las frecuencias.
- ⚙️ **Ajustes de Apariencia**: Añadido interruptor global para habilitar/deshabilitar el visualizador.
- 🎨 **Estabilidad de UI**: Eliminados los saltos visuales al iniciar/pausar la música mediante placeholders.
- 📈 **Sensibilidad Optimizada**: Mejora en la respuesta de las barras a volúmenes bajos y frecuencias altas.
- 🛠️ Solucionado error de firma JVM (clash) en PlayerViewModel.
- 🔧 Refactorización de MediaController para mejor compatibilidad con Kotlin State.
- 📦 Mejora en la estabilidad interna del reproductor.

## [1.4] - 2026-04-20
### Estable
- ✨ Nuevo sistema de personalización visual (bordes, fuentes).
- 🎨 Soporte para desenfoque dinámico en fondo.
- 🎧 Implementación de Crossfade para transiciones suaves.
- ⏰ Temporizador de apagado automático.
- 📱 Opción de mantener pantalla siempre activa.
- 🚀 Optimización de menús de Artistas y Álbumes.
- ⚡ Mejoras significativas en rendimiento y nuevas animaciones.

## [1.3] - 2026-04-17
### Anterior
- 📊 Panel de estadísticas y contador de biblioteca.
- 🎚️ Ecualizador Pro con control de graves.
- 📜 Monitor Logcat integrado para depuración.
- 🎵 Barra de reproducción rediseñada.

## [1.2] 2026-04-13
### Estable
- 📂 Organización por Artistas, Álbumes y Favoritos.
- 🎤 Modo Karaoke con soporte para letras sincronizadas.
- 🔄 Sistema de escaneo dinámico y fetch de letras.
- 👋 Nueva experiencia de bienvenida e inicio.

## [1.1] 2026-04-9
### Mejora
- 🌈 Soporte inicial para Colores Dinámicos (Material You).
- ⚙️ Primer menú de ajustes estructurado.
- 🎮 Controles de reproducción expandidos.

## [1.0] 2026-03-28
### Lanzamiento
- 🚀 Versión inicial del reproductor.
- 📂 Soporte para archivos de audio locales.
- ✨ Interfaz básica con controles esenciales.
