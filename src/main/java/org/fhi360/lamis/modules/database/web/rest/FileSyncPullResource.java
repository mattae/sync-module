package org.fhi360.lamis.modules.database.web.rest;

import org.fhi360.lamis.modules.database.service.ServerSymmetricEngine;
import org.jumpmind.symmetric.web.FileSyncPullUriHandler;
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
public class FileSyncPullResource extends FileSyncPullUriHandler {
    public FileSyncPullResource(ServerSymmetricEngine engine, IInterceptor... interceptors) {
        super(engine, interceptors);
    }

    @PutMapping("/{serverId}/filesync/pull")
    public void handlePutRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
	try {
            handle(req, res);
	}catch(Exception ignored)
	{}
    }

    @GetMapping("/{serverId}/filesync/pull")
    public void handleGetRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
	try {
            handle(req, res);
	}catch(Exception ignored)
	{}
    }

    @PostMapping("/{serverId}/filesync/pull")
    public void handlePostRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
	try {
            handle(req, res);
	}catch(Exception ignored)
	{}
    }
}
