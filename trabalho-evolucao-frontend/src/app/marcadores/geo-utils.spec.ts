import {
  coordenadasValidas, linksCompartilhamentoPonto, urlMapaPublico, urlOpenStreetMap, validarTitulo,
} from './geo-utils';

describe('geo-utils', () => {
  describe('coordenadasValidas (RD-05)', () => {
    it('aceita zero e os extremos inclusivos', () => {
      expect(coordenadasValidas(0, 0)).toBe(true);
      expect(coordenadasValidas(90, 180)).toBe(true);
      expect(coordenadasValidas(-90, -180)).toBe(true);
    });

    it('recusa valores fora de cada intervalo', () => {
      expect(coordenadasValidas(90.0001, 0)).toBe(false);
      expect(coordenadasValidas(-90.0001, 0)).toBe(false);
      expect(coordenadasValidas(0, 180.0001)).toBe(false);
      expect(coordenadasValidas(0, -180.0001)).toBe(false);
    });

    it('recusa nulo, indefinido e NaN', () => {
      expect(coordenadasValidas(null, 0)).toBe(false);
      expect(coordenadasValidas(0, undefined)).toBe(false);
      expect(coordenadasValidas(NaN, 0)).toBe(false);
    });
  });

  describe('validarTitulo (RD-04)', () => {
    it('recusa vazio e só espaços', () => {
      expect(validarTitulo('')).not.toBeNull();
      expect(validarTitulo('   ')).not.toBeNull();
      expect(validarTitulo(null)).not.toBeNull();
    });

    it('aceita 1 e 255 caracteres; recusa 256', () => {
      expect(validarTitulo('a')).toBeNull();
      expect(validarTitulo('a'.repeat(255))).toBeNull();
      expect(validarTitulo('a'.repeat(256))).not.toBeNull();
    });
  });

  describe('compartilhamento de ponto (RF-09)', () => {
    it('monta a URL do OpenStreetMap com a posição', () => {
      expect(urlOpenStreetMap(-12.9714, -38.5014)).toBe(
        'https://www.openstreetmap.org/?mlat=-12.971400&mlon=-38.501400#map=15/-12.971400/-38.501400'
      );
    });

    it('escapa título com caracteres especiais', () => {
      const l = linksCompartilhamentoPonto(1, 2, 'Café & Cia #1 <b>');
      expect(l.x).toContain(encodeURIComponent('Café & Cia #1 <b>'));
      expect(l.x).not.toContain('<b>');
      expect(l.facebook).toContain(encodeURIComponent(l.osm));
    });
  });

  describe('urlMapaPublico (RF-10)', () => {
    it('usa a origem informada e codifica nome e token', () => {
      expect(urlMapaPublico('http://127.0.0.1:4200', 'Ana', 'ab-_9')).toBe('http://127.0.0.1:4200/mapa/Ana/ab-_9');
      expect(urlMapaPublico('https://x.dev', 'a b', 'x/y')).toBe('https://x.dev/mapa/a%20b/x%2Fy');
    });
  });
});
