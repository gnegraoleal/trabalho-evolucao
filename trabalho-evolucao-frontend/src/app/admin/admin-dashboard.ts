import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService } from './admin.service';
import { Dashboard } from './admin.models';
import { erroHttp } from '../auth.models';
@Component({ imports:[RouterLink,DatePipe], templateUrl:'./admin-dashboard.html' })
export class AdminDashboardComponent {
  private readonly service = inject(AdminService);
  readonly dados = signal<Dashboard | null>(null); readonly erro = signal('');
  constructor() { this.carregar(); }
  carregar() { this.erro.set(''); this.service.dashboard().subscribe({next:d=>this.dados.set(d),error:e=>this.erro.set(erroHttp(e))}); }
}
