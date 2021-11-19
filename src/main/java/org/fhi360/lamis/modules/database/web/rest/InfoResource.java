package org.fhi360.lamis.modules.database.web.rest;

import org.jumpmind.symmetric.service.IConfigurationService;
import org.jumpmind.symmetric.service.INodeService;
import org.jumpmind.symmetric.service.IParameterService;
import org.jumpmind.symmetric.web.IInterceptor;
import org.jumpmind.symmetric.web.InfoUriHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/sync")
public class InfoResource extends InfoUriHandler {
    public InfoResource(IParameterService parameterService, INodeService nodeService, IConfigurationService configurationService, IInterceptor[] interceptors) {
        super(parameterService, nodeService, configurationService, interceptors);
    }

    @GetMapping("/{serverId}/info")
    public void handleRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }
}
