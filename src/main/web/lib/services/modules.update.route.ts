import { ModuleUpdate } from '../model/module.model';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot, Routes } from '@angular/router';
import { ModuleManagementService } from './module.management.service';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ModuleListComponent } from '../components/updates-management/module.list.component';
import { UserRouteAccessService } from '@lamis/web-core';
import { ModuleUpdateComponent } from '../components/updates-management/module.update.component';

@Injectable()
export class ModuleResolve implements Resolve<ModuleUpdate> {
    constructor(private service: ModuleManagementService) {
    }

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<ModuleUpdate> {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.findById(id).pipe(
                filter((response: HttpResponse<ModuleUpdate>) => response.ok),
                map((patient: HttpResponse<ModuleUpdate>) => patient.body)
            );
        }
        return of(<ModuleUpdate>{});
    }
}

export const ROUTES: Routes = [
    {
        path: '',
        data: {
            authorities: ['ROLE_MODULE_MANAGEMENT'],
            title: 'Modules Update List',
            breadcrumb: 'MODULES UPDATE LIST'
        },
        canActivate: [UserRouteAccessService],
        children: [
            {
                path: '',
                component: ModuleListComponent,
                data: {
                    title: 'Modules Update List',
                    breadcrumb: 'MODULES UPDATE LIST'
                }
            },
            {
                path: 'new',
                component: ModuleUpdateComponent,
                data: {
                    title: 'Upload Module Update',
                    breadcrumb: 'UPLOAD MODULE UPDATE'
                }
            },
            {
                path: ':id/update',
                component: ModuleUpdateComponent,
                resolve: {
                    module: ModuleResolve
                },
                data: {
                    title: 'Upload Module Update',
                    breadcrumb: 'UPLOAD MODULE UPDATE'
                }
            }
        ]
    }
];
