import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ROUTES } from './services/modules.update.route';
import { CoreModule } from '@alfresco/adf-core';
import { MaterialModule } from './material.module';
import { CovalentCommonModule, CovalentFileModule } from '@covalent/core';
import { LamisSharedModule } from '@lamis/web-core';

@NgModule({
    imports: [
        CommonModule,
        CoreModule,
        MaterialModule
    ],
})
export class UpdatesManagementModule {

}
