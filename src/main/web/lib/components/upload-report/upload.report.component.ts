import { NotificationService } from '@alfresco/adf-core';
import { Component, OnInit } from '@angular/core';
import { AppLoaderService } from '@lamis/web-core';
import { saveAs } from 'file-saver';
import { SyncService } from '../../services/sync.service';

@Component({
    selector: 'upload-report',
    templateUrl: './upload.report.component.html'
})
export class UploadReportComponent implements OnInit {
    running = false;
    state: any;
    format: number;
    states: any[];

    constructor(private service: SyncService, private appLoader: AppLoaderService, private notification: NotificationService) {
    }

    ngOnInit() {
        this.service.states().subscribe(res => this.states = res);
    }

    previousState() {
        window.history.back();
    }

    generate() {
        this.running = true;
        this.appLoader.open('Generating report; please wait...')
        this.service.uploadReport(this.state.id, this.format).subscribe(
            (res) => {
                this.appLoader.close();
                this.running = false;
                const format = this.format === 0 ? 'pdf' : 'xlsx';
                const file = new File([res], `${this.state.name} Database Upload_Biometric Coverage Report.${format}`,
                    {type: 'application/octet-stream'});
                saveAs(file);
            },
            (err) => {
                this.appLoader.close();
                this.running = false;
                this.notification.showError(`An error occurred generating report: ${err.message}`)
            });
    }
}
