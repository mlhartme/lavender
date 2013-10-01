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
import net.oneandone.sushi.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class LavenderProperties {
    private static final Logger LOG = LoggerFactory.getLogger(LavenderProperties.class);

    public static Collection<SvnModuleConfig> parse(Properties properties) {
        return parse(properties, DefaultModule.DEFAULT_INCLUDES);
    }

    public static Collection<SvnModuleConfig> parse(Properties properties, List<String> defaultIncludes) {
        String key;
        String value;
        String name;
        SvnModuleConfig config;
        Map<String, SvnModuleConfig> result;

        result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            key = (String) entry.getKey();
            if (key.startsWith(SvnModuleConfig.SVN_PREFIX)) {
                key = key.substring(SvnModuleConfig.SVN_PREFIX.length());
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
                    config = new SvnModuleConfig(name, DefaultModule.filterForProperties(properties, SvnModuleConfig.SVN_PREFIX + name, defaultIncludes));
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
                        config.resourcePathPrefix = value;
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
                    } else if (key.equals("livePath")) {
                        config.livePath = value;
                    }
                }
            }
        }
        return result.values();
    }

}
