# Servidor del evento: qué poner

Servidor **Fabric** 1.21.11 (loader 0.19.5), Java 21 o superior.

## mods/
- `backrooms-evento-<versión>.jar` (el mismo que va en el pack: cliente y servidor
  tienen que llevar EXACTAMENTE el mismo jar)
- `fabric-api-0.141.6+1.21.11.jar`
- `voicechat-fabric-1.21.11-2.6.24.jar` (Simple Voice Chat, de Modrinth; el mismo que baja el launcher)

## config/
- `backrooms-acceso.json`: entradas de pago. `clavePublica` es la de la tienda
  (`node tools/backrooms.js clave` en peakmc-store). Deja `staff` vacío: con
  offline-mode cualquiera podría ponerse ese nick; el staff entra con su propia entrada
  (`node tools/backrooms.js generar 5 staff` en la tienda).
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
