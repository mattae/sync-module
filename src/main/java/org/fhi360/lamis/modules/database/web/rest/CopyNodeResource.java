package org.fhi360.lamis.modules.database.web.rest;

import org.fhi360.lamis.modules.database.service.ServerSymmetricEngine;
import org.jumpmind.symmetric.web.CopyNodeUriHandler;
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
public class CopyNodeResource extends CopyNodeUriHandler {
    public CopyNodeResource(ServerSymmetricEngine engine, IInterceptor... interceptors) {
        super(engine, interceptors);
    }

    @PutMapping("/{serverId}/copy")
    public void handleRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }

    @GetMapping("/{serverId}/copy")
    public void handleGetRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }

    @PostMapping("/{serverId}/copy")
    public void handlePostRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }
}
