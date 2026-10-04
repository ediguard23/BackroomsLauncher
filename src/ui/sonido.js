'use strict';
/**
 * Sonido del launcher.
 *
 * Si existen los archivos de `assets/sounds/` (ambiente, musica, hover,
 * click, parpadeo) se usan esos. Mientras no esten, todo se sintetiza con
 * WebAudio: el zumbido de 120 Hz de los fluorescentes con su chisporroteo,
 * el "tic" al pasar por un boton y el golpe de rele al pulsarlo.
 */

(function () {
  const RUTA = '../../assets/sounds/';
  const EXT = ['ogg', 'mp3', 'wav'];

  /** Intenta cargar `nombre.(ogg|mp3|wav)`; resuelve con la URL o null. */
  function buscar (nombre) {
    return new Promise((resolve) => {
      let i = 0;
      let hecho = false;
      const fin = (v) => { if (!hecho) { hecho = true; resolve(v); } };
      setTimeout(() => fin(null), 4000);
      const probar = () => {
        if (i >= EXT.length) return fin(null);
        const url = `${RUTA}${nombre}.${EXT[i++]}`;
        const a = new Audio();
        a.preload = 'metadata';
        a.onloadedmetadata = () => fin(url);
        a.oncanplaythrough = () => fin(url);
        a.onerror = probar;
        a.src = url;
      };
      probar();
    });
  }

  class Sonido {
    constructor () {
      this.ctx = new (window.AudioContext || window.webkitAudioContext)();
      this.maestro = this.ctx.createGain();
      this.maestro.connect(this.ctx.destination);
      this.volumen = 0.6;
      this.silencio = false;
      this.archivos = {};
      this.zumbido = null;
      this.pistas = [];
      this._aplicar();
    }

    async cargar () {
      const nombres = ['ambiente', 'musica', 'hover', 'click', 'parpadeo', 'jugar'];
      const urls = await Promise.all(nombres.map(buscar));
      nombres.forEach((n, i) => { if (urls[i]) this.archivos[n] = urls[i]; });
    }

    _aplicar () {
      const v = this.silencio ? 0 : this.volumen;
      this.maestro.gain.setTargetAtTime(v, this.ctx.currentTime, 0.08);
      for (const p of this.pistas) p.volume = v * (p.dataset.base || 1);
    }

    ajustar ({ volumen, silencio }) {
      if (typeof volumen === 'number') this.volumen = volumen;
      if (typeof silencio === 'boolean') this.silencio = silencio;
      this._aplicar();
    }

    _pista (url, base, loop) {
      const a = new Audio(url);
      a.loop = loop;
      a.dataset.base = base;
      a.volume = (this.silencio ? 0 : this.volumen) * base;
      this.pistas.push(a);
      a.play().catch(() => {});
      return a;
    }

    /** Ambiente de fondo: archivos si los hay, si no el zumbido sintetizado. */
    iniciarAmbiente () {
      if (this.ctx.state === 'suspended') this.ctx.resume();
      if (this.archivos.ambiente) this.ambiente = this._pista(this.archivos.ambiente, 0.75, true);
      else this._zumbidoSintetico();
      if (this.archivos.musica) this._pista(this.archivos.musica, 0.42, true);
    }

    _zumbidoSintetico () {
      if (this.zumbido) return;
      const ctx = this.ctx;
      const salida = ctx.createGain();
      salida.gain.value = 0.11;
      salida.connect(this.maestro);

      // red electrica: 120 Hz con armonicos
      [[120, 0.5], [240, 0.22], [360, 0.12], [480, 0.06], [1200, 0.015]].forEach(([f, g]) => {
        const o = ctx.createOscillator();
        o.type = 'sine';
        o.frequency.value = f + (Math.random() - 0.5) * 0.6;
        const gg = ctx.createGain();
        gg.gain.value = g;
        o.connect(gg).connect(salida);
        o.start();
      });

      // chisporroteo del cebador: ruido filtrado con un temblor lento
      const largo = ctx.sampleRate * 2;
      const buf = ctx.createBuffer(1, largo, ctx.sampleRate);
      const d = buf.getChannelData(0);
      for (let i = 0; i < largo; i++) d[i] = (Math.random() * 2 - 1) * (Math.random() < 0.002 ? 1 : 0.25);
      const ruido = ctx.createBufferSource();
      ruido.buffer = buf;
      ruido.loop = true;
      const filtro = ctx.createBiquadFilter();
      filtro.type = 'bandpass';
      filtro.frequency.value = 3200;
      filtro.Q.value = 0.8;
      const gr = ctx.createGain();
      gr.gain.value = 0.05;
      const lfo = ctx.createOscillator();
      lfo.frequency.value = 0.23;
      const lfoG = ctx.createGain();
      lfoG.gain.value = 0.03;
      lfo.connect(lfoG).connect(gr.gain);
      ruido.connect(filtro).connect(gr).connect(salida);
      ruido.start();
      lfo.start();
      this.zumbido = salida;
    }

    /** Corta el zumbido un instante, a la vez que el parpadeo de la imagen. */
    parpadeo (duracion = 0.12) {
      if (this.archivos.parpadeo) {
      this._efecto(this.archivos.parpadeo, 0.7);
      // el zumbido grabado se hunde a la vez que la luz (mismo patron que app.js)
      const a = this.ambiente;
      if (a) {
        const v = () => (this.silencio ? 0 : this.volumen) * 0.75;
        [[0, 0.25], [70, 1], [150, 0.4], [210, 1]].forEach(([t, k]) => setTimeout(() => { a.volume = v() * k; }, t));
      }
      return;
    }
      if (!this.zumbido) return;
      const t = this.ctx.currentTime;
      const g = this.zumbido.gain;
      g.cancelScheduledValues(t);
      g.setValueAtTime(0.11, t);
      g.linearRampToValueAtTime(0.01, t + 0.02);
      g.setValueAtTime(0.01, t + duracion);
      g.linearRampToValueAtTime(0.11, t + duracion + 0.03);
      this._clic(0.05, 1800);
    }

    _efecto (url, base) {
      const a = new Audio(url);
      a.volume = (this.silencio ? 0 : this.volumen) * base;
      a.play().catch(() => {});
    }

    _clic (ganancia, frecuencia) {
      const ctx = this.ctx;
      const t = ctx.currentTime;
      const o = ctx.createOscillator();
      o.type = 'square';
      o.frequency.setValueAtTime(frecuencia, t);
      o.frequency.exponentialRampToValueAtTime(frecuencia * 0.25, t + 0.06);
      const g = ctx.createGain();
      g.gain.setValueAtTime(ganancia, t);
      g.gain.exponentialRampToValueAtTime(0.0001, t + 0.08);
      o.connect(g).connect(this.maestro);
      o.start(t);
      o.stop(t + 0.09);
    }

    /** Al pulsar JUGAR: los tubos se apagan y caes al otro lado. */
    jugar () {
      if (this.archivos.jugar) { this._efecto(this.archivos.jugar, 0.9); return; }
      this.click();
    }

    hover () {
      if (this.archivos.hover) this._efecto(this.archivos.hover, 0.5);
      else this._clic(0.025, 2400);
    }

    click () {
      if (this.archivos.click) { this._efecto(this.archivos.click, 0.8); return; }
      // golpe de rele: un clic seco y un "tunk" grave
      this._clic(0.09, 900);
      const ctx = this.ctx;
      const t = ctx.currentTime;
      const o = ctx.createOscillator();
      o.frequency.setValueAtTime(140, t);
      o.frequency.exponentialRampToValueAtTime(55, t + 0.18);
      const g = ctx.createGain();
      g.gain.setValueAtTime(0.18, t);
      g.gain.exponentialRampToValueAtTime(0.0001, t + 0.22);
      o.connect(g).connect(this.maestro);
      o.start(t);
      o.stop(t + 0.24);
    }

    /** Silencia todo mientras se juega (el launcher queda minimizado). */
    pausar () { this.ctx.suspend(); for (const p of this.pistas) p.pause(); }
    reanudar () { this.ctx.resume(); for (const p of this.pistas) p.play().catch(() => {}); }
  }

  window.Sonido = Sonido;
})();
