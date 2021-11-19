import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoreModule } from '@alfresco/adf-core';
import { MaterialModule } from './material.module';
import { ModuleListComponent } from './components/updates-management/module.list.component';
import { RouterModule } from '@angular/router';
import { ModuleUpdateComponent } from './components/updates-management/module.update.component';
import { CovalentCommonModule, CovalentFileModule } from '@covalent/core';
import { ModuleResolve, ROUTES } from './services/modules.update.route';
import { LamisSharedModule } from '@lamis/web-core';

@NgModule({
    imports: [
        CommonModule,
        RouterModule.forChild(ROUTES),
        CoreModule,
        MaterialModule,
        CovalentCommonModule,
        CovalentFileModule,
        LamisSharedModule
    ],
    declarations: [
        ModuleListComponent,
        ModuleUpdateComponent
    ],
    providers: [
        ModuleResolve
    ]
})
export class ModulesUpdateModule {
}
