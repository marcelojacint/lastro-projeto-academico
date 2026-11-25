import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Fornecedor } from '../../modelos/fornecedor';
import { FornecedoresPagina } from './fornecedores';

describe('FornecedoresPagina', () => {
  let fixture: ComponentFixture<FornecedoresPagina>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FornecedoresPagina],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(FornecedoresPagina);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function elemento(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function fornecedor(id: number, dados: Partial<Fornecedor> = {}): Fornecedor {
    return {
      id,
      nome: `Fornecedor ${id}`,
      cnpj: null,
      contato: null,
      layoutPdf: 'TABELA_REFERENCIA',
      ativo: true,
      criadoEm: '2026-11-24T10:00:00Z',
      atualizadoEm: '2026-11-24T10:00:00Z',
      ...dados,
    };
  }

  function listagem(): TestRequest {
    return http.expectOne({ method: 'GET', url: '/api/fornecedores' });
  }

  async function responder(
    requisicao: TestRequest,
    corpo: Fornecedor | Fornecedor[],
  ): Promise<void> {
    requisicao.flush(corpo);
    await fixture.whenStable();
  }

  function preencher(campo: string, valor: string): void {
    const entrada = elemento().querySelector<HTMLInputElement>(`[formControlName="${campo}"]`)!;
    entrada.value = valor;
    entrada.dispatchEvent(new Event('input'));
  }

  async function escolherLayout(rotulo: string): Promise<void> {
    elemento().querySelector<HTMLElement>('mat-select .mat-mdc-select-trigger')!.click();
    await fixture.whenStable();
    const opcoes = Array.from(document.querySelectorAll<HTMLElement>('mat-option'));
    opcoes.find((opcao) => opcao.textContent?.trim() === rotulo)!.click();
    await fixture.whenStable();
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

  function linhas(): (string | undefined)[][] {
    return Array.from(elemento().querySelectorAll('tr[mat-row]')).map((linha) =>
      Array.from(linha.querySelectorAll('td'))
        .slice(0, 4)
        .map((celula) => celula.textContent?.trim()),
    );
  }

  it('deve listar nome, cnpj formatado, layout e status', async () => {
    await responder(listagem(), [
      fornecedor(1, { nome: 'Natural Sul', cnpj: '12345678000190' }),
      fornecedor(2, { nome: 'Zeta', layoutPdf: 'ATACADO_VAREJO', ativo: false }),
    ]);

    expect(linhas()).toEqual([
      ['Natural Sul', '12.345.678/0001-90', 'Tabela com referência', 'Ativo'],
      ['Zeta', '—', 'Atacado e varejo', 'Inativo'],
    ]);
  });

  it('deve mostrar mensagem quando nao houver fornecedor cadastrado', async () => {
    await responder(listagem(), []);

    expect(elemento().querySelector('table')).toBeNull();
    expect(elemento().querySelector('.lista-vazia')?.textContent?.trim()).toBe(
      'Nenhum fornecedor cadastrado.',
    );
  });

  it('deve exigir nome e layout sem chamar a API', async () => {
    await responder(listagem(), []);

    await enviar();

    expect(mensagensDeErro()).toEqual(['Campo obrigatório', 'Campo obrigatório']);
    http.expectNone({ method: 'POST', url: '/api/fornecedores' });
  });

  it('deve recusar cnpj sem quatorze digitos', async () => {
    await responder(listagem(), []);
    preencher('nome', 'Natural Sul');
    preencher('cnpj', '12.345.678/0001-9');
    await escolherLayout('Tabela com referência');

    await enviar();

    expect(mensagensDeErro()).toEqual(['CNPJ deve ter 14 dígitos']);
    http.expectNone({ method: 'POST', url: '/api/fornecedores' });
  });

  it('deve cadastrar ativo com cnpj so com digitos e recarregar a lista', async () => {
    await responder(listagem(), []);
    expect(elemento().querySelector('mat-slide-toggle')).toBeNull();
    preencher('nome', ' Natural Sul ');
    preencher('cnpj', '12.345.678/0001-90');
    preencher('contato', '   ');
    await escolherLayout('Atacado e varejo');

    await enviar();

    const cadastro = http.expectOne({ method: 'POST', url: '/api/fornecedores' });
    expect(cadastro.request.body).toEqual({
      nome: 'Natural Sul',
      cnpj: '12345678000190',
      contato: null,
      layoutPdf: 'ATACADO_VAREJO',
      ativo: true,
    });
    await responder(cadastro, fornecedor(1));
    await responder(listagem(), [fornecedor(1, { nome: 'Natural Sul' })]);
    expect(linhas().map((linha) => linha[0])).toEqual(['Natural Sul']);
  });

  it('deve editar e inativar o fornecedor escolhido na lista', async () => {
    await responder(listagem(), [
      fornecedor(7, { nome: 'Natural Sul', cnpj: '12345678000190', contato: 'Maria' }),
    ]);

    elemento().querySelector<HTMLButtonElement>('tr[mat-row] button')!.click();
    await fixture.whenStable();
    expect(elemento().querySelector('h2')?.textContent?.trim()).toBe('Editar fornecedor');
    expect(elemento().querySelector<HTMLInputElement>('[formControlName="cnpj"]')!.value).toBe(
      '12.345.678/0001-90',
    );
    elemento().querySelector<HTMLButtonElement>('mat-slide-toggle button')!.click();
    await fixture.whenStable();

    await enviar();

    const edicao = http.expectOne({ method: 'PUT', url: '/api/fornecedores/7' });
    expect(edicao.request.body).toEqual({
      nome: 'Natural Sul',
      cnpj: '12345678000190',
      contato: 'Maria',
      layoutPdf: 'TABELA_REFERENCIA',
      ativo: false,
    });
    await responder(edicao, fornecedor(7, { ativo: false }));
    await responder(listagem(), [fornecedor(7, { nome: 'Natural Sul', ativo: false })]);
    expect(elemento().querySelector('h2')?.textContent?.trim()).toBe('Novo fornecedor');
    expect(linhas()[0][3]).toBe('Inativo');
  });

  it('deve mostrar no campo o conflito de cnpj devolvido pela API', async () => {
    await responder(listagem(), []);
    preencher('nome', 'Natural Sul');
    preencher('cnpj', '12345678000190');
    await escolherLayout('Tabela com referência');

    await enviar();

    http.expectOne({ method: 'POST', url: '/api/fornecedores' }).flush(
      {
        status: 409,
        detail: 'Já existe um fornecedor com este CNPJ',
        erros: [{ campo: 'cnpj', mensagem: 'Já existe um fornecedor com este CNPJ' }],
      },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    expect(mensagensDeErro()).toEqual(['Já existe um fornecedor com este CNPJ']);
  });
});
