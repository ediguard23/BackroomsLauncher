#!/usr/bin/env node
'use strict';
/**
 * Los sonidos de la Fiesta (Nivel Fun, las Feeding Rooms) y del Anfitrion. Sintetizados aqui,
 * en mono (para que suenen en 3D) salvo el aire de la sala. Las voces van aparte (voces.js).
 *
 *   node tools/sonidos/fiesta.js [prefijo]   (sin prefijo, todos)
 *
 *  Ambiente (AmbienteFiesta, en el cliente)
 *   fiesta_ambiente        el aire de la sala en bucle: zumbido electrico y ventilacion (estereo)
 *   fiesta_musica1-2       musica de fiesta lejana por un altavoz viejo: una cajita de musica con
 *                          su bajo, desafinada y con la cinta que tiembla; la segunda, con una
 *                          nota que no toca y que al final se va quedando sin cuerda
 *   fiesta_globo1-3        un globo que cruje (goma que roza)
 *   fiesta_juguete1-3      juguetes que se encienden solos: uno que chirria al apretarlo, una
 *                          cajita de musica que se para y un juguete electronico que pita
 *  Lo de jugar
 *   fiesta_moneda          coger una moneda: dos notas de campanilla
 *   fiesta_tickets         coger tickets: la maquina que los escupe y el papel que se rasga
 *   laberinto_puerta       cruzar una puerta: el picaporte, la bisagra que cruje y el golpe al cerrar
 *   fiesta_torno_no        sin monedas: zumbador
 *   fiesta_objetivo        ya tienes los tickets: arpegio de fiesta (un poco desafinado)
 *   fiesta_regalo          dentro del regalo del Gran Premio: papel, un soplo que sube y un petardo
 *   semaforo_musica        la musiquilla de la luz verde, en bucle (xilofono)
 *   semaforo_silbato       te han pillado: silbato y un tobogan de nota que baja
 *   semaforo_fanfarria     meta: fanfarria de matasuegras
 *   semaforo_luz_verde/roja  la senal de cada luz (debajo de la voz que la canta, en voces.js)
 *  Suelos defectuosos, parkour y juegos
 *   suelo_zumbido          el zumbido electrico, muy bajo, de un suelo que falla (la pista)
 *   suelo_cruje            alguien se ha quedado encima: chasquido y crujido de tablero
 *   suelo_cae              cede: un fallo digital (como un salto de imagen) y el aire de la caida
 *   parkour_bolas          caer en la piscina de bolas
 *   parkour_fallo          a la piscina fuera de sitio: un "boing" y vuelta al principio
 *   parkour_sube           la trampilla que sube: motor y un soplo hacia arriba
 *   memoria_nota_*         las cuatro notas de las baldosas (xilofono: do, mi, sol, do)
 *   memoria_turno          te toca: dos campanillas
 *   memoria_error          fallo: un acorde desafinado que se cae
 *   regalos_ping           un regalo que tintinea (para orientarse)
 *   regalos_coger          coger un regalo: papel y una campanilla
 *   regalos_reloj          el tic del reloj de los ultimos segundos
 *   regalos_fin            se acabo el tiempo: timbre
 *  El Anfitrion
 *   anfitrion_paso1-4      zapatones de goma: un golpe blando y el chirrido de la suela
 *   anfitrion_detecta      te ha visto: un matasuegras desafinado y algo grave debajo
 *   anfitrion_respira1-3   respiracion ahogada dentro de la cabezota de espuma
 *   anfitrion_pierde       se va / te pierde: un globo que se deshincha
 *   anfitrion_abrazo       te abraza: un petardo de fiesta, la tela que aprieta y un "mmm" grave
 */

const fs = require('fs');
const path = require('path');
const D = require('./dsp');
const E = require('./expedicion');

const { SR } = D;
const TAU = Math.PI * 2;
const { mono, poner, reverb, pico, guardarMono, guardarEst, SALIDA } = E;

const nota = (n) => 440 * Math.pow(2, (n - 69) / 12); // MIDI -> Hz

/** Una sala de fiesta: reverberacion media, blanda (moqueta, globos y techo bajo). */
function sala (buf, mezcla = 0.35, tam = 0.6) {
  return reverb(buf, tam, mezcla, { amortiguacion: 0.55, predelayMs: 12 });
}

/** Por un altavoz viejo y lejos: banda estrecha, algo saturado y apagado. */
function altavozViejo (buf, { bajo = 300, alto = 2600, sat = 1.8 } = {}) {
  const hp = new D.Biquad('hp', bajo, 0.7);
  const lp = new D.Biquad('lp', alto, 0.7);
  const out = new Float32Array(buf.length);
  for (let i = 0; i < buf.length; i++) out[i] = Math.tanh(lp.paso(hp.paso(buf[i])) * sat) / sat;
  return out;
}

/** Nota de cajita de musica: un parcial claro y otros inarmonicos que se apagan antes. */
function cajita (f, t) {
  return Math.sin(TAU * f * t) * Math.exp(-t * 3.2) + Math.sin(TAU * f * 3.01 * t) * 0.25 * Math.exp(-t * 9) + Math.sin(TAU * f * 5.4 * t) * 0.08 * Math.exp(-t * 18);
}

/* ============================================================== ambiente */

function ambiente () {
  const dur = 16;
  const [l, r] = D.estereo(dur);
  const rl = D.azar(11); const rr = D.azar(12);
  const bl = D.marron(rl); const br = D.marron(rr);
  const lpl = new D.Biquad('lp', 420, 0.7); const lpr = new D.Biquad('lp', 420, 0.7);
  for (let i = 0; i < l.length; i++) {
    const t = i / SR;
    const zum = (Math.sin(TAU * 60 * t) + Math.sin(TAU * 120 * t) * 0.5 + Math.sin(TAU * 180 * t) * 0.25) * 0.03;
    const vent = 0.9 + 0.1 * Math.sin(TAU * t / 4);
    l[i] = lpl.paso(bl()) * 0.7 * vent + zum;
    r[i] = lpr.paso(br()) * 0.7 * vent + zum * 0.95;
  }
  return D.masterizar(D.cerrarBucle([l, r], dur - 1), { rmsDb: -30, picoDb: -8 });
}

/* ============================================================== musica */

// la cancion de la fiesta (propia): Do mayor, 6/8 saltarin. [nota MIDI, tiempo en corcheas, duracion]
const CANCION = [
  [72, 0, 1], [76, 1, 1], [79, 2, 1], [76, 3, 1], [74, 4, 1], [72, 5, 1],
  [74, 6, 2], [77, 8, 1], [76, 9, 2], [72, 11, 1],
  [69, 12, 1], [72, 13, 1], [76, 14, 1], [74, 15, 1], [72, 16, 1], [71, 17, 1],
  [72, 18, 3], [67, 21, 3],
  [77, 24, 1], [76, 25, 1], [74, 26, 1], [72, 27, 1], [74, 28, 1], [76, 29, 1],
  [79, 30, 2], [76, 32, 1], [72, 33, 3],
  [74, 36, 1], [76, 37, 1], [77, 38, 1], [74, 39, 1], [71, 40, 1], [67, 41, 1],
  [72, 42, 6]
];
const BAJO = [48, 55, 48, 55, 53, 48, 45, 52, 48, 43, 53, 48, 50, 43, 48, 48];

function musica (variante) {
  const corchea = 0.2;
  const vueltas = 3;
  const largo = 48 * corchea;
  const dur = largo * vueltas + 2;
  const buf = mono(dur);
  const rnd = D.azar(30 + variante);
  // la cinta tiembla (wow y flutter) y todo va un pelin desafinado
  const temblor = (t) => 1 + Math.sin(TAU * 0.6 * t) * 0.009 + Math.sin(TAU * 5.3 * t) * 0.002 - 0.006;
  for (let v = 0; v < vueltas; v++) {
    for (const [n, c, d] of CANCION) {
      let m = n;
      if (variante === 2 && v === 1 && c === 30) m = n + 1; // la nota que no toca
      // la segunda se queda sin cuerda: en la ultima vuelta, cada nota mas tarde y mas grave
      const sinCuerda = variante === 2 && v === vueltas - 1 ? Math.pow(c / 48, 2) : 0;
      const t0 = v * largo + c * corchea * (1 + sinCuerda * 0.6);
      const f = nota(m) * (1 - sinCuerda * 0.06);
      poner(buf, t0, d * corchea * 2.5, (t) => cajita(f * temblor(t0 + t), t) * 0.32);
    }
    BAJO.forEach((n, i) => {
      const t0 = v * largo + i * 3 * corchea;
      const f = nota(n);
      poner(buf, t0, 3 * corchea, (t) => {
        const s = Math.sin(TAU * f * temblor(t0 + t) * t);
        return Math.tanh(s * 2.5) * 0.12 * Math.exp(-t * 4);
      });
    });
  }
  // un siseo de cinta debajo
  const hp = new D.Biquad('hp', 2500, 0.7);
  for (let i = 0; i < buf.length; i++) buf[i] += hp.paso(rnd() * 2 - 1) * 0.008;
  return pico(sala(altavozViejo(buf, { alto: 3000 }), 0.45, 0.75), -3);
}

/* ============================================================== globos y juguetes */

function globo (semilla) {
  const rnd = D.azar(400 + semilla);
  const dur = 0.5 + rnd() * 0.5;
  const buf = mono(dur + 0.4);
  // goma que roza: tirones cortos (pegarse y soltarse) de un tono que sube y baja
  let t = 0.02;
  const f0 = 380 + rnd() * 300;
  while (t < dur) {
    const tt = t;
    const f = f0 * (1 + Math.sin(tt * 9) * 0.25);
    const d = 0.012 + rnd() * 0.02;
    poner(buf, tt, d + 0.02, (u) => Math.sin(TAU * f * u) * Math.exp(-u * 90) * (0.4 + rnd() * 0.5));
    t += d + rnd() * 0.03;
  }
  return pico(sala(buf, 0.3, 0.5), -6);
}

function juguete (variante) {
  const rnd = D.azar(500 + variante);
  if (variante === 1) {
    // juguete de goma que chirria al apretarlo y al soltarlo
    const buf = mono(1.2);
    for (const [t0, f0, f1] of [[0.05, 1300, 900], [0.55, 800, 1250]]) {
      const bp = new D.Biquad('bp', 1600, 3);
      let fase = 0;
      poner(buf, t0, 0.35, (t) => {
        const f = f0 + (f1 - f0) * (t / 0.35);
        fase += f / SR;
        const s = Math.sin(TAU * fase) + 0.4 * Math.sin(TAU * fase * 2.01);
        return (bp.paso(s + (rnd() - 0.5) * 0.6)) * Math.sin(Math.PI * t / 0.35) * 0.8;
      });
    }
    return pico(sala(buf, 0.3, 0.55), -4);
  }
  if (variante === 2) {
    // una cajita de musica que se para: cinco notas cada vez mas lentas
    const buf = mono(3.4);
    const notas = [79, 76, 72, 74, 71];
    let t0 = 0.02;
    notas.forEach((n, i) => {
      const f = nota(n) * (1 - i * 0.012);
      poner(buf, t0, 1.6, (t) => cajita(f, t) * 0.5);
      t0 += 0.22 + i * i * 0.08;
    });
    return pico(sala(buf, 0.35, 0.6), -5);
  }
  // un juguete electronico: pitidos de "jugamos?" y un motorcito que se atasca
  const buf = mono(1.6);
  [[0, 1568], [0.14, 1976], [0.28, 2349], [0.5, 1568]].forEach(([t0, f]) => {
    poner(buf, t0, 0.1, (t) => Math.sign(Math.sin(TAU * f * t)) * 0.18 * Math.min(1, (0.1 - t) * 60));
  });
  const lp = new D.Biquad('lp', 900, 0.8);
  poner(buf, 0.7, 0.8, (t) => lp.paso(Math.sign(Math.sin(TAU * (90 - t * 50) * t)) + (rnd() - 0.5) * 0.4) * 0.3 * Math.sin(Math.PI * t / 0.8));
  return pico(sala(buf, 0.25, 0.5), -6);
}

/* ============================================================== jugar */

function moneda () {
  const buf = mono(1.2);
  poner(buf, 0, 1.0, (t) => (Math.sin(TAU * nota(95) * t) + Math.sin(TAU * nota(95) * 2.7 * t) * 0.2) * Math.exp(-t * 6) * 0.5);
  poner(buf, 0.08, 1.0, (t) => (Math.sin(TAU * nota(100) * t) + Math.sin(TAU * nota(100) * 2.7 * t) * 0.2) * Math.exp(-t * 4) * 0.55);
  return pico(sala(buf, 0.2, 0.4), -2);
}

function tickets () {
  const rnd = D.azar(601);
  const buf = mono(1.0);
  // la maquinita: tic-tic-tic del rodillo
  for (let k = 0; k < 7; k++) {
    const bp = new D.Biquad('bp', 3200 + rnd() * 600, 8);
    poner(buf, 0.02 + k * 0.055, 0.03, (t) => bp.paso(rnd() * 2 - 1) * Math.exp(-t * 200) * 2.2);
  }
  // y el papel que se rasga
  const bp = new D.Biquad('bp', 2600, 0.9);
  poner(buf, 0.45, 0.22, (t) => bp.paso(rnd() * 2 - 1) * (0.6 + 0.4 * Math.sin(TAU * 60 * t)) * Math.sin(Math.PI * t / 0.22) * 1.1);
  return pico(sala(buf, 0.2, 0.4), -3);
}

function tornoNo () {
  const buf = mono(0.8);
  const bp = new D.Biquad('bp', 700, 1.5);
  for (const t0 of [0.02, 0.32]) {
    poner(buf, t0, 0.22, (t) => bp.paso(D.sierra((t * 140) % 1, 140 / SR)) * Math.min(1, (0.22 - t) * 40) * 1.4);
  }
  return pico(sala(buf, 0.2, 0.4), -3);
}

function objetivo () {
  const buf = mono(2.4);
  [72, 76, 79, 84, 88].forEach((n, i) => {
    poner(buf, i * 0.11, 1.4, (t) => cajita(nota(n) * (1 + (i === 4 ? 0.012 : 0)), t) * 0.5);
  });
  // matasuegras al final
  const bp = new D.Biquad('bp', 1100, 2);
  poner(buf, 0.6, 0.9, (t) => bp.paso(D.sierra((t * (440 + t * 30)) % 1, 440 / SR)) * Math.min(1, t * 20) * Math.min(1, (0.9 - t) * 8) * 0.9);
  return pico(sala(buf, 0.35, 0.6), -2);
}

function regalo () {
  const rnd = D.azar(621);
  const buf = mono(2.4);
  const bp = new D.Biquad('bp', 3000, 0.8);
  poner(buf, 0.0, 0.9, (t) => bp.paso(rnd() * 2 - 1) * (0.5 + 0.5 * Math.sin(TAU * 7 * t)) * Math.sin(Math.PI * t / 0.9) * 0.6);
  const hp = new D.Biquad('bp', 800, 1.2);
  poner(buf, 0.6, 1.1, (t) => hp.paso(rnd() * 2 - 1) * Math.pow(t / 1.1, 2) * 0.9);
  poner(buf, 1.7, 0.6, (t) => (rnd() * 2 - 1) * Math.exp(-t * 30) * 1.5 + Math.sin(TAU * 90 * t) * Math.exp(-t * 20));
  return pico(sala(buf, 0.4, 0.7), -1);
}

/** La musiquilla del semaforo: 16 corcheas de xilofono, cerrada en bucle (las colas dan la vuelta). */
function semaforoMusica () {
  const negra = 0.36;
  const notas = [[76, 0], [76, 0.5], [79, 1], [76, 1.5], [72, 2], [74, 2.5], [76, 3], [74, 3.5],
    [72, 4], [72, 4.5], [76, 5], [79, 5.5], [81, 6], [79, 6.5], [76, 7], [74, 7.5]];
  const largo = 8 * negra;
  const buf = new Float32Array(Math.round(largo * SR));
  const xilo = (f, t) => (Math.sin(TAU * f * t) + Math.sin(TAU * f * 3.93 * t) * 0.3 * Math.exp(-t * 20)) * Math.exp(-t * 7);
  for (const [n, b] of notas) {
    const i0 = Math.round(b * negra * SR);
    const f = nota(n);
    for (let i = 0; i < Math.round(0.8 * SR); i++) buf[(i0 + i) % buf.length] += xilo(f, i / SR) * 0.4;
  }
  for (let k = 0; k < 8; k++) {
    const i0 = Math.round(k * negra * SR);
    const f = nota(k % 2 === 0 ? 48 : 55);
    for (let i = 0; i < Math.round(0.3 * SR); i++) buf[(i0 + i) % buf.length] += Math.sin(TAU * f * i / SR) * Math.exp(-i / SR * 12) * 0.25;
  }
  let m = 0;
  for (const v of buf) m = Math.max(m, Math.abs(v));
  for (let i = 0; i < buf.length; i++) buf[i] = buf[i] / m * 0.8;
  return buf;
}

function silbato () {
  const rnd = D.azar(631);
  const buf = mono(1.6);
  poner(buf, 0, 0.5, (t) => (Math.sin(TAU * (2900 + Math.sin(TAU * 32 * t) * 120) * t) + (rnd() - 0.5) * 0.2) * Math.min(1, t * 60) * Math.min(1, (0.5 - t) * 30) * 0.6);
  let fase = 0;
  poner(buf, 0.55, 0.9, (t) => {
    fase += (900 - t * 650) / SR;
    return Math.sin(TAU * fase) * Math.sin(Math.PI * t / 0.9) * 0.5;
  });
  return pico(sala(buf, 0.3, 0.6), -2);
}

function fanfarria () {
  const buf = mono(2.2);
  [[67, 0, 0.18], [72, 0.2, 0.18], [76, 0.4, 0.18], [79, 0.6, 0.9]].forEach(([n, t0, d]) => {
    const bp = new D.Biquad('bp', 1300, 1.6);
    const f = nota(n);
    poner(buf, t0, d + 0.05, (t) => bp.paso(D.sierra((f * t * (1 + Math.sin(TAU * 6 * t) * 0.01)) % 1, f / SR)) * Math.min(1, t * 40) * Math.min(1, (d + 0.05 - t) * 20) * 1.2);
  });
  return pico(sala(buf, 0.35, 0.6), -2);
}

function senal (verde) {
  const buf = mono(0.8);
  const f = verde ? [nota(84), nota(88)] : [nota(64), nota(61)];
  f.forEach((x, i) => poner(buf, i * 0.14, 0.6, (t) => (verde ? Math.sin(TAU * x * t) : Math.sign(Math.sin(TAU * x * t)) * 0.5) * Math.exp(-t * (verde ? 6 : 4)) * 0.6));
  return pico(sala(buf, 0.3, 0.5), -3);
}

/* ============================================================== el Anfitrion */

function paso (semilla) {
  const rnd = D.azar(700 + semilla);
  const buf = mono(0.5);
  const lp = new D.Biquad('lp', 260, 0.8);
  poner(buf, 0, 0.25, (t) => (Math.sin(TAU * (70 + rnd() * 20) * t) * 0.8 + lp.paso(rnd() * 2 - 1) * 0.5) * (1 - Math.exp(-t * 300)) * Math.exp(-t * 18));
  // el chirrido de la suela de goma, no siempre igual
  const f0 = 900 + rnd() * 500;
  const d = 0.06 + rnd() * 0.08;
  poner(buf, 0.03 + rnd() * 0.03, d, (t) => Math.sin(TAU * (f0 - t * 2500) * t) * Math.sin(Math.PI * t / d) * (0.25 + rnd() * 0.2));
  return pico(sala(buf, 0.25, 0.5), -4);
}

function detecta () {
  const buf = mono(2.4);
  // el matasuegras: lengueta de sierra con vibrato que se hincha, desafinado contra otro
  for (const [f, g, s] of [[349, 1, 0], [370, 0.7, 0.05]]) {
    const bp = new D.Biquad('bp', 1200, 1.4);
    let fase = 0;
    poner(buf, s, 1.3, (t) => {
      const ff = f * (1 + Math.sin(TAU * 7 * t) * 0.02 + t * 0.05);
      fase = (fase + ff / SR) % 1;
      return bp.paso(D.sierra(fase, ff / SR)) * Math.min(1, t * 12) * Math.min(1, (1.3 - t) * 6) * g;
    });
  }
  // y debajo algo grave, como un golpe de tambor de feria en una sala vacia
  poner(buf, 0, 2.0, (t) => Math.sin(TAU * (55 - t * 8) * t) * Math.exp(-t * 2.2) * 0.9);
  return pico(sala(buf, 0.5, 0.8), -1);
}

function respira (semilla) {
  const rnd = D.azar(720 + semilla);
  const buf = mono(2.4);
  const bp = new D.Biquad('bp', 650 + rnd() * 200, 1.6);
  const lp = new D.Biquad('lp', 900, 0.7);
  for (const [t0, d, g] of [[0.05, 0.9, 0.8], [1.1, 1.1, 1.0]]) {
    poner(buf, t0, d, (t) => lp.paso(bp.paso(rnd() * 2 - 1)) * Math.pow(Math.sin(Math.PI * t / d), 1.6) * g * 2.2);
  }
  return pico(sala(buf, 0.3, 0.45), -6);
}

function pierde () {
  const rnd = D.azar(731);
  const buf = mono(1.8);
  let fase = 0;
  poner(buf, 0, 1.5, (t) => {
    const f = 700 * Math.exp(-t * 1.2) * (1 + Math.sin(TAU * 23 * t) * 0.06) + 90;
    fase = (fase + f / SR) % 1;
    return (D.sierra(fase, f / SR) * 0.3 + (rnd() - 0.5) * 0.5) * Math.min(1, t * 30) * Math.exp(-t * 1.4);
  });
  const lp = new D.Biquad('lp', 2200, 0.7);
  for (let i = 0; i < buf.length; i++) buf[i] = lp.paso(buf[i]);
  return pico(sala(buf, 0.35, 0.6), -3);
}

function abrazo () {
  const rnd = D.azar(741);
  const buf = mono(2.2);
  poner(buf, 0, 0.4, (t) => (rnd() * 2 - 1) * Math.exp(-t * 40) * 1.4 + Math.sin(TAU * 110 * t) * Math.exp(-t * 25) * 0.6);
  const bp = new D.Biquad('bp', 900, 0.8);
  poner(buf, 0.15, 1.2, (t) => bp.paso(rnd() * 2 - 1) * (0.5 + 0.5 * Math.sin(TAU * 3 * t)) * Math.sin(Math.PI * t / 1.2) * 0.8);
  // el "mmm" grave, ahogado dentro de la cabeza
  const lp = new D.Biquad('lp', 500, 0.7);
  poner(buf, 0.5, 1.5, (t) => lp.paso(D.sierra((t * 95 * (1 + Math.sin(TAU * 5 * t) * 0.03)) % 1, 95 / SR)) * Math.sin(Math.PI * t / 1.5) * 1.2);
  return pico(sala(buf, 0.35, 0.6), -1);
}

/* ============================================================== suelos, parkour y juegos */

function sueloZumbido () {
  const rnd = D.azar(801);
  const buf = mono(1.2);
  poner(buf, 0, 1.1, (t) => (Math.sin(TAU * 120 * t) * 0.5 + Math.sin(TAU * 240 * t) * 0.3 + (rnd() - 0.5) * 0.2 * (Math.sin(TAU * 7 * t) > 0.6 ? 1 : 0))
    * Math.sin(Math.PI * t / 1.1) * 0.5);
  return pico(buf, -10);
}

function sueloCruje () {
  const rnd = D.azar(811);
  const buf = mono(0.9);
  for (let k = 0; k < 9; k++) {
    const bp = new D.Biquad('bp', 700 + rnd() * 900, 4);
    poner(buf, 0.02 + rnd() * 0.6, 0.05, (t) => bp.paso(rnd() * 2 - 1) * Math.exp(-t * 80) * 2);
  }
  const lp = new D.Biquad('lp', 400, 0.8);
  poner(buf, 0, 0.7, (t) => lp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 0.7) * 0.5);
  return pico(sala(buf, 0.2, 0.4), -3);
}

function sueloCae () {
  const rnd = D.azar(821);
  const buf = mono(1.6);
  // el fallo digital: tramos de onda cuadrada que saltan de tono, entrecortados
  let t0 = 0;
  while (t0 < 0.45) {
    const f = 200 + rnd() * 1800;
    const d = 0.02 + rnd() * 0.05;
    poner(buf, t0, d, (t) => Math.sign(Math.sin(TAU * f * t)) * 0.4);
    t0 += d + rnd() * 0.02;
  }
  const bp = new D.Biquad('bp', 600, 0.8);
  poner(buf, 0.3, 1.2, (t) => bp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 1.2) * 0.9);
  return pico(sala(buf, 0.3, 0.6), -2);
}

function bolas () {
  const rnd = D.azar(831);
  const buf = mono(1.0);
  for (let k = 0; k < 26; k++) {
    const f = 900 + rnd() * 1500;
    poner(buf, rnd() * 0.5, 0.06, (t) => Math.sin(TAU * f * t) * Math.exp(-t * 70) * (0.3 + rnd() * 0.3));
  }
  return pico(sala(buf, 0.25, 0.5), -3);
}

function parkourFallo () {
  const buf = mono(1.0);
  let fase = 0;
  poner(buf, 0, 0.6, (t) => {
    fase += (220 + Math.sin(TAU * 9 * t) * 80 * Math.exp(-t * 3)) / SR;
    return Math.sin(TAU * fase) * Math.exp(-t * 4) * 0.8;
  });
  return pico(sala(buf, 0.25, 0.5), -3);
}

function parkourSube () {
  const rnd = D.azar(841);
  const buf = mono(2.0);
  const lp = new D.Biquad('lp', 300, 0.8);
  poner(buf, 0, 1.6, (t) => lp.paso(D.sierra((t * (60 + t * 40)) % 1, 80 / SR)) * Math.sin(Math.PI * t / 1.6) * 0.8);
  const hp = new D.Biquad('bp', 1500, 0.7);
  poner(buf, 0.5, 1.4, (t) => hp.paso(rnd() * 2 - 1) * Math.pow(Math.sin(Math.PI * t / 1.4), 2) * 0.6);
  return pico(sala(buf, 0.3, 0.5), -2);
}

function memoriaNota (n) {
  const buf = mono(1.0);
  const f = nota(n);
  poner(buf, 0, 1.0, (t) => (Math.sin(TAU * f * t) + Math.sin(TAU * f * 3.93 * t) * 0.3 * Math.exp(-t * 20)) * Math.exp(-t * 5) * 0.8);
  return pico(sala(buf, 0.3, 0.5), -2);
}

function memoriaTurno () {
  const buf = mono(1.0);
  [[0, 88], [0.12, 93]].forEach(([t0, n]) => poner(buf, t0, 0.8, (t) => Math.sin(TAU * nota(n) * t) * Math.exp(-t * 6) * 0.5));
  return pico(sala(buf, 0.25, 0.5), -3);
}

function memoriaError () {
  const buf = mono(1.4);
  for (const n of [59, 60, 66]) {
    poner(buf, 0, 1.2, (t) => Math.sign(Math.sin(TAU * nota(n) * (1 - t * 0.15) * t)) * Math.exp(-t * 2.5) * 0.25);
  }
  const lp = new D.Biquad('lp', 1800, 0.7);
  for (let i = 0; i < buf.length; i++) buf[i] = lp.paso(buf[i]);
  return pico(sala(buf, 0.25, 0.5), -2);
}

function regalosPing () {
  const buf = mono(1.4);
  [[0, 96], [0.09, 100], [0.18, 103]].forEach(([t0, n]) => poner(buf, t0, 1.0, (t) => Math.sin(TAU * nota(n) * t) * Math.exp(-t * 7) * 0.4));
  return pico(sala(buf, 0.35, 0.6), -6);
}

function regalosCoger () {
  const rnd = D.azar(851);
  const buf = mono(1.0);
  const bp = new D.Biquad('bp', 3000, 0.9);
  poner(buf, 0, 0.3, (t) => bp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 0.3) * 0.7);
  poner(buf, 0.18, 0.8, (t) => Math.sin(TAU * nota(91) * t) * Math.exp(-t * 6) * 0.5);
  return pico(sala(buf, 0.25, 0.5), -2);
}

function regalosReloj () {
  const rnd = D.azar(861);
  const buf = mono(0.3);
  const bp = new D.Biquad('bp', 2500, 9);
  poner(buf, 0, 0.05, (t) => bp.paso(rnd() * 2 - 1) * Math.exp(-t * 160) * 3);
  return pico(buf, -4);
}

function regalosFin () {
  const buf = mono(1.4);
  poner(buf, 0, 1.0, (t) => Math.sign(Math.sin(TAU * 440 * t)) * Math.sin(TAU * 18 * t) * Math.min(1, (1 - t) * 6) * 0.3);
  return pico(sala(buf, 0.25, 0.5), -2);
}

/* ============================================================== el laberinto */

// la puerta: el picaporte, la bisagra que cruje (diente de sierra que sube y baja con temblor) y el golpe
function puerta () {
  const rnd = D.azar(901);
  const buf = mono(1.3);
  const bp = new D.Biquad('bp', 2400, 6);
  poner(buf, 0, 0.05, (t) => bp.paso(rnd() * 2 - 1) * Math.exp(-t * 120) * 2.2);
  const cr = new D.Biquad('bp', 900, 3);
  let fase = 0;
  poner(buf, 0.08, 0.6, (t) => {
    fase += (180 + Math.sin(t * 9) * 60 + rnd() * 40) / SR;
    return cr.paso(D.sierra(fase % 1, 200 / SR)) * Math.sin(Math.PI * t / 0.6) * 0.5;
  });
  const lp = new D.Biquad('lp', 180, 0.8);
  poner(buf, 0.86, 0.35, (t) => lp.paso(rnd() * 2 - 1) * Math.exp(-t * 18) * 3.0);
  return pico(sala(buf, 0.3, 0.5), -3);
}

// el localizador: una campana de feria, tres veces y cada vez mas alto (suena desde la puerta de la salida)
function localizador () {
  const buf = mono(3.0);
  for (let k = 0; k < 3; k++) {
    [[0, 84], [0.12, 88], [0.24, 91]].forEach(([t0, n]) => {
      const f = nota(n + k * 2);
      poner(buf, k * 0.85 + t0, 1.2, (t) => (Math.sin(TAU * f * t) + Math.sin(TAU * f * 2.76 * t) * 0.25 * Math.exp(-t * 9)) * Math.exp(-t * 3.2) * 0.42);
    });
  }
  return pico(sala(buf, 0.3, 0.7), -1);
}

// el zumbido de la puerta de la salida: un fluorescente viejo, con algun chasquido
function salidaZumbido () {
  const rnd = D.azar(911);
  const buf = mono(1.6);
  const bp = new D.Biquad('bp', 2600, 5);
  poner(buf, 0, 1.6, (t) => {
    const h = Math.sin(TAU * 100 * t) * 0.5 + Math.sin(TAU * 200 * t) * 0.3 + Math.sin(TAU * 300 * t) * 0.15;
    const chasquido = rnd() > 0.9993 ? 3 : 0;
    return (h * (0.8 + Math.sin(TAU * 3 * t) * 0.2) + bp.paso((rnd() * 2 - 1) * chasquido)) * Math.min(1, t * 8, (1.6 - t) * 8) * 0.5;
  });
  return pico(buf, -8);
}

// el noclip: la imagen que se rompe (tramos de onda cuadrada que saltan) y caer hacia abajo
function noclip () {
  const rnd = D.azar(921);
  const buf = mono(1.8);
  let t0 = 0;
  while (t0 < 0.6) {
    const f = 120 + rnd() * 2400;
    const d = 0.015 + rnd() * 0.05;
    poner(buf, t0, d, (t) => Math.sign(Math.sin(TAU * f * t)) * 0.35);
    t0 += d + rnd() * 0.03;
  }
  let fase = 0;
  poner(buf, 0.3, 1.4, (t) => {
    fase += (300 * Math.exp(-t * 2.2) + 40) / SR;
    return Math.sin(TAU * fase) * Math.sin(Math.PI * t / 1.4) * 0.6;
  });
  const bp = new D.Biquad('bp', 500, 0.7);
  poner(buf, 0.3, 1.4, (t) => bp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 1.4) * 0.7);
  return pico(sala(buf, 0.3, 0.6), -2);
}

// el tobogan: el roce del plastico que sube y baja, con los golpecitos de las juntas
function tobogan () {
  const rnd = D.azar(931);
  const buf = mono(2.2);
  const bp = new D.Biquad('bp', 700, 1.2);
  poner(buf, 0, 2.0, (t) => {
    bp.ajustar(500 + Math.sin(Math.PI * t / 2.0) * 900);
    return bp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 2.0) * 0.9;
  });
  const lp = new D.Biquad('lp', 160, 0.8);
  for (let k = 0; k < 6; k++) {
    poner(buf, 0.2 + k * 0.28, 0.12, (t) => lp.paso(rnd() * 2 - 1) * Math.exp(-t * 30) * 2.4);
  }
  return pico(sala(buf, 0.25, 0.6), -2);
}

/* ============================================================== todo */

const SONIDOS = [
  ['fiesta_musica1', () => musica(1)], ['fiesta_musica2', () => musica(2)],
  ['fiesta_globo1', () => globo(1)], ['fiesta_globo2', () => globo(2)], ['fiesta_globo3', () => globo(3)],
  ['fiesta_juguete1', () => juguete(1)], ['fiesta_juguete2', () => juguete(2)], ['fiesta_juguete3', () => juguete(3)],
  ['fiesta_moneda', moneda], ['fiesta_tickets', tickets], ['laberinto_puerta', puerta], ['fiesta_torno_no', tornoNo],
  ['laberinto_localizador', localizador], ['laberinto_salida', salidaZumbido], ['laberinto_noclip', noclip], ['laberinto_tobogan', tobogan],
  ['fiesta_objetivo', objetivo], ['fiesta_regalo', regalo],
  ['semaforo_musica', semaforoMusica], ['semaforo_silbato', silbato], ['semaforo_fanfarria', fanfarria],
  ['semaforo_luz_verde', () => senal(true)], ['semaforo_luz_roja', () => senal(false)],
  ['anfitrion_paso1', () => paso(1)], ['anfitrion_paso2', () => paso(2)], ['anfitrion_paso3', () => paso(3)], ['anfitrion_paso4', () => paso(4)],
  ['anfitrion_detecta', detecta],
  ['anfitrion_respira1', () => respira(1)], ['anfitrion_respira2', () => respira(2)], ['anfitrion_respira3', () => respira(3)],
  ['anfitrion_pierde', pierde], ['anfitrion_abrazo', abrazo],
  ['suelo_zumbido', sueloZumbido], ['suelo_cruje', sueloCruje], ['suelo_cae', sueloCae],
  ['parkour_bolas', bolas], ['parkour_fallo', parkourFallo], ['parkour_sube', parkourSube],
  ['memoria_nota_roja', () => memoriaNota(72)], ['memoria_nota_amarilla', () => memoriaNota(76)],
  ['memoria_nota_azul', () => memoriaNota(79)], ['memoria_nota_verde', () => memoriaNota(84)],
  ['memoria_turno', memoriaTurno], ['memoria_error', memoriaError],
  ['regalos_ping', regalosPing], ['regalos_coger', regalosCoger], ['regalos_reloj', regalosReloj], ['regalos_fin', regalosFin]
];

/** Entradas de sounds.json: evento -> [archivos, distancia de atenuacion]. Las de voz van en voces.js. */
const EVENTOS = {
  'fiesta.ambiente': [['fiesta_ambiente'], 16],
  'fiesta.musica': [['fiesta_musica1', 'fiesta_musica2'], 40],
  'fiesta.globo': [['fiesta_globo1', 'fiesta_globo2', 'fiesta_globo3'], 20],
  'fiesta.juguete': [['fiesta_juguete1', 'fiesta_juguete2', 'fiesta_juguete3'], 28],
  'fiesta.moneda': [['fiesta_moneda'], 12],
  'fiesta.tickets': [['fiesta_tickets'], 12],
  'fiesta.torno': [['laberinto_puerta'], 16],
  'fiesta.localizador': [['laberinto_localizador'], 24],
  'fiesta.salida': [['laberinto_salida'], 8],
  'fiesta.noclip': [['laberinto_noclip'], 16],
  'fiesta.tobogan': [['laberinto_tobogan'], 20],
  'fiesta.torno_no': [['fiesta_torno_no'], 16],
  'fiesta.objetivo': [['fiesta_objetivo'], 16],
  'fiesta.regalo': [['fiesta_regalo'], 24],
  'semaforo.musica': [['semaforo_musica'], 48],
  'anfitrion.paso': [['anfitrion_paso1', 'anfitrion_paso2', 'anfitrion_paso3', 'anfitrion_paso4'], 24],
  'anfitrion.detecta': [['anfitrion_detecta'], 64],
  'anfitrion.respira': [['anfitrion_respira1', 'anfitrion_respira2', 'anfitrion_respira3'], 20],
  'anfitrion.pierde': [['anfitrion_pierde'], 40],
  'anfitrion.abrazo': [['anfitrion_abrazo'], 32],
  'suelo.zumbido': [['suelo_zumbido'], 6],
  'suelo.cruje': [['suelo_cruje'], 12],
  'suelo.cae': [['suelo_cae'], 20],
  'parkour.bolas': [['parkour_bolas'], 16],
  'parkour.fallo': [['parkour_fallo'], 16],
  'parkour.sube': [['parkour_sube'], 16],
  'memoria.nota_roja': [['memoria_nota_roja'], 32],
  'memoria.nota_amarilla': [['memoria_nota_amarilla'], 32],
  'memoria.nota_azul': [['memoria_nota_azul'], 32],
  'memoria.nota_verde': [['memoria_nota_verde'], 32],
  'memoria.turno': [['memoria_turno'], 24],
  'memoria.error': [['memoria_error'], 24],
  'regalos.ping': [['regalos_ping'], 24],
  'regalos.coger': [['regalos_coger'], 12],
  'regalos.reloj': [['regalos_reloj'], 8],
  'regalos.fin': [['regalos_fin'], 12]
};

if (require.main === module) (async () => {
  const prefijo = process.argv[2] || '';
  console.log('Sonidos de la Fiesta y el Anfitrion');
  if ('fiesta_ambiente'.startsWith(prefijo)) await guardarEst('fiesta_ambiente', ambiente());
  for (const [nombre, fn] of SONIDOS) {
    if (!nombre.startsWith(prefijo)) continue;
    await guardarMono(nombre, fn());
  }
  const f = path.join(SALIDA, '..', 'sounds.json');
  const texto = fs.readFileSync(f, 'utf8');
  const crlf = texto.includes('\r\n');
  const j = JSON.parse(texto);
  for (const [evento, [archivos, distancia]] of Object.entries(EVENTOS)) {
    j[evento] = { sounds: archivos.map((a) => ({ name: `backrooms_evento:${a}`, attenuation_distance: distancia })) };
  }
  let out = JSON.stringify(j, null, 2) + '\n';
  if (crlf) out = out.replace(/\n/g, '\r\n');
  fs.writeFileSync(f, out);
  console.log('  sounds.json');
})();

module.exports = { senal, silbato, fanfarria, altavozViejo, sala };
