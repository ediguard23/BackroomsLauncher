'use strict';
/**
 * Ajustes del jugador (nick, premium, RAM, volumen...) en
 * `%APPDATA%/Backrooms Launcher/config.json`.
 *
 * La cuenta premium lleva el refresh token de Microsoft, que equivale a la
 * sesion: se guarda cifrado con safeStorage (DPAPI en Windows), atado al
 * usuario de Windows que la creo.
 */

const fs = require('fs');
const path = require('path');

const POR_DEFECTO = { nick: '', premium: false, ramMB: 0, volumen: 0.6, silencio: false, efectos: true, pantallaCompleta: false };

class Config {
  constructor (dir, safeStorage) {
    this.file = path.join(dir, 'config.json');
    this.safe = safeStorage;
    this.datos = { ...POR_DEFECTO };
    try { Object.assign(this.datos, JSON.parse(fs.readFileSync(this.file, 'utf8'))); } catch { /* primera vez */ }
  }

  get () {
    const { cuentaCifrada, ...resto } = this.datos;
    return resto;
  }

  set (cambios) {
    for (const k of Object.keys(POR_DEFECTO)) if (k in cambios) this.datos[k] = cambios[k];
    this.guardar();
    return this.get();
  }

  cuenta () {
    if (!this.datos.cuentaCifrada) return null;
    try {
      const buf = Buffer.from(this.datos.cuentaCifrada, 'base64');
      return JSON.parse(this.safe.decryptString(buf));
    } catch {
      return null; // otro usuario de Windows o datos rotos: se pide login de nuevo
    }
  }

  guardarCuenta (cuenta) {
    if (!cuenta) delete this.datos.cuentaCifrada;
    else if (this.safe.isEncryptionAvailable()) this.datos.cuentaCifrada = this.safe.encryptString(JSON.stringify(cuenta)).toString('base64');
    else throw new Error('Windows no permite guardar la sesion cifrada en este equipo');
    this.guardar();
  }

  guardar () {
    fs.mkdirSync(path.dirname(this.file), { recursive: true });
    fs.writeFileSync(`${this.file}.tmp`, JSON.stringify(this.datos, null, 2));
    fs.renameSync(`${this.file}.tmp`, this.file);
  }
}

module.exports = Config;
