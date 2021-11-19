import { CoreModule } from '@alfresco/adf-core';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import {
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatInputModule,
    MatListModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule
} from '@angular/material';
import { RouterModule } from '@angular/router';
import { CovalentDialogsModule, CovalentMessageModule } from '@covalent/core';
import { ROUTES } from './services/modules-update.route';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatDateFormatModule } from '@lamis/web-core';
import { CustomFormsModule } from 'ng2-validation';
import { ModulesUpdateComponent } from './components/modules-update/modules-update.component';

@NgModule({
    declarations: [
        ModulesUpdateComponent
    ],
    imports: [
        CommonModule,
        MatInputModule,
        MatIconModule,
        MatCardModule,
        MatSelectModule,
        MatButtonModule,
        RouterModule.forChild(ROUTES),
        MatProgressBarModule,
        FormsModule,
        CovalentMessageModule,
        CovalentDialogsModule,
        MatTableModule,
        MatListModule,
        CoreModule,
        ReactiveFormsModule,
        MatDateFormatModule,
        CustomFormsModule
    ],
    exports: [
    ],
    entryComponents: [],
    providers: [
    ]
})
export class UpdatesModule {
}
