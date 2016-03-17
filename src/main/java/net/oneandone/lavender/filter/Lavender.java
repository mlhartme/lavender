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
package net.oneandone.lavender.filter;

import net.oneandone.sushi.fs.ExistsException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A servlet delegate that <em>lavendelizes</em> the response content in production mode.
 * In development mode, it delivers resources.
 */
public class Lavender implements Filter, LavenderMBean {
    private static final Logger LOG = LoggerFactory.getLogger(Lavender.class);

    public static final String LAVENDER_IDX = "WEB-INF/lavender.idx";
    public static final String LAVENDER_NODES = "WEB-INF/lavender.nodes";

    private AtomicReference<Filter> delegate = new AtomicReference<>();

    /** The delegate configuration. */
    protected FilterConfig filterConfig;

    @Override
    public void init(FilterConfig config) throws ServletException {
        filterConfig = config;

        try {
            LOG.info("Start initializing Lavender delegate");
            loadFilter();
        } catch (ExistsException e) {
            e.printStackTrace();
            throw new ServletException("Could not initialize Lavender delegate", e);
        } catch (RuntimeException se) {
            se.printStackTrace();
            LOG.error("Error in Lavender.init()", se);
            throw se;
        }

        registerMBean();
    }

    private void loadFilter() throws ExistsException, ServletException {
        Filter filter = createFilter();
        filter.init(filterConfig);

        setFilter(filter);
    }

    private void setFilter(Filter filter) {
        Filter previousFilter = delegate.get();
        delegate.set(filter);

        if (previousFilter != null) {
            previousFilter.destroy();
        }
    }

    private Filter createFilter() throws ExistsException, ServletException {
        Filter filter;
        if (useProductionFilter()) {
            filter = createProductionFilter();
        } else {
            filter = createDevelopmentFilter();
        }
        return filter;
    }

    Filter createProductionFilter() throws ServletException {
        return new ProductionFilter();
    }

    Filter createDevelopmentFilter() throws ServletException {
        return new DevelopmentFilter();
    }

    private boolean useProductionFilter() throws ExistsException {
        // do not try ssh agent, because it crashes )with an abstract method error)
        // when jna is not in version 3.4.0. Which happens easily ...
        World world = new World(false);

        String contextPath = filterConfig.getServletContext().getRealPath("");
        Node indexSource = world.file(contextPath).join(LAVENDER_IDX);
        return indexSource.exists();
    }

    private void registerMBean() {
        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(this,
                    new ObjectName("net.oneandone:type=Lavender,name=" + UUID.randomUUID().toString()));
        } catch (InstanceAlreadyExistsException | NotCompliantMBeanException | MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        } catch (MBeanRegistrationException e) {
            LOG.error("MBean initialization failure", e);
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        delegate.get().doFilter(request, response, chain);
    }

    public boolean getProd() {
        return delegate.get() instanceof ProductionFilter;
    }

    public int getModules() {
        Filter filter = delegate.get();
        if (filter instanceof DevelopmentFilter) {
            return ((DevelopmentFilter) filter).getModulesCount();
        }
        return -1;
    }

    @Override
    public void reload() {
        try {
            loadFilter();
        } catch (ExistsException | ServletException e) {
            LOG.error("Could not reload filter", e);
        }
    }


    @Override
    public void destroy() {
        delegate.get().destroy();
    }

}
