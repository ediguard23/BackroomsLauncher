'use strict';
/**
 * Cuentas: nick libre (offline) o Microsoft (premium).
 *
 * Premium sigue la cadena oficial: Microsoft -> Xbox Live -> XSTS ->
 * Minecraft. El token de Minecraft caduca a las ~24 h, asi que antes de cada
 * arranque se renueva con el refresh token guardado; sin eso, al dia
 * siguiente el juego abre pero los servidores premium rechazan la sesion.
 */

const crypto = require('crypto');
const { USER_AGENT } = require('./net');

// Cliente del launcher oficial de Minecraft: es el unico que Mojang deja
// usar en login_with_xbox sin registrar una app aparte.
const CLIENT_ID = '00000000402b5328';
const REDIRECT = 'https://login.live.com/oauth20_desktop.srf';
const SCOPE = 'service::user.auth.xboxlive.com::MBI_SSL';

const NICK_VALIDO = /^[A-Za-z0-9_]{3,16}$/;

/* ----------------------------------------------------------------- offline */

/** UUID que el servidor en offline-mode asigna a ese nick (UUID v3 de "OfflinePlayer:<nick>"). */
function uuidOffline (nick) {
  const b = crypto.createHash('md5').update(`OfflinePlayer:${nick}`, 'utf8').digest();
  b[6] = (b[6] & 0x0f) | 0x30;
  b[8] = (b[8] & 0x3f) | 0x80;
  const h = b.toString('hex');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}

function cuentaOffline (nick) {
  if (!NICK_VALIDO.test(nick || '')) throw new Error('El nick debe tener de 3 a 16 letras, numeros o _');
  return { type: 'offline', name: nick, uuid: uuidOffline(nick), accessToken: '0', userType: 'legacy', xuid: '0' };
}

/* --------------------------------------------------------------- microsoft */

async function pedir (url, { method = 'GET', json, form, token } = {}) {
  const headers = { 'User-Agent': USER_AGENT, Accept: 'application/json' };
  let body;
  if (json) { headers['Content-Type'] = 'application/json'; body = JSON.stringify(json); }
  if (form) { headers['Content-Type'] = 'application/x-www-form-urlencoded'; body = new URLSearchParams(form).toString(); }
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(url, { method, headers, body, signal: AbortSignal.timeout(20000) });
  const texto = await res.text();
  let datos = {};
  try { datos = texto ? JSON.parse(texto) : {}; } catch { /* no es json */ }
  if (!res.ok) {
    const err = new Error(datos.error_description || datos.errorMessage || datos.error || `HTTP ${res.status}`);
    err.status = res.status;
    err.datos = datos;
    throw err;
  }
  return datos;
}

function urlLogin () {
  const p = new URLSearchParams({ client_id: CLIENT_ID, response_type: 'code', scope: SCOPE, redirect_uri: REDIRECT, prompt: 'select_account' });
  return `https://login.live.com/oauth20_authorize.srf?${p}`;
}

const ERRORES_XSTS = {
  2148916233: 'Esta cuenta de Microsoft no tiene perfil de Xbox. Entra una vez en minecraft.net para crearlo.',
  2148916235: 'Xbox Live no esta disponible en tu pais.',
  2148916236: 'La cuenta necesita verificacion de edad en xbox.com.',
  2148916237: 'La cuenta necesita verificacion de edad en xbox.com.',
  2148916238: 'Es una cuenta de menor: un adulto tiene que anadirla a una familia de Microsoft.'
};

async function xbox (msToken) {
  let xbl;
  try {
    xbl = await pedir('https://user.auth.xboxlive.com/user/authenticate', {
      method: 'POST',
      json: { Properties: { AuthMethod: 'RPS', SiteName: 'user.auth.xboxlive.com', RpsTicket: `t=${msToken}` }, RelyingParty: 'http://auth.xboxlive.com', TokenType: 'JWT' }
    });
  } catch (err) {
    if (err.status !== 400 && err.status !== 401) throw err;
    // Segun el tipo de token, Xbox lo quiere con o sin el prefijo "t=".
    xbl = await pedir('https://user.auth.xboxlive.com/user/authenticate', {
      method: 'POST',
      json: { Properties: { AuthMethod: 'RPS', SiteName: 'user.auth.xboxlive.com', RpsTicket: msToken }, RelyingParty: 'http://auth.xboxlive.com', TokenType: 'JWT' }
    });
  }
  let xsts;
  try {
    xsts = await pedir('https://xsts.auth.xboxlive.com/xsts/authorize', {
      method: 'POST',
      json: { Properties: { SandboxId: 'RETAIL', UserTokens: [xbl.Token] }, RelyingParty: 'rp://api.minecraftservices.com/', TokenType: 'JWT' }
    });
  } catch (err) {
    const x = err.datos && err.datos.XErr;
    throw new Error(ERRORES_XSTS[x] || `Xbox rechazo la cuenta (${x || err.message})`);
  }
  return { token: xsts.Token, uhs: xsts.DisplayClaims.xui[0].uhs };
}

async function minecraft (msToken) {
  const x = await xbox(msToken);
  const mc = await pedir('https://api.minecraftservices.com/authentication/login_with_xbox', {
    method: 'POST',
    json: { identityToken: `XBL3.0 x=${x.uhs};${x.token}` }
  });
  let perfil;
  try {
    perfil = await pedir('https://api.minecraftservices.com/minecraft/profile', { token: mc.access_token });
  } catch (err) {
    if (err.status === 404) throw new Error('Esta cuenta de Microsoft no tiene Minecraft Java comprado.');
    throw err;
  }
  const id = perfil.id;
  const skin = (perfil.skins || []).find((s) => s.state === 'ACTIVE') || (perfil.skins || [])[0];
  return {
    type: 'msa',
    name: perfil.name,
    uuid: `${id.slice(0, 8)}-${id.slice(8, 12)}-${id.slice(12, 16)}-${id.slice(16, 20)}-${id.slice(20)}`,
    accessToken: mc.access_token,
    expiresAt: Date.now() + (mc.expires_in || 86400) * 1000,
    userType: 'msa',
    xuid: '0',
    skin: skin ? skin.url : null
  };
}

/**
 * Login completo. `abrirVentana(url, prefijo)` abre el login de Microsoft y
 * resuelve con la URL final en cuanto empieza por `prefijo`.
 */
async function loginMicrosoft (abrirVentana) {
  const final = await abrirVentana(urlLogin(), REDIRECT);
  const q = new URL(final).searchParams;
  if (q.get('error')) {
    if (q.get('error') === 'access_denied') throw new Error('Inicio de sesion cancelado');
    throw new Error(q.get('error_description') || q.get('error'));
  }
  const code = q.get('code');
  if (!code) throw new Error('Microsoft no devolvio el codigo de acceso');
  const ms = await pedir('https://login.live.com/oauth20_token.srf', {
    method: 'POST',
    form: { client_id: CLIENT_ID, code, grant_type: 'authorization_code', redirect_uri: REDIRECT, scope: SCOPE }
  });
  return { ...(await minecraft(ms.access_token)), refreshToken: ms.refresh_token };
}

/** Devuelve la cuenta con el token vigente, renovandolo si caduca en menos de 10 min. */
async function renovar (cuenta) {
  if (!cuenta || cuenta.type !== 'msa') return cuenta;
  if (cuenta.expiresAt && cuenta.expiresAt - Date.now() > 10 * 60 * 1000) return cuenta;
  if (!cuenta.refreshToken) throw Object.assign(new Error('La sesion premium caduco. Vuelve a iniciar sesion.'), { relogin: true });
  let ms;
  try {
    ms = await pedir('https://login.live.com/oauth20_token.srf', {
      method: 'POST',
      form: { client_id: CLIENT_ID, refresh_token: cuenta.refreshToken, grant_type: 'refresh_token', redirect_uri: REDIRECT, scope: SCOPE }
    });
  } catch (err) {
    if (err.status === 400 || err.status === 401) {
      throw Object.assign(new Error('La sesion premium caduco. Vuelve a iniciar sesion.'), { relogin: true });
    }
    throw err;
  }
  return { ...(await minecraft(ms.access_token)), refreshToken: ms.refresh_token || cuenta.refreshToken };
}

module.exports = { NICK_VALIDO, uuidOffline, cuentaOffline, loginMicrosoft, renovar, REDIRECT };
