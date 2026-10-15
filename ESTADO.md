# Estado del proyecto (para la siguiente sesión)

Última actualización: 2026-10-15 (hora de Honduras). Mod `backrooms_evento` 0.17.0, launcher 1.0.0
(con arreglos sin publicar) y un launcher aparte para la beta.

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
