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
package net.oneandone.lavender.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/** represents module (or application module) properties */
public abstract class PropertiesBase {
    public static boolean eatBoolean(Properties p, String key, boolean dflt) {
        String result;

        result = eatOpt(p, key, null);
        if (result == null) {
            return dflt;
        }
        switch (result) {
            case "true": return true;
            case "false": return false;
            default: throw new IllegalArgumentException("true or false expected, got " + result);
        }
    }

    public static String eat(Properties p, String key) {
        String result;

        result = eatOpt(p, key, null);
        if (result == null) {
            throw new IllegalArgumentException("key not found: " + key);
        }
        return result;
    }

    public static String eatOpt(Properties p, String key, String dflt) {
        String result;

        result = (String) p.remove(key);
        return result == null ? dflt : result;
    }

    public static Map<String, String> eatIndex(Properties p) {
        final String prefix = "index.";
        final int length = prefix.length();
        Map<String, String> map;
        Iterator<Map.Entry<Object, Object>> iter;
        Map.Entry<Object, Object> entry;
        String key;

        map = new HashMap<>();
        iter = p.entrySet().iterator();
        while (iter.hasNext()) {
            entry = iter.next();
            key = (String) entry.getKey();
            if (key.startsWith(prefix)) {
                map.put(key.substring(length), (String) entry.getValue());
            }
        }
        for (String suffix : map.keySet()) {
            if (p.remove(prefix + suffix) == null) {
                throw new IllegalStateException();
            }
        }
        return map;
    }
}
