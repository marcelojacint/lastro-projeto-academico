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
});
