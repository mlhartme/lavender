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

import net.oneandone.lavender.config.HostProperties;
import net.oneandone.lavender.modules.NodeModule;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.xml.XmlException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ScanLegacy extends Base {
    private final FileNode war;

    public ScanLegacy(Globals globals, FileNode war) throws IOException, URISyntaxException {
        super(globals);

        this.war = war.checkFile();
    }

    public void run() throws IOException, SAXException, XmlException, URISyntaxException {
        HostProperties properties;
        List<String> legacy;

        properties = globals.properties();
        legacy = new ArrayList<>();
        NodeModule.fromWebapp(globals.cacheroot(), true, war.openZip(), properties.secrets, true, legacy);
        System.out.println("legacy modules: " + legacy);
    }
}
