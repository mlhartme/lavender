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

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Action;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;
import net.oneandone.sushi.fs.svn.SvnFilesystem;
import net.oneandone.sushi.fs.svn.SvnNode;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SvnProperties {
    public static final String SVN_PREFIX = "svn.";

    public final String name;
    public final Filter filter;
    public final String svnurl;
    /** to pin svnurl -- not for svnurlDevel */
    public final long svnurlRevision;
    public final String svnurlDevel;
    public final String type;
    public final boolean lavendelize;
    public final String resourcePathPrefix;
    public final String targetPathPrefix;

    /** Absolute path relative to local sources for this module, null if not available */
    public final String source;

    public SvnProperties(String name, Filter filter, String svnurl, long svnurlRevision, String svnurlDevel, String type, boolean lavendelize, String resourcePathPrefix,
                         String targetPathPrefix, String source) {
        if (name.startsWith("/") || name.endsWith("/")) {
            throw new IllegalArgumentException(name);
        }
        this.name = name;
        this.filter = filter;
        this.svnurl = svnurl;
        this.svnurlRevision = svnurlRevision;
        this.svnurlDevel = svnurlDevel;
        this.type = type;
        this.lavendelize = lavendelize;
        this.resourcePathPrefix = resourcePathPrefix;
        this.targetPathPrefix = targetPathPrefix;
        this.source = source;
    }

    public Module create(FileNode cacheDir, boolean prod, String svnUsername, String svnPassword, final JarConfig jarConfig) throws IOException {
        World world;
        FileNode cache;
        final SvnNode root;
        String idxName;
        final FileNode checkout;
        String url;
        long pinnedRevision;

        world = cacheDir.getWorld();
        if (svnurl == null) {
            throw new IllegalArgumentException("missing svn url");
        }

        // TODO: ugly side-effect
        world.getFilesystem("svn", SvnFilesystem.class).setDefaultCredentials(svnUsername, svnPassword);

        if (source != null) {
            checkout = world.file(source);
            if (checkout.isDirectory()) {
                // I could also check if the svnurl noted in the artifact matches the svn url of checkout,
                // but that fails for frontend teams creating a branch without adjusting scm elements in the pom.

                return new NodeModule(type, name, lavendelize, resourcePathPrefix, targetPathPrefix, filter) {
                    @Override
                    protected Map<String, Node> doScan(final Filter filter) throws Exception {
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
                                    if (jarConfig != null) {
                                        path = jarConfig.getPath(path);
                                    }
                                    if (path != null) {
                                        result.put(path, node);
                                    }
                                }
                            }
                        });
                        return result;

                    }
                };
            }
        }
        if (prod) {
            url = svnurl;
            pinnedRevision = svnurlRevision;
        } else {
            url = svnurlDevel;
            // devel url is never pinned:
            pinnedRevision = -1;
        }
        try {
            root = (SvnNode) world.node("svn:" + url);
            // make sure to get a propery error message, and to get it early
            root.checkDirectory();
            idxName = root.getSvnurl().getPath().replace('/', '.') + ".idx";
            idxName = Strings.removeLeftOpt(idxName, ".");
            // CAUTION: place all files directly in the configured cache directory - sub directories would cause permission problems
            cache = cacheDir.join(root.getRoot().getRepository().getRepositoryRoot(false).getHost() + "_" + idxName);
            return SvnModule.create(type, name, cache, root, pinnedRevision, lavendelize, resourcePathPrefix, targetPathPrefix, filter, jarConfig);
        } catch (RuntimeException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("error scanning svn module " + url + ": " + e.getMessage(), e);
        }
    }
}
