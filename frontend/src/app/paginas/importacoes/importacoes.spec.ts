import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Importacao, StatusImportacao } from '../../modelos/importacao';
import { ImportacoesPagina } from './importacoes';

describe('ImportacoesPagina', () => {
  let fixture: ComponentFixture<ImportacoesPagina>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImportacoesPagina],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(ImportacoesPagina);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function elemento(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function importacao(
    id: string,
    status: StatusImportacao,
    dados: Partial<Importacao> = {},
  ): Importacao {
    return {
      id,
      fornecedorId: 1,
      fornecedorNome: 'Natural Sul',
      nomeArquivo: `${id}.pdf`,
      tamanhoBytes: 2048,
      hashSha256: 'a'.repeat(64),
      recebidaEm: '2026-11-25T13:05:00Z',
      concluidaEm: null,
      status,
      totalItens: 4,
      itensValidos: 0,
      itensPendentes: 0,
      motivoFalha: null,
      ...dados,
    };
  }

  async function responder(importacoes: Importacao[]): Promise<void> {
    http.expectOne({ method: 'GET', url: '/api/importacoes' }).flush(importacoes);
    await fixture.whenStable();
  }

  function linhas(): HTMLElement[] {
    return Array.from(elemento().querySelectorAll<HTMLElement>('tr.mat-mdc-row'));
  }

  it('deve listar data, fornecedor, arquivo, total de itens e status de cada importacao', async () => {
    await responder([importacao('abc', 'RECEBIDA')]);

    const celulas = Array.from(linhas()[0].querySelectorAll('td')).map((celula) =>
      celula.textContent?.replace(/\s+/g, ' ').trim(),
    );
    expect(celulas.slice(0, 5)).toEqual([
      expect.stringMatching(/^25\/11\/2026 \d{2}:05$/),
      'Natural Sul',
      'abc.pdf',
      '4',
      expect.stringContaining('Recebida'),
    ]);
  });

  it('deve mostrar o status com texto e icone decorativo', async () => {
    await responder([importacao('falha', 'FALHOU')]);

    const status = linhas()[0].querySelector('app-status-importacao')!;
    expect(status.textContent).toContain('Falhou');
    expect(status.querySelector('mat-icon')?.getAttribute('aria-hidden')).toBe('true');
  });

  it('deve oferecer a acao conforme o status', async () => {
    await responder([
      importacao('recebida', 'RECEBIDA'),
      importacao('concluida', 'CONCLUIDA_COM_PENDENCIAS'),
      importacao('descartada', 'DESCARTADA'),
    ]);

    const acoes = linhas().map((linha) => {
      const link = linha.querySelector('td:last-child a');
      return link ? [link.textContent?.trim(), link.getAttribute('href')] : null;
    });
    expect(acoes).toEqual([
      ['Conferir', '/importacoes/recebida/conferencia'],
      ['Ver comparação', '/importacoes/concluida/comparacao'],
      null,
    ]);
  });

  it('deve mostrar mensagem quando nao ha importacoes', async () => {
    await responder([]);

    expect(elemento().querySelector('table')).toBeNull();
    expect(elemento().textContent).toContain('Nenhuma importação ainda.');
  });

  it('deve levar para o envio de PDF', async () => {
    await responder([]);

    const botao = elemento().querySelector('a[href="/importacoes/nova"]');
    expect(botao?.textContent).toContain('Enviar PDF');
  });
});
