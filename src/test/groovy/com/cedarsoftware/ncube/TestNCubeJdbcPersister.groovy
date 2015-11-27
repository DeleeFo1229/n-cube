package com.cedarsoftware.ncube
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail
import static org.mockito.Matchers.anyInt
import static org.mockito.Matchers.anyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the 'License')
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an 'AS IS' BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class TestNCubeJdbcPersister
{
    static final String APP_ID = "ncube.test";
    static final String USER_ID = "jdirt";

    private ApplicationID defaultSnapshotApp = new ApplicationID(ApplicationID.DEFAULT_TENANT, APP_ID, "1.0.0", ApplicationID.DEFAULT_STATUS, ApplicationID.TEST_BRANCH)

    @Before
    void setUp()
    {
        TestingDatabaseHelper.setupDatabase()
    }

    @After
    void tearDown()
    {
        TestingDatabaseHelper.tearDownDatabase()
    }

    @Test
    void testDbApis()
    {
        NCubePersister persister = TestingDatabaseHelper.persister

        NCube ncube1 = NCubeBuilder.testNCube3D_Boolean
        NCube ncube2 = NCubeBuilder.getTestNCube2D(true)

        persister.updateCube(defaultSnapshotApp, ncube1, USER_ID)
        persister.updateCube(defaultSnapshotApp, ncube2, USER_ID)

        Object[] cubeList = persister.search(defaultSnapshotApp, "test.%", null, [(NCubeManager.SEARCH_ACTIVE_RECORDS_ONLY) : true])

        assertTrue(cubeList != null)
        assertTrue(cubeList.length == 2)

        assertTrue(ncube1.numDimensions == 3)
        assertTrue(ncube2.numDimensions == 2)

        ncube1.deleteAxis("bu")
        ApplicationID next = defaultSnapshotApp.createNewSnapshotId("0.2.0")
        persister.updateCube(defaultSnapshotApp, ncube1, USER_ID)
        int numRelease = persister.releaseCubes(defaultSnapshotApp, "0.2.0")
        assertEquals(0, numRelease)

        cubeList = NCubeManager.search(next, 'test.*', null, [(NCubeManager.SEARCH_ACTIVE_RECORDS_ONLY):true])
        // Two cubes at the new 1.2.3 SNAPSHOT version.
        assert cubeList.length == 2

        // Verify that you cannot delete a RELEASE ncube
        try
        {
            assertFalse(persister.deleteCube(defaultSnapshotApp, ncube1.name, false, USER_ID))
        }
        catch (Exception e)
        {
            assertTrue(e.message.contains("not"))
            assertTrue(e.message.contains("delete"))
            assertTrue(e.message.contains("nable"))
            assertTrue(e.message.contains("find"))
        }
        try
        {
            assertFalse(persister.deleteCube(defaultSnapshotApp, ncube2.name, false, USER_ID))
        }
        catch (Exception e)
        {
            assertTrue(e.message.contains("not"))
            assertTrue(e.message.contains("delete"))
            assertTrue(e.message.contains("nable"))
            assertTrue(e.message.contains("find"))
        }

        // Delete new SNAPSHOT cubes
        assertTrue(persister.deleteCube(next, ncube1.name, false, USER_ID))
        assertTrue(persister.deleteCube(next, ncube2.name, false, USER_ID))

        // Ensure that all test ncubes are deleted
        cubeList = persister.search(defaultSnapshotApp, "test.%", null, ['activeRecordsOnly' : true])
        assertTrue(cubeList.length == 0)
    }

    @Test
    void testGetAppNamesWithSQLException()
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getAppNames(c, defaultSnapshotApp.DEFAULT_TENANT, null, ApplicationID.TEST_BRANCH)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('cannot be null or empty')
        }
    }

    @Test
    void testGetAppVersionsWithSQLException()
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getAppVersions(c, "DEFAULT", "FOO", "SNAPSHOT", null)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('cannot be null or empty')
        }
    }

    @Test
    void testUpdateBranchThatIsNotFound()
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps).thenReturn(ps).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs).thenReturn(rs)
        when(rs.next()).thenReturn(false)
        when(rs.getLong(1)).thenReturn(5L)
        when(rs.getDate(anyString())).thenReturn(new java.sql.Date(System.currentTimeMillis()))

        assertNull(new NCubeJdbcPersister().updateCube(c, defaultSnapshotApp, 0, USER_ID))
    }

    @Test
    void testReleaseCubesWithRuntimeExceptionWhileCreatingNewSnapshot()
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps).thenReturn(ps).thenReturn(ps).thenThrow(NullPointerException.class)
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(false)

        try
        {
            new NCubeJdbcPersister().releaseCubes(c, defaultSnapshotApp, "1.2.3")
            fail()
        }
        catch (NullPointerException e)
        {
            assert e.message == null
        }
    }

    @Test
    void testCommitCubeWithInvalidRevision()
    {
        try
        {
            new NCubeJdbcPersister().commitCube(null, defaultSnapshotApp, null, USER_ID)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.message.contains("cannot be empty"))
        }
    }

    @Test
    void testCommitCubeThatDoesntExist()
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps)
        when(c.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(ps)
        when(ps.executeQuery()).thenReturn(rs)
        when(ps.executeUpdate()).thenReturn(1).thenReturn(0);
        when(rs.next()).thenReturn(false)
        when(rs.getBytes("cube_value_bin")).thenReturn("".getBytes("UTF-8"))

        assertNull(new NCubeJdbcPersister().commitCube(c, defaultSnapshotApp, 1L, USER_ID))
    }

    @Test
    void testCreateBranchWithNullPointerException()
    {
        Connection c = getConnectionThatThrowsExceptionAfterExistenceCheck(false, NullPointerException.class)

        try
        {
            new NCubeJdbcPersister().createBranch(c, defaultSnapshotApp)
            fail()
        }
        catch (NullPointerException e)
        {
        }
    }

    @Test
    void testGetBranchesWithNullTenant()
    {
        Connection c = getConnectionThatThrowsSQLException()
        try
        {
            new NCubeJdbcPersister().getBranches(c, null)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.message.contains('tenant must not be null or empty'))
        }
    }

    private static getConnectionThatThrowsSQLException = { ->
        Connection c = mock(Connection.class)
        when(c.prepareStatement(anyString())).thenThrow(SQLException.class)
        when(c.createStatement()).thenThrow(SQLException.class)
        when(c.createStatement(anyInt(), anyInt())).thenThrow(SQLException.class)
        when(c.createStatement(anyInt(), anyInt(), anyInt())).thenThrow(SQLException.class)
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(c.metaData).thenReturn(metaData);
        when(metaData.getDriverName()).thenReturn("Oracle");
        return c;
    }

    private static Connection getConnectionThatThrowsExceptionAfterExistenceCheck(boolean exists, Class exceptionClass = SQLException.class) throws SQLException
    {
        Connection c = mock(Connection.class)
        PreparedStatement ps = mock(PreparedStatement.class)
        ResultSet rs = mock(ResultSet.class)
        when(c.prepareStatement(anyString())).thenReturn(ps).thenThrow(exceptionClass)
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(c.metaData).thenReturn(metaData);
        when(metaData.getDriverName()).thenReturn("HSQL");
        when(ps.executeQuery()).thenReturn(rs)
        when(rs.next()).thenReturn(exists)
        return c;
    }

    @Test
    void testUpdateBranchCubeWithNull()
    {
        try
        {
            new NCubeJdbcPersister().updateCube((Connection)null, (ApplicationID) null,(Long) null, null);
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.message.contains('cannot be empty'));
        }
    }

//    @Test
//    void testReleaseCubesWithCubeThatExistsAlready()
//    {
//        NCubeBuilder.getTestNCube2D(true)
//        Connection c = getConnectionThatThrowsExceptionAfterExistenceCheck(true)
//
//        try
//        {
//            new NCubeJdbcPersister().releaseCubes(c, defaultSnapshotApp, "1.2.3")
//            fail()
//        }
//        catch (IllegalStateException e)
//        {
//            assertTrue(e.message.contains("already exists"))
//        }
//    }

//    @Test
//    void testChangeVersionWhenCubeAlreadyExists()
//    {
//        NCubeBuilder.getTestNCube2D(true)
//        Connection c = getConnectionThatThrowsExceptionAfterExistenceCheck(true)
//        try
//        {
//            new NCubeJdbcPersister().changeVersionValue(c, defaultSnapshotApp, "1.1.1")
//            fail()
//        }
//        catch (IllegalStateException e)
//        {
//            assertTrue(e.message.contains("already"))
//            assertTrue(e.message.contains("exist"))
//        }
//    }
}
