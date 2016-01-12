package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.util.LongHashSet;
import com.cedarsoftware.util.CaseInsensitiveMap;
import com.cedarsoftware.util.StringUtilities;

import java.util.Map;
import java.util.Set;

/**
 * This class represents a binding to a Set of columns, and the associated
 * return value.  It also knows at what depth it occurred in the execution
 * stack.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class Binding
{
    private final String cubeName;
    private final int depth;
    private final Map<String, Column> coord = new CaseInsensitiveMap<>();
    private final Set<Long> idCoord = new LongHashSet();
    private Object value;
    private static final String newLine = "\n";

    public Binding(String cubeName, int stackDepth)
    {
        this.cubeName = cubeName;
        this.depth = stackDepth;
    }

    public String getCubeName()
    {
        return cubeName;
    }

    public void bind(String axisName, Column column)
    {
        coord.put(axisName, column);
        idCoord.add(column.id);
    }

    public Object getValue()
    {
        return value;
    }

    public int getDepth()
    {
        return depth;
    }

    public void setValue(Object value)
    {
        this.value = value;
    }

    public int getNumBoundAxes()
    {
        return idCoord.size();
    }

    public Set<Long> getIdCoordinate()
    {
        return idCoord;
    }

    public Map<String, Column> getCoordinate()
    {
        return coord;
    }

    public String toString()
    {
        return toHtml(false);
    }

    public String toHtml()
    {
        return toHtml(true);
    }

    String toHtml(boolean tagsOK)
    {
        String spaces = padString("    ", getDepth());
        StringBuilder s = new StringBuilder(spaces);
        s.append(cubeName);
        s.append(newLine);
        for (Map.Entry<String, Column> entry : coord.entrySet())
        {
            Column column = entry.getValue();
            s.append(spaces);
            s.append("  ");
            s.append(entry.getKey());
            s.append(": ");
            String name = (String) column.getMetaProperty("name");
            if (StringUtilities.hasContent(name))
            {
                s.append(name);
                s.append(" / ");
            }
            s.append(column.getValue());
            s.append(newLine);
        }

        s.append(spaces);
        if (tagsOK)
        {
            s.append("  <b>value = ");
        }
        else
        {
            s.append("  value = ");
        }
        s.append(value == null ? "null" : value.toString());
        if (tagsOK)
        {
            s.append("</b>");
        }
        return s.toString();
    }

    public static String padString(String string, int length)
    {
        StringBuilder s = new StringBuilder();
        for (int i=0; i < length; i++)
        {
            s.append(string);
        }
        return s.toString();
    }
}
