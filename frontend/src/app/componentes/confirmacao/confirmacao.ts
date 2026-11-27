import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface DadosConfirmacao {
  titulo: string;
  mensagens: string[];
  confirmar: string;
}

@Component({
  selector: 'app-confirmacao',
  imports: [MatButtonModule, MatDialogModule],
  templateUrl: './confirmacao.html',
  styleUrl: './confirmacao.scss',
})
export class ConfirmacaoComponente {
  protected readonly dados = inject<DadosConfirmacao>(MAT_DIALOG_DATA);
}
