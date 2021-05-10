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
package net.oneandone.lavender.modules;

import net.oneandone.lavender.config.Secrets;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class NodeModule extends Module<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(Module.class);
    // used to detect a recent parent pom

    public static List<Module> fromWebapp(FileNode cache, boolean prod, Node<?> webapp, Secrets secrets)
            throws IOException {
        List<Module> result;
        PustefixWarConfig rootConfig;
        ModuleProperties application;
        PustefixJar pustefixJar;


        LOG.trace("scanning " + webapp);
        application = ModuleProperties.loadApp(prod, webapp);
        result = new ArrayList<>();
        rootConfig = PustefixWarConfig.fromXml(webapp);
        // add modules before webapp, because they have a prefix
        for (Node<?> jar : webapp.find("WEB-INF/lib/*.jar")) {
            pustefixJar = PustefixJar.forNodeOpt(prod, jar, rootConfig);
            if (pustefixJar != null) {
                if (pustefixJar.moduleProperties != null) {
                    result.add(pustefixJar.moduleProperties.create(cache, prod, secrets, pustefixJar.config));
                }
            }
        }
        result.add(application.create(cache, prod, secrets, null));
        return result;
    }

    //--

    public NodeModule(Node origin, String name, ModuleProperties descriptorOpt, boolean lavendelize, String resourcePathPrefix, String targetPathPrefix, Filter filter) {
        this(origin.getUri().toString(), name, descriptorOpt, lavendelize, resourcePathPrefix, targetPathPrefix, filter);
    }

    public NodeModule(String origin, String name, ModuleProperties descriptorOpt, boolean lavendelize, String resourcePathPrefix, String targetPathPrefix, Filter filter) {
        super(origin, name, descriptorOpt, lavendelize, resourcePathPrefix, targetPathPrefix, filter);
    }

    protected Resource createResource(String resourcePath, Node file) throws IOException {
        return NodeResource.forNode(file, resourcePath);
    }
}
