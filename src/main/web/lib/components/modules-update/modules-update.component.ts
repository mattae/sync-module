import { Component, OnDestroy, OnInit } from '@angular/core';
import { CardViewDatetimeItemModel, CardViewItem, CardViewTextItemModel } from '@alfresco/adf-core';
import { ModuleUpdate } from '../../model/module.model';
import { ModulesUpdateService } from '../../services/modules.update.service';
import { RxStompService } from '@stomp/ng2-stompjs';
import { SyncService } from '../../services/sync.service';
import { Message } from '@stomp/stompjs';
import { Subscription } from 'rxjs';
import * as moment_ from 'moment';

const moment = moment_;

@Component({
    selector: 'modules-update',
    templateUrl: './modules-update.component.html'
})
export class ModulesUpdateComponent implements OnInit, OnDestroy {
    statusSubscription: Subscription;
    modules: ModuleUpdate[] = [];
    isUpdating = false;
    installed = false;
    completeChecking = false;
    checked = false;
    properties: Array<CardViewItem> = [];
    serverContacted = false;

    constructor(private service: ModulesUpdateService, private stompService: RxStompService, private syncService: SyncService) {
    }

    ngOnInit(): void {
        this.statusSubscription = this.stompService.watch('/topic/update/download/completed').subscribe((msg: Message) => {
            this.completeChecking = msg.body === 'true';
            this.checked = true
            this.service.availableUpdates().subscribe(res => this.modules = res)
        });
        this.service.availableUpdates().subscribe(res => this.modules = res);
        this.service.lastHeartbeat().subscribe(res => {
            this.serverContacted = !!res;
            this.properties = [];
            this.properties.push(new CardViewDatetimeItemModel({
                key: 'hb',
                label: 'Last Contact with Server',
                value: res,
                format: 'DD MMM, YYYY HH:MM'
            }))
        })
    }

    ngOnDestroy() {
        this.statusSubscription.unsubscribe()
    }

    checkForUpdates() {
        this.service.checkForUpdates().subscribe(res => {
            this.checked = true;
            this.completeChecking = false;
        })
    }

    getProperties(module: ModuleUpdate): Array<CardViewItem> {
        const properties = [];
        const description = new CardViewTextItemModel({
            label: 'Name',
            value: module.name,
            key: 'desc',
        });
        properties.push(description);
        const version = new CardViewTextItemModel({
            label: 'version',
            value: module.version,
            key: 'version',
        });
        properties.push(version);
        const active = new CardViewDatetimeItemModel({
            label: 'Build Time',
            value: moment(module.buildTime),
            key: 'active',
            format: 'DD MMM, YYYY HH:MM'
        });
        properties.push(active);
        return properties;
    }

    updateModules() {
        this.isUpdating = true;
        this.installed = false;
        this.service.installUpdates().subscribe(res => {
            this.modules = res;
            this.isUpdating = false;
            this.installed = true;
            this.service.availableUpdates().subscribe(res => this.modules = res)
        })
    }

    previousState() {
        window.history.back();
    }
}
