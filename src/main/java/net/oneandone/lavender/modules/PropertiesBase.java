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

import java.util.Properties;

/** represents module (or application module) properties */
public class PropertiesBase {
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
}
