package com.cedarsoftware.ncube

import com.cedarsoftware.ncube.proximity.LatLon
import com.cedarsoftware.ncube.proximity.Point2D
import com.cedarsoftware.ncube.proximity.Point3D
import com.cedarsoftware.util.Converter
import com.cedarsoftware.util.SafeSimpleDateFormat
import com.cedarsoftware.util.StringUtilities
import com.cedarsoftware.util.io.JsonObject

import java.text.DecimalFormat
import java.text.MessageFormat
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Get information about a cell (makes it a uniform query-able object).  Optional method
 * exists to collapse types for UI.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License")
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
public class CellInfo
{
    public String value;
    public String dataType;
    public boolean isUrl;
    public boolean isCached;
    static final SafeSimpleDateFormat dateFormat = new SafeSimpleDateFormat("yyyy-MM-dd")
    static final SafeSimpleDateFormat dateTimeFormat = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private static final UNSUPPORTED_TYPE = 'bogusType'
    private static final Pattern DECIMAL_REGEX = Pattern.compile("[.]")
    private static final Pattern HEX_DIGIT = Pattern.compile("[0-9a-fA-F]+")
    private static final ThreadLocal<DecimalFormat> decimalIntFormat = new ThreadLocal<DecimalFormat>() {
        public DecimalFormat initialValue()
        {
            return new DecimalFormat("#,##0")
        }
    };

    private static final ThreadLocal<DecimalFormat> decimalFormat = new ThreadLocal<DecimalFormat>() {
        public DecimalFormat initialValue()
        {
            return new DecimalFormat("#,##0.0##############")
        }
    };


    public CellInfo(String type, String value, Object isUrl, Object isCached)
    {
        this.dataType = type;
        this.value = value;
        this.isUrl = booleanValue(isUrl)
        this.isCached = booleanValue(isCached)
    }

    public static boolean booleanValue(Object o)
    {
        if (o instanceof Boolean)
        {
            return (Boolean) o;
        }
        else if (o instanceof String)
        {
            String s = (String) o;

            if ("true".equalsIgnoreCase(s))
            {
                return true;
            }
        }

        return false;
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
            dataType = 'string'
        }
        else if (cell instanceof Long)
        {
            value = cell.toString()
            dataType = 'long'
        }
        else if (cell instanceof Boolean)
        {
            value = cell.toString()
            dataType = 'boolean'
        }
        else if (cell instanceof GroovyExpression)
        {
            GroovyExpression exp = cell as GroovyExpression
            isUrl = StringUtilities.hasContent(exp.getUrl())
            value = isUrl ? exp.getUrl() : exp.getCmd()
            dataType = 'exp'
            isCached = exp.isCacheable()
        }
        else if (cell instanceof Byte)
        {
            value = cell.toString()
            dataType = 'byte'
        }
        else if (cell instanceof Short)
        {
            value = cell.toString()
            dataType = 'short'
        }
        else if (cell instanceof Integer)
        {
            value = cell.toString()
            dataType = 'int'
        }
        else if (cell instanceof Date)
        {
            value = formatForDisplay(cell as Date)
            dataType = 'date'
        }
        else if (cell instanceof Double)
        {
            value = formatForEditing(cell)
            dataType = 'double'
        }
        else if (cell instanceof Float)
        {
            value = formatForEditing(cell)
            dataType = 'float'
        }
        else if (cell instanceof BigDecimal)
        {
            value = (cell as BigDecimal).stripTrailingZeros().toPlainString()
            dataType = 'bigdec'
        }
        else if (cell instanceof BigInteger)
        {
            value = cell.toString()
            dataType = 'bigint'
        }
        else if (cell instanceof byte[])
        {
            value = StringUtilities.encode(cell as byte[])
            dataType = 'binary'
        }
        else if (cell instanceof Point2D)
        {
            value = cell.toString()
            dataType = 'point2d'
        }
        else if (cell instanceof Point3D)
        {
            value = cell.toString()
            dataType = 'point3d'
        }
        else if (cell instanceof LatLon)
        {
            value = cell.toString()
            dataType = 'latlon'
        }
        else if (cell instanceof GroovyMethod)
        {
            GroovyMethod method = cell as GroovyMethod
            isUrl = StringUtilities.hasContent(method.getUrl())
            value = isUrl ? method.getUrl() : method.getCmd()
            dataType = 'method'
            isCached = true;
        }
        else if (cell instanceof StringUrlCmd)
        {
            StringUrlCmd strCmd = cell as StringUrlCmd
            value = strCmd.getUrl()
            dataType = 'string'
            isUrl = true;
            isCached = strCmd.isCacheable()
        }
        else if (cell instanceof BinaryUrlCmd)
        {
            BinaryUrlCmd binCmd = cell as BinaryUrlCmd
            value = binCmd.getUrl()
            dataType = 'binary'
            isUrl = true;
            isCached = binCmd.isCacheable()
        }
        else if (cell instanceof GroovyTemplate)
        {
            GroovyTemplate templateCmd = cell as GroovyTemplate
            isUrl = StringUtilities.hasContent(templateCmd.getUrl())
            value = isUrl ? templateCmd.getUrl() : templateCmd.getCmd()
            dataType = 'template'
            isCached = templateCmd.isCacheable()
        }
        else if (cell instanceof Range)
        {
            Range range = cell as Range
            isUrl = false;
            value = formatForEditing(range)
            dataType = 'range';
            isCached = false;
        }
        else if (cell instanceof RangeSet)
        {
            RangeSet set = cell as RangeSet
            isUrl = false;
            StringBuilder builder = new StringBuilder()
            for (int i=0; i < set.size(); i++)
            {
                if (i != 0)
                {
                    builder.append(", ")
                }
                Object val = set.get(i)
                if (val instanceof Range)
                {
                    Range range = (Range) val;
                    builder.append('[')
                    builder.append(formatForEditing(range.low))
                    builder.append(", ")
                    builder.append(formatForEditing(range.high))
                    builder.append("]")
                }
                else
                {
                    builder.append(formatForEditing(val))
                }
            }
            value = builder.toString()
            dataType = 'rangeset';
            isCached = false;
        }
        else
        {
            throw new IllegalArgumentException("Unknown cell value type, value: " + cell.toString() + ", class: " + cell.getClass().getName())
        }
    }

    public static String getType(Object cell, String section)
    {
        String type = getType(cell)
        if (UNSUPPORTED_TYPE.equals(type))
        {
            throw new IllegalArgumentException(MessageFormat.format("Unsupported type {0} found in {1}", cell.getClass().getName(), section))
        }
        return type
    }

    public static String getType(Object cell)
    {
        if (cell == null) {
            return null
        }

        if (cell instanceof String) {
            return 'string'
        }

        if (cell instanceof Double) {
            return 'double'
        }

        if (cell instanceof Long) {
            return 'long'
        }

        if (cell instanceof Boolean) {
            return 'boolean'
        }

        if (cell instanceof BigDecimal) {
            return 'bigdec'
        }

        if (cell instanceof Integer) {
            return 'int'
        }

        if (cell instanceof BigInteger) {
            return 'bigint'
        }

        if (cell instanceof Date) {
            return 'date'
        }

        if (cell instanceof BinaryUrlCmd || cell instanceof byte[]) {
            return 'binary'
        }

        if (cell instanceof GroovyExpression || cell instanceof Collection || cell.getClass().isArray()) {
            return 'exp'
        }

        if (cell instanceof GroovyMethod) {
            return 'method'
        }

        if (cell instanceof GroovyTemplate) {
            return 'template'
        }

        if (cell instanceof StringUrlCmd) {
            return 'string'
        }

        if (cell instanceof Byte) {
            return 'byte'
        }

        if (cell instanceof Short) {
            return 'short'
        }

        if (cell instanceof Float) {
            return 'float'
        }

        if (cell instanceof Point2D)
        {
            return 'point2d'
        }

        if (cell instanceof Point3D)
        {
            return 'point3d'
        }

        if (cell instanceof LatLon)
        {
            return 'latlon'
        }

        if (cell instanceof Range)
        {
            return 'range'
        }

        if (cell instanceof RangeSet)
        {
            return 'rangeset'
        }

        return UNSUPPORTED_TYPE
    }

    public Object recreate()
    {
        switch (dataType)
        {
            case "string":
                return isUrl ? new StringUrlCmd(value, isCached) : value

            case "date":
                return Converter.convert(value, Date.class)

            case "boolean":
                return Converter.convert(value, boolean.class)

            case "byte":
                return Converter.convert(value, byte.class)

            case "short":
                return Converter.convert(value, short.class)

            case "int":
                return Converter.convert(value, int.class)

            case "long":
                return Converter.convert(value, long.class)

            case "float":
                return Converter.convert(value, float.class)

            case "double":
                return Converter.convert(value, double.class)

            case 'bigdec':
                return Converter.convert(value, BigDecimal.class)

            case "bigint":
                return Converter.convert(value, BigInteger.class)

            case "binary":
                return isUrl ? new BinaryUrlCmd(value, isCached) : StringUtilities.decode(value)

            case "exp":
                return new GroovyExpression(isUrl ? null : value, isUrl ? value : null, isCached)

            case "method":
                return new GroovyMethod(isUrl ? null : value, isUrl ? value : null, isCached)

            case "template":
                return new GroovyTemplate(isUrl ? null : value, isUrl ? value : null, isCached)

            case "latlon":
                Matcher m = Regexes.valid2Doubles.matcher(value)
                if (!m.matches())
                {
                    throw new IllegalArgumentException(String.format("Invalid Lat/Long value (%s)", value))
                }
                return new LatLon((double) Converter.convert(m.group(1), double.class), (double) Converter.convert(m.group(2), double.class))

            case "point2d":
                Matcher m = Regexes.valid2Doubles.matcher(value)
                if (!m.matches())
                {
                    throw new IllegalArgumentException(String.format("Invalid Point2D value (%s)", value))
                }
                return new Point2D((double) Converter.convert(m.group(1), double.class), (double) Converter.convert(m.group(2), double.class))

            case "point3d":
                Matcher m = Regexes.valid3Doubles.matcher(value)
                if (!m.matches())
                {
                    throw new IllegalArgumentException(String.format("Invalid Point3D value (%s)", value))
                }
                return new Point3D((double) Converter.convert(m.group(1), double.class),
                        (double) Converter.convert(m.group(2), double.class),
                        (double) Converter.convert(m.group(3), double.class))

            case 'range':
                throw new UnsupportedOperationException("'range' does not yet support recreate()")

            case 'rangeset':
                throw new UnsupportedOperationException("'rangeset' does not support recreate()")

            case "null":
                return null

            case null:
                return null

            default:
                throw new IllegalArgumentException("Invalid Type:  " + dataType)
        }
    }

    /**
     * Collapse: byte, short, int ==> long
     * Collapse: float ==> double
     * Collapse: BigInteger ==> BigDecimal
     */
    public void collapseToUiSupportedTypes()
    {
        if ('byte'.equals(dataType) || 'short'.equals(dataType) || 'int'.equals(dataType))
        {
            dataType = 'long'
        }
        else if ('float'.equals(dataType))
        {
            dataType = 'double'
        }
        else if ('bigint'.equals(dataType))
        {
            dataType = 'bigdec'
        }
    }

    public static Object parseJsonValue(final Object val, final String url, final String type, boolean cache)
    {
        if (url != null)
        {
            if ('exp'.equals(type))
            {
                return new GroovyExpression(null, url, cache)
            }
            else if ('method'.equals(type))
            {
                return new GroovyMethod(null, url, cache)
            }
            else if ('template'.equals(type))
            {
                return new GroovyTemplate(null, url, cache)
            }
            else if ('string'.equals(type))
            {
                return new StringUrlCmd(url, cache)
            }
            else if ('binary'.equalsIgnoreCase(type))
            {
                return new BinaryUrlCmd(url, cache)
            }
            else
            {
                throw new IllegalArgumentException("url can only be specified with 'exp', 'method', 'template', 'string', or 'binary' types")
            }
        }

        return parseJsonValue(type, val, cache)
    }

    static Object parseJsonValue(String type, Object val, boolean cache)
    {
        if ('null'.equals(val) || val == null)
        {
            return null;
        }
        else if (val instanceof Double)
        {
            if ('bigdec'.equals(type))
            {
                return new BigDecimal((Double)val)
            }
            else if ('float'.equals(type))
            {
                return ((Double)val).floatValue()
            }
            return val;
        }
        else if (val instanceof Long)
        {
            if ('int'.equals(type))
            {
                return ((Long)val).intValue()
            }
            else if ('bigint'.equals(type))
            {
                return new BigInteger(val.toString())
            }
            else if ('byte'.equals(type))
            {
                return ((Long)val).byteValue()
            }
            else if ('short'.equals(type))
            {
                return (((Long) val).shortValue())
            }
            else if ('bigdec'.equals(type))
            {
                return new BigDecimal((Long)val)
            }
            return val;
        }
        else if (val instanceof Boolean)
        {
            return val;
        }
        else if (val instanceof String)
        {
            val = (val as String).trim()
            if (StringUtilities.isEmpty(type))
            {
                return val;
            }
            else if ('boolean'.equals(type))
            {
                String bool = (String)val;
                if ('true'.equalsIgnoreCase(bool) || 'false'.equalsIgnoreCase(bool))
                {
                    return 'true'.equalsIgnoreCase((String) val)
                }
                throw new IllegalArgumentException("Boolean must be 'true' or 'false'.  Case does not matter.")
            }
            else if ('byte'.equals(type))
            {
                return Converter.convert(val, byte.class)
            }
            else if ('short'.equals(type))
            {
                return Converter.convert(val, short.class)
            }
            else if ('int'.equals(type))
            {
                return Converter.convert(val, int.class)
            }
            else if ('long'.equals(type))
            {
                return Converter.convert(val, long.class)
            }
            else if ('double'.equals(type))
            {
                return Converter.convert(val, double.class)
            }
            else if ('float'.equals(type))
            {
                return Converter.convert(val, float.class)
            }
            else if ('exp'.equals(type))
            {
                return new GroovyExpression((String)val, null, cache)
            }
            else if ('method'.equals(type))
            {
                return new GroovyMethod((String) val, null, cache)
            }
            else if ('date'.equals(type) || "datetime".equals(type))
            {
                try
                {
                    Date date = Converter.convert(val, Date.class) as Date
                    return (date == null) ? val : date;
                }
                catch (Exception ignored)
                {
                    throw new IllegalArgumentException("Could not parse '" + type + "': " + val)
                }
            }
            else if ('template'.equals(type))
            {
                return new GroovyTemplate((String)val, null, cache)
            }
            else if ('string'.equals(type))
            {
                return val;
            }
            else if ('binary'.equals(type))
            {   // convert hex string "10AF3F" as byte[]
                String hex = (String)val;
                if (hex.length() % 2 != 0)
                {
                    throw new IllegalArgumentException("Binary (hex) values must have an even number of digits.")
                }
                if (!HEX_DIGIT.matcher(hex).matches())
                {
                    throw new IllegalArgumentException("Binary (hex) values must contain only the numbers 0 thru 9 and letters A thru F.")
                }
                return StringUtilities.decode((String) val)
            }
            else if ('bigint'.equals(type))
            {
                return new BigInteger((String) val)
            }
            else if ('bigdec'.equals(type))
            {
                return new BigDecimal((String)val)
            }
            else if ('latlon'.equals(type))
            {
                Matcher m = Regexes.valid2Doubles.matcher((String) val)
                if (!m.matches())
                {
                    throw new IllegalArgumentException(String.format("Invalid Lat/Long value (%s)", val))
                }

                return new LatLon((double)Converter.convert(m.group(1), double.class), (double) Converter.convert(m.group(2), double.class))
            }
            else if ('point2d'.equals(type))
            {
                Matcher m = Regexes.valid2Doubles.matcher((String) val)
                if (!m.matches())
                {
                    throw new IllegalArgumentException(String.format("Invalid Point2D value (%s)", val))
                }
                return new Point2D((double) Converter.convert(m.group(1), double.class), (double) Converter.convert(m.group(2), double.class))
            }
            else if ('point3d'.equals(type))
            {
                Matcher m = Regexes.valid3Doubles.matcher((String) val)
                if (!m.matches())
                {
                    throw new IllegalArgumentException(String.format("Invalid Point3D value (%s)", val))
                }
                return new Point3D((double) Converter.convert(m.group(1), double.class),
                        (double) Converter.convert(m.group(2), double.class),
                        (double) Converter.convert(m.group(3), double.class))
            }
            else
            {
                throw new IllegalArgumentException("Unknown value (" + type + ") for 'type' field")
            }
        }
        else if (val instanceof JsonObject)
        {   // Legacy support - remove once we drop support for array type (can be done using GroovyExpression).
            StringBuilder exp = new StringBuilder()
            exp.append("[")
            Object[] values = (val as JsonObject).getArray()
            int i=0;
            for (Object value : values)
            {
                i++;
                Object o = parseJsonValue(value, null, type, cache)
                exp.append(javaToGroovySource(o))
                if (i < values.length)
                {
                    exp.append(",")
                }
            }
            exp.append("] as Object[]")
            return new GroovyExpression(exp.toString(), null, cache)
        }
        else
        {
            throw new IllegalArgumentException("Error reading value of type '" + val.getClass().getName() + "' - Simple JSON format for NCube only supports Long, Double, String, String Date, Boolean, or null")
        }
    }

    /**
     * Convert Java data-type to a Groovy Source equivalent
     * @param o Java primitive type
     * @return Groovy source code equivalent of passed in value.  For example, if a BigInteger is passed in,
     * the value will be return as a String with a "G" at the end.
     */
    static String javaToGroovySource(Object o)
    {
        StringBuilder builder = new StringBuilder()
        if (o instanceof String)
        {
            builder.append("'")
            builder.append(o.toString())
            builder.append("'")
        }
        else if (o instanceof GroovyExpression)
        {
            builder.append("'")
            builder.append(((GroovyExpression) o).getCmd())
            builder.append("'")
        }
        else if (o instanceof Boolean)
        {
            builder.append((o as Boolean) ? "true" : "false")
        }
        else if (o instanceof Double)
        {
            builder.append(formatForEditing(o))
            builder.append('d')
        }
        else if (o instanceof Integer)
        {
            builder.append(o)
            builder.append('i')
        }
        else if (o instanceof Long)
        {
            builder.append(o)
            builder.append('L')
        }
        else if (o instanceof BigDecimal)
        {
            builder.append((o as BigDecimal).stripTrailingZeros().toPlainString())
            builder.append('G')
        }
        else if (o instanceof BigInteger)
        {
            builder.append(o)
            builder.append('G')
        }
        else if (o instanceof Byte)
        {
            builder.append(o)
            builder.append(" as Byte")
        }
        else if (o instanceof Float)
        {
            builder.append(formatForEditing(o))
            builder.append('f')
        }
        else if (o instanceof Short)
        {
            builder.append(o)
            builder.append(" as Short")
        }
        else
        {
            throw new IllegalArgumentException("Unknown Groovy Type : " + o.getClass())
        }
        return builder.toString()
    }

    public static String formatForDisplay(Comparable val)
    {
        if (val instanceof Double || val instanceof Float)
        {
            return decimalFormat.get().format(val)
        }
        else if (val instanceof BigDecimal)
        {
            BigDecimal x = val as BigDecimal
            String s = x.stripTrailingZeros().toPlainString()
            if (s.contains("."))
            {
                String[] pieces = DECIMAL_REGEX.split(s)
                return decimalIntFormat.get().format(new BigInteger(pieces[0])) + "." + pieces[1];
            }
            else
            {
                return decimalIntFormat.get().format(val)
            }
        }
        else if (val instanceof Number)
        {
            return decimalIntFormat.get().format(val)
        }
        else if (val instanceof Date)
        {
            return getDateAsString(val as Date)
        }
        else if (val == null)
        {
            return "Default";
        }
        else
        {
            return val.toString()
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
            DecimalFormat fmt = new DecimalFormat("#0.0##############")
            return fmt.format(((Number)val).doubleValue())
        }
        else if (val instanceof BigDecimal)
        {
            return ((BigDecimal)val).stripTrailingZeros().toPlainString()
        }
        else if (val instanceof Range)
        {
            Range range = (Range) val;
            return formatForEditing(range.low) + ", " + formatForEditing(range.high)
        }
        return val.toString()
    }

    private static String getDateAsString(Date date)
    {
        Calendar cal = Calendar.getInstance()
        cal.clear()
        cal.setTime(date)
        if (cal.get(Calendar.HOUR) == 0 && cal.get(Calendar.MINUTE) == 0 && cal.get(Calendar.SECOND) == 0)
        {
            return dateFormat.format(date)
        }
        return dateTimeFormat.format(date)
    }

    public boolean equals(Object o)
    {
        if (this.is(o))
        {
            return true
        }
        if (!(o instanceof CellInfo))
        {
            return false
        }

        CellInfo cellInfo = (CellInfo) o;

        if (isUrl != cellInfo.isUrl)
        {
            return false
        }
        if (isCached != cellInfo.isCached)
        {
            return false
        }
        if (value != null ? !value.equals(cellInfo.value) : cellInfo.value != null)
        {
            return false
        }
        return !(dataType != null ? !dataType.equals(cellInfo.dataType) : cellInfo.dataType != null)
    }

    public int hashCode()
    {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (dataType != null ? dataType.hashCode() : 0)
        result = 31 * result + (isUrl ? 1 : 0)
        result = 31 * result + (isCached ? 1 : 0)
        return result
    }
}
