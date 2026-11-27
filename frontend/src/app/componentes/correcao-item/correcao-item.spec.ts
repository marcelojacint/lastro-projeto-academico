import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ImportacaoDetalhe, ItemConferencia } from '../../modelos/item-importado';
import { CorrecaoItemComponente, DadosCorrecaoItem } from './correcao-item';

describe('CorrecaoItemComponente', () => {
  let fixture: ComponentFixture<CorrecaoItemComponente>;
  let http: HttpTestingController;
  let fechado: ImportacaoDetalhe | undefined;

  const item: ItemConferencia = {
    id: 42,
    numeroLinha: 3,
    linhaOriginal: '1077 DOCANTE STEVIA COLOR ANDINA 40G UN 78 0',
    referencia: '1077',
    descricao: 'DOCANTE STEVIA COLOR ANDINA 40G',
    unidade: 'UN',
    modalidadePreco: 'FARDO',
    quantidade: null,
    preco: 0,
    situacao: 'INVALIDO',
    erros: [{ codigo: 'PRECO_ZERO', mensagem: 'Preço igual a zero' }],
    apresentacaoId: 7,
    produtoId: null,
    produtoNome: null,
  };

  beforeEach(async () => {
    fechado = undefined;
    const dados: DadosCorrecaoItem = { importacaoId: 'imp-1', item };
    await TestBed.configureTestingModule({
      imports: [CorrecaoItemComponente],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MAT_DIALOG_DATA, useValue: dados },
        {
          provide: MatDialogRef,
          useValue: { close: (resultado: ImportacaoDetalhe) => (fechado = resultado) },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(CorrecaoItemComponente);
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function elemento(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function campo(nome: string): HTMLInputElement {
    return elemento().querySelector<HTMLInputElement>(`[formControlName="${nome}"]`)!;
  }

  async function preencher(nome: string, valor: string): Promise<void> {
    campo(nome).value = valor;
    campo(nome).dispatchEvent(new Event('input'));
    campo(nome).dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  }

  async function salvar(): Promise<void> {
    elemento().querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('deve abrir com os dados do item, a linha do PDF e a modalidade', () => {
    expect(campo('referencia').value).toBe('1077');
    expect(campo('descricao').value).toBe('DOCANTE STEVIA COLOR ANDINA 40G');
    expect(campo('unidade').value).toBe('UN');
    expect(campo('preco').value).toBe('R$ 0,00');
    expect(elemento().textContent).toContain('1077 DOCANTE STEVIA COLOR ANDINA 40G UN 78 0');
    expect(elemento().textContent).toContain('Fardo');
  });

  it('deve exigir descricao e preco maior que zero sem chamar a API', async () => {
    await preencher('descricao', '  ');
    await salvar();

    const mensagens = Array.from(elemento().querySelectorAll('mat-error')).map((erro) =>
      erro.textContent?.trim(),
    );
    expect(mensagens).toEqual(['Campo obrigatório', 'Informe um preço maior que zero']);
  });

  it('deve enviar a correcao com o preco convertido e fechar com o detalhe atualizado', async () => {
    await preencher('preco', 'R$ 74,50');
    await preencher('unidade', ' ');
    await salvar();

    const requisicao = http.expectOne({ method: 'PUT', url: '/api/importacoes/imp-1/itens/42' });
    expect(requisicao.request.body).toEqual({
      referencia: '1077',
      descricao: 'DOCANTE STEVIA COLOR ANDINA 40G',
      unidade: null,
      preco: 74.5,
    });
    const detalhe = { importacao: {}, itens: [] } as unknown as ImportacaoDetalhe;
    requisicao.flush(detalhe);
    await fixture.whenStable();

    expect(fechado).toEqual(detalhe);
  });

  it('deve mostrar no campo o erro devolvido pela API', async () => {
    await salvar();
    await preencher('preco', '10');
    await salvar();

    http.expectOne({ method: 'PUT', url: '/api/importacoes/imp-1/itens/42' }).flush(
      {
        status: 400,
        erros: [{ campo: 'descricao', mensagem: 'Descrição deve ter no máximo 300 caracteres' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(elemento().textContent).toContain('Descrição deve ter no máximo 300 caracteres');
    expect(fechado).toBeUndefined();
  });
});
