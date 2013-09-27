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
package net.oneandone.lavender.modules;

import net.oneandone.lavender.config.Docroot;
import net.oneandone.lavender.config.Filter;
import net.oneandone.lavender.index.Index;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.svn.SvnFilesystem;
import net.oneandone.sushi.fs.svn.SvnNode;
import net.oneandone.sushi.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SvnModuleConfig {
    private static final Logger LOG = LoggerFactory.getLogger(SvnModuleConfig.class);

    private static final String SVN_PREFIX = "svn.";

    public final String folder;
    public final Filter filter;
    public String svnurl;
    public String type = Docroot.WEB;
    public boolean lavendelize = true;
    public String sourcePathPrefix = "";
    public String targetPathPrefix = "";

    public SvnModuleConfig(String folder, Filter filter) {
        this.folder = folder;
        this.filter = filter;
    }

    public static Collection<SvnModuleConfig> parse(Properties properties) {
        String key;
        String value;
        String name;
        SvnModuleConfig config;
        Map<String, SvnModuleConfig> result;

        result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            key = (String) entry.getKey();
            if (key.startsWith(SVN_PREFIX)) {
                key = key.substring(SVN_PREFIX.length());
                value = (String) entry.getValue();
                int idx = key.indexOf('.');
                if (idx == -1) {
                    name = key;
                    key = null;
                } else {
                    name = key.substring(0, idx);
                    key = key.substring(idx + 1);
                }
                config = result.get(name);
                if (config == null) {
                    config = new SvnModuleConfig(name, Filter.forProperties(properties, SVN_PREFIX + name, null));
                    result.put(name, config);
                }
                if (key == null) {
                    config.svnurl = Strings.removeLeftOpt((String) entry.getValue(), "scm:svn:");
                } else {
                    if (key.equals("targetPathPrefix")) {
                        config.targetPathPrefix = value;
                    } else if (key.equals("pathPrefix")) {
                        // TODO: dump
                        LOG.warn("CAUTION: out-dated pathPrefix - use targetPathPrefix instead");
                        config.targetPathPrefix = value;
                    } else if (key.equals("sourcePathPrefix")) {
                        config.sourcePathPrefix = value;
                    } else if (key.equals("type")) {
                        config.type = value;
                    } else if (key.equals("storage")) {
                        if (value.startsWith("flash-")) {
                            // TODO: dump
                            LOG.warn("CAUTION: out-dated storage configured - use type instead");
                            config.type = Docroot.FLASH;
                        } else {
                            throw new IllegalArgumentException("storage no longer supported: " + value);
                        }
                    } else if (key.equals("lavendelize")) {
                        if ("true".equals(value)) {
                            config.lavendelize = true;
                        } else if ("false".equals(value)) {
                            config.lavendelize = false;
                        } else {
                            throw new IllegalArgumentException("illegal value: " + value);
                        }
                    }
                }
            }
        }
        return result.values();
    }

    public SvnModule create(World world, String svnUsername, String svnPassword) throws IOException {
        FileNode cache;
        final SvnNode root;
        final Index oldIndex;
        String name;

        if (svnurl == null) {
            throw new IllegalArgumentException("missing svn url");
        }
        if (folder.startsWith("/") || folder.endsWith("/")) {
            throw new IllegalArgumentException(folder);
        }
        try {
            // TODO: ugly side-effect
            world.getFilesystem("svn", SvnFilesystem.class).setDefaultCredentials(svnUsername, svnPassword);
            root = (SvnNode) world.node("svn:" + svnurl);
            name = root.getSvnurl().getPath().replace('/', '.') + ".idx";
            name = Strings.removeLeftOpt(name, ".");
            cache = (FileNode) world.getHome().join(".cache/lavender",
                    root.getRoot().getRepository().getRepositoryRoot(false).getHost(), name);
            if (cache.exists()) {
                oldIndex = Index.load(cache);
            } else {
                cache.getParent().mkdirsOpt();
                oldIndex = new Index();
            }
            return new SvnModule(filter, type, oldIndex, new Index(), cache, root, lavendelize, sourcePathPrefix, targetPathPrefix, folder);
        } catch (RuntimeException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("error scanning svn module " + svnurl + ": " + e.getMessage(), e);
        }
    }
}
