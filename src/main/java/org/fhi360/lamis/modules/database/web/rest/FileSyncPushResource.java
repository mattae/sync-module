package org.fhi360.lamis.modules.database.web.rest;

import org.apache.commons.fileupload.FileUploadException;
import org.fhi360.lamis.modules.database.service.ServerSymmetricEngine;
import org.jumpmind.symmetric.web.FileSyncPushUriHandler;
import org.jumpmind.symmetric.web.IInterceptor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/sync")
@Profile("server")
public class FileSyncPushResource extends FileSyncPushUriHandler {
    public FileSyncPushResource(ServerSymmetricEngine engine, IInterceptor... interceptors) {
        super(engine, interceptors);
    }

    @PutMapping("/{serverId}/filesync/push")
    public void handlePutRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException, FileUploadException {
	try {
            handle(req, res);
	}catch(Exception ignored)
	{}
    }

    @GetMapping("/{serverId}/filesync/push")
    public void handleGetRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException, FileUploadException {
	try {
            handle(req, res);
	}catch(Exception ignored)
	{}
    }

    @PostMapping("/{serverId}/filesync/push")
    public void handlePostRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException, FileUploadException {
	try {
            handle(req, res);
	}catch(Exception ignored)
	{}
    }
}
