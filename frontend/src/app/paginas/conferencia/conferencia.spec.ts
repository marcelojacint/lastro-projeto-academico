import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Importacao, StatusImportacao } from '../../modelos/importacao';
import { ImportacaoDetalhe, ItemConferencia } from '../../modelos/item-importado';
import { ConferenciaPagina } from './conferencia';

describe('ConferenciaPagina', () => {
  let fixture: ComponentFixture<ConferenciaPagina>;
  let http: HttpTestingController;
  let resultadoDoDialogo: unknown;

  function importacao(status: StatusImportacao, dados: Partial<Importacao> = {}): Importacao {
    return {
      id: 'imp-1',
      fornecedorId: 1,
      fornecedorNome: 'Natural Sul',
      nomeArquivo: 'Fornecedor_1.pdf',
      tamanhoBytes: 2048,
      hashSha256: 'a'.repeat(64),
      recebidaEm: '2026-11-26T13:05:00Z',
      concluidaEm: null,
      status,
      totalItens: 3,
      itensValidos: 1,
      itensPendentes: 2,
      motivoFalha: null,
      ...dados,
    };
  }

  function item(id: number, dados: Partial<ItemConferencia>): ItemConferencia {
    return {
      id,
      numeroLinha: 2,
      linhaOriginal: '1076 ACUCAR DE COCO KG 32 32',
      referencia: '1076',
      descricao: 'ACUCAR DE COCO',
      unidade: 'KG',
      modalidadePreco: 'UNITARIO',
      quantidade: null,
      preco: 32,
      situacao: 'VALIDO',
      erros: [],
      apresentacaoId: 1,
      produtoId: 10,
      produtoNome: 'Açúcar de coco',
      ...dados,
    };
  }

  const itens: ItemConferencia[] = [
    item(1, {}),
    item(2, {
      modalidadePreco: 'FARDO',
      preco: 0,
      situacao: 'INVALIDO',
      erros: [{ codigo: 'PRECO_ZERO', mensagem: 'Preço igual a zero' }],
    }),
    item(3, {
      numeroLinha: 4,
      linhaOriginal: 'Frete por conta do cliente',
      referencia: null,
      descricao: null,
      unidade: null,
      modalidadePreco: null,
      preco: null,
      situacao: 'PENDENTE_ASSOCIACAO',
      apresentacaoId: null,
      produtoId: null,
      produtoNome: null,
    }),
  ];

  beforeEach(async () => {
    resultadoDoDialogo = undefined;
    await TestBed.configureTestingModule({
      imports: [ConferenciaPagina],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: MatDialog,
          useValue: { open: () => ({ afterClosed: () => of(resultadoDoDialogo) }) },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ConferenciaPagina);
    fixture.componentRef.setInput('id', 'imp-1');
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function elemento(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  async function carregar(detalhe: ImportacaoDetalhe): Promise<void> {
    http.expectOne({ method: 'GET', url: '/api/importacoes/imp-1' }).flush(detalhe);
    await fixture.whenStable();
  }

  function linhas(): HTMLElement[] {
    return Array.from(elemento().querySelectorAll<HTMLElement>('tr.mat-mdc-row'));
  }

  function contadores(): string[][] {
    return Array.from(elemento().querySelectorAll('.contadores > div')).map((bloco) => [
      bloco.querySelector('dt')!.textContent!.trim(),
      bloco.querySelector('dd')!.textContent!.trim(),
    ]);
  }

  it('deve mostrar cabecalho, contadores e itens com situacao, produto e erros', async () => {
    await carregar({ importacao: importacao('EM_CONFERENCIA'), itens });

    expect(elemento().textContent).toContain('Natural Sul · Fornecedor_1.pdf');
    expect(contadores()).toEqual([
      ['Itens', '3'],
      ['Válidos', '1'],
      ['Pendentes', '2'],
    ]);
    expect(linhas()).toHaveLength(3);
    expect(linhas()[0].textContent).toContain('Açúcar de coco');
    expect(linhas()[0].textContent).toContain('Válido');
    expect(linhas()[0].textContent).toContain('não informada');
    expect(linhas()[1].textContent).toContain('Preço igual a zero');
    expect(linhas()[1].textContent).toContain('Inválido');
    expect(linhas()[2].textContent).toContain('Frete por conta do cliente');
    expect(linhas()[2].textContent).toContain('Pendente de associação');
  });

  it('deve filtrar os itens pela situacao escolhida', async () => {
    await carregar({ importacao: importacao('EM_CONFERENCIA'), itens });

    const chips = Array.from(elemento().querySelectorAll<HTMLElement>('mat-chip-option'));
    expect(chips.map((chip) => chip.textContent?.trim())).toEqual([
      'Todos (3)',
      'Válidos (1)',
      'Pendentes de associação (1)',
      'Inválidos (1)',
    ]);
    chips[3].querySelector<HTMLElement>('button')!.click();
    await fixture.whenStable();

    expect(linhas()).toHaveLength(1);
    expect(linhas()[0].textContent).toContain('Preço igual a zero');
  });

  it('deve atualizar a tabela e os contadores depois da correcao', async () => {
    await carregar({ importacao: importacao('EM_CONFERENCIA'), itens });
    resultadoDoDialogo = {
      importacao: importacao('EM_CONFERENCIA', { itensValidos: 2, itensPendentes: 1 }),
      itens: [itens[0], { ...itens[1], preco: 30, situacao: 'VALIDO', erros: [] }, itens[2]],
    };

    linhas()[1].querySelector<HTMLButtonElement>('button')!.click();
    await fixture.whenStable();

    expect(contadores()[1]).toEqual(['Válidos', '2']);
    expect(linhas()[1].textContent).not.toContain('Preço igual a zero');
    expect(linhas()[1].textContent).toContain('Válido');
  });

  it('deve esconder a correcao quando a importacao nao esta em conferencia', async () => {
    await carregar({ importacao: importacao('CONCLUIDA'), itens });

    expect(elemento().querySelectorAll('td button')).toHaveLength(0);
    expect(elemento().textContent).toContain('não está em conferência');
  });
  it('deve oferecer associar so os itens que tem apresentacao', async () => {
    await carregar({ importacao: importacao('EM_CONFERENCIA'), itens });

    const rotulos = linhas().map((linha) =>
      Array.from(linha.querySelectorAll('button')).map((botao) => botao.textContent?.trim()),
    );
    expect(rotulos).toEqual([
      ['Corrigir', 'Trocar produto'],
      ['Corrigir', 'Trocar produto'],
      ['Corrigir'],
    ]);
  });

  it('deve oferecer o mesmo produto para a outra modalidade da linha', async () => {
    const pendentes = [
      item(1, { produtoId: null, produtoNome: null, situacao: 'PENDENTE_ASSOCIACAO' }),
      item(2, {
        modalidadePreco: 'FARDO',
        apresentacaoId: 2,
        produtoId: null,
        produtoNome: null,
        situacao: 'PENDENTE_ASSOCIACAO',
      }),
    ];
    await carregar({ importacao: importacao('EM_CONFERENCIA'), itens: pendentes });
    resultadoDoDialogo = {
      detalhe: {
        importacao: importacao('EM_CONFERENCIA', { itensValidos: 1, itensPendentes: 1 }),
        itens: [
          { ...pendentes[0], produtoId: 10, produtoNome: 'Açúcar de coco', situacao: 'VALIDO' },
          pendentes[1],
        ],
      },
      produto: { id: 10, nome: 'Açúcar de coco' },
    };

    linhas()[0].querySelectorAll<HTMLButtonElement>('button')[1].click();
    await fixture.whenStable();

    const oferta = elemento().querySelector('.oferta');
    expect(oferta?.textContent).toContain('A linha 2 também tem Fardo sem produto.');
    expect(oferta?.textContent).toContain('Associar a Açúcar de coco?');

    Array.from(oferta!.querySelectorAll<HTMLButtonElement>('button'))
      .find((botao) => botao.textContent?.trim() === 'Associar também')!
      .click();
    const envio = http.expectOne({ method: 'POST', url: '/api/importacoes/imp-1/itens/2/vinculo' });
    expect(envio.request.body).toEqual({ produtoId: 10 });
    envio.flush({
      importacao: importacao('EM_CONFERENCIA', { itensValidos: 2, itensPendentes: 0 }),
      itens: [
        { ...pendentes[0], produtoId: 10, produtoNome: 'Açúcar de coco', situacao: 'VALIDO' },
        { ...pendentes[1], produtoId: 10, produtoNome: 'Açúcar de coco', situacao: 'VALIDO' },
      ],
    });
    await fixture.whenStable();

    expect(elemento().querySelector('.oferta')).toBeNull();
    expect(linhas()[1].textContent).toContain('Açúcar de coco');
  });

  it('deve esconder a oferta quando o usuario recusa', async () => {
    const pendentes = [
      item(1, { produtoId: null, produtoNome: null, situacao: 'PENDENTE_ASSOCIACAO' }),
      item(2, { modalidadePreco: 'FARDO', apresentacaoId: 2, produtoId: null, produtoNome: null }),
    ];
    await carregar({ importacao: importacao('EM_CONFERENCIA'), itens: pendentes });
    resultadoDoDialogo = {
      detalhe: {
        importacao: importacao('EM_CONFERENCIA'),
        itens: [{ ...pendentes[0], produtoId: 10, produtoNome: 'Açúcar de coco' }, pendentes[1]],
      },
      produto: { id: 10, nome: 'Açúcar de coco' },
    };

    linhas()[0].querySelectorAll<HTMLButtonElement>('button')[1].click();
    await fixture.whenStable();
    Array.from(elemento().querySelectorAll<HTMLButtonElement>('.oferta button'))
      .find((botao) => botao.textContent?.trim() === 'Agora não')!
      .click();
    await fixture.whenStable();

    expect(elemento().querySelector('.oferta')).toBeNull();
  });
});
