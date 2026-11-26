import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MensagemErro } from '../../modelos/item-importado';
import { ErrosItemComponente } from './erros-item';

describe('ErrosItemComponente', () => {
  let fixture: ComponentFixture<ErrosItemComponente>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ErrosItemComponente] }).compileComponents();
    fixture = TestBed.createComponent(ErrosItemComponente);
  });

  function elemento(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  async function mostrar(erros: MensagemErro[]): Promise<void> {
    fixture.componentRef.setInput('erros', erros);
    await fixture.whenStable();
  }

  it('deve mostrar todos os erros do item juntos e na ordem', async () => {
    await mostrar([
      { codigo: 'DESCRICAO_VAZIA', mensagem: 'Descrição vazia' },
      { codigo: 'PRECO_ZERO', mensagem: 'Preço igual a zero' },
      { codigo: 'UNIDADE_AUSENTE', mensagem: 'Unidade não informada' },
    ]);

    const itens = Array.from(elemento().querySelectorAll('li')).map((item) =>
      item.querySelector('span')?.textContent?.trim(),
    );
    expect(itens).toEqual(['Descrição vazia', 'Preço igual a zero', 'Unidade não informada']);
  });

  it('deve mostrar o icone de alerta como decoracao ao lado do texto', async () => {
    await mostrar([{ codigo: 'PRECO_AUSENTE', mensagem: 'Preço não informado' }]);

    const icone = elemento().querySelector('li mat-icon');
    expect(icone?.textContent?.trim()).toBe('warning');
    expect(icone?.getAttribute('aria-hidden')).toBe('true');
    expect(elemento().querySelector('li')?.textContent).toContain('Preço não informado');
  });

  it('deve indicar quando o item nao tem erros', async () => {
    await mostrar([]);

    expect(elemento().querySelector('ul')).toBeNull();
    expect(elemento().querySelector('.oculto')?.textContent).toBe('Sem erros');
  });
});
