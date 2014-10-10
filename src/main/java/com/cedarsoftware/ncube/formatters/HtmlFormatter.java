package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.AxisType;
import com.cedarsoftware.ncube.CellInfo;
import com.cedarsoftware.ncube.Column;
import com.cedarsoftware.ncube.CommandCell;
import com.cedarsoftware.ncube.GroovyBase;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.proximity.LatLon;
import com.cedarsoftware.ncube.proximity.Point2D;
import com.cedarsoftware.ncube.proximity.Point3D;
import com.cedarsoftware.util.CaseInsensitiveMap;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.abs;

/**
 * Format an NCube into an HTML document
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
public class HtmlFormatter implements NCubeFormatter
{
    String[] _headers;

    public HtmlFormatter(String... headers)
    {
        _headers = headers;
    }

    /**
     * Calculate important values needed to display an NCube.
     *
     * @return Object[], where element 0 is a List containing the axes
     * where the first axis (element 0) is the axis to be displayed at the
     * top and the rest are the axes sorted smallest to larges.  Element 1
     * of the returned object array is the height of the cells (how many
     * rows it would take to display the entire ncube). Element 2 is the
     * width of the cell matrix (the number of columns would it take to display
     * the cell portion of the NCube).
     */
    protected Object[] getDisplayValues(NCube ncube)
    {
        if (_headers == null)
        {
            _headers = new String[]{};
        }
        Map headerStrings = new CaseInsensitiveMap();
        for (String header : _headers)
        {
            headerStrings.put(header, null);
        }
        // Step 1. Sort axes from smallest to largest.
        // Hypercubes look best when the smaller axes are on the inside, and the larger axes are on the outside.
        List<Axis> axes = new ArrayList<Axis>(ncube.getAxes());
        Collections.sort(axes, new Comparator<Axis>()
        {
            public int compare(Axis a1, Axis a2)
            {
                return a2.size() - a1.size();
            }
        });

        // Step 2.  Now find an axis that is a good candidate for the single (top) axis.  This would be an axis
        // with the number of columns closest to 12.
        int smallestDelta = Integer.MAX_VALUE;
        int candidate = -1;
        int count = 0;

        for (Axis axis : axes)
        {
            if (headerStrings.keySet().contains(axis.getName()))
            {
                candidate = count;
                break;
            }
            int delta = abs(axis.size() - 12);
            if (delta < smallestDelta)
            {
                smallestDelta = delta;
                candidate = count;
            }
            count++;
        }

        // Step 3. Compute cell area size
        Axis top = axes.remove(candidate);
        axes.add(0, top);   // Top element is now first.
        top = axes.remove(0);   // Grab 1st (candidate axis) one more time
        if (top.getType() == AxisType.RULE)
        {   // If top is a rule axis, place it last.  It is recognized that there could
            // be more than one rule axis, and there could also be a single rule axis, in
            // which this is a no-op.
            axes.add(top);
        }
        else
        {
            axes.add(0, top);
        }
        long width = axes.get(0).size();
        long height = 1;
        final int len = axes.size();

        for (int i = 1; i < len; i++)
        {
            height = axes.get(i).size() * height;
        }

        return new Object[]{axes, height, width};
    }

    /**
     * Use this API to generate an HTML view of this NCube.
     * matches one of the passed in headers, will be the axis chosen to be displayed at the top.
     *
     * @return String containing an HTML view of this NCube.
     */
    public String format(NCube ncube)
    {
        if (ncube.getAxes().size() < 1)
        {
            return getNoAxisHtml();
        }

        String html = getHtmlPreamble(ncube);

        StringBuilder s = new StringBuilder();
        Object[] displayValues = getDisplayValues(ncube);
        List<Axis> axes = (List<Axis>) displayValues[0];
        long height = (Long) displayValues[1];
        long width = (Long) displayValues[2];

        s.append(html);

        // Top row (special case)
        Axis topAxis = axes.get(0);
        List<Column> topColumns = topAxis.getColumns();
        final int topColumnSize = topColumns.size();
        final String topAxisName = topAxis.getName();

        if (axes.size() == 1)
        {   // Ensure that one dimension is vertically down the page
            s.append(" <th data-id=\"a").append(topAxis.getId()).append("\" class=\"th-ncube ncube-head\">");
            s.append("  <div class=\"btn-group axis-menu\" data-id=\"").append(topAxisName).append("\">\n");
            s.append("   <button type=\"button\" class=\"btn-sm btn-primary dropdown-toggle axis-btn\" data-toggle=\"dropdown\">");
            s.append("    <span>").append(topAxisName).append("</span><span class=\"caret\"></span>");
            s.append("   </button></th>\n");
            s.append("  </div>\n");
            s.append(" <th class=\"th-ncube ncube-dead\">");
            s.append(ncube.getName());
            s.append("</th>\n");
            s.append("</tr>\n");

            for (int i = 0; i < width; i++)
            {
                s.append("<tr>\n");
                Column column = topColumns.get(i);
                boolean isCmd = isColumnInlineExpression(column);
                String colId = String.valueOf(column.getId());
                s.append(" <th data-id=\"").append(colId);
                s.append("\" data-axis=\"").append(topAxisName).append("\" class=\"th-ncube ");
                s.append(getColumnCssClass(topAxis, column));
                s.append("\">");
                if (isCmd)
                {
                    s.append("<pre class=\"ncube-pre\">");
                }
                addColumnPrefixText(s, column);
                s.append(column.isDefault() ? "Default" : column.toString());
                if (isCmd)
                {
                    s.append("</pre>");
                }
                s.append("</th>\n");
                Set<Long> colIds = new LinkedHashSet<>();
                colIds.add(column.getId());
                buildCell(ncube, s, colIds);
                s.append("</tr>\n");
            }
        }
        else
        {   // 2D+ shows as one column on the X axis and all other dimensions on the Y axis.
            int deadCols = axes.size() - 1;
            if (deadCols > 0)
            {
                s.append(" <th class=\"th-ncube ncube-dead\" colspan=\"").append(deadCols).append("\">");
                s.append(ncube.getName());
                s.append("</th>\n");
            }
            s.append(" <th data-id=\"a").append(topAxis.getId()).append("\" class=\"th-ncube ncube-head\" colspan=\"");
            s.append(topAxis.size());
            s.append("\">");
            s.append("  <div class=\"btn-group axis-menu\" data-id=\"").append(topAxisName).append("\">\n");
            s.append("  <button type=\"button\" class=\"btn-sm btn-primary dropdown-toggle axis-btn\" data-toggle=\"dropdown\">");
            s.append("   <span>").append(topAxisName).append("</span><span class=\"caret\"></span>");
            s.append("  </button>\n");
            s.append("   </div>\n");
            s.append(" </th>\n</tr>\n");

            // Second row (special case)
            s.append("<tr>\n");
            Map<String, Long> rowspanCounter = new HashMap<>();
            Map<String, Long> rowspan = new HashMap<>();
            Map<String, Long> columnCounter = new HashMap<>();
            Map<String, List<Column>> columns = new HashMap<>();

            final int axisCount = axes.size();

            for (int i = 1; i < axisCount; i++)
            {
                Axis axis = axes.get(i);
                String axisName = axis.getName();
                s.append(" <th data-id=\"a").append(axis.getId()).append("\" class=\"th-ncube ncube-head\">\n");
                s.append("  <div class=\"btn-group axis-menu\" data-id=\"").append(axisName).append("\">\n");
                s.append("   <button type=\"button\" class=\"btn-sm btn-primary dropdown-toggle axis-btn\" data-toggle=\"dropdown\">");
                s.append("    <span>").append(axisName).append("</span><span class=\"caret\"></span>");
                s.append("   </button>\n");
                s.append("   </div>\n");
                s.append(" </th>\n");
                long colspan = 1;

                for (int j = i + 1; j < axisCount; j++)
                {
                    colspan *= axes.get(j).size();
                }

                rowspan.put(axisName, colspan);
                rowspanCounter.put(axisName, 0L);
                columnCounter.put(axisName, 0L);
                columns.put(axisName, axis.getColumns());
            }

            for (Column column : topColumns)
            {
                boolean isCmd = isColumnInlineExpression(column);
                String colId = String.valueOf(column.getId());
                s.append(" <th data-id=\"").append(colId).append("\" data-axis=\"").append(topAxisName).append("\" class=\"th-ncube-top ");
                s.append(getColumnCssClass(topAxis, column));
                s.append("\">");
                if (isCmd)
                {
                    s.append("<pre class=\"ncube-pre\">");
                }
                addColumnPrefixText(s, column);
                s.append(column.toString());
                if (isCmd)
                {
                    s.append("</pre>");
                }
                s.append("</th>\n");
            }

            if (topAxis.size() != topColumnSize)
            {
                s.append(" <th class=\"th-ncube-top ");
                s.append(getColumnCssClass(topAxis, topAxis.getDefaultColumn()));
                s.append("\">Default</th>");
            }

            s.append("</tr>\n");
            Map<String, Long> coord = new HashMap<>();
            Map<String, Long> colIds = new HashMap<>();

            // The left column headers and cells
            for (long h = 0; h < height; h++)
            {
                s.append("<tr>\n");
                // Column headers for the row
                for (int i = 1; i < axisCount; i++)
                {
                    Axis axis = axes.get(i);
                    String axisName = axis.getName();
                    Long count = rowspanCounter.get(axisName);

                    if (count == 0)
                    {
                        Long colIdx = columnCounter.get(axisName);
                        Column column = columns.get(axisName).get(colIdx.intValue());
                        coord.put(axisName, colIdx);
                        colIds.put(axisName, column.getId());
                        long span = rowspan.get(axisName);

                        String columnId = String.valueOf(column.getId());
                        String colCssClass = getColumnCssClass(axis, column);
                        if (span == 1)
                        {   // drop rowspan tag since rowspan="1" is redundant and wastes space in HTML
                            // Use column's ID as TH element's ID
                            s.append(" <th data-id=\"").append(columnId).append("\" data-axis=\"").append(axisName).append("\" class=\"th-ncube ");
                            s.append(colCssClass);
                        }
                        else
                        {   // Need to show rowspan attribute
                            // Use column's ID as TH element's ID
                            s.append(" <th data-id=\"").append(columnId).append("\" data-axis=\"").append(axisName).append("\" class=\"th-ncube ");
                            s.append(colCssClass);
                            s.append("\" rowspan=\"");
                            s.append(span);
                        }
                        s.append("\">");
                        boolean isCmd = isColumnInlineExpression(column);

                        if (isCmd)
                        {
                            s.append("<pre class=\"ncube-pre\">");
                        }
                        addColumnPrefixText(s, column);
                        s.append(column.toString());
                        if (isCmd)
                        {
                            s.append("</pre>");
                        }
                        s.append("</th>\n");

                        // Increment column counter
                        colIdx++;
                        if (colIdx >= axis.size())
                        {
                            colIdx = 0L;
                        }
                        columnCounter.put(axisName, colIdx);
                    }
                    // Increment row counter (counts from 0 to rowspan of subordinate axes)
                    count++;
                    if (count >= rowspan.get(axisName))
                    {
                        count = 0L;
                    }
                    rowspanCounter.put(axisName, count);
                }

                // Cells for the row
                for (int i = 0; i < width; i++)
                {
                    Column column = topColumns.get(i);
                    colIds.put(topAxisName, column.getId());
                    coord.put(topAxisName, (long)i);
                    // Other coordinate values are set above this for-loop
                    buildCell(ncube, s, new LinkedHashSet<>(colIds.values()));
                }

                s.append("</tr>\n");
            }
        }

        s.append("</table>\n");
        s.append("</body>\n");
        s.append("</html>");
        return s.toString();
    }

    private static String getHtmlPreamble(NCube ncube)
    {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                " <meta charset=\"UTF-8\">\n" +
                " <title>NCube: " + ncube.getName() + "</title>\n" +
                " <style>\n" +
                ".table-ncube\n" +
                "{\n" +
                "border-collapse:collapse;\n" +
                "border:1px solid lightgray;\n" +
                "font-family: \"arial\",\"helvetica\", sans-serif;\n" +
                "font-size: small;\n" +
                "padding: 2px;\n" +
                "}\n" +
                ".td-ncube .th-ncube .th-ncube-top\n" +
                "{\n" +
                "border:1px solid lightgray;\n" +
                "font-family: \"arial\",\"helvetica\", sans-serif;\n" +
                "font-size: small;\n" +
                "padding: 2px;\n" +
                "}\n" +
                ".td-ncube\n" +
                "{\n" +
                "color: black;\n" +
                "background: white;\n" +
                "text-align: center;\n" +
                "}\n" +
                ".th-ncube\n" +
                "{\n" +
                "color: white;\n" +
                "font-weight: normal;\n" +
                "}\n" +
                ".th-ncube-top\n" +
                "{\n" +
                "color: white;\n" +
                "text-align: center;\n" +
                "font-weight: normal;\n" +
                "}\n" +
                ".td-ncube:hover { background: #E0F0FF }\n" +
                ".th-ncube:hover { background: #A2A2A2 }\n" +
                ".th-ncube-top:hover { background: #A2A2A2 }\n" +
                ".ncube-num\n" +
                "{\n" +
                "text-align: right;\n" +
                "}\n" +
                ".ncube-dead\n" +
                "{\n" +
                "background: #6495ED;\n" +
                "}\n" +
                ".ncube-head\n" +
                "{\n" +
                "background: #4D4D4D;\n" +
                "}\n" +
                ".column\n" +
                "{\n" +
                "background: #929292;\n" +
                "}\n" +
                ".column-code\n" +
                "{\n" +
                "vertical-align: top;\n" +
                "}\n" +
                ".column-url\n" +
                "{\n" +
                "color: blue;\n" +
                "text-align: left;\n" +
                "vertical-align: top;\n" +
                "}\n" +
                ".cell\n" +
                "{\n" +
                "color: black;\n" +
                "background: white;\n" +
                "text-align: center;\n" +
                "vertical-align: middle\n" +
                "}\n" +
                ".cell-url\n" +
                "{\n" +
                "color: mediumblue;\n" +
                "background: cornsilk;\n" +
                "text-align: left;\n" +
                "vertical-align: top\n" +
                "}\n" +
                ".cell-code\n" +
                "{\n" +
                "background: white;\n" +
                "text-align: left;\n" +
                "vertical-align: top\n" +
                "}\n" +
                ".ncube-pre\n" +
                "{\n" +
                "padding: 2px;\n" +
                "margin: 2px;\n" +
                "word-break: normal;\n" +
                "word-wrap: normal;\n" +
                "border: 0;\n" +
                "border-radius: 0;\n" +
                "background: white;\n" +
                "color: mediumblue" +
                "}\n" +
                ".ncube-pre:hover { background: #E0F0FF }\n" +
                " </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<table class=\"table-ncube\" border=\"1\">\n" +
                "<tr>\n";
    }

    private static String getNoAxisHtml()
    {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "  <head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Empty NCube</title>\n" +
                "  </head>\n" +
                "  <body/>\n" +
                "</html>";
    }

    private static void addColumnPrefixText(StringBuilder s, Column column)
    {
        if (column.getValue() instanceof CommandCell)
        {
            CommandCell cmd = (CommandCell) column.getValue();
            String name = (String)column.getMetaProperty("name");
            if (StringUtilities.hasContent(name))
            {
                s.append("name: ");
                s.append(name);
                s.append("<hr style=\"margin:1px\"/>");
            }
            if (StringUtilities.hasContent(cmd.getUrl()))
            {
                s.append("url: ");
            }
        }
    }

    private static String getColumnCssClass(Axis axis, Column col)
    {
        if (axis.getType() == AxisType.RULE)
        {
            return "column column-code";
        }
        if (col.getValue() instanceof CommandCell)
        {
            CommandCell cmd = (CommandCell) col.getValue();
            if (StringUtilities.hasContent(cmd.getUrl()))
            {
                return "column column-url";
            }
            else if (cmd instanceof GroovyBase)
            {
                return "column column-code";
            }
        }
        return "column";
    }

    private static void buildCell(NCube ncube, StringBuilder s, Set<Long> coord)
    {
        String id = makeCellId(coord);
        s.append(" <td data-id=\"").append(id).append("\" class=\"td-ncube ");

        if (ncube.containsCellById(coord))
        {
            Object cell = ncube.getCellByIdNoExecute(coord);
            if (cell instanceof CommandCell)
            {
                CommandCell cmd = (CommandCell) cell;
                if (StringUtilities.hasContent(cmd.getUrl()))
                {
                    s.append("cell cell-url\">url: ");
                    s.append(cmd.getUrl());
                }
                else if (cmd instanceof GroovyBase)
                {
                    s.append("cell cell-code\"><pre class=\"ncube-pre\">");
                    s.append(getCellValueAsString(cell));
                    s.append("</pre>");
                }
                else
                {
                    s.append("cell\">");
                    s.append(getCellValueAsString(cell));
                }
            }
            else
            {
                s.append("cell\">");
                s.append(getCellValueAsString(cell));
            }
        }
        else
        {
            s.append("cell\">");
        }
        s.append("</td>\n");
    }

    static String getCellValueAsString(Object cellValue)
    {
        if (cellValue == null)
        {
            return "null";
        }
        boolean isArray = cellValue.getClass().isArray();

        if (cellValue instanceof Date || cellValue instanceof String)
        {
            return CellInfo.formatForDisplay((Comparable) cellValue);
        }
        else if (cellValue instanceof Boolean || cellValue instanceof Character)
        {
            return String.valueOf(cellValue);
        }
        else if (cellValue instanceof Point2D || cellValue instanceof Point3D || cellValue instanceof LatLon)
        {
            return cellValue.toString();
        }
        else if (cellValue instanceof Number)
        {
            return CellInfo.formatForDisplay((Comparable) cellValue);
        }
        else if (cellValue instanceof byte[])
        {
            return StringUtilities.encode((byte[]) cellValue);
        }
        else if (isArray && JsonReader.isPrimitive(cellValue.getClass().getComponentType()))
        {
            StringBuilder str = new StringBuilder();
            str.append('[');
            final int len = Array.getLength(cellValue);
            final int len1 = len - 1;

            for (int i = 0; i < len; i++)
            {
                Object elem = Array.get(cellValue, i);
                str.append(elem.toString());
                if (i < len1)
                {
                    str.append(", ");
                }
            }
            str.append(']');
            return str.toString();
        }
        else if (isArray && Object[].class == cellValue.getClass())
        {
            StringBuilder str = new StringBuilder();
            str.append('[');
            final int len = Array.getLength(cellValue);
            final int len1 = len - 1;

            for (int i = 0; i < len; i++)
            {
                Object elem = Array.get(cellValue, i);
                str.append(getCellValueAsString(elem));
                if (i < len1)
                {
                    str.append(", ");
                }
            }
            str.append(']');
            return str.toString();
        }
        else if (cellValue instanceof CommandCell)
        {
            return ((CommandCell) cellValue).getCmd();
        }
        else
        {
            try
            {
                return JsonWriter.objectToJson(cellValue);
            }
            catch (IOException e)
            {
                throw new IllegalStateException("Error with simple JSON format", e);
            }
        }
    }

    private static String makeCellId(Set<Long> colIds)
    {
        StringBuilder s = new StringBuilder();
        Iterator<Long> i = colIds.iterator();
        while (i.hasNext())
        {
            s.append(i.next());
            if (i.hasNext())
            {
                s.append('_');
            }
        }

        return s.toString();
    }

    private static boolean isColumnInlineExpression(Column column)
    {
        return column.getValue() instanceof CommandCell && StringUtilities.isEmpty(((CommandCell)column.getValue()).getUrl());
    }
}
