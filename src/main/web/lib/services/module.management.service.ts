import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { SERVER_API_URL_CONFIG, ServerApiUrlConfig } from '@lamis/web-core';
import { ModuleUpdate } from '../model/module.model';
import { map } from 'rxjs/operators';
import * as moment_ from 'moment';

const moment = moment_;

@Injectable({
    providedIn: 'root'
})
export class ModuleManagementService {
    public resourceUrl = '';

    constructor(protected http: HttpClient, @Inject(SERVER_API_URL_CONFIG) private serverUrl: ServerApiUrlConfig) {
        this.resourceUrl = serverUrl.SERVER_API_URL + '/api/modules-update';
    }

    findById(id: number) {
        return this.http.get<ModuleUpdate>(`${this.resourceUrl}/module/${id}`, {observe: 'response'}).pipe(
            map(res => {
                res.body.buildTime = res.body.buildTime != null ? moment(res.body.buildTime) : null
                return res;
            })
        )
    }

    availableModules() {
        return this.http.get<ModuleUpdate[]>(`${this.resourceUrl}/available-modules`).pipe(
            map(res => {
                res.forEach(m => {
                    m.buildTime = m.buildTime != null ? moment(m.buildTime) : null
                })
                return res;
            })
        )
    }

    uploadModule(form) {
        return this.http.post<ModuleUpdate>(`${this.resourceUrl}/upload`, form, {observe: 'response'});
    }

    install(module) {
        return this.http.post<ModuleUpdate>(`${this.resourceUrl}/save-update`, module, {observe: 'response'})
    }

    uninstall(module) {
        return this.http.post<ModuleUpdate>(`${this.resourceUrl}/uninstall`, module, {observe: 'response'})
    }
}
