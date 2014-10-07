package com.cedarsoftware.ncube.util;

import groovy.lang.GroovyClassLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 *  @author Ken Partlow (kpartlow@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class CdnClassLoader extends GroovyClassLoader
{
    private final boolean _preventRemoteBeanInfo;
    private final boolean _preventRemoteCustomizer;

    /**
     * creates a GroovyClassLoader using the given ClassLoader as parent
     */
    public CdnClassLoader(ClassLoader loader, boolean preventRemoteBeanInfo, boolean preventRemoteCustomizer)
    {
        super(loader, null);
        _preventRemoteBeanInfo = preventRemoteBeanInfo;
        _preventRemoteCustomizer = preventRemoteCustomizer;
    }

    protected Class<?> findClass(final String name) throws ClassNotFoundException
    {
        return isLocalOnlyClass(name) ? super.getParent().loadClass(name) : super.findClass(name);
    }

    boolean isLocalOnlyClass(String name)
    {
        if (name.startsWith("java.lang.") ||
                name.startsWith("java.io.") ||
                name.startsWith("java.net.") ||
                name.startsWith("java.util.") ||
                name.startsWith("java.text.") ||
                name.startsWith("groovy.lang.") ||
                name.startsWith("groovy.util.") ||
                name.startsWith("com.cedarsoftware") ||
                name.startsWith("ncube$grv") ||
                name.startsWith("ncube.grv"))
        {
            return true;
        }

        if (_preventRemoteBeanInfo && name.endsWith("BeanInfo"))
        {
            return true;
        }

        if (_preventRemoteCustomizer && name.endsWith("Customizer"))
        {
            return true;
        }

        return false;
    }

    /**
     * @param name Name of resource
     * @return true if we should only look locally.
     */
    boolean isLocalOnlyResource(String name)
    {
        //  Groovy ASTTransform Service
        if (name.endsWith("org.codehaus.groovy.transform.ASTTransformation"))
        {
            return true;
        }

        if (name.startsWith("META-INF") ||
            name.startsWith("ncube/grv/") ||
            name.startsWith("java/lang/ncube") ||
            name.startsWith("java/io/ncube") ||
            name.startsWith("java/util/ncube") ||
            name.startsWith("java/net/ncube") ||
            name.startsWith("com/cedarsoftware/"))
        {
            if (name.startsWith("ncube/grv/closure/NCubeTemplateClosures"))
            {
                return false;
            }
            return true;
        }

        if (_preventRemoteBeanInfo)
        {
            if (name.endsWith("BeanInfo.groovy"))
            {
                return true;
            }
        }

        if (_preventRemoteCustomizer)
        {
            if (name.endsWith("Customizer.groovy"))
            {
                return true;
            }
        }

        return false;
    }

    public Enumeration<URL> getResources(String name) throws IOException
    {
        if (isLocalOnlyResource(name))
        {
            return new Enumeration<URL>()
            {
                public boolean hasMoreElements()
                {
                    return false;
                }

                public URL nextElement()
                {
                    throw new NoSuchElementException();
                }
            };
        }
        return super.getResources(name);
    }

    public URL getResource(String name)
    {
        if (isLocalOnlyResource(name))
        {
            return null;
        }
        return super.getResource(name);
    }
}
