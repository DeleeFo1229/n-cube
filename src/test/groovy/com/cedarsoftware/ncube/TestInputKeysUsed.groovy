package com.cedarsoftware.ncube

import groovy.transform.CompileStatic
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License")
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
@CompileStatic
class TestInputKeysUsed
{
    @Before
    public void setUp()
    {
        TestingDatabaseHelper.setupDatabase()
    }

    @After
    public void tearDown()
    {
        TestingDatabaseHelper.tearDownDatabase()
    }

    @Test
    void testKeyTracking()
    {
        NCube ncube = NCubeBuilder.getTrackingTestCube()
        Map input = [column: 'A', Age: 7]
        Map output = [:]
        def x = ncube.getCell(input, output)
        assert 'a1' == x
        RuleInfo ruleInfo = ncube.getRuleInfo(output)
        assert ruleInfo.getInputKeysUsed().size() == 2
        assert ruleInfo.getInputKeysUsed().contains('Column')
        assert ruleInfo.getInputKeysUsed().contains('Row')
    }

    @Test
    void testKeyTrackingWithContainsKeyAccess()
    {
        NCube ncube = NCubeBuilder.getTrackingTestCube()
        Map input = [column: 'B']
        Map output = [:]
        ncube.getCell(input, output)
        RuleInfo ruleInfo = ncube.getRuleInfo(output)
        assert ruleInfo.getInputKeysUsed().size() == 3
        assert ruleInfo.getInputKeysUsed().contains('Row')
        assert ruleInfo.getInputKeysUsed().contains('Column')
        assert ruleInfo.getInputKeysUsed().contains('smokes')

        input = [column: 'B', Row: 99]
        output = [:]
        ncube.getCell(input, output)
        ruleInfo = ncube.getRuleInfo(output)
        assert ruleInfo.getInputKeysUsed().size() == 3
        assert ruleInfo.getInputKeysUsed().contains('Column')
        assert ruleInfo.getInputKeysUsed().contains('Row')
        assert ruleInfo.getInputKeysUsed().contains('smokes')

        input = [column: 'B', Row: 99, SMOKES: null]
        output = [rate: 0]
        ncube.getCell(input, output)
        ruleInfo = ncube.getRuleInfo(output)
        assert ruleInfo.getInputKeysUsed().size() == 3
        assert ruleInfo.getInputKeysUsed().contains('Column')
        assert ruleInfo.getInputKeysUsed().contains('Row')
        assert ruleInfo.getInputKeysUsed().contains('smokes')
    }

    @Test
    void testKeyTrackingInputAccessedInCode()
    {
        NCube ncube = NCubeBuilder.getTrackingTestCube()
        Map input = [column: 'A', Row: 1, Age: 7]
        Map output = [:]
        ncube.getCell(input, output)
        RuleInfo ruleInfo = ncube.getRuleInfo(output)
        assert ruleInfo.getInputKeysUsed().size() == 4
        assert ruleInfo.getInputKeysUsed().contains('Column')
        assert ruleInfo.getInputKeysUsed().contains('Row')
        assert ruleInfo.getInputKeysUsed().contains('age')
        assert ruleInfo.getInputKeysUsed().contains('Weight')
    }

    @Test
    void testKeyTrackingAllInputProvided()
    {
        NCube ncube = NCubeBuilder.getTrackingTestCube()
        Map input = [column: 'A', Row: 1, Age: 7, weight: 210]
        Map output = [:]
        ncube.getCell(input, output)
        RuleInfo ruleInfo = ncube.getRuleInfo(output)
        assert ruleInfo.getInputKeysUsed().size() == 4
        assert ruleInfo.getInputKeysUsed().contains('Column')
        assert ruleInfo.getInputKeysUsed().contains('Row')
        assert ruleInfo.getInputKeysUsed().contains('age')
        assert ruleInfo.getInputKeysUsed().contains('Weight')
    }

    @Test
    void testKeyTrackingNested()
    {
        NCube ncube = NCubeBuilder.getTrackingTestCube()
        NCube ncube2 = NCubeBuilder.getTrackingTestCubeSecondary()
        NCubeManager.addCube(ApplicationID.testAppId, ncube)
        NCubeManager.addCube(ApplicationID.testAppId, ncube2)

        Map input = [Column: 'B', Row:1, state:'OH']
        Map output = [:]
        def x = ncube.getCell(input, output)
        assert 9 == x
        RuleInfo ruleInfo = ncube.getRuleInfo(output)

        assert ruleInfo.getInputKeysUsed().size() == 5
        assert ruleInfo.getInputKeysUsed().contains('Column')
        assert ruleInfo.getInputKeysUsed().contains('Row')
        assert ruleInfo.getInputKeysUsed().contains('state')
        assert ruleInfo.getInputKeysUsed().contains('age')
        assert ruleInfo.getInputKeysUsed().contains('weight')
    }

    // TODO: Test with RULE cube
}