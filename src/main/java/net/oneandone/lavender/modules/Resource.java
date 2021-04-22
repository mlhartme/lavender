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

import java.io.IOException;
import java.io.OutputStream;

public abstract class Resource {
    /** resource path */
    public abstract String getPath();

    /** something useable as etag; revision for svn; CAUTION: this is not the md5 hash! */
    public abstract String getContentId();

    /** for logging purpose */
    public abstract String getOrigin();

    public abstract void writeTo(OutputStream dest) throws IOException;

    /**
     * Resources that can quickly check if a resource is out-dated should do so here; all other should return true to indicate that it
     * might be outdated
     */
    public abstract boolean isOutdated();

    @Override
    public String toString() {
        return getPath() + " ->" + getOrigin();
    }
}
