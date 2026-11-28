import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Fornecedor } from '../../modelos/fornecedor';
import { Importacao } from '../../modelos/importacao';
import { ImportacaoNovaPagina } from './importacao-nova';

describe('ImportacaoNovaPagina', () => {
  let fixture: ComponentFixture<ImportacaoNovaPagina>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImportacaoNovaPagina],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(ImportacaoNovaPagina);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
    document.querySelectorAll('.cdk-overlay-container').forEach((overlay) => overlay.remove());
  });

  function elemento(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function fornecedor(id: number, nome: string, ativo: boolean): Fornecedor {
    return {
      id,
      nome,
      cnpj: null,
      contato: null,
      layoutPdf: 'TABELA_REFERENCIA',
      ativo,
      criadoEm: '2026-11-24T10:00:00Z',
      atualizadoEm: '2026-11-24T10:00:00Z',
    };
  }

  async function carregarFornecedores(fornecedores: Fornecedor[]): Promise<void> {
    http.expectOne({ method: 'GET', url: '/api/fornecedores' }).flush(fornecedores);
    await fixture.whenStable();
  }

  async function abrirSelecao(): Promise<HTMLElement[]> {
    elemento().querySelector<HTMLElement>('.mat-mdc-select-trigger')!.click();
    await fixture.whenStable();
    return Array.from(document.querySelectorAll<HTMLElement>('mat-option'));
  }

  async function escolherFornecedor(nome: string): Promise<void> {
    const opcoes = await abrirSelecao();
    opcoes.find((opcao) => opcao.textContent?.trim() === nome)!.click();
    await fixture.whenStable();
  }

  async function escolherArquivo(
    nome: string,
    tamanho: number,
    tipo = 'application/pdf',
  ): Promise<File> {
    const arquivo = new File(['%PDF-1.4'], nome, { type: tipo });
    Object.defineProperty(arquivo, 'size', { value: tamanho });
    const entrada = elemento().querySelector<HTMLInputElement>('input[type="file"]')!;
    Object.defineProperty(entrada, 'files', { value: [arquivo], configurable: true });
    entrada.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    return arquivo;
  }

  async function enviar(): Promise<void> {
    elemento().querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  function erroDoArquivo(): string | undefined {
    return elemento().querySelector('#erro-arquivo')?.textContent?.replace('error', '').trim();
  }

  function envio(): TestRequest {
    return http.expectOne({ method: 'POST', url: '/api/importacoes' });
  }

  it('deve oferecer so fornecedores ativos', async () => {
    await carregarFornecedores([
      fornecedor(1, 'Natural Sul', true),
      fornecedor(2, 'Parado Ltda', false),
      fornecedor(3, 'Zeta Naturais', true),
    ]);

    const opcoes = await abrirSelecao();
    expect(opcoes.map((opcao) => opcao.textContent?.trim())).toEqual([
      'Natural Sul',
      'Zeta Naturais',
    ]);
  });

  it('deve avisar quando nao ha fornecedor ativo', async () => {
    await carregarFornecedores([fornecedor(2, 'Parado Ltda', false)]);

    expect(elemento().querySelector('form')).toBeNull();
    expect(elemento().querySelector('a[href="/fornecedores"]')).not.toBeNull();
  });

  it('deve exigir fornecedor e arquivo antes de enviar', async () => {
    await carregarFornecedores([fornecedor(1, 'Natural Sul', true)]);

    await enviar();

    expect(elemento().textContent).toContain('Escolha o fornecedor');
    expect(erroDoArquivo()).toBe('Escolha o PDF da tabela de preços');
  });

  it('deve recusar na hora arquivo que nao e PDF', async () => {
    await carregarFornecedores([fornecedor(1, 'Natural Sul', true)]);

    await escolherArquivo('tabela.xlsx', 2048, 'application/vnd.ms-excel');

    expect(erroDoArquivo()).toBe('O arquivo deve ser um PDF');
  });

  it('deve recusar na hora PDF acima de 5 MB', async () => {
    await carregarFornecedores([fornecedor(1, 'Natural Sul', true)]);

    await escolherArquivo('grande.pdf', 5 * 1024 * 1024 + 1);
    await escolherFornecedor('Natural Sul');
    await enviar();

    expect(erroDoArquivo()).toBe('O arquivo deve ter no máximo 5 MB');
  });

  it('deve enviar fornecedor e arquivo e mostrar o id da importacao', async () => {
    await carregarFornecedores([fornecedor(1, 'Natural Sul', true)]);
    await escolherFornecedor('Natural Sul');
    const arquivo = await escolherArquivo('Fornecedor_1.pdf', 2048);

    await enviar();
    const requisicao = envio();
    const corpo = requisicao.request.body as FormData;
    expect(corpo.get('fornecedorId')).toBe('1');
    expect((corpo.get('arquivo') as File).name).toBe(arquivo.name);

    const recebida: Importacao = {
      id: '4f1c2a8e-0b7d-4c55-9a52-3f6b9d1e2a10',
      fornecedorId: 1,
      fornecedorNome: 'Natural Sul',
      nomeArquivo: 'Fornecedor_1.pdf',
      tamanhoBytes: 2048,
      hashSha256: 'b'.repeat(64),
      recebidaEm: '2026-11-25T13:05:00Z',
      concluidaEm: null,
      status: 'RECEBIDA',
      totalItens: 0,
      itensValidos: 0,
      itensPendentes: 0,
      motivoFalha: null,
    };
    requisicao.flush(recebida);
    await fixture.whenStable();

    expect(elemento().querySelector('.id')?.textContent).toBe(recebida.id);
    expect(elemento().textContent).toContain('Importação recebida');
    expect(elemento().querySelector('a[href="/importacoes"]')?.textContent).toContain(
      'Ver importações',
    );
  });

  it('deve mostrar no campo o erro de arquivo devolvido pela API', async () => {
    await carregarFornecedores([fornecedor(1, 'Natural Sul', true)]);
    await escolherFornecedor('Natural Sul');
    await escolherArquivo('falso.pdf', 2048);

    await enviar();
    envio().flush(
      { status: 400, erros: [{ campo: 'arquivo', mensagem: 'O arquivo deve ser um PDF' }] },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(erroDoArquivo()).toBe('O arquivo deve ser um PDF');
    expect(elemento().querySelector('.id')).toBeNull();
  });
  it('deve mostrar itens lidos, validos e pendentes na confirmacao', async () => {
    await carregarFornecedores([fornecedor(1, 'Natural Sul', true)]);
    await escolherFornecedor('Natural Sul');
    await escolherArquivo('Fornecedor_1.pdf', 2048);

    await enviar();
    envio().flush({
      id: '4f1c2a8e-0b7d-4c55-9a52-3f6b9d1e2a10',
      fornecedorId: 1,
      fornecedorNome: 'Natural Sul',
      nomeArquivo: 'Fornecedor_1.pdf',
      tamanhoBytes: 2048,
      hashSha256: 'c'.repeat(64),
      recebidaEm: '2026-11-26T13:05:00Z',
      concluidaEm: null,
      status: 'EM_CONFERENCIA',
      totalItens: 4,
      itensValidos: 2,
      itensPendentes: 2,
      motivoFalha: null,
    });
    await fixture.whenStable();

    const dados = Array.from(elemento().querySelectorAll('dt')).map((rotulo) => [
      rotulo.textContent?.trim(),
      rotulo.nextElementSibling?.textContent?.trim(),
    ]);
    expect(dados).toContainEqual(['Itens lidos', '4']);
    expect(dados).toContainEqual(['Válidos', '2']);
    expect(dados).toContainEqual(['Pendentes', '2']);
    expect(elemento().textContent).toContain('Em conferência');
  });

  it('deve avisar que a leitura falhou e ficou registrada na lista', async () => {
    await carregarFornecedores([fornecedor(1, 'Natural Sul', true)]);
    await escolherFornecedor('Natural Sul');
    await escolherArquivo('Fornecedor_2.pdf', 2048);

    await enviar();
    envio().flush(
      {
        status: 422,
        importacaoId: '9b2d6c1e-3f4a-4b5c-8d7e-0a1b2c3d4e5f',
        motivoFalha: 'LAYOUT_NAO_RECONHECIDO',
        erros: [
          { campo: 'arquivo', mensagem: 'O PDF não segue o layout cadastrado para o fornecedor' },
        ],
      },
      { status: 422, statusText: 'Unprocessable Content' },
    );
    await fixture.whenStable();

    expect(erroDoArquivo()).toBe('O PDF não segue o layout cadastrado para o fornecedor');
    expect(elemento().textContent).toContain('A tentativa ficou registrada como falha');
    expect(elemento().querySelector('a[href="/importacoes"]')).not.toBeNull();
  });

  describe('quando o arquivo ja foi importado', () => {
    const original = '7c3e9a10-5b2d-4f6e-8a1c-2d3e4f5a6b7c';
    const mensagem = 'Este arquivo já foi importado para este fornecedor';

    async function enviarDuplicado(): Promise<void> {
      await carregarFornecedores([fornecedor(1, 'Natural Sul', true)]);
      await escolherFornecedor('Natural Sul');
      await escolherArquivo('Fornecedor_1.pdf', 2048);
      await enviar();
      envio().flush(
        {
          status: 409,
          detail: mensagem,
          importacaoId: original,
          erros: [{ campo: 'arquivo', mensagem }],
        },
        { status: 409, statusText: 'Conflict' },
      );
      await fixture.whenStable();
    }

    function detalheDaOriginal(status: Importacao['status']): void {
      http.expectOne({ method: 'GET', url: `/api/importacoes/${original}` }).flush({
        importacao: {
          id: original,
          fornecedorId: 1,
          fornecedorNome: 'Natural Sul',
          nomeArquivo: 'Fornecedor_1.pdf',
          tamanhoBytes: 2048,
          hashSha256: 'd'.repeat(64),
          recebidaEm: '2026-11-20T10:00:00Z',
          concluidaEm: status === 'EM_CONFERENCIA' ? null : '2026-11-20T11:00:00Z',
          status,
          totalItens: 4,
          itensValidos: 4,
          itensPendentes: 0,
          motivoFalha: null,
        },
        itens: [],
      });
    }

    function aviso(): HTMLElement | null {
      return elemento().querySelector('.aviso-duplicada');
    }

    it('deve avisar com link para a conferencia da importacao original', async () => {
      await enviarDuplicado();
      detalheDaOriginal('EM_CONFERENCIA');
      await fixture.whenStable();

      expect(aviso()?.textContent).toContain(mensagem);
      expect(aviso()?.querySelector('a')?.getAttribute('href')).toBe(
        `/importacoes/${original}/conferencia`,
      );
      expect(erroDoArquivo()).toBeUndefined();
      expect(elemento().textContent).not.toContain('A tentativa ficou registrada como falha');
    });

    it('deve levar a comparacao quando a importacao original ja foi concluida', async () => {
      await enviarDuplicado();
      detalheDaOriginal('CONCLUIDA');
      await fixture.whenStable();

      expect(aviso()?.querySelector('a')?.getAttribute('href')).toBe(
        `/importacoes/${original}/comparacao`,
      );
    });

    it('deve tirar o aviso ao escolher outro arquivo', async () => {
      await enviarDuplicado();
      detalheDaOriginal('EM_CONFERENCIA');
      await fixture.whenStable();

      await escolherArquivo('Fornecedor_1_v2.pdf', 2048);

      expect(aviso()).toBeNull();
    });
  });
});
