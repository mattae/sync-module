package org.fhi360.lamis.modules.database.web.rest;

import lombok.extern.slf4j.Slf4j;
import org.jumpmind.symmetric.service.IParameterService;
import org.jumpmind.symmetric.service.IRegistrationService;
import org.jumpmind.symmetric.web.IInterceptor;
import org.jumpmind.symmetric.web.RegistrationUriHandler;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/sync")
@Slf4j
public class RegistrationResource extends RegistrationUriHandler {
    public RegistrationResource(IParameterService parameterService, IRegistrationService registrationService, IInterceptor... interceptors) {
        super(parameterService, registrationService, interceptors);
    }

    @PutMapping("/{serverId}/registration")
    public void handlePutRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }

    @GetMapping("/{serverId}/registration")
    public void handleGetRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }

    @PostMapping("/{serverId}/registration")
    public void handlePostRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }
}
