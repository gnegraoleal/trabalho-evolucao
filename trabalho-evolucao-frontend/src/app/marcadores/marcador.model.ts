export interface Marcador {
  id: number;
  titulo: string;
  descricao: string | null;
  latitude: number;
  longitude: number;
}

export interface NovoMarcador {
  titulo: string;
  descricao: string | null;
  latitude: number;
  longitude: number;
}

/** Visão pública: sem id e sem dados do dono. */
export interface MarcadorPublico {
  titulo: string;
  descricao: string | null;
  latitude: number;
  longitude: number;
}

export interface LinkPublico {
  nomeUsuario: string;
  token: string;
}

/** Formato único consumido pelo MapView, tanto no mapa do titular quanto no público (RNF-09). */
export interface PontoMapa {
  chave: string | number;
  titulo: string;
  descricao: string | null;
  latitude: number;
  longitude: number;
}

export const paraPonto = (m: Marcador): PontoMapa => ({ chave: m.id, ...m });

export const paraPontoPublico = (m: MarcadorPublico, indice: number): PontoMapa => ({ chave: indice, ...m });
