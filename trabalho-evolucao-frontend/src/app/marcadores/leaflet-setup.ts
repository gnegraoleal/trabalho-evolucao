import * as L from 'leaflet';

const BASE = 'assets/leaflet/';

export const ICONE_PADRAO = L.icon({
  iconUrl: BASE + 'marker-icon.png',
  iconRetinaUrl: BASE + 'marker-icon-2x.png',
  shadowUrl: BASE + 'marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

export const TILES_URL = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
export const ATRIBUICAO =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';
