#!/usr/bin/env bash
# Reinicia el entorno de pruebas del evento con el ultimo jar de evento/:
# servidor Fabric de D:\backrooms-prueba, pack local y launcher entrando solo.
#
#   bash tools/reiniciar-prueba.sh [--mundo-nuevo]
#
# Solo para desarrollo en el PC del organizador (rutas fijas).
set -u
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
SERVIDOR="D:/backrooms-prueba"
JAVA="C:/Program Files/Microsoft/jdk-21.0.12.8-hotspot/bin/java.exe"
GUION="${GUION_JUGAR:-$TEMP/claude/pulsar-jugar-ya.js}"

cd "$RAIZ"
JAR=$(ls -t evento/build/libs/backrooms-evento-*.jar | head -1)
echo "jar: $JAR"

# parar lo que hubiera
node tools/rcon.js stop > /dev/null 2>&1
powershell -NoProfile -Command "Get-Process java,javaw -ErrorAction SilentlyContinue | Where-Object { \$_.MainWindowTitle -eq 'BACKROOMS' } | Stop-Process -Force; Get-Process electron -ErrorAction SilentlyContinue | Stop-Process -Force"
for p in $(netstat -ano | grep "127.0.0.1:8765 .*LISTENING" | awk '{print $5}'); do taskkill //F //PID "$p" > /dev/null; done
until ! netstat -ano | grep -q ":25566 .*LISTENING"; do sleep 1; done
# el puerto se libera antes de que el proceso suelte los jars: esperar a que muera
while powershell -NoProfile -Command "if (Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -like '*fabric-server.jar*' }) { exit 0 } else { exit 1 }"; do sleep 1; done

# el menu viejo (backrooms-menu) ya va dentro del mod del evento: si queda suelto, choca
rm -f "$SERVIDOR"/mods/backrooms-evento-*.jar pack/mods/backrooms-evento-*.jar pack/mods/backrooms-menu-*.jar "$SERVIDOR"/mods/backrooms-menu-*.jar
cp "$JAR" "$SERVIDOR/mods/" && cp "$JAR" pack/mods/
if [ "${1:-}" = "--mundo-nuevo" ]; then rm -rf "$SERVIDOR/nivel0"; fi
sed -i "s/^pauseOnLostFocus:.*/pauseOnLostFocus:${PAUSA_FOCO:-false}/" "$APPDATA/.backrooms-event/options.txt" 2>/dev/null

# servidor y pack en segundo plano
( cd "$SERVIDOR" && nohup "$JAVA" -Xms2G -Xmx4G -jar fabric-server.jar nogui > servidor.log 2>&1 & )
nohup node tools/publicar-pack.js --local --ficha=pack/.evento-local.json > /tmp/pack-local.log 2>&1 &
sleep 3
until grep -q -E "RCON running|Exception|ERROR" "$SERVIDOR/servidor.log" 2>/dev/null; do sleep 2; done
grep -E "ERROR|Exception" "$SERVIDOR/servidor.log" | head -5
until curl -s -o /dev/null http://127.0.0.1:8765/manifest.json; do sleep 1; done
node tools/rcon.js "setworldspawn 3 33 3" "gamerule respawn_radius 0" "gamerule keep_inventory true" > /dev/null

# launcher: entra solo al servidor de pruebas
N=$(grep -c "joined the game" "$SERVIDOR/servidor.log")
BACKROOMS_PRUEBAS=1 BACKROOMS_MANIFEST=http://127.0.0.1:8765/manifest.json BACKROOMS_QUICKPLAY=127.0.0.1:25566 BACKROOMS_PRUEBA="$GUION" \
  nohup ./node_modules/electron/dist/electron.exe . > /dev/null 2>&1 &
until [ "$(grep -c "joined the game" "$SERVIDOR/servidor.log")" -gt "$N" ]; do sleep 3; done
grep "logged in" "$SERVIDOR/servidor.log" | tail -1
