/** Funções puras de coordenadas e URLs: testáveis sem navegador e sem rede (RNF-10). */

export const CENTRO_PADRAO: [number, number] = [-12.9714, -38.5014]; // Salvador (RNF-05)
export const ZOOM_PADRAO = 12;

export function coordenadasValidas(lat: number | null | undefined, lng: number | null | undefined): boolean {
  return (
    typeof lat === 'number' && typeof lng === 'number' &&
    Number.isFinite(lat) && Number.isFinite(lng) &&
    lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180 // RD-05: limites inclusivos
  );
}

/** Retorna a mensagem de erro, ou null se o título é válido (RD-04: 1 a 255 caracteres). */
export function validarTitulo(titulo: string | null | undefined): string | null {
  const t = (titulo ?? '').trim();
  if (t.length === 0) return 'Informe um título.';
  if (t.length > 255) return 'O título pode ter no máximo 255 caracteres.';
  return null;
}

const f = (n: number) => n.toFixed(6);

export function urlOpenStreetMap(lat: number, lng: number, zoom = 15): string {
  return `https://www.openstreetmap.org/?mlat=${f(lat)}&mlon=${f(lng)}#map=${zoom}/${f(lat)}/${f(lng)}`;
}

/** RF-09 / RD-10: divulga só a posição do ponto; nunca o token do mapa. */
export function linksCompartilhamentoPonto(lat: number, lng: number, titulo: string) {
  const osm = urlOpenStreetMap(lat, lng);
  const alvo = encodeURIComponent(osm);
  return {
    osm,
    facebook: `https://www.facebook.com/sharer.php?u=${alvo}`,
    x: `https://twitter.com/intent/tweet?url=${alvo}&text=${encodeURIComponent(titulo)}`,
  };
}

/** RF-10: a URL usa a origem da instância que respondeu, não um host fixo. */
export function urlMapaPublico(origem: string, nomeUsuario: string, token: string): string {
  return `${origem}/mapa/${encodeURIComponent(nomeUsuario)}/${encodeURIComponent(token)}`;
}

/** Posição do navegador, ou null se negada, indisponível ou demorada (RNF-05). */
export function posicaoAtual(timeoutMs = 5000): Promise<[number, number] | null> {
  return new Promise((resolve) => {
    if (typeof navigator === 'undefined' || !navigator.geolocation) return resolve(null);
    navigator.geolocation.getCurrentPosition(
      (p) => resolve([p.coords.latitude, p.coords.longitude]),
      () => resolve(null),
      { timeout: timeoutMs, maximumAge: 60_000 }
    );
  });
}
