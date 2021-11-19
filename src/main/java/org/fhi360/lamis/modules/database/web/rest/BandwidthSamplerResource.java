package org.fhi360.lamis.modules.database.web.rest;

import org.jumpmind.symmetric.service.IParameterService;
import org.jumpmind.symmetric.web.BandwidthSamplerUriHandler;
import org.jumpmind.symmetric.web.IInterceptor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/sync")
public class BandwidthSamplerResource extends BandwidthSamplerUriHandler {
    public BandwidthSamplerResource(IParameterService parameterService, IInterceptor[] interceptors) {
        super(parameterService, interceptors);
    }

    @GetMapping("/{serverId}/bandwidth")
    public void handleGetRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }

    @PostMapping("/{serverId}/bandwidth")
    public void handlePostRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }

    @PutMapping("/{serverId}/bandwidth")
    public void handlePuttRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }
}
