package org.fhi360.lamis.modules.database.web.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fhi360.lamis.modules.database.service.ClientSymmetricEngine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Slf4j
public class SymmetricNodeResource {
    private final ClientSymmetricEngine clientSymmetricEngine;

    @GetMapping("/reset-node")
    public void reset() {
        clientSymmetricEngine.stop();
        clientSymmetricEngine.uninstall();
        clientSymmetricEngine.setupDatabase(true);
        clientSymmetricEngine.startEngine();
    }

    @PostConstruct
    public void init() {
        try {
            if (!StringUtils.equals(clientSymmetricEngine.getNodeId(), "000")) {
                reset();
            }
        } catch (Exception ignored) {
        }
    }
}
