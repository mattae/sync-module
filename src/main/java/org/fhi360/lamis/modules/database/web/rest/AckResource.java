package org.fhi360.lamis.modules.database.web.rest;

import org.jumpmind.symmetric.service.IAcknowledgeService;
import org.jumpmind.symmetric.service.IParameterService;
import org.jumpmind.symmetric.web.AckUriHandler;
import org.jumpmind.symmetric.web.IInterceptor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/sync")
public class AckResource  extends AckUriHandler {

    public AckResource(IParameterService parameterService, IAcknowledgeService acknowledgeService, IInterceptor... interceptors) {
        super(parameterService, acknowledgeService, interceptors);
    }

    @PostMapping("/{serverId}/ack")
    public void handleRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }
}
