package ncube.grv.exp;

import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.ncube.NCubeManager;
import com.cedarsoftware.ncube.exception.RuleStop;

import java.util.Map;

/**
 * Base class for all GroovyExpression and GroovyMethod's within n-cube CommandCells.
 * @see com.cedarsoftware.ncube.GroovyBase
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
public class NCubeGroovyExpression
{
    protected Map input;
    protected Map output;
    protected Object stack;
    protected NCube ncube;

    /**
     * @param args a Map that contains the 'input' Map, 'output' Map, 'ncube',
     *  and the 'stack.'
     */
    public void init(Map args)
    {
        input = (Map) args.get("input");
        output = (Map) args.get("output");
        stack = args.get("stack");
        ncube = (NCube) args.get("ncube");
    }

    public Object getFixedCell(Map coord)
    {
        return ncube.getCell(coord, output);
    }

    public Object getFixedCubeCell(String name, Map coord)
    {
        NCube cube = NCubeManager.getCube(name, ncube.getVersion());
        if (cube == null)
        {
            throw new IllegalArgumentException("NCube '" + name + "' not loaded into NCubeManager, attempting fixed ($) reference to cell: " + coord.toString());
        }
        return cube.getCell(coord, output);
    }

    public Object getRelativeCell(Map coord)
    {
        input.putAll(coord);
        return ncube.getCell(input, output);
    }

    public Object getRelativeCubeCell(String name, Map coord)
    {
        input.putAll(coord);
        NCube cube = NCubeManager.getCube(name, ncube.getVersion());
        if (cube == null)
        {
            throw new IllegalArgumentException("NCube '" + name + "' not loaded into NCubeManager, attempting relative (@) reference to cell: " + coord.toString());
        }
        return cube.getCell(input, output);
    }

    public Object runRule(Map coord)
    {
        input.putAll(coord);
        return ncube.getCells(input, output);
    }

    public Object runRule(String name, Map coord)
    {
        input.putAll(coord);
        NCube cube = NCubeManager.getCube(name, ncube.getVersion());
        if (cube == null)
        {
            throw new IllegalArgumentException("NCube '" + name + "' not loaded into NCubeManager, attempting relative (@) reference to cell: " + coord.toString());
        }
        return cube.getCells(input, output);
    }

    public void ruleStop()
    {
        throw new RuleStop();
    }
}