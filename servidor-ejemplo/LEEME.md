# Servidor del evento: qué poner

Servidor **Fabric** 1.21.11 (loader 0.19.5), Java 21 o superior.

## mods/
- `backrooms-evento-<versión>.jar` (el mismo que va en el pack: cliente y servidor
  tienen que llevar EXACTAMENTE el mismo jar)
- `fabric-api-0.141.6+1.21.11.jar`
- `voicechat-fabric-1.21.11-2.6.24.jar` (Simple Voice Chat, de Modrinth; el mismo que baja el launcher)

## config/
- `backrooms-acceso.json`: entradas de pago. `clavePublica` es la de la tienda
  (`node tools/backrooms.js clave` en peakmc-store). En `token` pega el
  `BACKROOMS_SERVIDOR_TOKEN` de la tienda (está en su `.env` y en Render): con él el
  servidor descarga la **lista del evento** cada minuto (solo entra quien está en ella,
  y a quien sale de la lista se le echa) y funciona `/brwhitelist`. Deja `staff` vacío:
  con offline-mode cualquiera podría ponerse ese nick; el staff entra con su propia
  entrada (`/brwhitelist agregar <nick>`).

## La lista del evento (whitelist)
Son los nicks con entrada en la tienda: quien compra queda dentro al pagar, con el nick
con el que entró a la tienda. No hay que ejecutar nada en Tebex. Para el staff (OP 3 o
consola):
- `/brwhitelist agregar <nick> [nota]`: le crea su entrada (regalo, staff) y te da su
  código (clic para copiar) para que se lo pases; solo vale con ese nick.
- `/brwhitelist quitar <nick>`: anula sus entradas; si está dentro, fuera.
- `/brwhitelist ver <nick>`, `/brwhitelist lista`, `/brwhitelist recargar`.
Lo mismo se ve y se hace en el panel de la tienda → Eventos.
- `voicechat/voicechat-server.properties`: voz de proximidad a 28 bloques y sin grupos
  (que nadie hable con otro al otro lado del mapa). Necesita el **puerto UDP 24454**
  abierto en el host; si el host da otro puerto UDP, cámbialo en `port=`.

## server.properties
Las claves de `server.properties` de esta carpeta. Lo esencial: `level-type`
(el mundo principal ES el Nivel 0) y `online-mode=false` (entran premium y no premium;
la entrada con pase es lo que impide colarse).

## Antes de abrir
1. Mundo nuevo (borra la carpeta del mundo) si cambió el mod.
2. Pregenerar es opcional: el Nivel 0 se genera muy rápido.
3. Prueba entrar con el launcher y una entrada de staff.
4. `/backrooms comida reponer` y `/backrooms escapados olvidar` entre partidas.
