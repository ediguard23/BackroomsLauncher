'use strict';
/**
 * Cache de archivos ya verificados.
 *
 * Volver a calcular el SHA1 de los ~4000 recursos de Minecraft en cada
 * arranque cuesta segundos. Aqui se recuerda, por ruta, el tamano y la fecha
 * de modificacion que tenia el archivo cuando su hash fue correcto: si no han
 * cambiado, se da por bueno sin leerlo.
 *
 * Los archivos del pack del evento NO usan esta cache (`siempre: true` en
 * `estaBien`): esos se comprueban enteros en cada arranque para que nadie
 * pueda cambiar un mod conservando su fecha.
 */

const fs = require('fs');
const path = require('path');
const { hashArchivo } = require('./net');

class Verificados {
  constructor (file) {
    this.file = file;
    this.datos = {};
    this.sucio = false;
    try { this.datos = JSON.parse(fs.readFileSync(file, 'utf8')); } catch { /* primera vez */ }
  }

  /** true si `file` existe y su contenido tiene ese sha1 (y tamano, si se da). */
  async estaBien (file, sha1, size, { siempre = false } = {}) {
    let st;
    try { st = fs.statSync(file); } catch { return false; }
    if (!st.isFile()) return false;
    if (size != null && st.size !== size) return false;
    if (!sha1) return true;
    const firma = `${st.size}:${Math.floor(st.mtimeMs)}:${sha1}`;
    if (!siempre && this.datos[file] === firma) return true;
    const real = await hashArchivo(file);
    if (real !== sha1.toLowerCase()) return false;
    this.datos[file] = firma;
    this.sucio = true;
    return true;
  }

  /** Apunta un archivo recien descargado (ya verificado por download()). */
  marcar (file, sha1) {
    try {
      const st = fs.statSync(file);
      this.datos[file] = `${st.size}:${Math.floor(st.mtimeMs)}:${sha1}`;
      this.sucio = true;
    } catch { /* desaparecio */ }
  }

  guardar () {
    if (!this.sucio) return;
    fs.mkdirSync(path.dirname(this.file), { recursive: true });
    fs.writeFileSync(this.file, JSON.stringify(this.datos));
    this.sucio = false;
  }
}

module.exports = Verificados;
