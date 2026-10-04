'use strict';
/* Backrooms Launcher — interfaz. */

(function () {
  const L = window.launcher;
  const $ = (id) => document.getElementById(id);
  const NICK = /^[A-Za-z0-9_]{3,16}$/;
  const MESES = ['ENE', 'FEB', 'MAR', 'ABR', 'MAY', 'JUN', 'JUL', 'AGO', 'SEP', 'OCT', 'NOV', 'DIC'];
  const ETAPAS = {
    cuenta: 'COMPROBANDO LA CUENTA',
    evento: 'CARGANDO EL EVENTO',
    juego: 'PREPARANDO MINECRAFT',
    java: 'PREPARANDO JAVA',
    pack: 'PREPARANDO EL PACK',
    arranque: 'ABRIENDO MINECRAFT'
  };

  const estado = {
    config: null,
    cuenta: null,
    manifest: null,
    acceso: true,
    jugando: false,
    preparando: false,
    inicio: Date.now()
  };

  const pasillo = new window.Pasillo($('pasillo'));
  const grano = window.crearGrano($('grano'));
  let sonido = null;
  try { sonido = new window.Sonido(); } catch { sonido = null; }

  /* ---------------------------------------------------------------- avisos */

  let avisoT = null;
  function aviso (texto, error = false, ms = 8000) {
    const el = $('aviso');
    el.textContent = texto ? (error ? `! ${texto}` : texto) : '';
    el.classList.toggle('error', error);
    clearTimeout(avisoT);
    if (texto && ms) avisoT = setTimeout(() => { el.textContent = ''; }, ms);
  }

  /* ------------------------------------------------------------------- HUD */

  const dos = (n) => String(n).padStart(2, '0');
  function relojHud () {
    const s = Math.floor((Date.now() - estado.inicio) / 1000);
    $('timecode').textContent = `${dos(Math.floor(s / 3600))}:${dos(Math.floor(s / 60) % 60)}:${dos(s % 60)}`;
    const d = new Date();
    $('fecha').textContent = `${MESES[d.getMonth()]}. ${dos(d.getDate())} ${d.getFullYear()}   ${dos(d.getHours())}:${dos(d.getMinutes())}`;
  }

  /* ------------------------------------------------------- pase y cuenta */

  function numeroPase (texto) {
    let h = 7;
    for (const c of texto || '') h = (h * 31 + c.charCodeAt(0)) >>> 0;
    return `N.º ${String(h % 10000).padStart(4, '0')}`;
  }

  function caraDesconocida (ctx) {
    // silueta con interrogacion, en pixeles
    const filas = [
      '33333333',
      '32222223',
      '32211223',
      '32222123',
      '32221223',
      '32222223',
      '32221223',
      '33333333'
    ];
    const col = { 1: '#e9dfb8', 2: '#6f6648', 3: '#5a523a' };
    filas.forEach((f, y) => [...f].forEach((c, x) => { ctx.fillStyle = col[c]; ctx.fillRect(x, y, 1, 1); }));
  }

  function pintarCara () {
    const ctx = $('cara').getContext('2d');
    ctx.imageSmoothingEnabled = false;
    ctx.clearRect(0, 0, 8, 8);
    const c = estado.config && estado.config.premium ? estado.cuenta : null;
    if (!c || !c.skin) return caraDesconocida(ctx);
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      ctx.clearRect(0, 0, 8, 8);
      ctx.drawImage(img, 8, 8, 8, 8, 0, 0, 8, 8);
      ctx.drawImage(img, 40, 8, 8, 8, 0, 0, 8, 8);
    };
    img.onerror = () => caraDesconocida(ctx);
    img.src = c.skin.replace(/^http:/, 'https:');
  }

  function pintarPase () {
    const premium = Boolean(estado.config.premium && estado.cuenta);
    $('premium').checked = premium;
    $('nick').disabled = premium;
    $('nick').value = premium ? estado.cuenta.name : (estado.config.nick || '');
    $('cuenta-premium').hidden = !premium;
    $('cuenta-nombre').textContent = premium ? estado.cuenta.name : '';
    $('sello').hidden = !premium;
    $('pase-num').textContent = numeroPase(premium ? estado.cuenta.uuid : $('nick').value);
    pintarCara();
    validarNick();
  }

  function nombreActual () {
    return estado.config.premium && estado.cuenta ? estado.cuenta.name : $('nick').value.trim();
  }

  function validarNick () {
    const premium = estado.config.premium && estado.cuenta;
    const v = $('nick').value.trim();
    let msg = '';
    if (!premium && v && !NICK.test(v)) {
      msg = v.length < 3 ? 'Mínimo 3 caracteres.' : 'Solo letras, números y _ (máx. 16).';
    }
    $('nick-error').textContent = msg;
    actualizarJugar();
  }

  let guardarNickT = null;
  $('nick').addEventListener('input', () => {
    $('nick').value = $('nick').value.replace(/\s/g, '');
    $('pase-num').textContent = numeroPase($('nick').value);
    validarNick();
    clearTimeout(guardarNickT);
    guardarNickT = setTimeout(async () => {
      const nick = $('nick').value.trim();
      estado.config = await L.guardarConfig({ nick });
      comprobarAcceso();
    }, 350);
  });

  $('premium').addEventListener('change', async (e) => {
    sonido && sonido.click();
    if (e.target.checked) {
      if (!estado.cuenta) {
        e.target.disabled = true;
        aviso('Abriendo el inicio de sesión de Microsoft…', false, 0);
        const r = await L.loginMicrosoft();
        e.target.disabled = false;
        if (!r.ok) {
          e.target.checked = false;
          aviso(r.error, true);
          return;
        }
        estado.cuenta = r.datos;
        aviso(`Sesión iniciada: ${r.datos.name}`);
      }
      estado.config = await L.guardarConfig({ premium: true });
    } else {
      estado.config = await L.guardarConfig({ premium: false });
    }
    pintarPase();
    comprobarAcceso();
  });

  $('btn-logout').addEventListener('click', async () => {
    await L.logout();
    estado.cuenta = null;
    estado.config = await L.guardarConfig({ premium: false });
    pintarPase();
    aviso('Sesión premium cerrada');
  });

  /* ----------------------------------------------------------------- JUGAR */

  function puedeJugar () {
    if (estado.jugando || estado.preparando || !estado.manifest) return false;
    if (estado.config.premium) return Boolean(estado.cuenta);
    return NICK.test($('nick').value.trim()) && estado.acceso;
  }

  function actualizarJugar () {
    const btn = $('btn-jugar');
    const txt = $('jugar-txt');
    btn.classList.toggle('ocupado', estado.preparando || estado.jugando);
    if (estado.jugando) txt.textContent = 'EN PARTIDA';
    else if (estado.preparando) txt.textContent = 'PREPARANDO';
    else if (estado.manifest && !estado.acceso) txt.textContent = 'CERRADO';
    else txt.textContent = 'JUGAR';
    btn.disabled = !puedeJugar() || (estado.config.premium && !estado.acceso);
  }

  $('btn-jugar').addEventListener('click', async () => {
    if (!puedeJugar()) return;
    sonido && sonido.jugar();
    estado.preparando = true;
    actualizarJugar();
    mostrarProgreso({ etapa: 'cuenta', texto: ETAPAS.cuenta });
    aviso('');
    const r = await L.jugar();
    if (!r.ok) {
      estado.preparando = false;
      ocultarProgreso();
      actualizarJugar();
      aviso(r.error, true, 12000);
      if (r.relogin) {
        estado.cuenta = null;
        pintarPase();
      }
    }
  });

  /* -------------------------------------------------------------- progreso */

  const MB = (b) => (b / 1048576).toFixed(b > 104857600 ? 0 : 1);
  let etapaActual = '';
  function mostrarProgreso (p) {
    const caja = $('progreso');
    caja.hidden = false;
    if (p.etapa !== etapaActual) {
      etapaActual = p.etapa;
      caja.classList.add('indeterminado');
      $('progreso-pct').textContent = '';
    }
    if (p.texto) $('progreso-etapa').textContent = p.texto.toUpperCase();
    if (p.total) {
      const pct = Math.max(0, Math.min(100, (p.hecho / p.total) * 100));
      caja.classList.remove('indeterminado');
      $('progreso-relleno').style.width = `${pct.toFixed(1)}%`;
      $('progreso-pct').textContent = p.total > 2 * 1048576
        ? `${MB(p.hecho)} / ${MB(p.total)} MB`
        : `${Math.floor(pct)}%`;
    }
  }
  function ocultarProgreso () {
    $('progreso').hidden = true;
    $('progreso-relleno').style.width = '0%';
    etapaActual = '';
  }

  L.onProgreso(mostrarProgreso);

  L.onJuego((j) => {
    if (j.estado === 'abriendo') {
      mostrarProgreso({ etapa: 'arranque', texto: 'Abriendo Minecraft…' });
    } else if (j.estado === 'jugando') {
      estado.preparando = false;
      estado.jugando = true;
      ocultarProgreso();
      aviso('Minecraft abierto. Suerte ahí dentro.');
      sonido && sonido.pausar();
      pasillo.parar();
    } else if (j.estado === 'cerrado') {
      estado.preparando = false;
      estado.jugando = false;
      ocultarProgreso();
      if (j.error) aviso(j.error, true, 15000);
      else aviso('Has vuelto. Por ahora.');
      sonido && sonido.reanudar();
      if (estado.config.efectos) pasillo.iniciar();
    }
    actualizarJugar();
  });

  /* ------------------------------------------------------------ el evento */

  function escapar (s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  function pintarNoticias (lista) {
    const ol = $('noticias');
    if (!lista || !lista.length) {
      ol.innerHTML = '<li class="vacio">Sin entradas todavía.</li>';
      return;
    }
    ol.innerHTML = lista.map((n) => {
      // "2026-10-04" a secas se lee como medianoche UTC: en America saldria el dia anterior
      const fecha = n.date ? new Date(/^\d{4}-\d{2}-\d{2}$/.test(n.date) ? `${n.date}T12:00:00` : n.date) : null;
      const f = fecha && !isNaN(fecha) ? `${dos(fecha.getDate())} ${MESES[fecha.getMonth()]} ${fecha.getFullYear()}` : '';
      return `<li>${f ? `<span class="n-fecha">${f}</span>` : ''}<span class="n-titulo">${escapar(n.title)}</span>` +
        `${n.text ? `<span class="n-texto">${escapar(n.text)}</span>` : ''}</li>`;
    }).join('');
  }

  async function cargarManifest () {
    const r = await L.manifest();
    if (!r.ok) {
      aviso(r.error, true, 0);
      $('ca-titulo').textContent = 'SIN SEÑAL';
      $('ca-valor').textContent = '--:--:--';
      setTimeout(cargarManifest, 20000);
      return;
    }
    estado.manifest = r.datos;
    if (r.datos.desdeCache) aviso('Sin conexión: se usa la última configuración descargada.', true);
    if (r.datos.nombre) {
      $('nombre-evento').textContent = r.datos.nombre.toUpperCase();
      $('nombre-evento').dataset.texto = r.datos.nombre.toUpperCase();
    }
    pintarNoticias(r.datos.news);
    $('btn-discord').disabled = !r.datos.links.discord;
    $('btn-tienda').disabled = !r.datos.links.tienda;
    if (!estado.config.ramMB && r.datos.ram && r.datos.ram.recommended) $('ram').value = r.datos.ram.recommended;
    cuentaAtras();
    comprobarAcceso();
    pingServidor();
  }

  async function comprobarAcceso () {
    const m = estado.manifest;
    if (!m) return;
    const empieza = m.eventStart ? Date.parse(m.eventStart) : 0;
    if (!empieza || Date.now() >= empieza) {
      estado.acceso = true;
    } else {
      const r = await L.acceso(nombreActual());
      estado.acceso = Boolean(r);
    }
    actualizarJugar();
  }

  let directoAvisado = false;
  function cuentaAtras () {
    const m = estado.manifest;
    if (!m) return;
    const caja = $('cuenta-atras');
    const empieza = m.eventStart ? Date.parse(m.eventStart) : 0;
    const falta = empieza - Date.now();
    if (!empieza || falta <= 0) {
      caja.classList.add('directo');
      $('ca-titulo').textContent = 'LA PUERTA ESTÁ ABIERTA';
      $('ca-valor').textContent = 'EN DIRECTO';
      if (empieza && !directoAvisado) {
        directoAvisado = true;
        comprobarAcceso();
      }
      return;
    }
    caja.classList.remove('directo');
    $('ca-titulo').textContent = 'LA PUERTA SE ABRE EN';
    const s = Math.floor(falta / 1000);
    const d = Math.floor(s / 86400);
    $('ca-valor').textContent = `${d ? `${d}D ` : ''}${dos(Math.floor(s / 3600) % 24)}:${dos(Math.floor(s / 60) % 60)}:${dos(s % 60)}`;
  }

  async function pingServidor () {
    if (!estado.manifest) return;
    const s = await L.ping();
    const barras = $('barras');
    const is = barras.querySelectorAll('i');
    if (!s || !s.online) {
      barras.classList.add('caido');
      is.forEach((i) => i.classList.remove('on'));
      $('srv-estado').textContent = 'SERVIDOR SIN SEÑAL';
      $('srv-jugadores').textContent = 'ERRANTES --';
      return;
    }
    barras.classList.remove('caido');
    const nivel = s.latencia < 60 ? 4 : s.latencia < 120 ? 3 : s.latencia < 220 ? 2 : 1;
    is.forEach((i, k) => i.classList.toggle('on', k < nivel));
    $('srv-estado').textContent = `SERVIDOR EN LÍNEA · ${s.latencia} MS`;
    $('srv-jugadores').textContent = `ERRANTES ${s.jugadores.online}/${s.jugadores.max}`;
  }

  $('btn-discord').addEventListener('click', () => { sonido && sonido.click(); L.abrirEnlace('discord'); });
  $('btn-tienda').addEventListener('click', () => { sonido && sonido.click(); L.abrirEnlace('tienda'); });

  /* ------------------------------------------------------------ menu OSD */

  function textoRam (v) { return `${(v / 1024).toFixed(1)} GB`; }

  function abrirOsd () {
    sonido && sonido.click();
    $('osd').hidden = false;
  }
  function cerrarOsd () {
    sonido && sonido.click();
    $('osd').hidden = true;
  }
  $('btn-menu').addEventListener('click', abrirOsd);
  $('osd-salir').addEventListener('click', cerrarOsd);
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && !$('osd').hidden) cerrarOsd(); });

  $('ram').addEventListener('input', () => { $('ram-txt').textContent = textoRam(+$('ram').value); });
  $('ram').addEventListener('change', async () => { estado.config = await L.guardarConfig({ ramMB: +$('ram').value }); });

  $('volumen').addEventListener('input', () => {
    const v = +$('volumen').value;
    $('volumen-txt').textContent = `${Math.round(v * 100)}%`;
    sonido && sonido.ajustar({ volumen: v });
  });
  $('volumen').addEventListener('change', async () => { estado.config = await L.guardarConfig({ volumen: +$('volumen').value }); });

  function aplicarEfectos () {
    const on = estado.config.efectos !== false;
    $('efectos').textContent = on ? 'ON' : 'OFF';
    pasillo.mover = on;
    grano.activo = on;
    if (on && !estado.jugando) pasillo.iniciar(); else pasillo.parar();
  }
  $('efectos').addEventListener('click', async () => {
    estado.config = await L.guardarConfig({ efectos: estado.config.efectos === false });
    sonido && sonido.click();
    aplicarEfectos();
  });

  function aplicarSonido () {
    $('txt-sonido').textContent = estado.config.silencio ? 'OFF' : 'ON';
    sonido && sonido.ajustar({ volumen: estado.config.volumen, silencio: Boolean(estado.config.silencio) });
  }
  $('btn-sonido').addEventListener('click', async () => {
    estado.config = await L.guardarConfig({ silencio: !estado.config.silencio });
    aplicarSonido();
    sonido && sonido.click();
  });

  $('btn-min').addEventListener('click', () => L.minimizar());
  $('btn-cerrar').addEventListener('click', () => L.cerrar());

  /* -------------------------------------------------------- actualizacion */

  L.onActualizacion((a) => {
    const chip = $('aviso-actualizacion');
    if (a.estado === 'lista') {
      chip.hidden = false;
      chip.textContent = `ACTUALIZACIÓN ${a.version} · REINICIAR`;
      chip.onclick = () => L.instalarActualizacion();
    }
  });

  /* ------------------------------------------------- parpadeo de las luces */

  function parpadeoAleatorio () {
    setTimeout(() => {
      if (!estado.jugando && estado.config.efectos !== false && !document.hidden) {
        const pasos = [[0, 0.25], [70, 1], [150, 0.4], [210, 1]];
        if (Math.random() < 0.3) pasos.push([520, 0.15], [700, 1]);
        for (const [t, v] of pasos) setTimeout(() => { pasillo.luz = v; }, t);
        sonido && sonido.parpadeo();
        document.body.animate([{ filter: 'brightness(1)' }, { filter: 'brightness(0.7)' }, { filter: 'brightness(1)' }], { duration: 230 });
      }
      parpadeoAleatorio();
    }, 7000 + Math.random() * 16000);
  }

  /* ------------------------------------------------------ imagen de fondo */

  function probarFondo () {
    const opciones = ['fondo-launcher.webp', 'fondo-launcher.jpg', 'fondo-launcher.png'];
    const probar = (i) => {
      if (i >= opciones.length) return;
      const url = `../../assets/${opciones[i]}`;
      const img = new Image();
      img.onload = () => {
        $('fondo-imagen').style.backgroundImage = `url("${url}")`;
        document.body.classList.add('con-imagen');
        pasillo.parar();
        pasillo.activo = false;
      };
      img.onerror = () => probar(i + 1);
      img.src = url;
    };
    probar(0);
  }

  /* ---------------------------------------------------------------- inicio */

  document.querySelectorAll('button, .interruptor').forEach((b) => {
    b.addEventListener('mouseenter', () => { if (!b.disabled && sonido) sonido.hover(); });
  });

  async function iniciar () {
    const s = await L.estado();
    estado.config = s.config;
    estado.cuenta = s.cuenta;
    estado.jugando = s.juegoAbierto;
    $('subtitulo').textContent = s.evento.subtitulo || '';
    $('nombre-evento').textContent = s.evento.nombre;
    $('nombre-evento').dataset.texto = s.evento.nombre;

    const maxRam = Math.max(2048, Math.min(16384, Math.floor((s.memoriaMB - 2048) / 512) * 512));
    $('ram').max = maxRam;
    $('ram').value = Math.min(s.config.ramMB, maxRam);
    $('ram-txt').textContent = textoRam(+$('ram').value);
    $('volumen').value = s.config.volumen;
    $('volumen-txt').textContent = `${Math.round(s.config.volumen * 100)}%`;
    $('osd-nota').textContent = `MEMORIA DEL EQUIPO ${textoRam(s.memoriaMB)} · LAUNCHER v${s.version}`;

    pintarPase();
    aplicarSonido();
    aplicarEfectos();
    probarFondo();
    relojHud();
    setInterval(relojHud, 1000);
    setInterval(cuentaAtras, 1000);
    setInterval(pingServidor, 30000);

    // Los sonidos cargan en paralelo: la intro no los espera.
    const sonidosListos = sonido ? sonido.cargar().catch(() => {}) : Promise.resolve();
    await document.fonts.ready;
    setTimeout(() => {
      document.body.classList.remove('cargando');
      document.body.classList.add('listo');
      parpadeoAleatorio();
      sonidosListos.then(() => sonido && sonido.iniciarAmbiente());
    }, 900);

    cargarManifest();
  }

  iniciar();
})();
