# Backrooms Launcher

Launcher cerrado para el evento Backrooms. El jugador solo pone su nick (o
inicia sesión premium) y pulsa **JUGAR**: los mods, la configuración y el
servidor los decide el organizador.

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

Campos de `evento.json`: `name`, `minecraft`, `loader` (`fabric`, `quilt` o
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
launcher, pero en otro nivel: **las Piscinas** (Nivel 37), un pasillo inundado
que se dibuja en tiempo real con un shader propio (`assets/backrooms/shaders`).

- Botones: JUGAR (entra directo al servidor del manifest, aceptando su resource
  pack), CONFIGURACIÓN, SALIR, DISCORD y TIENDA (con la confirmación de enlace
  de Minecraft).
- Un jugador, multijugador, Realms y la lista de servidores no se pueden abrir:
  cualquier camino hacia ellos acaba en este menú (`MinecraftMixin`).
- Si el servidor echa al jugador, sale «SEÑAL PERDIDA» con el motivo,
  REINTENTAR y VOLVER, en vez de la pantalla de Minecraft.
- Todos los menús (opciones, conectando...) tienen el pasillo de fondo.
- Sonidos propios de las Piscinas (`npm run sonidos:piscinas`): depuradora,
  agua, gotas y una música en Sol lidio. Sustituyen a la música de menú.
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

Falta `assets/icon.png` (se generará con el resto de imágenes).
