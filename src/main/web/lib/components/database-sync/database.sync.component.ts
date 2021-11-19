import { Component, OnDestroy, OnInit } from '@angular/core';
import { of, Subscription } from 'rxjs';
import { RxStompService } from '@stomp/ng2-stompjs';
import { Message } from '@stomp/stompjs';
import {
    CardViewBoolItemModel,
    CardViewDatetimeItemModel,
    CardViewItem,
    NotificationService
} from '@alfresco/adf-core';
import * as moment_ from 'moment';
import { SyncService } from '../../services/sync.service';
import { catchError, map } from "rxjs/operators";
import { AppLoaderService } from '@lamis/web-core';

const moment = moment_;

@Component({
    selector: 'database-sync',
    templateUrl: './database.sync.component.html'
})
export class DatabaseSyncComponent implements OnInit, OnDestroy {
    syncing = false;
    downloading = false;
    serverContacted = false;
    tables = '';
    statusSubscription: Subscription;
    serverSubscription: Subscription;
    tableSubscription: Subscription;
    syncSubscription: Subscription;
    properties: CardViewItem[] = [];
    facility: any;

    constructor(private stompService: RxStompService, private syncService: SyncService, private notification: NotificationService,
                private loaderService: AppLoaderService) {
    }

    ngOnInit(): void {
        this.syncService.getActiveFacility().subscribe(res => this.facility = res);
        this.properties = [];
        this.statusSubscription = this.stompService.watch('/topic/sync/server-status').subscribe((msg: Message) => {
            this.properties = this.properties.filter(i => i.key !== 'server');
            this.insertAt(this.properties, 0, (new CardViewDatetimeItemModel({
                key: 'server',
                value: msg.body && moment(msg.body, moment.ISO_8601) || null,
                label: 'Last contact with Server',
                format: 'DD MMM, YYYY HH:MM'
            })));
        });

        this.statusSubscription = this.stompService.watch('/topic/sync/sync-status').subscribe((msg: Message) => {
            this.properties = this.properties.filter(i => i.key !== 'sync');
            this.insertAt(this.properties, 1, (new CardViewDatetimeItemModel({
                key: 'sync',
                value: msg.body && moment(msg.body, moment.ISO_8601) || null,
                label: 'Last Sync to Server',
                format: 'DD MMM, YYYY HH:MM'
            })));
        });

        this.syncSubscription = this.stompService.watch('/topic/sync/upload-status/completed').subscribe((msg: Message) => {
            this.syncing = msg.body === 'false';
            this.properties = this.properties.filter(i => i.key !== 'status');
            this.properties.push(new CardViewBoolItemModel({
                key: 'status',
                value: msg.body === 'true',
                label: 'Upload Completed',
            }));
        });

        this.tableSubscription = this.stompService.watch('/topic/sync/table-status').subscribe((msg: Message) => {
            this.tables = msg.body
        });

        this.serverSubscription = this.stompService.watch('/topic/sync/server').subscribe((msg: Message) => {
            this.serverContacted = !!msg.body
        });

        this.syncService.init().subscribe()
    }

    ngOnDestroy(): void {
        this.statusSubscription.unsubscribe();
        this.tableSubscription.unsubscribe();
        this.syncSubscription.unsubscribe();
        this.syncService.destroy().subscribe()
        this.serverSubscription.unsubscribe();
    }

    sync() {
        this.syncing = true;
        this.syncService.sync().subscribe()
    }

    downloadCparp() {
        this.downloading = true;
        this.loaderService.open('Downloading records, please wait...')
        this.syncService.downloadCparp(this.facility.id).pipe(
            map(res => {
                this.loaderService.close();
                this.downloading = false;
                this.notification.showInfo("Available records for facility successfully downloaded")
            }),
            catchError((err) => {
                this.loaderService.close();
                this.notification.showError("There was an error downloading records; please try again");
                this.downloading = false;
                return of();
            })).subscribe();

    }

    downloadMobileRecords() {
        this.downloading = true;
        this.loaderService.open('Downloading records, please wait...')
        this.syncService.downloadMobileRecords().pipe(
            map(res => {
                this.loaderService.close();
                this.downloading = false;
                this.notification.showInfo("Available records for facility successfully downloaded")
            }),
            catchError((err) => {
                this.loaderService.close();
                this.notification.showError("There was an error downloading records; please try again");
                this.downloading = false;
                return of();
            })).subscribe();

    }

    downloadBiometrics() {
        this.loaderService.open('Downloading biometric data from server. Please wait....')
        this.syncService.downloadBiometrics().subscribe()

        let id = setInterval(() => {
            this.syncService.biometricDownloadCompleted().subscribe(res => {
                if (res) {
                    this.loaderService.close();
                    clearInterval(id);
                }
            })
        }, 10000);
    }

    uploadBiometrics() {
        this.loaderService.open('Uploading biometric data to the server. Please wait....')
        this.syncService.uploadBiometrics().subscribe()

        let id = setInterval(() => {
            this.syncService.biometricUploadCompleted().subscribe(res => {
                if (res) {
                    this.loaderService.close();
                    clearInterval(id);
                }
            })
        }, 10000);
    }

    previousState() {
        window.history.back();
    }

    insertAt(array, index, ...elementsArray) {
        array.splice(index, 0, ...elementsArray);
    }
}
