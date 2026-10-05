'use strict';
/**
 * Tienda de mentira para probar las entradas en local, sin tocar la tienda de
 * verdad ni su base de datos:
 *
 *   node tools/acceso/tienda-prueba.js [carpeta-del-servidor]
 *
 * Escucha en http://127.0.0.1:8770/api/backrooms/canjear con la misma API y el
 * mismo formato de pase que peakmc-store (server/lib/backrooms.js), pero firma
 * con una clave de PRUEBA y guarda los canjes en memoria. Si se le da la
 * carpeta del servidor de pruebas, le escribe config/backrooms-acceso.json con
 * esa clave publica para que el mod acepte sus pases.
 *
 * Cualquier codigo con formato BR-XXXX-XXXX-XXXX vale, salvo BR-MALO-MALO-MALO.
 */

const crypto = require('crypto');
const fs = require('fs');
const http = require('http');
const os = require('os');
const path = require('path');

const PUERTO = 8770;
const ARCHIVO_CLAVE = path.join(os.tmpdir(), 'backrooms-tienda-prueba.key');

let privada;
if (fs.existsSync(ARCHIVO_CLAVE)) {
  privada = crypto.createPrivateKey({ key: fs.readFileSync(ARCHIVO_CLAVE), format: 'der', type: 'pkcs8' });
} else {
  privada = crypto.generateKeyPairSync('ed25519').privateKey;
  fs.writeFileSync(ARCHIVO_CLAVE, privada.export({ format: 'der', type: 'pkcs8' }));
}
const publica = crypto.createPublicKey(privada).export({ format: 'der', type: 'spki' }).toString('base64');

const servidor = process.argv[2];
if (servidor) {
  const f = path.join(servidor, 'config', 'backrooms-acceso.json');
  fs.mkdirSync(path.dirname(f), { recursive: true });
  fs.writeFileSync(f, JSON.stringify({ activo: true, clavePublica: publica, evento: 'backrooms-0', staff: [] }, null, 2));
  console.log(`config del servidor: ${f}`);
}

const b64url = (b) => Buffer.from(b).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
const canjes = new Map();

http.createServer((req, res) => {
  const responder = (status, obj) => {
    res.writeHead(status, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(obj));
  };
  if (req.method !== 'POST' || req.url !== '/api/backrooms/canjear') return responder(404, { error: 'no' });
  let cuerpo = '';
  req.on('data', (d) => { cuerpo += d; });
  req.on('end', () => {
    let d;
    try { d = JSON.parse(cuerpo); } catch { return responder(400, { error: 'JSON' }); }
    const codigo = String(d.codigo || '').toUpperCase();
    if (!/^BR-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$/.test(codigo) || codigo === 'BR-MALO-MALO-MALO') {
      return responder(404, { error: 'Ese código no existe o ya no es válido.' });
    }
    if (!/^[A-Za-z0-9_]{3,16}$/.test(d.nick || '')) return responder(400, { error: 'Ese nick no es válido.' });
    const previo = canjes.get(codigo);
    if (previo && previo.toLowerCase() !== d.nick.toLowerCase()) return responder(409, { error: 'Este código ya se usó con otro nick.' });
    canjes.set(codigo, d.nick);
    const datos = Buffer.from(JSON.stringify({ v: 1, evento: 'backrooms-0', nick: d.nick, disp: d.dispositivo, exp: Math.floor(Date.now() / 1000) + 90 * 86400 }));
    const pase = `${b64url(datos)}.${b64url(crypto.sign(null, datos, privada))}`;
    console.log(`canje ${codigo} -> ${d.nick}`);
    responder(200, { ok: true, pase, nick: d.nick });
  });
}).listen(PUERTO, '127.0.0.1', () => console.log(`tienda de prueba en http://127.0.0.1:${PUERTO} (clave publica ${publica})`));
