'use strict';
/**
 * La entrada del evento: el launcher se descarga gratis, pero para entrar al
 * servidor hace falta un PASE firmado por la tienda, y el pase se consigue
 * canjeando el codigo que te dan al comprar (BR-XXXX-XXXX-XXXX).
 *
 * Al canjear se genera un par de claves Ed25519 "del dispositivo": la publica
 * va a la tienda y queda dentro del pase; la privada se queda en este PC. Al
 * conectarse, el servidor manda un reto y el mod lo firma con ella: un pase
 * copiado a otro PC no sirve. Reinstalar es volver a escribir el mismo
 * codigo con el mismo nick.
 *
 * Todo se guarda en <juego>/backrooms-pase.json, que es lo que lee el mod.
 */

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const ARCHIVO = 'backrooms-pase.json';

function leerPase (gameDir) {
  try {
    return JSON.parse(fs.readFileSync(path.join(gameDir, ARCHIVO), 'utf8'));
  } catch {
    return null;
  }
}

/** Los datos del pase (sin comprobar la firma: eso lo hace el servidor). */
function datosPase (pase) {
  try {
    const [cuerpo] = String(pase).split('.');
    return JSON.parse(Buffer.from(cuerpo.replace(/-/g, '+').replace(/_/g, '/'), 'base64').toString('utf8'));
  } catch {
    return null;
  }
}

/** true si hay un pase para `nick` que no ha caducado. */
function paseValido (gameDir, nick) {
  const g = leerPase(gameDir);
  if (!g || !g.pase || !g.clave || String(g.nick).toLowerCase() !== String(nick).toLowerCase()) return false;
  const d = datosPase(g.pase);
  return Boolean(d && d.exp && d.exp * 1000 > Date.now() + 60 * 60 * 1000);
}

function limpiarCodigo (texto) {
  const s = String(texto || '').toUpperCase().replace(/[^A-Z0-9]/g, '').replace(/^BR/, '');
  return s.length === 12 ? `BR-${s.slice(0, 4)}-${s.slice(4, 8)}-${s.slice(8, 12)}` : null;
}

/**
 * Canjea `codigo` para `nick` en la tienda (`api` = .../api/backrooms) y
 * guarda el pase. Devuelve el nick con el que quedo.
 */
async function canjear (api, gameDir, codigo, nick) {
  const limpio = limpiarCodigo(codigo);
  if (!limpio) throw new Error('El código tiene el formato BR-XXXX-XXXX-XXXX');
  // se reutiliza la clave del dispositivo si ya habia una (asi no cuenta como cambio de PC)
  const previo = leerPase(gameDir);
  let privada;
  if (previo && previo.clave) {
    try {
      privada = crypto.createPrivateKey({ key: Buffer.from(previo.clave, 'base64'), format: 'der', type: 'pkcs8' });
    } catch { privada = null; }
  }
  if (!privada) privada = crypto.generateKeyPairSync('ed25519').privateKey;
  const publica = crypto.createPublicKey(privada).export({ format: 'der', type: 'spki' }).toString('base64');

  let r;
  try {
    r = await fetch(`${api.replace(/\/$/, '')}/canjear`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ codigo: limpio, nick, dispositivo: publica }),
      signal: AbortSignal.timeout(20000)
    });
  } catch {
    throw new Error('No se pudo contactar con la tienda. Revisa tu conexión.');
  }
  let datos = {};
  try { datos = await r.json(); } catch { /* respuesta vacia */ }
  if (!r.ok || !datos.pase) throw new Error(datos.error || `La tienda respondió ${r.status}`);

  fs.mkdirSync(gameDir, { recursive: true });
  fs.writeFileSync(path.join(gameDir, ARCHIVO), JSON.stringify({
    nick: datos.nick || nick,
    codigo: limpio,
    pase: datos.pase,
    clave: privada.export({ format: 'der', type: 'pkcs8' }).toString('base64')
  }, null, 2));
  return datos.nick || nick;
}

module.exports = { paseValido, canjear, limpiarCodigo, leerPase, datosPase, ARCHIVO };
