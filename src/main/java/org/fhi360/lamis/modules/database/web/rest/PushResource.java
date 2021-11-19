package org.fhi360.lamis.modules.database.web.rest;

import org.jumpmind.symmetric.service.IDataLoaderService;
import org.jumpmind.symmetric.service.INodeService;
import org.jumpmind.symmetric.service.IParameterService;
import org.jumpmind.symmetric.statistic.IStatisticManager;
import org.jumpmind.symmetric.web.IInterceptor;
import org.jumpmind.symmetric.web.PushUriHandler;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/sync")
public class PushResource extends PushUriHandler {
    public PushResource(IParameterService parameterService, IDataLoaderService dataLoaderService, IStatisticManager statisticManager, INodeService nodeService, IInterceptor... interceptors) {
        super(parameterService, dataLoaderService, statisticManager, nodeService, interceptors);
    }

    @PutMapping("/{serverId}/push")
    public void handlePutRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }

    @GetMapping("/{serverId}/push")
    public void handleGetRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }

    @PostMapping("/{serverId}/push")
    public void handlePostRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }
}
