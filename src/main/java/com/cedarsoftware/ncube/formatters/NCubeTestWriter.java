package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.CellInfo;
import com.cedarsoftware.ncube.NCubeTest;
import com.cedarsoftware.ncube.StringValuePair;

import java.io.IOException;

/**
 * Created by kpartlow on 9/24/2014.
 */
public class NCubeTestWriter extends BaseJsonFormatter
{
    public String format(Object[] tests) throws IOException {
        startArray();
        for (Object test : tests) {
            writeTest((NCubeTest)test);
            comma();
        }
        uncomma();
        endArray();

        return builder.toString();
    }

    private void writeTest(NCubeTest test) throws IOException {
        startObject();
        writeObjectKeyValue("name", test.getName(), true);
        writeObjectKey("coord");
        writeCoord(test.getCoord());
        comma();
        writeObjectKey("assertions");
        writeAssertions(test.getAssertions());
        endObject();
    }

    private void writeCoord(StringValuePair<CellInfo>[] coord) throws IOException {
        startArray();
        if (coord != null)
        {
            for (StringValuePair<CellInfo> parameter : coord)
            {
                startObject();
                writeObjectKey(parameter.getKey());
                writeCellInfo(parameter.getValue());
                endObject();
                comma();
            }
            uncomma();
        }
        endArray();
    }

    private void writeAssertions(CellInfo[] assertions) throws IOException {
        startArray();
        if (assertions != null)
        {
            for (CellInfo item : assertions)
            {
                writeCellInfo(item);
                comma();
            }
            uncomma();
        }
        endArray();
    }

    public void writeCellInfo(CellInfo info) throws IOException
    {
        startObject();
        if (info.dataType != null)
        {
            writeObjectKeyValue("type", info.dataType, true);
        }
        if (info.isUrl)
        {
            writeObjectKeyValue("isUrl", info.isUrl, true);
        }
        if (info.isCached)
        {
            writeObjectKeyValue("isCached", info.isCached, true);
        }
        writeObjectKeyValue("value", info.value, false);
        endObject();
    }
}
