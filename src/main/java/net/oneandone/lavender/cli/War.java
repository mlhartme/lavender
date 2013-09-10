/**
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.lavender.cli;

import net.oneandone.lavender.config.Net;
import net.oneandone.lavender.config.Settings;
import net.oneandone.lavender.config.View;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Value;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;

public class War extends Base {
    @Value(name = "view", position = 1)
    private String viewName;

    @Value(name = "inputWar", position = 2)
    private FileNode inputWar;

    @Value(name = "outputWar", position = 3)
    private FileNode outputWar;

    @Value(name = "idxName", position = 4)
    private String indexName;


    public War(Console console, Settings settings, Net net) {
        super(console, settings, net);
    }

    @Override
    public void invoke() throws IOException {
        FileNode tmp;
        FileNode outputNodesFile;
        WarEngine engine;
        View view;

        inputWar.checkFile();
        outputWar.checkNotExists();
        tmp = inputWar.getWorld().getTemp();
        outputNodesFile = tmp.createTempFile();
        view = net.view(viewName);
        engine = new WarEngine(view, indexName, settings.svnUsername, settings.svnPassword,
                inputWar, outputWar, outputNodesFile, view.get(View.WEB).alias.nodesFile());
        engine.run();
        outputNodesFile.deleteFile();
    }
}
