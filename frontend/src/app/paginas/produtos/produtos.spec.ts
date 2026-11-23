import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { provideRouter } from '@angular/router';
import { Pagina } from '../../modelos/pagina';
import { Produto } from '../../modelos/produto';
import { PaginadorIntl } from '../../servicos/paginador-intl';
import { ESPERA_DIGITACAO_MS, ProdutosPagina } from './produtos';

describe('ProdutosPagina', () => {
  let fixture: ComponentFixture<ProdutosPagina>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProdutosPagina],
      providers: [
        provideRouter([{ path: '**', children: [] }]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatPaginatorIntl, useClass: PaginadorIntl },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ProdutosPagina);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function elemento(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function texto(seletor: string): string {
    return elemento().querySelector(seletor)?.textContent?.trim() ?? '';
  }

  function consulta(parametros: Record<string, string>): TestRequest {
    return http.expectOne(
      (requisicao) =>
        requisicao.method === 'GET' &&
        requisicao.url === '/api/produtos' &&
        requisicao.params.keys().length === Object.keys(parametros).length &&
        Object.entries(parametros).every(([nome, valor]) => requisicao.params.get(nome) === valor),
    );
  }

  async function responder(requisicao: TestRequest, resposta: Pagina<Produto>): Promise<void> {
    requisicao.flush(resposta);
    await fixture.whenStable();
  }

  function digitar(campo: string, valor: string): void {
    const entrada = elemento().querySelector<HTMLInputElement>(`[formControlName="${campo}"]`)!;
    entrada.value = valor;
    entrada.dispatchEvent(new Event('input'));
  }

  async function esperarDigitacao(): Promise<void> {
    await new Promise((resolver) => setTimeout(resolver, ESPERA_DIGITACAO_MS + 50));
  }

  function produto(id: number, dados: Partial<Produto> = {}): Produto {
    return {
      id,
      codigo: null,
      nome: `Produto ${id}`,
      categoria: null,
      unidade: null,
      precoCompra: null,
      precoVenda: 10,
      estoqueAtual: 0,
      estoqueMinimo: 0,
      status: 'ATIVO',
      criadoEm: '2026-11-23T10:00:00Z',
      atualizadoEm: '2026-11-23T10:00:00Z',
      ...dados,
    };
  }

  function pagina(itens: Produto[], numero = 0, totalItens = itens.length): Pagina<Produto> {
    return {
      itens,
      pagina: numero,
      tamanho: 20,
      totalItens,
      totalPaginas: Math.ceil(totalItens / 20),
    };
  }

  it('deve listar status, estoque atual e minimo com link de edicao em cada linha', async () => {
    await responder(
      consulta({ page: '0' }),
      pagina([
        produto(7, { nome: 'Granola', status: 'ATIVO', estoqueAtual: 12, estoqueMinimo: 3 }),
        produto(9, { nome: 'Aveia', status: 'INATIVO', estoqueAtual: 0.5, estoqueMinimo: 2 }),
      ]),
    );

    const linhas = Array.from(elemento().querySelectorAll('tr[mat-row]')).map((linha) =>
      Array.from(linha.querySelectorAll('td')).map((celula) => celula.textContent?.trim()),
    );
    expect(linhas.map((linha) => [linha[1], linha[3], linha[4], linha[5]])).toEqual([
      ['Granola', 'Ativo', '12', '3'],
      ['Aveia', 'Inativo', '0.5', '2'],
    ]);
    const edicoes = Array.from(elemento().querySelectorAll('tr[mat-row] a')).map((link) => [
      link.getAttribute('href'),
      link.getAttribute('aria-label'),
    ]);
    expect(edicoes).toEqual([
      ['/produtos/7', 'Editar Granola'],
      ['/produtos/9', 'Editar Aveia'],
    ]);
  });

  it('deve ter botao Novo apontando para o cadastro', async () => {
    await responder(consulta({ page: '0' }), pagina([]));

    const novo = elemento().querySelector('.cabecalho a')!;
    expect(novo.textContent?.trim()).toContain('Novo produto');
    expect(novo.getAttribute('href')).toBe('/produtos/novo');
  });

  it('deve mostrar mensagem quando nao houver produto cadastrado', async () => {
    await responder(consulta({ page: '0' }), pagina([]));

    expect(elemento().querySelector('table')).toBeNull();
    expect(texto('.lista-vazia')).toBe('Nenhum produto cadastrado.');
  });

  it('deve buscar com os filtros informados e avisar quando nada for encontrado', async () => {
    await responder(consulta({ page: '0' }), pagina([produto(1)]));

    digitar('nome', ' gran ');
    digitar('categoria', 'Cereais');
    await esperarDigitacao();

    await responder(consulta({ page: '0', nome: 'gran', categoria: 'Cereais' }), pagina([]));
    expect(texto('.lista-vazia')).toBe('Nenhum produto encontrado com os filtros informados.');
  });

  it('deve paginar de vinte em vinte e pedir a proxima pagina', async () => {
    const primeiros = Array.from({ length: 20 }, (_, indice) => produto(indice + 1));
    await responder(consulta({ page: '0' }), pagina(primeiros, 0, 21));

    expect(elemento().querySelectorAll('tr[mat-row]')).toHaveLength(20);
    expect(texto('.mat-mdc-paginator-range-label')).toBe('1 – 20 de 21');

    elemento().querySelector<HTMLButtonElement>('.mat-mdc-paginator-navigation-next')!.click();

    await responder(consulta({ page: '1' }), pagina([produto(21)], 1, 21));
    expect(elemento().querySelectorAll('tr[mat-row]')).toHaveLength(1);
    expect(texto('.mat-mdc-paginator-range-label')).toBe('21 – 21 de 21');
  });
});
