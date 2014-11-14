package com.cedarsoftware.ncube;

import com.cedarsoftware.util.io.JsonWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * NCubeManager Tests
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
public class TestNCubeManager
{
    static final String APP_ID = "ncube.test";
    static final String USER_ID = "jdirt";
    public static ApplicationID defaultSnapshotApp = new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, "1.0.0", ReleaseStatus.SNAPSHOT.name());

    @Before
    public void setUp() throws Exception
    {
        TestingDatabaseHelper.setupDatabase();
    }

    @After
    public void tearDown() throws Exception
    {
        TestingDatabaseHelper.tearDownDatabase();
    }

    private static NCube createCube() throws Exception
    {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        Map coord = new HashMap();
        coord.put("gender", "male");
        coord.put("age", "47");
        ncube.setCell(1.0, coord);

        coord.put("gender", "female");
        ncube.setCell(1.1, coord);

        coord.put("age", 16);
        ncube.setCell(1.5, coord);

        coord.put("gender", "male");
        ncube.setCell(1.8, coord);

        NCubeManager.createCube(defaultSnapshotApp, ncube, USER_ID);
        NCubeManager.updateTestData(defaultSnapshotApp, ncube.getName(), JsonWriter.objectToJson(coord));
        NCubeManager.updateNotes(defaultSnapshotApp, ncube.getName(), "notes follow");
        return ncube;
    }

    @Test
    public void testLoadCubes() throws Exception
    {
        NCube<Double> ncube = TestNCube.getTestNCube2D(true);

        Map coord = new HashMap();
        coord.put("gender", "male");
        coord.put("age", "47");
        ncube.setCell(1.0, coord);

        coord.put("gender", "female");
        ncube.setCell(1.1, coord);

        coord.put("age", 16);
        ncube.setCell(1.5, coord);

        coord.put("gender", "male");
        ncube.setCell(1.8, coord);

        String version = "0.1.0";
        String name1 = ncube.getName();

        ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, version, ReleaseStatus.SNAPSHOT.name());
        NCubeManager.createCube(appId, ncube, USER_ID);
        NCubeManager.updateTestData(appId, ncube.getName(), JsonWriter.objectToJson(coord));
        NCubeManager.updateNotes(appId, ncube.getName(), "notes follow");

        assertTrue(NCubeManager.doesCubeExist(appId, name1));

        ncube = TestNCube.getTestNCube3D_Boolean();
        String name2 = ncube.getName();
        NCubeManager.createCube(appId, ncube, USER_ID);

        NCubeManager.clearCache(appId);
        NCubeManager.getCubeRecordsFromDatabase(appId, "");

        NCube ncube1 = NCubeManager.getCube(appId, name1);
        NCube ncube2 = NCubeManager.getCube(appId, name2);
        assertNotNull(ncube1);
        assertNotNull(ncube2);
        assertEquals(name1, ncube1.getName());
        assertEquals(name2, ncube2.getName());
        assertTrue(NCubeManager.isCubeCached(appId, name1));
        assertTrue(NCubeManager.isCubeCached(appId, name2));
        NCubeManager.clearCache(appId);
        assertFalse(NCubeManager.isCubeCached(appId, name1));
        assertFalse(NCubeManager.isCubeCached(appId, name2));

        NCubeManager.deleteCube(appId, name1, true, USER_ID);
        NCubeManager.deleteCube(appId, name2, true, USER_ID);

        // Cubes are deleted ('allowDelete' was set to true above)
        assertFalse(NCubeManager.doesCubeExist(appId, name1));
        assertFalse(NCubeManager.doesCubeExist(appId, name2));

        Object[] cubeInfo = NCubeManager.getCubeRecordsFromDatabase(appId, name1);
        assertEquals(0, cubeInfo.length);
        cubeInfo = NCubeManager.getCubeRecordsFromDatabase(appId, name2);
        assertEquals(0, cubeInfo.length);
    }

    @Test
    public void testDoesCubeExistInvalidId()
    {
        try
        {
            NCubeManager.doesCubeExist(defaultSnapshotApp, null);
            fail();
        }
        catch(IllegalArgumentException e)
        {
            assertEquals("n-cube name cannot be null or empty", e.getMessage());
        }
    }

    @Test
    public void testDoesCubeExistInvalidName() {
        try
        {
            NCubeManager.doesCubeExist(null, "foo");
            fail();
        }
        catch(IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("cannot"));
            assertTrue(e.getMessage().contains("null"));
        }

    }

    //This exception is impossible to hit without mocking since we prohibit you on createCube() from
    //adding in a second duplicate cube with all the same parameters.
    @Test
    public void testGetReferencedCubesThatLoadsTwoCubes() throws Exception {
        try
        {
            Set<String> set = new HashSet<>();
            NCubeManager.getReferencedCubeNames(defaultSnapshotApp, "AnyCube", set);
            fail();
        } catch(IllegalArgumentException e) {
            assertNull(e.getCause());
        }
    }

    @Test
    public void testRenameWithMatchingNames() throws Exception
    {
        try
        {
            NCubeManager.renameCube(defaultSnapshotApp, "foo", "foo");
            fail();
        } catch(IllegalArgumentException e) {
            assertNull(e.getCause());
        }
    }

    @Test
    public void testBadCommandCellCommandWithJdbc() throws Exception
    {
        //setNCubePersister();
        NCube<Object> continentCounty = new NCube<>("test.ContinentCountries");
        continentCounty.setApplicationID(defaultSnapshotApp);
        NCubeManager.addCube(defaultSnapshotApp, continentCounty);
        continentCounty.addAxis(TestNCube.getContinentAxis());
        Axis countries = new Axis("Country", AxisType.DISCRETE, AxisValueType.STRING, true);
        countries.addColumn("Canada");
        countries.addColumn("USA");
        countries.addColumn("Mexico");
        continentCounty.addAxis(countries);

        NCube<Object> canada = new NCube<>("test.Provinces");
        canada.setApplicationID(defaultSnapshotApp);
        NCubeManager.addCube(defaultSnapshotApp, canada);
        canada.addAxis(TestNCube.getProvincesAxis());

        NCube<Object> usa = new NCube<>("test.States");
        usa.setApplicationID(defaultSnapshotApp);
        NCubeManager.addCube(defaultSnapshotApp, usa);
        usa.addAxis(TestNCube.getStatesAxis());

        Map coord1 = new HashMap();
        coord1.put("Continent", "North America");
        coord1.put("Country", "USA");
        coord1.put("State", "OH");

        Map coord2 = new HashMap();
        coord2.put("Continent", "North America");
        coord2.put("Country", "Canada");
        coord2.put("Province", "Quebec");

        continentCounty.setCell(new GroovyExpression("@test.States([:])", null), coord1);
        continentCounty.setCell(new GroovyExpression("$test.Provinces(crunch)", null), coord2);

        usa.setCell(1.0, coord1);
        canada.setCell(0.78, coord2);

        assertTrue((Double) continentCounty.getCell(coord1) == 1.0);

        try
        {
            assertTrue((Double) continentCounty.getCell(coord2) == 0.78);
            fail("should throw exception");
        }
        catch (Exception ignored)
        { }

        NCubeManager.createCube(defaultSnapshotApp, continentCounty, USER_ID);
        NCubeManager.createCube(defaultSnapshotApp, usa, USER_ID);
        NCubeManager.createCube(defaultSnapshotApp, canada, USER_ID);

        assertTrue(NCubeManager.getCubeNames(defaultSnapshotApp).size() == 3);

        // make sure items aren't in cache for next load from db for next getCubeNames call
        // during create they got added to database.
        NCubeManager.clearCache();

        assertTrue(NCubeManager.getCubeNames(defaultSnapshotApp).size() == 3);

        NCubeManager.clearCache();

        NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "");
        NCube test = NCubeManager.getCube(defaultSnapshotApp, "test.ContinentCountries");
        assertTrue((Double) test.getCell(coord1) == 1.0);

        NCubeManager.deleteCube(defaultSnapshotApp, "test.ContinentCountries", false, USER_ID);
        NCubeManager.deleteCube(defaultSnapshotApp, "test.States", false, USER_ID);
        NCubeManager.deleteCube(defaultSnapshotApp, "test.Provinces", false, USER_ID);
        assertTrue(NCubeManager.getCubeNames(defaultSnapshotApp).size() == 0);
    }

    @Test
    public void testGetReferencedCubeNames() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("template1.json");
        NCube n2 = NCubeManager.getNCubeFromResource("template2.json");

        NCubeManager.createCube(defaultSnapshotApp, n1, USER_ID);
        NCubeManager.createCube(defaultSnapshotApp, n2, USER_ID);

        Set refs = new TreeSet();
        NCubeManager.getReferencedCubeNames(defaultSnapshotApp, n1.getName(), refs);

        assertEquals(1, refs.size());
        assertTrue(refs.contains("Template2Cube"));

        refs.clear();
        NCubeManager.getReferencedCubeNames(defaultSnapshotApp, n2.getName(), refs);
        assertEquals(1, refs.size());
        assertTrue(refs.contains("Template1Cube"));

        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, n1.getName(), true, USER_ID));
        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, n2.getName(), true, USER_ID));

        try
        {
            NCubeManager.getReferencedCubeNames(defaultSnapshotApp, n2.getName(), null);
            fail();
        }
        catch (IllegalArgumentException ignored)
        { }
    }

    @Test
    public void testDuplicateNCube() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("stringIds.json");
        String ver = "1.1.1";
        NCubeManager.createCube(defaultSnapshotApp, n1, USER_ID);
        ApplicationID newId = new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, "1.1.2", ReleaseStatus.SNAPSHOT.name());

        NCubeManager.duplicate(defaultSnapshotApp, newId, n1.getName(), n1.getName(), USER_ID);
        NCube n2 = NCubeManager.getCube(defaultSnapshotApp, n1.getName());

        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, n1.getName(), true, USER_ID));
        assertTrue(NCubeManager.deleteCube(newId, n2.getName(), true, USER_ID));
        assertTrue(n1.equals(n2));
    }


    @Test
    public void testGetAppNames() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("stringIds.json");
        NCubeManager.createCube(defaultSnapshotApp, n1, USER_ID);

        Object[] names = NCubeManager.getAppNames(defaultSnapshotApp.getTenant());
        boolean foundName = false;
        for (Object name : names)
        {
            if ("ncube.test".equals(name))
            {
                foundName = true;
                break;
            }
        }

        Object[] vers = NCubeManager.getAppVersions(defaultSnapshotApp);
        boolean foundVer = false;
        String version = "1.0.0";
        for (Object ver : vers)
        {
            if (version.equals(ver))
            {
                foundVer = true;
                break;
            }
        }

        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, n1.getName(), true, USER_ID));
        assertTrue(foundName);
        assertTrue(foundVer);
    }


    @Test
    public void testChangeVersionValue() throws Exception
    {
        NCube n1 = NCubeManager.getNCubeFromResource("stringIds.json");
        ApplicationID newId = defaultSnapshotApp.createNewSnapshotId("1.1.20");

        assertNull(NCubeManager.getCube(defaultSnapshotApp, "idTest"));
        assertNull(NCubeManager.getCube(newId, "idTest"));
        NCubeManager.createCube(defaultSnapshotApp, n1, USER_ID);

        assertNotNull(NCubeManager.getCube(defaultSnapshotApp, "idTest"));
        assertNull(NCubeManager.getCube(newId, "idTest"));
        NCubeManager.changeVersionValue(defaultSnapshotApp, "1.1.20");

        assertNotNull(NCubeManager.getCube(newId, "idTest"));

        NCube n2 = NCubeManager.getCube(newId, "idTest");
        assertEquals(n1, n2);

        assertTrue(NCubeManager.deleteCube(newId, n1.getName(), true, USER_ID));
    }

    @Test
    public void testGetNCubes() throws Exception
    {
        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        NCube ncube2 = TestNCube.getTestNCube2D(true);

        NCubeManager.createCube(defaultSnapshotApp, ncube1, USER_ID);
        NCubeManager.createCube(defaultSnapshotApp, ncube2, USER_ID);

        Object[] cubeList = NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "test.%");

        assertTrue(cubeList != null);
        assertTrue(cubeList.length == 2);

        assertTrue(ncube1.getNumDimensions() == 3);
        assertTrue(ncube2.getNumDimensions() == 2);

        ncube1.deleteAxis("bu");
        NCubeManager.updateCube(defaultSnapshotApp, ncube1, USER_ID);
        NCube cube1 = NCubeManager.getCube(defaultSnapshotApp, "test.ValidTrailorConfigs");
        assertTrue(cube1.getNumDimensions() == 2);    // used to be 3

        assertTrue(3 == NCubeManager.releaseCubes(defaultSnapshotApp));

        // After the line below, there should be 4 test cubes in the database (2 @ version 0.1.1 and 2 @ version 0.2.0)
        NCubeManager.createSnapshotCubes(defaultSnapshotApp, "0.2.0");

        ApplicationID newId = defaultSnapshotApp.createNewSnapshotId("0.2.0");

        String notes1 = NCubeManager.getNotes(defaultSnapshotApp, "test.ValidTrailorConfigs");
        NCubeManager.getNotes(newId, "test.ValidTrailorConfigs");

        NCubeManager.updateNotes(defaultSnapshotApp, "test.ValidTrailorConfigs", null);
        notes1 = NCubeManager.getNotes(defaultSnapshotApp, "test.ValidTrailorConfigs");
        assertTrue("".equals(notes1));

        NCubeManager.updateNotes(defaultSnapshotApp, "test.ValidTrailorConfigs", "Trailer Config Notes");
        notes1 = NCubeManager.getNotes(defaultSnapshotApp, "test.ValidTrailorConfigs");
        assertTrue("Trailer Config Notes".equals(notes1));

        NCubeManager.updateTestData(newId, "test.ValidTrailorConfigs", null);
        String testData = NCubeManager.getTestData(newId, "test.ValidTrailorConfigs");
        assertTrue("".equals(testData));

        NCubeManager.updateTestData(newId, "test.ValidTrailorConfigs", "This is JSON data");
        testData = NCubeManager.getTestData(newId, "test.ValidTrailorConfigs");
        assertTrue("This is JSON data".equals(testData));

        // Verify that you cannot delete a RELEASE ncube
        try
        {
            NCubeManager.deleteCube(defaultSnapshotApp, ncube1.getName(), false, USER_ID);
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().contains("not"));
            assertTrue(e.getMessage().contains("delete"));
            assertTrue(e.getMessage().contains("nable"));
            assertTrue(e.getMessage().contains("find"));
        }
        try
        {
            NCubeManager.deleteCube(defaultSnapshotApp, ncube2.getName(), false, USER_ID);
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().contains("not"));
            assertTrue(e.getMessage().contains("delete"));
            assertTrue(e.getMessage().contains("nable"));
            assertTrue(e.getMessage().contains("find"));
        }

        // Delete ncubes using 'true' to allow the test to delete a released ncube.
        NCubeManager.deleteCube(defaultSnapshotApp, ncube1.getName(), true, USER_ID);
        NCubeManager.deleteCube(defaultSnapshotApp, ncube2.getName(), true, USER_ID);

        // Delete new SNAPSHOT cubes
        assertTrue(NCubeManager.deleteCube(newId, ncube1.getName(),  false, USER_ID));
        assertTrue(NCubeManager.deleteCube(newId, ncube2.getName(), false, USER_ID));

        // Ensure that all test ncubes are deleted
        cubeList = NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "test.%");
        assertTrue(cubeList.length == 0);
    }

    @Test
    public void testRenameNCube() throws Exception
    {
        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        NCube ncube2 = TestNCube.getTestNCube2D(true);

        NCubeManager.createCube(defaultSnapshotApp, ncube1, USER_ID);
        NCubeManager.createCube(defaultSnapshotApp, ncube2, USER_ID);

        NCubeManager.renameCube(defaultSnapshotApp, ncube1.getName(), "test.Floppy");

        Object[] cubeList = NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "test.%");

        assertTrue(cubeList.length == 2);

        NCubeInfoDto nc1 = (NCubeInfoDto) cubeList[0];
        NCubeInfoDto nc2 = (NCubeInfoDto) cubeList[1];

        assertTrue(nc1.toString().startsWith("NONE/ncube.test/1.0.0/SNAPSHOT/test.Age-Gender"));
        assertTrue(nc2.toString().startsWith("NONE/ncube.test/1.0.0/SNAPSHOT/test.Floppy"));

        assertTrue(nc1.name.equals("test.Floppy") || nc2.name.equals("test.Floppy"));
        assertFalse(nc1.name.equals("test.Floppy") && nc2.name.equals("test.Floppy"));

        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, "test.Floppy", true, USER_ID));
        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, ncube2.getName(),true, USER_ID));
    }

    @Test
    public void testNCubeManagerGetCubes() throws Exception
    {
        NCube ncube1 = TestNCube.getTestNCube3D_Boolean();
        NCube ncube2 = TestNCube.getTestNCube2D(true);

        NCubeManager.createCube(defaultSnapshotApp, ncube1, USER_ID);
        NCubeManager.createCube(defaultSnapshotApp, ncube2, USER_ID);

        // This proves that null is turned into '%' (no exception thrown)
        Object[] cubeList = NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, null);

        assertEquals(2, cubeList.length);
    }

    @Test
    public void testNCubeManagerUpdateCube() throws Exception
    {
        try
        {
            NCubeManager.updateCube(defaultSnapshotApp, null, USER_ID);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        NCube testCube = TestNCube.getTestNCube2D(false);
        try
        {
            ApplicationID id = new ApplicationID(ApplicationID.DEFAULT_TENANT, "DASHBOARD", ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());
            NCubeManager.updateCube(id, testCube, USER_ID);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().contains("rror updating"));
            assertTrue(e.getMessage().contains("no"));
            assertTrue(e.getMessage().contains("exist"));
        }
    }

    @Test
    public void testNCubeManagerCreateCubes() throws Exception
    {
        ApplicationID id = new ApplicationID(ApplicationID.DEFAULT_TENANT, "DASHBOARD", ApplicationID.DEFAULT_VERSION, ReleaseStatus.SNAPSHOT.name());
        try
        {
            NCubeManager.createCube(id, null, USER_ID);
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("cannot be null"));
        }

        NCube ncube1 = createCube();
        try
        {
            createCube();
            fail("Should not make it here");
        }
        catch (IllegalStateException e)
        {
            assertTrue(e.getMessage().contains("ube"));
            assertTrue(e.getMessage().contains("already exists"));
        }

        NCubeManager.deleteCube(defaultSnapshotApp, ncube1.getName(), true, USER_ID);
    }

    @Test
    public void testNCubeManagerCreateSnapshots() throws Exception
    {
        try
        {
            NCubeManager.createSnapshotCubes(defaultSnapshotApp, "1.0.0");
            fail("versions are not allowed to match");
        }
        catch (IllegalArgumentException ignore)
        {
            assertTrue(ignore.getMessage().contains("cannot be the same as the RELEASE version."));
        }

        try
        {
            createCube();
            NCubeManager.releaseCubes(defaultSnapshotApp);
            NCubeManager.createSnapshotCubes(defaultSnapshotApp, "0.1.1");
            NCubeManager.createSnapshotCubes(defaultSnapshotApp, "0.1.1");
            fail("should not make it here");
        }
        catch (IllegalStateException ignore)
        {
        }
    }

    @Test
    public void testNCubeManagerDeleteNotExistingCube() throws Exception
    {
        ApplicationID id = new ApplicationID(ApplicationID.DEFAULT_TENANT, "DASHBOARD", "0.1.0", ReleaseStatus.SNAPSHOT.name());
        try
        {
            NCubeManager.deleteCube(id, "DashboardRoles", true, USER_ID);
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().contains("not"));
            assertTrue(e.getMessage().contains("delete"));
            assertTrue(e.getMessage().contains("exist"));
        }
    }

    @Test
    public void testNotes() throws Exception
    {
        try
        {
            NCubeManager.getNotes(defaultSnapshotApp, "DashboardRoles");
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("not"));
            assertTrue(e.getMessage().contains("fetch"));
            assertTrue(e.getMessage().contains("notes"));
        }

        createCube();
        String notes = NCubeManager.getNotes(defaultSnapshotApp, "test.Age-Gender");
        assertNotNull(notes);
        assertTrue(notes.length() > 0);

        try
        {
            NCubeManager.updateNotes(defaultSnapshotApp, "test.funky", null);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().contains("not"));
            assertTrue(e.getMessage().contains("update"));
            assertTrue(e.getMessage().contains("exist"));
        }

        try
        {
            ApplicationID newId = defaultSnapshotApp.createNewSnapshotId("0.1.1");
            NCubeManager.getNotes(newId, "test.Age-Gender");
            fail("Should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("not"));
            assertTrue(e.getMessage().contains("fetch"));
            assertTrue(e.getMessage().contains("notes"));
        }

        NCubeManager.deleteCube(defaultSnapshotApp, "test.Age-Gender", true, USER_ID);
    }

    @Test
    public void testNCubeManagerTestData() throws Exception
    {
        try
        {
            NCubeManager.getTestData(defaultSnapshotApp, "DashboardRoles");
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }

        createCube();
        String testData = NCubeManager.getTestData(defaultSnapshotApp, "test.Age-Gender");
        assertNotNull(testData);
        assertTrue(testData.length() > 0);

        try
        {
            NCubeManager.updateTestData(defaultSnapshotApp, "test.funky", null);
            fail("should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().contains("no"));
            assertTrue(e.getMessage().contains("cube"));
            assertTrue(e.getMessage().contains("exist"));
        }

        ApplicationID newId = defaultSnapshotApp.createNewSnapshotId("0.1.1");
        try
        {
            NCubeManager.getTestData(newId, "test.Age-Gender");
            fail("Should not make it here");
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().contains("no"));
            assertTrue(e.getMessage().contains("cube"));
            assertTrue(e.getMessage().contains("exist"));
        }

        assertTrue(NCubeManager.deleteCube(defaultSnapshotApp, "test.Age-Gender", USER_ID));
    }


    @Test
    public void testEmptyNCubeMetaProps() throws Exception
    {
        NCube ncube = createCube();
        String json = ncube.toFormattedJson();
        ncube = NCube.fromSimpleJson(json);
        assertTrue(ncube.getMetaProperties().size() == 1);  // sha1

        List<Axis> axes = ncube.getAxes();
        for (Axis axis : axes)
        {
            assertTrue(axis.getMetaProperties().size() == 0);

            for (Column column : axis.getColumns())
            {
                assertTrue(column.getMetaProperties().size() == 0);
            }
        }
        NCubeManager.deleteCube(defaultSnapshotApp, ncube.getName(), true, USER_ID);
    }

//    @Test
//    public void testBadUrlsAddedToClassLoader() throws Exception
//    {
//        String url = "htp://this wont work";
//        List urls = new ArrayList();
//        urls.add(url);
//        try
//        {
//            ApplicationID appId = new ApplicationID(ApplicationID.DEFAULT_TENANT, ApplicationID.DEFAULT_APP, "2.0.0", ReleaseStatus.SNAPSHOT.name());
//            NCubeManager.addBaseResourceUrls(appId, urls);
//            fail("Should not make it here");
//        }
//        catch (IllegalArgumentException e)
//        {
//            assertTrue(e.getMessage().contains("malformed"));
//        }
//    }

    @Test
    public void testLoadCubesWithNullApplicationID() throws Exception
    {
        try
        {
            // This API is now package friendly and only to be used by tests or NCubeManager implementation work.
            NCubeManager.getCubeRecordsFromDatabase(null, "");
            fail();
        }
        catch(Exception ignored)
        { }
    }

    @Test(expected=RuntimeException.class)
    public void testGetNCubesFromResourceException() throws Exception
    {
        NCubeManager.getNCubesFromResource(null);
    }

    @Test
    public void testRestoreNonExistingCube() throws Exception
    {
        try
        {
            NCubeManager.restoreCube(defaultSnapshotApp, "fingers", USER_ID);
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("not"));
            assertTrue(e.getMessage().contains("restore"));
            assertTrue(e.getMessage().contains("exist"));
        }
    }

    @Test
    public void testRestoreExistingCube() throws Exception
    {
        NCube cube = createCube();
        try
        {
            NCubeManager.restoreCube(defaultSnapshotApp, cube.getName(), USER_ID);
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("already"));
            assertTrue(e.getMessage().contains("restored"));
        }
        NCubeManager.deleteCube(defaultSnapshotApp, cube.getName(), USER_ID);
    }

    @Test
    public void testRestoreDeletedCube() throws Exception
    {
        NCube cube = createCube();
        Object[] records = NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "");
        assertEquals(1, records.length);

        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(defaultSnapshotApp, "").length);

        NCubeManager.deleteCube(defaultSnapshotApp, cube.getName(), USER_ID);

        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(defaultSnapshotApp, "").length);

        records = NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "");
        assertEquals(0, records.length);
        assertTrue(NCubeManager.doesCubeExist(defaultSnapshotApp, cube.getName()));

        NCubeManager.restoreCube(defaultSnapshotApp, cube.getName(), USER_ID);
        records = NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "");
        assertEquals(1, records.length);
        NCubeInfoDto cubeInfo = (NCubeInfoDto) records[0];
        assertTrue(cubeInfo.notes.contains("restored"));
        assertTrue(cubeInfo.notes.contains("on "));
        assertTrue(cubeInfo.notes.contains("by "));

        NCubeManager.deleteCube(defaultSnapshotApp, cube.getName(), USER_ID);
    }

    @Test
    public void testRestoreCubeWithCubeThatDoesNotExist() throws Exception
    {
        try
        {
            NCubeManager.restoreCube(defaultSnapshotApp, "foo", USER_ID);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("not restore cube"));
            assertTrue(e.getMessage().contains("does not exist"));
        }

    }

    @Test
    public void testDeleteWithRevisions() throws Exception
    {
        NCube cube = createCube();
        assertEquals(1, NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "").length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(defaultSnapshotApp, null).length);
        assertEquals(1, NCubeManager.getRevisionHistory(defaultSnapshotApp, cube.getName()).length);

        Axis oddAxis = TestNCube.getOddAxis(true);
        cube.addAxis(oddAxis);

        NCubeManager.updateCube(defaultSnapshotApp, cube, USER_ID);
        assertEquals(2, NCubeManager.getRevisionHistory(defaultSnapshotApp, cube.getName()).length);
        assertEquals(1, NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "").length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(defaultSnapshotApp, "").length);

        Axis conAxis = TestNCube.getContinentAxis();
        cube.addAxis(conAxis);

        NCubeManager.updateCube(defaultSnapshotApp, cube, USER_ID);

        assertEquals(3, NCubeManager.getRevisionHistory(defaultSnapshotApp, cube.getName()).length);
        assertEquals(1, NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "").length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(defaultSnapshotApp, "").length);

        NCubeManager.deleteCube(defaultSnapshotApp, cube.getName(), USER_ID);

        assertEquals(0, NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "").length);
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(defaultSnapshotApp, "").length);
        assertEquals(4, NCubeManager.getRevisionHistory(defaultSnapshotApp, cube.getName()).length);
        assertTrue(NCubeManager.doesCubeExist(defaultSnapshotApp, cube.getName()));

        NCubeManager.restoreCube(defaultSnapshotApp, cube.getName(), USER_ID);

        assertEquals(1, NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "").length);
        assertEquals(0, NCubeManager.getDeletedCubesFromDatabase(defaultSnapshotApp, "").length);
        assertEquals(5, NCubeManager.getRevisionHistory(defaultSnapshotApp, cube.getName()).length);

        NCubeManager.deleteCube(defaultSnapshotApp, cube.getName(), USER_ID);

        assertEquals(0, NCubeManager.getCubeRecordsFromDatabase(defaultSnapshotApp, "").length);
        assertEquals(1, NCubeManager.getDeletedCubesFromDatabase(defaultSnapshotApp, "").length);
        assertEquals(6, NCubeManager.getRevisionHistory(defaultSnapshotApp, cube.getName()).length);
    }

    @Test
    public void testResolveClasspathWithInvalidUrl() throws Exception {
        NCube cube = NCubeManager.getNCubeFromResource("sys.classpath.invalid.url.json");
        NCubeManager.createCube(defaultSnapshotApp, cube, USER_ID);
        createCube();

        // force reload from hsql and reget classpath
        assertEquals(1, NCubeManager.getUrlClassLoader(defaultSnapshotApp).getURLs().length);

        NCubeManager.clearCache(defaultSnapshotApp);
        assertNull(NCubeManager.getUrlClassLoader(defaultSnapshotApp));

        NCubeManager.getCube(defaultSnapshotApp, "test.AgeGender");
        assertEquals(0, NCubeManager.getUrlClassLoader(defaultSnapshotApp).getURLs().length);
    }

    @Test
    public void testResolveClassPath() {
        loadTestClassPathCubes();

        Map map = new HashMap();
        map.put("env", "DEV");

        NCube baseCube = NCubeManager.getCube(defaultSnapshotApp, "sys.classpath.base");

        assertEquals("https://cdn.com/private/ud/ra-resources/1.19.1-SNAPSHOT/", baseCube.getCell(map));
        map.put("env", "CERT");
        assertEquals("https://cdn.com/private/ud/ra-resources/1.12.0/", baseCube.getCell(map));
        map.put("env", "LOCAL");
        assertEquals("file:///C:/Development/Java/Idea/RefApp/foo/src/main/", baseCube.getCell(map));
        map.put("username", "jderegnaucourt");
        assertEquals("file:///Users/jderegnaucourt/Development/foo/src/main/", baseCube.getCell(map));

        NCube classPathCube = NCubeManager.getCube(defaultSnapshotApp, "sys.classpath");
        List<String> list = (List<String>)classPathCube.getCell(map);
        assertEquals(3, list.size());
    }

    @Test
    public void testGetApplicationId() {
        loadTestClassPathCubes();
        loadTestBootstrapCubes();

        ApplicationID bootAppId = NCubeManager.getApplicationID(defaultSnapshotApp.getTenant(), defaultSnapshotApp.getApp(), null);
        assertEquals(defaultSnapshotApp, bootAppId);

        Map map = new HashMap();
        map.put("env", "DEV");

        bootAppId = NCubeManager.getApplicationID(defaultSnapshotApp.getTenant(), defaultSnapshotApp.getApp(), map);
        assertEquals(defaultSnapshotApp.getTenant(), bootAppId.getTenant());
        assertEquals(defaultSnapshotApp.getApp(), bootAppId.getApp());
        assertEquals(defaultSnapshotApp.getVersion(), "1.0.0");
        assertEquals(defaultSnapshotApp.getStatus(), bootAppId.getStatus());
    }

    @Test
    public void testEnsureLoadedException() {
        try {
            NCubeManager.ensureLoaded(null);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Failed"));
            assertTrue(e.getMessage().contains("retrieve cube from cache"));
        }
    }

    private void loadTestClassPathCubes() {
        NCube cube = NCubeManager.getNCubeFromResource(ApplicationID.defaultAppId, "sys.versions.json");
        NCubeManager.createCube(defaultSnapshotApp, cube, USER_ID);
        cube = NCubeManager.getNCubeFromResource("sys.classpath.local.json");
        NCubeManager.createCube(defaultSnapshotApp, cube, USER_ID);
        cube = NCubeManager.getNCubeFromResource("sys.classpath.json");
        NCubeManager.createCube(defaultSnapshotApp, cube, USER_ID);
        cube = NCubeManager.getNCubeFromResource("sys.classpath.base.json");
        NCubeManager.createCube(defaultSnapshotApp, cube, USER_ID);
    }

    private void loadTestBootstrapCubes() {
        ApplicationID appId = defaultSnapshotApp.createNewSnapshotId("0.0.0");

        NCube cube = NCubeManager.getNCubeFromResource(appId, "sys.bootstrap.json");
        NCubeManager.createCube(appId, cube, USER_ID);
        cube = NCubeManager.getNCubeFromResource("sys.version.json");
        NCubeManager.createCube(appId, cube, USER_ID);
        cube = NCubeManager.getNCubeFromResource("sys.status.json");
        NCubeManager.createCube(appId, cube, USER_ID);
    }




}
