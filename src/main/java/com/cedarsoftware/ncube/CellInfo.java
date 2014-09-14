package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.proximity.LatLon;
import com.cedarsoftware.ncube.proximity.Point2D;
import com.cedarsoftware.ncube.proximity.Point3D;
import com.cedarsoftware.util.DateUtilities;
import com.cedarsoftware.util.SafeSimpleDateFormat;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.io.JsonObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Get information about a cell (makes it a uniform query-able object).  Optional method
 * exists to collapse types for UI.
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
public class CellInfo
{
    public String value;
    public String dataType;
    public boolean isUrl;
    public boolean isCached;
    static final SafeSimpleDateFormat dateFormat = new SafeSimpleDateFormat("yyyy-MM-dd");
    static final SafeSimpleDateFormat dateTimeFormat = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static final Pattern DECIMAL_REGEX = Pattern.compile("[.]");

    public CellInfo(String type, String value, String isUrl, String isCached) {
        this.dataType = type;
        this.value = value;
        this.isUrl = Boolean.valueOf(isUrl);
        this.isCached = Boolean.valueOf(isCached);
    }

    public CellInfo(Object cell)
    {
        isUrl = false;
        isCached = false;
        value = null;
        dataType = null;

        if (cell == null)
        {
            return;
        }

        if (cell instanceof String)
        {
            value = (String) cell;
            dataType = CellTypes.String.desc();
        }
        else if (cell instanceof Long)
        {
            value = cell.toString();
            dataType = CellTypes.Long.desc();
        }
        else if (cell instanceof Boolean)
        {
            value = cell.toString();
            dataType = CellTypes.Boolean.desc();
        }
        else if (cell instanceof GroovyExpression)
        {
            GroovyExpression exp = (GroovyExpression) cell;
            isUrl = StringUtilities.hasContent(exp.getUrl());
            value = isUrl ? exp.getUrl() : exp.getCmd();
            dataType = CellTypes.Exp.desc();
            isCached = true;
        }
        else if (cell instanceof Byte)
        {
            value = cell.toString();
            dataType = CellTypes.Byte.desc();
        }
        else if (cell instanceof Short)
        {
            value = cell.toString();
            dataType = CellTypes.Short.desc();
        }
        else if (cell instanceof Integer)
        {
            value = cell.toString();
            dataType = CellTypes.Integer.desc();
        }
        else if (cell instanceof Date)
        {
            value = CellInfo.formatForDisplay((Date) cell);
            dataType = CellTypes.Date.desc();
        }
        else if (cell instanceof Double)
        {
            value = CellInfo.formatForEditing(cell);
            dataType = CellTypes.Double.desc();
        }
        else if (cell instanceof Float)
        {
            value = CellInfo.formatForEditing(cell);
            dataType = CellTypes.Float.desc();
        }
        else if (cell instanceof BigDecimal)
        {
            value = ((BigDecimal) cell).stripTrailingZeros().toPlainString();
            dataType = CellTypes.BigDecimal.desc();
        }
        else if (cell instanceof BigInteger)
        {
            value = cell.toString();
            dataType = CellTypes.BigInteger.desc();
        }
        else if (cell instanceof byte[])
        {
            value = StringUtilities.encode((byte[]) cell);
            dataType = CellTypes.Binary.desc();
        }
        else if (cell instanceof Point2D)
        {
            value = cell.toString();
            dataType = CellTypes.Point2D.desc();
        }
        else if (cell instanceof Point3D)
        {
            value = cell.toString();
            dataType = CellTypes.Point3D.desc();
        }
        else if (cell instanceof LatLon)
        {
            value = cell.toString();
            dataType = CellTypes.LatLon.desc();
        }
        else if (cell instanceof GroovyMethod)
        {
            GroovyMethod method = (GroovyMethod) cell;
            value = method.getCmd();
            dataType = CellTypes.Method.desc();
            isUrl = StringUtilities.hasContent(method.getUrl());
            isCached = true;
        }
        else if (cell instanceof StringUrlCmd)
        {
            StringUrlCmd strCmd = (StringUrlCmd) cell;
            value = strCmd.getUrl();
            dataType = CellTypes.String.desc();
            isUrl = true;
            isCached = strCmd.isCacheable();
        }
        else if (cell instanceof BinaryUrlCmd)
        {
            BinaryUrlCmd binCmd = (BinaryUrlCmd) cell;
            value = binCmd.getUrl();
            dataType = CellTypes.Binary.desc();
            isUrl = true;
            isCached = binCmd.isCacheable();
        }
        else if (cell instanceof GroovyTemplate)
        {
            GroovyTemplate templateCmd = (GroovyTemplate) cell;
            isUrl = StringUtilities.hasContent(templateCmd.getUrl());
            value = isUrl ? templateCmd.getUrl() : templateCmd.getCmd();
            dataType = CellTypes.Template.desc();
            isCached = templateCmd.isCacheable();
        }
        else if (cell instanceof Range)
        {
            Range range = (Range) cell;
            isUrl = false;
            value = "[" + CellInfo.formatForEditing(range.low) + ", " + CellInfo.formatForEditing(range.high) + "]";
            dataType = null;
            isCached = false;
        }
        else if (cell instanceof RangeSet)
        {
            RangeSet set = (RangeSet) cell;
            isUrl = false;
            StringBuilder builder = new StringBuilder();
            for (int i=0; i < set.size(); i++)
            {
                if (i != 0)
                {
                    builder.append(", ");
                }
                Object val = set.get(i);
                if (val instanceof Range)
                {
                    Range range = (Range) val;
                    builder.append('[');
                    builder.append(CellInfo.formatForEditing(range.low));
                    builder.append(", ");
                    builder.append(CellInfo.formatForEditing(range.high));
                    builder.append("]");
                }
                else
                {
                    builder.append(CellInfo.formatForEditing(val));
                }
            }
            value = builder.toString();
            dataType = null;
            isCached = false;
        }
        else
        {
            throw new IllegalStateException("Unknown cell value type, value: " + cell.toString() + ", class: " + cell.getClass().getName());
        }
    }

    public Object recreate() {
        switch (this.dataType) {
            case "string":
                return isUrl ? new StringUrlCmd(this.value, isCached) : this.value;

            case "date":
                return DateUtilities.parseDate(this.value);

            case "boolean":
                return Boolean.valueOf(this.value);

            case "byte":
                return Byte.valueOf(this.value);

            case "short":
                return Short.valueOf(this.value);

            case "int":
                return Integer.valueOf(this.value);

            case "long":
                return Long.valueOf(this.value);

            case "float":
                return Float.valueOf(this.value);

            case "double":
                return Double.valueOf(this.value);

            case "bigdec":
                return new BigDecimal(this.value);

            case "bigint":
                return new BigInteger(this.value);

            case "binary":
                return new BinaryUrlCmd(this.value, isCached);

            case "exp":
                return new GroovyExpression(isUrl ? null : value, isUrl ? value : null);

            case "method":
                return new GroovyMethod(isUrl ? null : value, isUrl ? value : null);

            case "template":
                return new GroovyTemplate(isUrl ? null : value, isUrl ? value : null, isCached);

            default:
                return new IllegalArgumentException("Invalid Cell Type Passed in:  " + this.dataType);
        }
    }
    /**
     * Collapse: byte, short, int ==> long
     * Collapse: float ==> double
     * Collapse: BigInteger ==> BigDecimal
     */
    public void collapseToUiSupportedTypes()
    {
        if (CellTypes.Byte.equals(dataType) || CellTypes.Short.equals(dataType) || CellTypes.Integer.equals(dataType))
        {
            dataType = CellTypes.Long.desc();
        }
        else if (CellTypes.Float.equals(dataType))
        {
            dataType = CellTypes.Double.desc();
        }
        else if (CellTypes.BigInteger.equals(dataType))
        {
            dataType = CellTypes.BigDecimal.desc();
        }
    }

    public static Object parseJsonValue(final Object val, final String url, final String type, boolean cache)
    {
        if (url != null)
        {
            if (CellTypes.Exp.desc().equalsIgnoreCase(type))
            {
                return new GroovyExpression(null, url);
            }
            else if (CellTypes.Method.desc().equalsIgnoreCase(type))
            {
                return new GroovyMethod(null, url);
            }
            else if (CellTypes.Template.desc().equalsIgnoreCase(type))
            {
                return new GroovyTemplate(null, url, cache);
            }
            else if (CellTypes.String.desc().equalsIgnoreCase(type))
            {
                return new StringUrlCmd(url, cache);
            }
            else if (CellTypes.Binary.desc().equalsIgnoreCase(type))
            {
                return new BinaryUrlCmd(url, cache);
            }
            else
            {
                throw new IllegalArgumentException("url can only be specified with 'exp', 'method', 'template', 'string', or 'binary' types");
            }
        }

        return parseJsonValue(type, val);
    }

    static Object parseJsonValue(String type, Object val)
    {
        if (CellTypes.Null.desc().equals(val) || val == null)
        {
            return null;
        }
        else if (val instanceof Double)
        {
            if (CellTypes.BigDecimal.desc().equals(type))
            {
                return new BigDecimal((Double)val);
            }
            else if (CellTypes.Float.desc().equals(type))
            {
                return ((Double)val).floatValue();
            }
            return val;
        }
        else if (val instanceof Long)
        {
            if (CellTypes.Integer.desc().equals(type))
            {
                return ((Long)val).intValue();
            }
            else if (CellTypes.BigInteger.desc().equals(type))
            {
                return new BigInteger(val.toString());
            }
            else if (CellTypes.Byte.desc().equals(type))
            {
                return ((Long)val).byteValue();
            }
            else if (CellTypes.Short.desc().equals(type))
            {
                return (((Long) val).shortValue());
            }
            else if (CellTypes.BigDecimal.desc().equals(type))
            {
                return new BigDecimal((Long)val);
            }
            return val;
        }
        else if (val instanceof Boolean)
        {
            return val;
        }
        else if (val instanceof String)
        {
            val = ((String) val).trim();
            if (StringUtilities.isEmpty(type))
            {
                return val;
            }
            else if (CellTypes.Boolean.desc().equals(type))
            {
                String bool = (String)val;
                if ("true".equalsIgnoreCase(bool) || "false".equalsIgnoreCase(bool))
                {
                    return "true".equalsIgnoreCase((String) val);
                }
                throw new IllegalArgumentException("Boolean must be 'true' or 'false'.  Case does not matter.");
            }
            else if (CellTypes.Byte.desc().equals(type))
            {
                return Byte.parseByte((String)val);
            }
            else if (CellTypes.Short.desc().equals(type))
            {
                return Short.parseShort((String)val);
            }
            else if (CellTypes.Integer.desc().equals(type))
            {
                return Integer.parseInt((String)val);
            }
            else if (CellTypes.Long.desc().equals(type))
            {
                return Long.parseLong((String)val);
            }
            else if (CellTypes.Double.desc().equals(type))
            {
                return Double.parseDouble((String)val);
            }
            else if (CellTypes.Float.desc().equals(type))
            {
                return Float.parseFloat((String)val);
            }
            else if (CellTypes.Exp.desc().equals(type))
            {
                return new GroovyExpression((String)val, null);
            }
            else if (CellTypes.Method.desc().equals(type))
            {
                return new GroovyMethod((String) val, null);
            }
            else if (CellTypes.Date.desc().equals(type) || "datetime".equals(type))
            {
                try
                {
                    Date date = DateUtilities.parseDate((String) val);
                    return (date == null) ? val : date;
                }
                catch (Exception e)
                {
                    throw new IllegalArgumentException("Could not parse '" + type + "': " + val);
                }
            }
            else if (CellTypes.Template.desc().equals(type))
            {
                return new GroovyTemplate((String)val, null, true);
            }
            else if (CellTypes.String.desc().equals(type))
            {
                return val;
            }
            else if (CellTypes.Binary.desc().equals(type))
            {   // convert hex string "10AF3F" as byte[]
                String hex = (String)val;
                if (hex.length() % 2 != 0)
                {
                    throw new IllegalArgumentException("Binary (hex) values must have an even number of digits.");
                }
                if (!hex.matches("[0-9a-fA-F]+"))
                {
                    throw new IllegalArgumentException("Binary (hex) values must contain only the numbers 0 thru 9 and letters A thru F.");
                }
                return StringUtilities.decode((String) val);
            }
            else if (CellTypes.BigInteger.desc().equals(type))
            {
                return new BigInteger((String) val);
            }
            else if (CellTypes.BigDecimal.desc().equals(type))
            {
                return new BigDecimal((String)val);
            }
            else if (CellTypes.LatLon.desc().equals(type))
            {
                Matcher m = Regexes.valid2Doubles.matcher((String) val);
                if (!m.matches())
                {
                    throw new IllegalArgumentException(String.format("Illegal Lat/Long value (%s)", val));
                }
                return new LatLon(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)));
            }
            else if (CellTypes.Point2D.desc().equals(type))
            {
                Matcher m = Regexes.valid2Doubles.matcher((String) val);
                if (!m.matches())
                {
                    throw new IllegalArgumentException(String.format("Illegal Point2D value (%s)", val));
                }
                return new Point2D(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)));
            }
            else if (CellTypes.Point3D.desc().equals(type))
            {
                Matcher m = Regexes.valid3Doubles.matcher((String) val);
                if (!m.matches())
                {
                    throw new IllegalArgumentException(String.format("Illegal Point3D value (%s)", val));
                }
                return new Point3D(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3)));
            }
            else
            {
                throw new IllegalArgumentException("Unknown value (" + type + ") for 'type' field");
            }
        }
        else if (val instanceof JsonObject)
        {   // Legacy support - remove once we drop support for array type (can be done using GroovyExpression).
            StringBuilder exp = new StringBuilder();
            exp.append("[");
            Object[] values = ((JsonObject) val).getArray();
            int i=0;
            for (Object value : values)
            {
                i++;
                Object o = parseJsonValue(value, null, type, false);
                exp.append(javaToGroovySource(o));
                if (i < values.length)
                {
                    exp.append(",");
                }
            }
            exp.append("] as Object[]");
            return new GroovyExpression(exp.toString(), null);
        }
        else
        {
            throw new IllegalArgumentException("Error reading value of type '" + val.getClass().getName() + "' - Simple JSON format for NCube only supports Long, Double, String, String Date, Boolean, or null");
        }
    }

    /**
     * Convert Java data-type to a Groovy Source equivalent
     * @param o Java primitive type
     * @return Groovy source code equivalent of passed in value.  For example, if a BigInteger is passed in,
     * the value will be return as a String with a "G" at the end.
     */
    private static String javaToGroovySource(Object o)
    {
        StringBuilder builder = new StringBuilder();
        if (o instanceof String)
        {
            builder.append("'");
            builder.append(o.toString());
            builder.append("'");
        }
        else if (o instanceof GroovyExpression)
        {
            builder.append("'");
            builder.append(((GroovyExpression) o).getCmd());
            builder.append("'");
        }
        else if (o instanceof Boolean)
        {
            builder.append((Boolean) o ? "true" : "false");
        }
        else if (o instanceof Double)
        {
            builder.append(CellInfo.formatForEditing(o));
            builder.append('d');
        }
        else if (o instanceof Integer)
        {
            builder.append(o);
            builder.append('i');
        }
        else if (o instanceof Long)
        {
            builder.append(o);
            builder.append('L');
        }
        else if (o instanceof BigDecimal)
        {
            builder.append(((BigDecimal) o).stripTrailingZeros().toPlainString());
            builder.append('G');
        }
        else if (o instanceof BigInteger)
        {
            builder.append(o);
            builder.append('G');
        }
        else if (o instanceof Byte)
        {
            builder.append(o);
            builder.append(" as Byte");
        }
        else if (o instanceof Float)
        {
            builder.append(CellInfo.formatForEditing(o));
            builder.append('f');
        }
        else if (o instanceof Short)
        {
            builder.append(o);
            builder.append(" as Short");
        }
        else
        {
            throw new IllegalArgumentException("Unknown Groovy Type : " + o.getClass());
        }
        return builder.toString();
    }

    public static String formatForDisplay(Comparable val)
    {
        if (val instanceof Date)
        {
            return getDateAsString((Date)val);
        }
        else if (val instanceof Double || val instanceof Float)
        {
            return new DecimalFormat("#,##0.0##############").format(val);
        }
        else if (val instanceof BigDecimal)
        {
            BigDecimal x = (BigDecimal) val;
            String s = x.stripTrailingZeros().toPlainString();
            if (s.contains("."))
            {
                String[] pieces = DECIMAL_REGEX.split(s);
                if (pieces.length != 2)
                {
                    throw new IllegalArgumentException("Invalid value for BigDecimal: " + val);
                }
                return new DecimalFormat("#,##0").format(new BigInteger(pieces[0])) + "." + pieces[1];
            }
            else
            {
                return new DecimalFormat("#,##0").format(val);
            }
        }
        else if (val instanceof Number)
        {
            return new DecimalFormat("#,##0").format(val);
        }
        else if (val == null)
        {
            return "Default";
        }
        else
        {
            return val.toString();
        }
    }

    public static String formatForEditing(Object val)
    {
        if (val instanceof Date)
        {
            return '"' + getDateAsString((Date)val) + '"';
        }
        else if (val instanceof Double || val instanceof Float)
        {
            DecimalFormat fmt = new DecimalFormat("#0.0##############");
            return fmt.format(((Number)val).doubleValue());
        }
        else if (val instanceof BigDecimal)
        {
            return ((BigDecimal)val).stripTrailingZeros().toPlainString();
        }
        return val.toString();
    }

    private static String getDateAsString(Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        if (cal.get(Calendar.HOUR) == 0 && cal.get(Calendar.MINUTE) == 0 && cal.get(Calendar.SECOND) == 0)
        {
            return dateFormat.format(date);
        }
        return dateTimeFormat.format(date);
    }
}
