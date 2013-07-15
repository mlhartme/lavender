package com.oneandone.lavendel.publisher.maven;

import com.oneandone.lavendel.index.Index;
import com.oneandone.lavendel.publisher.Distributor;
import com.oneandone.lavendel.publisher.config.Net;
import com.oneandone.lavendel.publisher.WarEngine;
import com.oneandone.lavendel.publisher.Log;
import com.oneandone.lavendel.publisher.cli.War;
import com.oneandone.lavendel.publisher.config.Vhost;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Enables Lavendel for a web application:
 * <ul>
 * <li>Extracts static resources from the war file into a temporary local folder</li>
 * <li>Creates the lavendel index file</li>
 * <li>Creates the lavendel nodes file</li>
 * <li>Updates the web.xml by adding the Lavendel Filter.</li>
 * <li>Creates an updated war file</li>
 * <li>Updates the web.xml in the exploded directory by adding the Lavendel Servlet. This is for testing only</li>
 * </ul>
 */
@Mojo(name = "publish")
public class PublishMojo extends AbstractMojo {
    /**
     * Lavendel cluster. Has no effect because this plugin does not actually publish anything.
     */
    @Parameter(defaultValue = "eu")
    private String cluster;

    /**
     * The original WAR file.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.war", required = true)
    private File inputWar;

    /**
     * The lavendelized WAR file.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}-lavendelized.war", required = true)
    private File outputWar;

    /**
     * The storage directory where static resources are pushed to.
     */
    @Parameter(defaultValue = "${project.build.directory}/lavendel", required = true)
    private File outputDir;

    /**
     * The location of the merged web.xml file.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.artifactId}/WEB-INF/web.xml", required = true)
    private File webXmlFile;

    /**
     * The location of the merged web.xml file.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.artifactId}/WEB-INF/lib", required = true)
    private File libDir;

    /**
     * The location of the created index file.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.artifactId}/WEB-INF/lavendel.idx", required = true)
    private File indexFile;

    /**
     * The location of the created nodes file. This file lists the urls of the Lavendel servers that will
     * serve the extracted resources.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.artifactId}/WEB-INF/lavendel.nodes", required = true)
    private File nodesFile;

    /**
     * {@inheritDoc}
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        World world;
        Map<String, Distributor> storages;
        Net net;
        Index index;

        world = new World();
        net = Net.normal();
        outputDir.mkdirs();
        try {
            // TODO: constants ...
            storages = War.createDefaultStorages(world, net.cluster(cluster),
                    net.cluster("flash-eu"), net.cluster("flash-us"), "lavendel.idx");
            index = new Index();
            WarEngine engine = new WarEngine(createLog(), world.file(inputWar), world.file(outputWar), storages,
                    world.file(webXmlFile), index, world.file(nodesFile), Vhost.one("lavendel").nodesFile());
            index.save(indexFile);
            engine.run();
            addLavendelizerJar(world);
            // TODO addCdsServlet(storages.get(Extractor.DEFAULT_STORAGE).baseDirectory.toPath().toFile());
        } catch (IOException e) {
            throw new MojoExecutionException("Index creation failed.", e);
        }
    }

    private Log createLog() {
        return new Log() {
            @Override
            public void warn(String str) {
                PublishMojo.this.getLog().warn(str);
            }

            @Override
            public void info(String str) {
                PublishMojo.this.getLog().info(str);
            }

            @Override
            public void verbose(String str) {
                PublishMojo.this.getLog().debug(str);
            }
        };
    }

    private void addLavendelizerJar(World world) throws IOException {
        Node src;
        Node dest;

        src = world.resource("/lavendelizer.jar");
        dest = world.file(libDir).join("lavendelizer.jar");
        src.copyFile(dest);
    }


    private void addCdsServlet(World world, File docroot) throws IOException {
        FileNode fn;
        String endTag = "</web-app>";
        String servletString = "";
        servletString += "<servlet>\n";
        servletString += "<servlet-name>LavendelizeServlet</servlet-name>\n";
        servletString += "<servlet-class>com.oneandone.lavendel.servlet.Cds</servlet-class>\n";
        servletString += "<init-param>\n";
        servletString += "<param-name>docbase</param-name>\n";
        servletString += "<param-value>" + docroot.getCanonicalPath() + "</param-value>\n";
        servletString += "</init-param>\n";
        servletString += "</servlet>\n";
        servletString += "<servlet-mapping>\n";
        servletString += "<servlet-name>LavendelizeServlet</servlet-name>\n";
        servletString += "<url-pattern>/lavendel/*</url-pattern>\n";
        servletString += "</servlet-mapping>\n";
        fn = world.file(webXmlFile);
        String webXmlContent = fn.readString();
        webXmlContent = webXmlContent.replace(endTag, servletString + endTag);
        fn.writeString(webXmlContent);
    }
}
