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
import net.oneandone.lavender.config.UsernamePassword;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Action;
import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.fs.filter.Predicate;
import net.oneandone.sushi.fs.svn.SvnNode;
import net.oneandone.sushi.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/** Factory for Scm modules. Stores scm info from the respective pom plus filter configuration */
public class ScmProperties {
    private static final Logger LOG = LoggerFactory.getLogger(ScmProperties.class);

    public static final String SCM_PREFIX = "scm.";

    public final String name;
    public final boolean classic;
    public final Filter filter;
    public final String scmurl;

    /** for svn: revision number; git: commit hash */
    public final String revision;
    public final String path;
    public final boolean lavendelize;
    public final String resourcePathPrefix;
    public final String targetPathPrefix;

    public final Map<String, String> indexOpt;

    /** Absolute path relative to local sources for this module, null if not available */
    public final String source;

    /// CHECKSTYLE:OFF
    public ScmProperties(String name, boolean classic, Filter filter, String scmurl, String revision, String path,
                         boolean lavendelize, String resourcePathPrefix, String targetPathPrefix, Map<String, String> indexOpt,
                         String source) {
        /// CHECKSTYLE:ON
        if (scmurl == null) {
            throw new NullPointerException();
        }
        if (name.startsWith("/") || name.endsWith("/")) {
            throw new IllegalArgumentException(name);
        }
        this.name = name;
        this.classic = classic;
        this.filter = filter;
        this.scmurl = scmurl;
        this.revision = revision;
        this.path = path;
        this.lavendelize = lavendelize;
        this.resourcePathPrefix = resourcePathPrefix;
        this.targetPathPrefix = targetPathPrefix;
        this.indexOpt = indexOpt;
        this.source = source;
    }

    /** @param jarConfigOpt null when creating the application module or a temporary module for the scm command */
    public Module create(FileNode cacheDir, boolean prod, Secrets secrets, PustefixJarConfig jarConfigOpt) throws IOException {
        World world;
        final FileNode checkout;
        String scm;
        long pinnedRevision;

        world = cacheDir.getWorld();
        if (source != null) {
            checkout = world.file(source);
            if (checkout.isDirectory()) {
                LOG.info(name + ": create source module: " + source);
                // I could also check if the svnurl noted in the artifact matches the svn url of checkout,
                // but that fails for frontend teams creating a branch without adjusting scm elements in the pom.

                return new NodeModule(checkout, name, this, lavendelize, resourcePathPrefix, targetPathPrefix, filter) {
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
                                String relative;

                                relative = node.getRelative(checkout);
                                if (filter.matches(relative)) {
                                    if (jarConfigOpt != null) {
                                        relative = jarConfigOpt.getPath(relative);
                                    }
                                    if (relative != null) {
                                        result.put(relative, node);
                                    }
                                }
                            }
                        });
                        return result;
                    }
                };
            }
            // fall-through
        }
        scm = Strings.removeLeft(scmurl, "scm:");
        if (indexOpt != null) {
            return createIndexedModule(world, scm, revision, accessPathPrefix(path), jarConfigOpt, secrets);
        } else {
            if (scm.startsWith("svn:")) {
                pinnedRevision = !prod || revision.isEmpty() ? -1 : Long.parseLong(revision);
                return createSvnModule(cacheDir, jarConfigOpt, world, scm + path, secrets, pinnedRevision);
            } else if (scm.startsWith("git:")) {
                return createBitbucketModule(world, scm, secrets, accessPathPrefix(path), jarConfigOpt);
            } else {
                throw new IllegalStateException("scm url not supported: " + scm);
            }
        }
    }

    private static String accessPathPrefix(String path) {
        if (path.isEmpty()) {
            return path;
        } else {
            return Strings.removeLeft(path, "/") + "/";
        }
    }

    public static String urlToFilename(String url) {
        url = url.replace(":", "-");
        // CAUTION: place all files directly in the configured cache directory - sub directories would cause permission problems
        url = url.replace("/", "_");
        url = Strings.removeLeftOpt(url, ".");
        return url;
    }

    private IndexedModule createIndexedModule(World world, String scm, String at, String accessPathPrefix,
                                              PustefixJarConfig configOpt, Secrets secrets) throws IOException {
        UrlPattern urlPattern;

        if (!scm.startsWith("git:")) {
            throw new UnsupportedOperationException("TODO " + scm);
        }
        urlPattern = UrlPattern.create(world, scm, at, secrets);
        return new IndexedModule(scm, name, this, lavendelize, resourcePathPrefix, targetPathPrefix, filter,
                accessPathPrefix, configOpt, indexOpt, urlPattern);
    }

    private SvnModule createSvnModule(FileNode cacheDir, PustefixJarConfig jarConfig, World world, String scm, Secrets secrets, long pinnedRevision) throws IOException {
        SvnNode root;
        FileNode cache;

        try {
            root = (SvnNode) world.node(secrets.withSecrets(URI.create(scm)));
            // make sure to get a proper error message - and to get it early
            root.checkDirectory();
            cache = cacheDir.join("svn", urlToFilename(scm) + ".idx");
            cache.getParent().mkdirsOpt();
            return new SvnModule(name, this, cache, root, pinnedRevision, lavendelize, resourcePathPrefix, targetPathPrefix, filter, jarConfig);
        } catch (RuntimeException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("error scanning svn module " + scm + ": " + e.getMessage(), e);
        }
    }

    private BitbucketModule createBitbucketModule(World world, String urlstr, Secrets secrets,
                                                  String accessPathPrefix, PustefixJarConfig config) throws IOException {
        URI uri;
        UsernamePassword up;
        String uriPath;
        String project;
        String repository;
        int idx;

        up = secrets.get(urlstr);
        uri = URI.create(urlstr);
        if (!uri.getScheme().equals("git")) {
            throw new IllegalArgumentException("git uri expected, got " + urlstr);
        }
        uri = URI.create(uri.getSchemeSpecificPart());
        uriPath = Strings.removeLeft(uri.getPath(), "/");
        idx = uriPath.indexOf('/');
        project = uriPath.substring(0, idx);
        repository = Strings.removeRight(uriPath.substring(idx + 1), ".git");
        return new BitbucketModule(Bitbucket.create(world, uri.getHost(), up),
                project, repository, repository.isEmpty() ? "master" : revision, accessPathPrefix, name, this, lavendelize, resourcePathPrefix,
                targetPathPrefix, filter, config);
    }

    public String toString() {
        return "name: " + name + "\n"
                + "classic: " + classic + "\n"
                + "revision: " + revision + "\n"
                + "include: " + Strings.toList(filter.getIncludes()) + "\n"
                + "exclude: " + Strings.toList(filter.getExcludes()) + "\n"
                + "connection: " + scmurl + "\n"
                + "path: " + path + "\n"
                + "lavendelize: " + lavendelize + "\n"
                + "resourcePathPrefix: " + resourcePathPrefix + "\n"
                + "targetPathPrefix: " + targetPathPrefix;
    }
}
