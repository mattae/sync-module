package org.fhi360.lamis.modules.database.web.rest;

import lombok.RequiredArgsConstructor;
import org.fhi360.lamis.modules.database.service.SyncService;
import org.fhi360.lamis.modules.database.service.UploadReportService;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/database-sync")
public class SyncResource {
    private final SyncService syncService;
    private final UploadReportService uploadReportService;
    private final SimpMessageSendingOperations messagingTemplate;

    @GetMapping("/running")
    public Boolean syncing() {
        return syncService.syncOngoing();
    }

    @GetMapping("/sync")
    public void run() {
        syncService.sync();
    }

    @GetMapping("init")
    public void init() {
        syncService.init();
    }

    @GetMapping("/destroy")
    public void destroy() {
        syncService.destroy();
    }

    @GetMapping("/cleanup-database")
    public void cleanupDatabase(@RequestParam List<Long> ids) {
        syncService.cleanupDatabase(ids);
    }

    @GetMapping("/upload-report")
    public void convertData(@RequestParam Long stateId, @RequestParam(defaultValue = "0", required = false) Integer format,
                            HttpServletResponse response) throws IOException {
        ByteArrayOutputStream baos = uploadReportService.getUploadReport(stateId, format);
        setStream(baos, response);
    }

    @GetMapping("/states")
    public List<Map<String, Object>> getStates() {
        return uploadReportService.getStates();
    }

    private void setStream(ByteArrayOutputStream baos, HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "application/octet-stream");
        response.setHeader("Content-Length", Integer.valueOf(baos.size()).toString());
        OutputStream outputStream = response.getOutputStream();
        outputStream.write(baos.toByteArray());
        outputStream.close();
        response.flushBuffer();
    }

    @GetMapping("/upload-biometrics")
    public void uploadBiometrics() {
        syncService.uploadBiometrics();
    }

    @GetMapping("/download-biometrics")
    public void downloadBiometrics() {
        syncService.downloadBiometrics();
    }

    @GetMapping("/biometric-upload-completed")
    public boolean biometricUploadCompleted() {
        return syncService.biometricUploadCompleted();
    }

    @GetMapping("/biometric-download-completed")
    public boolean biometricDownloadCompleted() {
        return syncService.biometricDownloadCompleted();
    }
}
