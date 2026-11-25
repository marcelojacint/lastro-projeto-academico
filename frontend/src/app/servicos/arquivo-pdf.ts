export const TAMANHO_MAXIMO_PDF_MB = 5;
export const TAMANHO_MAXIMO_PDF_BYTES = TAMANHO_MAXIMO_PDF_MB * 1024 * 1024;

export function erroDoArquivoPdf(arquivo: File | null): string | null {
  if (arquivo === null) {
    return 'Escolha o PDF da tabela de preços';
  }
  const pareceSerPdf =
    arquivo.type === 'application/pdf' || arquivo.name.toLowerCase().endsWith('.pdf');
  if (!pareceSerPdf) {
    return 'O arquivo deve ser um PDF';
  }
  if (arquivo.size === 0) {
    return 'O arquivo está vazio';
  }
  if (arquivo.size > TAMANHO_MAXIMO_PDF_BYTES) {
    return `O arquivo deve ter no máximo ${TAMANHO_MAXIMO_PDF_MB} MB`;
  }
  return null;
}

export function formatarTamanho(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  const formato = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });
  if (bytes < 1024 * 1024) {
    return `${formato.format(bytes / 1024)} KB`;
  }
  return `${formato.format(bytes / (1024 * 1024))} MB`;
}
