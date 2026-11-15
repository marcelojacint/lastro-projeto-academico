import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes)],
    }).compileComponents();
  });

  it('deve exibir o menu com as quatro secoes do sistema', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;
    const rotulos = Array.from(elemento.querySelectorAll('nav a [matListItemTitle]')).map(
      (rotulo) => rotulo.textContent?.trim(),
    );
    expect(rotulos).toEqual(['Produtos', 'Fornecedores', 'Importações', 'Histórico']);
  });

  it('deve apontar cada item do menu para a rota da secao', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const elemento = fixture.nativeElement as HTMLElement;
    const destinos = Array.from(elemento.querySelectorAll('nav a')).map((link) =>
      link.getAttribute('href'),
    );
    expect(destinos).toEqual(['/produtos', '/fornecedores', '/importacoes', '/historico']);
  });
});
