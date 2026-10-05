#!/usr/bin/env node
'use strict';
/**
 * Genera los sonidos del evento en assets/sounds/*.ogg (npm run sonidos).
 *
 * Todo es sintesis propia, sin muestras de terceros, asi que no hay licencias
 * que respetar. Referencias de diseno:
 *
 *  - El zumbido de las backrooms es el de un balasto magnetico: el nucleo de
 *    hierro vibra al doble de la frecuencia de red (120 Hz con red de 60 Hz)
 *    y el 3.er armonico (360 Hz) es el mas fuerte despues del fundamental.
 *    Encima va el "bzzz" metalico del nucleo, que no es constante.
 *  - La musica de lo liminal son drones que flotan sin resolver, con
 *    melodias "erosionadas", cinta y mucha reverb. Para que sea agradable y
 *    no cansina, la musica esta afinada sobre el propio zumbido: Si menor con
 *    el Si en 120 Hz, asi musica y fluorescentes suenan juntos sin chocar.
 *
 *   ambiente.ogg   75 s en bucle: tubos, ventilacion, tono de sala, goteos lejanos
 *   musica.ogg     96 s en bucle: pads en Si menor, campanas lejanas, cinta
 *   hover.ogg      tic suave al pasar por un boton
 *   click.ogg      interruptor + cebador + tubo que arranca
 *   parpadeo.ogg   chasquidos y arco electrico de un tubo que falla
 *   jugar.ogg      "noclip": los tubos se apagan y caes al otro lado
 */

const fs = require('fs');
const path = require('path');
const D = require('./dsp');

const { SR } = D;
const SALIDA = path.join(__dirname, '..', '..', 'assets', 'sounds');
const TAU = Math.PI * 2;

const paneo = (p) => [Math.cos((p + 1) * Math.PI / 4), Math.sin((p + 1) * Math.PI / 4)];
const suave = (a, b, x) => { const t = Math.min(1, Math.max(0, (x - a) / (b - a))); return t * t * (3 - 2 * t); };

/* --------------------------------------------------------- el zumbido */

const ARMONICOS = [[1, 1.0], [2, 0.42], [3, 0.58], [4, 0.22], [5, 0.16], [6, 0.07], [7, 0.06], [9, 0.025]];

function zumbido (t, f, fase = 0) {
  let s = 0;
  for (const [n, a] of ARMONICOS) s += a * Math.sin(TAU * f * n * t + fase * n);
  return s / 2.6;
}

/** El "bzzz" del nucleo: tren de pulsos a 120 Hz filtrado en la zona metalica. */
function crearBuzz (frecuenciaFiltro = 2400) {
  const bp = new D.Biquad('bp', frecuenciaFiltro, 0.9);
  const hp = new D.Biquad('hp', 900, 0.7);
  return (t, f, fase = 0) => {
    const pulso = Math.pow(Math.max(0, Math.sin(TAU * f * t + fase)), 14) - 0.21;
    return hp.paso(bp.paso(pulso));
  };
}

/* ------------------------------------------------------------ ambiente */

function ambiente () {
  const N = 75;           // duracion del bucle
  const X = 4;            // fundido del cierre
  const PRE = 3;          // precalentar reverb y filtros
  const rnd = D.azar(1977);
  const buf = D.estereo(N + X);
  const total = Math.round((PRE + N + X) * SR);

  // Tres tubos. Las desafinaciones son multiplos de 1/75 Hz para que el
  // batido entre ellos cierre exacto en el bucle.
  const tubos = [
    { f: 120, g: 1.0, p: -0.4, fase: 0.3, buzz: crearBuzz(2300), mod: 0.07 },
    { f: 120 + 2 / N, g: 0.72, p: 0.15, fase: 1.7, buzz: crearBuzz(2700), mod: 0.053 },
    { f: 120 - 1 / N, g: 0.55, p: 0.5, fase: 4.1, buzz: crearBuzz(2050), mod: 0.04 }
  ].map((tb) => ({ ...tb, pan: paneo(tb.p), traq: 0.6 }));

  const rosaSala = D.rosa(rnd);
  const lpSala = new D.Biquad('lp', 340, 0.6);
  const marronVent = D.marron(rnd);
  const lpVent = new D.Biquad('lp', 105, 0.7);
  const rosaAire = D.rosa(rnd);
  const hpAire = new D.Biquad('hp', 4200, 0.7);
  const lpAire = new D.Biquad('lp', 9000, 0.7);
  const bpChispa = new D.Biquad('bp', 3600, 2.2);
  const rev = new D.Freeverb({ sala: 0.8, amortiguacion: 0.5, predelayMs: 22 });
  const lpL = new D.Biquad('lp', 10500, 0.7); const lpR = new D.Biquad('lp', 10500, 0.7);
  const hpL = new D.Biquad('hp', 45, 0.7); const hpR = new D.Biquad('hp', 45, 0.7);

  // Sucesos lejanos (posicion en segundos dentro del bucle)
  const goteos = [21.3, 22.0, 52.8, 64.4].map((t, i) => ({ t, f: 2100 - i * 160, p: [-0.7, -0.7, 0.6, -0.2][i], g: i === 1 ? 0.45 : 1 }));
  const golpe = { t: 38.2 };
  const lpGolpe = new D.Biquad('lp', 260, 0.7);
  const rnGolpe = D.azar(5);

  for (let k = 0; k < total; k++) {
    const t = k / SR - PRE;
    const tb = ((t % N) + N) % N;  // tiempo dentro del bucle
    let L = 0;
    let R = 0;
    let envio = 0;

    for (const tu of tubos) {
      // el traqueteo del nucleo deriva despacio, nunca es constante
      tu.traq += (rnd() - 0.5) * 0.0009;
      tu.traq = Math.min(1, Math.max(0.25, tu.traq * 0.99999 + 0.6 * 0.00001));
      const vida = 1 + 0.04 * Math.sin(TAU * tu.mod * t);
      const s = tu.g * vida * (zumbido(t, tu.f, tu.fase) * 0.85 + tu.buzz(t, tu.f, tu.fase) * 2.2 * tu.traq);
      L += s * tu.pan[0];
      R += s * tu.pan[1];
      envio += s * 0.35;
    }
    const nivelTubos = 0.11;
    L *= nivelTubos; R *= nivelTubos; envio *= nivelTubos;

    // chisporroteo del cebador: chasquidos muy dispersos
    let chispa = 0;
    if (rnd() < 0.7 / SR) chispa = (rnd() * 2 - 1) * (0.4 + rnd() * 0.6);
    const ch = bpChispa.paso(chispa) * 0.05;

    const sala = lpSala.paso(rosaSala()) * 0.3;
    const vent = lpVent.paso(marronVent()) * 0.12 + Math.sin(TAU * 47.2 * t) * 0.004 * (1 + 0.3 * Math.sin(TAU * 0.11 * t));
    const aire = lpAire.paso(hpAire.paso(rosaAire())) * 0.05;
    L += sala + vent + aire + ch * 0.8;
    R += sala * 0.96 + vent + aire * 0.9 + ch * 0.5;
    envio += sala * 0.3 + ch;

    // goteos: un "plic" con algo de tono, que suena lejos (casi todo reverb)
    let lejos = 0;
    let lejosPan = 0;
    for (const g of goteos) {
      const d = tb - g.t;
      if (d >= 0 && d < 0.25) {
        const f = g.f * (1 - 0.35 * Math.min(1, d / 0.03));
        const s = Math.sin(TAU * f * d) * Math.exp(-d / 0.035) * 0.05 * g.g;
        lejos += s;
        lejosPan = g.p;
      }
    }
    // golpe sordo muy lejano
    const dg = tb - golpe.t;
    if (dg >= 0 && dg < 1.5) {
      const f = 70 * Math.exp(-dg * 0.9) + 38;
      lejos += lpGolpe.paso(Math.sin(TAU * f * dg) * Math.exp(-dg / 0.35) * 0.1 + (rnGolpe() * 2 - 1) * Math.exp(-dg / 0.05) * 0.05);
    }
    const lp = paneo(lejosPan);
    L += lejos * lp[0] * 0.25;
    R += lejos * lp[1] * 0.25;
    envio += lejos * 1.4;

    const [wl, wr] = rev.paso(envio);
    L = hpL.paso(lpL.paso(L + wl * 0.55));
    R = hpR.paso(lpR.paso(R + wr * 0.55));

    if (t >= 0) {
      const i = Math.round(t * SR);
      if (i < buf[0].length) { buf[0][i] = L; buf[1][i] = R; }
    }
  }
  return D.masterizar(D.cerrarBucle(buf, N), { rmsDb: -23, picoDb: -1.5 });
}

/* -------------------------------------------------------------- musica */

const SI = 120;  // Si afinado sobre el zumbido
const nota = (semi) => SI * Math.pow(2, semi / 12);

// Voicings en semitonos desde Si2 (120 Hz). i - VI - III - VII en Si menor.
const ACORDES = [
  [-12, 0, 3, 7, 10, 14],        // Si m9
  [-16, -4, 3, 7, 10, 12],       // Sol maj9 (Sol Re Fa# La Si)
  [-9, 3, 7, 10, 14, 17],        // Re maj9
  [-14, -2, 5, 10, 12, 17]       // La sus2
];
const PENTATONICA = [12, 15, 17, 19, 22, 24, 27, 29, 31];

function musica () {
  const N = 96;
  const X = 5;
  const PRE = 12;
  const DUR = N / ACORDES.length;
  const rnd = D.azar(2026);
  const buf = D.estereo(N + X);
  const total = Math.round((PRE + N + X) * SR);

  // osciladores de los pads: 3 sierras desafinadas por nota
  const voces = [];
  ACORDES.forEach((ac, c) => {
    const n = ac.length;
    ac.forEach((semi, j) => {
      const grave = semi < -6;
      const f = nota(semi);
      const det = grave ? [0] : [-6, 0, 7];
      det.forEach((cents, d) => {
        voces.push({
          c,
          grave,
          f: f * Math.pow(2, cents / 1200),
          fase: rnd(),
          g: (grave ? 0.55 : 1 / Math.sqrt(n * det.length)) * (grave ? 1 : 0.9),
          pan: paneo(grave ? 0 : ((d - 1) * 0.55 + (j / n - 0.5) * 0.4))
        });
      });
    });
  });

  // campanas lejanas: una cada 3.5-7 s, en tiempo ciclico
  const campanas = [];
  for (let t = 1.5; t < N - 1; t += 3.5 + rnd() * 3.5) {
    if (rnd() < 0.18) continue;  // silencios: la melodia se "erosiona"
    campanas.push({ t, f: nota(PENTATONICA[Math.floor(rnd() * PENTATONICA.length)]) * (1 + (rnd() - 0.5) * 0.004), pan: paneo((rnd() - 0.5) * 1.2), g: 0.5 + rnd() * 0.5 });
  }

  const lp1 = [new D.Biquad('lp', 900, 0.6), new D.Biquad('lp', 900, 0.6)];
  const lp2 = [new D.Biquad('lp', 900, 0.6), new D.Biquad('lp', 900, 0.6)];
  const revPads = new D.Freeverb({ sala: 0.93, amortiguacion: 0.3, predelayMs: 35 });
  const revCamp = new D.Freeverb({ sala: 0.95, amortiguacion: 0.25, predelayMs: 60 });
  const cinta = new D.Cinta({ wowHz: 0.4, wowMs: 1.8, flutterHz: 5.7, flutterMs: 0.1, rnd });
  const hiss = D.rosa(rnd);
  const lpHiss = new D.Biquad('lp', 2200, 0.7);
  const fin = [new D.Biquad('lp', 8000, 0.7), new D.Biquad('lp', 8000, 0.7)];
  const hp = [new D.Biquad('hp', 42, 0.7), new D.Biquad('hp', 42, 0.7)];
  const calor = [new D.Biquad('graves', 160, 0.7, 1.5), new D.Biquad('graves', 160, 0.7, 1.5)];

  for (let k = 0; k < total; k++) {
    const t = k / SR - PRE;
    const u = ((t % N) + N) % N;

    // peso de cada acorde: fundidos de 6 s entre uno y otro
    const pesos = ACORDES.map((_, c) => {
      let uc = (((u - c * DUR) % N) + N) % N;
      if (uc > N - DUR) uc -= N;
      return suave(-3, 3, uc) * (1 - suave(DUR - 3, DUR + 3, uc));
    });

    if (k % 32 === 0) {
      const fc = 1150 + 450 * Math.sin(TAU * t / 32) + 180 * Math.sin(TAU * t / 11.3);
      for (const f of [lp1, lp2]) { f[0].ajustar(fc, 0.62); f[1].ajustar(fc * 1.04, 0.62); }
    }

    let pl = 0;
    let pr = 0;
    let sub = 0;
    for (const v of voces) {
      const dt = v.f / SR;
      v.fase += dt;
      if (v.fase >= 1) v.fase -= 1;
      const w = pesos[v.c];
      if (w <= 0) continue;
      if (v.grave) {
        // bajo con 2.o y 3.er armonico: se oye tambien en altavoces pequenos
        const ph = TAU * v.fase;
        sub += (Math.sin(ph) + 0.35 * Math.sin(2 * ph) + 0.15 * Math.sin(3 * ph)) * v.g * w;
      } else {
        const s = D.sierra(v.fase, dt) * v.g * w;
        pl += s * v.pan[0];
        pr += s * v.pan[1];
      }
    }
    pl = lp2[0].paso(lp1[0].paso(pl)) * 0.32;
    pr = lp2[1].paso(lp1[1].paso(pr)) * 0.32;
    // respiracion lenta del pad
    const resp = 0.85 + 0.15 * Math.sin(TAU * t / 16 + 1.3);
    pl *= resp; pr *= resp;
    sub *= 0.11;

    // campanas (FM suave), sumando tambien la vuelta anterior del bucle
    let cl = 0;
    let cr = 0;
    for (const b of campanas) {
      // la campana de esta vuelta y la de la vuelta anterior (su cola)
      for (const d of [u - b.t, u - b.t + N]) {
        if (d < 0 || d > 7) continue;
        const ataque = Math.min(1, d / 0.006);
        const indice = 2.2 * Math.exp(-d / 0.35);
        const s = Math.sin(TAU * b.f * d + indice * Math.sin(TAU * b.f * 2 * d)) * Math.exp(-d / 1.9) * ataque * 0.09 * b.g;
        cl += s * b.pan[0];
        cr += s * b.pan[1];
      }
    }

    const [rpl, rpr] = revPads.paso(pl + pr + sub * 0.3);
    const [rcl, rcr] = revCamp.paso((cl + cr) * 1.2);
    let L = pl * 0.7 + sub + rpl * 0.6 + cl * 0.35 + rcl * 0.9;
    let R = pr * 0.7 + sub + rpr * 0.6 + cr * 0.35 + rcr * 0.9;
    [L, R] = cinta.paso(L, R);
    const h = lpHiss.paso(hiss()) * 0.006;
    L = Math.tanh((L + h) * 1.3) / 1.3;
    R = Math.tanh((R + h) * 1.3) / 1.3;
    L = hp[0].paso(calor[0].paso(fin[0].paso(L)));
    R = hp[1].paso(calor[1].paso(fin[1].paso(R)));

    if (t >= 0) {
      const i = Math.round(t * SR);
      if (i < buf[0].length) { buf[0][i] = L; buf[1][i] = R; }
    }
  }
  return D.masterizar(D.cerrarBucle(buf, N), { rmsDb: -21, picoDb: -1.5 });
}

/* ------------------------------------------------------ efectos cortos */

function corto (segundos, semilla, sala, humedo, fn, picoDb) {
  const rnd = D.azar(semilla);
  const buf = D.estereo(segundos);
  const rev = new D.Freeverb({ sala, amortiguacion: 0.45, predelayMs: 8 });
  for (let k = 0; k < buf[0].length; k++) {
    const t = k / SR;
    const [l, r] = fn(t, rnd);
    const [wl, wr] = rev.paso((l + r) * 0.5);
    buf[0][k] = l + wl * humedo;
    buf[1][k] = r + wr * humedo;
  }
  let pico = 0;
  for (let k = 0; k < buf[0].length; k++) pico = Math.max(pico, Math.abs(buf[0][k]), Math.abs(buf[1][k]));
  const g = Math.pow(10, picoDb / 20) / (pico || 1);
  for (let k = 0; k < buf[0].length; k++) { buf[0][k] *= g; buf[1][k] *= g; }
  return D.fundidos(buf, 0.0005, 0.04);
}

function hover () {
  const bp = new D.Biquad('bp', 3200, 1.3);
  return corto(0.22, 11, 0.45, 0.18, (t, rnd) => {
    const ruido = t < 0.003 ? (rnd() * 2 - 1) : 0;
    const s = bp.paso(ruido) * 0.8 + Math.sin(TAU * 1900 * t) * Math.exp(-t / 0.012) * 0.35;
    return [s, s];
  }, -9);
}

function click () {
  const bpClack = new D.Biquad('bp', 1700, 0.8);
  const bpTink = new D.Biquad('bp', 6000, 3);
  const buzz = crearBuzz(2400);
  return corto(0.9, 12, 0.55, 0.22, (t, rnd) => {
    let s = 0;
    // clac del interruptor y su rebote
    if (t < 0.006) s += bpClack.paso(rnd() * 2 - 1) * 1.2; else s += bpClack.paso(0);
    if (t >= 0.024 && t < 0.028) s += (rnd() * 2 - 1) * 0.25;
    s += Math.sin(TAU * 160 * t) * Math.exp(-t / 0.06) * 0.6;
    // cebador: dos "tinks"
    for (const tk of [0.065, 0.15]) {
      const d = t - tk;
      if (d >= 0 && d < 0.05) s += Math.sin(TAU * 5200 * d) * Math.exp(-d / 0.012) * 0.22 + bpTink.paso(d < 0.002 ? rnd() * 2 - 1 : 0) * 0.3;
    }
    // el tubo arranca: zumbido que titubea y luego se asienta
    if (t > 0.08) {
      const d = t - 0.08;
      const titubeo = d < 0.12 ? (Math.sin(TAU * 31 * d) > -0.2 ? 1 : 0.15) : 1;
      const env = Math.min(1, d / 0.03) * Math.exp(-Math.max(0, d - 0.25) / 0.18) * titubeo;
      s += (zumbido(t, 120) * 0.8 + buzz(t, 120) * 1.1) * env * 0.45;
    }
    return [s, s * 0.92];
  }, -5);
}

function parpadeo () {
  // mismo patron que la imagen (app.js): 0 / 70 / 150 / 210 ms
  const cortes = [0, 0.07, 0.15, 0.21];
  const bpArco = new D.Biquad('bp', 3100, 1.1);
  const bpPop = new D.Biquad('bp', 1400, 0.9);
  const buzz = crearBuzz(2600);
  return corto(0.65, 13, 0.6, 0.25, (t, rnd) => {
    let s = 0;
    for (const c of cortes) {
      const d = t - c;
      if (d >= 0 && d < 0.004) s += bpPop.paso((rnd() * 2 - 1) * 1.4);
    }
    if (t < 0.26) {
      // arco electrico: ruido con modulacion rapida e irregular
      const am = Math.max(0, Math.sin(TAU * 87 * t) + (rnd() - 0.5) * 1.4);
      s += bpArco.paso((rnd() * 2 - 1) * am) * 0.55;
      s += buzz(t, 120) * 0.9 * (0.5 + 0.5 * Math.sin(TAU * 13 * t));
    } else {
      s += bpArco.paso(0);
    }
    return [s, s];
  }, -6);
}

function jugar () {
  const rnd0 = D.azar(14);
  const rosaL = D.rosa(rnd0);
  const rosaR = D.rosa(D.azar(15));
  const bpL = new D.Biquad('bp', 300, 1.2);
  const bpR = new D.Biquad('bp', 300, 1.2);
  const lpImpacto = new D.Biquad('lp', 180, 0.7);
  const buzz = crearBuzz(2400);
  let faseZ = 0;
  let faseSub = 0;
  return corto(3.2, 16, 0.9, 0.38, (t, rnd) => {
    // los tubos se mueren: el zumbido cae de 120 a 30 Hz
    let s = 0;
    if (t < 0.85) {
      const f = 120 * Math.pow(0.25, t / 0.85);
      faseZ += f / SR;
      const env = 1 - suave(0.4, 0.85, t);
      s += (Math.sin(TAU * faseZ) + 0.5 * Math.sin(TAU * faseZ * 3) + buzz(t, f) * 0.8) * env * 0.35;
    }
    // barrido de aire (sube hasta el corte y luego cae)
    const fc = t < 0.85 ? 260 * Math.pow(10, t / 0.85) : 2600 * Math.pow(0.04, Math.min(1, (t - 0.85) / 1.6));
    if ((Math.round(t * SR)) % 32 === 0) { bpL.ajustar(fc, 1.1); bpR.ajustar(fc * 1.08, 1.1); }
    const envAire = suave(0, 0.8, t) * (1 - suave(0.9, 2.6, t));
    let L = s + bpL.paso(rosaL()) * envAire * 1.6;
    let R = s + bpR.paso(rosaR()) * envAire * 1.6;
    // impacto y caida grave
    if (t >= 0.85) {
      const d = t - 0.85;
      const f = 28 + 62 * Math.exp(-d / 0.45);
      faseSub += f / SR;
      const sub = Math.tanh(Math.sin(TAU * faseSub) * 1.6) * Math.exp(-d / 0.9) * 0.9;
      const golpe = lpImpacto.paso(d < 0.02 ? (rnd() * 2 - 1) : 0) * 2.5;
      L += sub + golpe;
      R += sub + golpe;
    }
    return [L, R];
  }, -3);
}

/* ------------------------------------------------------------- salida */

async function aOgg (nombre, audio, calidad) {
  const bytes = await D.guardarOgg(path.join(SALIDA, `${nombre}.ogg`), audio, calidad);
  console.log(`  ${nombre}.ogg  ${(audio[0].length / SR).toFixed(2)} s  ${(bytes / 1024).toFixed(0)} KB`);
}

module.exports = { ambiente, musica, hover, click, parpadeo, jugar };

if (require.main === module) (async () => {
  fs.mkdirSync(SALIDA, { recursive: true });
  const solo = process.argv[2];
  const lista = { ambiente: [ambiente, 5], musica: [musica, 5], hover: [hover, 4], click: [click, 4], parpadeo: [parpadeo, 4], jugar: [jugar, 5] };
  for (const [nombre, [fn, q]] of Object.entries(lista)) {
    if (solo && solo !== nombre) continue;
    const t0 = Date.now();
    const audio = fn();
    await aOgg(nombre, audio, q);
    console.log(`    (${((Date.now() - t0) / 1000).toFixed(1)} s)`);
  }
})();
