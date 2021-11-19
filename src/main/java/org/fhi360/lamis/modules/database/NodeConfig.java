package org.fhi360.lamis.modules.database;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.fhi360.lamis.modules.database.service.ClientSymmetricEngine;
import org.lamisplus.modules.base.config.ContextProvider;
import org.lamisplus.modules.base.module.ModuleLifecycle;

import java.io.File;
import java.io.IOException;

@Slf4j
public class NodeConfig implements ModuleLifecycle {

    @Override
    public void preInstall() {
        try {
            File file = new File("C:/symmetric-server/engines");
            if (file.exists()) {
                FileUtils.cleanDirectory(file);
            }
        } catch (IOException ignored) {

        }
    }

    @Override
    public void preUninstall() {
        ClientSymmetricEngine symmetricEngine = ContextProvider.getBean(ClientSymmetricEngine.class);
        symmetricEngine.stop();
    }
}
