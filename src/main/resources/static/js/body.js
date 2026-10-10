// body.js — mapa corporal en SVG, generado por código.
//
// Es una función pura (sin DOM): recibe qué zonas resaltar y devuelve un string con el <svg>.
// Cada zona lleva data-zona (el mismo código que la tabla zona_corporal) y data-lado, de modo
// que la base de datos y el dibujo comparten identificador: ese es el "puente" entre ambos.
//
// Convención de lados: DERECHO / IZQUIERDO son siempre los del JUGADOR. En la vista frontal el
// lado derecho del jugador queda a la izquierda del dibujo; en la vista de espalda, al revés.

const ANCHO = 200;
const ALTO = 372;

// Formas: 'e' = elipse {cx,cy,rx,ry}, 'r' = rectángulo redondeado {x,y,w,h,r}.
// Las zonas "lado" se definen para la mitad IZQUIERDA del dibujo y se espejan para la otra mitad.
const FRENTE = {
  etiqueta: 'FRENTE',
  izquierdaDibujo: 'DERECHO',
  centro: [
    ['cabeza', { k: 'e', cx: 100, cy: 24, rx: 17, ry: 21 }],
    ['cuello', { k: 'r', x: 91, y: 46, w: 18, h: 12, r: 4 }],
    ['pecho', { k: 'r', x: 80, y: 60, w: 40, h: 36, r: 6 }],
    ['abdomen', { k: 'r', x: 82, y: 98, w: 36, h: 34, r: 5 }],
    ['pubis', { k: 'r', x: 93, y: 134, w: 14, h: 14, r: 3 }],
  ],
  lado: [
    ['hombro', { k: 'r', x: 58, y: 60, w: 21, h: 24, r: 8 }],
    ['brazo', { k: 'r', x: 58, y: 86, w: 15, h: 38, r: 5 }],
    ['codo', { k: 'e', cx: 65, cy: 131, rx: 9, ry: 7 }],
    ['antebrazo', { k: 'r', x: 57, y: 140, w: 14, h: 36, r: 5 }],
    ['mano', { k: 'e', cx: 63, cy: 187, rx: 8, ry: 10 }],
    ['ingle', { k: 'r', x: 79, y: 134, w: 13, h: 22, r: 4 }],
    ['cuadriceps', { k: 'r', x: 78, y: 158, w: 21, h: 66, r: 7 }],
    ['rodilla', { k: 'e', cx: 88, cy: 233, rx: 11, ry: 9 }],
    ['tibia', { k: 'r', x: 79, y: 244, w: 18, h: 64, r: 6 }],
    ['tobillo', { k: 'e', cx: 88, cy: 316, rx: 8, ry: 6 }],
    ['pie', { k: 'e', cx: 86, cy: 338, rx: 11, ry: 15 }],
  ],
};

const ESPALDA = {
  etiqueta: 'ESPALDA',
  izquierdaDibujo: 'IZQUIERDO',
  centro: [
    ['cabeza', { k: 'e', cx: 100, cy: 24, rx: 17, ry: 21 }],
    ['cuello', { k: 'r', x: 91, y: 46, w: 18, h: 12, r: 4 }],
    ['espalda-alta', { k: 'r', x: 80, y: 60, w: 40, h: 40, r: 6 }],
    ['lumbar', { k: 'r', x: 82, y: 102, w: 36, h: 30, r: 5 }],
  ],
  lado: [
    ['hombro', { k: 'r', x: 58, y: 60, w: 21, h: 24, r: 8 }],
    ['brazo', { k: 'r', x: 58, y: 86, w: 15, h: 38, r: 5 }],
    ['codo', { k: 'e', cx: 65, cy: 131, rx: 9, ry: 7 }],
    ['antebrazo', { k: 'r', x: 57, y: 140, w: 14, h: 36, r: 5 }],
    ['mano', { k: 'e', cx: 63, cy: 187, rx: 8, ry: 10 }],
    ['gluteo', { k: 'r', x: 80, y: 134, w: 19, h: 28, r: 8 }],
    ['isquiotibial', { k: 'r', x: 78, y: 164, w: 21, h: 60, r: 7 }],
    ['rodilla', { k: 'e', cx: 88, cy: 233, rx: 11, ry: 9 }],
    ['gemelo', { k: 'r', x: 79, y: 244, w: 19, h: 48, r: 7 }],
    ['aquiles', { k: 'r', x: 83, y: 294, w: 11, h: 14, r: 3 }],
    ['tobillo', { k: 'e', cx: 88, cy: 316, rx: 8, ry: 6 }],
    ['pie', { k: 'e', cx: 86, cy: 338, rx: 11, ry: 15 }],
  ],
};

const VISTAS = { FRENTE, ESPALDA };

const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

function espejo(f) {
  return f.k === 'e' ? { ...f, cx: ANCHO - f.cx } : { ...f, x: ANCHO - f.x - f.w };
}

function forma(f) {
  return f.k === 'e'
    ? `<ellipse cx="${f.cx}" cy="${f.cy}" rx="${f.rx}" ry="${f.ry}"/>`
    : `<rect x="${f.x}" y="${f.y}" width="${f.w}" height="${f.h}" rx="${f.r}"/>`;
}

const textoLado = (lado) => ({ DERECHO: ' (derecho)', IZQUIERDO: ' (izquierdo)' }[lado] || '');
const opuesto = (lado) => (lado === 'DERECHO' ? 'IZQUIERDO' : 'DERECHO');

/**
 * @param {'FRENTE'|'ESPALDA'} vista
 * @param {{nombres?: Record<string,string>, resaltes?: Array<{zona:string, lado:string, n?:number, clase?:'activa'|'pasada'}>}} opciones
 *   resaltes: zonas a colorear. Si varios coinciden en la misma zona y lado, se suman y 'activa' gana a 'pasada'.
 */
export function svgMapa(vista, { nombres = {}, resaltes = [] } = {}) {
  const v = VISTAS[vista];
  if (!v) throw new Error(`Vista desconocida: ${vista}`);

  const grupo = (zona, lado, f) => {
    const coinciden = resaltes.filter((r) => r.zona === zona && r.lado === lado);
    const n = coinciden.reduce((s, r) => s + (r.n ?? 1), 0);
    const clase = coinciden.some((r) => r.clase === 'activa') ? 'activa'
      : coinciden.length ? 'pasada' : '';
    const nombre = (nombres[zona] || zona) + textoLado(lado);
    const titulo = n ? `${nombre}: ${n}` : nombre;
    return `<g class="z${clase ? ` hit ${clase}` : ''}" data-zona="${esc(zona)}" data-lado="${lado}"`
      + ` style="--n:${Math.min(n, 5)}" tabindex="0" role="button" aria-label="${esc(titulo)}">`
      + `<title>${esc(titulo)}</title>${forma(f)}</g>`;
  };

  const piezas = [];
  for (const [zona, f] of v.centro) piezas.push(grupo(zona, 'NO_APLICA', f));
  for (const [zona, f] of v.lado) {
    piezas.push(grupo(zona, v.izquierdaDibujo, f));
    piezas.push(grupo(zona, opuesto(v.izquierdaDibujo), espejo(f)));
  }

  const marcaIzq = v.izquierdaDibujo === 'DERECHO' ? 'D' : 'I';
  const marcaDer = marcaIzq === 'D' ? 'I' : 'D';
  return `<svg class="mapa" viewBox="0 0 ${ANCHO} ${ALTO}" role="group" aria-label="Mapa corporal, vista ${v.etiqueta.toLowerCase()}">`
    + `<text class="mapa-lado" x="10" y="14">${marcaIzq}</text><text class="mapa-lado" x="190" y="14" text-anchor="end">${marcaDer}</text>`
    + piezas.join('')
    + `<text class="mapa-vista" x="100" y="368" text-anchor="middle">${v.etiqueta}</text></svg>`;
}
