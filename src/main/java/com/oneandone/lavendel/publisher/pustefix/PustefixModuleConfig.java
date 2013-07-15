package com.oneandone.lavendel.publisher.pustefix;

import com.oneandone.lavendel.publisher.pustefix.module.ModuleDescriptorType;
import com.oneandone.lavendel.publisher.pustefix.module.ResourceMappingType;
import com.oneandone.lavendel.publisher.pustefix.module.StaticType;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * META-INF/pustefix-module.xml.
 */
public class PustefixModuleConfig {

    private PustefixProjectConfig projectConfig;
    private ModuleDescriptorType config;

    public PustefixModuleConfig(PustefixProjectConfig projectConfig, ZipInputStream jarInputStream)
            throws JAXBException, IOException {
        this.projectConfig = projectConfig;
        loadModuleXml(jarInputStream);
    }

    public String getModuleName() {
        return config.getModuleName();
    }

    /**
     * Checks if the given resource is public.
     * @param resourceName
     *            the resource name
     * @return true if the resource is public
     */
    public boolean isPublicResource(String resourceName) {
        if (getStaticMapped(resourceName) != null) {
            return true;
        }
        if (!hasResourceMapping(resourceName)) {
            return false;
        }

        String mappedName = getPath(resourceName);
        return projectConfig.isPublicResource(mappedName);
    }

    private static final String MODULES = "modules/";

    /**
     * Maps the name within the JAR module to the Example: 'PUSTEFIX-INF/img/close.gif' is mapped to
     * 'modules/stageassistent/img/close.gif'.
     * @param resourceName
     *            the resource name
     * @return the mapped name
     */
    public String getPath(String resourceName) {
        String st;

        st = getStaticMapped(resourceName);
        if (st != null) {
            return st;
        }
        if (!hasResourceMapping(resourceName)) {
            throw new IllegalArgumentException(resourceName);
        }

        StringBuilder sb = new StringBuilder();

        sb.append(MODULES);

        sb.append(getModuleName());
        sb.append('/');

        sb.append(getResourceMapping(resourceName));

        return sb.toString();
    }

    private static final String PUSTEFIX_INF = "PUSTEFIX-INF/";

    private String getStaticMapped(String resourceName) {
        if (resourceName.startsWith("/")) {
            throw new IllegalArgumentException(resourceName);
        }
        if (!resourceName.startsWith(PUSTEFIX_INF)) {
            return null;
        }
        resourceName = resourceName.substring(PUSTEFIX_INF.length());
        StaticType st = config.getStatic();
        if (st != null) {
            for (String path : st.getPath()) {
                path = path.trim();
                if (path.isEmpty() || path.startsWith("/")) {
                    throw new IllegalStateException(path);
                }
                if (!path.endsWith("/")) {
                    path = path + "/";
                }
                if (resourceName.startsWith(path)) {
                    return MODULES + getModuleName() + "/" + resourceName;
                }
            }
        }
        return null;
    }

    private boolean hasResourceMapping(String resourceName) {
        return getResourceMapping(resourceName) != null;
    }

    private String getResourceMapping(String resourceName) {
        String name = null;
        if (config.getResources() != null) {
            List<ResourceMappingType> resourceMappings = config.getResources().getResourceMapping();
            for (ResourceMappingType resourceMapping : resourceMappings) {
                String src = resourceMapping.getSrcpath();
                String target = resourceMapping.getTargetpath();
                if (src != null && target != null && resourceName.startsWith(src)) {
                    name = resourceName.replace(src, target);
                }
            }
        }
        return name;
    }

    private void loadModuleXml(InputStream jarInputStream) throws JAXBException, IOException {
        // Use a shield to prevent the original stream from being closed
        // because JAXB.unmarshall() calls close() on the stream
        config = JAXB.unmarshal(doNotClose(jarInputStream), ModuleDescriptorType.class);
    }

    //--

    public static InputStream doNotClose(InputStream dest) {
        return new FilterInputStream(dest) {
            @Override
            public void close() {
                // do nothing
            }
        };
    }
}
