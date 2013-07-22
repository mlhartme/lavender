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
package net.oneandone.lavender.config;

import net.oneandone.sushi.util.Separator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class Filter {
    public static Filter forProperties(Properties properties, String prefix, List<String> defaultIncludeExtensions) {
        return new Filter(
                get(properties, prefix + ".includeExtensions", defaultIncludeExtensions),
                get(properties, prefix + ".excludeExtensions", null));
    }

    private static List<String> get(Properties properties, String key, List<String> dflt) {
        String str;
        List<String> extensions;
        List<String> result;

        str = properties.getProperty(key);
        extensions = str == null ? dflt : Separator.COMMA.split(str);
        if (extensions == null) {
            return null;
        }
        result = new ArrayList<>();
        for (String extension : extensions) {
            result.add("*." + extension);
        }
        return result;
    }

    private Collection<String> includes;
    private Collection<String> excludes;

    public Filter() {
        this(null, null);
    }

    public Filter(Collection<String> includes, Collection<String> excludes) {
        this.includes = includes;
        this.excludes = excludes;
    }

    public Collection<String> getIncludes() {
        return includes;
    }

    public void setIncludes(Collection<String> includes) {
        this.includes = includes;
    }

    public void setIncludes(String... includes) {
        this.includes = Arrays.asList(includes);
    }

    public Collection<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(Collection<String> excludes) {
        this.excludes = excludes;
    }

    public void setExcludes(String... excludes) {
        this.excludes = Arrays.asList(excludes);
    }

    @Override
    public String toString() {
        return String.format("Configuration [includes=%s, excludes=%s]", includes, excludes);
    }

    public boolean isIncluded(String path) {
        if (includes != null) {
            boolean isIncluded = matches(path, includes);
            if (!isIncluded) {
                return false;
            }
        }

        if (excludes != null) {
            boolean isExcluded = matches(path, excludes);
            if (isExcluded) {
                return false;
            }
        }

        return true;
    }

    private static boolean matches(String path, Collection<String> patterns) {
        for (String pattern : patterns) {
            if (SelectorUtils.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
