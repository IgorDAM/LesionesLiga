// app.js — frontend sin dependencias ni paso de compilación: HTML + JS nativo.
import { svgMapa } from './body.js';

/* ================= Utilidades ================= */

const $ = (sel, raiz = document) => raiz.querySelector(sel);
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const hoyISO = () => new Date().toLocaleDateString('sv-SE'); // yyyy-mm-dd en hora local
const fecha = (iso) => (iso ? new Date(`${iso}T00:00:00`).toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' }) : '—');
const num = (n, dec = 0) => (n == null ? '—' : Number(n).toLocaleString('es-ES', { maximumFractionDigits: dec }));
const plural = (n, uno, varios) => `${n} ${n === 1 ? uno : varios}`;
const ladoTxt = (l) => ({ DERECHO: 'derecho', IZQUIERDO: 'izquierdo' }[l] || '');
const lesionTxt = (e) => `${e.tipo} — ${e.zona}${ladoTxt(e.lado) ? ` (${ladoTxt(e.lado)})` : ''}`;
const divisionTxt = (d) => (d === 'PRIMERA' ? 'Primera' : 'Segunda');
const urlSegura = (u) => (/^https?:\/\//i.test(u || '') ? u : '#');
const corto = (s, max = 24) => (s.length > max ? `${s.slice(0, max - 1)}…` : s);
const vacioANull = (v) => (v === '' || v == null ? null : v);
const debounce = (fn, ms = 250) => { let t; return (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms); }; };

// sessionStorage puede no estar disponible (modo privado, etc.): se envuelve para no romper la web.
const leerSesion = (k) => { try { return sessionStorage.getItem(k) || ''; } catch { return ''; } };
const guardarSesion = (k, v) => { try { if (v) sessionStorage.setItem(k, v); else sessionStorage.removeItem(k); } catch { /* sin storage */ } };

let adminToken = leerSesion('adminToken');

async function api(ruta, { method = 'GET', body, admin = false } = {}) {
  const cabeceras = {};
  if (body !== undefined) cabeceras['Content-Type'] = 'application/json';
  if (admin) cabeceras['X-Admin-Token'] = adminToken;
  const res = await fetch(ruta, { method, headers: cabeceras, body: body !== undefined ? JSON.stringify(body) : undefined });
  if (!res.ok) {
    let mensaje = res.statusText || `Error ${res.status}`;
    try { mensaje = (await res.json()).error || mensaje; } catch { /* cuerpo no JSON */ }
    const err = new Error(mensaje);
    err.status = res.status;
    throw err;
  }
  if (res.status === 204) return null;
  return (res.headers.get('content-type') || '').includes('json') ? res.json() : res.text();
}

const estado = { zonas: [], nombresZona: {}, clubes: [] };

function pintarMapas(contenedor, resaltes = [], zonaSeleccionada = '') {
  contenedor.innerHTML = `<div class="mapas">${svgMapa('FRENTE', { nombres: estado.nombresZona, resaltes })}${svgMapa('ESPALDA', { nombres: estado.nombresZona, resaltes })}</div>`;
  if (zonaSeleccionada) {
    contenedor.querySelectorAll(`.z[data-zona="${CSS.escape(zonaSeleccionada)}"]`).forEach((z) => z.classList.add('seleccionada'));
  }
}

function enlazarMapa(contenedor, alElegir) {
  const elegir = (ev) => {
    const z = ev.target.closest('.z');
    if (z) alElegir(z.dataset.zona, z.dataset.lado);
  };
  contenedor.addEventListener('click', elegir);
  contenedor.addEventListener('keydown', (ev) => {
    if (ev.key === 'Enter' || ev.key === ' ') { ev.preventDefault(); elegir(ev); }
  });
}

const opcionesZonas = () => estado.zonas.map((z) => `<option value="${esc(z.codigo)}">${esc(z.nombre)}</option>`).join('');
const opcionesClubes = () => ['PRIMERA', 'SEGUNDA'].map((div) => `<optgroup label="${divisionTxt(div)} División">${
  estado.clubes.filter((c) => c.division === div).map((c) => `<option value="${c.id}">${esc(c.nombre)}</option>`).join('')}</optgroup>`).join('');

const insignias = (e) => (e.recaida ? '<span class="badge aviso">recaída</span>' : '') + (e.cirugia ? '<span class="badge">cirugía</span>' : '');
const enlaceFuente = (e) => (e.fuenteUrl ? `<a href="${esc(urlSegura(e.fuenteUrl))}" target="_blank" rel="noopener noreferrer nofollow">${esc(e.fuenteNombre || 'Fuente')}</a>` : '—');

/* ================= Cabecera y pestañas ================= */

async function cargarResumen() {
  try {
    const r = await api('/api/resumen');
    $('#actualizacion').textContent = r.ultimaActualizacion
      ? `Última actualización: ${new Date(r.ultimaActualizacion).toLocaleDateString('es-ES', { day: 'numeric', month: 'long', year: 'numeric' })} · ${plural(r.lesionadosActivos, 'lesionado', 'lesionados')} ahora · ${plural(r.casosCerrados, 'caso cerrado', 'casos cerrados')}`
      : 'Aún no hay lesiones registradas.';
  } catch { $('#actualizacion').textContent = ''; }
}

const PESTANAS = ['ahora', 'jugadores', 'comparar', 'admin'];

function activarTab(id) {
  document.querySelectorAll('[data-tab]').forEach((b) => b.setAttribute('aria-selected', String(b.dataset.tab === id)));
  document.querySelectorAll('.panel').forEach((p) => { p.hidden = p.id !== `tab-${id}`; });
  try { history.replaceState(null, '', `#${id}`); } catch { /* ignorar */ }
  if (id === 'ahora') cargarAhora();
  if (id === 'admin') iniciarAdmin();
}

/* ================= Lesionados ahora ================= */

const filtros = { division: '', clubId: '', zona: '' };
let ultimaLista = [];

function montarAhora() {
  Object.assign(filtros, { division: '', clubId: '', zona: '' });
  $('#tab-ahora').innerHTML = `
    <div class="filtros">
      <label>División<select id="f-division"><option value="">Todas</option><option value="PRIMERA">Primera</option><option value="SEGUNDA">Segunda</option></select></label>
      <label>Club<select id="f-club"><option value="">Todos</option>${opcionesClubes()}</select></label>
      <label>Zona del cuerpo<select id="f-zona"><option value="">Todas</option>${opcionesZonas()}</select></label>
    </div>
    <div class="con-mapa">
      <div><p id="ahora-resumen" class="sub" aria-live="polite"></p><div id="ahora-lista" class="tabla-wrap"></div></div>
      <aside class="tarjeta"><h3>Dónde se lesionan</h3><div id="ahora-mapa"></div>
        <div class="leyenda"><span><i style="background:var(--alerta)"></i>Lesión activa</span><span>Pulsa una zona para filtrar</span></div></aside>
    </div>`;
  $('#f-division').addEventListener('change', (e) => { filtros.division = e.target.value; cargarAhora(); });
  $('#f-club').addEventListener('change', (e) => { filtros.clubId = e.target.value; cargarAhora(); });
  $('#f-zona').addEventListener('change', (e) => { filtros.zona = e.target.value; pintarAhora(); });
  enlazarMapa($('#ahora-mapa'), (zona) => {
    filtros.zona = filtros.zona === zona ? '' : zona;
    $('#f-zona').value = filtros.zona;
    pintarAhora();
  });
  $('#ahora-lista').addEventListener('click', (ev) => {
    const b = ev.target.closest('[data-jugador]');
    if (b) abrirFicha(b.dataset.jugador);
  });
}

async function cargarAhora() {
  const q = new URLSearchParams();
  if (filtros.division) q.set('division', filtros.division);
  if (filtros.clubId) q.set('clubId', filtros.clubId);
  try {
    ultimaLista = await api(`/api/lesiones/activas?${q}`);
    pintarAhora();
  } catch (e) {
    $('#ahora-lista').innerHTML = `<p class="error">${esc(e.message)}</p>`;
  }
  cargarResumen();
}

function pintarAhora() {
  const visibles = filtros.zona ? ultimaLista.filter((e) => e.zonaCodigo === filtros.zona) : ultimaLista;
  $('#ahora-resumen').textContent = plural(visibles.length, 'lesión activa', 'lesiones activas');
  pintarMapas($('#ahora-mapa'), ultimaLista.map((e) => ({ zona: e.zonaCodigo, lado: e.lado, n: 1, clase: 'activa' })), filtros.zona);

  if (!visibles.length) {
    $('#ahora-lista').innerHTML = '<p class="vacio">No hay lesiones activas registradas con estos filtros. Los datos se van acumulando semana a semana.</p>';
    return;
  }
  const hoy = hoyISO();
  $('#ahora-lista').innerHTML = `<table><thead><tr><th>Jugador</th><th>Lesión</th><th>Desde</th><th>Regreso estimado</th><th>Fuente</th></tr></thead><tbody>${
    visibles.map((e) => `<tr>
      <td><button class="enlace" data-jugador="${e.jugadorId}">${esc(e.jugador)}</button><div class="sub">${esc(e.club)} · ${divisionTxt(e.division)}</div></td>
      <td>${esc(lesionTxt(e))}${insignias(e)}${e.diagnosticoClub ? `<div class="sub">${esc(e.diagnosticoClub)}</div>` : ''}</td>
      <td>${fecha(e.fechaInicio)}<div class="sub">${plural(e.diasBaja, 'día', 'días')}</div></td>
      <td>${e.regresoEstimado ? `${fecha(e.regresoEstimado)}${e.regresoEstimado < hoy ? '<div class="sub">plazo del club superado</div>' : '<div class="sub">según el club</div>'}` : '<span class="sub">sin plazo</span>'}</td>
      <td>${enlaceFuente(e)}</td></tr>`).join('')}</tbody></table>`;
}

/* ================= Jugadores ================= */

function montarJugadores() {
  $('#tab-jugadores').innerHTML = `
    <label for="q-jugador">Buscar jugador</label>
    <input id="q-jugador" type="search" placeholder="Escribe al menos 2 letras…" autocomplete="off">
    <ul id="res-jugadores" class="resultados"></ul>
    <div id="ficha"></div>`;
  const buscar = debounce(async () => {
    const q = $('#q-jugador').value.trim();
    const ul = $('#res-jugadores');
    if (q.length < 2) { ul.innerHTML = ''; return; }
    try {
      const lista = await api(`/api/jugadores?q=${encodeURIComponent(q)}`);
      ul.innerHTML = lista.length
        ? lista.map((j) => `<li><button data-jugador="${j.id}">${esc(j.nombre)} <span class="sub">${esc(j.club)} · ${divisionTxt(j.division)}</span></button></li>`).join('')
        : '<li class="vacio">Ningún jugador coincide. Solo aparecen los jugadores con lesiones registradas o dados de alta.</li>';
    } catch (e) { ul.innerHTML = `<li class="error">${esc(e.message)}</li>`; }
  });
  $('#q-jugador').addEventListener('input', buscar);
  $('#res-jugadores').addEventListener('click', (ev) => {
    const b = ev.target.closest('[data-jugador]');
    if (b) abrirFicha(b.dataset.jugador);
  });
  $('#ficha').addEventListener('click', (ev) => {
    const b = ev.target.closest('[data-comparar]');
    if (b) abrirComparativa(b.dataset.zona, Number(b.dataset.comparar));
  });
}

async function abrirFicha(id) {
  activarTab('jugadores');
  const cont = $('#ficha');
  cont.innerHTML = '<p class="sub">Cargando…</p>';
  try {
    const f = await api(`/api/jugadores/${id}`);
    const j = f.jugador;
    const resaltes = [
      ...f.activas.map((e) => ({ zona: e.zonaCodigo, lado: e.lado, n: 1, clase: 'activa' })),
      ...f.historial.map((e) => ({ zona: e.zonaCodigo, lado: e.lado, n: 1, clase: 'pasada' })),
    ];
    const estadoActual = f.activas.length
      ? f.activas.map((e) => `<div class="estado activa"><div><strong>Lesionado:</strong> ${esc(lesionTxt(e))}${insignias(e)}
          <div class="sub">Desde ${fecha(e.fechaInicio)} (${plural(e.diasBaja, 'día', 'días')})${e.regresoEstimado ? ` · regreso estimado por el club: ${fecha(e.regresoEstimado)}` : ''} · ${enlaceFuente(e)}</div>
          ${e.diagnosticoClub ? `<div class="sub">${esc(e.diagnosticoClub)}</div>` : ''}</div></div>`).join('')
      : '<div class="estado sana"><div><strong>Sin lesión activa registrada.</strong><div class="sub">Solo consta lo registrado desde que empezó el seguimiento.</div></div></div>';

    cont.innerHTML = `<div class="con-mapa">
      <div><h2>${esc(j.nombre)}</h2><p class="sub">${esc(j.club)} · ${divisionTxt(j.division)}${j.posicion ? ` · ${esc(j.posicion.toLowerCase())}` : ''}</p>
        ${estadoActual}
        <h3>Historial</h3>
        ${f.historial.length ? `<div class="tabla-wrap"><table><thead><tr><th>Lesión</th><th>Inicio</th><th>Alta</th><th class="num">Días</th><th class="num">Partidos</th><th>Fuente</th><th></th></tr></thead><tbody>${
          f.historial.map((e) => `<tr><td>${esc(lesionTxt(e))}${insignias(e)}</td><td>${fecha(e.fechaInicio)}</td><td>${fecha(e.fechaAlta)}</td>
            <td class="num">${e.diasBaja}</td><td class="num">${num(e.partidosPerdidos)}</td><td>${enlaceFuente(e)}</td>
            <td><button class="enlace" data-comparar="${e.tipoId}" data-zona="${esc(e.zonaCodigo)}">Comparar</button></td></tr>`).join('')}</tbody></table></div>`
          : '<p class="vacio">Sin lesiones anteriores registradas.</p>'}
      </div>
      <aside class="tarjeta"><div id="ficha-mapa"></div>
        <div class="leyenda"><span><i style="background:var(--alerta)"></i>Activa</span><span><i style="background:var(--aviso)"></i>Anterior</span></div></aside></div>`;
    pintarMapas($('#ficha-mapa'), resaltes);
  } catch (e) {
    cont.innerHTML = `<p class="error">${esc(e.message)}</p>`;
  }
}

/* ================= Comparar recuperaciones ================= */

const cmp = { zona: '', tipoId: null, cirugia: '', recaidas: true };

function montarComparar() {
  Object.assign(cmp, { zona: '', tipoId: null, cirugia: '', recaidas: true });
  $('#tab-comparar').innerHTML = `
    <p class="sub">Elige una zona (en la lista o pulsando el cuerpo) y un tipo de lesión para ver cuánto han tardado en volver los jugadores registrados. Cuantos más casos, más fiable es la comparación.</p>
    <div class="con-mapa">
      <div>
        <div class="filtros"><label>Zona<select id="c-zona"><option value="">Elige una zona…</option>${opcionesZonas()}</select></label></div>
        <div id="c-tipos"></div><div id="c-detalle"></div>
      </div>
      <aside class="tarjeta"><div id="c-mapa"></div></aside>
    </div>`;
  pintarMapas($('#c-mapa'));
  const elegirZona = (zona) => {
    cmp.zona = zona;
    cmp.tipoId = null;
    $('#c-zona').value = zona;
    $('#c-detalle').innerHTML = '';
    pintarMapas($('#c-mapa'), [], zona);
    cargarTiposZona();
  };
  $('#c-zona').addEventListener('change', (e) => elegirZona(e.target.value));
  enlazarMapa($('#c-mapa'), (zona) => elegirZona(zona));
  $('#c-tipos').addEventListener('click', (ev) => {
    const b = ev.target.closest('[data-tipo]');
    if (b) { cmp.tipoId = Number(b.dataset.tipo); mostrarDetalle(); }
  });
  const det = $('#c-detalle');
  det.addEventListener('change', (ev) => {
    if (ev.target.id === 'c-cirugia') cmp.cirugia = ev.target.value;
    if (ev.target.id === 'c-recaidas') cmp.recaidas = ev.target.checked;
    mostrarDetalle();
  });
  det.addEventListener('click', (ev) => {
    const b = ev.target.closest('[data-jugador]');
    if (b) abrirFicha(b.dataset.jugador);
  });
}

async function abrirComparativa(zona, tipoId) {
  activarTab('comparar');
  cmp.zona = zona;
  cmp.tipoId = tipoId;
  cmp.cirugia = '';
  cmp.recaidas = true;
  $('#c-zona').value = zona;
  pintarMapas($('#c-mapa'), [], zona);
  await cargarTiposZona();
  mostrarDetalle();
}

async function cargarTiposZona() {
  const cont = $('#c-tipos');
  if (!cmp.zona) { cont.innerHTML = ''; return; }
  try {
    const filas = await api(`/api/estadisticas/zona/${encodeURIComponent(cmp.zona)}`);
    cont.innerHTML = `<h2>${esc(estado.nombresZona[cmp.zona] || cmp.zona)}</h2><div class="tabla-wrap"><table>
      <thead><tr><th>Tipo de lesión</th><th>Gravedad</th><th class="num">Casos cerrados</th><th class="num">Mediana (días)</th><th class="num">Media (días)</th><th class="num">Activos</th></tr></thead><tbody>${
      filas.map((f) => `<tr class="${f.tipoId === cmp.tipoId ? 'clicable' : ''}"><td><button class="enlace" data-tipo="${f.tipoId}">${esc(f.tipo)}</button></td>
        <td>${esc(f.gravedad.toLowerCase())}</td><td class="num">${f.casosCerrados}</td><td class="num">${num(f.medianaDias, 1)}</td><td class="num">${num(f.mediaDias, 1)}</td><td class="num">${f.casosActivos}</td></tr>`).join('')}</tbody></table></div>`;
  } catch (e) { cont.innerHTML = `<p class="error">${esc(e.message)}</p>`; }
}

async function mostrarDetalle() {
  const cont = $('#c-detalle');
  if (!cmp.tipoId) { cont.innerHTML = ''; return; }
  const q = new URLSearchParams({ incluirRecaidas: String(cmp.recaidas) });
  if (cmp.cirugia !== '') q.set('cirugia', cmp.cirugia);
  try {
    const d = await api(`/api/estadisticas/tipo/${cmp.tipoId}?${q}`);
    const desvio = d.desvioMedioVsEstimado;
    cont.innerHTML = `
      <h3 style="margin-top:1.5rem">${esc(d.tipo)} — ${esc(d.zona)}</h3>
      <div class="opciones">
        <label>Cirugía<select id="c-cirugia"><option value="">Todos los casos</option><option value="true"${cmp.cirugia === 'true' ? ' selected' : ''}>Con cirugía</option><option value="false"${cmp.cirugia === 'false' ? ' selected' : ''}>Sin cirugía</option></select></label>
        <label class="check"><input id="c-recaidas" type="checkbox"${cmp.recaidas ? ' checked' : ''}> Incluir recaídas</label>
      </div>
      ${d.casosCerrados === 0 ? '<p class="aviso-caja">Todavía no hay casos cerrados de esta lesión con estos filtros, así que no se puede calcular una recuperación media.</p>'
        : d.pocosCasos ? `<p class="aviso-caja">Solo hay ${plural(d.casosCerrados, 'caso cerrado', 'casos cerrados')} (se recomiendan al menos ${d.minimoCasos}). Los valores son orientativos y pueden no ser representativos.</p>` : ''}
      <div class="cifras">
        <div class="cifra"><b>${d.casosCerrados}</b><span>casos cerrados${d.casosActivos ? ` · ${d.casosActivos} activos` : ''}</span></div>
        <div class="cifra"><b>${num(d.medianaDias, 1)}</b><span>mediana (días de baja)</span></div>
        <div class="cifra"><b>${num(d.mediaDias, 1)}</b><span>media (días de baja)</span></div>
        <div class="cifra"><b>${d.minDias == null ? '—' : `${d.minDias}–${d.maxDias}`}</b><span>mínimo – máximo (días)</span></div>
        <div class="cifra"><b>${num(d.mediaPartidos, 1)}</b><span>partidos perdidos (media)</span></div>
        <div class="cifra"><b>${desvio == null ? '—' : `${desvio > 0 ? '+' : ''}${num(desvio, 1)}`}</b><span>días vs. plazo del club (${d.casosConEstimacion} ${d.casosConEstimacion === 1 ? 'caso' : 'casos'})</span></div>
      </div>
      ${graficoDias(d.casos, d.medianaDias)}
      <h3>Casos</h3>
      <div class="tabla-wrap"><table><thead><tr><th>Jugador</th><th>Lado</th><th>Inicio</th><th>Alta</th><th class="num">Días</th><th class="num">Partidos</th><th>Fuente</th></tr></thead><tbody>${
        d.casos.map((e) => `<tr><td><button class="enlace" data-jugador="${e.jugadorId}">${esc(e.jugador)}</button><div class="sub">${esc(e.club)}</div></td>
          <td>${esc(ladoTxt(e.lado)) || '—'}${insignias(e)}</td><td>${fecha(e.fechaInicio)}</td>
          <td>${e.activa ? '<span class="badge alerta">activa</span>' : fecha(e.fechaAlta)}</td>
          <td class="num">${e.diasBaja}</td><td class="num">${num(e.partidosPerdidos)}</td><td>${enlaceFuente(e)}</td></tr>`).join('')}</tbody></table></div>`;
    cargarTiposZona(); // refresca la fila seleccionada
  } catch (e) { cont.innerHTML = `<p class="error">${esc(e.message)}</p>`; }
}

/** Barras horizontales (una por caso cerrado) con la mediana marcada. */
function graficoDias(casos, mediana) {
  const cerrados = casos.filter((c) => !c.activa).sort((a, b) => a.diasBaja - b.diasBaja);
  if (!cerrados.length) return '';
  const fila = 24, izq = 160, ancho = 640, der = 50;
  const max = Math.max(...cerrados.map((c) => c.diasBaja), mediana || 0, 1);
  const x = (dias) => izq + (dias / max) * (ancho - izq - der);
  const alto = cerrados.length * fila + 26;
  const barras = cerrados.map((c, i) => `<text class="g-etq" x="${izq - 8}" y="${i * fila + 16}" text-anchor="end">${esc(corto(c.jugador))}</text>`
    + `<rect class="g-barra" x="${izq}" y="${i * fila + 4}" width="${Math.max(2, x(c.diasBaja) - izq)}" height="${fila - 8}" rx="3"/>`
    + `<text class="g-val" x="${x(c.diasBaja) + 6}" y="${i * fila + 16}">${c.diasBaja}</text>`).join('');
  const linea = mediana == null ? '' : `<line class="g-med" x1="${x(mediana)}" x2="${x(mediana)}" y1="0" y2="${alto - 18}"/>`
    + `<text class="g-etq" x="${x(mediana)}" y="${alto - 4}" text-anchor="middle">mediana ${num(mediana, 1)} días</text>`;
  return `<svg class="grafico" viewBox="0 0 ${ancho} ${alto}" role="img" aria-label="Días de baja de cada caso cerrado, con la mediana marcada">${barras}${linea}</svg>`;
}

/* ================= Administración ================= */

const SUBPANELES = {
  registrar: ['Registrar lesión', admRegistrar],
  activas: ['Altas y borrados', admActivas],
  jugadores: ['Jugadores, clubes y catálogo', admJugadores],
  semanal: ['Revisión semanal', admSemanal],
  copia: ['Copia de seguridad', admCopia],
};
let subpanel = 'registrar';

const mensaje = (texto, tipo = 'ok') => { const m = $('#adm-msg'); if (m) m.innerHTML = texto ? `<p class="${tipo}" role="status">${esc(texto)}</p>` : ''; };

function cerrarSesionAdmin(aviso) {
  adminToken = '';
  guardarSesion('adminToken', '');
  pintarLogin(aviso);
}

/** Ejecuta una acción de administración mostrando los errores; si el token caduca, vuelve al login. */
async function conAviso(accion) {
  try {
    mensaje('');
    await accion();
  } catch (e) {
    if (e.status === 401) cerrarSesionAdmin('El token ya no es válido. Vuelve a introducirlo.');
    else mensaje(e.message, 'error');
  }
}

function pintarLogin(aviso = '') {
  $('#tab-admin').innerHTML = `<form id="f-login" class="form caja">
    <h2>Administración</h2>
    <p class="sub">Solo para quien mantiene los datos. Introduce el token configurado en el servidor (variable ADMIN_TOKEN).</p>
    ${aviso ? `<p class="error">${esc(aviso)}</p>` : ''}
    <label>Token<input id="login-token" type="password" autocomplete="current-password" required></label>
    <button class="primario" type="submit">Entrar</button></form>`;
  $('#f-login').addEventListener('submit', (ev) => {
    ev.preventDefault();
    adminToken = $('#login-token').value;
    guardarSesion('adminToken', adminToken);
    iniciarAdmin();
  });
}

async function iniciarAdmin() {
  if (!adminToken) { pintarLogin(); return; }
  try {
    await api('/api/admin/ping', { admin: true });
    pintarPanelAdmin();
  } catch (e) {
    if (e.status === 401) cerrarSesionAdmin('Token incorrecto.');
    else $('#tab-admin').innerHTML = `<p class="error">${esc(e.message)}</p>`;
  }
}

function pintarPanelAdmin() {
  $('#tab-admin').innerHTML = `
    <div class="subnav" role="group" aria-label="Secciones de administración">${
      Object.entries(SUBPANELES).map(([k, [titulo]]) => `<button class="secundario" data-sub="${k}" aria-pressed="${k === subpanel}">${titulo}</button>`).join('')
    }<button class="secundario" id="adm-salir">Salir</button></div>
    <div id="adm-msg" aria-live="polite"></div><div id="adm-contenido"></div>`;
  $('#tab-admin .subnav').addEventListener('click', (ev) => {
    const b = ev.target.closest('button');
    if (!b) return;
    if (b.id === 'adm-salir') { cerrarSesionAdmin(); return; }
    subpanel = b.dataset.sub;
    document.querySelectorAll('[data-sub]').forEach((x) => x.setAttribute('aria-pressed', String(x.dataset.sub === subpanel)));
    mensaje('');
    conAviso(SUBPANELES[subpanel][1]);
  });
  conAviso(SUBPANELES[subpanel][1]);
}

/** Tras cambiar datos, se refrescan la cabecera y las listas que dependen de ellos. */
async function datosCambiados({ recargarClubes = false } = {}) {
  if (recargarClubes) {
    estado.clubes = await api('/api/clubes');
    montarAhora();
  }
  cargarResumen();
}

/* ---- Registrar lesión ---- */
async function admRegistrar() {
  $('#adm-contenido').innerHTML = `<form id="f-ep" class="form">
    <fieldset><legend>Jugador</legend>
      <label>Buscar por nombre<input id="ep-q" type="search" placeholder="Mínimo 2 letras" autocomplete="off"></label>
      <select id="ep-jug" size="4" required aria-label="Jugador"></select>
      <p class="sub">¿No aparece? Créalo antes en «Jugadores, clubes y catálogo».</p>
    </fieldset>
    <fieldset><legend>Lesión</legend>
      <div class="form-fila">
        <label>Zona<select id="ep-zona" required>${opcionesZonas()}</select></label>
        <label>Tipo<select id="ep-tipo" required></select></label>
        <label id="ep-lado-l">Lado<select id="ep-lado"><option value="DERECHO">Derecho</option><option value="IZQUIERDO">Izquierdo</option></select></label>
        <label>Inicio<input id="ep-inicio" type="date" required value="${hoyISO()}" max="${hoyISO()}"></label>
        <label>Plazo anunciado por el club (días)<input id="ep-plazo" type="number" min="1"></label>
      </div>
      <label class="check"><input id="ep-recaida" type="checkbox"> Es una recaída</label>
      <label class="check"><input id="ep-cirugia" type="checkbox"> Requirió cirugía</label>
      <label>Diagnóstico tal como lo publica el club (texto corto, sin copiar artículos)<textarea id="ep-diag" maxlength="500" rows="2"></textarea></label>
    </fieldset>
    <fieldset><legend>Si ya está de alta (para cargar histórico)</legend>
      <div class="form-fila">
        <label>Fecha de alta<input id="ep-alta" type="date" max="${hoyISO()}"></label>
        <label>Partidos perdidos<input id="ep-partidos" type="number" min="0"></label>
      </div>
    </fieldset>
    <fieldset><legend>Fuente (obligatoria)</legend>
      <label>URL del comunicado o noticia<input id="ep-url" type="url" required placeholder="https://…"></label>
      <div class="form-fila">
        <label>Nombre de la fuente<input id="ep-fnombre" maxlength="120" placeholder="Comunicado del club, Marca…"></label>
        <label>Fecha de publicación<input id="ep-ffecha" type="date"></label>
      </div>
      <label>Notas<textarea id="ep-notas" maxlength="1000" rows="2"></textarea></label>
    </fieldset>
    <button class="primario" type="submit">Guardar lesión</button></form>`;

  const cargarTipos = async () => {
    const codigo = $('#ep-zona').value;
    const zona = estado.zonas.find((z) => z.codigo === codigo);
    $('#ep-lado-l').hidden = !(zona && zona.bilateral);
    const tipos = await api(`/api/tipos?zona=${encodeURIComponent(codigo)}`);
    $('#ep-tipo').innerHTML = tipos.map((t) => `<option value="${t.id}">${esc(t.nombre)} (${esc(t.gravedad.toLowerCase())})</option>`).join('');
  };
  $('#ep-zona').addEventListener('change', () => conAviso(cargarTipos));
  await cargarTipos();

  $('#ep-q').addEventListener('input', debounce(() => conAviso(async () => {
    const q = $('#ep-q').value.trim();
    const lista = q.length < 2 ? [] : await api(`/api/jugadores?q=${encodeURIComponent(q)}`);
    $('#ep-jug').innerHTML = lista.map((j) => `<option value="${j.id}">${esc(j.nombre)} — ${esc(j.club)}</option>`).join('');
    if (lista.length === 1) $('#ep-jug').value = String(lista[0].id);
  })));

  $('#f-ep').addEventListener('submit', (ev) => {
    ev.preventDefault();
    conAviso(async () => {
      const zona = estado.zonas.find((z) => z.codigo === $('#ep-zona').value);
      const cuerpo = {
        jugadorId: Number($('#ep-jug').value),
        tipoId: Number($('#ep-tipo').value),
        lado: zona && zona.bilateral ? $('#ep-lado').value : 'NO_APLICA',
        fechaInicio: $('#ep-inicio').value,
        fechaAlta: vacioANull($('#ep-alta').value),
        partidosPerdidos: $('#ep-partidos').value === '' ? null : Number($('#ep-partidos').value),
        recaida: $('#ep-recaida').checked,
        cirugia: $('#ep-cirugia').checked,
        diagnosticoClub: vacioANull($('#ep-diag').value),
        plazoEstimadoDias: $('#ep-plazo').value === '' ? null : Number($('#ep-plazo').value),
        fuenteUrl: $('#ep-url').value.trim(),
        fuenteNombre: vacioANull($('#ep-fnombre').value),
        fechaFuente: vacioANull($('#ep-ffecha').value),
        notas: vacioANull($('#ep-notas').value),
      };
      const e = await api('/api/admin/episodios', { method: 'POST', body: cuerpo, admin: true });
      await admRegistrar();
      mensaje(`Guardado: ${e.jugador} — ${lesionTxt(e)}.`);
      datosCambiados();
    });
  });
}

/* ---- Altas y borrados ---- */
async function admActivas() {
  const lista = await api('/api/lesiones/activas');
  const cont = $('#adm-contenido');
  if (!lista.length) { cont.innerHTML = '<p class="vacio">No hay lesiones activas.</p>'; return; }
  cont.innerHTML = `<div class="tabla-wrap"><table><thead><tr><th>Jugador</th><th>Lesión</th><th>Desde</th><th>Fecha de alta</th><th>Partidos</th><th></th></tr></thead><tbody>${
    lista.map((e) => `<tr data-id="${e.id}"><td>${esc(e.jugador)}<div class="sub">${esc(e.club)}</div></td><td>${esc(lesionTxt(e))}</td><td>${fecha(e.fechaInicio)}</td>
      <td><input type="date" class="alta-fecha" value="${hoyISO()}" min="${esc(e.fechaInicio)}" max="${hoyISO()}"></td>
      <td><input type="number" class="alta-partidos" min="0" placeholder="opc." style="width:6rem"></td>
      <td><button class="primario" data-accion="alta">Dar de alta</button> <button class="secundario" data-accion="borrar">Borrar</button></td></tr>`).join('')}</tbody></table></div>`;
  cont.onclick = (ev) => {
    const b = ev.target.closest('[data-accion]');
    if (!b) return;
    const fila = b.closest('tr');
    const id = fila.dataset.id;
    conAviso(async () => {
      if (b.dataset.accion === 'alta') {
        const p = fila.querySelector('.alta-partidos').value;
        await api(`/api/admin/episodios/${id}/alta`, { method: 'PATCH', admin: true,
          body: { fechaAlta: fila.querySelector('.alta-fecha').value, partidosPerdidos: p === '' ? null : Number(p) } });
        await admActivas();
        mensaje('Alta registrada. La recuperación ya cuenta en las estadísticas.');
      } else if (confirm('¿Borrar este registro? No se puede deshacer.')) {
        await api(`/api/admin/episodios/${id}`, { method: 'DELETE', admin: true });
        await admActivas();
        mensaje('Registro borrado.');
      }
      datosCambiados();
    });
  };
}

/* ---- Jugadores, clubes y catálogo ---- */
async function admJugadores() {
  $('#adm-contenido').innerHTML = `
    <form id="f-jug" class="form caja" style="margin-bottom:1rem"><h3>Nuevo jugador</h3>
      <div class="form-fila">
        <label>Nombre<input id="j-nombre" required maxlength="120"></label>
        <label>Club<select id="j-club" required>${opcionesClubes()}</select></label>
        <label>Posición<select id="j-pos"><option value="">—</option><option value="PORTERO">Portero</option><option value="DEFENSA">Defensa</option><option value="CENTROCAMPISTA">Centrocampista</option><option value="DELANTERO">Delantero</option></select></label>
        <label>Fecha de nacimiento<input id="j-nac" type="date"></label>
      </div><button class="primario" type="submit">Crear jugador</button></form>

    <form id="f-fichaje" class="form caja" style="margin-bottom:1rem"><h3>Fichaje (conserva el historial)</h3>
      <label>Buscar jugador<input id="fi-q" type="search" placeholder="Mínimo 2 letras" autocomplete="off"></label>
      <select id="fi-jug" size="3" required aria-label="Jugador"></select>
      <label>Nuevo club<select id="fi-club" required>${opcionesClubes()}</select></label>
      <button class="primario" type="submit">Cambiar de club</button></form>

    <form id="f-club" class="form caja" style="margin-bottom:1rem"><h3>Club</h3>
      <label>Club<select id="c-sel"><option value="">(nuevo club)</option>${opcionesClubes()}</select></label>
      <div class="form-fila">
        <label>Nombre<input id="c-nombre" required maxlength="120"></label>
        <label>División<select id="c-div"><option value="PRIMERA">Primera</option><option value="SEGUNDA">Segunda</option></select></label>
      </div>
      <label>Web oficial (opcional)<input id="c-web" type="url" placeholder="https://…"></label>
      <label>Página del parte médico (opcional)<input id="c-parte" type="url" placeholder="https://…"></label>
      <button class="primario" type="submit">Guardar club</button></form>

    <form id="f-tipo" class="form caja"><h3>Añadir tipo de lesión al catálogo</h3>
      <div class="form-fila">
        <label>Zona<select id="t-zona">${opcionesZonas()}</select></label>
        <label>Nombre<input id="t-nombre" required maxlength="120"></label>
        <label>Categoría<select id="t-cat"><option>MUSCULAR</option><option>LIGAMENTOSA</option><option>OSEA</option><option>TENDINOSA</option><option>ARTICULAR</option><option>CONTUSION</option><option>OTRA</option></select></label>
        <label>Gravedad típica<select id="t-grav"><option>LEVE</option><option>MODERADA</option><option>GRAVE</option></select></label>
      </div><button class="primario" type="submit">Añadir tipo</button></form>`;

  $('#f-jug').addEventListener('submit', (ev) => { ev.preventDefault(); conAviso(async () => {
    const j = await api('/api/admin/jugadores', { method: 'POST', admin: true, body: {
      nombre: $('#j-nombre').value.trim(), clubId: Number($('#j-club').value),
      posicion: vacioANull($('#j-pos').value), fechaNacimiento: vacioANull($('#j-nac').value) } });
    $('#f-jug').reset();
    mensaje(`Jugador creado: ${j.nombre} (${j.club}).`);
  }); });

  $('#fi-q').addEventListener('input', debounce(() => conAviso(async () => {
    const q = $('#fi-q').value.trim();
    const lista = q.length < 2 ? [] : await api(`/api/jugadores?q=${encodeURIComponent(q)}`);
    $('#fi-jug').innerHTML = lista.map((j) => `<option value="${j.id}">${esc(j.nombre)} — ${esc(j.club)}</option>`).join('');
    if (lista.length === 1) $('#fi-jug').value = String(lista[0].id);
  })));
  $('#f-fichaje').addEventListener('submit', (ev) => { ev.preventDefault(); conAviso(async () => {
    const j = await api(`/api/admin/jugadores/${$('#fi-jug').value}/club?clubId=${$('#fi-club').value}`, { method: 'PATCH', admin: true });
    mensaje(`${j.nombre} ahora figura en ${j.club}.`);
  }); });

  const rellenarClub = () => {
    const c = estado.clubes.find((x) => String(x.id) === $('#c-sel').value);
    $('#c-nombre').value = c ? c.nombre : '';
    $('#c-div').value = c ? c.division : 'PRIMERA';
    $('#c-web').value = c?.urlOficial || '';
    $('#c-parte').value = c?.urlParteMedico || '';
  };
  $('#c-sel').addEventListener('change', rellenarClub);
  $('#f-club').addEventListener('submit', (ev) => { ev.preventDefault(); conAviso(async () => {
    const id = $('#c-sel').value;
    const body = { nombre: $('#c-nombre').value.trim(), division: $('#c-div').value,
      urlOficial: vacioANull($('#c-web').value), urlParteMedico: vacioANull($('#c-parte').value) };
    const c = await api(id ? `/api/admin/clubes/${id}` : '/api/admin/clubes', { method: id ? 'PUT' : 'POST', admin: true, body });
    await datosCambiados({ recargarClubes: true });
    await admJugadores();
    mensaje(`Club guardado: ${c.nombre}.`);
  }); });

  $('#f-tipo').addEventListener('submit', (ev) => { ev.preventDefault(); conAviso(async () => {
    const t = await api('/api/admin/tipos', { method: 'POST', admin: true, body: {
      zona: $('#t-zona').value, nombre: $('#t-nombre').value.trim(), categoria: $('#t-cat').value, gravedad: $('#t-grav').value } });
    $('#t-nombre').value = '';
    mensaje(`Tipo añadido: ${t.nombre} (${t.zonaNombre}).`);
  }); });
}

/* ---- Revisión semanal ---- */
async function admSemanal() {
  estado.clubes = await api('/api/clubes');
  const hoy = Date.parse(hoyISO());
  const dias = (c) => (c.ultimaRevision ? Math.round((hoy - Date.parse(c.ultimaRevision)) / 86400000) : null);
  const textoDias = (c) => { const d = dias(c); return d == null ? 'nunca' : d === 0 ? 'hoy' : `hace ${plural(d, 'día', 'días')}`; };
  const ordenados = [...estado.clubes].sort((a, b) => (a.ultimaRevision || '').localeCompare(b.ultimaRevision || '') || a.nombre.localeCompare(b.nombre, 'es'));
  const alDia = estado.clubes.filter((c) => dias(c) != null && dias(c) < 7).length;
  const noticias = (c) => `https://news.google.com/search?q=${encodeURIComponent(`${c.nombre} lesión parte médico`)}&hl=es&gl=ES&ceid=ES:es`;
  const cont = $('#adm-contenido');
  cont.innerHTML = `<p><strong>${alDia} de ${estado.clubes.length}</strong> clubes revisados en los últimos 7 días. Los más antiguos aparecen primero.</p>
    <div class="tabla-wrap"><table><thead><tr><th>Club</th><th>Última revisión</th><th>Enlaces</th><th></th></tr></thead><tbody>${
      ordenados.map((c) => `<tr><td>${esc(c.nombre)}<div class="sub">${divisionTxt(c.division)}</div></td><td>${textoDias(c)}</td>
        <td><a href="${noticias(c)}" target="_blank" rel="noopener noreferrer">Noticias</a>${
          c.urlOficial ? ` · <a href="${esc(urlSegura(c.urlOficial))}" target="_blank" rel="noopener noreferrer">Web</a>` : ''}${
          c.urlParteMedico ? ` · <a href="${esc(urlSegura(c.urlParteMedico))}" target="_blank" rel="noopener noreferrer">Parte médico</a>` : ''}</td>
        <td><button class="secundario" data-revisado="${c.id}">Marcar revisado</button></td></tr>`).join('')}</tbody></table></div>`;
  cont.onclick = (ev) => {
    const b = ev.target.closest('[data-revisado]');
    if (b) conAviso(async () => { await api(`/api/admin/clubes/${b.dataset.revisado}/revisado`, { method: 'PATCH', admin: true }); await admSemanal(); });
  };
}

/* ---- Copia de seguridad ---- */
async function admCopia() {
  $('#adm-contenido').innerHTML = `<div class="caja form"><h3>Exportar todas las lesiones (CSV)</h3>
    <p class="sub">Se abre directamente en Excel. Descárgalo de vez en cuando como copia de seguridad.</p>
    <button class="primario" id="b-csv">Descargar CSV</button></div>`;
  $('#b-csv').addEventListener('click', () => conAviso(async () => {
    const csv = await api('/api/admin/export/episodios.csv', { admin: true });
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    enlace.download = `lesiones-${hoyISO()}.csv`;
    document.body.append(enlace);
    enlace.click();
    enlace.remove();
    URL.revokeObjectURL(enlace.href);
  }));
}

/* ================= Arranque ================= */

async function iniciar() {
  try {
    const [zonas, clubes] = await Promise.all([api('/api/zonas'), api('/api/clubes')]);
    estado.zonas = zonas;
    estado.clubes = clubes;
    estado.nombresZona = Object.fromEntries(zonas.map((z) => [z.codigo, z.nombre]));
  } catch (e) {
    $('main').innerHTML = `<p class="error">No se pudo cargar la aplicación: ${esc(e.message)}</p>`;
    return;
  }
  montarAhora();
  montarJugadores();
  montarComparar();
  document.querySelector('.tabs').addEventListener('click', (ev) => {
    const b = ev.target.closest('[data-tab]');
    if (b) activarTab(b.dataset.tab);
  });
  const inicial = location.hash.slice(1);
  activarTab(PESTANAS.includes(inicial) ? inicial : 'ahora');
}

iniciar();
