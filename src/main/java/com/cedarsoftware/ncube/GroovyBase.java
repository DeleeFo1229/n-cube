package com.cedarsoftware.ncube;

import com.cedarsoftware.ncube.exception.CoordinateNotFoundException;
import com.cedarsoftware.ncube.exception.RuleStop;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for Groovy CommandCells.
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
public abstract class GroovyBase extends UrlCommandCell
{
    static final Pattern groovyAbsRefCubeCellPattern = Pattern.compile("([^a-zA-Z0-9_]|^)[$]([" + NCube.validCubeNameChars + "]+)[(]([^)]*)[)]");
    static final Pattern groovyAbsRefCubeCellPatternA = Pattern.compile("([^a-zA-Z0-9_]|^)[$]([" + NCube.validCubeNameChars + "]+)(\\[[^\\]]*\\])");
    static final Pattern groovyAbsRefCellPattern = Pattern.compile("([^a-zA-Z0-9_]|^)[$][(]([^)]*)[)]");
    static final Pattern groovyAbsRefCellPatternA = Pattern.compile("([^a-zA-Z0-9_]|^)[$](\\[[^\\]]*\\])");
    static final Pattern groovyRelRefCubeCellPattern = Pattern.compile("([^a-zA-Z0-9_]|^)@([" + NCube.validCubeNameChars + "]+)[(](.*?\\[.*?:.*?\\])[)]");
    static final Pattern groovyRelRefCellPattern = Pattern.compile("([^a-zA-Z0-9_]|^)@[(]([^)]*)[)]");
    static final Pattern groovyRelRefCellPatternA = Pattern.compile("([^a-zA-Z0-9_]|^)@(\\[[^\\]]*\\])");
    static final Pattern groovyProgramClassName = Pattern.compile("([^a-zA-Z0-9_])");
    static final Pattern groovyExplicitCubeRefPattern = Pattern.compile("ncubeMgr\\.getCube\\(['\"]([^']+)['\"]\\)");
    static final Pattern importPattern = Pattern.compile("import[\\s]+[^;]+?;");
    static final GroovyClassLoader groovyClassLoader = new GroovyClassLoader(GroovyBase.class.getClassLoader());
    static final Map<String, Class> compiledClasses = new LinkedHashMap<String, Class>();

    public GroovyBase(String cmd, boolean cache)
    {
        super(cmd, cache);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof GroovyBase))
        {
            return false;
        }

        GroovyBase that = (GroovyBase) other;
        return getCmd().equals(that.getCmd());
    }

    protected static String fixClassName(String name)
    {
        return groovyProgramClassName.matcher(name).replaceAll("_");
    }

    protected abstract String buildGroovy(String theirGroovy, String cubeName);

    protected abstract String getMethodToExecute(Map args);

    protected void preRun(Map args)
    {
        super.preRun(args);
        NCube ncube = (NCube) args.get("ncube");
        compileIfNeeded(ncube.getName());
    }

    protected Object runFinal(Map args)
    {
        try
        {
            Constructor c = getRunnableCode().getConstructor();
            Object groovyExecutableCell = c.newInstance();

            Method runMethod = getRunnableCode().getMethod("run", Map.class);
            return runMethod.invoke(groovyExecutableCell, args);
        }
        catch(InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof CoordinateNotFoundException)
            {
                throw (RuntimeException) cause;
            }
            else if (cause instanceof RuleStop)
            {
                throw (RuleStop) cause;
            }
            throw new RuntimeException("Exception occurred invoking method " + getMethodToExecute(args) + "()", e) ;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error occurred invoking method " + getMethodToExecute(args) + "()", e);
        }
    }

    /**
     * Conditionally compile the passed in command.  If it is already compiled, this method
     * immediately returns.  Insta-check because it is just a ref == null check.
     */
    private void compileIfNeeded(String cubeName)
    {
        if (getRunnableCode() == null)
        {   // Not yet compiled, compile the cell (Lazy compilation)
            synchronized(GroovyBase.class)
            {
                if (getRunnableCode() != null)
                {   // More than one thread saw the empty code, but only let the first thread
                    // call setRunnableCode().
                    return;
                }

                if (compiledClasses.containsKey(getCmdHash()))
                {   // Already been compiled, re-use class
                    setRunnableCode(compiledClasses.get(getCmdHash()));
                    return;
                }
                try
                {
                    compile(cubeName);
                }
                catch (Exception e)
                {
                    setCompileErrorMsg("Failed to compile Groovy Command '" + getCmd() + "', NCube '" + cubeName + "'");
                    throw new IllegalArgumentException(getCompileErrorMsg(), e);
                }
            }
        }
    }

    protected void compile(String cubeName) throws Exception
    {
        String groovy = buildGroovy(getCmd(), cubeName);
        String exp = expandNCubeShortCuts(groovy);

        // 2nd argument would be classname, if the groovy being compiled did not have a class XYZ name.
//        GroovyCodeSource grvCodeSrc = new GroovyCodeSource(exp, fixClassName(cubeName) + "_" + getCmdHash(), getCodeBase());
//        grvCodeSrc.setCachable(false);
//        setRunnableCode(groovyClassLoader.parseClass(grvCodeSrc, false));

        // 2nd argument would be classname, if the groovy being compiled did not have a class XYZ name.
        setRunnableCode(groovyClassLoader.parseClass(exp));

        compiledClasses.put(getCmdHash(), getRunnableCode());
    }

    protected abstract String getCodeBase();

    static String expandNCubeShortCuts(String groovy)
    {
        Matcher m = groovyAbsRefCubeCellPattern.matcher(groovy);
        String exp = m.replaceAll("$1getFixedCubeCell('$2',$3)");

        m = groovyAbsRefCubeCellPatternA.matcher(exp);
        exp = m.replaceAll("$1getFixedCubeCell('$2',$3)");

        m = groovyAbsRefCellPattern.matcher(exp);
        exp = m.replaceAll("$1getFixedCell($2)");

        m = groovyAbsRefCellPatternA.matcher(exp);
        exp = m.replaceAll("$1getFixedCell($2)");

        m = groovyRelRefCubeCellPattern.matcher(exp);
        exp = m.replaceAll("$1getRelativeCubeCell('$2',$3)");

        m = groovyRelRefCubeCellPatternA.matcher(exp);
        exp = m.replaceAll("$1getRelativeCubeCell('$2',$3)");

        m = groovyRelRefCellPattern.matcher(exp);
        exp = m.replaceAll("$1getRelativeCell($2)");

        m = groovyRelRefCellPatternA.matcher(exp);
        exp = m.replaceAll("$1getRelativeCell($2)");
        return exp;
    }

    public void getCubeNamesFromCommandText(final Set<String> cubeNames)
    {
        getCubeNamesFromText(cubeNames, getCmd());
    }

    static void getCubeNamesFromText(final Set<String> cubeNames, final String text)
    {
        Matcher m = groovyAbsRefCubeCellPattern.matcher(text);
        while (m.find())
        {
            cubeNames.add(m.group(2));  // based on Regex pattern - if pattern changes, this could change
        }

        m = groovyAbsRefCubeCellPatternA.matcher(text);
        while (m.find())
        {
            cubeNames.add(m.group(2));  // based on Regex pattern - if pattern changes, this could change
        }

        m = groovyRelRefCubeCellPattern.matcher(text);
        while (m.find())
        {
            cubeNames.add(m.group(2));  // based on Regex pattern - if pattern changes, this could change
        }

        m = groovyRelRefCubeCellPatternA.matcher(text);
        while (m.find())
        {
            cubeNames.add(m.group(2));  // based on Regex pattern - if pattern changes, this could change
        }

        m = groovyExplicitCubeRefPattern.matcher(text);
        while (m.find())
        {
            cubeNames.add(m.group(1));  // based on Regex pattern - if pattern changes, this could change
        }
    }

    /**
     * Find all occurrences of 'input.variableName' in the Groovy code
     * and add the variableName as a scope (key).
     * @param scopeKeys Set to add required scope keys to.
     */
    public void getScopeKeys(Set<String> scopeKeys)
    {
        Matcher m = inputVar.matcher(getCmd());
        while (m.find())
        {
            scopeKeys.add(m.group(2));
        }
    }

    public Set<String> getImports(String text, StringBuilder newGroovy)
    {
        Matcher m = importPattern.matcher(text);
        Set<String> importNames = new LinkedHashSet<String>();
        while (m.find())
        {
            importNames.add(m.group(0));  // based on Regex pattern - if pattern changes, this could change
        }

        m.reset();
        newGroovy.append(m.replaceAll(""));
        return importNames;
    }
}
