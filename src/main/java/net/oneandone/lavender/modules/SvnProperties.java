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
import net.oneandone.lavender.index.Index;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Action;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;
import net.oneandone.sushi.fs.svn.SvnFilesystem;
import net.oneandone.sushi.fs.svn.SvnNode;
import net.oneandone.sushi.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class SvnProperties {
    public static final String SVN_PREFIX = "svn.";

    public final String folder;
    public final Filter filter;
    public String svnurl;
    public String type = Docroot.WEB;
    public boolean lavendelize = true;
    public String resourcePathPrefix = "";
    public String targetPathPrefix = "";
    public String livePath;

    public SvnProperties(String folder, Filter filter) {
        this.folder = folder;
        this.filter = filter;
    }

    public Module create(boolean prod, World world, String svnUsername, String svnPassword) throws IOException {
        FileNode cache;
        final SvnNode root;
        final Index oldIndex;
        String name;
        final FileNode checkout;

        if (svnurl == null) {
            throw new IllegalArgumentException("missing svn url");
        }
        if (folder.startsWith("/") || folder.endsWith("/")) {
            throw new IllegalArgumentException(folder);
        }

        // TODO: ugly side-effect
        world.getFilesystem("svn", SvnFilesystem.class).setDefaultCredentials(svnUsername, svnPassword);

        if (!prod && livePath != null) {
            checkout = world.file(livePath);
            if (checkout.isDirectory()) {
                if (svnurl.equals(SvnNode.urlFromWorkspace(checkout))) {
                    return new DefaultModule(type, folder, resourcePathPrefix, targetPathPrefix, filter) {
                        @Override
                        protected Map<String, Node> scan(final Filter filter) throws Exception {
                            Filter f;
                            final Map<String, Node> result;

                            result = new HashMap<>();
                            f = checkout.getWorld().filter().predicate(Predicate.FILE).includeAll();
                            f.invoke(checkout, new Action() {
                                public void enter(Node node, boolean isLink) {
                                }

                                public void enterFailed(Node node, boolean isLink, IOException e) throws IOException {
                                    throw e;
                                }

                                public void leave(Node node, boolean isLink) {
                                }

                                public void select(Node node, boolean isLink) {
                                    String path;

                                    path = node.getRelative(checkout);
                                    if (filter.matches(path)) {
                                        result.put(path, node);
                                    }
                                }
                            });
                            return result;

                        }
                    };
                }
            }
        }
        try {
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
            return new SvnModule(filter, type, oldIndex, new Index(), cache, root, lavendelize, resourcePathPrefix, targetPathPrefix, folder);
        } catch (RuntimeException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("error scanning svn module " + svnurl + ": " + e.getMessage(), e);
        }
    }
}
