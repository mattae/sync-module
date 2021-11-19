import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { SERVER_API_URL_CONFIG, ServerApiUrlConfig } from '@lamis/web-core';

@Injectable({
    providedIn: 'root'
})
export class SyncService {
    public resourceUrl = '';

    constructor(protected http: HttpClient, @Inject(SERVER_API_URL_CONFIG) private serverUrl: ServerApiUrlConfig) {
        this.resourceUrl = serverUrl.SERVER_API_URL + '/api/database-sync';
    }

    sync() {
        return this.http.get(`${this.resourceUrl}/sync`);
    }

    downloadBiometrics() {
        return this.http.get(`${this.resourceUrl}/download-biometrics`)
    }

    uploadBiometrics() {
        return this.http.get(`${this.resourceUrl}/upload-biometrics`)
    }

    biometricDownloadCompleted() {
        return this.http.get<boolean>(`${this.resourceUrl}/biometric-download-completed`)
    }

    biometricUploadCompleted() {
        return this.http.get<boolean>(`${this.resourceUrl}/biometric-upload-completed`)
    }

    downloadCparp(facilityId: number) {
        return this.http.get(`${this.resourceUrl}/cparp/update/${facilityId}`);
    }

    downloadMobileRecords() {
        return this.http.get(`${this.resourceUrl}/download-records`);
    }

    init() {
        return this.http.get(`${this.resourceUrl}/init`);
    }

    states() {
        return this.http.get<any[]>(`${this.resourceUrl}/states`);
    }

    getActiveFacility() {
        return this.http.get<any>('/api/facilities/active')
    }

    destroy() {
        return this.http.get(`${this.resourceUrl}/destroy`);
    }

    uploadReport(stateId: any, format: number) {
        return this.http.get(`${this.resourceUrl}/upload-report?stateId=${stateId}&format=${format}`, { responseType: 'blob'})
    }
}
