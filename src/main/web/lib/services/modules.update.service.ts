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
export class ModulesUpdateService {
    public resourceUrl = '';

    constructor(protected http: HttpClient, @Inject(SERVER_API_URL_CONFIG) private serverUrl: ServerApiUrlConfig) {
        this.resourceUrl = serverUrl.SERVER_API_URL + '/api/modules-update';
    }

    installUpdates() {
        return this.http.get<ModuleUpdate[]>(`${this.resourceUrl}/install-updates`);
    }

    availableUpdates() {
        return this.http.get<ModuleUpdate[]>(`${this.resourceUrl}/available-modules`);
    }

    checkForUpdates() {
        return this.http.get(`${this.resourceUrl}/check-for-updates`);
    }

    lastHeartbeat() {
        return this.http.get(`${this.resourceUrl}/last-heartbeat`).pipe(
            map(res => {
                res = res != null ? moment(res, moment.ISO_8601) : null
                return res;
            })
        );
    }
}
