package net.oneandone.lavender.filter;

import net.oneandone.lavender.config.Properties;
import net.oneandone.lavender.index.Hex;
import net.oneandone.lavender.modules.DefaultModule;
import net.oneandone.lavender.modules.Module;
import net.oneandone.lavender.modules.Resource;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.xml.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Created by awiegant on 04.03.16.
 */
public class DevelopmentFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(ProductionFilter.class);

    FilterConfig filterConfig;

    World world;
    protected List<Module> modules;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        long started = System.currentTimeMillis();
        Properties properties = null;
        Node webapp;

        this.filterConfig = filterConfig;

        try {
            world = new World(false);
            webapp = world.file(filterConfig.getServletContext().getRealPath(""));
            properties = Properties.load(Properties.file(world), false);
            FileNode cache = properties.lockedCache(5, "lavenderServlet");
            modules = loadModulesFromWebapp(webapp, properties, cache);
        } catch (XmlException | IOException | SAXException e) {
            e.printStackTrace();
            throw new ServletException("Could not initialize Lavender development filter", e);
        } finally {
            unlockPropertiesCache(properties);
        }
        LOG.info("Lavender development filter for " + webapp + ", " + modules.size()
                + " resources. Init in " + (System.currentTimeMillis() - started + " ms"));

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (!intercept(request, response)) {
            chain.doFilter(request, response);
            LOG.debug("[passed through: " + request.getMethod() + " " + request.getRequestURI() + ": " + response.getStatus() + "]");
        }
    }

    @Override
    public void destroy() {
        if (modules != null) {
            for (Module module : modules) {
                try {
                    module.saveCaches();
                } catch (IOException e) {
                    LOG.error("cannot save caches for " + module.getName() + ": " + e.getMessage(), e);
                }
            }
        }
    }

    public int getModulesCount() {
        return modules.size();
    }

    List<Module> loadModulesFromWebapp(Node webapp, Properties properties, FileNode cache)
            throws IOException, SAXException, XmlException {
        return DefaultModule.fromWebapp(cache, false, webapp, properties.svnUsername, properties.svnPassword);
    }

    private void unlockPropertiesCache(Properties properties) {
        if (properties != null) {
            try {
                properties.unlockCache();
            } catch (IOException e) {
                LOG.error("Could not unlock properties cache", e);
            }
        }
    }

    public boolean intercept(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String path;
        Resource resource;

        path = request.getPathInfo();
        if (path == null || !path.startsWith("/")) {
            return false;
        }
        path = path.substring(1);
        resource = lookup(path);
        if (resource == null) {
            return false;
        }

        switch (request.getMethod()) {
            case "GET":
                doGetRequest(resource, request, response, true);
                LOG.info(response.getStatus() + " GET " + path + " -> " + resource.getOrigin());
                return true;
            case "HEAD":
                doGetRequest(resource, request, response, false);
                LOG.info(response.getStatus() + " HEAD " + path + " -> " + resource.getOrigin());
                return true;
            default:
                return false;
        }
    }

    private synchronized Resource lookup(String resourcePath) throws IOException {
        Resource resource;

        // lookup cached stuff first
        for (Module module : modules) {
            if (module.hasFiles()) {
                resource = module.probe(resourcePath);
                if (resource != null) {
                    if (resource.isOutdated()) {
                        LOG.info(resource.getOrigin() + ": outdated");
                    } else {
                        return resource;
                    }
                }
            }
        }
        for (Module module : modules) {
            if (module.matches(resourcePath) != null) {
                module.softInvalidate();
                resource = module.probe(resourcePath);
                if (resource != null) {
                    return resource;
                }
            }
        }
        return null;
    }


    public void doGetRequest(Resource resource, HttpServletRequest request, HttpServletResponse response, boolean withBody) throws IOException {
        String etag;
        String contentType;
        long contentLength;
        ServletOutputStream out;
        byte[] data;
        String previousEtag;

        etag = Hex.encodeString(resource.getMd5());
        response.setDateHeader("Last-Modified", resource.getLastModified());
        response.setHeader("ETag", etag);
        contentType = filterConfig.getServletContext().getMimeType(resource.getPath());
        if (contentType != null) {
            response.setContentType(contentType);
        }

        previousEtag = request.getHeader("If-None-Match");
        if (etag.equals(previousEtag)) {
            LOG.debug("ETag match: returning 304 Not Modified: " + resource.getPath());
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
        } else  { 		// first time through - set last modified time to now
            data = resource.getData();
            contentLength = data.length;
            if (contentLength >= Integer.MAX_VALUE) {
                throw new IOException(resource.getPath() + ": resource too big: " + contentLength);
            }
            if (withBody) {
                response.setContentLength((int) contentLength);
                out = response.getOutputStream();
                try {
                    response.setBufferSize(4096);
                } catch (IllegalStateException e) {
                    // Silent catch
                }
                out.write(data);
            }
        }
    }

}
