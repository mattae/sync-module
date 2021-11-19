package org.fhi360.lamis.modules.database.web.rest;

import org.jumpmind.symmetric.service.*;
import org.jumpmind.symmetric.statistic.IStatisticManager;
import org.jumpmind.symmetric.web.IInterceptor;
import org.jumpmind.symmetric.web.PullUriHandler;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/sync")
public class PullResource extends PullUriHandler {
    public PullResource(IParameterService parameterService, INodeService nodeService,
                        IConfigurationService configurationService,
                        IDataExtractorService dataExtractorService,
                        IRegistrationService registrationService,
                        IStatisticManager statisticManager,
                        IOutgoingBatchService outgoingBatchService,
                        IInterceptor... interceptors) {
        super(parameterService, nodeService, configurationService, dataExtractorService, registrationService, statisticManager, outgoingBatchService, interceptors);
    }

    @PutMapping("/{serverId}/pull")
    public void handlePutRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }

    @GetMapping("/{serverId}/pull")
    public void handleGetRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }

    @PostMapping("/{serverId}/pull")
    public void handlePostRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }
}
