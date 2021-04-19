Changelog: https://github.com/mlhartme/lavender/blob/master/CHANGELOG.md

# Lavender

Lavender is a tool set for static resources. It enables you to serve static resources for your Java Web Applications via dedicated servers.

Lavender has two components:
* a _publisher_ to upload resources to your dedicated servers
* a _servlet filter_ to rewrite resource references

The typical scenario is this:

You create a new Java Web Application, packaging static resources into the respective war file. Fine. Later, you want to optimize resource handling in your application, you want to use dedicated servers that support
* parallel requests
* far-future expires headers

You start using lavender:
* you setup dedicated servers
* you add the publishing tool to your deployment process: resources from the war file will be synced to the docroot of your servers
* you add the servlet filter to your application: references to your resource (e.g. images tags in html) will be re-written to point to your servers.

Finally, you can shrink your war files by not packaging resources into it; the publisher can pick resources from svn instead of the war file.

TODO: currently, lavender supports Pustefix Applications only.

## Setup

Add Lavender to your `pom.xml`:

     <dependency>
       <groupId>net.oneandone</groupId>
       <artifactId>lavender</artifactId>
       <version>2.7.0</version>
     </dependency>

Add the Servlet Filter to your `web.xml`:

      <filter>
        <filter-name>Lavender</filter-name>
        <filter-class>net.oneandone.lavender.filter.Lavender</filter-class>
      </filter>
      <filter-mapping>
        <filter-name>Lavender</filter-name>
        <url-pattern>/*</url-pattern>
      </filter-mapping>

Add module `WEB-INF/lavender.properties`.

Optionally, you can configure logging for the category `net.oneandone.lavender`.

## Properties

There are two types of property file in Lavender - host.properties and lavender.properties.

### Lavender Properties

The `lavender.properties` file is located in war or jar files and defines external resource paths for resources, typically subversion repositories. Lavender scans the war with its jar for module `lavender.properties` files and extracts files from the war/jar and optional from external resource paths to publish the files on the target cluster.

An example module `lavender.properties` file looks like this

    ## Include everything from JAR file below htdocs
    pustefix.relative=jar
    # pustefix.excludes=**/*

    ## To include resources from subversion uncomment following lines
    # svn.jar-module=scm:svn:http://localhost:7181/svn/jar-module/tags/jar-module-1.0.0/src/main/resources
    # svn.jar-module.devel=scm:svn:http://localhost:7181/svn/jar-module/trunk/src/main/resources
    # svn.jar-module.includes=**/*.gif, **/*.png, **/*.jpg, **/*.jpeg, **/*.pdf
    # svn.jar-module.resourcePathPrefix=modules/jar-module/
    # svn.jar-module.relative=

For war files the module `lavender.properties` file is located in `WEB-INF/lavender.properties`, for jar files it is located in `META-INF/lavender.properties`.

### Host Properties

Lavender's `host.properties` defines the network- and secrets configuration to use.

    ## host.properties file
    network=svn:https://svn.1and1.org/svn/sales/tools/lavender/config/network.xml
    secrets=~/.fault/com.oneandone.sales:shop-wars/lavender.secrets

Bitbucket access uses ssh with username/password. You define a repository like this:

    myrepo=git:ssh://git@bitbucket.your-company.com/yourproject/
    myrepo.username=foo
    myrepo.password=bar
