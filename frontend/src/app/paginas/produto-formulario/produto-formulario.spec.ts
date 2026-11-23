import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ProdutoFormularioPagina } from './produto-formulario';

describe('ProdutoFormularioPagina', () => {
  let fixture: ComponentFixture<ProdutoFormularioPagina>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProdutoFormularioPagina],
      providers: [
        provideRouter([{ path: '**', children: [] }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ProdutoFormularioPagina);
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function elemento(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function preencher(seletor: string, valor: string): void {
    const campo = elemento().querySelector<HTMLInputElement>(`[formControlName="${seletor}"]`)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
  }

  async function enviar(): Promise<void> {
    elemento().querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  function mensagensDeErro(): string[] {
    return Array.from(elemento().querySelectorAll('mat-error')).map(
      (erro) => erro.textContent?.trim() ?? '',
    );
  }

  it('deve exigir nome e preco de venda sem chamar a API', async () => {
    await enviar();

    expect(mensagensDeErro()).toEqual(['Campo obrigatório', 'Campo obrigatório']);
    http.expectNone('/api/produtos');
  });

  it('deve recusar nome preenchido so com espacos', async () => {
    preencher('nome', '   ');
    preencher('precoVenda', '10,00');

    await enviar();

    expect(mensagensDeErro()).toEqual(['Campo obrigatório']);
    http.expectNone('/api/produtos');
  });

  it('deve recusar preco e estoque negativos', async () => {
    preencher('nome', 'Granola');
    preencher('precoVenda', '-5,00');
    preencher('estoqueAtual', '-1');

    await enviar();

    expect(mensagensDeErro()).toEqual([
      'O valor não pode ser negativo',
      'O valor não pode ser negativo',
    ]);
    http.expectNone('/api/produtos');
  });

  it('deve enviar o produto com o preco convertido do formato brasileiro', async () => {
    preencher('nome', ' Granola ');
    preencher('precoCompra', '1.234,5');
    preencher('precoVenda', 'R$ 1.500,90');

    await enviar();

    const requisicao = http.expectOne({ method: 'POST', url: '/api/produtos' });
    expect(requisicao.request.body).toEqual({
      codigo: null,
      nome: 'Granola',
      categoria: null,
      unidade: null,
      precoCompra: 1234.5,
      precoVenda: 1500.9,
      estoqueAtual: 0,
      estoqueMinimo: 0,
      status: 'ATIVO',
    });
    requisicao.flush({});
  });
});
