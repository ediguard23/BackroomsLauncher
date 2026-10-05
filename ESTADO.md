# Estado del proyecto (para la siguiente sesión)

Última actualización: 2026-10-05 17:15 (hora de Honduras). Mod `backrooms_evento` 0.8.0, launcher 1.0.0.

Leer esto antes de tocar nada. Lo que se va haciendo se anota aquí y se sube.

## Cómo probar

- Servidor de pruebas: `D:\backrooms-prueba` (puerto 25566, RCON 25576 clave `pruebas-locales`).
- `bash tools/reiniciar-prueba.sh [--mundo-nuevo]` compila nada: copia el último jar de
  `evento/build/libs`, reinicia servidor + pack local + launcher y entra solo con `xgdier_`.
  Con `JVM_EXTRA="-Dlog4j.configurationFile=log4j2-depurar.xml"` el servidor saca la
  traza de los comandos que fallan («An unexpected error occurred»).
- Órdenes al cliente sin tocar la ventana (solo con BACKROOMS_PRUEBAS): escribir en
  `%TEMP%/backrooms-ordenes.txt` una de `inventario | pausa | cerrar | cinematica |
  linterna | camara | tab [n] | captura <nombre>`. `tab 200` llena el TAB con 200
  jugadores ficticios. Las capturas salen en `%APPDATA%/.backrooms-event/screenshots`.
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
- Acceso con entrada (pase Ed25519 de la tienda): si la config se rompe, el servidor queda
  CERRADO (antes quedaba abierto).

## Pendiente

1. **Tienda (peakmc-store): desplegar las entradas.** Está en commit local (`493e574`),
   NO desplegada. Falta aplicar `server/migrations/005_backrooms.sql` en Supabase y
   desplegar (`npm run exportar` → Render). **Pedir permiso al usuario**: es producción.
2. **No publicar el pack** (`npm run publicar-pack`) hasta que la tienda esté desplegada:
   `pack/evento.json` ya lleva `acceso.api` y sin la tienda nadie podría canjear ni entrar.
3. IP real del servidor en `pack/evento.json` (sigue `play.tuservidor.net`).
4. Pasos propios suaves sobre la moqueta y balanceo de cámara al andar (pedido, sin hacer).
5. Simple Voice Chat: comprobar licencia y versión 1.21.11 y si el host abre el puerto UDP.
6. Fallo raro una vez: `tp` del vestíbulo al Nivel 0 dio «unexpected error» y dejó al
   cliente en «Cargando el terreno». No se ha repetido; si vuelve, el log de depuración
   (ver arriba) da la traza.
7. Probar la pantalla de muerte en su variante «eliminado» con captura (la lógica está;
   la animación y el ban ya se vieron funcionar).

## Avisos

- La lista `staff` de `config/backrooms-acceso.json` del servidor deja entrar sin pase por
  el nick: con offline-mode cualquiera podría ponerse ese nick. Dejarla vacía.
- Los textos que se ven en el juego van en español; el log del servidor los saca en inglés
  porque el servidor no tiene el idioma (en el cliente salen bien).
