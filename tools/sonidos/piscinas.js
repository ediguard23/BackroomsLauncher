#!/usr/bin/env node
'use strict';
/**
 * Sonidos del menu de Minecraft: las Piscinas (npm run sonidos:piscinas).
 * Salen en evento/src/main/resources/assets/backrooms/sounds/.
 *
 * Referencias de diseno:
 *  - Una gota que cae al agua suena por la burbuja de aire que atrapa: su
 *    resonancia (frecuencia de Minnaert, ~3,3/R) y la subida de tono cuando
 *    la burbuja llega a la superficie. Por eso aqui cada "plic" es un seno
 *    corto cuyo tono SUBE, no un clic de ruido.
 *  - Una piscina cubierta es una de las salas mas reverberantes que hay
 *    (azulejo, agua y techo duro: RT60 de mas de 2 s), y brillante: la
 *    reverb casi no se amortigua en agudos.
 *  - Lo liminal de las Poolrooms es el silencio con eco: la depuradora de
 *    fondo, el agua que lame los azulejos y, de vez en cuando, algo lejano.
 *
 *   piscinas_ambiente.ogg  80 s en bucle: depuradora, agua, gotas, eco
 *   piscinas_musica.ogg   96 s en bucle: piano electrico en Sol lidio, coro, agua
 *   gota.ogg              al pasar por un boton
 *   toque.ogg             al pulsar un boton
 *   inmersion.ogg         al pulsar JUGAR: chapuzon y bajo el agua
 *   eco.ogg               chapoteo lejano, para los momentos de luz
 */

const path = require('path');
const D = require('./dsp');

const { SR } = D;
const TAU = Math.PI * 2;
const SALIDA = path.join(__dirname, '..', '..', 'evento', 'src', 'main', 'resources', 'assets', 'backrooms', 'sounds');

const paneo = (p) => [Math.cos((p + 1) * Math.PI / 4), Math.sin((p + 1) * Math.PI / 4)];
const suave = (a, b, x) => { const t = Math.min(1, Math.max(0, (x - a) / (b - a))); return t * t * (3 - 2 * t); };

/** "Plic" de gota: burbuja que resuena y sube de tono. Devuelve f(d) para d >= 0. */
function burbuja (f0, subida = 1.9, caida = 0.045) {
  let fase = 0;
  let ultimo = -1;
  return (d) => {
    if (d < 0 || d > caida * 8) return 0;
    const f = f0 * (1 + (subida - 1) * Math.min(1, d / 0.035));
    if (ultimo >= 0) fase += TAU * f * (d - ultimo);
    ultimo = d;
    return Math.sin(fase) * Math.exp(-d / caida) * Math.min(1, d / 0.0015);
  };
}

/* ------------------------------------------------------------ ambiente */

function ambiente () {
  const N = 80;
  const X = 5;
  const PRE = 4;
  const rnd = D.azar(3737);
  const buf = D.estereo(N + X);
  const total = Math.round((PRE + N + X) * SR);

  // depuradora: motor a 98 Hz (Sol) con armonicos, muy de fondo
  const FM = 98 + 3 / N;
  const rosaAgua = [D.rosa(rnd), D.rosa(rnd)];
  const bpAgua = [new D.Biquad('bp', 700, 1.2), new D.Biquad('bp', 760, 1.2)];
  const lpGorgoteo = new D.Biquad('lp', 260, 0.8);
  const rosaG = D.rosa(rnd);
  const rosaAire = D.rosa(rnd);
  const lpAire = new D.Biquad('lp', 2400, 0.7);
  const rev = new D.Freeverb({ sala: 0.94, amortiguacion: 0.12, predelayMs: 28, ancho: 1 });
  const hp = [new D.Biquad('hp', 40, 0.7), new D.Biquad('hp', 40, 0.7)];
  const lp = [new D.Biquad('lp', 12000, 0.7), new D.Biquad('lp', 12000, 0.7)];

  // olas contra el azulejo: rafagas con envolvente lenta
  const olas = [];
  for (let t = 0.4; t < N; t += 0.8 + rnd() * 2.2) olas.push({ t, dur: 0.5 + rnd() * 0.9, g: 0.4 + rnd() * 0.6, p: (rnd() - 0.5) * 1.6, f: 450 + rnd() * 700 });
  // gotas: cada 2.5-8 s, tonos de burbuja de 1.2-2.3 kHz
  const gotas = [];
  for (let t = 1.1; t < N; t += 2.5 + rnd() * 5.5) {
    gotas.push({ t, voz: burbuja(1200 + rnd() * 1100, 1.6 + rnd() * 0.6), p: (rnd() - 0.5) * 1.8, g: 0.5 + rnd() * 0.5 });
    if (rnd() < 0.35) gotas.push({ t: t + 0.11 + rnd() * 0.1, voz: burbuja(1500 + rnd() * 900, 1.8), p: (rnd() - 0.5) * 1.8, g: 0.35 });
  }
  // algo lejano: un chapoteo y un golpe metalico (escalera de la piscina)
  const lejanos = [{ t: 27.5, tipo: 'chapoteo' }, { t: 61.2, tipo: 'metal' }];
  const bpLejos = new D.Biquad('bp', 900, 0.9);
  const rnLejos = D.azar(99);

  for (let k = 0; k < total; k++) {
    const t = k / SR - PRE;
    const tb = ((t % N) + N) % N;
    let L = 0;
    let R = 0;
    let envio = 0;

    const motor = (Math.sin(TAU * FM * t) * 0.6 + Math.sin(TAU * FM * 2 * t) * 0.3 + Math.sin(TAU * FM * 3 * t) * 0.12) *
      (1 + 0.05 * Math.sin(TAU * 0.23 * t)) * 0.018;
    const gorgoteo = lpGorgoteo.paso(rosaG()) * (0.6 + 0.4 * Math.sin(TAU * 0.37 * t + Math.sin(TAU * 0.11 * t) * 2)) * 0.22;
    L += motor + gorgoteo;
    R += motor * 0.95 + gorgoteo * 0.9;
    envio += motor * 0.4 + gorgoteo * 0.3;

    // agua lamiendo: ruido filtrado modulado por las rafagas (cada canal la suya)
    let envOla = [0, 0];
    for (const o of olas) {
      const d = tb - o.t;
      if (d < 0 || d > o.dur + 0.8) continue;
      const e = suave(0, 0.18, d) * Math.exp(-Math.max(0, d - 0.18) / (o.dur * 0.55)) * o.g;
      const pp = paneo(o.p);
      envOla[0] += e * pp[0];
      envOla[1] += e * pp[1];
    }
    for (let c = 0; c < 2; c++) {
      const s = bpAgua[c].paso(rosaAgua[c]()) * envOla[c] * 0.8;
      if (c === 0) L += s; else R += s;
      envio += s * 0.6;
    }

    let gl = 0;
    let gr = 0;
    for (const g of gotas) {
      const d = tb - g.t;
      if (d < 0 || d > 0.4) continue;
      const s = g.voz(d) * 0.06 * g.g;
      const pp = paneo(g.p);
      gl += s * pp[0];
      gr += s * pp[1];
    }
    L += gl * 0.5; R += gr * 0.5; envio += (gl + gr) * 1.3;

    let lejos = 0;
    for (const e of lejanos) {
      const d = tb - e.t;
      if (d < 0 || d > 2) continue;
      if (e.tipo === 'chapoteo') lejos += bpLejos.paso((rnLejos() * 2 - 1) * Math.exp(-d / 0.25) * suave(0, 0.03, d)) * 0.12;
      else lejos += (Math.sin(TAU * 610 * d) * 0.6 + Math.sin(TAU * 1633 * d) * 0.4) * Math.exp(-d / 0.5) * 0.02;
    }
    envio += lejos * 1.6;

    const aire = lpAire.paso(rosaAire()) * 0.012;
    L += aire; R += aire;

    const [wl, wr] = rev.paso(envio);
    L = lp[0].paso(hp[0].paso(L + wl * 0.7));
    R = lp[1].paso(hp[1].paso(R + wr * 0.7));
    if (t >= 0) {
      const i = Math.round(t * SR);
      if (i < buf[0].length) { buf[0][i] = L; buf[1][i] = R; }
    }
  }
  return D.masterizar(D.cerrarBucle(buf, N), { rmsDb: -24, picoDb: -1.5 });
}

/* -------------------------------------------------------------- musica */

// Sol lidio (Sol La Si Do# Re Mi Fa#) con el Sol en 98 Hz, igual que la depuradora.
const SOL = 98;
const nota = (semi) => SOL * Math.pow(2, semi / 12);
// semitonos desde Sol2; cada acorde: bajo + notas del arpegio
const ACORDES = [
  { bajo: -12, arpegio: [12, 16, 19, 23, 26, 30] },      // Sol maj7 (9, #11 arriba)
  { bajo: -10, arpegio: [14, 18, 21, 25, 28, 30] },      // La / Sol: el II mayor lidio
  { bajo: -15, arpegio: [9, 12, 16, 19, 21, 26] },       // Mi m9
  { bajo: -19, arpegio: [12, 16, 19, 23, 25, 30] }       // Do maj7(#11)
];

/** Piano electrico FM: portadora 1:1 con indice que decae y una "lengueta" 1:14 breve. */
function rhodes (f, vel) {
  return (d) => {
    if (d < 0 || d > 5) return 0;
    const idx = 1.6 * Math.exp(-d / 0.5) * vel + 0.2;
    const cuerpo = Math.sin(TAU * f * d + idx * Math.sin(TAU * f * d));
    const lengueta = Math.sin(TAU * f * 14 * d) * Math.exp(-d / 0.02) * 0.25 * vel;
    return (cuerpo + lengueta) * Math.exp(-d / 1.6) * Math.min(1, d / 0.003) * vel;
  };
}

function musica () {
  const N = 96;
  const X = 6;
  const PRE = 10;
  const DUR = 12;            // 4 acordes x 12 s = 48 s; dos vueltas con variacion
  const PASO = 0.75;         // corcheas lentas
  const rnd = D.azar(4242);
  const buf = D.estereo(N + X);
  const total = Math.round((PRE + N + X) * SR);

  const notas = [];
  for (let t = 0; t < N; t += PASO) {
    const c = ACORDES[Math.floor(t / DUR) % ACORDES.length];
    const paso = Math.round(t / PASO);
    if (rnd() < 0.22) continue;                       // huecos: se "erosiona"
    const vuelta = Math.floor(t / 48);
    const orden = vuelta === 0 ? [0, 2, 1, 3, 2, 4, 3, 5] : [5, 3, 4, 2, 3, 1, 2, 0];
    const semi = c.arpegio[orden[paso % orden.length] % c.arpegio.length];
    const vel = 0.45 + rnd() * 0.4 + (paso % 4 === 0 ? 0.15 : 0);
    notas.push({ t: t + (rnd() - 0.5) * 0.03, voz: rhodes(nota(semi), vel), p: (rnd() - 0.5) * 0.9 });
  }

  const lpPad = [new D.Biquad('lp', 1000, 0.6), new D.Biquad('lp', 1000, 0.6)];
  const coro = new D.Cinta({ wowHz: 0.8, wowMs: 2.2, flutterHz: 1.13, flutterMs: 1.4, rnd });
  const rev = new D.Freeverb({ sala: 0.93, amortiguacion: 0.15, predelayMs: 40 });
  const ondulacion = [new D.Biquad('lp', 5000, 0.7), new D.Biquad('lp', 5000, 0.7)];
  const hp = [new D.Biquad('hp', 38, 0.7), new D.Biquad('hp', 38, 0.7)];
  const fasesPad = new Float64Array(16);

  for (let k = 0; k < total; k++) {
    const t = k / SR - PRE;
    const u = ((t % N) + N) % N;
    const ci = Math.floor(u / DUR) % ACORDES.length;

    // arpegio (tambien la cola de la vuelta anterior)
    let al = 0;
    let ar = 0;
    for (const n of notas) {
      for (const d of [u - n.t, u - n.t + N]) {
        if (d < 0 || d > 5) continue;
        const s = n.voz(d) * 0.2;
        const pp = paneo(n.p);
        al += s * pp[0];
        ar += s * pp[1];
      }
    }

    // pad suave: triangulos del acorde actual con fundido entre acordes
    let pad = 0;
    let bajo = 0;
    for (let c = 0; c < ACORDES.length; c++) {
      let uc = (((u - c * DUR) % 48) + 48) % 48;
      if (uc > 48 - DUR) uc -= 48;
      const w = suave(-2.5, 2.5, uc) * (1 - suave(DUR - 2.5, DUR + 2.5, uc));
      if (w <= 0) continue;
      const ac = ACORDES[c];
      [ac.arpegio[0], ac.arpegio[1], ac.arpegio[2]].forEach((semi, j) => {
        const f = nota(semi - 12);
        const ix = c * 4 + j;
        fasesPad[ix] = (fasesPad[ix] + f / SR) % 1;
        const tri = 1 - 4 * Math.abs(fasesPad[ix] - 0.5);
        pad += tri * w * 0.06;
      });
      const fb = nota(ac.bajo);
      const ixb = c * 4 + 3;
      fasesPad[ixb] = (fasesPad[ixb] + fb / SR) % 1;
      const ph = TAU * fasesPad[ixb];
      bajo += (Math.sin(ph) + 0.3 * Math.sin(2 * ph)) * w * 0.05;
    }
    if (k % 64 === 0) {
      const fc = 900 + 300 * Math.sin(TAU * t / 24);
      lpPad[0].ajustar(fc, 0.6); lpPad[1].ajustar(fc * 1.05, 0.6);
      // "bajo el agua": el brillo general sube y baja muy despacio
      const fo = 4200 + 2200 * Math.sin(TAU * t / 40 + 1);
      ondulacion[0].ajustar(fo, 0.7); ondulacion[1].ajustar(fo, 0.7);
    }
    const pl = lpPad[0].paso(pad);
    const pr = lpPad[1].paso(pad);

    let [cl, cr] = coro.paso(al, ar);
    let L = al * 0.6 + cl * 0.55 + pl + bajo;
    let R = ar * 0.6 + cr * 0.55 + pr + bajo;
    const [wl, wr] = rev.paso((al + ar) * 0.9 + (pl + pr) * 0.6);
    L += wl * 0.75;
    R += wr * 0.75;
    L = hp[0].paso(ondulacion[0].paso(Math.tanh(L * 1.2) / 1.2));
    R = hp[1].paso(ondulacion[1].paso(Math.tanh(R * 1.2) / 1.2));
    if (t >= 0) {
      const i = Math.round(t * SR);
      if (i < buf[0].length) { buf[0][i] = L; buf[1][i] = R; }
    }
  }
  return D.masterizar(D.cerrarBucle(buf, N), { rmsDb: -21, picoDb: -1.5 });
}

/* ------------------------------------------------------ efectos cortos */

function corto (segundos, sala, humedo, fn, picoDb, amort = 0.15) {
  const rnd = D.azar(Math.floor(segundos * 1000));
  const buf = D.estereo(segundos);
  const rev = new D.Freeverb({ sala, amortiguacion: amort, predelayMs: 12 });
  for (let k = 0; k < buf[0].length; k++) {
    const [l, r] = fn(k / SR, rnd);
    const [wl, wr] = rev.paso((l + r) * 0.5);
    buf[0][k] = l + wl * humedo;
    buf[1][k] = r + wr * humedo;
  }
  let pico = 0;
  for (let k = 0; k < buf[0].length; k++) pico = Math.max(pico, Math.abs(buf[0][k]), Math.abs(buf[1][k]));
  const g = Math.pow(10, picoDb / 20) / (pico || 1);
  for (let k = 0; k < buf[0].length; k++) { buf[0][k] *= g; buf[1][k] *= g; }
  return D.fundidos(buf, 0.0005, 0.08);
}

function gota () {
  const v = burbuja(1650, 1.7, 0.03);
  return corto(0.6, 0.85, 0.35, (t) => { const s = v(t); return [s, s]; }, -10);
}

function toque () {
  // golpecito en azulejo + una burbuja pequena
  const bp = new D.Biquad('bp', 2300, 4);
  const v = burbuja(2100, 1.5, 0.025);
  return corto(0.9, 0.9, 0.3, (t, rnd) => {
    const golpe = bp.paso(t < 0.002 ? rnd() * 2 - 1 : 0) * 1.4 + Math.sin(TAU * 1180 * t) * Math.exp(-t / 0.018) * 0.4;
    const s = golpe + v(t - 0.045) * 0.7;
    return [s, s * 0.95];
  }, -6);
}

function inmersion () {
  const rosa = [D.rosa(D.azar(5)), D.rosa(D.azar(6))];
  const bp = [new D.Biquad('bp', 3000, 0.8), new D.Biquad('bp', 3200, 0.8)];
  const lpBajo = [new D.Biquad('lp', 400, 0.7), new D.Biquad('lp', 380, 0.7)];
  const burbujas = [];
  const rb = D.azar(77);
  for (let i = 0; i < 14; i++) burbujas.push({ t: 0.35 + rb() * 1.8, v: burbuja(500 + rb() * 900, 1.4 + rb() * 0.5, 0.04), p: (rb() - 0.5) * 1.4 });
  return corto(3.4, 0.92, 0.3, (t) => {
    // chapuzon: ruido brillante que cae a grave
    const fc = t < 0.25 ? 3000 : 3000 * Math.pow(0.08, Math.min(1, (t - 0.25) / 0.5));
    if (Math.round(t * SR) % 32 === 0) { bp[0].ajustar(fc, 0.8); bp[1].ajustar(fc * 1.06, 0.8); }
    const envCh = suave(0, 0.02, t) * Math.exp(-t / 0.35);
    // bajo el agua: retumbe grave que se abre y se apaga
    const envBajo = suave(0.2, 0.7, t) * (1 - suave(1.6, 3.2, t));
    let L = bp[0].paso(rosa[0]()) * envCh * 1.6 + lpBajo[0].paso(rosa[0]()) * envBajo * 2.2;
    let R = bp[1].paso(rosa[1]()) * envCh * 1.6 + lpBajo[1].paso(rosa[1]()) * envBajo * 2.2;
    for (const b of burbujas) {
      const s = b.v(t - b.t) * 0.12;
      const pp = paneo(b.p);
      L += s * pp[0];
      R += s * pp[1];
    }
    return [L, R];
  }, -3);
}

function eco () {
  const rosa = D.rosa(D.azar(8));
  const bp = new D.Biquad('bp', 900, 0.9);
  return corto(3.0, 0.96, 1.2, (t) => {
    const s = bp.paso(rosa()) * Math.exp(-t / 0.22) * suave(0, 0.02, t) * 0.6;
    return [s * 0.8, s];
  }, -8, 0.1);
}

module.exports = { ambiente, musica, gota, toque, inmersion, eco };

if (require.main === module) (async () => {
  const lista = { piscinas_ambiente: [ambiente, 5], piscinas_musica: [musica, 5], gota: [gota, 4], toque: [toque, 4], inmersion: [inmersion, 5], eco: [eco, 4] };
  for (const [nombre, [fn, q]] of Object.entries(lista)) {
    if (process.argv[2] && process.argv[2] !== nombre) continue;
    const t0 = Date.now();
    const audio = fn();
    const bytes = await D.guardarOgg(path.join(SALIDA, `${nombre}.ogg`), audio, q);
    console.log(`  ${nombre}.ogg  ${(audio[0].length / SR).toFixed(2)} s  ${(bytes / 1024).toFixed(0)} KB  (${((Date.now() - t0) / 1000).toFixed(1)} s)`);
  }
})();
