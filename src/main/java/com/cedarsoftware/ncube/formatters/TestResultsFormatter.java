package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.RuleInfo;
import groovy.util.MapEntry;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements an n-cube.  This is a hyper (n-dimensional) cube
 * of cells, made up of 'n' number of axes.  Each Axis is composed
 * of Columns that denote discrete nodes along an axis.  Use NCubeManager
 * to manage a list of NCubes.  Documentation on Github.
 *
 * @author Ken Partlow (kpartlow@gmail.com)
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
public class TestResultsFormatter
{
    private Map output;
    private StringBuilder builder = new StringBuilder();
    private static final String newLine = "\n";

    public TestResultsFormatter(Map out)
    {
        this.output = out;
    }

    public String format()
    {
        formatLastExecutedStatement();
        formatAssertions();
        formatOutputMap();
        formatSystemOut("System.out");
        formatSystemOut("System.err");

        RuleInfo info = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        format(info.getRuleExecutionTrace());

        return builder.toString();
    }

    public void format(List<MapEntry> trace)
    {
        builder.append("<b>Trace</b>");
        builder.append("<pre>");
        builder.append(newLine);
        StringBuilder spaces = new StringBuilder("   ");
        for (MapEntry entry : trace)
        {
            if (entry.getValue() instanceof Map)
            {
                ((Map)entry.getValue()).remove("ncube");
            }

            boolean end = isEnd(entry.getKey());
            boolean begin = isBegin(entry.getKey());

            if (end)
            {
                spaces.setLength(spaces.length()-3);
            }

            builder.append(spaces);
            builder.append(entry.getKey());

            if (begin)
            {
                spaces.append("   ");
                builder.append("(");
                turnMapIntoCoords((Map<String, Object>) entry.getValue());
                builder.append(")");
            }
            else
            {
                builder.append(" = ");
                builder.append(entry.getValue());
            }

            builder.append(newLine);
        }
        builder.setLength(builder.length()-1);
        builder.append("</pre>");
    }

    public static boolean isBegin(Object o)
    {
        return o instanceof String && ((String)o).startsWith("begin:");
    }

    public static boolean isEnd(Object o)
    {
        return o instanceof String && ((String)o).startsWith("end:");
    }

    public void turnMapIntoCoords(Map<String, Object> map)
    {
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            builder.append(entry.getKey());
            builder.append(":");
            builder.append(entry.getValue());
            builder.append(",");
        }
        builder.setLength(builder.length()-1);
    }

    public void formatLastExecutedStatement()
    {
        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        builder.append("<b>Last executed statement</b>");
        builder.append("<pre>");
        builder.append(newLine);
        builder.append("   ");
        builder.append(ruleInfo.getLastExecutedStatementValue());
        builder.append(newLine);
        builder.append("</pre>");
    }

    public void formatAssertions()
    {
        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        builder.append("<b>Assertions</b>");
        builder.append("<pre>");
        builder.append(newLine);

        Set<String> failures = ruleInfo.getAssertionFailures();
        if (failures.isEmpty())
        {
            builder.append("No assertion failures");
            builder.append(newLine);
        }
        else
        {
            for (String entry : failures)
            {
                builder.append("   ");
                builder.append(entry);
                builder.append(newLine);
            }
            builder.setLength(builder.length()-1);
        }
        builder.append("</pre>");
    }

    public void formatOutputMap()
    {
        builder.append("<b>Output Map</b>");
        builder.append("<pre>");
        builder.append(newLine);

        if (output.size() < 2)
        {   // size() == 1 minimum (_rule metakey).
            builder.append("   No output");
            builder.append(newLine);
        }
        else
        {
            for (Object o : output.entrySet())
            {
                Map.Entry item = (Map.Entry) o;

                final Object key = item.getKey();
                if (NCube.RULE_EXEC_INFO.equals(key))
                {
                    continue;
                }
                builder.append("   ");
                builder.append(item.getKey());
                builder.append(" = ");
                builder.append(item.getValue());
                builder.append(newLine);
            }
        }
        builder.append("</pre>");
    }

    public void formatSystemOut(String section)
    {
        boolean isErr = section.toLowerCase().contains("err");
        builder.append("<b>");
        builder.append(section);
        builder.append("</b>");
        if (isErr)
        {
            builder.append("<pre style=\"color:darkred\">");
        }
        else
        {
            builder.append("<pre>");
        }
        builder.append(newLine);

        RuleInfo ruleInfo = (RuleInfo) output.get(NCube.RULE_EXEC_INFO);
        if (isErr)
        {
            builder.append(ruleInfo.getSystemErr());
        }
        else
        {
            builder.append(ruleInfo.getSystemOut());
        }
        builder.append("</pre>");
    }
}
