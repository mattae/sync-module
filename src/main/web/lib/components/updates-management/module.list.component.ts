import { Component, OnDestroy, OnInit } from '@angular/core';
import { ModuleUpdate } from '../../model/module.model';
import { ModuleManagementService } from '../../services/module.management.service';
import {
    CardViewBoolItemModel,
    CardViewDatetimeItemModel,
    CardViewItem,
    CardViewTextItemModel
} from '@alfresco/adf-core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
    selector: 'module-update-list',
    templateUrl: './module.list.component.html'
})
export class ModuleListComponent implements OnInit, OnDestroy {
    modules: ModuleUpdate[];

    constructor(private moduleManagementService: ModuleManagementService, private router: Router, private route: ActivatedRoute) {
    }

    ngOnInit(): void {
        this.moduleManagementService.availableModules().subscribe(res => this.modules = res)
    }

    ngOnDestroy(): void {
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
            value: module.buildTime,
            key: 'build',
            format: 'DD MMM, YYYY HH:MM'
        });
        properties.push(active);
        properties.push(new CardViewBoolItemModel({
            key: 'uninstall',
            label: 'Uninstall',
            value: module.uninstall
        }))
        return properties;
    }

    update(module: ModuleUpdate) {
        this.router.navigate(['.', module.id, 'update'], {relativeTo: this.route})
    }

    uninstall(module: ModuleUpdate) {
        this.moduleManagementService.uninstall(module).subscribe(res => {
            this.moduleManagementService.availableModules().subscribe(res => this.modules = res)
        });
    }

    previousState() {
        window.history.back();
    }
}
