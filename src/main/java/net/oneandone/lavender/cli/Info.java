/*
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

import net.oneandone.lavender.modules.NodeModule;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import net.oneandone.lavender.modules.Module;
import net.oneandone.sushi.util.Strings;

public class Info extends Base {
    private final FileNode war;

    public Info(Globals globals, FileNode war) throws IOException {
        super(globals);

        this.war = war.checkFile();
    }

    public void run() throws IOException, URISyntaxException {
        Node<?> open;
        List<Module> modules;

        open = war.openZip();
        modules = NodeModule.fromWebapp(globals.cacheroot(), true, open, globals.properties().secrets);
        for (Module module : modules) {
            System.out.println(module.getName());
            System.out.println(Strings.indent("" + module.descriptorOpt, "  "));
        }
    }
}
