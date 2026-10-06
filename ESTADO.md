# Estado del proyecto (para la siguiente sesión)

Última actualización: 2026-10-05 21:50 (hora de Honduras). Mod `backrooms_evento` 0.9.4, launcher 1.0.0
(con arreglos sin publicar).

## Tienda: apartado Eventos y lista del evento (2026-10-05, noche; xgdier_)

Lo pidió el organizador: la entrada se vende HOY pero el launcher aún no se publica, y hay
que saber quién compra. Todo en peakmc-store (commit `d689b62`, **sin desplegar**) y en
el mod. Ver `VENTA.md` (reescrito) para el modelo completo.

- **Tienda**: página `/eventos` (enlace en el menú y aviso en la portada) con la entrada,
  plazas, fecha y preguntas. La entrada NO sale en el catálogo ni en destacados (lo pidió
  así). Una entrada por nick, solo Java, plazas limitadas (100). **El código solo se canjea
  con el nick que compró.** El enlace del launcher solo lo ve quien tiene entrada.
- **La lista del evento** = nicks con entrada válida en la base de la tienda (se guarda
  desde la primera venta). Panel → Eventos: verla, CSV, añadir nicks (regalos/staff),
  anular, reiniciar canje, ajustes (plazas, fecha, enlace del launcher, venta abierta).
- **Mod 0.9.4**: `acceso/Whitelist.java`. El servidor descarga la lista cada minuto con
  el token (`token` y `tienda` en `config/backrooms-acceso.json`); al entrar, pase válido
  y nick en la lista; a quien sale de la lista se le echa (máximo 5 de golpe, por si la
  tienda falla). Comando `/brwhitelist agregar|quitar|ver|lista|recargar` (OP 3).
- **Probado**: en local con una base PGlite, todo el recorrido de la tienda (ocultar del
  catálogo, una por nick, Bedrock fuera, plazas/agotadas, código de otro nick → 403, pase
  con el bueno, API con token, reembolso → fuera de la lista) y los tests de la tienda.
  `/brwhitelist` probado por RCON en el servidor de pruebas contra la tienda local
  (agregar da el código, quitar lo anula, la lista se descarga al arrancar). Luego dejé el
  servidor de pruebas como estaba (config y jar 0.9.2). **Sin probar en el juego**: que el
  login rechace a quien no está en la lista y la expulsión (hace falta un cliente con pase).
- **Falta (organizador)**: desplegar la tienda, poner `BACKROOMS_FIRMA` y
  `BACKROOMS_SERVIDOR_TOKEN` en Render (los dos en el `.env` local de peakmc-store), crear
  el paquete en Tebex (2 $, sin comandos) y vincularlo en el panel. Ver VENTA.md.
- El texto de la ventana ENTRADA del launcher ya dice que el código solo vale con el nick de
  la compra (llega con la próxima versión). `pack/evento.json` (local, no está en git):
  `acceso.comprar` apunta ya a /eventos.


## Cursor invisible y logo que se arrastraba (2026-10-05, noche; Bruno)

Bruno (FastPaper1) probó el servidor de pruebas y lo arregló desde su PC con Claude.

- **Mod 0.9.3: el cursor ya no se queda invisible.** `PantallaCinematica.init()` oculta el
  cursor con GLFW y nadie lo devolvía: Minecraft solo lo enseña al soltar un ratón capturado
  y, con la cinemática abierta, no lo está. Si la cinemática se cortaba (una desconexión, la
  pantalla de carga al viajar al Nivel 0), el cursor seguía invisible en SEÑAL PERDIDA y en
  todos los menús (los botones se podían pulsar, pero sin ver el cursor). Ahora `removed()`
  lo devuelve; `setScreen` llama a `removed()` antes de capturar el ratón, así que al volver
  a la partida se captura como siempre. Efecto a vigilar: en la pantalla de carga del viaje
  al Nivel 0 (a los ~41 s, un momento) el cursor ahora se ve, como en cualquier pantalla de
  carga de Minecraft; antes quedaba oculto por el mismo fallo. Si molesta, se puede no
  devolverlo mientras la cinemática siga activa y la conexión abierta.
  Compila; **sin probar en el juego**. Para probarlo
  (orden de prueba nueva `cursor`): con BACKROOMS_PRUEBAS, `cinematica`, a mitad
  `node tools/rcon.js "kick <nick>"` y luego `cursor` → el log debe decir «Prueba: cursor
  visible» (con la 0.9.2 dice «oculto»). Y un `/start` entero con la ventana activa: al
  acabar, `cursor` → «capturado».
- **Launcher: ya no se arrastra el logo** (se podía coger la puerta del logo y llevarla por
  la ventana con el cursor de «prohibido»; también el logo del pie y el texto del nick).
  `dragstart` cancelado en toda la ventana (`src/ui/app.js`). Probado con una prueba
  automática (CDP, ventana oculta): ninguno de los tres inicia ya un arrastre, y el clic en
  MENU y la barra de volumen siguen funcionando. Llega a los jugadores con la próxima versión
  del launcher.
- **Lo que le pasó a Bruno en la prueba** (log de su cliente): no murió. La cinemática le
  empezó a las 19:57:01 (hora de Honduras) y a las 19:57:43, a los ~42 s (cuando el servidor
  lleva al Nivel 0), se le cortó la conexión con «Desconectado». Ese texto es el motivo
  genérico de Minecraft: la conexión se cerró sin que el servidor mandara motivo (no fue un
  kick ni un ban). Sus intentos de volver (19:58 y 20:01) no llegaron a entrar. Parece el
  fallo raro del punto 6 de Pendiente: mirar `logs/` y `crash-reports/` del servidor de
  pruebas a esa hora.

## Prueba abierta y cartel (2026-10-05, noche)

- **El pack PUBLICADO ahora es uno de prueba** (`pack/.evento-prueba.json`, pack 2026.10.05-02a371a9):
  apunta al servidor de pruebas del PC del organizador (por un túnel de playit.gg), sin
  `eventStart` y sin `acceso`, con el mod 0.9.2. Lo pidió el organizador para que un amigo
  pruebe con el launcher 1.0.0. **Antes de vender hay que volver a publicar el pack de verdad**
  (`pack/evento.json`, con `acceso` y la IP real) cuando la tienda esté desplegada.
- En ese servidor de pruebas `config/backrooms-acceso.json` está con `activo: false` (entra
  cualquier nick). El chat de voz por el túnel está pendiente: playit exige que el
  organizador verifique su correo para abrir el túnel UDP.
- **Cartel animado para Yamipa** (`npx electron tools/cartel/main.js`): GIF de 7x4 bloques
  (896x512, 60 fotogramas a 0,1 s) en la paleta exacta de los mapas, con el pasillo del
  Nivel 0 andando en bucle, un apagón y la sonrisa en la puerta del logo; además, la imagen
  fija a todo color. Salen a `video/cartel/` (no va al repo). Comprobado con ImageIO de Java
  (lo que usa Yamipa): 60 pasos, retardo de 2 ticks y todos los píxeles en la paleta. Sin
  probar todavía en un servidor con Yamipa.

## 0.9.2 (2026-10-05, tarde)

- **Probado en el juego**: la 0.9.x arranca; el miedo se ve (bordes que laten y colores
  separados con una Bacteria cazando); la pantalla de muerte del eliminado sale bien
  («HAS CAÍDO», solo «SALIR DEL EVENTO»); entradas con pase: entra el bueno y fuera un
  cliente sin mod, un pase falsificado, el de otro nick y el copiado a otro PC.
- Arreglado: en el apagón la mano salía iluminada (la pasada de la mano iba antes de que el
  juego la dibujara: ahora va al final de renderLevel). Linterna con más alcance y luz.
- **Sonidos sin nada genérico** (el usuario dijo que varios lo eran): tools/sonidos/acciones.js
  hace herido y muerte del jugador por el respirador del traje (sustituyen al «auch» de
  Minecraft con assets/minecraft/sounds.json), coger casete/agua/comida, tragos y suspiro al
  beber agua, leer nota, sentarse, zarpazo de la Bacteria, zumbador y campanilla del
  ascensor, misión completada y escapado. La eliminación lleva antes un monitor cardiaco
  que se queda plano (tools/sonidos/eliminado.js).
- Muestrario para que el usuario escuche: `video/muestrario-sonidos.mp3` + `.txt` con los
  segundos (no va al repo; se rehace con ffmpeg concatenando los ogg).
- Voces (Higgsfield, 3,5 créditos): megafonía del vestíbulo y de la alarma, y dos susurros
  para las alucinaciones (tools/sonidos/voces.js, originales en tools/sonidos/voces/).

- **Simple Voice Chat 2.6.24** en el pack: va en `externos` de la ficha (URL de Modrinth y
  sha1; el launcher lo baja de ahí, no se resube) y en el servidor. Probado: el servidor lo
  arranca en el puerto UDP 24454 y el cliente se conecta. Voz a 28 bloques y sin grupos.
  Crédito en el menú del launcher. Plantillas del servidor en `servidor-ejemplo/`.
- El fallo raro del tp vestíbulo → Nivel 0 no se repitió en 4 viajes seguidos.
- Vestíbulo regenerado: ya se ven el logo del evento y el de PeakMC en la pantalla, las
  candilejas y la sala más iluminada.
- **VENTA.md**: cómo se vende (la entrada, no el launcher) y por qué no se puede entrar sin pagar.

## En qué se quedó la sesión anterior

Se hizo la 0.9.0 (compila, arranca sin errores en cliente y servidor), pero **falta probarla
a fondo en el juego**. Lo último fue una prueba con la orden `andar 5`: el jugador avanzó 21 bloques
por la moqueta y el log no dio «unknown sound». La captura `andando.png` está en
`%APPDATA%/.backrooms-event/screenshots`, sin revisar. Falta:
- Oír las pisadas nuevas (el usuario o un bot) y ver el balanceo en movimiento.
- Probar el miedo: `/backrooms invocar bacteria` con el jugador en aventura y Resistencia
  255, y una `captura` mientras le persigue. Se deben ver bordes oscuros que laten y oír
  el corazón cada vez más rápido.
- Estado del servidor de pruebas al cortar: xgdier_ en el Nivel 0 (overworld, ~3 33 25),
  ya devuelto a creativo y sin efectos. Para volver a probar, ponerle en aventura con
  `effect give xgdier_ resistance 300 255 true`.

Novedades de la 0.9.0 (todo en `evento/`):
- **Pisadas propias**: la moqueta y la moqueta mojada tienen SoundType propio (Bloques.java).
  Pisadas en `moqueta.paso` y `moqueta_mojada.paso`, 4 variantes cada una, con el
  subtítulo «Pasos». Se rompen y se ponen con los sonidos de siempre (lana / esponja).
  Se generan con `node tools/sonidos/expedicion.js moqueta`.
- **Balanceo de cámara** (cliente/Balanceo.java + mixin/cliente/BalanceoMixin.java):
  sustituye el bobView de Minecraft. Al andar es más suave que el original; al girar la vista
  se inclina; parado respira; sin aliento jadea; con la cordura baja la vista se tambalea.
  Si el jugador quita la opción «Balanceo de la vista», no hay ninguno.
- **Miedo** (cliente/efectos/Miedo.java + efectos.fsh/efectos_mano.fsh, nuevo `Estado4`
  en el UBO): con una Bacteria cazándote a menos de 32 bloques suena el corazón
  (latido.ogg), cada vez más rápido. Con cada latido la imagen se encoge un poco, los bordes
  se oscurecen y se separan los colores. Si la Bacteria aún no te ha visto, el efecto es suave.
  Mientras late, el corazón de la cordura baja se calla para que no suenen los dos.
- Órdenes de prueba nuevas: `andar [s]` y `correr [s]`.

Leer esto antes de tocar nada. Lo que se va haciendo se anota aquí y se sube.

## Cómo probar

- Servidor de pruebas: `D:\backrooms-prueba` (puerto 25566, RCON 25576 clave `pruebas-locales`).
- `bash tools/reiniciar-prueba.sh [--mundo-nuevo]` compila nada: copia el último jar de
  `evento/build/libs`, reinicia servidor + pack local + launcher y entra solo con `xgdier_`.
  Con `JVM_EXTRA="-Dlog4j.configurationFile=log4j2-depurar.xml"` el servidor saca la
  traza de los comandos que fallan («An unexpected error occurred»).
- Órdenes al cliente sin tocar la ventana (solo con BACKROOMS_PRUEBAS): escribir en
  `%TEMP%/backrooms-ordenes.txt` una de `inventario | pausa | cerrar | cinematica |
  linterna | camara | tab [n] | andar [s] | correr [s] | captura <nombre> | cursor`. `tab 200` llena el TAB con 200
  jugadores ficticios. Las capturas salen en `%APPDATA%/.backrooms-event/screenshots`.
  `cursor` escribe en el log («Prueba: cursor visible | oculto | capturado») cómo está el cursor.
- Compilar el mod: `cd evento && JAVA_HOME="$LOCALAPPDATA/Programs/jdk-25" ./gradlew build`
  (si falla por OneDrive, borrar `evento/build/resources` y repetir).
- **El usuario a veces juega en ese mismo cliente**: avisar antes de teletransportarle o matarle.

## Hecho y probado en el juego (2026-10-05)

- Apagón (oscuridad total), linterna (F), cámara (C, «NIGHT SHOT»), Smiler (flashbang si
  lo miras sin cámara, −12 % de cordura; con la cámara no pasa nada), Bacteria (mata en
  segundos si te alcanza), cordura con efectos de locura, comida, vestíbulo y butacas.
- Eliminación: quien cae en la expedición → animación «SEÑAL PERDIDA» en la pantalla de
  todos (cara, nick, quedan N), línea propia en el chat `[✖] causa · ELIMINADO · quedan N`,
  y `/ban` a los 4 s (los OP no se banean). `/backrooms perdonar` quita esos bans.
- Pantalla de muerte propia («HAS CAÍDO»); si te eliminan no ofrece reaparecer.
- TAB propio: logo, `[+] nick` (corchetes grises, + amarillo pálido, nick blanco), orden
  alfabético, se ajusta solo hasta 200+ jugadores; Discord, tienda y «Desarrollado por».
- Brazo en primera persona con el guante y la manga del traje.
- Agua de almendras: 2 por jugador al llegar a cada fase, a 40-220 bloques de su punto de
  llegada (400 con 200 jugadores). Se guarda en `<mundo>/backrooms/aguas.json`;
  `/backrooms comida reponer` la limpia entre partidas.
- Estamina ~23 s corriendo; cordura y estamina en paneles a los lados de la barra.
- Bacteria (0.8.1): persigue a 5,3 bloques/s en la fase 1 (medido en el juego; antes ~3,6,
  más lenta que andar). Un jugador corre a 5,6, así que solo se escapa corriendo y rompiendo
  la línea de vista. Ojo: el atributo de velocidad de un mob va al cuadrado (≈43,2·v² b/s);
  `Bacteria.atributo()` lo convierte, y Fase.velocidad y la alarma multiplican b/s.
  Voz nueva: rugido grave (60-120 Hz) con gruñido, golpe en el pecho, ecos de pasillo y sala
  grande (antes era una voz a ~700 Hz, «de niña»). Mientras caza jadea cada ~2 s
  (`bacteria.caza`) y pisa fuerte. Sonidos: `node tools/sonidos/expedicion.js bacteria`
  (con prefijo solo rehace esos; ya no pisa los susurros que añade voces.js).
- Acceso con entrada (pase Ed25519 de la tienda): si la config se rompe, el servidor queda
  CERRADO (antes quedaba abierto).

## Pendiente

1. **Tienda (peakmc-store): desplegar las entradas.** Está en commit local (`493e574`),
   NO desplegada. Falta aplicar `server/migrations/005_backrooms.sql` en Supabase y
   desplegar (`npm run exportar` → Render). **Pedir permiso al usuario**: es producción.
2. **No publicar el pack** (`npm run publicar-pack`) hasta que la tienda esté desplegada:
   `pack/evento.json` ya lleva `acceso.api` y sin la tienda nadie podría canjear ni entrar.
3. IP real del servidor en `pack/evento.json` (sigue `play.tuservidor.net`).
4. Pisadas, balanceo y miedo: hechos en la 0.9.0, falta probarlos en el juego (ver arriba).
5. Simple Voice Chat: HECHO. Falta que el host del servidor abra el puerto UDP 24454.
   AmbientSounds 2 no encaja (pone sonidos de bosque/agua por bioma): descartado.
6. Fallo raro: `tp` del vestíbulo al Nivel 0 dio «unexpected error» y dejó al
   cliente en «Cargando el terreno». Si vuelve, el log de depuración (ver arriba) da la
   traza. Posiblemente se repitió con Bruno el 2026-10-05 a las 19:57:43: desconexión sin
   motivo justo al viajar al Nivel 0 (ver arriba).
7. Probar la pantalla de muerte en su variante «eliminado» con captura (la lógica está;
   la animación y el ban ya se vieron funcionar).
8. Que el usuario escuche el rugido nuevo de la Bacteria y diga si le convence
   (`/backrooms invocar bacteria` en supervivencia).
9. Mod 0.9.3: probar en el juego el arreglo del cursor (ver arriba) y poner ese jar en el
   servidor de pruebas y en el pack (el mismo jar en los dos).
10. Launcher: el arreglo del arrastre necesita una versión nueva (subir `version` en
    `package.json` y `npm run release`; pedir permiso antes).

## Avisos

- La lista `staff` de `config/backrooms-acceso.json` del servidor deja entrar sin pase por
  el nick: con offline-mode cualquiera podría ponerse ese nick. Dejarla vacía.
- Los textos que se ven en el juego van en español; el log del servidor los saca en inglés
  porque el servidor no tiene el idioma (en el cliente salen bien).
