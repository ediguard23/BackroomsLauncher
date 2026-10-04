'use strict';
/**
 * Estado del servidor con el ping de la lista de servidores de Minecraft
 * (Server List Ping): handshake + peticion de estado, y la respuesta es un
 * JSON con la version, el MOTD y los jugadores.
 */

const net = require('net');
const dns = require('dns').promises;

function varint (n) {
  const out = [];
  do {
    let b = n & 0x7f;
    n >>>= 7;
    if (n) b |= 0x80;
    out.push(b);
  } while (n);
  return Buffer.from(out);
}

function leerVarint (buf, pos) {
  let valor = 0;
  let desp = 0;
  for (;;) {
    if (pos >= buf.length) return null;
    const b = buf[pos++];
    valor |= (b & 0x7f) << desp;
    if (!(b & 0x80)) return { valor, pos };
    desp += 7;
    if (desp > 35) throw new Error('VarInt demasiado largo');
  }
}

function paquete (id, ...partes) {
  const cuerpo = Buffer.concat([varint(id), ...partes]);
  return Buffer.concat([varint(cuerpo.length), cuerpo]);
}

function cadena (s) {
  const b = Buffer.from(s, 'utf8');
  return Buffer.concat([varint(b.length), b]);
}

/** Resuelve el registro SRV `_minecraft._tcp` si el puerto es el de por defecto. */
async function resolver (host, port) {
  if (port !== 25565 || net.isIP(host)) return { host, port };
  try {
    const [srv] = await dns.resolveSrv(`_minecraft._tcp.${host}`);
    if (srv) return { host: srv.name, port: srv.port };
  } catch { /* sin SRV */ }
  return { host, port };
}

async function ping (hostOriginal, portOriginal = 25565, timeout = 5000) {
  const { host, port } = await resolver(hostOriginal, portOriginal);
  return new Promise((resolve) => {
    const inicio = Date.now();
    const sock = net.connect({ host, port });
    let buf = Buffer.alloc(0);
    let hecho = false;
    const fin = (r) => {
      if (hecho) return;
      hecho = true;
      sock.destroy();
      resolve(r);
    };
    sock.setTimeout(timeout, () => fin({ online: false }));
    sock.on('error', () => fin({ online: false }));
    sock.on('connect', () => {
      const puerto = Buffer.alloc(2);
      puerto.writeUInt16BE(portOriginal);
      // Protocolo -1: el servidor responde aunque la version no coincida.
      sock.write(paquete(0x00, varint(-1), cadena(hostOriginal), puerto, varint(1)));
      sock.write(paquete(0x00));
    });
    sock.on('data', (c) => {
      buf = Buffer.concat([buf, c]);
      try {
        const largo = leerVarint(buf, 0);
        if (!largo || buf.length < largo.pos + largo.valor) return;
        const id = leerVarint(buf, largo.pos);
        const len = leerVarint(buf, id.pos);
        const json = JSON.parse(buf.slice(len.pos, len.pos + len.valor).toString('utf8'));
        fin({
          online: true,
          latencia: Date.now() - inicio,
          jugadores: { online: (json.players && json.players.online) || 0, max: (json.players && json.players.max) || 0 },
          version: json.version && json.version.name
        });
      } catch {
        fin({ online: false });
      }
    });
  });
}

module.exports = { ping };
