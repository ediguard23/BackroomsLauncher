# Prompt: Launcher + mod de menú para el evento Backrooms

Copia todo lo de abajo en una sesión nueva.

---

Vamos a crear desde cero **Backrooms Launcher**, un launcher cerrado para un evento de Minecraft ambientado en backrooms. Es un proyecto NUEVO y APARTE en `C:\Users\ediyt\OneDrive\Desktop\proyectos claude\backrooms-launcher\`, con su propio repo git. Se construye desde cero: **no uses ni copies nada de `novacraft-launcher/` ni de `novacraft-mod/`** por ahora. Stack del launcher: Electron + `minecraft-launcher-core` + `electron-updater`, igual que cualquier launcher moderno de Windows.

## Idea

Yo, el organizador, decido qué mods y qué configuración tiene el evento. El jugador no decide nada: abre el launcher, escribe su nick o inicia sesión premium, pulsa JUGAR y entra directo al servidor del evento.

## 1. El launcher (Electron)

Una sola pantalla, sin menús laterales:
- Campo de **nick** (3–16 caracteres, `A-Za-z0-9_`, validado) y un interruptor **«Tengo cuenta premium»**. Con el interruptor se abre el login de Microsoft (con sesión recordada y refresco automático del token); si es premium, el nick pasa a ser el real y se bloquea. Sin premium se usa el nick libre en modo offline. Recuerda el último nick.
- Botón **JUGAR**, barra de progreso con estado en texto («Verificando archivos», «Descargando mods»...).
- Panel de evento alimentado por el manifest remoto: noticias/anuncios que yo edito, **cuenta atrás** hasta `eventStart` (JUGAR bloqueado antes de esa hora si el campo existe), y estado del servidor (ping Minecraft SLP: online/offline y jugadores).
- Único ajuste visible: RAM (con un valor por defecto sensato según la memoria del equipo). Nada de elegir versión, loader ni carpetas.
- **Sin vista de mods**: no hay pestaña de mods, no hay botón «abrir carpeta», no se listan los archivos en ningún sitio.
- Estética backrooms: amarillo moho / fluorescente, zumbido, parpadeo de tubo, ruido VHS, viñeta, fondo de pasillo con movimiento lento. **Música y sonidos ambiente** que yo te pasaré en `assets/sounds/` (con botón de silencio y volumen); mientras no estén, deja huecos con archivos de relleno.
- Java automático (descarga Temurin de la versión que pida Minecraft), instalación del loader y autoactualización del propio launcher con electron-updater desde GitHub Releases (repo `ediguard23/<nombre>`, público). Instalador NSIS sin espacios en el nombre del artefacto.

## 2. El pack remoto (lo que controlo yo)

- `manifest.json` publicado en GitHub Releases: `{ packVersion, minecraft, loader, loaderVersion, server: {host, port}, links: {discord, tienda}, eventStart, news[], files: [{path, sha1, size, url}] }`.
- Al abrir y **antes de cada arranque** el launcher sincroniza: descarga lo que falta o cambió, verifica SHA1 y **borra todo lo que no esté en el manifest** dentro de `mods/`, `config/`, `resourcepacks/`, `shaderpacks/`. Así nadie puede quitar, añadir ni modificar mods entre partidas.
- Los jars se guardan en el disco con **nombres ofuscados** (hash), no con el nombre original, para que el jugador no vea qué mods son con un vistazo a la carpeta. La carpeta del juego vive en una ruta propia (`%APPDATA%\.backrooms-event`), aislada de cualquier `.minecraft`.
- Hazme un script `tools/publicar-pack.js` que recorra una carpeta `pack/` mía, calcule hashes, genere el manifest y suba los archivos a la Release. Yo solo copio mods ahí y ejecuto el script.
- Sé honesto en el README sobre los límites: esto frena al jugador curioso, no a alguien decidido con un descompilador. Menciónalo y no lo vendas como infalible.

## 3. El mod de menú (Fabric, propio, dentro de `pack/`)

Recomendación y decisión tomada: **escribir un mod propio** en vez de depender de FancyMenu, porque necesito *bloquear* pantallas (no solo ocultar botones) y controlar todo el flujo. Si ves un motivo técnico fuerte para usar FancyMenu como base, dímelo antes de empezar.

- **Pantalla de título custom** con exactamente cinco botones: **JUGAR**, **CONFIGURACIÓN**, **SALIR**, **DISCORD** y **TIENDA**. Sin Un jugador, sin Multijugador, sin Realms, sin accesibilidad ni idioma sueltos. DISCORD y TIENDA abren en el navegador las URLs que da el manifest (`links.discord`, `links.tienda`), con aviso previo de «vas a salir del juego»; si una URL está vacía, el botón se ve pero desactivado. CONFIGURACIÓN abre las opciones de Minecraft con el aspecto del evento (sonido, vídeo, controles, y los efectos del menú). Todos los botones dibujados a medida (texturas 9-slice generadas con Higgsfield o dibujadas por código), con hover animado, zumbido de tubo fluorescente y sonido de clic.
- **JUGAR** se conecta solo a la IP fija que el launcher le pasa (`server.host:port` del manifest; el mod la lee de un archivo/propiedad que escribe el launcher, nunca de una pantalla). Si el servidor está caído, mensaje en estilo backrooms y reintento.
- **Bloqueo**: las pantallas de Multijugador, Un jugador, Realms y «añadir servidor» deben ser inalcanzables (incluso por atajos). Al desconectarse o ser expulsado, vuelve a la pantalla custom, no a la lista de servidores. Menú de pausa en partida también con aspecto del evento y sin «abrir a LAN».
- **Fondo**: imagen de fondo a pantalla completa con efectos en tiempo real: zoom/parallax lento, parpadeo de luz, ruido de película, viñeta, aberración cromática leve. Los efectos se pueden apagar en Opciones para equipos flojos.
- **Música**: la del menú, cargada desde un resourcepack incluido en el pack (los sonidos que yo te pase), con fundido al entrar a la partida.
- **Sin pantalla de mods**: no incluyas Mod Menu ni nada que liste mods. Revisa y oculta lo que delate la lista (por ejemplo la marca del loader en F3 si se puede cambiar de forma limpia).
- Versión de Minecraft y loader: **provisional 1.21.x + Fabric**. Puede cambiar (incluso a una versión antigua) si los mods de backrooms que elija lo exigen. Por eso la versión y el loader viven solo en el manifest y en un `gradle.properties`, y el mod de menú debe compilarse por versión sin reescribir la lógica (separa la parte independiente de versión de la capa fina que toca clases de Minecraft). Te daré la lista de mods; antes de compilar dime qué versión implica.

## 4. Imágenes (Higgsfield ya está conectado)

Usa el conector de Higgsfield de la sesión (carga sus herramientas con ToolSearch; `models_explore` para elegir modelo, `generate_image`, `upscale_image`, `remove_background`). La cuenta es gratuita y tiene pocos créditos, así que **antes de generar nada consulta `balance` y dime el coste estimado**; no gastes créditos sin mi visto bueno. Genera:
- Fondo del menú (16:9, pasillo de backrooms: moqueta húmeda, papel pintado amarillo, tubos fluorescentes, sin gente, sin texto) y el fondo del launcher (puede ser el mismo recortado).
- Texturas de los cinco botones (normal / hover / pulsado / desactivado), estilo papel pintado amarillo viejo con borde sucio, coherentes entre sí, con fondo transparente (`remove_background`).
- Logo/título del evento si te lo pido.
Guarda los resultados en `assets/` con nombres claros y apunta el prompt usado de cada uno en `assets/PROMPTS.md`. Mientras no estén, trabaja con placeholders procedurales para no quedarte bloqueado.

## 5. Requisitos de servidor que debes tener en cuenta

El servidor mezcla premium y no premium, así que va en offline-mode. Sin un plugin de login, cualquiera puede entrar con el nick de otro. Avísame en la entrega de qué necesita el servidor (por ejemplo AuthLogin) para que la unión premium/offline sea segura.

## Cómo quiero que trabajes

1. Empieza comprobando el entorno (Node, JDK, Gradle) y que Higgsfield responde (`balance`).
2. Haz el plan por fases (launcher base → pack/manifest/sincronización → mod de menú → estética y sonido → publicación) y espera mi visto bueno antes de la primera fase.
3. Prueba de verdad: arranca el launcher, sincroniza un pack de prueba, lanza el juego y comprueba que entra al servidor y que una carpeta manipulada se repara sola. Dime lo que no pudiste probar.
4. Commits y push los haces tú.
