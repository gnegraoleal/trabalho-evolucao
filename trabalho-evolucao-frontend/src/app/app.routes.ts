import { Routes } from '@angular/router';

export const routes: Routes = [{ path: 'mapa', loadComponent: () => import('./map.component').then(m => m.MapComponent) }];

