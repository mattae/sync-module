import { Routes } from '@angular/router';
import { DatabaseSyncComponent } from '../components/database-sync/database.sync.component';

export const ROUTES: Routes = [
    {
        path: '',
        data: {
            title: 'Database Sync',
            breadcrumb: 'DATABASE SYNC'
        },
        children: [
            {
                path: '',
                component: DatabaseSyncComponent,
                data: {
                    authorities: ['ROLE_ADMIN'],
                    title: 'Database Sync',
                    breadcrumb: 'DATABASE SYNC'
                },
                //canActivate: [UserRouteAccessService]
            }
        ]
    }
];
