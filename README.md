# Backrooms Launcher

Launcher cerrado para el evento Backrooms, desarrollado por **PeakMC Studio**.
El jugador solo pone su nick (o inicia sesión premium) y pulsa **JUGAR**: los
mods, la configuración y el servidor los decide el organizador.

**Descarga:** [última versión del instalador](https://github.com/ediguard23/BackroomsLauncher/releases/latest).
Se actualiza solo.

## La interfaz

- Arranca como una cinta VHS: ▶ PLAY y la firma «PeakMC Studio presenta»
  (un clic o una tecla la saltan).
- Pasillo del Nivel 0 en tiempo real (de vez en cuando la cinta salta a las
  Piscinas, el próximo evento, con su rótulo de avance), el logo del evento con glitch de cinta,
  pase de explorador (nick o premium), JUGAR, cuenta atrás, estado del
  servidor, noticias y «Desarrollado por PeakMC Studio» en el pie.
- MENU: memoria, volumen, efectos, ventana o pantalla completa para el juego y
  **REPARAR** (el siguiente JUGAR vuelve a comprobar cada archivo por su hash).
- Escribe `noclip` fuera del campo del nick.

## Qué hace al pulsar JUGAR

1. Comprueba la cuenta: nick libre (offline) o Microsoft, renovando el token
   si caduca en menos de 10 minutos.
2. Descarga el `manifest.json` del evento (si no hay red, usa el último).
3. Instala Minecraft y el loader leyendo los json oficiales y verifica cada
   archivo por SHA1. No usa `minecraft-launcher-core`: nunca volvía a bajar
   una librería corrupta y su detección de natives falla con la numeración
   26.x.
4. Descarga el Java que pide esa versión (Temurin, comprobado por SHA256).
5. Sincroniza el pack: repone lo que falte o haya cambiado y **borra todo lo
   que no esté en el manifest** dentro de `mods/`, `resourcepacks/` y
   `shaderpacks/`. Los mods se guardan con nombre ofuscado.
6. Escribe `config/backrooms-event.json` (servidor, enlaces) para el mod del
   menú y abre el juego.

La carpeta del juego es `%APPDATA%\.backrooms-event`, aislada de `.minecraft`.

## Probar en local

```bash
npm install
npm run probar
```

Sirve `pack/` en `http://127.0.0.1:8765/` y abre el launcher apuntando ahí.
La primera vez descarga Minecraft entero (~600 MB).

## El pack (lo que controla el organizador)

`pack/` no se sube al repositorio. Copia `pack-ejemplo/evento.json` a
`pack/evento.json` y mete los archivos:

| Carpeta en `pack/` | Qué pasa en el equipo del jugador |
|---|---|
| `mods/*.jar` | Se imponen, con nombre ofuscado (`mods/<hash>.jar`) |
| `config/`, `resourcepacks/`, `shaderpacks/`, ... | Se imponen tal cual |
| `una-vez/` | Se copian solo si no existen (p. ej. `options.txt`): el jugador conserva sus ajustes |

Campos de `evento.json`: `name`, `level` (nivel de las Backrooms del evento: `"0"` por defecto, `"37"` para las Piscinas; cambia el fondo, los colores y los sonidos de los menús del juego), `minecraft`, `loader` (`fabric`, `quilt` o
`vanilla`), `loaderVersion`, `server` (`host`, `port`), `links` (`discord`,
`tienda`), `eventStart` (fecha ISO; antes de esa hora solo entra el `staff`),
`staff` (nicks), `ram.recommended` (MB), `news` (`date`, `title`, `text`) y
`strict` (carpetas que se limpian; por defecto `mods`, `resourcepacks`,
`shaderpacks`).

Publicar para los jugadores (necesita `GH_TOKEN` con permiso `repo`):

```bash
npm run publicar-pack
```

Sube a la release `pack` de GitHub solo los archivos nuevos (cada uno con su
SHA1 como nombre) y reemplaza el `manifest.json`. Los jugadores reciben el
cambio la próxima vez que pulsan JUGAR, sin reinstalar nada. Con `--limpiar`
borra de la release lo que ya no se usa.

## El menú dentro de Minecraft (`mod/`)

Mod de Fabric propio (`backrooms-menu`, solo cliente) que va dentro del pack en
`pack/mods/`. Sustituye el menú de inicio por uno con el mismo diseño que el
launcher, sobre el pasillo del nivel del evento dibujado en tiempo real con un
shader propio (`assets/backrooms/shaders`): el **Nivel 0** por defecto, el
mismo que el fondo del launcher, con sus sonidos. **Las Piscinas** (Nivel 37)
quedan listas para el próximo evento: basta `"level": "37"` en el pack.

- Botones: JUGAR (entra directo al servidor del manifest, aceptando su resource
  pack), CONFIGURACIÓN, SALIR, DISCORD y TIENDA (con la confirmación de enlace
  de Minecraft).
- Un jugador, multijugador, Realms y la lista de servidores no se pueden abrir:
  cualquier camino hacia ellos acaba en este menú (`MinecraftMixin`).
- Si el servidor echa al jugador, sale «SEÑAL PERDIDA» con el motivo,
  REINTENTAR y VOLVER, en vez de la pantalla de Minecraft.
- La pantalla de carga roja de Mojang Studios es una cinta VHS: logo del evento
  con su tubo parpadeando, barra de carga del launcher, consejos de explorador
  y «Desarrollado por PeakMC Studio». Como en la primera carga no hay fuentes
  todavía, las letras son de píxeles dibujados a mano (`render/Osd.java`) y los
  logos se leen del jar (`render/Logos.java`).
- La ventana se llama como el evento (no «Minecraft* 1.21.11») y lleva de icono
  la puerta del logo. El F3 no enseña la marca «fabric».
- Menú de pausa propio: VOLVER, CONFIGURACIÓN, LOGROS, ESTADÍSTICAS, DISCORD,
  TIENDA y DESCONECTAR, sin «abrir a LAN» ni enlaces de Mojang.
- Todos los menús (opciones, conectando...) tienen el pasillo de fondo.
- Sonidos de cada nivel en vez de la música de menú: los del launcher en el
  Nivel 0 y los de las Piscinas (`npm run sonidos:piscinas`: depuradora, agua,
  gotas y una música en Sol lidio) en el Nivel 37.
- Lee servidor, enlaces, noticias y cuenta atrás de
  `config/backrooms-event.json`, que escribe el launcher antes de abrir el juego.

Compilar (Loom 1.18 necesita Java 25 para ejecutar Gradle; el mod se compila
para Java 21):

```bash
cd mod
JAVA_HOME="$LOCALAPPDATA/Programs/jdk-25" ./gradlew build
```

El jar sale en `mod/build/libs/` y se copia a `pack/mods/`. Si Gradle falla con
«Failed to clean up stale outputs», es OneDrive bloqueando `build/`: se repite
y pasa.

## Imágenes

Los dos logos (Backrooms y PeakMC Studio) se generaron con IA sobre fondo
magenta; los originales y los prompts están en `assets/fuentes-ia/` y
`assets/PROMPTS.md`. Todo lo demás sale de ellos:

```bash
npm run imagenes
```

Quita el magenta, recorta, y escribe los logos del launcher, el icono
(`assets/icon.png`, que electron-builder convierte en `.ico`), los iconos de la
ventana de Minecraft y las texturas del mod en varios tamaños (Minecraft no
hace mipmaps de las texturas de la interfaz).

## Sonidos

`assets/sounds/*.ogg` salen de `npm run sonidos`
([tools/sonidos/generar.js](tools/sonidos/generar.js)): síntesis propia, sin
muestras de terceros. Si pones tus propios `ambiente`, `musica`, `hover`,
`click`, `parpadeo` o `jugar` (`.ogg`, `.mp3` o `.wav`) en esa carpeta, el
launcher usa los tuyos.

## Límites (para no venderlo como lo que no es)

- La protección de mods frena al jugador curioso, no a alguien decidido: los
  archivos siguen en su disco y la lista de mods aparece en
  `logs/latest.log`. Lo que sí garantiza es que en cada arranque el pack
  vuelve a ser exactamente el publicado.
- Premium y no premium juntos obligan a poner el servidor en
  `online-mode=false`. Sin un plugin de login (AuthLogin o similar)
  cualquiera puede entrar con el nick de otro.

## Compilar el instalador

```bash
npm run build
```

Deja en `dist/` el instalador NSIS (`Backrooms-Launcher-Setup-<versión>.exe`).
No está firmado: la primera vez Windows SmartScreen avisa («Más información →
Ejecutar de todas formas»).

## Publicar una versión nueva del launcher

1. Sube `version` en `package.json`.
2. Con un `GH_TOKEN` con permiso `repo`:

   ```bash
   npm run release
   ```

   Compila y crea la release `v<versión>` con el instalador, su `.blockmap` y
   `latest.yml`. Los launchers ya instalados la descargan solos al abrirse y
   la instalan al cerrar (o al pulsar el aviso «ACTUALIZACIÓN · REINICIAR»).

El pack del evento va aparte (`npm run publicar-pack`): cambiar mods, noticias o
servidor no necesita una versión nueva del launcher.
