# Backrooms — contexto del proyecto (léelo entero antes de tocar nada)

Evento de Minecraft de **PeakMC Studio** ambientado en las Backrooms (Nivel 0). Hasta
~200 jugadores bajan al Nivel 0, cumplen misiones y van pasando fases cada vez más
duras; solo escapan unos pocos. Este repo tiene las dos piezas del jugador:

- **El launcher** (Electron, raíz del repo): nick o cuenta premium, JUGAR y dentro.
  Instala Minecraft 1.21.11 + Fabric, sincroniza el pack del evento y canjea la entrada.
- **El mod `backrooms_evento`** (Fabric 1.21.11, carpeta `evento/`): va en el cliente Y en el
  servidor. Es el evento entero: el mundo, las entidades, la luz, las misiones, el HUD,
  el menú, la pantalla de carga, el vestíbulo, la protección de entradas...

La venta de entradas vive en OTRO proyecto (la tienda de PeakMC, `peakmc-store`, privado,
Node + Supabase + Tebex). Aquí solo hace falta saber cómo funciona (ver `VENTA.md`).

Todo se escribe en **español**: código, comentarios, textos del juego, commits y respuestas
al organizador.

## Lee primero, en este orden

1. **`ESTADO.md`**: en qué se quedó la última sesión, qué está probado, qué falta y qué
   necesita permiso. Es el traspaso entre sesiones: actualízalo al acabar y súbelo.
2. `VENTA.md`: el modelo de venta (se vende la entrada, no el launcher) y la protección.
3. `README.md`: launcher, pack, publicación.
4. `servidor-ejemplo/LEEME.md`: qué lleva el servidor del evento.

## Mapa del repo

```
main.js, preload.js, src/        launcher (src/core: lógica; src/ui: interfaz)
src/core/acceso.js               canje del código de entrada y pase en disco
evento/                          el mod (Gradle, Loom 1.18, mappings de Mojang)
  src/main/java/net/backrooms/
    menu/                        menú principal, pausa, carga, inventario, muerte, TAB...
    evento/
      mundo/                     Plano.java (trazado procedural) y GeneradorNivel0
      vestibulo/                 el "Centro de Expediciones" (PlanoVestibulo = cajas de bloques)
      fase/                      las 4 fases y los ascensores de salida
      expedicion/                el /start (cinemática y viaje escalonado)
      mision/                    misiones, casetes, grabación con la cámara
      ambiente/                  apagones y alarmas
      entidad/                   Bacteria, Smiler y quién aparece dónde (Acechadores)
      supervivencia/             cordura, estamina, comida, agua de almendras
      objetos/                   traje, linterna, cámara, comida
      acceso/                    comprobación del pase de entrada en el login
      red/                       paquetes servidor <-> cliente
      cliente/                   todo lo de cliente: efectos (shader de apagón/miedo),
                                 entidad (modelos), cinematica, HUD, sonidos...
  src/main/resources/assets/     texturas, modelos, sonidos, shaders (casi todo GENERADO)
tools/
  texturas/*.js                  dibujan las texturas y modelos (npm run texturas)
  texturas/entidades.js          modelos de Bacteria y Smiler -> genera Malla*.java (no editarlos a mano)
  sonidos/*.js                   sintetizan TODOS los sonidos (sin muestras de terceros)
  sonidos/voces/                 las únicas grabaciones: voces generadas con Higgsfield
  video/                         tráiler 4K (npm run trailer)
  cartel/                        cartel animado para Yamipa (GIF 7x4 en colores de mapa)
  publicar-pack.js               publica el pack (GitHub Release "pack")
  reiniciar-prueba.sh            reinicia el entorno de pruebas
  acceso/tienda-prueba.js        tienda falsa para probar las entradas en local
pack-ejemplo/                    cómo es pack/ (pack/ no está en el repo)
servidor-ejemplo/                configuración del servidor del evento
```

## Compilar y probar

- Node 20+ (`npm install` en la raíz). Launcher en modo desarrollo: `npm start`.
- Mod: **Java 25 para Gradle** (Loom 1.18 lo exige), aunque el mod compila a Java 21:
  `cd evento && JAVA_HOME=<ruta-jdk-25> ./gradlew build` → `evento/build/libs/`.
  Si falla con «Failed to clean up stale outputs» o «Cannot snapshot» (OneDrive), borra
  `evento/build/resources` y repite.
- Fuentes de Minecraft para consultar: `./gradlew genSources` y descomprime el
  `*-sources.jar` de `evento/.gradle/loom-cache/minecraftMaven`.
- Entorno de pruebas: un servidor Fabric 1.21.11 local (puerto 25566, RCON 25576) con lo
  de `servidor-ejemplo/` y el jar del mod. `tools/reiniciar-prueba.sh` copia el último jar,
  reinicia servidor + pack local + launcher y entra solo. Las rutas se cambian con las
  variables `SERVIDOR`, `JAVA_SERVIDOR` y `GUION_JUGAR` (por defecto `tools/pruebas/pulsar-jugar.js`;
  `pulsar-jugar-codigo.js` además canjea un código de prueba). Para las entradas en local:
  `node tools/acceso/tienda-prueba.js <carpeta-del-servidor>`.
- Probar sin tocar la ventana del juego (solo con `BACKROOMS_PRUEBAS=1`): escribir en
  `%TEMP%/backrooms-ordenes.txt` una orden: `inventario | pausa | cerrar | cinematica |
  linterna | camara | tab [n] | andar [s] | correr [s] | captura <nombre> | cursor`. `captura` guarda
  la imagen en `%APPDATA%/.backrooms-event/screenshots` (vale aunque la ventana esté a
  pantalla completa). El cursor del sistema no sale en las capturas: `cursor` escribe en el
  log si se ve, está oculto o capturado. `node tools/rcon.js "<comando>"` manda comandos al servidor.
- Comandos de staff: `/start`, `/backrooms ...` (buscar, cinematica, apagon, alarma, luz,
  ambiente, fase, vestibulo, muerte, escapados, invocar bacteria|smiler, acechadores,
  cordura, comida reponer, misiones...). Ver `Comandos.java`. La lista del evento (quién puede
  entrar): `/brwhitelist agregar|quitar|ver|lista|recargar` (`acceso/Whitelist.java`; la lista vive
  en la tienda, ver `VENTA.md`).

## Reglas (del organizador; no se negocian)

- **Nada de assets de terceros sin licencia.** El pack «EC-Backrooms» de EliteCreatures
  (sacado de NullForums) NO se usa ni se convierte: ni modelos, ni texturas, ni sonidos.
  Las texturas de Char1ie son All Rights Reserved: no van en el mod de pago sin permiso.
  Los modelos, texturas y sonidos del evento se hacen por código en `tools/`.
- Las texturas de Minecraft no se suben al repo: se copian en tiempo de ejecución
  (AtlasCinematica) o al hacer el tráiler.
- Mods de otros (Simple Voice Chat) no se resuben: van en `externos` de la ficha del pack
  con la URL oficial y su sha1.
- **Cliente y servidor llevan exactamente el mismo jar.** Si cambian datos (bloques,
  objetos, sonidos, dimensiones...), sube `mod_version` en `evento/gradle.properties`.
- **El repo es público**: ni tokens, ni contraseñas, ni la clave de firma de las entradas
  (`BACKROOMS_FIRMA` solo está en el `.env` de la tienda y en Render). La clave PÚBLICA sí
  puede ir en el mod.
- Pedir permiso antes de: desplegar la tienda o tocar su base de datos (producción),
  publicar el pack (`npm run publicar-pack`), publicar una versión del launcher, gastar
  créditos de Higgsfield, aceptar un EULA.
- No publicar el pack hasta que la tienda tenga las entradas desplegadas: el pack ya pide
  entrada y sin tienda nadie podría entrar.
- El organizador a veces juega en el cliente de pruebas: avisa antes de teletransportarle
  o matarle. Los procesos se matan por PID o por el título de ventana «BACKROOMS», nunca
  todos los `java`/`javaw`.

## Trampas conocidas (Minecraft 1.21.11 / Fabric)

- A menos de 32 bloques del fondo del mundo la niebla se vuelve negra: por eso el suelo
  del Nivel 0 está en y=32 (techo en y=37, pasillos de 4 de alto).
- Sin cielo, el buscador de spawn tira al vacío (PlayerSpawnFinderMixin).
- Las caras de abajo salen al 50 %: los tubos usan `shade:false` + `light_emission`.
- Un `ArmorMaterial` con enchantmentValue 0 revienta al arrancar.
- `DataComponents` está en `net.minecraft.core.component`.
- La GUI de 1.21.11 no deja uniforms propios: el tiempo va en los UV del vértice.
- La pasada de oscuridad (EfectosMundo) va antes de la mano; la de la mano, al FINAL de
  `renderLevel` (la mano se encola y se dibuja después).
- La velocidad de un mob va al cuadrado (≈43,2·v² bloques/s): `Bacteria.atributo()`.
- Los textos y objetos flotantes (display entities) con giro 0 miran al sur; las paredes
  ocupan su bloque entero, así que un rótulo va 0,05 por delante de la cara.
- Minecraft solo sitúa en 3D los sonidos MONO; los estéreo suenan "dentro de la cabeza".
- `tools/sonidos` y `tools/texturas` sobrescriben archivos generados: cambia el script, no
  el resultado.

## Cómo trabajar en equipo (dos personas con Claude)

- Al empezar: `git pull` y leer `ESTADO.md`. Al acabar: actualizar `ESTADO.md` (qué
  hiciste, qué probaste de verdad, qué falta) y subirlo.
- Commits pequeños y con mensaje en español que diga qué cambia. Si dos tocáis el mod a
  la vez, cuidado con `mod_version`: sube la versión al final, justo antes del commit.
- Lo que no se ha probado en el juego se dice así en `ESTADO.md` («sin probar»).
