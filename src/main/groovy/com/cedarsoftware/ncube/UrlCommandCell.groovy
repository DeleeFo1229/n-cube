package com.cedarsoftware.ncube
import groovy.transform.CompileStatic

import java.util.concurrent.atomic.AtomicBoolean
/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 * @author Ken Partlow (kpartlow@gmail.com)
 * <br>
 * Copyright (c) Cedar Software LLC
 * <br><br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <br><br>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br><br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@CompileStatic
public abstract class UrlCommandCell implements CommandCell
{
    private String cmd
    private volatile transient String errorMsg = null
    private String url = null
    private int hash
    public static final char EXTENSION_SEPARATOR = '.'
    private AtomicBoolean hasBeenCached = new AtomicBoolean(false)
    private Object cache
    // would prefer this was a final
    private boolean cacheable

    //  Private constructor only for serialization.
    protected UrlCommandCell() { }

    public UrlCommandCell(String cmd, String url, boolean cacheable)
    {
        if (cmd == null && url == null)
        {
            throw new IllegalArgumentException("Both 'cmd' and 'url' cannot be null")
        }

        if (cmd != null && cmd.isEmpty())
        {   // Because of this, cmdHash() never has to worry about an empty ("") command (when url is null)
            throw new IllegalArgumentException("'cmd' cannot be empty")
        }

        this.cmd = cmd
        this.url = url
        this.cacheable = cacheable
        this.hash = cmd == null ? url.hashCode() : cmd.hashCode()
    }

    public String getUrl()
    {
        return url
    }

    public boolean isCacheable()
    {
        return cacheable
    }

    public void clearClassLoaderCache()
    {
        if (!hasBeenCached.get())
        {
            return
        }

        synchronized (this)
        {
            if (!hasBeenCached.get())
            {
                return
            }

            // classpath case, lets clear all classes before setting to null.
            if (cache instanceof GroovyClassLoader)
            {
                ((GroovyClassLoader)cache).clearCache()
                cache = null
            }

            hasBeenCached.set(false)
        }
    }

    protected URL getActualUrl(Map<String, Object> ctx)
    {
        URL actualUrl
        NCube ncube = getNCube(ctx)
        String localUrl = url.toLowerCase()

        if (localUrl.startsWith('http:') || localUrl.startsWith('https:') || localUrl.startsWith('file:'))
        {   // Absolute URL
            actualUrl = new URL(url)
        }
        else
        {   // Relative URL
            URLClassLoader loader = NCubeManager.getUrlClassLoader(ncube.applicationID, getInput(ctx))

            // Make URL absolute (uses URL roots added to NCubeManager)
            actualUrl = loader.getResource(url)
        }

        if (actualUrl == null)
        {
            String err = "Unable to resolve URL, make sure appropriate resource URLs are added to the sys.classpath cube, URL: " +
                    url + ", cube: " + ncube.name + ", app: " + ncube.applicationID
            setErrorMessage(err)
            throw new IllegalStateException(err)
        }
        return actualUrl
    }

    public static NCube getNCube(Map<String, Object> ctx)
    {
        return (NCube) ctx.ncube
    }

    public static Map getInput(Map<String, Object> ctx)
    {
        return (Map) ctx.input
    }

    public static Map getOutput(Map<String, Object> ctx)
    {
        return (Map) ctx.output
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof UrlCommandCell))
        {
            return false
        }

        UrlCommandCell that = (UrlCommandCell) other

        if (cmd != null)
        {
            return cmd.equals(that.cmd)
        }

        return url.equals(that.getUrl())
    }

    public int hashCode()
    {
        return this.hash
    }

    public String getCmd()
    {
        return cmd
    }

    public String toString()
    {
        return url == null ? cmd : url
    }

    public void failOnErrors()
    {
        if (errorMsg != null)
        {
            throw new IllegalStateException(errorMsg)
        }
    }

    public void setErrorMessage(String msg)
    {
        errorMsg = msg
    }

    public String getErrorMessage()
    {
        return errorMsg
    }

    public int compareTo(CommandCell cmdCell)
    {
        String cmd1 = cmd == null ? '' : cmd
        String cmd2 = cmdCell.getCmd() == null ? '' : cmdCell.getCmd()

        int comp = cmd1.compareTo(cmd2)

        if (comp == 0)
        {
            String url1 = url == null ? '' : url
            String url2 = cmdCell.getUrl() == null ? '' : cmdCell.getUrl()
            return url1.compareTo(url2)
        }

        return comp
    }

    public void getCubeNamesFromCommandText(Set<String> cubeNames)
    {
    }

    public void getScopeKeys(Set<String> scopeKeys)
    {
    }

    public Object execute(Map<String, Object> ctx)
    {
        failOnErrors()

        if (!isCacheable())
        {
            return fetchResult(ctx)
        }

        if (hasBeenCached.get())
        {
            return cache
        }

        synchronized (this)
        {
            if (hasBeenCached.get())
            {
                return cache
            }

            cache = fetchResult(ctx)
            hasBeenCached.set(true)
            return cache
        }
    }

    protected abstract Object fetchResult(Map<String, Object> ctx)
}
