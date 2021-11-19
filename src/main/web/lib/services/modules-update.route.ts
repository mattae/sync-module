import { Routes } from '@angular/router';
import { ModulesUpdateComponent } from '../components/modules-update/modules-update.component';

export const ROUTES: Routes = [
    {
        path: '',
        data: {
            title: 'Module Updates',
            breadcrumb: 'MODULE UPDATES'
        },
        children: [
            {
                path: '',
                component: ModulesUpdateComponent,
                data: {
                    authorities: ['ROLE_ADMIN'],
                    title: 'Module Updates',
                    breadcrumb: 'MODULE UPDATES'
                },
                //canActivate: [UserRouteAccessService]
            }
        ]
    }
];

