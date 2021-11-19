package org.fhi360.lamis.modules.database.web.rest;

import org.jumpmind.symmetric.service.IDataExtractorService;
import org.jumpmind.symmetric.service.IParameterService;
import org.jumpmind.symmetric.web.BatchUriHandler;
import org.jumpmind.symmetric.web.IInterceptor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/sync")
public class BatchResource extends BatchUriHandler {
    public BatchResource(IParameterService parameterService, IDataExtractorService dataExtractorService, IInterceptor[] interceptors) {
        super(parameterService, dataExtractorService, interceptors);
    }

    @GetMapping("/{serverId}/batch")
    public void handleGetRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }

    @PutMapping("/{serverId}/batch")
    public void handlePutRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }

    @PostMapping("/{serverId}/batch")
    public void handlePostRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }
}
