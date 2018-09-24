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
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/** Factory for Scm modules. Stores scm info from the respective pom plus filter configuration */
public class ScmProperties {
    public static final String SVN_PREFIX = "svn.";
    public static final String SCM_PREFIX = "scm.";

    public final String name;
    public final Filter filter;
    public final String connectionProd;
    public final String connectionDevel;
    /** for svn: revision number */
    public final String tag;
    public final String path;
    public final String type;
    public final boolean lavendelize;
    public final String resourcePathPrefix;
    public final String targetPathPrefix;

    /** Absolute path relative to local sources for this module, null if not available */
    public final String source;

    public ScmProperties(String name, Filter filter, String connectionProd, String connectionDevel, String tag, String path, String type, boolean lavendelize, String resourcePathPrefix,
                         String targetPathPrefix, String source) {
        if (connectionProd == null) {
            throw new NullPointerException();
        }
        if (name.startsWith("/") || name.endsWith("/")) {
            throw new IllegalArgumentException(name);
        }
        this.name = name;
        this.filter = filter;
        this.connectionProd = connectionProd;
        this.connectionDevel = connectionDevel;
        this.tag = tag;
        this.path = path;
        this.type = type;
        this.lavendelize = lavendelize;
        this.resourcePathPrefix = resourcePathPrefix;
        this.targetPathPrefix = targetPathPrefix;
        this.source = source;
    }

    public Module create(FileNode cacheDir, boolean prod, String svnUsername, String svnPassword, final JarConfig jarConfig) throws IOException {
        World world;
        final FileNode checkout;
        String scm;
        long pinnedRevision;

        world = cacheDir.getWorld();

        // TODO: ugly side-effect
        world.getFilesystem("svn", SvnFilesystem.class).setDefaultCredentials(svnUsername, svnPassword);

        if (source != null) {
            checkout = world.file(source);
            if (checkout.isDirectory()) {
                // I could also check if the svnurl noted in the artifact matches the svn url of checkout,
                // but that fails for frontend teams creating a branch without adjusting scm elements in the pom.

                return new NodeModule(type, name, lavendelize, resourcePathPrefix, targetPathPrefix, filter) {
                    @Override
                    protected Map<String, Node> loadEntries() throws Exception {
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
            scm = connectionProd;
            pinnedRevision = tag.isEmpty() ? -1 : Long.parseLong(tag);
        } else {
            scm = connectionDevel;
            // devel url is never pinned:
            pinnedRevision = -1;
        }
        scm = Strings.removeLeft(scm, "scm:");
        if (scm.startsWith("svn:")) {
            return createSvnModule(cacheDir, jarConfig, world, scm + path, pinnedRevision);
        } else if (scm.startsWith("git:")) {
            return createBitbucketModule(cacheDir.getWorld(), Strings.removeLeft(scm,  "git:"), accessPathPrefix(path), jarConfig);
        } else {
            throw new IllegalStateException("scm url not supported: " + scm);
        }
    }

    private static String accessPathPrefix(String path) {
        if (path.isEmpty()) {
            return path;
        } else {
            return Strings.removeLeft(path, "/") + "/";
        }
    }

    private SvnModule createSvnModule(FileNode cacheDir, JarConfig jarConfig, World world, String scm, long pinnedRevision) throws IOException {
        SvnNode root;
        String idxName;
        FileNode cache;

        try {
            root = (SvnNode) world.node(scm);
            // make sure to get a proper error message - and to get it early
            root.checkDirectory();
            idxName = root.getSvnurl().getPath().replace('/', '.') + ".idx";
            idxName = Strings.removeLeftOpt(idxName, ".");
            // CAUTION: place all files directly in the configured cache directory - sub directories would cause permission problems
            cache = cacheDir.join("svn", root.getRoot().getRepository().getRepositoryRoot(false).getHost() + "_" + idxName);
            cache.getParent().mkdirsOpt();
            return new SvnModule(type, name, cache, root, pinnedRevision, lavendelize, resourcePathPrefix, targetPathPrefix, filter, jarConfig);
        } catch (RuntimeException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("error scanning svn module " + scm + ": " + e.getMessage(), e);
        }
    }

    private BitbucketModule createBitbucketModule(World world, String urlstr, String accessPathPrefix, JarConfig config) throws IOException {
        URI uri;
        String path;
        String project;
        String repository;
        int idx;

        uri = URI.create(urlstr);
        if (uri.isOpaque()) {
            throw new IllegalArgumentException("uri format not supported: " + uri);
        }
        System.out.println("host: " + uri.getHost());
        path = Strings.removeLeft(uri.getPath(), "/");
        idx = path.indexOf('/');
        project = path.substring(0, idx);
        repository = Strings.removeRight(path.substring(idx + 1), ".git");
        return BitbucketModule.create(world, uri.getHost(), project, repository, tag.isEmpty() ? "master" : tag, accessPathPrefix, name, lavendelize, resourcePathPrefix,
                targetPathPrefix, filter, config);
    }
}
