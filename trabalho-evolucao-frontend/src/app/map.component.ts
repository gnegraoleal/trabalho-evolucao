import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, output, signal } from '@angular/core';
import * as L from 'leaflet';

@Component({
  selector: 'app-map',
  template: `<div #canvas aria-label="Mapa interativo" style="height: 65vh"></div>
    @if (failed()) { <p role="status">Não foi possível carregar parte do mapa.</p> }`
})
export class MapComponent implements AfterViewInit, OnDestroy {
  @ViewChild('canvas', {static: true}) private canvas!: ElementRef<HTMLElement>;
  readonly mapReady = output<L.Map>();
  protected readonly failed = signal(false);
  private map?: L.Map;

  ngAfterViewInit() {
    this.map = L.map(this.canvas.nativeElement).setView([0, 0], 2);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).on('tileerror', () => this.failed.set(true)).addTo(this.map);
    this.mapReady.emit(this.map);
  }

  ngOnDestroy() {
    this.map?.remove();
  }
}
