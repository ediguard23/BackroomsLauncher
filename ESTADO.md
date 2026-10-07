# Estado del proyecto (para la siguiente sesión)

Última actualización: 2026-10-06 17:10 (hora de Honduras). Mod `backrooms_evento` 0.9.7, launcher 1.0.0
(con arreglos sin publicar) y un launcher aparte para la beta.

## Lo que salió en la beta #1 (2026-10-06, noche; mod 0.9.8)

Jugaron ~9 personas en el hosting. Arreglado y probado en el servidor de pruebas:
- **Grabar no contaba**: las misiones iban en orden y la primera era siempre la de los casetes,
  así que grabar a la Bacteria o a un Smiler antes no servía. Ahora cuentan en cualquier orden
  (al completar una se pone delante: [0, actual) son las hechas, el cliente no cambia). Probado:
  "Graba la Bacteria en alarma" hecha con los casetes pendientes.
- **Casetes casi imposibles**: se reparten el doble de los necesarios (vale cualquiera), en anillos
  de ~35 a ~330 bloques; radar +30 % (90/75/60/48). Estado.necesarios (las partidas viejas, todos).
- **Agua de almendras escasa**: 5 por jugador (antes 2), a 25-175 bloques.
- **Cortes de conexión**: dos veces se cayó todo el mundo a la vez con "Timed out". El servidor no
  se colgó (sin "Can't keep up" y apuntando las desconexiones al momento) y también se cortó el chat
  de voz (UDP, aparte): fue la red del hosting. AguanteConexionMixin (ReadTimeout 30 → 90 s) y
  KeepAliveMixin (15 → 45 s) hacen que un corte así sea un tirón: probado congelando el cliente 25 s
  (antes a los 15 s te echaba). Recomendado: el chat de voz en otro puerto (algunos antiDDoS cortan
  el UDP del puerto del juego) y preguntar al hosting.
- La barra de localizador (gamerule locator_bar) se apaga sola al arrancar.
- Pack de la beta con la 0.9.8 y `Downloads/Backrooms-servidor-beta1-0.9.8.zip` (con pregenerar en
  el LEEME y `pause-when-empty-seconds=-1`).

## Beta #1 (2026-10-06, tarde; xgdier_)

Unas 10 personas en el hosting **66.51.99.93:25704**. Todo hecho y probado salvo lo marcado.

- **Launcher de la beta** (NO se publica): `node tools/construir-beta.js --nombre="BETA #1"` →
  `dist-beta/Backrooms-Beta-Setup-1.0.0.exe`. Es el de siempre con otra ficha: carpeta
  `.backrooms-beta`, baja el pack de la release **`pack-beta`**, no se autoactualiza
  (`"actualizar": false` en src/event.json) y se instala como «Backrooms Beta». Probado: abre
  con «BETA #1», la noticia de la beta y la puerta abierta.
- **Pack de la beta**: `pack/.evento-beta.json` (servidor del hosting, sin eventStart, con acceso)
  publicado con `GH_TOKEN=… node tools/publicar-pack.js --tag=pack-beta --ficha=pack/.evento-beta.json`
  (el token sale de `git credential fill`). El pack público (`pack`) NO se ha tocado: sigue
  apuntando a playit. Si cambia el mod durante la beta: compilar, copiar el jar a `pack/mods`,
  volver a publicar `pack-beta` y cambiar el jar del servidor (el mismo).
- **Mods de optimización** (externos de Modrinth, no se resuben): Sodium 0.8.15-beta.1, Lithium,
  FerriteCore, ImmediatelyFast, EntityCulling, Dynamic FPS, BadOptimizations y Krypton. Con
  Sodium se ven bien apagón, linterna, alarma, tubos teñidos y Bacteria. Dynamic FPS va con
  `pack/una-vez/config/dynamic_fps.json`: solo baja FPS con la ventana minimizada y nunca el
  volumen (por defecto ponía 1 FPS y 25 % de volumen al cambiar de ventana). OJO al probar:
  con la ventana minimizada no dibuja y las `captura` salen congeladas (en el cliente de
  pruebas está a 15 FPS minimizado). Descartados: MoreCulling (pide Cloth Config), ModernFix
  (no hay para 1.21.11), C2ME/VMP/ScalableLux (alfa). AmbientSounds: NO (pájaros en el Nivel 0).
- **Servidor**: `Downloads/Backrooms-servidor-beta1-0.9.6.zip` con Lithium, FerriteCore y Krypton,
  `server-port=25704`, voz por el mismo puerto (`port=-1`, UDP), acceso ACTIVO con el token
  y la clave de la tienda real. LEEME-HOSTING.txt reescrito. Sin eula.
- **Códigos de beta** (tienda desplegada, migración 008): origen `beta`, sin nick, un uso, no
  ocupan plaza ni salen en Discord. 20 + 1 del organizador en `Downloads/codigos-beta-1.txt`
  (el suyo ya canjeado con xgdier_). Panel → Eventos: generar y «Anular todos los de beta»
  (hacerlo al acabar). También `POST /api/backrooms/beta` con el token del servidor.
- **Probado de punta a punta con la tienda real**: canje → pase firmado → servidor con acceso
  activo baja la lista (10 nicks) → entra. Ojo: el servidor de pruebas tenía en
  `clavePublica` la de la tienda falsa (por eso «pase no auténtico»); la real es la del mod.
  Después lo dejé como estaba (`activo: false`).
- **Ascensores del vestíbulo** (lo pidió el organizador): ver commit 7fa3204. Probado: /start →
  entrar en la cabina → hueco → cinemática → Nivel 0 a los 45 s; volver a entrar estando en
  el hueco te sube al vestíbulo.
- **Luz desde el /start**: probado (al llegar, apagón a los 6-8 min). El organizador se quejó de
  que al dar /start "ya era de noche": era un apagón atrasado de cuando la fase estaba vacía.
- **Tamaño del mapa según la gente** y **5 casetes**: probado (con 1 jugador cae a ±560, 5 casetes).
- **Agarre**: sin el aviso de Minecraft de bajarse (probado). Que los DEMÁS vean el agarre: el
  código lo manda a todos (pasajero, pose sincronizada, sonidos, partículas), pero **sin
  probar con dos clientes** (no había RAM con Lunar abierto). Mirarlo en la beta.
- **Megafonía del vestíbulo**: ya no dice «empieza en breve» con la expedición en marcha.
  Compila; sin oírlo en el juego.

- **Cinemáticas de bajada a las fases 2 y 3** (las pidió el organizador; mod 0.9.7): guiones 2 y 3
  de `cinematica.fsh`, en el ascensor de salida tal como es en el mundo (acero 5x5, panel al fondo).
  Fase 2: frena, te acercas al panel y lo arreglas a golpes (chispas), vuelve a fallar y cae, el
  freno lo para y se abre a medias al sector B. Fase 3: algo cruza el pasillo al cerrarse, apagón
  y visión nocturna, garras por la junta, golpes en el techo, y al abrirse una sonrisa que se te
  echa encima. Bandas: `tools/sonidos/cinematica-fases.js`. El servidor te lleva en el negro
  (Fases#bajada: 28,2 / 25,8 s) y mientras tanto ni Bacteria ni Smilers ni daño (Fases#viajando).
  A la fase 4 y a la salida sigue el fundido de antes. **Probado**: las dos cinemáticas con
  capturas y un viaje real a la fase 2 con una Bacteria al lado (no agarra; llega a los 29 s).
  Para verlas: `/backrooms cinematica fase <2|3> [nick]`; para bajar: `/backrooms bajar <nick>`.
- La del /start enseña por las puertas el vestíbulo de verdad (texturas del mod en las filas 4-5
  del atlas, AtlasCinematica admite "espacio:ruta").
- **Traje morado y negro (arreglado)**: al cambiar de mundo Minecraft para todos los sonidos e
  interrumpe el hilo de sonido; si estaba leyendo un sonido en streaming del jar, Java cierra el
  jar entero (ClosedByInterruptException) y desde ahí falla todo lo que se carga tarde (texturas
  del traje, sonidos). Ya ningún sonido del mod va con "stream" (sounds.json y los scripts).
  OJO: no volver a poner "stream": true.
- Minecraft corta la banda de la cinemática al viajar: lo de después va en `*_despues.ogg` y
  CinematicaCliente#tick lo pone al cambiar de mundo. CinematicaSonidoMixin deja pasar las bandas.
- Pack de la beta re-publicado con la 0.9.7 y `Downloads/Backrooms-servidor-beta1-0.9.7.zip` (el
  0.9.6 está obsoleto). El launcher de la beta no cambia (baja el pack solo).

## 0.9.5: escapar de la Bacteria, el agarre y la caída del servidor (2026-10-06; xgdier_)

Lo pidió el organizador: era casi imposible escapar de la Bacteria (te seguía siempre) y
mataba a golpes. **Compila; nada de esto se ha probado en el juego todavía** (el organizador
pidió no encender el servidor del PC).

- **La caída del servidor (el «fallo raro» del punto 6)**: no era el tp. El servidor se cayó
  3 veces el 2026-10-05 (17:20, 19:29 y 19:57, `crash-reports/` de D:ackrooms-prueba) con
  `NullPointerException` en `DistanceManager.removePlayer`: al sacar a un jugador de una
  dimensión (al viajar al Nivel 0 o al DESCONECTARSE) Minecraft lo busca en un chunk donde no
  está apuntado. A Bruno le salió «Desconectado» porque se cayó el servidor entero. Sin la
  causa raíz clara, `mixin/DistanceManagerMixin` lo busca donde sí está, lo quita de ahí y lo
  apunta en el log («no estaba apuntado en el chunk...»): si sale, mirar qué hacía ese jugador.
  Además `Expedicion.viajar` levanta de la butaca antes de cambiar de mundo.
- **Paredes huecas** (lo pidió así el organizador, como en Escape the Backrooms; probado en el
  juego el 2026-10-06): algunos tramos de pared (~1 de cada 6-7, `Plano#tramoHueco`) son DOBLES
  (dos bloques de grosor) y huecos por dentro de punta a punta (5x2 bloques, toda la altura). En
  uno o en los dos lados el pladur está reventado (`Plano#celdaHueca`): boquete de 2 de ancho y
  1,6 de alto, se entra AGACHADO (Mayús; Minecraft te deja agachado mientras no quepas de pie) y
  dentro se está de pie, a oscuras (los tabiques tapan la luz: `useShapeForLightOcclusion`),
  con el revés del pladur, escombros, travesaño y aislante. Dentro: «ESCONDIDO» en el HUD
  (`escondite/Escondites`, `cliente/HuecoCliente`). Bloque `escondite/Hueco` (cada cara nada,
  tabique entero o roto; estado multipart de `tools/texturas/huecos.js`). Solo en terreno NUEVO.
  Se probó antes «arrastrarse» (tumbado) y huecos de un bloque: el organizador los descartó.
  Agachado la Bacteria te ve a la mitad de distancia.
- **La Bacteria nueva** (`entidad/Bacteria`): ya no es adivina. Persigue a donde te vio por
  última vez; si no te ve en 5 s o no te encuentra allí, te deja y un rato no te «oye» correr.
  Si te metes en un hueco renuncia (gruñido), se va a 40-55 bloques en dirección contraria y
  en 30 s no vuelve a por ti. FOLLOW_RANGE 64.
- **El agarre** en vez de golpes: a 2,1 bloques te agarra, te levanta con los brazos (vas
  «montado» en ella, `positionRider`; Mayús no suelta: `PlayerMixin`), muerde 3 veces y te devora
  (~2,8 s; daño `backrooms_evento:devorado`, «X fue devorado por La Bacteria»). Si te
  desconectas mientras te come, mueres igual. La víctima ve su cara (vista clavada, temblor,
  rojo en cada mordisco, negro al final; `cliente/AgarreCliente`) y los demás la ven colgando
  (`mixin/cliente/AgarradoPoseMixin`). Las poses de `ModeloBacteria` salen de calcular con
  las medidas de `MallaBacteria` dónde caen manos y boca a 1,55 bloques (lo que llegan sus
  brazos): **hay que verlo en el juego** y retocar si las garras no cierran sobre el jugador.
- **Sonidos** (`tools/sonidos/bacteria.js`, más duros): grito (inspiración al revés + 3 capas +
  distorsión), caza, acecho, y nuevos: renuncia, agarre, levanta, 3 mordiscos, devora, 2 gritos
  de la víctima y el del hueco. Los del pack de EliteCreatures (NullForums) NO se usan: el
  organizador lo pidió y se le explicó que el mod se reparte público con venta de entradas.
- **Cuántas Bacterias**: `Fase.bacterias` por cada 10 jugadores (1 / 1,5 / 2,2 / 3), tope 40
  por dimensión: con 200 en la fase 1, unas 20.
- **Paquete para el hosting**: `Descargas/Backrooms-servidor-0.9.5.zip` (fabric-server.jar,
  mods probados, config, server.properties con view 6 / simulation 4, LEEME-HOSTING.txt). Sin
  eula ni token. El pack local (`pack/mods`) ya lleva el jar 0.9.5; publicarlo cuando haya IP.


## El código de la entrada también por correo (2026-10-06; xgdier_)

- Tienda desplegada: al confirmarse el pago se manda el código al correo de Tebex (Resend,
  dominio peakmc.lat verificado, `RESEND_API_KEY` y `MAIL_FROM` en Render). Una sola vez aunque
  lleguen webhook y vuelta del comprador. Panel → Eventos: reenviar por correo y correo de prueba.
- Probado en local con un Resend falso (webhook → código → correo; sin duplicados; reenviar).
  **Falta**: un correo real (botón «Enviar correo de prueba» del panel) y la primera compra real.

## Instalador con la marca del evento (2026-10-06; xgdier_)

- El instalador (NSIS) ya lleva el icono de la puerta (`assets/icon.ico`), la barra lateral y la
  cabecera del evento (`assets/instalador/`, se rehacen con `tools/instalador/generar.ps1`) y va en
  español. Pantalla de bienvenida propia y, al final, dos casillas: «Abrir Backrooms Launcher» y
  «Crear un acceso directo en el escritorio» (`assets/instalador/instalador.nsh`). El del menú
  Inicio se crea siempre: así sale al buscar «Backrooms». Al desinstalar se borra el del
  escritorio (no al actualizar).
- `main.js`: `app.setAppUserModelId('com.backrooms.launcher')` para que la ventana se agrupe con
  esos accesos directos y la barra de tareas enseñe su icono.
- Compilado en local (`npm run build`, sin publicar) y visto: la bienvenida sale con la barra y
  los acentos bien. La pantalla final y la desinstalación **sin ver todavía** (el organizador lo
  está instalando). Llega a los jugadores con la próxima versión del launcher (sigue la 1.0.0).

## Tienda: apartado Eventos y lista del evento (2026-10-05, noche; xgdier_)

Lo pidió el organizador: la entrada se vende HOY pero el launcher aún no se publica, y hay
que saber quién compra. Todo en peakmc-store (**desplegado** en tienda.peakmc.lat el 2026-10-05) y en
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
- **Tráiler** en /eventos: versión web 1080p30 (23,6 MB, `public/assets/eventos/backrooms-trailer.mp4`,
  sacada del 4K con el ffmpeg de Medal) con portada del segundo 58; solo se descarga al darle a play.
- **Render (hecho 2026-10-06 04:10 UTC, con el conector de Render)**: `BACKROOMS_FIRMA` y
  `BACKROOMS_SERVIDOR_TOKEN` puestas en el servicio `peakmc` (srv-da6gilu7bikc738hel00) y
  redesplegado. Comprobado en directo: `/api/backrooms/clave` da la misma clave pública que lleva el
  mod, y la whitelist responde 200 con el token y 401 sin él.
- **Falta (organizador)**: el paquete de la entrada en Tebex (2 $, sin comandos) y vincularlo en el
  panel (Productos → «Entrada · Backrooms»); y pegar el token (está en el `.env` de peakmc-store) en
  `token` de `config/backrooms-acceso.json` del servidor del evento. Ver VENTA.md.
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

1. **Probar la 0.9.5 en el juego**: arrastrarse, huecos (que se vean bien y se pueda entrar
   sin atascarse), la Bacteria que te pierde y se retira, y el agarre entero (poses, cámara,
   sonidos, muerte y eliminación). Necesita encender un servidor: pedir permiso.
2. **El pack publicado es el de prueba** (apunta a playit, que ya no está). Con la IP del
   hosting: `pack/evento.json` (con `acceso`) o una ficha de prueba, y publicar (pedir permiso).
3. Tienda: desplegada (ver arriba); falta el paquete de Tebex y pegar el token en el servidor.
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
