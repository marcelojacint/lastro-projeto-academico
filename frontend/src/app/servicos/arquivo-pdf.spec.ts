import { TAMANHO_MAXIMO_PDF_BYTES, erroDoArquivoPdf, formatarTamanho } from './arquivo-pdf';

function arquivo(nome: string, tamanho: number, tipo = 'application/pdf'): File {
  const arquivoFalso = new File(['%PDF-'], nome, { type: tipo });
  Object.defineProperty(arquivoFalso, 'size', { value: tamanho });
  return arquivoFalso;
}

describe('erroDoArquivoPdf', () => {
  it('deve exigir um arquivo', () => {
    expect(erroDoArquivoPdf(null)).toBe('Escolha o PDF da tabela de preços');
  });

  it('deve aceitar PDF de ate 5 MB', () => {
    expect(erroDoArquivoPdf(arquivo('tabela.pdf', TAMANHO_MAXIMO_PDF_BYTES))).toBeNull();
  });

  it('deve aceitar arquivo com extensao pdf mesmo sem tipo informado', () => {
    expect(erroDoArquivoPdf(arquivo('TABELA.PDF', 2048, ''))).toBeNull();
  });

  it('deve recusar arquivo que nao parece PDF', () => {
    expect(erroDoArquivoPdf(arquivo('tabela.xlsx', 2048, 'application/vnd.ms-excel'))).toBe(
      'O arquivo deve ser um PDF',
    );
  });

  it('deve recusar PDF acima de 5 MB', () => {
    expect(erroDoArquivoPdf(arquivo('grande.pdf', TAMANHO_MAXIMO_PDF_BYTES + 1))).toBe(
      'O arquivo deve ter no máximo 5 MB',
    );
  });

  it('deve recusar arquivo vazio', () => {
    expect(erroDoArquivoPdf(arquivo('vazio.pdf', 0))).toBe('O arquivo está vazio');
  });
});

describe('formatarTamanho', () => {
  it('deve mostrar bytes, KB e MB em pt-BR', () => {
    expect(formatarTamanho(512)).toBe('512 B');
    expect(formatarTamanho(1536)).toBe('1,5 KB');
    expect(formatarTamanho(3 * 1024 * 1024)).toBe('3 MB');
  });
});
