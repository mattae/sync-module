package org.fhi360.lamis.modules.database.web.rest;

import org.apache.commons.fileupload.FileUploadException;
import org.jumpmind.symmetric.service.INodeCommunicationService;
import org.jumpmind.symmetric.service.IParameterService;
import org.jumpmind.symmetric.web.IInterceptor;
import org.jumpmind.symmetric.web.PushStatusUriHandler;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/sync")
public class PushStatusResource extends PushStatusUriHandler {
    public PushStatusResource(IParameterService parameterService, INodeCommunicationService nodeCommunicationService, IInterceptor... interceptors) {
        super(parameterService, nodeCommunicationService, interceptors);
    }

    @PutMapping("/{serverId}/pushstatus")
    public void handlePutRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException, FileUploadException {
        handle(req, res);
    }

    @GetMapping("/{serverId}/pushstatus")
    public void handleGetRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException, FileUploadException {
        handle(req, res);
    }

    @PostMapping("/{serverId}/pushstatus")
    public void handlePostRequest(@PathVariable String serverId, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException, FileUploadException {
        handle(req, res);
    }
}
