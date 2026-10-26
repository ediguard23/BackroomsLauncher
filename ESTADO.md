# Estado del proyecto (para la siguiente sesión)

Última actualización: 2026-10-23 (hora de Honduras). Mod `backrooms_evento` 0.28.0, launcher 1.0.0
(con arreglos sin publicar) y un launcher aparte para la beta. **Beta #3** (2026-10-23): pack-beta
2026.10.23-a5f1fa08 con la 0.28.0 y `Downloads/Backrooms-servidor-beta3-0.28.0.zip` (el de la beta #2 con el mod
nuevo, sin Krypton). El mismo hosting 66.51.99.171:25626.

## Tráiler de los cuatro niveles (2026-10-23/24; Bruno)

Bruno pidió un tráiler cinematográfico de los cuatro niveles (Nivel 0, Pool Rooms, Funny Rooms y Nivel 3), sin spoilers.
Todo está en `tools/rodaje/`, **que sigue sin subirse al repo** (es local, como el rodaje de la Bacteria):
- **Modo «trailer» del mod de rodaje** (`Trailer`, `Captura`, `FotogramaMixin`):
  - planos en cualquier dimensión, leídos de `tools/rodaje/trailer.json`;
  - teletransporta al jugador y precarga los chunks de todo el recorrido;
  - figuras solo de cliente y comandos;
  - graba **fotograma a fotograma** a ffmpeg (1080p30, fluido aunque el PC vaya lento), con `/tick rate` ajustado al ritmo del vídeo.
- **`montaje-trailer.js`**:
  - banda sonora con los sonidos del mod y música sintetizada;
  - montaje con ffmpeg: fundidos, parpadeos, apagón, títulos de nivel, franjas de cine, grano, montaje rápido final y logo;
  - salida en 1080p y en una versión para Discord de menos de 10 MB.
- Guion técnico plano a plano en `tools/rodaje/LEEME-TRAILER.md`. 15 planos revisados con capturas; 77,8 s.
- **Hecho** (madrugada del 24):
  - grabado entero (1938 fotogramas) y montado;
  - `Desktop/BackRooms/trailer/trailer-1080p.mp4` (94 MB) y `trailer-discord.mp4` (9,0 MB);
  - revisado por fotogramas: títulos, apariciones fugaces de la Bacteria y del Amasijo, apagón y logo.
- Arreglos del rodaje:
  - no más de 2 lecturas de GPU pendientes (con la gráfica integrada se ahogaba);
  - cada plano graba sus fotogramas exactos;
  - no minimizar el juego mientras graba (deja de dar fotogramas).
- 2026-10-24, segunda versión:
  - **música de tráiler épica** (`tools/rodaje/musica-trailer.js`: cuerdas, ostinato, taikos, braams y coro en re menor, que crecen hasta la Estación y el logo);
  - compresor en la mezcla;
  - la Bacteria del Nivel 0 ya no atraviesa la pared (va por x = 62).
- Tercera versión: **solo música**, sin los sonidos de los niveles (`--solo-musica`): `trailer-musica-1080p.mp4` y `trailer-musica-discord.mp4`, más `trailer-sin-sonido-1080p.mp4` para ponerle otra música en un editor. La canción de Minecraft que pidió Bruno no se mete (sin licencia).
- **Versión 2 con historia** (2026-10-24):
  - Steve ve el cartel del Proyecto Umbral en una calle de noche, abre su billetera vacía, arranca una tira, firma (la cinemática del /start con «Steve» en el contrato), cae en el ascensor y recorre los niveles huyendo de lo que se intuye. Dura 2:12.
  - Datapack de la calle solo en el mundo de pruebas. Ver `tools/rodaje/LEEME-TRAILER.md`.
  - **Faltan las voces** (Higgsfield: hay que iniciar sesión).
- **Versión 3** (misma tarde), con los cambios que pidió Bruno: movimientos de jugador, Steve sin flotar en la pasarela, traje en los niveles con planos de cara, el cartel pequeño en un poste e inserto de la tira guardada en la billetera, y la lluvia más baja. Dura 2:14.
- **Versión 5** (2026-10-25, madrugada): la firma va fotograma a fotograma (el reloj de CinematicaCliente se fija antes de cada fotograma; antes se congelaba). Steve guarda la tira doblada en la billetera, llega al edificio de Umbral, se para, mira arriba, abre la puerta y entra a un vestíbulo (recepción, ascensores, sala de espera, plantas). Ciudad con relieve (balcones, ventanas hundidas, árboles, coches, bancos, parada de bus, farolas con brazo). Central sin el rótulo de materiales (oculto solo en el cliente que graba). Persecución en la pasarela: el Amasijo alcanza a Steve, muere y suena el monitor cardíaco en el negro antes del logo. El rodaje pone el juego en ventana 1920x1080 (orden «ventana»): a pantalla completa se minimizaba y dejaba de dibujar. 2:28.
- **Versión 4** (2026-10-24, noche):
  - Steve se coloca en cada fotograma, sin interpolar entre ticks (antes iba a saltos en la calle), y las piernas las mueve el guion.
  - Flyer pequeño de anuncio (tools/rodaje/cartel-trailer.js: textura del mod de rodaje con el papel y custom_model_data «cartel») en un poste de hormigón.
  - Fuera el plano de la habitación vacía de la Fiesta (parecía que Steve se teletransportaba); Steve se gira al oír la risa.
  - La central se graba más alta, mirando a los controles.
  - Pasos reales de Minecraft (piedra, moqueta, agua y rejilla), rasgado de papel más realista, la risa del payaso que se corta con el apagón y la música continua hasta el logo.
  - **Voces de Higgsfield** (Chrome): radio y megafonía (Marisol), «Es un estudio sencillo» y «Le veremos al otro lado» (Juan), en Desktop/BackRooms/trailer/voces.
- **Versión 6** (2026-10-25), con los cambios que pidió Bruno al ver la v5:
  - **Entrada al edificio en primera persona** (`umbral_pov`): desde los ojos de Steve, mira arriba al rótulo UMBRAL, baja la vista a las puertas, las empuja y se funde a negro; se abre en la sala de la firma. Fuera el plano del vestíbulo.
  - Las puertas de hierro de la entrada tenían las bisagras al revés: al abrirse, las dos hojas se juntaban en el centro (por eso en la v5 parecía que Steve las atravesaba).
  - **Fuera los coches** de la calle.
  - **Sin apariciones a mitad de plano**: las figuras están desde el primer fotograma (antes salían en el primer tick; en la piscina se veía el agua y luego aparecía Steve).
  - **Final en primera persona** (`e_caza`): Steve corre por la pasarela con el traje (se le ven los guantes y las mangas al correr), mira atrás, ve al Amasijo, corre más (la cámara tiembla cada vez más), vuelve a mirar y el Amasijo le salta encima; corte a negro y la línea plana del monitor. Sin la muerte de Minecraft.
  - Modo `pov` del rodaje: la cámara va en la cabeza de una figura (que no se dibuja: `CabezaMixin`; y sin casco), con balanceo al andar y sacudida por tramos; `cuello` configurable por figura.
  - **Desenfoque de movimiento**: se graba a 60 fps y el montaje funde cada dos fotogramas (`"mezcla": 2`); sale a 30.
  - **Mod 0.28.1**: en la cinemática de la firma, los faldones de la bata del doctor van con cada pierna (al andar, las piernas la atravesaban). Shader `cinematica.fsh`. **Sin publicar** (ni pack ni servidor beta): hace falta permiso para publicar el pack.
  - Probado: las tomas nuevas grabadas y revisadas por fotogramas (entrada, puerta abierta, carrera, salto del Amasijo, bata al andar, piscina sin aparición).
- **Versión 7** (2026-10-26):
  - el edificio de Umbral es simétrico respecto a las dos puertas y por dentro está vacío y negro (al abrir la puerta, fundido a negro y la firma);
  - los rótulos «PROYECTO UMBRAL» (centrado sobre la puerta) y «UMBRAL» (más grande y más bajo, sobre un panel oscuro) tienen relieve;
  - la cámara en primera persona va amortiguada y sin los saltos del paso;
  - el vídeo principal sale a más calidad (CRF 13).
  - 7b: los rótulos quedan centrados con la puerta (las coordenadas enteras de los comandos van al centro del bloque), el fundido a negro empieza 0,3 s antes y se borraron los tráileres y pruebas viejos de Desktop/BackRooms/trailer.
  - Ver tools/rodaje/LEEME-TRAILER.md.
- **Sin escuchar**: la banda sonora (solo revisada con medidores y espectrograma).

## 0.28.0: el Nivel 3 sin generador, rejas corredizas, eco en la voz (2026-10-23; Bruno)

Bruno pidió otra tanda para el Nivel 3:
- **Fuera el generador**: sin sala del generador, sin gasolina, sin bidones, sin apagones y **sin HUD** (se
  borraron `EstacionCliente` y el paquete `EstadoEstacion`, la misión COMBUSTIBLE, `estacion combustible`, los
  bloques del generador, la boca, el escape y el macizo, con sus texturas y sonidos). El nivel siempre tiene luz.
- **Se aparece** en uno de **8 sitios al azar** de la zona de abajo del piso 1 (`PlanoEstacion.SPAWNS`, mirando a
  un pasillo abierto). Al reaparecer, al piso más alto abierto, como antes.
- **Sin cámara** en la Estación: al llegar se quita y `Equipo.herramientas` ya no la da allí (solo la linterna).
- **Rejas corredizas**: un marco fijo a cada lado (`marco_reja`, acero) y la caja del fusible en el de la
  derecha; la hoja del medio (1 de ancho, 3 de alto) se desliza a la izquierda al poner el fusible y vuelve al
  quitarlo. Fuera el hueco del costado. La animación dura **1 s**.
- **Amasijo** un 15 % más lento (4,25 y 2,04 bloques/s), más estrecho (0,9 para caber por la reja) y, si en 2 s
  no consigue acercarse (una reja cerrada delante), **se rinde**, se calma 10 s y vuelve a deambular. Solo
  detecta viendo (ya no hay «a oscuras»).
- **Comida** un 25 % menos en todo el nivel (también en los buzones).
- **Sonidos**: pasos de los jugadores en la Estación con algo de eco de nave (`estacion_paso1-6` en hormigón,
  `estacion_paso_chapa1-6` en chapa y rejilla; menos que en las Piscinas); pasos del Amasijo rehechos, blandos
  y húmedos (`amasijo_pasos1-4`); `estacion_arranca` ya no es un diésel (contactores y zumbido).
- **Eco en el chat de voz** (`cliente/EcoVoz`, hecho por nosotros con la API de Simple Voice Chat, solo en el
  cliente): cuanto más lejos habla alguien, más reverberación y más apagada la voz (desde 6 bloques; entera a 32;
  algo más en la Estación). Sin mods nuevos en el pack.
- Staff: `backrooms estacion reja <n>` lleva delante de la reja n. El panel de la Estación ya no tiene botones
  del generador; «Ir a» con las salas nuevas.

**Probado**: compila; aparición al azar mirando a un pasillo, sin HUD y sin cámara; la reja corrediza se ve bien
cerrada (marcos y caja a la derecha); el Amasijo caza y agarra. **Sin probar**: ver la hoja deslizándose (las
pruebas se cortaron: el Amasijo me mataba al apuntar a la caja), que el Amasijo se rinda delante de una reja
cerrada, cómo suenan los pasos nuevos en el juego y el eco de la voz (hace falta otra persona hablando).
Para probar la reja sin que moleste: `kill @e[type=backrooms_evento:amasijo]` y, por RCON, los `tp` con
`execute in backrooms_evento:estacion run` (si no, te manda al overworld).

## 0.27.0: el Nivel 3 por zonas que suben, rejas animadas, buzones y arreglos de la Fiesta (2026-10-23; Bruno)

Bruno explicó cómo es de verdad el nivel 3 de Escape Together y pidió una tanda de cambios:
- **El trazado** (`PlanoEstacion`, rehecho): todo el nivel sube en diagonal hacia el norte (-z).
  - **Sala del generador** (y = 34, donde se cae): limpia y simétrica, **generador grande** de 4x3x3 bloques
    (`generador_estacion`, 36 piezas con frente de mandos, laterales de radiador y tapa con el escape), la boca
    del depósito al lado, el tablón, dos fusibles, dos bidones y comida. Se sale por la **reja del tutorial**
    (fusible en su caja) y una escalera de 6 hasta el piso 1.
  - **Cada piso** = laberinto de 15x4 celdas por zona y **tres zonas, cada una 4 bloques más alta**; entre zona y
    zona, **dos rejas con su escalera** (la caja de la reja se alcanza también desde la escalera). Arriba del
    todo, el **pasillo largo** (se sale por el medio) y en un costado la **central**; su **puerta** (persiana) da a
    un pasillo al **piso siguiente, a la misma altura** (piso 1 → 2 → 3: y 40, 48, 56). Fuera las escaleras largas.
    Los materiales están sobre todo en las zonas de abajo; dos trasteros con reja por piso (el del piso 3, la cizalla).
  - La **pasarela** sale de la central del piso 3 hacia el norte; al final, la puerta encadenada.
- **Rejas animadas**: suben o bajan en 1,5 s (`JuegoEstacion#abrir/animar`: 9 BlockDisplay que se deslizan; el
  bloque queda OCULTA mientras tanto: subiendo sigue chocando, bajando ya se pasa). Lo mismo las persianas de las
  centrales. Mientras se mueve, la caja no hace caso. La reja es **fina** (4 px): ya no tapa su caja.
- **Buzones** (`buzon_estacion`, 2 por zona): con un fusible (que se queda dentro) se abren: 15 % cinta, 15 %
  cable, 10 % fusible, 35 % comida, 25 % nada.
- **Colisión** en todo lo de la Estación (tuberías, cuadros, señales, luz roja, rótulos, cables), con su forma.
- **Sin radar** en la Estación (la flecha de la hoja no apunta a nada).
- Mensaje de la central: «X ha colocado 2 fusibles y 1 cinta aislante en la central del piso 1 (3/3).»
- **Gasolina** un 15 % más lenta. **Amasijos**: 2, 3 y 4 por piso, siempre; no salen de su piso; corren derechos
  y, si no hay camino (reja cerrada), se calman y vuelven a deambular.
- Texturas: **luz roja** rehecha (era la «textura glitcheada»), **cinta** y **fusible** nuevos (cartucho), los
  fusibles del punto tumbados en su bandeja. Decoración siempre dentro de la celda (antes había cuadros y rótulos
  metidos en la pared, que dejaban agujeros); las tuberías van de pared a pared.
- **Sonido de repostar** rehecho: el tapón, el chorro con burbujas que suben de tono y los glu-glu de la garrafa.
- **HUD** de la Estación y de la Fiesta un 18 % más grande; en la Fiesta, tickets y «/1500» del mismo tamaño.
- **Fiesta**: el número de las puertas, centrado en su moneda; **2 Anfitriones** (3 con 10 jugadores o más);
  a salvo dentro de **cualquier habitación** (no solo las seguras y las de inicio).
- Pruebas: la orden `usar` acepta decimales (con `usar 0.15` se quedaba el clic pulsado).

**Probado**: sala del generador (generador grande, HUD), la reja del tutorial (poner el fusible: sube animada),
una reja entre zonas y su escalera, la central 1 (se abre su puerta al piso 2, franja amarilla), centrales 2 y 3
con `avanzar`, la pasarela, la cadena con la cizalla y salir; Amasijos 2 en el piso 1 y 3 en el piso 2.
**Sin probar**: recorrer los laberintos enteros a pie, los buzones en el juego, que el Amasijo se calme ante una
reja cerrada, el sonido nuevo de repostar (generado, no escuchado), la Fiesta (HUD, puertas, Anfitriones).

## 0.26.0: el Nivel 3 rehecho como el de Escape Together (2026-10-21; Bruno) — sustituido por la 0.27.0

Bruno pidió que la Estación fuera como el nivel 3 de **Backrooms: Escape Together** (se buscó en los foros
de Steam: subestación, generador de gasolina, rejas que se abren y cierran con fusibles, cinta, cable,
cizalla, pasarela, el «arm demon»). Lo de la 0.25.0 de abajo (sectores, paneles, interruptores en orden) ya
no existe; se quedan el generador, el Amasijo, el HUD, las texturas y los sonidos.

- **Tres pisos que se suben** (`PlanoEstacion`, y = 40, 54 y 68; la dimensión ahora tiene 96 de alto). Se
  llega cayendo a la **nave del generador** (piso 1, al sur). Cada piso es un **laberinto** de 19x15 celdas
  (semilla fija) partido en tres zonas por dos líneas de **rejas** (dos rejas por línea) y con dos
  **trasteros** cerrados con reja. Al final, un **pasillo largo** de lado a lado (se sale del laberinto por
  el medio) y en un costado la **central**; al otro costado, una puerta tapiada. Detrás de la central, la
  **escalera** (persiana que se abre con la central) al piso de arriba. Pisos: 1 sur→norte (central al este),
  2 norte→sur (al oeste), 3 sur→norte (al este) y al este de la central del 3, la **pasarela** sobre un pozo
  hasta la **puerta encadenada**.
- **Rejas** (`reja_estacion`, 3x3 de barrotes): se abren con un fusible en su **caja** (`caja_fusible`, bloque
  entero en el muro de al lado, se ve por los dos lados); clic derecho otra vez: sacas el fusible y la reja
  baja (no baja si hay alguien debajo). Cortan el paso al Amasijo.
- **Centrales** (bloque `panel_averiado`, ahora «Central eléctrica»): piso 1: 2 fusibles + 1 cinta; piso 2:
  2 fusibles + 1 cinta + 1 cable; piso 3: 3 fusibles + 2 cintas + 2 cables. Clic derecho pone todo lo que
  lleves que le falte; con todo y corriente, se activa (pantalla encima con lo que falta).
- **Objetos**: fusible (genérico, ya no hay de colores), cinta, cable, bidón de gasolina y la **cizalla**
  (una, en un trastero del piso 3: corta la cadena del final). Se reponen solos; la cizalla, solo si nadie
  la tiene. Fuera la caja de herramientas y los interruptores.
- **Generador**: 10 min el depósito lleno, empieza al 40 %. Sin gasolina, apagón y el Amasijo te encuentra
  sin verte (a 26 bloques).
- **Amasijo**: uno por piso con gente (+1 cada 10 jugadores, +1 en el piso 3 con la pasarela); no sube
  escaleras (solo ve y caza en su piso).
- Misiones: COMBUSTIBLE, PISO1, PISO2, PISO3, PASARELA. Al reaparecer, al piso más alto abierto. HUD: piso,
  lo puesto en la central de tu piso, centrales activas (un cuadrito por piso) y la corriente.
- Staff: `estacion ir nave|laberinto_1..3|pasillo_final_1..3|central_1..3|llegada_2|llegada_3|pasarela`,
  `estacion avanzar` (activa la central siguiente; al final, corta la cadena). Panel NIVEL 3 al día.
- Arreglo: si `misiones.json` tiene una misión que ya no existe, se descarta al cargar (se caía el servidor).

**Probado** (mundo de pruebas, Estación nueva): llegada (HUD «PISO 1 · CENTRAL 0/3»), una reja (poner el fusible
abre, sacarlo cierra y lo devuelve), activar la central 1 con 2 fusibles y 1 cinta, subir andando la escalera
al piso 2 (el HUD cambia de piso), la central 2 (con `avanzar`), subir al piso 3, la central 3 (pasarela y
focos), la cadena (sin cizalla avisa; con ella se corta) y salir (escapa); el Amasijo sale en el piso del
jugador. **Sin probar**: recorrer los laberintos enteros a pie (que todas las zonas conecten por sus rejas,
dónde caen los puntos y los trasteros), el Amasijo cazando entre rejas, varios jugadores, los pisos 2 y 3
con sus propios materiales (solo con `avanzar`).

## 0.25.0: Nivel 3, la Estación Eléctrica (2026-10-21; Bruno) — sustituido por la 0.26.0

Nivel nuevo después de la Fiesta: el tobogán del salón final lleva ahora a la **Estación Eléctrica**
(`estacion/`, dimensión `backrooms_evento:estacion`); si el servidor no la tiene, se escapa como antes.
Historia en LORE.md (sección «Nivel 3»).

- **El sitio** (`PlanoEstacion`, cajas como la Fiesta; `GeneradorEstacion` con índice de cajas por chunk):
  acceso (vestuario, la radio, el tablón de instrucciones roto), pasillo, **nave del generador** (techo de 12,
  generador naranja de 9x7 con su boca, escapes, turbinas, lámparas en las paredes), **sala de control**
  (caja de fusibles al oeste, interruptores al este, consolas, pantalla con la secuencia, persiana de la
  pasarela), la **pasarela** de rejilla sobre un pozo (focos a los lados, la puerta del fondo) y **4 sectores**
  laberinto (A rojo, B amarillo, C azul, D verde; semilla fija): salas de paneles, de máquinas y de
  fusibles (cerrada con persiana), rótulos «A-07», callejones con puntos de recogida, zonas a oscuras.
- **El juego** (`JuegoEstacion`, todo compartido, se guarda en `<mundo>/backrooms/estacion.json`):
  1. **Combustible**: el generador gasta (7 min el depósito lleno, solo con alguien en el nivel; empieza al
     35 %). Bidones en las salas de máquinas y callejones (uno por persona; se reponen a los 2 min). Sin
     gasoil: apagón en todo el nivel y nada eléctrico responde. Alarma de reserva por debajo del 15 %.
  2. **Paneles**: 8 (2 por sector), cada uno pide cable, herramientas o cinta (en los callejones; se reponen a
     los 90 s). Con los dos de un sector sube la persiana de su sala de fusibles.
  3. **Fusibles**: los 4 a la caja de la sala de control y los **interruptores en el orden de la pantalla**
     (se sortea). Cada acierto lo dice; un error: fallo de 12 s (apagón) y a empezar.
  4. **Pasarela**: se abre la persiana, los focos se encienden de dos en dos hasta el fondo y quien llega a la
     plataforma del final (vivo, no creativo) sale (de momento, escapa).
  Misiones COMBUSTIBLE, PANELES, FUSIBLES y PASARELA (hoja y radar: a lo que toca ahora). Lo de la Estación
  (materiales, bidones, fusibles) se le quita a quien sale del nivel, para que un fusible no se pierda.
- **El Amasijo** (`Amasijo`, `AmasijoEstacion`; modelo en `entidades.js`: carne pálida, 6 brazos de patas,
  8 que tantean, boca con dientes y ojillos que brillan): ronda los sectores, ve a 20 bloques (11 sin
  corriente), grita y caza a 5 b/s hacia donde te vio; si rompes la línea de visión, a los 3,5 s te olvida.
  Agarra y mata. Sale al arrancar el generador; uno más con la mitad de los paneles y otro con la pasarela
  (+1 por cada 8 jugadores), lejos de todos. Cada sistema que vuelve hace un estruendo en un sector lejano.
- **Cliente**: HUD arriba a la derecha (combustible con su barra, paneles n/8, fusibles por color, corriente),
  maquinaria y generador en bucle según haya corriente, estruendos, chispas de los paneles rotos.
- **Texturas y modelos**: `tools/texturas/estacion.js` (en `npm run texturas`); sonidos:
  `tools/sonidos/estacion.js` (generador diésel, arranque, parada, reserva, repostar, fallo, persiana,
  palanca, fusible, la radio, pasos/grito/respiración/mordisco del Amasijo).
- **Staff**: `/backrooms estacion [jugador]`, `estacion ir <sala>` (acceso, nave, control, pasarela,
  a_paneles_1, b_fusibles...), `estacion combustible <n>`, `estacion avanzar|reponer|ver`,
  `invocar amasijo` (los invocados no los quita el reparto). Pestaña **NIVEL 3** en el panel.
- Reaparecer (con «morir reaparece»): al acceso de la Estación. El aviso de eliminación dice «NIVEL 3».

**Probado** (mundo de pruebas, Estación nueva): llegada, HUD, coger un bidón y repostar (arranca y completa la
misión), reparar un panel con su material, meter los 4 fusibles, un interruptor equivocado (fallo y apagón) y
la secuencia buena (pasarela activa, focos), salir por la plataforma (escapa, y se le quitan los
materiales de la Estación), el Amasijo (aparece, caza y
agarra), cómo se ven acceso, nave, sala de control, sala de paneles y pasarela. **Sin probar**: llegar desde
el tobogán de la Fiesta de verdad, con varios jugadores a la vez, las persianas de las salas de fusibles al
reparar los dos paneles de un sector (con `avanzar` sí se abren todas), los sonidos dentro del juego (se
generaron, no se han escuchado), el panel (pestaña NIVEL 3) pulsando.
**Falta**: la **voz de la radio** (Higgsfield; Chrome no estaba conectado): `tools/sonidos/voces/estacion_radio.wav`
y volver a correr `node tools/sonidos/estacion.js estacion_radio` (sin el wav la radio es solo ruido y el
corte). Guion propuesto (voz Juan, cansado): «¿Hola? ¿Alguien me recibe? Aquí la estación de relevo. Si habéis
caído por el techo del acceso, escuchad: arrancad el generador antes que nada. Gasta mucho: no dejéis que se
vacíe. Y no os quedéis a oscuras en los sectores. Hay algo ahí abajo que oye las máquinas. Si salís, decidles
que nos dejaron aquí. Me llamo...». Tampoco está el Nivel 3 en el avance del launcher.

## 0.24.0: panel del staff, el evento de las Backrooms (no solo del Nivel 0) y arreglos (2026-10-21; Bruno)

- **Panel del staff `/backrooms panel`** (`PanelAdmin`, `red/EstadoPanel`, `cliente/PantallaPanel`): pestañas
  General, Nivel 0, Piscinas y Nivel Fun. En cada una, lo que pasa ahora (jugadores, entidades vivas, luz,
  inundación, tarjetas...; se refresca cada 5 s), sus entidades (con invocar), sus misiones (texto de
  `TipoMision`), sus sucesos y accesos («ir a») con botones que mandan los comandos de siempre (el servidor
  comprueba que eres operador). Los peligrosos (/start, perdonar, abrir o reponer la bóveda) piden dos clics.
  Abajo sale lo que contesta el servidor.
- **Fuera los parkours** de la Fiesta (las zonas de debajo del laberinto, los suelos defectuosos que llevaban a
  ellas y la trampilla): bloques `suelo_defectuoso` y `trampilla_fiesta`, `SueloDefectuoso` y sus sonidos.
  La piscina de bolas sigue (castillo y sala central).
- **Rastros de arrastre**: tres manchas de suelo seguidas (`mancha` 5, 6 y 7: del charco a las últimas
  gotas). En las Piscinas, junto a la mitad de los cadáveres (el rastro llega al cuerpo) y alguno suelto;
  en el Nivel 0, al azar por la moqueta (más a oscuras). `/backrooms buscar arrastre`.
- **Todo el evento, no solo el Nivel 0**:
  - Launcher: al abrir, tras la firma de PeakMC, un **avance** de los tres niveles (Nivel 0 · THE LOBBY,
    Nivel 37 · Las Piscinas, Nivel Fun · Feeding Rooms), 2,5 s cada uno con su rótulo; un clic o una tecla lo
    saltan. El fondo recorre ahora también el **Nivel Fun** (shader nuevo en `src/ui/pasillo.js`). Subtítulo
    «PROYECTO UMBRAL» (antes «NIVEL 0»).
  - Menús del juego: tema del **Nivel Fun** (`Tema.FIESTA`, shader `assets/backrooms/shaders/core/fiesta.fsh`).
    Dentro del juego el tema es el del nivel donde estás; en el menú de inicio, `level` del pack (`0`, `37`,
    `fun` o `todos`: uno al azar, lo nuevo por defecto).
  - El aviso de eliminación dice el nivel donde ha caído cada uno (antes siempre «el Nivel 0»); la pantalla
    de muerte da las gracias «por entrar en las Backrooms»; consejos de carga de las Piscinas y la Fiesta;
    motd del servidor de ejemplo; README y LORE (se baja por el tobogán, no en una cabina).
- **Arreglos**:
  - Quien se desconectaba en mitad de la cinemática del tobogán se quedaba **invulnerable para siempre**.
  - Sin inventario, lo que se cogía con la barra llena iba a huecos invisibles: ahora **solo se coge a la
    barra** (`SoloBarraMixin`; si está llena, se queda en el suelo).
  - El menú de pausa decía que las misiones estaban en el inventario (E): ahora apunta a la hoja (M).
  - La misión de los tickets hablaba de las flechas del techo (ya no están).
**Probado**: el panel (pestaña General, botones y estado en directo), el tema de la Fiesta en la pausa y su
shader (compila), los rastros de arrastre en las Piscinas (puestos a mano), la secuencia del avance del
launcher (en un navegador, sin Electron) y el fondo del Nivel Fun. **Sin probar**: las otras pestañas del
panel pulsando, los rastros generados solos (en mundo nuevo), el launcher de verdad en Electron.

## 0.23.0: la bóveda y el tobogán a la Fiesta, y retoques (2026-10-20; Bruno)

Piscinas → Fiesta (sin ascensor):
- La salida de emergencia es una **puerta de bóveda** redonda de 3x3 (aro de rayas amarillas y negras,
  volante, bisagras, marco de acero; los lectores a la derecha). La hoja que se ve es una entidad
  (`Boveda`, BlockDisplay de `PUERTA_BOVEDA`, no se guarda); los bloques `PUERTA_SALIDA` (3x3, PARTE 0..8)
  solo chocan. Con la tercera tarjeta **se abre girando** hacia dentro (3,5 s, interpolación de Minecraft).
- Detrás: **pasillo de acero** de 3x3 y 7 de largo (rejilla, dos luces) y al fondo la **boca del tobogán**
  con un pozo rojo (`BloquesFiesta.TOBOGAN`) hasta y=34. `PlanoPiscinas#buscarSalida` busca ahora un sitio
  donde todo lo de detrás de la pared sea macizo (no abre salas ni agua) y `salidaBloque` lo excava. **La
  salida cambia de sitio**: hay que regenerar las Piscinas (en pruebas se borró `dimensions/.../piscinas`).
- Al entrar en el pasillo (con las misiones hechas) empieza la **cinemática** (`TransicionFiesta`: quieto e
  invulnerable; paquete `CinematicaTobogan`; `ToboganCliente`, `PantallaTobogan`): cámara propia (Marker),
  sin HUD ni jugadores, franjas de cine. Andas, parpadean las luces, **alarma roja**, te das la vuelta y ves
  **subir el agua** por la puerta (la superficie de la inundación, solo en tu cliente), corres, te metes por la
  boca y caes por el pozo; negro; a los 11 s el servidor te lleva a la Fiesta y se abre la imagen. Sonido:
  `tools/sonidos/tobogan.js` (`cinematica_tobogan.ogg` y `_despues.ogg`).
- La megafonía (voz Marisol de Higgsfield, 1 crédito): «Atención. Fallo de contención en el Nivel treinta y
  siete. Las compuertas no resisten. Inundación total del nivel inminente.» (pausas recortadas y un 10 % más
  rápida para que quepa: `tools/sonidos/voces/megafonia_contencion.wav`).
Otros:
- Puertas del laberinto: la cifra pequeña (3x5) grabada en una moneda de oro.
- Panel de monedas y tickets mucho más pequeño (124x33).
- Fuera los carteles NOCLIP de la sala central (y el bloque `CARTEL_NOCLIP`).
- **Localizador** sin texto: aparato con pantalla verde de salida (muñeco corriendo a la puerta), un ratón
  con el botón derecho que **parpadea** (textura animada), rejillas de altavoz y dos moneditas.
- **Sin inventario** (la E no abre nada; en creativo sí): solo la barra. Fuera `InventarioBackrooms`.
- La **linterna y la cámara no se tiran** con Q (cliente y servidor, `SoltarObjeto*Mixin`).
- **Linterna en la mano**: en primera persona se dibuja el brazo (con el guante del traje) y la linterna en el
  puño apuntando al frente (`ManoLinternaMixin`).
**Probado** (capturas): la bóveda cerrada, abriéndose y abierta; el pasillo y la boca; la cinemática entera
hasta aparecer en la Fiesta con el título; números de las puertas, localizador y panel nuevos (texturas y HUD);
la linterna en la mano. **Sin probar**: oír la cinemática en el juego, con varios jugadores a la vez,
la Q con la linterna y la cámara en el juego, la E sin inventario, la linterna en tercera persona.

## 0.22.2: Nivel Fun, la segunda lista de Bruno (2026-10-20; Bruno)

Laberinto y salas:
- **Laberinto de 38x38 celdas (152x152 bloques)** con **60 salas seguras** (`PlanoFiesta.SEGURAS`) y 16 salas
  de inicio. Cada sala segura tiene **dos puertas de monedas** en paredes distintas (la más lejana posible):
  entras por una y sales por la otra a otra parte del laberinto, sin teletransporte. El localizador y la mesa
  van en paredes sin puerta.
- Las puertas cuestan 1, 2 o 3 monedas y la placa lo dice con **una moneda y la cifra** (antes palitos).
- El **localizador** es una placa que se entiende: «BUSCAR SALIDA», un altavoz con ondas y «CLIC DCHO».
- La **puerta morada** de los 1500 tickets es de **un solo sentido** («No hay vuelta atrás.»).
- Regalos y peluches ya no flotan en losas: regalos sueltos (o de dos en dos) y el peluche de la siesta en el
  suelo junto a las camas. Los estantes de pared del salón final, fuera (flotaban).
Decorado:
- **Globos en ramos de tres** atados con hilos a un peso (`RAMO_GLOBOS`), en vez de tirados por el suelo.
- **Tartas** nuevas: plato, dos pisos con capas de nata, glaseado que chorrea, fresas y velas de rayas con
  llama que alumbra (una a medio comer).
- **Moneda** sin la carita triste: borde en relieve y una estrella.
- **Red del castillo**: la malla casa de un bloque a otro y la primera y la última de cada tramo se meten en
  el poste (`red_castillo_0/1/2`).
- **Sala central** llena: banderines, marcadores, mostradores de premios junto a la puerta, tres mesas con
  sillas y tartas, maquinitas, piscina de bolas con valla, ramos y carteles. Los **tres noclip** llevan una
  franja amarilla y negra alrededor (`FRANJA`), un cartel «NOCLIP / OTRA VUELTA» con flecha
  (`CARTEL_NOCLIP`) y partículas de portal que se ven de lejos.
Anfitrión (payaso):
- **No muere ni desaparece** (ni con la linterna; ya no se guarda en el mundo: si no hay nadie cerca se quita
  y vuelve a salir cerca de los jugadores del laberinto).
- **1 por cada 5 jugadores** del nivel (mínimo 1), aparece lejos (24-80 bloques) y se ríe alto al salir.
- **Con luz** ronda despacio (la mitad de su velocidad) y no ataca. **En el apagón** va un 20 % más rápido
  (4,4 × 1,2 = 5,3 b/s, por debajo del esprint de 5,6) y persigue a quien vea a **8 bloques o menos y sin
  paredes en medio** (ya no oye). En una sala segura o de inicio no te pilla.
- `ConfigFiesta`: `anfitrionPorJugadores`, `anfitrionDetecta`, `anfitrionVelocidad`, `anfitrionApagon`,
  `anfitrionOlvida` (fuera `vista`, `oido`, `caza`, `paseo`, `aparicionCada`).
- En la Fiesta el apagón deja ver un poco sin linterna (`OSCURIDAD_FIESTA` 0,72) y más con ella.
Sonido y HUD:
- La **música baja al 25 %** mientras habla o se ríe el Anfitrión o habla la megafonía (`AmbienteFiesta.Cancion`).
- Las dos canciones tienen fundido de entrada y de salida (una se cortaba al final).
- Nueva voz del semáforo pillado (Higgsfield), sin «vuelve al inicio».
- **HUD** nuevo de monedas y tickets: panel con borde dorado, moneda grande, tickets con barra de progreso
  a 1500 y el bote de la vuelta.
Comida: **pizza +3 %** de cordura y **galletas +5 %** (`ComidaCordura`), además del hambre.
Dudas: las **monedas ya eran de cada jugador** (cada uno tiene sus cogidas; al que la coge se le esconde). El
**modo aventura** viene de `server.properties` (`gamemode=adventure`, `force-gamemode=false`): quien entró
alguna vez en otro modo lo conserva; para el evento conviene `force-gamemode=true`.
**Probado** (capturas y rcon): 60 salas seguras con sus dos puertas, cifras en las puertas, localizador, red
del castillo, sala central y noclip, tarta, ramos, sala de regalos, peluche, HUD nuevo; Anfitrión: aparece a
~36 bloques con un jugador, con luz se aleja (1,8 b/s) sin perseguir, en el apagón persigue y abraza, se quita
sin jugadores cerca. **Sin probar**: oír la voz nueva y que la música baje, la velocidad de persecución a mano,
el recuento de payasos con varios jugadores.

## 0.22.1: Nivel Fun y Piscinas, lo que pidió Bruno tras probar (2026-10-19; Bruno)

Fiesta:
- Las **tartas** se apoyan en la mesa (el modelo bajado 3 px: la mesa llega a 13/16 del bloque).
- Sin **radar de fichas** (la hoja solo marca la puerta del final cuando ya tienes los 1500).
- **Monedas** tumbadas en las mesas, quietas y con la luz del sitio; se cogen con **clic derecho**
  (`FichaEntidad#interact` → `JuegoFiesta#cogerFicha`), no al pasar. En cada mesa, la moneda en una mitad y
  la tarta o la comida en la otra.
- **Sin tickets tirados**: solo se ganan (bote de la vuelta, memoria, regalos, primer parkour). Donde había
  tiras ahora hay monedas o comida.
- **Sin flechas de neón** (ni en el laberinto ni en el pasillo del castillo).
- **Laberinto más grande** (30x26 celdas) con **26 salas seguras** (antes 4) repartidas solas con semilla
  fija (`PlanoFiesta.SEGURAS`, de 7x7, 7x3 o 3x7, nunca pegadas a otra habitación), cada una con su
  localizador y una mesa con moneda y comida.
- **Regalos** rehecho: sala de 15x11 casi a oscuras con estanterías y montones; 8 de los ~30 regalos son
  los tuyos (sorteo por jugador), se abren con **clic derecho** (`RegaloBloque`), **30 s**, y se juega **una
  sola vez** por jugador (cuenta desde que empieza).
- **Música**: la canción de la fiesta ya no se pisa con otra (dura 30,8 s y se relanzaba cada 28-34) ni suena
  en el castillo; la musiquilla del semáforo (el «pim pim pim» en bucle) se calla al salir del castillo
  (antes seguía si salías con la luz en verde).
- **Salón final** lleno: banderines por todo el techo, columnas con globos, tarta gigante con velas delante del
  tobogán, arco de globos, pista de baile de baldosas encendidas, mostrador de premios con peluches y regalos,
  maquinitas de feria, confeti y regalos por el suelo.
- **Anfitrión**: ya no se va porque le mires; solo si le apuntas con la **linterna encendida** (a menos de 20
  bloques, en el cono del foco y sin paredes en medio). En el apagón sale **uno por cada 3 jugadores del
  laberinto** (mínimo uno), en la ronda del laberinto, a más de 24 bloques de todos y separados entre sí, y se
  **ríe alto** al salir.
Piscinas:
- **Agua de la inundación**: la superficie se dibujaba con el rayo desde el centro de la cámara, sin el
  balanceo al andar, y se movía con el jugador; ahora el rayo sale de dos puntos del píxel (`efectos.fsh`).
- **+30 cadáveres** (de 125 a 155 en este mundo; `PROB_CADAVER` 0,04 → 0,0479) y comprobación más estricta
  de que el cuerpo tumbado quepa entero (cabeza y brazos incluidos) sin meterse en una pared.
  `/backrooms piscinas cadaveres` los cuenta.
Dudas: la comida (galletas, pizza) quita el hambre; la cordura **solo** la sube el agua de almendras (+30 %).
**Probado** (capturas): tarta apoyada; moneda tumbada y cogida con clic derecho; 26 salas seguras; regalos (abrir uno
de verdad, el tiempo de 30 s, el consuelo y «ya jugados» al repetir); Anfitrión: mirándole sin linterna sigue, con
la linterna encendida se va; el salón final nuevo; el recuento de cadáveres. **Sin probar**: ver el agua de la
inundación andando (las capturas quedaron bajo el agua), los cadáveres junto a paredes a la vista, la música
(no se puede oír en las pruebas), el apagón con varios jugadores.

## 0.22.0: el Nivel Fun rehecho como en Backrooms: Escape Together (2026-10-19; Bruno)

Bruno vio que la Fiesta no seguía la idea y pasó el vídeo de referencia (Level Fun de *Backrooms:
Escape Together*): se vio entero a capturas cada 4 s (con sus subtítulos) y la guía escrita que enlaza.
El mapa (`PlanoFiesta`) es nuevo; se aprovecha casi todo lo demás. Una VUELTA:
1. **Habitación de inicio**: se empieza en uno de 12 cuartitos con puerta verde (al azar; al llegar,
   al reaparecer y al volver por el noclip).
2. **Laberinto** (24x22 celdas, semilla fija): muros blancos con zócalo granate, columnas de colores,
   damero azul/amarillo/blanco, banderines y **flechas de neón en el techo** que apuntan a la salida por
   el camino más corto. **Mesas de fiesta** en los pasillos con monedas, comida (pizza y galletas, por
   `Comida`) o tickets. **Puertas verdes de monedas** (1-3, se pagan una vez por vuelta) a 12
   habitaciones: 4 **salas seguras** con el **localizador de salida** (2 monedas: la puerta roja suena
   solo para ti y echa chispas), la memoria, los regalos, la cocina, la siesta, la secreta y la de las
   mesas. La salida es una **puerta roja** con rótulo SALIDA (zumba cuando estás cerca).
3. **Suelos glitcheados** (3, del color del damero): ceden → parkour debajo → la trampilla sube
   **directa al castillo** (atajo; 100 tickets la primera vez).
4. **Castillo de bolas** (dos pisos de 12x12 celdas): postes amarillos, redes, lona azul, escaleras de
   arcoíris y **semáforos** en las redes. Luz verde/roja para todos los que están dentro: moverse en rojo
   quita 25 tickets (una vez por luz roja), no te devuelve. Arriba al este, el **tobogán**.
5. **Sala central**: el tobogán te deja aquí con el **bote** de la vuelta (600 menos 40 por minuto, mínimo
   200; `ConfigFiesta.jackpot*`). Marcador de LEDs «TICKETS x/1500» (cada uno ve los suyos,
   `MarcadorRenderer`) y el BOTE que va bajando. **Cuadrados de noclip**: caes y empiezas otra vuelta
   (las monedas vuelven a las mesas, las puertas se vuelven a pagar). La **puerta del final** (morada)
   pide 1500 tickets.
6. **Salón final**: la fiesta grande (damero rojo/morado, mesas, piscina de bolas con valla, escenario) y
   al fondo el **tobogán rojo con el cartel CERRAMOS**: quedarse 1,5 s en su boca = salir (escapar, como
   antes el Gran Premio).
Se quitó: el ascensor de llegada, el salón con el Gran Premio, el pasillo del semáforo, el torno (ahora
`puerta_fiesta`, con TIPO) y la lámpara de semáforo de pared. Bloques nuevos en `tools/texturas/laberinto.js`;
sonidos nuevos (puerta, localizador, zumbido de la salida, noclip, tobogán) en `tools/sonidos/fiesta.js`.
Staff: `/backrooms fiesta ver` dice la vuelta y el bote; `fiesta ir <sala>` tiene las salas nuevas
(inicio_N, segura_N, memoria, regalos, cocina, siesta, secreta, mesas, castillo, central, final...).
**Probado** (con teletransportes y capturas, servidor y cliente de pruebas): aparecer en un inicio y
abrir su puerta, el aspecto de laberinto, habitaciones, castillo, sala central y salón final; coger una
moneda de una mesa; pagar una puerta de 1 moneda; el localizador (cobra y avisa); la puerta roja; el
tobogán del castillo (+301 de bote); el noclip (vuelta 2, monedas y puertas repuestas); el semáforo
(-75 en 15 s moviéndose sin parar); el suelo glitcheado → parkour → trampilla → castillo (+100); la puerta
del final (avisa de lo que falta y con 1500 deja pasar) y el tobogán del final (escapa); un apagón en el
laberinto (oscuridad, linterna, sale el Anfitrión). Las flechas apuntan bien (comprobada una).
**Sin probar**: recorrer el laberinto y el castillo andando de verdad de principio a fin (cuánto se tarda
y si el bote está bien de números), oír el localizador y el zumbido de la salida en el juego, el Anfitrión
persiguiendo por los pasillos y por el castillo, la comida de las mesas al cogerla, varios jugadores a la vez.
Nota: el jugador de pruebas Bruno2524f estaba en las Piscinas (de la partida de Bruno) y se le mandó a la
Fiesta para probar; acabó escapado.

## 0.21.2: la Fiesta, todas las voces (2026-10-18; Bruno)

- Las 9 voces que faltaban, con otra cuenta de Higgsfield (≈2,7 créditos): la niña (Pixie) en el
  semáforo «¡Te he visto! Vuelve a empezar.» y «¡Has ganado! ¡Toma tus tickets!», y desde otras salas
  «Jijiji... ¡te encontré!», «No te vayas todavía... la fiesta acaba de empezar.» y «Mi mamá también
  vino a la fiesta. Todavía no ha salido.»; el Anfitrión (Juan) «No te pierdas la fiesta...», «Ven...
  te guardé un trozo de tarta.» y dos risas grabadas. `anfitrion.voz` son ya sus 3 frases y
  `anfitrion.risa` las 2 risas (fuera las sintéticas `anfitrion_risa_sint*`).
**Probado**: cargan y suenan en el cliente (playsound de cada evento, sin errores en el log).
**Sin probar**: cómo quedan de volumen y ambiente oídas de verdad en la Fiesta.

## 0.21.1: la Fiesta, voces y retoques (2026-10-18; Bruno)

- **Voces nuevas** (Higgsfield): «¡Luz verde!» y «¡Luz roja!» de la niña en el semáforo, la niña que
  habla desde otra sala («¿Quieres jugar conmigo?», con el tono un poco distinto cada vez) y el
  Anfitrión («¿Dónde estás, amiguito?», mezclada con su risa y su respiración en `anfitrion.voz`).
  (Las 9 que faltaban, en la 0.21.2.)
- **Memoria**: ganaba a la 6.ª ronda (secuencia de 8) en vez de a la 5.ª (de 3 a 7 colores); arreglado.
- **Parkour**: las plataformas suben como mucho a y = 35 (antes 36: al saltar se daba con el techo).
- Staff: `/backrooms fiesta juegos <jugador>` dice la secuencia de la memoria y dónde están los regalos
  que le faltan (para probar y para ayudar en el evento).
**Probado** (con teletransportes y capturas): ganar la memoria (250 la primera vez, 100 después, las
4 baldosas encendidas), ganar los regalos (200), leer una nota (sale el texto en el chat), el parche
de la siesta cede y se cae a su parkour. **Sin probar**: las voces nuevas oídas en el juego.

## 0.21.0: la Fiesta, segunda entrega: suelos defectuosos, parkour y dos juegos más (2026-10-18; Bruno)

- **Suelos defectuosos** (`SueloDefectuoso`): tres parches de 2x2 que parecen el suelo de alrededor
  (moqueta en el salón, goma en el laberinto y en la siesta) pero su textura tiembla y se corrompe a
  ratos (animada), zumba muy bajo y suelta alguna chispa. Si te quedas encima cruje y cede: caes a
  una **zona de parkour** escondida debajo (`PlanoFiesta.PARKOURS`, suelo en y = 32), con piscina de
  bolas en el fondo (amortigua la caída; si caes en ella lejos de la llegada, vuelves al principio)
  y plataformas acolchadas hasta una **trampilla** que sube (2 s encima). Premio la primera vez
  (100 tickets + la tira de 50 del final). La del salón sube al salón; la del laberinto, **a la sala
  secreta** (sin pagar sus 3 monedas); la de la siesta, a la siesta. El parche se rehace al rato.
- **Sala de juegos** (torno de 1 moneda, en la pared este del salón, sur del semáforo):
  - **Memoria de colores** (`JuegoMemoria`): pisa la estrella; las 4 baldosas (roja, amarilla, azul,
    verde, sobre alfombra oscura) se encienden en orden con su nota y hay que pisarlas igual; cada
    ronda una más (de 3 a 7). Fallar, tardar 8 s o salir acaba. Premio 250 (100 repetido). Juega uno.
  - **Busca los regalos** (`JuegoRegalos`), detrás del tabique, casi a oscuras: pisa la estrella y
    tienes 60 s para encontrar 8 regalos que solo tú ves brillar (y que tintinean de vez en cuando);
    reloj los últimos 10 s. Premio 200 (80 repetido); si se acaba el tiempo, 10 por regalo. Cada
    jugador juega su partida.
- **Notas** (`NotaFiesta`): 8 repartidas (cocina, informe del Proyecto Umbral, siesta, laberinto, junto
  al parche del salón, llegada, semáforo, sala del premio) que cuentan a trozos para qué es la Fiesta.
- Sonidos nuevos en `tools/sonidos/fiesta.js` (zumbido, crujido, caída, bolas, trampilla, notas de la
  memoria, error, regalos, reloj) y texturas en `tools/texturas/fiesta.js`. Premios en `ConfigFiesta`.
**Probado** (capturas): el suelo del salón cede y se cae al parkour, la piscina devuelve al principio,
la trampilla sube con su premio (150 tickets), el del laberinto cede; la memoria se enciende en orden,
«¡Te toca!» y el fallo; los regalos empiezan, brillan y se acaban por tiempo; las notas se ven.
**Sin probar**: recorrer los parkours saltando de verdad (solo con teletransportes: puede haber algún
salto difícil). (Lo demás, probado en la 0.21.1.)

## 0.20.0: la Fiesta (Nivel Fun, las Feeding Rooms), primera entrega (2026-10-18; Bruno)

El nivel de después de las Piscinas (paquete `fiesta/`; lore en `LORE.md`, «Nivel Fun»):
- **Transición**: la cabina de la salida de las Piscinas ya no «escapa»: sube a la Fiesta
  (`Tarjetas#cruzar` → `Fiesta#llegar`). Se llega dentro de un ascensor, con el título
  «NIVEL FUN / FEEDING ROOMS» y la bienvenida del animador; se conserva el inventario, la cordura
  y el equipo, y se dan las misiones nuevas: **Consigue los tickets** y **Canjea el Gran Premio**.
- **El mapa** (`PlanoFiesta` + `GeneradorFiesta`, dimensión `backrooms_evento:fiesta`, cajas como
  el vestíbulo): llegada con arco de globos y taquilla, salón de fiesta (mesas con mantel, tartas,
  sillitas, regalos, escenario con telón, globos en el techo), laberinto acolchado de 11x17 celdas
  (semilla fija, con atajos) con su **sala secreta** (3 monedas), el **pasillo del semáforo**, la
  **cocina** (1 moneda: ollas, bandejas con nombres), la **sala de la siesta** (2 monedas: camitas)
  y la **sala del Gran Premio** con el regalo gigante.
- **Monedas y tickets** (`JuegoFiesta`, guardado en `<mundo>/backrooms/fiesta.json`): 15 monedas y
  unos 975 tickets repartidos (cada uno coge cada ficha una vez, al pasar; quien ya la cogió no la
  ve). Los **tornos** (`Torno`) se pagan una vez y se cruza al otro lado; desde detrás siempre se
  vuelve. La puerta del **Gran Premio** pide `objetivoTickets` (1500) y el regalo, quedarse 2 s
  dentro: de momento eso es **escapar** (no hay siguiente tramo todavía). HUD arriba a la derecha
  (`FiestaCliente`: monedas, tickets/objetivo y barra) y la hoja de misiones con el radar (ficha más
  cercana o la puerta del premio).
- **Semáforo** (`JuegoSemaforo`): luz verde/roja con tiempos al azar, voz y musiquilla; si te mueves
  en rojo pierdes 25 tickets y vuelves a la salida; meta: 300 tickets (150 las siguientes veces).
- **El Anfitrión** (`Anfitrion`, `AnfitrionFiesta`; modelo en `tools/texturas/entidades.js`): mascota
  de fieltro alta y flaca, ojos de botón que brillan a oscuras. **Apagones** cada 5-9 min (35-50 s):
  sale a cazar (vista 22, oído 12, te olvida a los 4 s) y si te alcanza te **abraza** y mueres
  («se quedó en la fiesta para siempre»). Con luz, **apariciones**: te mira de lejos y se va al
  mirarle. La cordura baja como en las Piscinas (x2 en el apagón, x4 si te caza).
- **Configurable** sin recompilar: `config/backrooms-fiesta.json` (`ConfigFiesta`; `/backrooms fiesta
  recargar`). Comandos: `/backrooms fiesta [jugador] | ir <sala> | apagon [s] | aparicion | tickets|monedas
  <n> [jugador] | ver [jugador] | recargar`.
- **Recursos**: texturas y modelos en `tools/texturas/fiesta.js` (papel de globos y de estrellas, zócalo
  de rayas, goma de puzle, moqueta de confeti, acolchados, baldosa, 8 carteles, 8 dibujos de niños
  —los últimos, inquietantes—, globos, guirnaldas, mesas, tartas, regalos, sillitas, peluches,
  ollas, bandejas, camitas, tornos con su precio, semáforo, líneas, moneda y tickets). Sonidos en
  `tools/sonidos/fiesta.js` (aire, música de cajita lejana y desafinada, globos, juguetes, moneda,
  tickets, tornos, semáforo, el Anfitrión...) y ambiente en el cliente (`AmbienteFiesta`: cada cosa
  suena suelta, en 3D y a su aire; la música se calla en el apagón).
- **Voces**: las 9 del animador (bienvenida, 5 anuncios —uno se corta—, apagón, vuelve la luz y
  premio) con Higgsfield (voz Juan). **Se acabaron los créditos de Higgsfield** antes de las de la
  niña y el Anfitrión: `voces.js` ya sabe montarlas en cuanto estén los wav (nombres abajo); mientras,
  suenan los efectos sintetizados (señal del semáforo, silbato, fanfarria, risa sintética).
  Faltan, en `tools/sonidos/voces/`: `semaforo_verde` («¡Luz verde!»), `semaforo_roja` («¡Luz roja!»),
  `semaforo_pillado` («¡Te he visto! Vuelve a empezar.»), `semaforo_gana` («¡Has ganado! ¡Toma tus
  tickets!»), `fiesta_nino_1..4` («¿Quieres jugar conmigo?», «Jijiji... ¡te encontré!», «No te vayas
  todavía... la fiesta acaba de empezar.», «Mi mamá también vino a la fiesta. Todavía no ha salido.»)
  con voz de niña (Pixie), y `anfitrion_voz_1..3` («¿Dónde estás, amiguito?», «No te pierdas la
  fiesta...», «Ven... te guardé un trozo de tarta.») y `anfitrion_risa_1..2` con voz grave (Juan).
**Probado** (capturas): de las Piscinas a la Fiesta por la cabina, la llegada y el título, HUD y hoja,
coger moneda y tickets, pagar un torno, el semáforo (castigo en rojo y premio en la meta), el apagón
(oscuridad), el Anfitrión cazando y abrazando (mató al jugador de pruebas), canjear el Gran Premio y
escapar, y todas las salas a la vista.
**Sin probar**: con varios jugadores, el semáforo andando de verdad (solo con teletransportes), los
sonidos escuchados, las apariciones vistas (se va al mirarle: no salió en la captura), el laberinto
recorrido entero.
**Falta** (tras la 0.22.0): el siguiente tramo tras el tobogán del final.

## 0.19.0: la lista larga de Bruno (2026-10-17; Bruno)

- **Entrar**: todos (menos quien ya escapó) aparecen en la **recepción** (`Vestibulo#alEntrar`).
  **Cola de entrada** (`ColaEntrada`, `ColaLoginMixin`, `ColaCliente`, `ColaConexionMixin`): entra
  uno cada 3 s; mientras espera, la pantalla de conexión dice «SALA DE ESPERA · PUESTO N DE M · Entras
  en unos X s» (y no le echa a los 30 s). El staff de `backrooms-acceso.json` y los OP se la saltan.
- **Megafonía de la recepción**: cada 2 min, el mismo aviso a la vez para todos (lo elige el
  servidor por turnos: `Sonidos.MEGAFONIA_RECEPCION`). Ya no manda al auditorio: la linterna y tres
  avisos nuevos de espera (voz Marisol de Higgsfield, `tools/sonidos/voces/megafonia_recepcion_*.wav`).
- **Megáfono del staff** (`/backrooms megafono`, `MegafonoStaff` + `VozCinematica`): lo que dice el
  staff por el chat de voz lo oyen todos, sin distancia; se apaga con el mismo comando.
- **Cinemática del /start**: se lee el contrato 20 s hasta la firma (antes 12): todo lo de después
  va 8 s más tarde (`PRO` = 44,5 en `cinematica.fsh`, `CinematicaCliente`, `Expedicion.RETRASO_VIAJE`
  = 890 + 820, subtítulos y `tools/sonidos/prologo.js`; nuevo «Tómese su tiempo»). El display del
  ascensor marca **128** (la planta de la firma) y baja planta a planta; en la caída cuenta hasta **0**.
  **Pasos** nuevos: talón y punta, ruido filtrado sin chasquido, cada pisada distinta.
- **Misiones en orden** (`Misiones#completar`): solo avanza la que está en curso; grabar algo de una
  posterior no cuenta, y el «OBJETIVO» del visor de la cámara solo sale si la misión en curso es de
  grabar. Piscinas: válvulas → Serpiente → salida (si ya la abrieron otros, se da hecha al llegar).
- **Papel de misiones** tan alto como lo que lleva (`HojaMisiones#alto`): sin hueco blanco.
- **Linterna solo con F y cámara solo con C**: el clic derecho no hace nada. **Sin mano izquierda**: la
  F de Minecraft no cambia de mano, el hueco no sale en el inventario y lo que acabe ahí vuelve a la
  mochila (`HerramientasServidor#sinManoIzquierda`; el staff en creativo, sí).
- **Modelos 3D** (`tools/texturas/objetos3d.js`, en `npm run texturas`): linterna de aluminio negro
  (lente con reflector; encendida, blanca), videocámara (objetivo, pantalla abatible, visor, correa,
  piloto REC) y botella de agua de almendras (etiqueta y tapón azul). Fuera los dibujos planos.
- **Al morir**: la comida y el agua de almendras se caen donde mueres (`Comida#soltarComida`); el
  traje, la linterna y la cámara se quedan (keep_inventory); la tarjeta ya se caía.
- **Cadáveres**: ninguno con parte del cuerpo sobre el agua (parecía que flotaban), +17 % en las
  Piscinas, los colgados con la cuerda a la mitad, y **cadáveres en el Nivel 0** (~1 de cada 8
  cuadros de 24 x 24; tirados o sentados, alguno sin brazo o pierna; nunca colgados).
- **Entidades**: cada 3 min con gente aparece una más por nivel (Bacteria en el Nivel 0, Serpiente
  en las Piscinas), hasta una más por jugador (`Acechadores#refuerzos`).
- **Tarjetas**: brillo de su color (halo y columna de luz, partículas) que se ve a 160 bloques, solo
  para quien tiene en curso la misión de la salida.
- **Inundación nueva** (`Inundacion`): cada 10-15 min. 20 s de alarma roja y sirena con la voz
  «fallo en el sistema de drenaje, busquen refugio en las plantas superiores»; el agua sube 7 bloques
  en 40 s (con la luz roja), se queda 40 s, la voz dice «avería contenida» y baja en 40 s mientras se
  apaga la alarma. El agua **cuenta como agua de Minecraft** (`InundacionAguaMixin`: se nada, se hunde,
  se ahoga con sus burbujas, sin daño de caída; `InundacionCamaraMixin`: la niebla de debajo del agua)
  y la superficie es la **textura del agua de Minecraft**, pegada al mundo (antes se movía con la
  cámara). Mientras dura, **las Serpientes no cazan** ni aparecen nuevas.
- **Piscinas sin alarmas sueltas**: la luz roja solo llega con la inundación. **Arreglado**: la sirena
  no sonaba a quien llegaba con la alarma ya puesta (Minecraft corta los sonidos al cambiar de mundo).
- La megafonía de la alarma («brecha de contención», que nombra el Nivel 0) ya no suena en las Piscinas.
- La Serpiente va 3 px más alta en el azulejo (las manos se metían en el suelo).
- `/backrooms reparar [radio]`: deja los chunks de alrededor como los hizo el generador. Con él se
  arregló la pared de -1014 33 -615 del mundo de pruebas (el bloque invisible de «escondido» ya no está).
- **Launcher**: fuera el rótulo «próximo evento · Nivel 37»; el fondo va pasando por los niveles
  (Nivel 0 y Piscinas), 40 s cada uno.
- La comida +10 % ya iba en la 0.18.0.
**Probado** (capturas en el cliente de pruebas): entrar sin OP (por la cola) y aparecer en la
recepción, la inundación entera (alarma, subida, debajo del agua con burbujas y ahogándose, la
superficie de agua desde arriba, la bajada), papel de misiones y su orden, misiones en orden
(completar), brillo de las tarjetas, modelos 3D en la mano, en tercera persona y en el inventario,
la cinemática (lectura de 20 s, firma, display 128 → 0) y `/backrooms reparar`.
**Sin probar**: la pantalla de la cola con gente esperando (hace falta más de un cliente), el megáfono
del staff (dos clientes con voz), los avisos de la recepción cada 2 min y las voces sonando, los
pasos nuevos escuchados, morir y que caiga la comida, la mano izquierda con objetos, los refuerzos de
entidades cada 3 min, los cadáveres del Nivel 0 y los de las Piscinas sin flotar vistos en el juego,
la Serpiente más alta, el launcher con el fondo de 40 s.
**Para el servidor**: el mismo jar 0.19.0 en el servidor y en el pack.

## 0.18.0: lo que pidió Bruno tras la beta #2 (2026-10-17; Bruno)

- **Jugadores invisibles o quietos** (en la recepción, tras reconectar o al entrar gente): fuera
  **EntityCulling** (deja de dibujar y de animar lo que cree que no se ve) y **Krypton** (toca cómo
  el servidor manda los jugadores a cada cliente), del pack (fichas beta y local) y del servidor.
  Y al entrar ya no se teletransporta en el mismo tick del login: un segundo después
  (`Vestibulo#tickEntrando`). Sin poder comprobarlo aquí (hace falta dos clientes).
- **Sin PvP en todo el evento** (`SinPvp`): ni golpes ni daño de otro jugador.
- **Cinemática del /start**: la cámara va quieta (sin temblor de mano ni rayas de la cinta) hasta
  que el ascensor falla; el investigador sale por el centro de la puerta (ruta nueva de 8 puntos)
  y la hoja de la puerta abre hacia el pasillo.
- **Comida +10 %** en el Nivel 0 y en las Piscinas (galletas y pizza; el agua de almendras igual).
- **Traje**: visor transparente (se ve la cara de quien lo lleva; hueco recortado con marco y un
  reflejo) y **acreditación en el pecho** con el nick (`TarjetaPecho`); a quien lleva la chaqueta
  no se le ve el nombre encima (`NombreTrajeMixin`). En la recepción, sin traje, nombre normal.
- **Alarma roja en las Piscinas** (cada 7-12 min, 35-55 s; `/backrooms alarma` también allí):
  la Serpiente va a la base en el azulejo y +35 % en el agua (lo que dijo Bruno: «agua 35 %, suelo
  normal»). Nunca a la vez que la inundación.
- **Papel de misiones** (`PapelMisiones`, tecla **M**): bollito de papel arriba a la izquierda; con
  la M se estira y muestra la hoja (con el detector) mientras te mueves. Ya no está en el
  inventario. Orden de pruebas `misiones`.
- **Tarjetas**: se ven para todos, pero solo las coge quien cerró sus válvulas y grabó a la
  Serpiente (si no, aviso). Quien lleva una sale en el **TAB con su color** (equipos del marcador
  `br_tarjeta_*`). El TAB va más junto (filas a la mitad de hueco).
- **Inundación de las Piscinas** (`Inundacion`, `InundacionCliente`, `efectos.fsh`): aviso por
  megafonía y el agua sube por la planta baja en 80 s hasta y = 46,5 (por encima de la cabeza en el
  suelo), se queda 50 s y baja en 40. Solo se salva quien sube a una planta alta (caracoles y
  terrazas, se pisa en 49). Cada 14-22 min; `/backrooms inundacion [segundos|parar]`. El agua NO son
  bloques: es una altura que el servidor manda y el cliente dibuja (superficie con olas y espuma,
  y azul verdoso por debajo); el aire y el ahogo los lleva el servidor. Sonido
  `tools/sonidos/inundacion.js`.
- **Sin chat ni voz en las cinemáticas** (`EnCinematica`, `VozCinematica`: plugin de Simple Voice
  Chat con su API, solo para compilar): quien la ve ni habla ni oye; moverse ya no podía.
- **Salida de las Piscinas = ascensor**: detrás de la puerta (que abierta deja de verse) hay una
  cabina de acero (`PlanoPiscinas#cabina`); quien tiene sus misiones entra y a los 1,5 s sube y
  escapa. **Arreglado** en las pruebas: escapar desde la cabina tumbaba el servidor
  (ConcurrentModificationException al recorrer `nivel.players()`).
- **Recepción de 50 x 50** (140 butacas, columnas, pasillos) y **música tenue** en bucle dentro
  (`tools/sonidos/recepcion.js`, `MusicaRecepcion`).
**Probado** (capturas en el cliente de pruebas): TAB, papel con la M, visor y acreditación,
recepción nueva, inundación (desde arriba, a ras y por debajo), alarma roja en las Piscinas,
cabina y escapar (dos veces, sin caída), la cinemática arranca y el investigador por la puerta.
**Sin probar**: con dos jugadores (invisibles, PvP, voz y chat en la cinemática, color del TAB al
coger tarjeta de verdad), ahogarse en la inundación sin ser creativo, el aviso de las tarjetas sin
misiones, la velocidad de la Serpiente con alarma, la música y el agua sonando.
**Para el servidor**: mundo nuevo (cambió la recepción y las Piscinas); quitar `krypton` de `mods/`.

## Beta #2 (2026-10-16; Bruno) y mod 0.17.1

Hosting nuevo **66.51.99.171:25626** (lo pasó EdiGuard). Acceso con **códigos de la tienda**.
- **0.17.1**: la banda de la cinemática vuelve a ir **sin "stream"** (en la 0.17.0 se le puso; ver
  el traje morado de la beta #1). Probado: `/start` real → Nivel 0 con el traje y texturas bien y
  sin ClosedByInterrupt en el log.
- **pack-beta re-publicado** (2026.10.16-6ba46353) con la ficha `pack/.evento-beta.json`
  (rehecha a partir del manifest de la beta #1: servidor nuevo, noticia «Beta #2», staff xgdier_ y
  Bruno2524f, acceso de la tienda). El pack público (`pack`) no se tocó.
- **Launcher de la beta** con `--nombre="BETA #2"` → `Downloads/Backrooms-Beta2-Setup.exe` (no se
  publica). El de la beta #1 también sirve (baja el pack-beta solo). Sin abrirlo en este PC.
- **Servidor**: `Downloads/Backrooms-servidor-beta2-0.17.1.zip` (rutas con «/», hecho con tar):
  fabric-server.jar, el mismo jar del mod que el pack, Fabric API, Lithium, FerriteCore, Krypton y
  Simple Voice Chat (voz por el mismo puerto, UDP), `server-port=25626`, sin eula y con
  LEEME-HOSTING.txt. **Falta** pegar el `BACKROOMS_SERVIDOR_TOKEN` (lo tiene EdiGuard) en
  `config/backrooms-acceso.json` y generar los códigos de beta en el panel de la tienda.
**Sin probar**: el servidor en el hosting, el canje de códigos de esta beta y la voz por UDP allí.

## 0.17.0: la cinemática del /start en 3D y de un solo plano (2026-10-15; Bruno)

Bruno pidió fundir el prólogo 2D con el ascensor (el corte quedaba raro), hacerlo en 3D y con más
calidad, que la hoja tarde **12 s** desde que se abre hasta la firma y un bolígrafo de verdad.
- **Todo es una sola toma** dentro del shader del ascensor (`cinematica.fsh`, guion 0), en primera
  persona y con el HUD de la videocámara: sala de reuniones 3 (bloques de la recepción: zócalo de
  andesita, franja cian, moqueta gris con borde cian, paneles de luz, pizarra con «FASE 1:
  OBSERVACIÓN», reloj a las 8:59, placa, librería, macetas) → el investigador (modelo de Minecraft
  con bata, corbata, acreditación y gafas que reflejan; anda, te mira, te acerca el papel y el
  bolígrafo) → **la hoja** de cerca: de la fila de arriba a la línea de la firma en 12 s (6 → 18 s)
  → tu mano coge el bolígrafo y **firma tu nick trazo a trazo** con rúbrica → se lleva el papel en
  su tablilla, «Acompáñeme» → le sigues por el pasillo (puertas de ARCHIVO y LABORATORIO 2, banco,
  extintor) → llama al ascensor, «Pase, por favor» → entras, **te das la vuelta y ves el pasillo
  por el que viniste**, con él delante de las puertas: «Le veremos al otro lado» → se cierran y
  sigue la bajada de siempre (misma cabina, sin corte).
- **El contrato** lo pinta `LienzoCinematica` al empezar (Sampler1 del shader, 512x512): la hoja con
  el nick, la fecha y el expediente, escrita con las imágenes de la fuente de Minecraft (con tildes),
  la máscara y el recorrido de la firma, y los carteles. **El bolígrafo** es 3D (por distancias): bola
  y punta de metal, cono, agarre de goma, cuerpo hexagonal con «UMBRAL» impreso, capuchón azul con
  clip, todo pixelado (~110 px de largo).
- Fuera `PantallaPrologo` y `cinematica_prologo.ogg`: el sonido del prólogo (`tools/sonidos/prologo.js`,
  con los pasos donde caen en los recorridos del shader) va delante del ascensor en `cinematica.ogg`
  (92 s). `CinematicaCliente.PRO` = 36.5 s; `Expedicion.RETRASO_VIAJE` = 36.5 s + 41 s.
- Para comprobar el shader sin abrir Minecraft se puede compilar con el WebGL2 de Electron (GLSL ES;
  ojo: `T` y `G` son macros del shader, no valen como nombres).
**Probado** (capturas en el cliente de pruebas): toda la secuencia, la firma con el nick, el giro
dentro del ascensor viendo el pasillo, la bajada y el `/start` real desde la recepción hasta caer en
el Nivel 0. **Sin probar**: cómo suena en el juego (la mezcla solo se revisó por niveles), nicks
muy largos (la firma se encoge), con varios jugadores a la vez.

## 0.16.0: recepción, prólogo de la firma y el vestíbulo como sala del final (2026-10-15; Bruno)

Lo que pidió Bruno para el principio del evento (LORE.md, 7):
- **Recepción** (`PlanoVestibulo#recepcion`, otro edificio en la dimensión del vestíbulo, x 100..134):
  sala de espera de oficina con techo de paneles de luz, mostrador con la pantalla del «PROYECTO
  UMBRAL», filas de sillas sobre moqueta gris, máquinas de agua, puertas (sala de reuniones,
  personal, aseos, entrada; de hierro, no se abren) y un rincón de instrucciones (normas y
  controles). **Al entrar al servidor por primera vez se aparece aquí** (`Vestibulo#alEntrar`; quien
  ya jugó va al vestíbulo). `/backrooms recepcion [jugador]`.
- **/start** ya no abre ascensores: lanza la cinemática a todos los de la recepción (`/start todos`
  y `/start <jugadores>` siguen). Cada uno ve el **prólogo** (`PantallaPrologo`, 26 s, dibujado en
  2D con sonido `cinematica.prologo` de `tools/sonidos/prologo.js`): la sala de reuniones con
  alguien de bata sin cara, el contrato «CONSENTIMIENTO INFORMADO» de cerca (la cláusula de las
  consecuencias marcada) y **su nick firmado a mano**, «Acompáñeme», el pasillo y el ascensor que se
  abre; sigue sin corte la cinemática del ascensor de siempre y cae al Nivel 0
  (`Expedicion.RETRASO_VIAJE` = 26 s + 41 s).
- **El vestíbulo es la sala del final** (adonde van quien escapa y quien cae): sin mostrador, sin
  taquillas, sin tablones de instrucciones y sin los ascensores al Nivel 0 (ni su hueco).
**Probado**: la recepción a la vista (capturas), /start desde la recepción → prólogo → ascensor →
Nivel 0, y la cinemática completa con capturas. **Sin probar**: con muchos jugadores a la vez,
sentarse en las sillas de la recepción, y que alguien que entra con el evento ya empezado espere
bien en la recepción. **Para pedir a EdiGuard**: que vea el cambio del vestíbulo (era el inicio).

## 0.15.0: cadáveres, válvulas suaves y la Serpiente por los techos bajos (2026-10-14; Bruno)

- **Cadáveres** en las Piscinas (~1 de cada 30 celdas; `PlanoPiscinas#cadaverEnCelda`, `Cadaveres`,
  `CadaverEntidad`, `CadaverRenderer`/`ModeloCadaver`): boca arriba, boca abajo, sentados contra la
  pared o, en salas de techo alto, colgados de los pies o del cuello con una cuerda; a algunos les
  falta un brazo o una pierna; charco de sangre debajo. Cinco pieles hechas por código
  (`tools/texturas/cadaveres.js`): tres al estilo de las fotos que mandó Bruno (pelo gris y jersey de
  rayas; figura negra con manchas verde agua; negra con ojos y sonrisa rojos) y dos exploradores con
  el traje amarillo. **Las skins de NameMC no se usan** (son de sus autores; regla de no usar
  assets de terceros). `/backrooms piscinas ir cadaver [colgado]`.
- **Válvulas**: el volante gira suave (block entity `ValvulaEntidad` + `ValvulaRenderer`; el
  bloque es solo el tubo) y lo girado **no se pierde al soltar** (se acumula por jugador y válvula;
  se pierde si se reinicia el servidor). Cierre nuevo con carácter de freno de aire (sintetizado;
  la referencia de Epidemic no se usa).
- **Techos bajos**: en las terrazas hay pasos de 2,5 de alto (bajo las losas de los arcos) y en el
  caracol de 3; la Serpiente (caja 1,5x3,2) no pasaba. Caja nueva 1,2x2,4 (el modelo igual).
- Velocidad en el agua: ya era +25 % sobre la base (6,9 bloques/s; medido 7,0).
**Probado** (capturas): cuerpo tumbado, colgado de los pies con su cuerda y la sangre debajo; el
volante gira suave y el cierre se completa en dos tandas de clic. **Sin probar**: sentados y colgados
del cuello a la vista, la Serpiente pasando por las terrazas bajas, el sonido nuevo en el juego.

## 0.14.1: sonidos de las Piscinas que pidió cambiar Bruno (2026-10-13; Bruno)

Bruno escuchó la hoja de sonidos y pidió quitar las gotas 4-6 (las del azulejo), toda la orilla y
el chapoteo 3, y rehacer los pasos y las brazadas. Mandó cuatro efectos de Epidemic Sound como
referencia (olas contra un muro, un chapuzón, pasos en un almacén, agua con salpicaduras); **no se
usan**: son de pago, la licencia normal no cubre meterlos en un juego que se reparte, el repo es
público y la regla del organizador es todo por código. Se sintetizaron parecidos (`nivel37.js`):
- `piscinas_olas1-3` (evento `piscinas.orilla`): una ola que rompe contra el borde; cada 15-40 s
  cerca de una piscina (antes la orilla, cada 7-18 s).
- `piscinas_zambullida1-2` (`piscinas.zambullida`, nuevo): al tirarse o caer al agua en las
  Piscinas (`PiscinasJugadorMixin`, en lugar del chapuzón de Minecraft).
- `piscinas_paso1-6`: pasos de nave enorme con eco. `piscinas_nado1-3`: brazadas nuevas.
Hoja actualizada (https://claude.ai/artifact/YJZ67xfhtovSKRR1HaiW5L). **Sin probar de oído en el juego.**

## 0.14.0: las misiones de las Piscinas, Serpiente sin patinar y azulejos (2026-10-12; Bruno)

**Misiones de las Piscinas** (las da `Misiones#asignarPiscinas` al bajar en el ascensor;
sustituyen a las del Nivel 0):
1. **Cierra tus válvulas** (individual). Hay 25 en el mapa, una por casilla de una cuadrícula
   de 5x5 sobre los 1.800x1.800 bloques donde se llega (`PlanoPiscinas#valvulas`, en paredes con
   suelo seco; ~360 entre vecinas). A cada uno le tocan 3 al azar entre las 7 más cercanas a
   donde llega (`piscinas/Valvulas`). Se cierran manteniendo el clic derecho 3 s: el volante
   rojo gira (cuatro posiciones del modelo), suena el metal, sale la barra «CERRANDO» y al
   final el siseo de la presión con vapor. Con las 3, una maquinaria lejana (solo la oye él).
   Las de otros están «atascadas». El radar de la hoja de misiones (E) marca su válvula más
   cercana con los metros («SEÑAL DE VÁLVULA 357 M»).
2. **Graba a la Serpiente** (individual): 4 s en el encuadre de la cámara (C) a menos de 30
   bloques; al acabar, el visor muestra «EVIDENCIA OBTENIDA / SUJETO CONFIRMADO» (`red/Evidencia`).
3. **Abre la salida de emergencia** (de todos). Solo hay **3 tarjetas** (roja, amarilla,
   verde) en todo el nivel (`piscinas/Tarjetas`, estado en `backrooms/tarjetas.json`), cada una
   más lejos y en peor sitio: la verde a ~230 bloques de la puerta en una sala con luz, la
   amarilla a ~450 en una sala a oscuras y la roja a ~650 en el fondo de una piscina honda,
   guardada por una Serpiente que no se aleja de ella. Al cogerla sale en el chat «X ha
   obtenido la tarjeta roja». Si quien la lleva muere, cae donde murió (el evento juega con
   `keep_inventory`, así que se le quita a mano) y sale en el chat; si se desconecta, vuelve a
   su escondite. La puerta de emergencia (2x3, en un pasillo cerca del centro del mapa) tiene un
   lector por color al lado y un cartel verde grande encima; por las paredes de alrededor
   (~220 carteles en 10 celdas a la redonda) hay carteles de salida con la flecha hacia ella.
   Con las 3 metidas la puerta se abre para siempre (chat en dorado y sonido) y la misión queda
   hecha para todos. **Cruzar**: solo quien tiene sus misiones hechas (a los demás la puerta
   abierta les sigue cerrando el paso: «ACCESO DENEGADO»). Como dijo Bruno, el final queda para
   el siguiente nivel: de momento cruzar es escapar (`Tarjetas#cruzar`, una línea que cambiar).

**La Serpiente**: en el agua un 25 % más rápida que la base (6,9 bloques/s; medido: 7,0) y ya
no patina: gira como la Bacteria (90 grados por tick en seco, 35 nadando, antes 14) y en el
agua frena enseguida (conserva el 75 % de la velocidad por tick, antes el 90 %); de cerca
rehace el camino cada 2-3 ticks. Sin probar a fondo cómo se ve girando en seco.

**Azulejos**: la junta ahora va a los dos lados de cada pieza (2 px entre piezas y entre
bloques): la textura es simétrica y casan las esquinas entre la cara de arriba y las de los
lados (Minecraft pinta las de los lados como en un espejo y antes quedaban corridas un píxel),
y todas las líneas miden igual (antes la sombra del bisel engordaba una).

**Sonidos**: hoja para escucharlos todos con cuándo suenan y cuánto duran (privada, del
usuario): https://claude.ai/artifact/YJZ67xfhtovSKRR1HaiW5L . Bruno va a marcar los que
quiere cambiar. Nuevos: `valvula_giro1-3`, `valvula_cierre`, `valvula_maquinaria`,
`tarjeta_coger`, `lector_acepta`, `puerta_salida` (en `tools/sonidos/nivel37.js`).

Comandos nuevos: `/backrooms piscinas ir valvula|salida|tarjeta_roja|tarjeta_amarilla|tarjeta_verde`
y `/backrooms tarjetas [reponer|abrir]`. Orden de prueba nueva: `usar [s]` (clic derecho mantenido).

**Probado en el servidor local** (capturas): la puerta, los lectores y los carteles en su sitio;
la válvula gira, se cierra y luego dice «Esta ya la cerraste»; la hoja cuenta «VÁLVULAS 1/3» y
el radar marca la válvula; la tarjeta verde flota con luz, se coge y sale en el chat; meterla
enciende su lector y cuenta 1/3; abierta la puerta, sin misiones no deja pasar y con ellas se
cruza (escapa); la tarjeta cae donde muere quien la lleva y sale en el chat; la grabación de la
Serpiente da la evidencia; la roja está en el fondo de la
piscina y su guardiana aparece y ronda a menos de 15 bloques; la Serpiente nada a 7 bloques/s.
**Sin probar**: soltarla con Q, desconectarse llevándola, la maquinaria al cerrar las tres, cómo suenan los
sonidos nuevos en el juego y varias personas a la vez con las tarjetas.

## 0.13.0: pulido de las Piscinas y la Serpiente con IA (2026-10-11; Bruno)

Lo del documento «ACTUALIZACIÓN Y PULIDO — POOLROOMS» de Bruno (las misiones van aparte, después).

- **Luces flotantes** (la foto de Bruno, en el túnel): `PlanoPiscinas#techoSobre` contaba las
  luces invisibles como techo y colgaba claraboyas en el aire. Arreglado; comprobado fuera del
  juego (0 de 45.000 luces) y en el túnel.
- **Sonido**: fuera el ambiente en bucle (el bioma ya no tiene `ambient_sounds`). Ahora suenan
  cosas sueltas, en 3D y cada una a su ritmo (`cliente/piscinas/AmbientePiscinas`; tiempos en su
  cabecera): gotas, agua que se mueve, el agua del borde (solo cerca de una piscina), chapoteos,
  tuberías, algo grave lejos y el gruñido de la Serpiente cada 1-2 min a 55-95 bloques (no quiere
  decir que esté cerca). Rarísimos: una voz humana lejana (12-40 min) y **un susurro con el
  nombre de quien lo oye**, justo detrás (25-75 min): el nombre se sintetiza en su cliente
  (`Susurrador`, sin grabaciones; se lee como en español) y nunca suena para todos a la vez.
  Pasos propios en el azulejo y en el agua (`AZULEJOS`, `PiscinasJugadorMixin`). Sonidos en
  `tools/sonidos/nivel37.js` (todos sintetizados).
- **Luces que fallan**: 1 de cada ~75 paneles (`LuzParpadeante`): de vez en cuando chasquea,
  se apaga un instante y vuelve; cada panel con su propio nervio.
- **Detalles raros** (`PlanoPiscinas#detallesCelda`, ~1 de cada 4 celdas tiene algo, casi
  siempre una sola cosa): manchas, azulejo con sangre, marcas de manos, arañazos, conteos,
  espirales, carteles viejos, suciedad, charcos, escombros, huellas, objetos abandonados
  (flotador, toalla, chanclas, mochila) y, muy de vez en cuando (~1 %), **CORRE** escrito a
  mano en tres azulejos. Texturas en `tools/texturas/piscinas.js`.
- **Comida y cordura** como en el Nivel 0: galletas y pizza en ~17 % de las celdas, aguas de
  almendras al llegar, y la cordura baja igual (`Supervivencia#locuraPiscinas`).
- **Agua pesada**: los jugadores van un 10 % más lentos en el agua de las Piscinas
  (`AguaPiscinasMixin`, en cliente y servidor).
- **La Serpiente con IA** (`entidad/Serpiente`): ronda despacio (la mitad de los paseos tiran
  hacia el jugador más cercano, sin cazarle), detecta a 25 bloques si te ve (a 8 aunque no),
  va a por el más cercano y solo cambia de presa si otro está claramente más cerca (3 bloques,
  no más de una vez cada 2 s). Velocidad: 5,5 bloques/s de base (la de la Bacteria), −15 % en
  el azulejo (4,7: corriendo se le escapa uno) y +15 % en el agua (6,3). Sale del agua por la
  orilla. **Invulnerable** (solo `/kill`). Una por cada 10 jugadores de la expedición en las
  Piscinas, mínimo una (`piscinas/Habitantes`): aparece a 45-75 bloques y se va si no queda
  nadie a 140. Con `/backrooms acechadores false` no sale ninguna.
- **Muerte**: se echa atrás, se lanza, muerde y sacude; a quien muere la vista se le va al
  cráneo, tirón y pantalla roja en el mordisco, fundido a negro y muere devorado
  (`red/Mordisco`, `cliente/piscinas/MordiscoCliente`). Luego la Serpiente sigue a lo suyo.
  Animaciones propias para quieta, nadando/andando, cazando y atacando (`ModeloSerpiente`).
- **Modelo mejor**: cráneo con relieve, colmillos, púas en el lomo, brazos más largos y garras
  más grandes (`tools/texturas/entidades.js`).
- **Comida que no volvía** (también en el Nivel 0): si se ponía justo al llegar o reaparecer,
  con el chunk aún sin cargar, se quedaba fuera del mundo y esa comida ya no salía. Ahora
  espera a que el chunk esté cargado (`Comida#materializar`).
- Comandos nuevos: `/backrooms piscinas oir <sonido> <jugador>` (gota, agua, orilla, chapoteo,
  tuberia, grave, grunido, voz, susurro) y `/backrooms piscinas ir detalle|corre`.

**Probado en el servidor local**: túnel sin luces flotantes; CORRE legible; los nueve sonidos
sueltos se lanzan sin avisos en el log; la Serpiente aparece sola a ~70 bloques, ronda y se
para, ve al jugador, le persigue nadando, sale de la piscina y le mata con la secuencia entera
(capturas: vista al cráneo, mordisco en rojo, negro, «HAS CAÍDO»); la cordura baja al verla; la
comida aparece tras teletransportes y reapariciones; en el agua se va un 10 % más lento
(medido: 3,52 bloques en 2 s, lo esperado con el −10 %). El susurro se probó fuera del juego
(mismo volumen que los susurros del Nivel 0).
**Sin probar de oído** (desde aquí no se puede grabar el audio del juego): cómo suenan en
partida los sonidos sueltos, el susurro con el nombre y los pasos. **Sin probar**: el parpadeo
de un panel en el juego (es como el tubo del Nivel 0) y varias Serpientes con muchos jugadores.
**Ojo**: los jugadores pueden ahogarse en las piscinas hondas (3 de agua) si se quedan en el fondo.

**Las 3 misiones de las Piscinas** (hechas en 0.14.0, ver arriba) se pidieron así:
Hoy al llegar a las Piscinas se conservan las misiones del Nivel 0 (`Misiones.Estado`; `TipoMision`
solo tiene las del Nivel 0) y no hay salida.
1. **Cerrar 3 válvulas** a ~150-200 bloques entre sí, en zonas distintas: animación de giro,
   sonido metálico al girar y de presión al cerrar; con las 3, ruido de maquinaria y se activa
   una parte del mapa.
2. **Grabar a la Serpiente** unos segundos sin perderla del encuadre (como `Grabacion#encuadra`);
   si te acercas demasiado puede desaparecer rápido; al acabar, la cámara muestra
   «EVIDENCIA OBTENIDA — SUJETO CONFIRMADO».
3. **Salida de emergencia**: carteles nuevos que apuntan a ella, 3 tarjetas escondidas en
   zonas cada vez más peligrosas y, con las 3, la puerta abre una **secuencia de escape** que
   hay que completar (no lleva directamente a otro nivel; al acabarla, `Fases#escapar`).
Piezas que hay: `Misiones` (estado, `completar`, `sincronizar` → `SyncMisiones` → `MisionesCliente`),
`Grabacion`, el radar, `Fases#escapar`. En `Fases#pulsar`/`tick` está la bajada a las Piscinas.
El entorno de pruebas quedó cerrado y el launcher otra vez en pantalla completa (para probar
con la ventana minimizada hay que ponerlo en VENTANA: en pantalla completa Windows la minimiza y
las capturas salen congeladas).

## 0.12.0, etapa 3: la entidad de las Piscinas (2026-10-11; Bruno)

**La Serpiente** (`backrooms_evento:serpiente`; nombre provisional, se cambia en `lang` y en la
clase), según la última imagen de referencia de Bruno: cuerpo larguísimo de azulejos blancos con
la junta azul y anillos de azulejo azul, que se alza del agua (la cabeza a ~3 bloques), cola en S
(unos 11 bloques en total), cuello con costillas al aire, un cráneo el doble de ancho que el
cuello con la mandíbula abierta y los **ojos cian que brillan** (capa de ojos, como el Smiler: se
ven a oscuras) y dos pares de brazos de hueso con garras. Modelo y textura generados en
`tools/texturas/entidades.js` (`MallaSerpiente.java`, `serpiente.png`, `serpiente_brillo.png`);
clases `entidad/Serpiente`, `cliente/entidad/SerpienteRenderer` y `ModeloSerpiente`.

Solo modelo e integración, como se pidió: **sin IA, sin animaciones y sin sonidos** (NoAI,
silenciosa). No aparece sola en ningún sitio (ni en el Nivel 0 ni en las Piscinas); el staff la
pone con `/backrooms invocar serpiente` (7 bloques por delante, mirándote; sobre el agua queda
medio hundida). Es invulnerable (salvo `/kill`), no se puede empujar ni desaparece al alejarse.
La caja de choque cubre la parte alzada (1,5 x 3,2); la cola no choca.
**Probado** en el servidor local: compila, el servidor arranca sin errores, se invoca en una sala
de piscina y se ve de frente, de perfil y desde debajo del agua con los ojos brillando (capturas).
Queda una puesta en el mundo de pruebas, en las Piscinas (-86.5, 40.4, 408.5).
**Etapa 4 (pendiente)**: misiones de las Piscinas, sonidos propios (ambiente, la Serpiente),
animaciones (nadar, alzarse, mandíbula, brazos) y su IA, y la salida del nivel.

## 0.11.0, etapa 2: las Piscinas (Nivel 37) (2026-10-11; Bruno)

Nivel nuevo debajo del Nivel 0, hecho a partir de las imágenes de referencia de Bruno (el reel
de Instagram no se pudo ver: pide iniciar sesión). Paquete `evento/.../piscinas/`:

- **Dimensión** `backrooms_evento:piscinas` (`data/.../dimension`, `dimension_type`, bioma):
  64 de alto, suelo en y=40 (se pisa en 41), agua hasta y=40 (un pelo por debajo del borde),
  niebla cian clara, agua turquesa, ambiente en bucle = el de las Piscinas del menú
  (`piscinas.ambiente`, registrado en `Sonidos`; los sonidos propios del nivel son de la etapa 4).
- **Trazado** (`PlanoPiscinas`, sin Minecraft; `GeneradorPiscinas` lo pone): celdas de 24x24
  conectadas por puertas (siempre al menos una por celda), y 1 de cada 3 manzanas de 2x2 es una
  sala grande sin paredes. Un tipo de sala por cada imagen: **pasillos** de azulejo (a veces con
  canal), **piscina** de calles con columnas y paneles (ref. de la entidad), **sala inundada**
  con columnas redondas y claraboyas (ref. del flotador), **bóvedas** de arista sobre una piscina
  de borde curvo (ref. de los arcos azules), **túnel** redondo encharcado con anillos (ref. del
  túnel), **caracol**: escalera alrededor de una columna con balcón arriba (ref. de la escalera)
  y **terrazas** a dos alturas con arcos oscuros y un canal (ref. de la cinta VHS). Caracoles y
  terrazas tienen planta alta (y=49) y se comunican por arriba si están juntos. Algunas salas,
  a oscuras. Las curvas de bóvedas y túneles llevan escalones del revés como chaflán.
- **Bloques** (`BloquesPiscinas`; texturas y modelos en `tools/texturas/piscinas.js`, ya en
  `npm run texturas`): azulejo (limpio, sucio, roto, azul), fondo de piscina y franja de calle
  con cáusticas animadas, borde con junta azul, rejilla, panel de luz, claraboya, foco de
  piscina (lente que brilla), losa y escalón de azulejo. Luces invisibles (`minecraft:light`)
  para que las salas grandes tengan luz «sin fuente».
- **Luz fría**: en las Piscinas la luz de los bloques es blanca y las sombras azuladas (sin el
  amarillo de Minecraft). Va en el mapa de luz del mod (`assets/minecraft/shaders/core/
  lightmap.fsh`, que fuera de las Piscinas hace exactamente lo mismo que el de Minecraft) y
  `LightTextureMixin` (marca en el color de ambiente). Comprobado que el Nivel 0 y el
  vestíbulo se ven igual que antes.
- **Bajada**: el ascensor de salida de la fase 1, con las misiones hechas, ya no hace escapar:
  baja a las Piscinas con la cinemática del guion 2 (`Fases`; si el servidor no tiene la
  dimensión, se escapa como antes). En la cinemática, al abrirse las puertas abajo se ve una
  sala de las Piscinas (trazada en `cinematica.fsh`: piscina de calles, columnas, paneles,
  reflejos y cáusticas) con su luz fría; la banda sonora acaba con la depuradora, el agua, gotas
  con eco, un chapoteo lejano y pasos sobre azulejo (`tools/sonidos/cinematica-fases.js`).
- Las Piscinas **no son una Fase**: sin apagones, Bacteria, Smilers ni cordura. Quien se
  reconecta allí sigue allí; quien muere cuenta como en el Nivel 0 (eliminado y al vestíbulo;
  `Piscinas.delEvento` en `Vestibulo` y `Eliminacion`). Aún no hay misiones ni salida: quien
  baja se queda en las Piscinas (etapa 4).
- Staff: `/backrooms piscinas [jugador]` (a un sitio libre) y `/backrooms piscinas ir <sala>`
  (pasillos, piscina, inundada, bovedas, tunel, caracol, terrazas: la más cercana).

**Probado** en el servidor local de Bruno: el servidor arranca sin errores; los 7 tipos de sala
generan y se ven (capturas); `/start` → misiones completadas → `/backrooms bajar` → cinemática con
la vista de las Piscinas y «NIVEL 37 · LAS PISCINAS» → llega a las Piscinas; reconectar le deja
allí; morir allí → «ELIMINADO» y al vestíbulo; el Nivel 0 y el vestíbulo, sin cambios a la vista.
**Sin probar**: con mucha gente a la vez (el reparto usa un radio de 900 y 72 de separación),
y la planta alta de dos salas juntas (puertas de arriba) no se ha recorrido entera.

## 0.10.0, etapa 1: el Nivel 0 se queda con la fase 1 (2026-10-11; Bruno)

Cambio de planificación (Bruno, con EdiGuard al tanto): el Nivel 0 deja de tener 4 fases para
hacer sitio a niveles nuevos; el siguiente es **las Piscinas (Nivel 37)**. Quitado: las fases 2,
3 y 4 de `Fase.TODAS` y sus dimensiones (`data/.../dimension/fase_2..4.json`), la cinemática de
la bajada a la fase 3 (guion 3 de `cinematica.fsh`, su HUD «NIGHT SHOT», sus sonidos
`cinematica_fase3*.ogg` y su parte de `tools/sonidos/cinematica-fases.js`). Se queda todo lo de
la fase 1, y el sistema de fases (`Fase`/`Fases`) sigue admitiendo varias. La cinemática de la
fase 2 (guion 2) pasa a ser la bajada a las Piscinas: su rótulo final dice «NIVEL 37 · LAS
PISCINAS» y se ve con `/backrooms cinematica piscinas [jugador]` (antes `cinematica fase <n>`).
**Probado** en el servidor de Bruno: /start todos → Nivel 0 con sus 3 misiones y casetes, la
Bacteria aparece, el ascensor de salida deniega sin misiones y con ellas (antes de la etapa 2)
escapaba; la cinemática del guion 2 compila y se ve entera. En los mundos viejos quedan sin usar
las carpetas `dimensions/backrooms_evento/fase_2..4`: se pueden borrar.

## Nuevo lore: PROYECTO UMBRAL (2026-10-08; Bruno)

La historia oficial del evento a partir de ahora está en **[`LORE.md`](LORE.md)** (texto
completo, tal como lo entregó Bruno). En resumen:

- Unos científicos abren por accidente una grieta a **The Threshold**: las Backrooms, un
  espacio entre la realidad y lo que hay fuera de ella. Están **vivas**, el tiempo no corre
  igual dentro y quien se queda demasiado se **transforma en entidad** (algunas entidades
  fueron personas y recuerdan quiénes eran).
- El **Proyecto Umbral** (gobierno + empresas) usa «sujetos» que firman un contrato con una
  cláusula oculta. **El jugador es uno de ellos**: firma, se desmaya y despierta con otros
  participantes ante una pantalla: «PROYECTO UMBRAL · FASE DE OBSERVACIÓN INICIADA · SUJETOS
  CONFIRMADOS · OBJETIVO: ALCANZAR LA SALIDA». Las puertas se abren al Nivel 0.
- Cada nivel es una prueba de la organización (miedo, cooperación, confianza, memoria...), pero
  los niveles empiezan a cambiar solos: las Backrooms aprenden de los jugadores. Por el camino
  hay **documentos** de sujetos anteriores («SUJETO 018 — TRANSFORMACIÓN COMPLETA») cuyos
  nombres coinciden con entidades vistas.
- Final: un único superviviente pulsa el botón de la habitación blanca, despierta en un
  hospital, encuentra papel tapiz amarillo bajo la almohada y una pantalla de la organización:
  «SUJETO FINAL: EXITOSO · LOS DEMÁS SUJETOS PERMANECEN DENTRO». Cierre: «PROYECTO UMBRAL —
  FASE 2» y «LAS BACKROOMS NUNCA FUERON UN SUEÑO».

Aún no se ha adaptado nada del juego a esta historia. Choca o habría que repasar: el
vestíbulo hoy es el «Centro de Expediciones» (en el lore, la sala donde despiertan los sujetos
con la pantalla del Proyecto Umbral), la megafonía y las notas del Nivel 0, el texto de
eliminación («SEÑAL PERDIDA»), las pantallas de escapado y la cinemática del ascensor. Las
entidades actuales (Bacteria, Smiler) encajan como sujetos transformados.

## 0.9.12: cordura al empezar, panel, /start repetido y arranque en ventana (2026-10-07; Bruno)

- **Morir por cordura nada más llegar (arreglado y probado)**: la cordura se guarda con el
  jugador y nada la reponía al empezar otra expedición; quien acabó la anterior a 0 sin morir
  (misiones olvidadas, servidor reiniciado...) moría por «locura» al llegar al Nivel 0. Ahora
  `Expedicion.viajar` la pone a 100. Probado: con la cordura a 0 (`/backrooms cordura 0`), llega
  vivo y con 99,7 %.
- **Panel de los ascensores del vestíbulo**: una pulsación cada 3 s por jugador (se podía
  spamear el sonido). Compila; sin probar pulsándolo.
- **`/start` con los ascensores ya abiertos**: ya no vuelve a sonar la apertura ni a salir el
  aviso. Las puertas ya estaban cerradas hasta el `/start` (se cierran al arrancar el servidor;
  comprobado bloque a bloque: cerradas → `/start` → abiertas).
- **Launcher + mod: el juego arranca en ventana** (centrado, como Minecraft normal) y, si el
  jugador eligió PANTALLA COMPLETA, el mod la pone al acabar la carga (`BackroomsMenu`); antes
  el launcher pasaba `--fullscreen` y Windows cambiaba de modo de golpe al arrancar. El launcher
  manda `pantallaCompleta` en `config/backrooms-event.json` y pone `fullscreen:false` en
  `options.txt` antes de abrir (si no, una F11 de la vez anterior volvía a abrirlo a pantalla
  completa). Probado: ventana 1038x614 centrada y, 8 s después, 1536x864. **Ojo al publicar**:
  primero el pack con el mod 0.9.12 y después el launcher nuevo (un launcher nuevo con el mod
  viejo dejaría el juego en ventana; el launcher 1.0.0 con el mod nuevo va como antes).
- El «lost their mind» en inglés del cliente de Bruno era porque su juego estaba en `en_us`. Ojo:
  el mod solo trae `es_es`; quien ponga español de Argentina, México... ve los textos del mod
  en inglés.

## 0.9.11: Bacteria más dura (2026-10-07; Bruno)

Lo pidió Bruno. La velocidad ya no es un multiplicador: cada fase lleva sus bloques/s
(`Fase.caza` y `Fase.cazaAlarma`, en vez de `velocidad` y el ×1,15 de la alarma):

| Fase | Cazando | Con alarma |
|---|---|---|
| 1 | 5,5 (antes 5,3) | 6,2 (6,1) |
| 2 | 6,0 (5,8) | 6,8 (6,7) |
| 3 | 6,5 (6,4) | 7,5 (7,3) |
| 4 | 7,0 (6,9) | 8,0 (7,9) |

- Vista 25 bloques, 35 con alarma (antes 22 / 34); agachado, la mitad.
- Oído (correr) 15 bloques, 20 con alarma (antes 12 / 20).
- Memoria: si te pierde va a donde te vio; si en 3 s yendo hacia allí no te vuelve a ver, te
  deja; al llegar busca 3,5 s y te deja; luego 3 s sin oírte correr (antes 5 s en total
  contando la búsqueda, 2,5 s buscando y 5 s sorda). Ahora los 3 s solo cuentan mientras va;
  antes se cortaba la búsqueda con el total.
Compila; sin medir en el juego (la conversión a b/s es la de `Bacteria.atributo()`, la que
dio 5,3 medido).

## 0.9.9 / 0.9.10: F3, traje y cámara (2026-10-07; Bruno)

Bruno (FastPaper1) lo pidió y lo hizo desde su PC con Claude. Tiene un servidor de pruebas
propio en `C:\Users\bruno\backrooms-prueba` (puerto 25566, RCON 25576, el mismo montaje).

- **Probado y descartado: el vestíbulo de verdad por las puertas de la cinemática** (0.9.9,
  commit 4db478f; quitado en la 0.9.10). Durante los primeros 3,6 s la cámara del juego se ponía
  en una cabina y la cinta salía transparente en la abertura. Funcionaba, pero a Bruno le quedó
  mal: la cinta tiembla y el mundo de detrás no, así que se nota que el ascensor es una imagen
  encima; y se ven abiertas las puertas de hierro reales mientras se cierran las del dibujo. Se
  queda la caja dibujada de antes. Si se retoma: quitar el temblor mientras se ve el mundo y
  que las puertas del dibujo sean las reales (o cerrar las del mundo a la vez).
- Para repetir la bajada en pruebas: `/backrooms misiones olvidar <nick>` (si no, quien ya hizo
  una expedición no baja: `Expedicion.poner` → `enExpedicion`) y no estar en creativo.
- **F3 reducido** (`menu/mixin/DebugScreenEntryListMixin`): solo versión, FPS, entidades y
  coordenadas, digan lo que digan F3+F6 o `debug-profile.json`; tampoco los atajos de F3 (cajas
  de las entidades, bordes de chunk...). Compila; sin mirar en el juego.
- **Traje sin la línea de «Maldición de ligamiento»** en la descripción (`TOOLTIP_DISPLAY` oculta
  `ENCHANTMENTS`; el encantamiento sigue y el brillo ya estaba apagado). Los trajes ya puestos
  se arreglan al entrar o reaparecer. Compila; sin mirar en el juego.
- **Cámara levantada**: tampoco se ven vida, armadura, comida, aire, montura, barra de
  experiencia, nivel ni el nombre del objeto (antes solo se ocultaban la mira y la barra), y no
  se cambia de casilla ni con 1-9 (`HerramientasCliente`) ni con la rueda (`RuedaCamaraMixin`).
  Compila; sin mirar en el juego (las capturas no salen con la ventana minimizada).

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
  `Bacteria.atributo()` lo convierte, y Fase.caza / Fase.cazaAlarma dan los b/s de cada fase (desde la 0.9.11).
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
