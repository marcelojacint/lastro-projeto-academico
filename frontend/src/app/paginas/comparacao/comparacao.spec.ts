import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localePt from '@angular/common/locales/pt';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ComparacaoImportacao, ItemAusente, ItemComparado } from '../../modelos/comparacao';
import { ComparacaoPagina } from './comparacao';

describe('ComparacaoPagina', () => {
  let fixture: ComponentFixture<ComparacaoPagina>;
  let http: HttpTestingController;

  function item(itemId: number, dados: Partial<ItemComparado>): ItemComparado {
    return {
      itemId,
      numeroLinha: 2,
      referencia: '1076',
      descricao: 'CHA DE HIBISCO',
      unidade: 'KG',
      modalidadePreco: 'UNITARIO',
      produtoId: 10,
      produtoNome: 'Hibisco',
      precoVenda: 50,
      precoAnterior: 33,
      precoAtual: 35,
      diferenca: 2,
      variacaoPercentual: 6.06,
      classificacao: 'AUMENTOU',
      motivosPendencia: [],
      ...dados,
    };
  }

  const itens: ItemComparado[] = [
    item(1, {}),
    item(2, {
      produtoNome: 'Alecrim',
      precoVenda: 35,
      precoAnterior: 15,
      precoAtual: 14,
      diferenca: -1,
      variacaoPercentual: -6.67,
      classificacao: 'DIMINUIU',
    }),
    item(3, {
      produtoNome: 'Camomila',
      precoVenda: 65,
      precoAnterior: 38,
      precoAtual: 38,
      diferenca: 0,
      variacaoPercentual: 0,
      classificacao: 'SEM_ALTERACAO',
    }),
    item(4, {
      produtoNome: 'Produto novo',
      precoVenda: null,
      precoAnterior: null,
      precoAtual: 20,
      diferenca: null,
      variacaoPercentual: null,
      classificacao: 'NOVO',
    }),
    item(5, {
      produtoId: null,
      produtoNome: null,
      precoVenda: null,
      precoAnterior: 32,
      precoAtual: 0,
      diferenca: null,
      variacaoPercentual: null,
      classificacao: 'PENDENTE',
      motivosPendencia: ['Preço igual a zero'],
    }),
  ];

  function comparacao(
    dados: Partial<ComparacaoImportacao> = {},
    ausentes?: ItemAusente[],
  ): ComparacaoImportacao {
    return {
      importacao: {
        id: 'imp-1',
        fornecedorId: 1,
        fornecedorNome: 'Natural Sul',
        nomeArquivo: 'Fornecedor_1_v2.pdf',
        tamanhoBytes: 2048,
        hashSha256: 'a'.repeat(64),
        recebidaEm: '2026-11-26T13:05:00Z',
        concluidaEm: '2026-11-26T13:20:00Z',
        status: 'CONCLUIDA_COM_PENDENCIAS',
        totalItens: 5,
        itensValidos: 4,
        itensPendentes: 1,
        motivoFalha: null,
      },
      totais: { AUMENTOU: 1, DIMINUIU: 1, SEM_ALTERACAO: 1, NOVO: 1, PENDENTE: 1 },
      itens,
      ...(ausentes ? { ausentes } : {}),
      ...dados,
    };
  }

  beforeAll(() => registerLocaleData(localePt));

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ComparacaoPagina],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(ComparacaoPagina);
    fixture.componentRef.setInput('id', 'imp-1');
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function elemento(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  async function responder(
    resposta: ComparacaoImportacao,
    classificacao: string | null = null,
  ): Promise<void> {
    const requisicao = http.expectOne(
      (pedido) => pedido.method === 'GET' && pedido.url === '/api/importacoes/imp-1/comparacao',
    );
    expect(requisicao.request.params.get('classificacao')).toBe(classificacao);
    requisicao.flush(resposta);
    await fixture.whenStable();
  }

  function celulas(): string[][] {
    return Array.from(elemento().querySelectorAll('tr.mat-mdc-row')).map((linha) =>
      Array.from(linha.querySelectorAll('td')).map((celula) => {
        const copia = celula.cloneNode(true) as HTMLElement;
        copia.querySelectorAll('mat-icon').forEach((icone) => icone.remove());
        return (copia.textContent ?? '').replace(/\s+/g, ' ').trim();
      }),
    );
  }

  it('deve mostrar anterior, atual, diferenca e variacao com sinal e valores em reais', async () => {
    await responder(comparacao());

    const [aumentou, diminuiu, semAlteracao, novo, pendente] = celulas();
    expect(aumentou).toEqual([
      'Hibisco 1076 · CHA DE HIBISCO · Unitário',
      'R$ 33,00',
      'R$ 35,00',
      '+R$ 2,00',
      '+6,06%',
      'Aumentou',
      'R$ 50,00',
    ]);
    expect(diminuiu.slice(3, 6)).toEqual(['−R$ 1,00', '−6,67%', 'Diminuiu']);
    expect(semAlteracao.slice(3, 6)).toEqual(['R$ 0,00', '0,00%', 'Sem alteração']);
    expect(novo.slice(1, 7)).toEqual([
      '—Sem preço anterior',
      'R$ 20,00',
      '—Sem diferença',
      '—Sem variação',
      'Novo',
      '—Sem preço de venda',
    ]);
    expect(pendente[0]).toContain('Sem produto');
    expect(pendente.slice(4, 6)).toEqual(['Preço igual a zero', 'Pendente']);
  });

  it('deve deixar os icones da classificacao fora da leitura de tela', async () => {
    await responder(comparacao());

    const icones = Array.from(elemento().querySelectorAll('app-rotulo-classificacao mat-icon'));
    expect(icones.map((icone) => icone.textContent?.trim())).toEqual([
      'arrow_upward',
      'arrow_downward',
      'equal',
      'star',
      'warning',
    ]);
    expect(icones.every((icone) => icone.getAttribute('aria-hidden') === 'true')).toBe(true);
  });

  it('deve filtrar pela classificacao pedindo a API e manter os totais', async () => {
    await responder(comparacao());

    const chips = Array.from(elemento().querySelectorAll<HTMLElement>('mat-chip-option'));
    expect(chips.map((chip) => chip.textContent?.trim())).toEqual([
      'Todos (5)',
      'Aumentou (1)',
      'Diminuiu (1)',
      'Sem alteração (1)',
      'Novo (1)',
      'Pendente (1)',
    ]);
    chips[5].querySelector<HTMLElement>('button')!.click();
    await fixture.whenStable();
    await responder(comparacao({ itens: [itens[4]] }), 'PENDENTE');

    expect(celulas()).toHaveLength(1);
    expect(celulas()[0][5]).toBe('Pendente');
    expect(elemento().querySelector('mat-chip-option')?.textContent?.trim()).toBe('Todos (5)');
  });

  it('deve mostrar a secao de itens ausentes so quando houver', async () => {
    await responder(
      comparacao({}, [
        {
          apresentacaoId: 7,
          referencia: '1077',
          descricao: 'DOCANTE STEVIA COLOR ANDINA 40G',
          unidade: 'UN',
          modalidadePreco: 'UNITARIO',
          produtoNome: 'Stevia',
          ultimoPrecoValido: 74,
        },
      ]),
    );

    const secao = elemento().querySelector('section.ausentes');
    expect(secao?.querySelector('h2')?.textContent).toBe('Itens ausentes');
    expect(secao?.textContent?.replace(/\s+/g, ' ')).toContain('Último preço válido: R$ 74,00');
    expect(secao?.textContent).toContain('Stevia');
  });

  it('deve esconder a secao de itens ausentes quando nao ha nenhum', async () => {
    await responder(comparacao({}, []));

    expect(elemento().querySelector('section.ausentes')).toBeNull();
  });

  it('deve avisar quando a comparacao nao pode ser carregada', async () => {
    http
      .expectOne('/api/importacoes/imp-1/comparacao')
      .flush(null, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(elemento().textContent).toContain('Comparação indisponível para esta importação.');
  });
});
