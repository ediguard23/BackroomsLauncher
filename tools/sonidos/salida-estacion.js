#!/usr/bin/env node
'use strict';
/**
 * El sonido del final del evento (la Estacion: SalidaEstacionCliente y CorteCadena), con grabaciones de
 * verdad (reales.js: CC0 de BigSoundBank y la musica de Kevin MacLeod, CC BY 4.0) y las voces de Higgsfield:
 *
 *   estacion_corte.ogg (mono, en el mundo): la cizalla, el candado que salta y cae y la cadena al suelo
 *   cinematica_salida_estacion.ogg (43 s):
 *     0 - 26      la Estacion de fondo (transformadores), el plafon de la cabina; 0.8 un «ding» suave
 *     5.0 - 11.2  el otro superviviente corre por la rejilla, lejos; 5.6 «¡Espera! ¡No cierres!»
 *     9.6         el Amasijo trepa (respira, golpes en la barandilla); 11.0 chilla; 11.2 el golpe
 *     11.25       los gritos del superviviente; 11.6 - 20.2 el Amasijo come (mastica, cruje)
 *     18.5 - 19.9 los pitidos de la botonera y el zumbido de confirmacion (fuerte)
 *     20.2        deja de comer: silencio; 20.8 grune; 22.0 chilla; 22.3 - 26 corre hacia el ascensor
 *     20.5 - 26   el jugador jadea; 24.9 las puertas; 26.0 el golpe al cerrarse y NADA mas (corte)
 *     28.0        la musica del ascensor por un altavoz pequeno; el motor de fondo
 *     41.5        la campanilla de llegada; 42.2 se corta con la imagen
 *   cinematica_salida_estacion_despues.ogg (7 s), en el vestibulo: la musica sigue y se va, las puertas se
 *     abren (1.0) y entra el murmullo del vestibulo.
 *
 *   node tools/sonidos/salida-estacion.js
 */

const path = require('path');
const D = require('./dsp');
const S = require('./reales');

const { SR } = S;
const DUR = 43.0;
const DUR_FIN = 7.0;
const MUSICA_DESDE = 28.0;
const CORTE = 42.2;

const USADOS = ['0008', '0191', '0619', '0359', '1773', '1771', '1777', '0086', '0064', '2118', '1318', '0105', '2452', '0497', '0407',
  '0720', '0185', '0758', '3061', '0336', '1103', '0218', '1631', '1712', '3542', 'incompetech_local_forecast_elevator'];

/* ------------------------------------------------------------------ el corte */

function corte () {
  const p = S.pista(2.4);
  // el chasquido de la cizalla: tijeras graves y el golpe seco de una cerradura
  p.poner(0.0, S.real('0008', 2.5, 2.9, { tono: 0.55 }), 0.9, 0, 0.08 / 0.55);
  p.poner(0.0, S.real('0191', 1.3, 1.9, { tono: 0.8 }), 1.0, 0, 0.11 / 0.8);
  // el candado cae y rebota en la chapa
  p.poner(0.45, S.real('1773', 0, 0.8, { tono: 1.4 }), 0.55, 0, 0.04);
  p.poner(0.62, S.real('1773', 0, 0.6, { tono: 1.6 }), 0.3, 0, 0.04);
  // la cadena que se descuelga y cae entera
  p.poner(0.35, S.real('0619', 1.8, 2.6, { entra: 0.02 }), 0.7, 0);
  p.poner(0.75, S.real('0619', 9.2, 10.2, { entra: 0.01, sale: 0.3 }), 0.7, 0);
  // los tramos llegan al suelo (la chapa de la plataforma): cadena pesada y el golpe sordo
  p.poner(1.25, S.real('0359', 3.3, 4.2, { entra: 0.005 }), 1.0, 0, 0.13);
  p.poner(1.25, S.filtro(S.real('1777', 0, 0.8, { tono: 0.7 }), 'lp', 2500), 0.6, 0, 0.1);
  p.poner(1.32, S.real('0359', 1.1, 2.0, { entra: 0.005 }), 0.7, 0, 0.11);
  const mono = new Float32Array(p.n);
  for (let i = 0; i < p.n; i++) mono[i] = (p.L[i] + p.R[i]) * 0.7;
  return D.fundidos(D.masterizar([mono, mono.slice()], { rmsDb: -16, picoDb: -1 }), 0.002, 0.2)[0];
}

/* ------------------------------------------------------------- la Estacion */

function cinematica () {
  const rnd = D.azar(7341);
  const fuera = S.pista(DUR);   // la pasarela: sala grande
  const cabina = S.pista(DUR);  // dentro del ascensor
  const jugador = S.pista(DUR); // pegado al oido
  const musica = S.pista(DUR);  // el altavoz del ascensor

  // de fondo: los transformadores de la Estacion y el plafon de la cabina
  const hum = S.filtro(S.real('0086', 0, 2.5, { entra: 0.2, sale: 0.2 }), 'lp', 600);
  for (let t = 0; t < 26.0; t += 2.3) fuera.poner(t, hum, 0.12);
  const plafon = S.filtro(S.real('2118', 0, 9.9, { entra: 0.5, sale: 0.5 }), 'lp', 900);
  for (let t = 0; t < 42; t += 9.3) cabina.poner(t, plafon, 0.04);
  cabina.poner(0.8, S.real('1631', 0, 2.2), 0.18);

  // el otro superviviente: pasos por la rejilla (de lejos, cada vez mas cerca) y su grito
  const zc = -4;
  const zIni = 58.5;
  const zAt = 24;
  const zSup = (t) => Math.max(zAt, zIni - (t - 5.0) * (34 / (11.2 - 5.0)));
  const pasos = [1.95, 4.4, 9.87, 13.98, 14.56, 10.18];
  for (let t = 5.1, k = 0; t < 11.2; t += 0.345, k++) {
    const d = zSup(t) - zc;
    const g = Math.min(0.8, 5.0 / d);
    fuera.poner(t, S.real('1318', pasos[k % 6] - 0.05, pasos[k % 6] + 0.3, { tono: 0.95 }), g, k % 2 ? 0.08 : -0.08);
    fuera.poner(t + 0.01, S.filtro(S.real('1771', 0, 0.4, { tono: 1.3 }), 'hp', 900), g * 0.25, 0);
  }
  const espera = S.voz('superviviente_espera.wav', 'amasijo_respira1.ogg');
  fuera.poner(5.6, S.filtro(espera.s, 'lp', 3500), espera.propia ? 0.32 : 0.1, 0.05);

  // el Amasijo trepa por el lado: respira, golpes en la barandilla, chilla y se echa encima
  fuera.poner(9.6, S.delJuego('amasijo_respira1', 0.9), 0.35, 0.3);
  fuera.poner(10.2, S.real('1777', 0, 1.2, { tono: 0.7 }), 0.35, 0.3, 0.1);
  fuera.poner(10.6, S.real('1773', 0, 0.8, { tono: 0.8 }), 0.3, 0.3, 0.07);
  fuera.poner(11.0, S.delJuego('amasijo_grito'), 0.75, 0.2);
  fuera.poner(11.2, S.real('2452', 0, 1.6), 0.8, 0.1, 0.18);
  fuera.poner(11.2, S.real('0105', 0.6, 1.6, { tono: 0.75 }), 0.6, 0.1, 0.25);
  fuera.poner(11.22, S.delJuego('amasijo_ataque'), 0.6, 0.1);
  const grito = S.voz('superviviente_grito.wav', 'amasijo_respira2.ogg');
  fuera.poner(11.25, grito.s.subarray(0, Math.round(8.5 * SR)), grito.propia ? 0.55 : 0.15, 0.05);
  // comiendo: masca, cruje y chupa (grabaciones de animales comiendo, muy graves), hasta OYE
  const come = S.pista(DUR);
  for (let t = 11.7, k = 0; t < 20.15; k++) {
    const c = [
      () => S.real('0497', 0.5, 1.6, { tono: 0.62 }),
      () => S.real('0407', 5.2, 5.9, { tono: 0.55 }),
      () => S.real('0720', 8.4, 9.4, { tono: 0.7 }),
      () => S.real('0185', 7.4, 8.0, { tono: 0.5 }),
      () => S.real('0497', 5.9, 6.8, { tono: 0.6 })
    ][k % 5]();
    const s = c.subarray(0, Math.min(c.length, Math.round((20.2 - t) * SR)));
    come.poner(t, s, 0.5 + rnd() * 0.25, rnd() * 0.3 - 0.05);
    t += 0.35 + rnd() * 0.5;
  }
  for (let i = 0; i < come.n; i++) {
    fuera.L[i] += come.L[i];
    fuera.R[i] += come.R[i];
  }

  // la botonera: cuatro pitidos y el zumbido de confirmacion, fuerte
  [18.5, 18.85, 19.2, 19.55].forEach((t, k) => cabina.poner(t, S.real('0758', [0.06, 0.43, 0.81, 1.19][k], [0.3, 0.68, 1.06, 1.45][k]), 0.75, 0.35, 0.02));
  cabina.poner(19.9, S.real('3061', 0, 1.2), 0.8, 0.35, 0.07);
  const zumbido = new Float32Array(Math.round(0.55 * SR));
  for (let i = 0; i < zumbido.length; i++) {
    const t = i / SR;
    zumbido[i] = (((t * 110) % 1) < 0.5 ? 1 : -1) * 0.5 * Math.min(1, t / 0.01) * Math.min(1, (0.55 - t) / 0.05);
  }
  cabina.poner(19.95, S.filtro(zumbido, 'lp', 2200), 0.35, 0.35);

  // le ha oido: un gruñido, el chillido y viene corriendo (pasos pesados por la rejilla)
  fuera.poner(20.8, S.delJuego('amasijo_respira2', 0.75), 0.6, 0);
  fuera.poner(22.0, S.delJuego('amasijo_grito', 0.92), 1.0, 0);
  for (let t = 22.4, k = 0; t < 26.0; t += 0.21, k++) {
    const d = Math.max(2.0, 29 - (t - 22.3) * 7.2);
    fuera.poner(t, S.delJuego(`amasijo_pasos${1 + (k % 4)}`), Math.min(1.1, 6.0 / d), k % 2 ? 0.1 : -0.1);
    fuera.poner(t, S.filtro(S.real('1777', 0, 0.6, { tono: 0.6 + rnd() * 0.1 }), 'lp', 1800), Math.min(0.6, 3.0 / d), 0);
  }
  // el jugador: jadea y el corazon a mil desde que le oye hasta que se cierran
  jugador.poner(20.4, S.envolvente(S.filtro(S.real('1103', 0, 6.0, { entra: 0.2 }), 'lp', 5000), [[0, 0.1], [1.5, 0.5], [5.6, 0.6], [6.0, 0]]), 1.0);
  jugador.poner(20.6, S.filtro(S.real('0218', 0, 5.4, { tono: 1.4, entra: 0.1 }), 'lp', 190), 0.7);

  // las puertas: el motor de cierre y el golpe al encajar (26.0)
  cabina.poner(26.0, S.real('0336', 2.6, 4.9, { sale: 0.2 }), 0.7, 0, 4.14 - 2.6);
  const cierre = S.pista(DUR);
  cierre.poner(26.0, S.filtro(S.real('1777', 0, 0.5, { tono: 0.55, sale: 0.15 }), 'lp', 1500), 1.0, 0, 0.07 / 0.55);
  cierre.poner(26.0, S.real('0105', 0.7, 1.1, { tono: 0.7, sale: 0.1 }), 0.6, 0, 0.13);

  // la musica del ascensor, por un altavoz pequeno, y el motor bajando de fondo
  const m = S.real('incompetech_local_forecast_elevator', 0, CORTE - MUSICA_DESDE + 0.5, { entra: 1.0, sale: 0.05 });
  musica.poner(MUSICA_DESDE, S.filtro(S.filtro(m, 'hp', 220, 0.7), 'lp', 5200, 0.7), 0.42);
  cabina.poner(28.5, S.envolvente(S.filtro(S.real('1712', 6.0, 19.0, { entra: 1.5, sale: 1.5 }), 'lp', 400), [[0, 1], [13, 1]]), 0.12);
  // la campanilla de llegada
  cabina.poner(CAMPANA(), S.real('1631', 0, 2.2), 0.7, 0);

  // las salas
  const pf = S.sala(fuera, { lp: 3800, sala: 0.88, amortiguacion: 0.4, predelayMs: 40, seco: 0.55, humedo: 0.6 });
  const pc = S.sala(cabina, { sala: 0.25, amortiguacion: 0.7, predelayMs: 3, seco: 1, humedo: 0.15 });
  const pm = S.sala(musica, { sala: 0.3, amortiguacion: 0.6, predelayMs: 5, seco: 1, humedo: 0.25 });
  const [L, R] = S.sumar(fuera.n, pf, pc, pm, jugador, cierre);
  // al cerrarse las puertas se corta todo lo de fuera de golpe (y el jadeo): solo el golpe del cierre
  const c0 = Math.round(26.04 * SR);
  for (let i = c0; i < Math.round(MUSICA_DESDE * SR); i++) {
    const keep = (cierre.L[i] + cierre.R[i] !== 0) || i < c0 + 0.25 * SR;
    L[i] = keep ? cierre.L[i] + pc.L[i] * 0.3 : pc.L[i] * 0.15;
    R[i] = keep ? cierre.R[i] + pc.R[i] * 0.3 : pc.R[i] * 0.15;
  }
  // y la imagen se corta tras la campanilla: el sonido tambien
  for (let i = Math.round(CORTE * SR); i < L.length; i++) {
    L[i] = 0;
    R[i] = 0;
  }
  return D.fundidos(D.masterizar([L, R], { rmsDb: -21, picoDb: -0.8 }), 0.004, 0.02);
}

function CAMPANA () {
  return 41.5;
}

/* ---------------------------------------------------------------- la llegada */

function despues () {
  const p = S.pista(DUR_FIN);
  // la musica sigue donde se quedo (en el corte) y se va al abrirse las puertas
  const desde = CORTE - MUSICA_DESDE;
  const m = S.real('incompetech_local_forecast_elevator', desde, desde + DUR_FIN, { entra: 0.3, sale: 0.5 });
  p.poner(0, S.envolvente(S.filtro(S.filtro(m, 'hp', 220), 'lp', 5200), [[0, 1], [1.4, 1], [4.0, 0]]), 0.42);
  // las puertas se abren
  p.poner(0.85, S.real('0336', 0.25, 2.6, { sale: 0.3 }), 0.55, 0);
  // el vestibulo: murmullo de gente, que entra al abrirse
  const gente = S.real('3542', 40, 40 + DUR_FIN, { entra: 0.1, sale: 0.6 });
  p.poner(0, S.envolvente(S.filtro(gente, 'lp', 4000), [[0, 0], [1.0, 0.02], [2.8, 0.38], [7, 0.38]]), 1.0);
  const sala = new D.Freeverb({ sala: 0.6, amortiguacion: 0.5, predelayMs: 20 });
  for (let i = 0; i < p.n; i++) {
    const [a, b] = sala.paso((p.L[i] + p.R[i]) * 0.5);
    p.L[i] += a * 0.25;
    p.R[i] += b * 0.25;
  }
  return D.fundidos(D.masterizar([p.L, p.R], { rmsDb: -24, picoDb: -2 }), 0.02, 1.0);
}

if (require.main === module) (async () => {
  console.log('Final de la Estacion');
  await S.descargar(USADOS);
  let b = await S.guardarMono(path.join(S.SONIDOS, 'estacion_corte.ogg'), corte());
  console.log(`  estacion_corte.ogg ${(b / 1024).toFixed(0)} KB`);
  for (const [nombre, f, dur] of [['cinematica_salida_estacion', cinematica, DUR], ['cinematica_salida_estacion_despues', despues, DUR_FIN]]) {
    b = await D.guardarOgg(path.join(S.SONIDOS, `${nombre}.ogg`), f(), 4);
    console.log(`  ${nombre}.ogg ${dur} s, ${(b / 1024).toFixed(0)} KB`);
  }
  S.soundsJson({
    'estacion.corte': { sounds: [{ name: 'backrooms_evento:estacion_corte' }] },
    'cinematica.salida_estacion': { sounds: [{ name: 'backrooms_evento:cinematica_salida_estacion' }] },
    'cinematica.salida_estacion.despues': { sounds: [{ name: 'backrooms_evento:cinematica_salida_estacion_despues' }] }
  });
  console.log('  sounds.json');
})();
