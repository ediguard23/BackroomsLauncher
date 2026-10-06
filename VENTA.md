# Cómo se vende el evento y cómo se protege

## La idea

**Lo que se vende es la entrada, y la entrada es de un nick.** Quien compra en la tienda
queda en la **lista del evento** (la "whitelist") con el nick con el que entró a la tienda,
y su código solo vale con ese nick. Para entrar al servidor hace falta un pase firmado por
la tienda, que solo se consigue canjeando ese código con ese nick, y además el nick tiene
que seguir en la lista. Así no importa que el launcher circule: sin pagar no se entra.

Precio: 2 $ por entrada (producto `entrada-backrooms` de la tienda de PeakMC, categoría
Eventos). La entrada **solo sale en tienda.peakmc.lat/eventos** (y en el aviso de la
portada), no en el catálogo ni en los destacados. Plazas limitadas (100 por defecto, se
cambian en el panel); las de regalo y staff también ocupan plaza.

## Dónde se guarda quién ha comprado

En la base de datos de la tienda, desde la primera venta: al confirmarse el pago (webhook
de Tebex o la vuelta del comprador) se guarda una fila por entrada con el **nick que
compró**, su **código**, la fecha y la compra. Se ve en el panel de la tienda → **Eventos**
(con buscador y «Descargar CSV»). No depende de que el servidor del evento ni el launcher
existan todavía, y Tebex guarda además su propio historial de pagos con el nick.

No hay que ejecutar comandos en Tebex: la lista son esos nicks.

## El recorrido del comprador

1. Entra en tienda.peakmc.lat/eventos, inicia sesión con **el nick con el que va a jugar**
   (de Java) y compra la entrada. Una por nick; para regalar una, se compra entrando con
   el nick del amigo. Bedrock no puede comprarla.
2. Paga con Tebex. Al confirmarse el pago, la tienda le enseña su código
   **BR-XXXX-XXXX-XXXX** (copiar o descargar en un .txt). Ese nick ya está en la lista.
3. El enlace del launcher solo lo ve quien tiene entrada: en esa ventana y en /eventos con
   su sesión iniciada (cuando el staff ponga el enlace en el panel → Eventos → Ajustes).
4. Abre el launcher, pone su nick (o inicia sesión premium) y pulsa JUGAR. Como ese nick
   aún no tiene pase, sale la ventana ENTRADA: pega el código y pulsa CANJEAR. Si el nick
   no es el de la compra, la tienda dice «Este código es del nick X».
5. El launcher guarda el pase y entra. Las siguientes veces JUGAR va directo.

Si reinstala o cambia de PC: vuelve a escribir el mismo código con el mismo nick
(hasta 5 cambios de PC; más, el staff le reinicia el canje en el panel).

## La lista del evento (whitelist)

- La tienda tiene una API con token (`BACKROOMS_SERVIDOR_TOKEN`) para el servidor del
  evento. El mod la descarga al arrancar y cada minuto (copia en
  `config/backrooms-whitelist.json` por si la tienda no responde).
- Al entrar: pase válido **y** nick en la lista (los OP no pasan por la lista).
- Quien sale de la lista (reembolso, `/brwhitelist quitar`) es expulsado. Si de golpe
  sobrarían más de 5, no se echa a nadie (sería un fallo de la tienda, no 5 reembolsos).
- Staff (OP 3 o consola): `/brwhitelist agregar <nick> [nota]` (le crea su entrada y da el
  código, clic para copiar), `quitar <nick>`, `ver <nick>`, `lista`, `recargar`. Lo mismo en
  el panel → Eventos y en `node tools/backrooms.js agregar|quitar|lista` de la tienda.

## Por qué no se puede entrar sin pagar

| Intento | Qué pasa |
|---|---|
| Entrar con Minecraft normal o con otro launcher | El servidor pregunta por el pase durante el login; quien no tiene el mod no entiende la pregunta → fuera |
| Inventarse un pase | El pase va firmado con Ed25519 por la tienda; el servidor solo acepta la firma de su clave → «no es auténtico» |
| Usar el código de otro con mi nick | El código es del nick que compró → la tienda no da el pase («Este código es del nick X») |
| Usar el pase de un amigo con mi nick | El pase dice para qué nick es → «es para otro nick» |
| Copiar el pase y la carpeta de un amigo con su nick | El pase lleva la clave pública del PC donde se canjeó; el servidor manda un reto que solo ese PC puede firmar → «es de otro PC» |
| Probar códigos al azar | 27^12 combinaciones y la tienda limita los intentos por IP |
| Pedir reembolso y seguir jugando | El webhook de Tebex anula sus entradas → sale de la lista → el servidor le echa y no le deja volver |

Lo que **no** se puede impedir del todo: alguien que preste su PC entero a otro. Eso pasa
con cualquier juego de pago; aquí además el nick es suyo y solo puede haber una conexión
por nick.

## Qué falta para empezar a vender (necesita al organizador)

1. **Desplegar la tienda** (peakmc-store): `npm run exportar` y subir la copia de
   Descargas a github.com/ediguard23/peakmc (Render redespliega solo). Al arrancar aplica
   las migraciones 005 (tabla de entradas) y 006 (categoría Eventos, producto y ajustes).
2. En Render → Environment: `BACKROOMS_FIRMA` (la clave privada del `.env` local de
   peakmc-store; no se puede cambiar nunca) y `BACKROOMS_SERVIDOR_TOKEN` (también en ese
   `.env`). Sin la primera no se puede canjear (sí comprar); sin la segunda no hay lista
   para el servidor.
3. Crear en Tebex el paquete de la entrada (2 $, sin comandos) y vincularlo en el panel:
   Productos → «Entrada · Backrooms» → Paquete de Tebex. Hasta entonces /eventos dice
   «A la venta muy pronto».
4. En el servidor del evento, lo de `servidor-ejemplo/` (mods y configs, con el token).
5. Cuando se publique el launcher: su enlace en el panel → Eventos → Ajustes, la IP real en
   `pack/evento.json` y publicar el pack (`npm run publicar-pack`).

## Ideas para vender más (opcionales)

- **Preventa con cuenta atrás**: el launcher ya muestra la cuenta atrás hasta el
  `eventStart`; la tienda ya enseña las plazas que quedan (100 por defecto; el servidor admite 250).
- **Clasificación pública** de los que escapan (`/backrooms escapados`) en la web y en
  Discord: da ganas de entrar en el siguiente.
- **Tráiler** (`npm run trailer`) en TikTok / YouTube Shorts con el enlace de la tienda.
- **Pack de amigos**: hoy cada entrada es de un nick (se compra entrando con el nick de cada uno).
