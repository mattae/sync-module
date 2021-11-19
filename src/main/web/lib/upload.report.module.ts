import {NgModule} from '@angular/core';
import {UploadReportComponent} from './components/upload-report/upload.report.component';
import {MaterialModule} from './material.module';
import {RouterModule, Routes} from '@angular/router';
import {CommonModule} from '@angular/common';
import {FormsModule} from "@angular/forms";

export const ROUTES: Routes = [
    {
        path: '',
        data: {
            title: 'Database Upload Report',
            breadcrumb: 'DATABASE UPLOAD REPORT'
        },
        children: [
            {
                path: '',
                component: UploadReportComponent,
                data: {
                    authorities: ['ROLE_ADMIN'],
                    title: 'Database Upload Report',
                    breadcrumb: 'DATABASE UPLOAD REPORT'
                },
                //canActivate: [UserRouteAccessService]
            }
        ]
    }
];

@NgModule({
    declarations: [
        UploadReportComponent
    ],
    imports: [
        CommonModule,
        RouterModule.forChild(ROUTES),
        MaterialModule,
        FormsModule
    ]
})
export class UploadReportModule {

}
