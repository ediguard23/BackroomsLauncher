#!/usr/bin/env node
'use strict';
/**
 * El sonido del prologo del /start: los primeros 44.5 s de la cinematica (la sala de
 * reuniones, la firma y el pasillo hasta el ascensor), que cinematica.js pega delante de
 * la bajada en un solo archivo. Los tiempos y los recorridos son los de
 * evento/.../shaders/core/cinematica.fsh (P_*, I_*, RUTA_I, RUTA_J):
 *
 *   0.0   la videocamara se enciende (pitidos y motor de cinta)
 *   0.9   la sala: el aire acondicionado, un zumbido de fluorescente y el reloj de pared
 *   2.4   te acerca el papel de un empujon por la mesa; 4.1 el boligrafo, rodando
 *   6.0   te inclinas sobre la hoja (roce del papel); lees 20 s (alguna pagina que se ajusta)
 *  25.4   coges el boligrafo; 26.9 la firma: la punta rascando el papel, a tirones
 *  30.9   lo dejas en la mesa; 32.6 se lleva el papel; 34.0 la silla al levantarte
 *  33.4   sus pasos (delante) y los tuyos: moqueta en la sala, suelo duro en el pasillo.
 *        Cada pisada es distinta (fuerza, talon y punta, tono, un roce a veces) y blanda,
 *        sin el chasquido seco de antes
 *  39.5   el boton de llamada, la campanilla y las puertas que se abren
 *  hasta 48.5 el zumbido del pasillo, que se apaga al cerrarse las puertas (PRO + 2.0)
 *
 * LEE: lo que se alargo la lectura (de 12 a 20 s); todo lo de despues va LEE s mas tarde.
 *
 * Sin voces: lo que se dice va en subtitulos. Todo es sintesis propia.
 */

const D = require('./dsp');
const E = require('./expedicion');

const { SR } = D;
const TAU = Math.PI * 2;
const { mono, poner, reverb } = E;

const PRO = 44.5;
const LEE = 8.0;
const DUR = PRO + 4.0;
const CIERRA0 = PRO + 2.0;
const CIERRA1 = PRO + 3.4;

const suave = (a, b, x) => { const t = Math.min(1, Math.max(0, (x - a) / (b - a))); return t * t * (3 - 2 * t); };

// los recorridos del shader, para poner los pasos donde caen
const RUTA_I = [[0.0, -18.05], [1.75, -17.85], [1.75, -15.2], [0.6, -14.6], [0.0, -14.1], [0.0, -12.6], [0.5, -4.0], [1.25, -2.35]];
const RUTA_J = [[-0.55, -15.0], [-0.35, -14.3], [0.0, -13.7], [0.0, -13.0], [0.0, -2.5], [0.0, 0.9]];

function avance (u, a) {
  u = Math.min(1, Math.max(0, u));
  const v = 1 / (1 - a);
  if (u < a) return 0.5 * v * u * u / a;
  if (u > 1 - a) return 1 - 0.5 * v * (1 - u) * (1 - u) / a;
  return v * (u - 0.5 * a);
}

function largo (r) {
  let l = 0;
  for (let i = 1; i < r.length; i++) l += Math.hypot(r[i][0] - r[i - 1][0], r[i][1] - r[i - 1][1]);
  return l;
}

function enRuta (r, s) {
  for (let i = 1; i < r.length; i++) {
    const l = Math.hypot(r[i][0] - r[i - 1][0], r[i][1] - r[i - 1][1]);
    if (s <= l || i === r.length - 1) {
      const k = Math.min(1, Math.max(0, s / l));
      return [r[i - 1][0] + (r[i][0] - r[i - 1][0]) * k, r[i - 1][1] + (r[i][1] - r[i - 1][1]) * k];
    }
    s -= l;
  }
  return r[r.length - 1];
}

/** Los momentos (y donde) en que se apoya un pie al recorrer `ruta` entre t0 y t1. */
function pasos (ruta, t0, t1, a, zancada) {
  const L = largo(ruta);
  const out = [];
  let siguiente = zancada * 0.5;
  for (let t = t0; t <= t1; t += 0.005) {
    const s = L * avance((t - t0) / (t1 - t0), a);
    if (s >= siguiente) {
      out.push({ t, p: enRuta(ruta, s) });
      siguiente += zancada;
    }
  }
  return out;
}

function prologo () {
  const rnd = D.azar(5100);
  const buf = mono(DUR);

  // --- la videocamara se enciende
  poner(buf, 0.1, 0.06, (t) => Math.sin(TAU * 2800 * t) * 0.08);
  poner(buf, 0.26, 0.06, (t) => Math.sin(TAU * 2800 * t) * 0.08);
  {
    const bp = new D.Biquad('bp', 900, 1.5);
    poner(buf, 0.0, 1.0, (t) => (Math.sin(TAU * (700 + 500 * t) * t) * 0.15 + bp.paso(rnd() * 2 - 1) * 0.6) * Math.sin(Math.PI * t) * 0.12);
  }
  poner(buf, 0.86, 0.03, (t) => (rnd() * 2 - 1) * Math.exp(-t * 200) * 0.2);

  // --- ambiente: el aire de la sala, luego el del pasillo; se apaga al cerrarse las puertas
  {
    const lp = new D.Biquad('lp', 380, 0.7);
    const lp2 = new D.Biquad('lp', 900, 0.7);
    poner(buf, 0.9, DUR - 0.9, (t) => {
      const ta = t + 0.9;
      const sala = 1 - suave(LEE + 27.0, LEE + 29.5, ta);
      const fuera = 1 - suave(CIERRA0, CIERRA1 + 0.2, ta);
      const entra = Math.min(1, t / 1.2);
      const aire = lp.paso(rnd() * 2 - 1) * 0.85 * sala + lp2.paso(rnd() * 2 - 1) * 0.32 * (1 - sala);
      const zumbido = Math.sin(TAU * 120 * ta) * 0.02 + Math.sin(TAU * 240 * ta) * 0.01 * (1 - sala);
      return (aire + zumbido) * entra * fuera;
    });
  }
  // el reloj de pared: tic cada segundo mientras estas en la sala
  for (let s = 1; s < LEE + 29; s++) {
    const bp = new D.Biquad('bp', s % 2 ? 2600 : 2300, 6);
    const g = 0.42 * (1 - suave(LEE + 26.5, LEE + 29.0, s));
    poner(buf, s, 0.04, (t) => bp.paso(rnd() * 2 - 1) * Math.exp(-t * 180) * g);
  }

  // --- el papel y el boligrafo
  const papel = (t0, dur, g, f = 3200) => {
    const bp = new D.Biquad('bp', f, 0.8);
    poner(buf, t0, dur, (t) => bp.paso(rnd() * 2 - 1) * Math.pow(Math.sin(Math.PI * t / dur), 0.7) * g * (0.7 + 0.3 * Math.sin(TAU * 7 * t)));
  };
  papel(2.55, 0.5, 0.95);          // el empujon
  papel(2.95, 1.0, 0.45, 2600);    // y sigue resbalando hasta pararse
  // el boligrafo rueda por la mesa: el hexagono va dando golpecitos cada vez mas espaciados
  for (let k = 0, t0 = 4.12; t0 < 4.8; k++, t0 += 0.045 + k * 0.012) {
    const bp = new D.Biquad('bp', 3400 + rnd() * 900, 5);
    poner(buf, t0, 0.03, (t) => bp.paso(rnd() * 2 - 1) * Math.exp(-t * 160) * (0.6 - k * 0.03));
  }
  papel(6.0, 0.55, 0.35);          // te inclinas y la ajustas
  papel(13.2, 0.4, 0.22, 2900);    // la sujetas mejor mientras lees
  papel(19.6, 0.35, 0.2, 3400);
  // lo coges: se levanta de la mesa
  poner(buf, LEE + 17.62, 0.05, (t) => (Math.sin(TAU * 2100 * t) * 0.5 + (rnd() * 2 - 1) * 0.3) * Math.exp(-t * 90) * 0.35);
  // la punta toca el papel
  poner(buf, LEE + 18.88, 0.03, (t) => (rnd() * 2 - 1) * Math.exp(-t * 220) * 0.4);
  // la firma: la punta rascando, a tirones (levanta entre letras)
  for (let t0 = LEE + 18.92; t0 < LEE + 22.25; t0 += 0.07 + rnd() * 0.11) {
    const bp = new D.Biquad('bp', 4200 + rnd() * 2200, 2.2);
    const d = 0.05 + rnd() * 0.12;
    poner(buf, t0, d, (t) => bp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / d) * 0.5);
  }
  // la rubrica: un trazo largo
  {
    const bp = new D.Biquad('bp', 3800, 1.6);
    poner(buf, LEE + 21.7, 0.55, (t) => bp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * t / 0.55) * 0.55);
  }
  // lo dejas en la mesa
  poner(buf, LEE + 23.32, 0.06, (t) => (Math.sin(TAU * 1800 * t) + (rnd() * 2 - 1) * 0.4) * Math.exp(-t * 80) * 0.38);
  // se lleva el papel
  papel(LEE + 24.75, 0.75, 0.8);
  papel(LEE + 25.45, 0.25, 0.6, 2200);
  // la silla al levantarte
  {
    const lp = new D.Biquad('lp', 900, 1.2);
    poner(buf, LEE + 26.05, 0.65, (t) => (lp.paso(rnd() * 2 - 1) + Math.sin(TAU * (190 + 40 * Math.sin(TAU * 9 * t)) * t) * 0.3) * Math.sin(Math.PI * t / 0.65) * 0.55);
  }

  // --- los pasos: los suyos delante, los tuyos mas fuertes. Cada pisada es un talon y,
  // un poco despues, la punta: ruido filtrado (sin chasquido ni tono puro), con ataque blando
  // y cada una con su fuerza, su tono y su momento (nadie anda como un metronomo)
  const pisada = (t0, g, fc, dec, grave) => {
    const lp = new D.Biquad('lp', fc, 0.6);
    const lp2 = new D.Biquad('lp', fc * 1.3, 0.6);
    const hp = new D.Biquad('hp', 60, 0.7);
    const fl = grave * (0.88 + rnd() * 0.24);
    poner(buf, t0, 0.3, (t) => {
      const env = (1 - Math.exp(-t / 0.006)) * Math.exp(-t / dec);
      const cuerpo = Math.sin(TAU * fl * t) * Math.exp(-t / 0.05) * 0.5;
      return hp.paso(lp2.paso(lp.paso(rnd() * 2 - 1)) * 2.2 + cuerpo) * env * g;
    });
  };
  const paso = (t0, p, g, cerca) => {
    const moqueta = p[1] < -13.5;
    const cabina = p[1] > -1.5;
    const fuerza = g * (cerca ? 1.0 : 0.55) * (0.78 + rnd() * 0.4);
    const t = t0 + (rnd() - 0.5) * 0.04;
    const fc = (moqueta ? 420 : cabina ? 1300 : 1050) * (0.85 + rnd() * 0.3);
    const dec = moqueta ? 0.045 : 0.06;
    const grave = moqueta ? 65 : cabina ? 95 : 80;
    pisada(t, fuerza, fc, dec, grave);
    pisada(t + 0.06 + rnd() * 0.05, fuerza * (0.3 + rnd() * 0.2), fc * 1.35, dec * 0.7, grave * 1.3);
    if (!moqueta && rnd() < 0.3) {
      // a veces la suela roza el suelo al apoyarse
      const bp = new D.Biquad('bp', 1800 + rnd() * 900, 1.0);
      const d = 0.08 + rnd() * 0.08;
      poner(buf, t + 0.02, d, (u) => bp.paso(rnd() * 2 - 1) * Math.sin(Math.PI * u / d) * fuerza * 0.12);
    }
  };
  for (const s of pasos(RUTA_I, LEE + 25.4, LEE + 31.4, 0.15, 1.2)) paso(s.t, s.p, 0.55, false);
  for (const s of pasos([RUTA_I[RUTA_I.length - 1], [0.35, -3.8]], LEE + 34.0, LEE + 35.4, 0.3, 0.9)) paso(s.t, s.p, 0.45, false);
  for (const s of pasos(RUTA_J, LEE + 28.0, LEE + 35.2, 0.12, 1.2)) paso(s.t, s.p, 0.75, true);

  // --- el ascensor: el boton, la campanilla y las puertas que se abren
  poner(buf, LEE + 31.5, 0.04, (t) => (rnd() * 2 - 1) * Math.exp(-t * 250) * 0.35);
  const campana = (t0, f, g) => poner(buf, t0, 2.4, (t) => (Math.sin(TAU * f * t) + Math.sin(TAU * f * 2.0 * t) * 0.45 * Math.exp(-t * 3)
    + Math.sin(TAU * f * 2.76 * t) * 0.3 * Math.exp(-t * 5)) * Math.exp(-t / 0.7) * g);
  campana(LEE + 31.9, 1318.5, 0.2);
  campana(LEE + 32.3, 1046.5, 0.2);
  {
    const lp = new D.Biquad('lp', 260, 0.8);
    const br = D.marron(D.azar(31));
    const bp = new D.Biquad('bp', 2200, 6);
    poner(buf, LEE + 32.0, 1.2, (t) => (lp.paso(br()) * 2.0 + bp.paso(rnd() < 30 / SR ? (rnd() * 2 - 1) * 2 : 0)) * Math.sin(Math.PI * t / 1.2) * 0.4);
    poner(buf, LEE + 33.2, 0.3, (t) => Math.sin(TAU * 95 * t) * Math.exp(-t / 0.06) * 0.35);
  }
  return reverb(buf, 0.5, 0.16);
}

module.exports = { prologo, PRO, DUR };
