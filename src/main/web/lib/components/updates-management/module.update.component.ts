import { Component, OnInit } from '@angular/core';
import { ModuleUpdate } from '../../model/module.model';
import {
    CardViewDatetimeItemModel,
    CardViewItem,
    CardViewTextItemModel,
    NotificationService
} from '@alfresco/adf-core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppLoaderService } from '@lamis/web-core';
import { ModuleManagementService } from '../../services/module.management.service';

@Component({
    selector: 'module-update-update',
    templateUrl: './module.update.component.html'
})
export class ModuleUpdateComponent implements OnInit {
    module: ModuleUpdate;
    files: File | FileList;
    properties: Array<CardViewItem> = [];
    update = false;
    uploaded = false;

    constructor(private notification: NotificationService, private router: Router, private service: ModuleManagementService,
                private loaderService: AppLoaderService, private route: ActivatedRoute) {

    }

    ngOnInit(): void {
        this.route.data.subscribe(({module}) => {
            this.module = !!module && module.body ? module.body : module;
            this.update = !!this.module;
        });
    }

    uploadEvent(file: File): void {
        this.uploaded = false;
        const formData = new FormData();
        formData.append('file', file);
        this.loaderService.open('Uploading module: please wait...');
        this.service.uploadModule(formData)
            .subscribe((res) => {
                    this.loaderService.close();
                    this.uploaded = true;
                    if (res.ok) {
                        this.module = res.body;
                        this.properties = [];
                        const name = new CardViewTextItemModel({
                            label: 'Name',
                            value: this.module.name,
                            key: 'name',
                        });
                        this.properties.push(name);
                        const version = new CardViewTextItemModel({
                            label: 'Version',
                            value: this.module.version,
                            key: 'version',
                        });
                        this.properties.push(version);
                        const buildTime = new CardViewDatetimeItemModel({
                            label: 'Build Time',
                            value: this.module.buildTime,
                            key: 'bp',
                            format: 'DD MMM, YYYY HH:MM'
                        });
                        this.properties.push(buildTime);

                    } else {
                        this.notification.showError('Module upload failed')
                    }
                },
                (error => this.notification.showError('Module upload failed: ' + error.text)));
    }

    install() {
        this.loaderService.open('Saving module update: please wait...');
        this.service.install(this.module).subscribe((res) => {
                this.loaderService.close();
                this.router.navigate(['..'], {relativeTo: this.route});
            },
            (err: any) => {
                this.loaderService.close();
                this.notification.showError('Could not save module update: ' + err.text);
            }
        );
    }

    selectEvent(files: File): void {
    }

    cancelEvent(): void {
        this.files = undefined
    }
}
