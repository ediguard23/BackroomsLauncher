# Cómo se vende el evento y cómo se protege

## La idea

**El launcher es gratis; lo que se vende es la entrada.** Cualquiera puede descargar el
launcher (el repo es público y las actualizaciones van por GitHub Releases), pero para
entrar al servidor hace falta un pase firmado por la tienda, y el pase solo se consigue
canjeando un código de compra. Así no importa que el launcher circule: sin pagar no se entra.

Precio: 2 $ por entrada (un producto de la tienda de PeakMC con slug `entrada-backrooms`).

## El recorrido del comprador

1. Entra en tienda.peakmc.lat, inicia sesión con su nick y compra «Entrada Backrooms».
2. Paga con Tebex. Al confirmarse el pago, la tienda le enseña su código
   **BR-XXXX-XXXX-XXXX** con un botón de copiar (y el enlace al launcher).
3. Abre el launcher, pone su nick (o inicia sesión premium) y pulsa JUGAR. Como ese nick
   aún no tiene pase, sale la ventana ENTRADA: pega el código y pulsa CANJEAR.
4. Desde ese momento el código queda atado a ese nick. El launcher guarda el pase y entra.
   Las siguientes veces JUGAR va directo.

Si reinstala o cambia de PC: vuelve a escribir el mismo código con el mismo nick
(hasta 5 cambios de PC; más, lo resuelve el staff con `liberar`).

## Por qué no se puede entrar sin pagar

| Intento | Qué pasa |
|---|---|
| Entrar con Minecraft normal o con otro launcher | El servidor pregunta por el pase durante el login; quien no tiene el mod no entiende la pregunta → fuera |
| Inventarse un pase | El pase va firmado con Ed25519 por la tienda; el servidor solo acepta la firma de su clave → «no es auténtico» |
| Usar el pase de un amigo con mi nick | El pase dice para qué nick es → «es para otro nick» |
| Copiar el pase y la carpeta de un amigo con su nick | El pase lleva la clave pública del PC donde se canjeó; el servidor manda un reto que solo ese PC puede firmar → «es de otro PC» |
| Revender el código después de usarlo | Ya está atado a un nick → «se usó con otro nick» |
| Probar códigos al azar | 27^12 combinaciones y la tienda limita los intentos por IP |
| Pedir reembolso y seguir jugando | El webhook de Tebex revoca las entradas de esa compra |

Las cuatro primeras filas se han probado contra el servidor de pruebas (con una tienda de
prueba) y fallan como deben.

Lo que **no** se puede impedir del todo: alguien que preste su PC entero a otro. Eso pasa
con cualquier juego de pago; aquí además el nick es suyo y solo puede haber una conexión
por nick.

## Qué falta para empezar a vender (necesita al organizador)

1. **Desplegar la tienda** con lo nuevo (commit `493e574` de peakmc-store): aplica la
   migración `005_backrooms.sql` en Supabase (es producción: crea la tabla
   `peakmc.backrooms_codigos`).
2. En Render → Environment: `BACKROOMS_FIRMA` con la clave privada que está en el `.env`
   local de peakmc-store (no se puede cambiar después: invalidaría todos los pases).
3. Crear el producto en Tebex y en el panel de la tienda con el slug `entrada-backrooms`.
4. Poner la IP real del servidor en `pack/evento.json` y publicar el pack
   (`npm run publicar-pack`). No antes de los pasos 1-3: el pack ya pide entrada.
5. En el servidor del evento, lo de `servidor-ejemplo/` (mods y configs).
6. Entradas del staff y de regalo: `node tools/backrooms.js generar <n> <nota>` en peakmc-store.

## Ideas para vender más (opcionales)

- **Preventa con cuenta atrás**: el launcher ya muestra la cuenta atrás hasta el
  `eventStart`; la tienda puede anunciar «plazas limitadas» (el servidor admite 250).
- **Clasificación pública** de los que escapan (`/backrooms escapados`) en la web y en
  Discord: da ganas de entrar en el siguiente.
- **Tráiler** (`npm run trailer`) en TikTok / YouTube Shorts con el enlace de la tienda.
- **Pack de amigos**: comprar 4 entradas a la vez (la tienda ya genera un código por unidad).
