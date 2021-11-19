import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from './material.module';
import { RouterModule } from '@angular/router';
import { ROUTES } from './services/database.sync.route';
import { DatabaseSyncComponent } from './components/database-sync/database.sync.component';
import { CoreModule } from '@alfresco/adf-core';

@NgModule({
    imports: [
        CommonModule,
        MaterialModule,
        RouterModule.forChild(ROUTES),
        CoreModule
    ],
    declarations: [
        DatabaseSyncComponent
    ]
})
export class DatabaseSyncModule {

}
