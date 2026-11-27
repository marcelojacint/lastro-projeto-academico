import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ImportacaoDetalhe, ItemConferencia } from '../../modelos/item-importado';
import { Produto } from '../../modelos/produto';
import {
  AssociacaoItemComponente,
  DadosAssociacaoItem,
  ESPERA_BUSCA_PRODUTO_MS,
  ResultadoAssociacao,
} from './associacao-item';

describe('AssociacaoItemComponente', () => {
  let fixture: ComponentFixture<AssociacaoItemComponente>;
  let http: HttpTestingController;
  let fechado: ResultadoAssociacao | undefined;

  const item: ItemConferencia = {
    id: 42,
    numeroLinha: 3,
    linhaOriginal: '1077 DOCANTE STEVIA COLOR ANDINA 40G UN 78 78',
    referencia: '1077',
    descricao: 'DOCANTE STEVIA COLOR ANDINA 40G',
    unidade: 'UN',
    modalidadePreco: 'UNITARIO',
    quantidade: null,
    preco: 78,
    situacao: 'PENDENTE_ASSOCIACAO',
    erros: [],
    apresentacaoId: 7,
    produtoId: null,
    produtoNome: null,
  };

  function produto(id: number, nome: string): Produto {
    return {
      id,
      nome,
      codigo: null,
      categoria: null,
      unidade: null,
      precoCompra: null,
      precoVenda: 10,
      estoqueAtual: 0,
      estoqueMinimo: 0,
      status: 'ATIVO',
      criadoEm: '2026-11-27T10:00:00Z',
      atualizadoEm: '2026-11-27T10:00:00Z',
    } as Produto;
  }

  beforeEach(async () => {
    fechado = undefined;
    const dados: DadosAssociacaoItem = { importacaoId: 'imp-1', item };
    await TestBed.configureTestingModule({
      imports: [AssociacaoItemComponente],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MAT_DIALOG_DATA, useValue: dados },
        {
          provide: MatDialogRef,
          useValue: { close: (resultado: ResultadoAssociacao) => (fechado = resultado) },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(AssociacaoItemComponente);
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => {
    http.verify();
    document.querySelectorAll('.cdk-overlay-container').forEach((overlay) => overlay.remove());
  });

  function elemento(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function botaoAssociar(): HTMLButtonElement {
    return elemento().querySelector<HTMLButtonElement>('button[type="submit"]')!;
  }

  async function buscar(texto: string): Promise<void> {
    const entrada = elemento().querySelector<HTMLInputElement>('input')!;
    entrada.focus();
    entrada.value = texto;
    entrada.dispatchEvent(new Event('input'));
    await new Promise((pronto) => setTimeout(pronto, ESPERA_BUSCA_PRODUTO_MS + 50));
    await fixture.whenStable();
  }

  it('deve buscar so produtos ativos pelo nome digitado', async () => {
    await buscar('stevia');

    const requisicao = http.expectOne(
      (pedido) => pedido.url === '/api/produtos' && pedido.params.get('nome') === 'stevia',
    );
    expect(requisicao.request.params.get('status')).toBe('ATIVO');
    requisicao.flush({ itens: [], pagina: 0, tamanho: 20, totalItens: 0, totalPaginas: 0 });
  });

  it('deve exigir escolher um produto da lista antes de associar', async () => {
    expect(botaoAssociar().disabled).toBe(true);
    expect(elemento().textContent).toContain('DOCANTE STEVIA COLOR ANDINA 40G');
    expect(elemento().textContent).toContain('Unitário');
  });

  it('deve associar o produto escolhido e fechar com o detalhe e o produto', async () => {
    await buscar('stevia');
    http
      .expectOne((pedido) => pedido.url === '/api/produtos')
      .flush({
        itens: [produto(10, 'Stevia Andina 40g'), produto(11, 'Stevia em pó')],
        pagina: 0,
        tamanho: 20,
        totalItens: 2,
        totalPaginas: 1,
      });
    await fixture.whenStable();

    const opcoes = Array.from(document.querySelectorAll<HTMLElement>('mat-option'));
    expect(opcoes.map((opcao) => opcao.textContent?.trim())).toEqual([
      'Stevia Andina 40g',
      'Stevia em pó',
    ]);
    opcoes[0].click();
    await fixture.whenStable();
    expect(botaoAssociar().disabled).toBe(false);

    elemento().querySelector('form')!.dispatchEvent(new Event('submit'));
    const envio = http.expectOne({
      method: 'POST',
      url: '/api/importacoes/imp-1/itens/42/vinculo',
    });
    expect(envio.request.body).toEqual({ produtoId: 10 });
    const detalhe = { importacao: {}, itens: [] } as unknown as ImportacaoDetalhe;
    envio.flush(detalhe);
    await fixture.whenStable();

    expect(fechado).toEqual({ detalhe, produto: { id: 10, nome: 'Stevia Andina 40g' } });
  });
});
