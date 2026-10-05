#!/usr/bin/env node
'use strict';
/**
 * Cliente RCON minimo para el servidor de pruebas:
 *
 *   node tools/rcon.js "comando 1" "comando 2" ...
 *
 * Host, puerto y clave salen de RCON_HOST, RCON_PORT y RCON_PASS (por
 * defecto 127.0.0.1:25576 y la clave del servidor local de pruebas).
 */

const net = require('net');

const HOST = process.env.RCON_HOST || '127.0.0.1';
const PUERTO = Number(process.env.RCON_PORT) || 25576;
const CLAVE = process.env.RCON_PASS || 'pruebas-locales';

function paquete (id, tipo, cuerpo) {
  const datos = Buffer.from(cuerpo, 'utf8');
  const b = Buffer.alloc(14 + datos.length);
  b.writeInt32LE(10 + datos.length, 0);
  b.writeInt32LE(id, 4);
  b.writeInt32LE(tipo, 8);
  datos.copy(b, 12);
  return b;
}

function conectar () {
  return new Promise((resolve, reject) => {
    const s = net.connect(PUERTO, HOST);
    let resto = Buffer.alloc(0);
    const esperando = new Map();
    s.on('data', (c) => {
      resto = Buffer.concat([resto, c]);
      while (resto.length >= 4) {
        const len = resto.readInt32LE(0);
        if (resto.length < len + 4) break;
        const id = resto.readInt32LE(4);
        const texto = resto.toString('utf8', 12, len + 2);
        resto = resto.subarray(len + 4);
        const f = esperando.get(id);
        if (f) { esperando.delete(id); f(texto); }
      }
    });
    s.on('error', reject);
    let n = 1;
    const enviar = (tipo, cuerpo) => new Promise((ok) => {
      const id = n++;
      esperando.set(id, ok);
      s.write(paquete(id, tipo, cuerpo));
    });
    s.on('connect', async () => {
      await enviar(3, CLAVE);
      resolve({ comando: (c) => enviar(2, c), cerrar: () => s.end() });
    });
  });
}

(async () => {
  const r = await conectar();
  for (const c of process.argv.slice(2)) {
    const salida = await r.comando(c);
    console.log(`> ${c}\n${salida.replace(/§./g, '')}`);
  }
  r.cerrar();
})().catch((e) => { console.error(e.message); process.exit(1); });
