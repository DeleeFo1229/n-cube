package com.cedarsoftware.ncube;

import com.cedarsoftware.util.ArrayUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.UniqueIdGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Class used to carry the NCube meta-information
 * to the client.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
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
public class NCubeJdbcPersister
{
    private static final Logger LOG = LogManager.getLogger(NCubeJdbcPersister.class);
    public static final String TEST_DATA_BIN = "test_data_bin";
    public static final String NOTES_BIN = "notes_bin";

    public void createCube(Connection c, ApplicationID appId, NCube cube, String username)
    {
        Long revision = getMaxRevision(c, appId, cube.name);

        if (revision != null && revision >= 0)
        {
            throw new IllegalStateException("Cannot create cube: " + cube.getName() + ".  It already exists (or existed) in app: " + appId + ".  If it was deleted, restore it.");
        }

        createCube(c, appId, cube, username, revision == null ? 0 : Math.abs(revision) + 1);
    }

    void createCube(Connection c, ApplicationID appId, NCube ncube, String username, long rev)
    {
        try
        {
            byte[] cubeData = StringUtilities.getBytes(ncube.toFormattedJson(), "UTF-8");
            if (!insertCube(c, appId, ncube.getName(), rev, cubeData, null, "Cube created", username))
            {
                throw new IllegalStateException("error inserting new n-cube: " + ncube.getName() + "', app: " + appId);
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to insert cube: " + ncube.getName() + ", app: " + appId + " into database";
            LOG.error(s, e);
            throw new IllegalStateException(s, e);
        }
    }

    void replaceHeadSha1(Connection c, ApplicationID appId, String cubeName, String sha1, Long revision)
    {
        String sql = "SELECT cube_value_bin FROM n_cube WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? AND revision_number = ?";

        try (PreparedStatement stmt = c.prepareStatement(sql))
        {
            stmt.setString(1, cubeName);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getStatus());
            stmt.setString(5, appId.getTenant());
            stmt.setString(6, appId.getBranch());
            stmt.setLong(7, revision);


            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] jsonBytes = rs.getBytes("cube_value_bin");

                    if (!ArrayUtilities.isEmpty(jsonBytes))
                    {
                        jsonBytes = replaceHeadSha1AndRemoveChanged(jsonBytes, sha1);
                    }

                    try (PreparedStatement insert = c.prepareStatement("UPDATE n_cube set cube_value_bin = ? WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? AND revision_number = ?"))
                    {
                        insert.setBytes(1, jsonBytes);
                        insert.setString(2, cubeName);
                        insert.setString(3, appId.getApp());
                        insert.setString(4, appId.getVersion());
                        insert.setString(5, appId.getStatus());
                        insert.setString(6, appId.getTenant());
                        insert.setString(7, appId.getBranch());
                        insert.setLong(8, revision);

                        if (insert.executeUpdate() != 1)
                        {
                            throw new IllegalStateException("error updating n-cube: " + cubeName + "', app: " + appId + ", row was not updated");
                        }
                    }
                }

            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to replace headSha1: " + cubeName + " from app: " + appId;
            LOG.error(s, e);
            throw new IllegalStateException(s, e);
        }
    }

    boolean copyBetweenBranches(Connection c, ApplicationID srcBranch, ApplicationID tgtBranch, String cubeName, String username, Long revision)
    {
        if (revision == null)
        {
            throw new IllegalArgumentException("Revision number cannot be null to copyBetweenBranches");
        }

        String sql = "SELECT n_cube_nm, app_cd, version_no_cd, status_cd, revision_number, branch_id, cube_value_bin, test_data_bin, notes_bin from n_cube WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? AND revision_number = ?";

        try (PreparedStatement stmt = c.prepareStatement(sql))
        {
            stmt.setString(1, cubeName);
            stmt.setString(2, srcBranch.getApp());
            stmt.setString(3, srcBranch.getVersion());
            stmt.setString(4, srcBranch.getStatus());
            stmt.setString(5, srcBranch.getTenant());
            stmt.setString(6, srcBranch.getBranch());
            stmt.setLong(7, revision);

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] jsonBytes = rs.getBytes("cube_value_bin");

                    if (tgtBranch.isHead())
                    {
                        jsonBytes = removeHeadSha1AndChangeType(jsonBytes);
                    }

                    Long maxRevision = getMaxRevision(c, tgtBranch, cubeName);

                    //  create case because maxrevision was not found.
                    if (maxRevision == null)
                    {
                        maxRevision = revision < 0 ? new Long(-1) : new Long(0);
                    }
                    else if (revision < 0)
                    {
                        // cube deleted in branch
                        maxRevision = -(Math.abs(maxRevision)+1);
                    }
                    else
                    {
                        maxRevision = Math.abs(maxRevision)+1;
                    }

                    byte[] testData = rs.getBytes(TEST_DATA_BIN);

                    if (!insertCube(c, tgtBranch, cubeName, maxRevision, jsonBytes, testData, "Cube committed", username))
                    {
                        String s = "Unable to copy cube: " + cubeName + " from app: " + srcBranch + " into app:  " + tgtBranch;
                        throw new IllegalStateException(s);
                    }
                    return true;
                }
                return false;
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to copy cube: " + cubeName + " from app: " + srcBranch + " into app:  " + tgtBranch;
            LOG.error(s, e);
            throw new IllegalStateException(s, e);
        }
    }

    public void updateCube(Connection connection, ApplicationID appId, NCube cube, String username)
    {
        try (PreparedStatement stmt = createSelectSingleCubeStatement(connection, appId, cube.name))
        {
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    Long revision = rs.getLong("revision_number");

                    if (revision < 0)
                    {
                        throw new IllegalArgumentException("Error updating cube: " + cube.getName() + ", app: " + appId + ", attempting to update deleted cube.  Restore it first.");
                    }

                    //TODO:  This code may be necessary for supporting Ken Sayer's loading from files on disk.
//                    String headSha1 = getHeadSha1(rs.getBytes("cube_value_bin"));
//                    cube.setHeadSha1(headSha1);
                    byte[] cubeData = StringUtilities.getBytes(cube.toFormattedJson(), "UTF-8");
                    byte[] testData = rs.getBytes(TEST_DATA_BIN);
                    byte[] notes = rs.getBytes(NOTES_BIN);

                    if (!insertCube(connection, appId, cube.name, revision + 1, cubeData, testData, "Cube updated", username))
                    {
                        throw new IllegalStateException("error updating n-cube: " + cube.name + "', app: " + appId + ", row was not updated");
                    }

                    return;
                }

                throw new IllegalArgumentException("Error updating cube: " + cube.getName() + ", app: " + appId + ", attempting to update non-existing cube.");
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to insert cube: " + cube.name + ", app: " + appId + " into database";
            LOG.error(s, e);
            throw new IllegalStateException(s, e);
        }
    }

    Long getMaxRevision(Connection c, ApplicationID appId, String name)
    {
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT revision_number FROM n_cube " +
                        "WHERE n_cube_nm = ? AND app_cd = ? AND status_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? " +
                        "ORDER BY abs(revision_number) DESC"))
        {
            stmt.setString(1, name);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getStatus());
            stmt.setString(4, appId.getVersion());
            stmt.setString(5, appId.getTenant());
            stmt.setString(6, appId.getBranch());

            try (ResultSet rs = stmt.executeQuery())
            {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
        catch (Exception e)
        {
            String s = "Unable to get maximum revision number for cube: " + name + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    Long getMinRevision(Connection c, ApplicationID appId, String cubeName)
    {
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT revision_number FROM n_cube " +
                        "WHERE n_cube_nm = ? AND app_cd = ? AND status_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? " +
                        "ORDER BY abs(revision_number) ASC"))
        {
            stmt.setString(1, cubeName);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getStatus());
            stmt.setString(4, appId.getVersion());
            stmt.setString(5, appId.getTenant());
            stmt.setString(6, appId.getBranch());

            try (ResultSet rs = stmt.executeQuery())
            {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
        catch (Exception e)
        {
            String s = "Unable to get maximum revision number for cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public PreparedStatement createSelectSingleCubeStatement(Connection c, ApplicationID appId, String cubeName) throws SQLException
    {
        String sql = "SELECT n.n_cube_nm, app_cd, version_no_cd, status_cd, n.revision_number, n.branch_id, n.cube_value_bin, n.test_data_bin, n.notes_bin " +
                "FROM n_cube n, " +
                "( " +
                "  SELECT n_cube_nm, max(abs(revision_number)) AS max_rev " +
                "  FROM n_cube " +
                "  WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?" +
                "  GROUP BY n_cube_nm " +
                ") m " +
                "WHERE m.n_cube_nm = n.n_cube_nm AND m.max_rev = abs(n.revision_number)" +
                " AND n.n_cube_nm = ? AND n.app_cd = ? AND n.version_no_cd = ? AND n.status_cd = ? AND n.tenant_cd = RPAD(?, 10, ' ') AND n.branch_id = ?";

        PreparedStatement s = c.prepareStatement(sql);
        s.setString(1, cubeName);
        s.setString(2, appId.getApp());
        s.setString(3, appId.getVersion());
        s.setString(4, appId.getStatus());
        s.setString(5, appId.getTenant());
        s.setString(6, appId.getBranch());
        s.setString(7, cubeName);
        s.setString(8, appId.getApp());
        s.setString(9, appId.getVersion());
        s.setString(10, appId.getStatus());
        s.setString(11, appId.getTenant());
        s.setString(12, appId.getBranch());
        return s;
    }

    public PreparedStatement createSelectSingleCubeStatement(Connection c, ApplicationID appId, String cubeName, Integer revision) throws SQLException
    {
        String sql = "SELECT n_cube_nm, app_cd, version_no_cd, status_cd, revision_number, branch_id, cube_value_bin FROM n_cube " +
                "WHERE n_cube_nm = ? AND app_cd = ? and version_no_cd = ? and status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? and revison_number = ?";


        PreparedStatement s = c.prepareStatement(sql);
        s.setString(1, cubeName);
        s.setString(2, appId.getApp());
        s.setString(3, appId.getVersion());
        s.setString(4, appId.getStatus());
        s.setString(5, appId.getTenant());
        s.setString(6, appId.getBranch());
        s.setLong(7, revision);
        return s;
    }

    public PreparedStatement createSelectCubesStatement(Connection c, ApplicationID appId, String pattern, boolean activeOnly, boolean deletedOnly, boolean includeTests) throws SQLException
    {
        if (activeOnly && deletedOnly)
        {
            throw new IllegalArgumentException("activeOnly and deletedOnly cannot both be true");
        }

        String nameCondition = "";
        if (StringUtilities.hasContent(pattern))
        {
            nameCondition = " AND n_cube_nm like ?";
        }

        String revisionCondition = "";
        if (activeOnly)
        {
            revisionCondition = " AND n.revision_number >= 0";
        }

        if (deletedOnly)
        {
            revisionCondition = " AND n.revision_number < 0";
        }

        String testFields = "";
        if (includeTests) {
            testFields = ", n.test_data_bin";
        }

        String sql = "SELECT n_cube_id, n.n_cube_nm, app_cd, n.notes_bin, version_no_cd, status_cd, n.create_dt, n.create_hid, n.revision_number, n.branch_id, n.cube_value_bin" +
                testFields +
                    " FROM n_cube n, " +
                    "( " +
                    "  SELECT n_cube_nm, max(abs(revision_number)) AS max_rev " +
                    "  FROM n_cube " +
                    "  WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?" +
                    nameCondition +
                    " GROUP BY n_cube_nm " +
                    ") m " +
                    "WHERE m.n_cube_nm = n.n_cube_nm AND m.max_rev = abs(n.revision_number) AND n.app_cd = ? AND n.version_no_cd = ? AND n.status_cd = ? AND n.tenant_cd = RPAD(?, 10, ' ') AND n.branch_id = ?" +
                    revisionCondition;

        if (StringUtilities.hasContent(pattern)) {
            sql += " AND m.n_cube_nm like ?";
        }

        PreparedStatement stmt = c.prepareStatement(sql);
        stmt.setString(1, appId.getApp());
        stmt.setString(2, appId.getVersion());
        stmt.setString(3, appId.getStatus());
        stmt.setString(4, appId.getTenant());
        stmt.setString(5, appId.getBranch());

        int i=6;
        if (pattern != null)
        {
            stmt.setString(i++, pattern);
        }

        stmt.setString(i++, appId.getApp());
        stmt.setString(i++, appId.getVersion());
        stmt.setString(i++, appId.getStatus());
        stmt.setString(i++, appId.getTenant());
        stmt.setString(i++, appId.getBranch());

        if (pattern != null)
        {
            stmt.setString(i++, pattern);
        }

        return stmt;
    }

    public Object[] getBranchChanges(Connection c, ApplicationID appId)
    {
        try (PreparedStatement s = createSelectCubesStatement(c, appId, null, false, false, false))
        {
            return getChangedRecords(appId, s);
        }
        catch (Exception e)
        {
            String s = "Unable to fetch branch cubes matching from database for app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Object[] getCubeRecords(Connection c, ApplicationID appId, String pattern, boolean activeOnly)
    {
        try (PreparedStatement s = createSelectCubesStatement(c, appId, convertPattern(pattern), activeOnly, false, false))
        {
            return getCubeInfoRecords(appId, s, activeOnly);
        }
        catch (Exception e)
        {
            String s = "Unable to fetch cubes matching '" + pattern + "' from database for app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Object[] getDeletedCubeRecords(Connection c, ApplicationID appId, String pattern)
    {
        try (PreparedStatement s = createSelectCubesStatement(c, appId, convertPattern(pattern), false, true, false))
        {
            return getCubeInfoRecords(appId, s, false);
        }
        catch (Exception e)
        {
            String s = "Unable to fetch deleted cubes from database for app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Object[] getRevisions(Connection c, ApplicationID appId, String cubeName)
    {
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT n_cube_id, n_cube_nm, notes_bin, version_no_cd, status_cd, app_cd, create_dt, create_hid, revision_number, branch_id, cube_value_bin " +
                        "FROM n_cube " +
                        "WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND status_cd = ? AND branch_id = ?" +
                        "ORDER BY abs(revision_number) DESC"))
        {
            stmt.setString(1, cubeName);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());
            stmt.setString(5, appId.getStatus());
            stmt.setString(6, appId.getBranch());

            Object[] records = getCubeInfoRecords(appId, stmt, false);
            if (records.length == 0)
            {
                throw new IllegalArgumentException("Cannot fetch revision history for cube:  " + cubeName + " as it does not exist in app:  " + appId);
            }
            return records;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to get revision history for cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    private Object[] getChangedRecords(ApplicationID appId, PreparedStatement stmt) throws Exception
    {
        List<NCubeInfoDto> list = new ArrayList<>();

        try (ResultSet rs = stmt.executeQuery())
        {
            while (rs.next())
            {
                NCubeInfoDto dto = new NCubeInfoDto();
                dto.name = rs.getString("n_cube_nm");
                dto.branch = rs.getString("branch_id");
                dto.tenant = appId.getTenant();
                byte[] notes = rs.getBytes(NOTES_BIN);
                dto.notes = new String(notes == null ? "".getBytes() : notes, "UTF-8");
                dto.version = appId.getVersion();
                dto.status = rs.getString("status_cd");
                dto.app = appId.getApp();
                dto.createDate = rs.getDate("create_dt");
                dto.createHid = rs.getString("create_hid");
                dto.revision = Long.toString(rs.getLong("revision_number"));
                byte[] jsonBytes = rs.getBytes("cube_value_bin");

                if (!ArrayUtilities.isEmpty(jsonBytes))
                {
                    String json = StringUtilities.createString(jsonBytes, "UTF-8");
                    Matcher m = Regexes.changedPattern.matcher(json);
                    if (m.find())
                    {
                        dto.changed = true;
                    }

                    m = Regexes.sha1Pattern.matcher(json);
                    if (m.find())
                    {
                        dto.sha1 = m.group(1);
                    }

                    //  Have to pull out the original head sha1 from which this branch was made.
                    //  We cannot calculate this on saves so has to be stored.
                    m = Regexes.headSha1Pattern.matcher(json);
                    if (m.find())
                    {
                        dto.headSha1 = m.group(1);
                    }

                    if (dto.isChanged()) {
                        list.add(dto);
                    }
                }

            }
        }
        return list.toArray();
    }

    private Object[] getCubeInfoRecords(ApplicationID appId, PreparedStatement stmt, boolean activeOnly) throws Exception
    {
        List<NCubeInfoDto> list = new ArrayList<>();

        try (ResultSet rs = stmt.executeQuery())
        {
            while (rs.next())
            {
                NCubeInfoDto dto = new NCubeInfoDto();
                dto.name = rs.getString("n_cube_nm");
                dto.branch = appId.getBranch();
                dto.tenant = appId.getTenant();
                byte[] notes = rs.getBytes(NOTES_BIN);
                dto.notes = new String(notes == null ? "".getBytes() : notes, "UTF-8");
                dto.version = appId.getVersion();
                dto.status = rs.getString("status_cd");
                dto.app = appId.getApp();
                dto.createDate = rs.getDate("create_dt");
                dto.createHid = rs.getString("create_hid");
                dto.revision = Long.toString(rs.getLong("revision_number"));
                byte[] jsonBytes = rs.getBytes("cube_value_bin");

                if (!ArrayUtilities.isEmpty(jsonBytes))
                {
                    String json = StringUtilities.createString(jsonBytes, "UTF-8");
                    Matcher m = Regexes.sha1Pattern.matcher(json);
                    if (m.find() && m.groupCount() > 0)
                    {
                        dto.sha1 = m.group(1);
                    }

                    //  Only do this when not head.
                    if (!ApplicationID.HEAD.equals(appId.getBranch()))
                    {
                        //  Have to pull out the original head sha1 from which this branch was made.
                        //  We cannot calculate this on saves so has to be stored.
                        m = Regexes.headSha1Pattern.matcher(json);
                        if (m.find() && m.groupCount() > 0)
                        {
                            dto.headSha1 = m.group(1);
                        }

                        m = Regexes.changedPattern.matcher(json);
                        if (m.find())
                        {
                            dto.changed = true;
                        }

                    }
                }

                if (!activeOnly || !dto.revision.startsWith("-"))
                {
                    list.add(dto);
                }
            }
        }
        return list.toArray();
    }

    public NCube loadCube(Connection c, ApplicationID appId, String cubeName)
    {
        try (PreparedStatement stmt = createSelectSingleCubeStatement(c, appId, cubeName))
        {
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] jsonBytes = rs.getBytes("cube_value_bin");
                    String json = new String(jsonBytes, "UTF-8");
                    NCube ncube = NCubeManager.ncubeFromJson(json);
                    ncube.setApplicationID(appId);
                    return ncube;
                }
            }
        }
        catch (Exception e)
        {
            String s = "Unable to load cube: " + appId + ", " + cubeName + " from database";
            LOG.error(s, e);
            throw new IllegalStateException(s, e);
        }

        throw new IllegalArgumentException("Unable to load cube: " + appId + ", " + cubeName + " from database");
    }

    public NCube loadCube(Connection c, ApplicationID appId, String cubeName, Integer revision)
    {
        try (PreparedStatement stmt = createSelectSingleCubeStatement(c, appId, cubeName, revision))
        {
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] jsonBytes = rs.getBytes("cube_value_bin");
                    String json = new String(jsonBytes, "UTF-8");
                    NCube ncube = NCubeManager.ncubeFromJson(json);
                    ncube.setApplicationID(appId);
                    return ncube;
                }
            }
        }
        catch (Exception e)
        {
            String s = "Unable to load cube: " + appId + ", " + cubeName + " from database";
            LOG.error(s, e);
            throw new IllegalStateException(s, e);
        }

        throw new IllegalArgumentException("Unable to load cube revision: " + appId + ", " + cubeName + ", " + revision + " from database");
    }

    public void restoreCube(Connection c, ApplicationID appId, String cubeName, String username)
    {
        try (PreparedStatement stmt = createSelectSingleCubeStatement(c, appId, cubeName))
        {
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    Long revision = rs.getLong("revision_number");

                    if (revision >= 0)
                    {
                        throw new IllegalArgumentException("Cube: " + cubeName + " is already restored in app: " + appId);
                    }

                    byte[] jsonBytes = rs.getBytes("cube_value_bin");
                    jsonBytes = setChanged(jsonBytes);
                    byte[] testData = rs.getBytes(TEST_DATA_BIN);
                    String notes = "Cube restored";

                    if (!insertCube(c, appId, cubeName, Math.abs(revision) + 1, jsonBytes, testData, notes, username))
                    {
                        throw new IllegalStateException("Could not restore n-cube: " + cubeName + "', app: " + appId);
                    }
                }
                else
                {
                    throw new IllegalArgumentException("Cannot restore cube: " + cubeName + " as it does not exist in app: " + appId);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (SQLException e) {
            String s = "Unable to restore cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean deleteCubes(Connection c, ApplicationID appId)
    {
        String sql = "DELETE FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?";

        try (PreparedStatement ps = c.prepareStatement(sql))
        {
            ps.setString(1, appId.getApp());
            ps.setString(2, appId.getVersion());
            ps.setString(3, appId.getTenant());
            ps.setString(4, appId.getBranch());
            return ps.executeUpdate() > 0;
        }
        catch (Exception e)
        {
            String s = "Unable to delete cubes for app: " + appId + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

    }

    public boolean rollbackCube(Connection c, ApplicationID appId, String cubeName)
    {
        Long revision = getMinRevision(c, appId, cubeName);

        if (revision == null) {
            throw new IllegalArgumentException("Could not rollback cube.  Cube was not found.  App:  " + appId + ", cube: " + cubeName);
        }

        String sql = "DELETE FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND status_cd = ? AND branch_id = ? AND n_cube_nm = ? AND revision_number <> ?";

        try (PreparedStatement s = c.prepareStatement(sql))
        {
            s.setString(1, appId.getApp());
            s.setString(2, appId.getVersion());
            s.setString(3, appId.getTenant());
            s.setString(4, appId.getStatus());
            s.setString(5, appId.getBranch());
            s.setString(6, cubeName);
            s.setLong(7, revision);
            return s.executeUpdate() > 0;
        }
        catch (Exception e)
        {
            String s = "Unable to rollback cube: " + cubeName + " for app: " + appId + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

    }

    public boolean deleteCube(Connection c, ApplicationID appId, String cubeName, boolean allowDelete, String username)
    {
        if (allowDelete)
        {
            String sql = "DELETE FROM n_cube WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?";

            try (PreparedStatement ps = c.prepareStatement(sql))
            {
                ps.setString(1, appId.getApp());
                ps.setString(2, cubeName);
                ps.setString(3, appId.getVersion());
                ps.setString(4, appId.getTenant());
                ps.setString(5, appId.getBranch());
                return ps.executeUpdate() > 0;
            }
            catch (Exception e)
            {
                String s = "Unable to delete cube: " + cubeName + ", app: " + appId + " from database";
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
        else
        {
            try (PreparedStatement stmt = createSelectSingleCubeStatement(c, appId, cubeName))
            {
                try (ResultSet rs = stmt.executeQuery())
                {
                    if (rs.next())
                    {
                        Long revision = rs.getLong("revision_number");

                        if (revision < 0)
                        {
                            return false;
                        }

                        byte[] jsonBytes = rs.getBytes("cube_value_bin");
                        byte[] testData = rs.getBytes(TEST_DATA_BIN);

                        jsonBytes = setChanged(jsonBytes);

                        if (!insertCube(c, appId, cubeName, -(revision + 1), jsonBytes, testData, "Cube deleted", username))
                        {
                            throw new IllegalStateException("Cannot delete n-cube: " + cubeName + "', app: " + appId + ", row was not deleted");
                        }
                        return true;
                    }
                    //  TODO:  In restoreCube() and updateCube() we throw an exception when there is nothing to update.
                    //  TODO:  For consistency do we want to do the same thing here?
                    return false;
                }
            }
            catch (RuntimeException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                String s = "Unable to delete cube: " + cubeName + ", app: " + appId;
                LOG.error(s, e);
                throw new RuntimeException(s, e);
            }
        }
    }

    public boolean updateNotes(Connection c, ApplicationID appId, String cubeName, String notes)
    {
        Long maxRev = getMaxRevision(c, appId, cubeName);
        if (maxRev == null)
        {
            throw new IllegalArgumentException("Cannot update notes, cube: " + cubeName + " does not exist in app: " + appId);
        }
        if (maxRev < 0)
        {
            throw new IllegalArgumentException("Cannot update notes, cube: " + cubeName + " is deleted in app: " + appId);
        }

        String statement = "UPDATE n_cube SET notes_bin = ? WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? AND revision_number = ?";

        try (PreparedStatement stmt = c.prepareStatement(statement))
        {
            stmt.setBytes(1, notes == null ? null : notes.getBytes("UTF-8"));
            stmt.setString(2, appId.getApp());
            stmt.setString(3, cubeName);
            stmt.setString(4, appId.getVersion());
            stmt.setString(5, appId.getTenant());
            stmt.setString(6, appId.getBranch());
            stmt.setLong(7, maxRev);
            return stmt.executeUpdate() == 1;
        }
        catch (Exception e)
        {
            String s = "Unable to update notes for cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public String getNotes(Connection c, ApplicationID appId, String cubeName)
    {
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT notes_bin FROM n_cube " +
                        "WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?" +
                        "ORDER BY abs(revision_number) DESC"
        ))
        {
            stmt.setString(1, appId.getApp());
            stmt.setString(2, cubeName);
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());
            stmt.setString(5, appId.getBranch());
            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] notes = rs.getBytes("notes_bin");
                    return new String(notes == null ? "".getBytes() : notes, "UTF-8");
                }
            }
            throw new IllegalArgumentException("Could not fetch notes, no cube: " + cubeName + " in app: " + appId);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to fetch notes for cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }


    public int createBranch(Connection c, ApplicationID appId)
    {
        if (doCubesExist(c, appId))
        {
            throw new IllegalStateException("Branch already exists, app: " + appId);
        }

        try
        {
            ApplicationID headId = appId.asHead();
            try (PreparedStatement stmt = createSelectCubesStatement(c, headId, null, false, false, true))
            {
                try (ResultSet rs = stmt.executeQuery())
                {
                    try (PreparedStatement insert = c.prepareStatement(
                            "INSERT INTO n_cube (n_cube_id, n_cube_nm, cube_value_bin, create_dt, create_hid, version_no_cd, status_cd, app_cd, test_data_bin, notes_bin, tenant_cd, branch_id, revision_number) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"))
                    {
                        int count = 0;
                        while (rs.next())
                        {
                            insert.setLong(1, UniqueIdGenerator.getUniqueId());
                            insert.setString(2, rs.getString("n_cube_nm"));

                            byte[] jsonBytes = rs.getBytes("cube_value_bin");

                            if (!ArrayUtilities.isEmpty(jsonBytes))
                            {
                                jsonBytes = injectHeadSha1FromSha1(jsonBytes);
                            }
                            insert.setBytes(3, jsonBytes);

                            insert.setDate(4, new java.sql.Date(System.currentTimeMillis()));
                            insert.setString(5, rs.getString("create_hid"));
                            insert.setString(6, appId.getVersion());
                            insert.setString(7, ReleaseStatus.SNAPSHOT.name());
                            insert.setString(8, appId.getApp());
                            insert.setBytes(9, rs.getBytes(TEST_DATA_BIN));
                            insert.setBytes(10, rs.getBytes(NOTES_BIN));
                            insert.setString(11, appId.getTenant());
                            insert.setString(12, appId.getBranch());
                            insert.setLong(13, (rs.getLong("revision_number") >= 0) ? 0 : -1);
                            insert.addBatch();
                            count++;
                        }
                        if (count > 0)
                        {
                            insert.executeBatch();
                        }
                        return count;
                    }
                }
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to create new BRANCH for app: " + appId + ", due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

//  TODO:  Might be needed for loading database with cubes from disk
//    public String getHeadSha1(byte[] jsonBytes)
//    {
//        StringBuffer sb = new StringBuffer();
//        String json = StringUtilities.createString(jsonBytes, "UTF-8");
//        Matcher m = Regexes.headSha1Pattern.matcher(json);
//        if (m.find() && m.groupCount() > 0)
//        {
//            return m.group(1);
//        }
//        return null;
//    }

    private byte[] injectHeadSha1FromSha1(byte[] jsonBytes)
    {
        StringBuffer sb = new StringBuffer();
        String json = StringUtilities.createString(jsonBytes, "UTF-8");
        Matcher m = Regexes.sha1Pattern.matcher(json);
        if (m.find() && m.groupCount() > 0)
        {
            String replacement = ", \"sha1\":\"" + m.group(1) + "\", \"headSha1\":\"" + m.group(1) + "\"";
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return StringUtilities.getBytes(sb.toString(), "UTF-8");
    }

    private byte[] removeHeadSha1AndChangeType(byte[] jsonBytes)
    {
        StringBuffer sb = new StringBuffer();
        String json = StringUtilities.createString(jsonBytes, "UTF-8");
        Matcher m = Regexes.headSha1orChangedPattern.matcher(json);
        //  replace headSha1 with existing sha1
        while (m.find())
        {
            m.appendReplacement(sb, "");
        }
        m.appendTail(sb);
        json = sb.toString();

        return StringUtilities.getBytes(json, "UTF-8");
    }

    private byte[] removeHeadSha1(byte[] jsonBytes)
    {
        StringBuffer sb = new StringBuffer();
        String json = StringUtilities.createString(jsonBytes, "UTF-8");

        Matcher m = Regexes.headSha1Pattern.matcher(json);
        //  replace headSha1 with existing sha1
        //  may not exist in the case of a cube created on branch.
        while (m.find())
        {
            m.appendReplacement(sb, "");
        }
        m.appendTail(sb);
        json = sb.toString();
        return StringUtilities.getBytes(json, "UTF-8");
    }

    private byte[] replaceHeadSha1(byte[] bytes, String newSha1)
    {
        StringBuffer sb = new StringBuffer();

        String json = StringUtilities.createString(bytes, "UTF-8");
        Matcher m = Regexes.headSha1Pattern.matcher(json);
        //  replace headSha1 with existing sha1
        //  may not exist in the case of a cube created on branch.
        while (m.find())
        {
            m.appendReplacement(sb, ", \"headSha1\":\"" + newSha1 + "\"");
        }

        m.appendTail(sb);
        return StringUtilities.getBytes(sb.toString(), "UTF-8");
    }

    private byte[] replaceHeadSha1AndRemoveChanged(byte[] jsonBytes, String newSha1)
    {
        StringBuffer sb = new StringBuffer();
        String json = StringUtilities.createString(jsonBytes, "UTF-8");

        Matcher m = Regexes.headSha1orChangedPattern.matcher(json);
        //  replace headSha1 with existing sha1
        //  may not exist in the case of a cube created on branch.
        while (m.find())
        {
            if (m.groupCount() > 0 && m.group().contains("headSha1")) {
                m.appendReplacement(sb, ", \"headSha1\":\"" + newSha1 + "\"");
            } else {
                m.appendReplacement(sb, "");
            }
        }
        m.appendTail(sb);
        json = sb.toString();

        //  If we don't find it, it was a create case.
        m = Regexes.headSha1Pattern.matcher(json);

        if (!m.find()) {
            //  created on branch and after commit now needs his own headSha1 value.
            return injectHeadSha1FromSha1(StringUtilities.getBytes(json, "UTF-8"));
        }

        return StringUtilities.getBytes(json, "UTF-8");
    }

    private byte[] setChanged(byte[] jsonBytes)
    {
        StringBuffer sb = new StringBuffer();
        String json = StringUtilities.createString(jsonBytes, "UTF-8");
        Matcher m = Regexes.changedPattern.matcher(json);
        if (m.find())
        {
            m.appendReplacement(sb, ", \"changed\":true");
            m.appendTail(sb);
        }
        else
        {
            m = Regexes.sha1Pattern.matcher(json);
            if (m.find()) {
                m.appendReplacement(sb, ", \"sha1\":\"" + m.group(1) + "\", \"changed\":true");
                m.appendTail(sb);
            }

        }
        return StringUtilities.getBytes(sb.toString(), "UTF-8");
    }


    public int releaseCubes(Connection c, ApplicationID appId, String newSnapVer)
    {
        if (doReleaseCubesExist(c, appId))
        {
            throw new IllegalStateException("A RELEASE version " + appId.getVersion() + " already exists, app: " + appId);
        }

        // Step 1: Update version number to new version where branch != HEAD (and rest of appId matches) ignore revision
        try
        {
            try (PreparedStatement statement = c.prepareStatement(
                    "UPDATE n_cube SET version_no_cd = ? " +
                    "WHERE app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id != 'HEAD'"))
            {
                statement.setString(1, newSnapVer);
                statement.setString(2, appId.getApp());
                statement.setString(3, appId.getVersion());
                statement.setString(4, appId.getTenant());
                statement.executeUpdate();
            }
        }
        catch (Exception e)
        {
            String s = "Unable to move branched snapshot cubes for app: " + appId + ", due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

        // Step 2: Release cubes where branch == HEAD (change their status from SNAPSHOT to RELEASE)
        try
        {
            try (PreparedStatement statement = c.prepareStatement(
                    "UPDATE n_cube SET create_dt = ?, status_cd = ? " +
                    "WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = 'HEAD'"))
            {
                statement.setDate(1, new java.sql.Date(System.currentTimeMillis()));
                statement.setString(2, ReleaseStatus.RELEASE.name());
                statement.setString(3, appId.getApp());
                statement.setString(4, appId.getVersion());
                statement.setString(5, ReleaseStatus.SNAPSHOT.name());
                statement.setString(6, appId.getTenant());
                statement.executeUpdate();
            }
        }
        catch (Exception e)
        {
            String s = "Unable to release head cubes for app: " + appId + ", due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }

        // Step 3: Create new SNAPSHOT cubes from the HEAD RELEASE cubes (next version higher, started for development)
        try
        {
            ApplicationID releaseId = appId.asRelease();
            try (PreparedStatement stmt = createSelectCubesStatement(c, releaseId, null, true, false, true))
            {
                try (ResultSet rs = stmt.executeQuery())
                {
                    int count = 0;
                    try (PreparedStatement insert = c.prepareStatement(
                            "INSERT INTO n_cube (n_cube_id, n_cube_nm, cube_value_bin, create_dt, create_hid, version_no_cd, status_cd, app_cd, test_data_bin, notes_bin, tenant_cd, branch_id, revision_number) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"))
                    {
                        while (rs.next())
                        {
                            count++;
                            insert.setLong(1, UniqueIdGenerator.getUniqueId());
                            insert.setString(2, rs.getString("n_cube_nm"));
                            insert.setBytes(3, rs.getBytes("cube_value_bin"));
                            insert.setDate(4, new java.sql.Date(System.currentTimeMillis()));
                            insert.setString(5, rs.getString("create_hid"));
                            insert.setString(6, newSnapVer);
                            insert.setString(7, ReleaseStatus.SNAPSHOT.name());
                            insert.setString(8, appId.getApp());
                            insert.setBytes(9, rs.getBytes(TEST_DATA_BIN));
                            insert.setBytes(10, rs.getBytes(NOTES_BIN));
                            insert.setString(11, appId.getTenant());
                            insert.setString(12, ApplicationID.HEAD);
                            insert.setLong(13, 0); // New SNAPSHOT revision numbers start at 0, we don't move forward deleted records.
                            insert.addBatch();
                        }

                        if (count > 0)
                        {
                            insert.executeBatch();
                        }
                    }
                    return count;
                }
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to create SNAPSHOT cubes for app: " + appId + ", new version: " + newSnapVer + ", due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public int changeVersionValue(Connection c, ApplicationID appId, String newVersion)
    {
        ApplicationID newSnapshot = appId.createNewSnapshotId(newVersion);
        if (doCubesExist(c, newSnapshot))
        {
            throw new IllegalStateException("Cannot change version value to " + newVersion + " because this version already exists.  Choose a different version number, app: " + appId);
        }

        try
        {
            try (PreparedStatement ps = c.prepareStatement("UPDATE n_cube SET version_no_cd = ? WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?"))
            {
                ps.setString(1, newVersion);
                ps.setString(2, appId.getApp());
                ps.setString(3, appId.getVersion());
                ps.setString(4, ReleaseStatus.SNAPSHOT.name());
                ps.setString(5, appId.getTenant());
                ps.setString(6, appId.getBranch());

                int count = ps.executeUpdate();
                if (count < 1)
                {
                    throw new IllegalStateException("No SNAPSHOT n-cubes found with version " + appId.getVersion() + ", therefore no versions updated, app: " + appId);
                }
                return count;
            }
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to change SNAPSHOT version from " + appId.getVersion() + " to " + newVersion + " for app: " + appId + ", due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public String getTestData(Connection c, ApplicationID appId, String cubeName)
    {
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT test_data_bin FROM n_cube " +
                        "WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?" +
                        "ORDER BY abs(revision_number) DESC"
        ))
        {
            stmt.setString(1, cubeName);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());
            stmt.setString(5, appId.getBranch());

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    byte[] testData = rs.getBytes("test_data_bin");
                    return testData == null ? "" : new String(testData, "UTF-8");
                }
            }
            throw new IllegalArgumentException("Unable to fetch test data, cube: " + cubeName + ", app: " + appId + " does not exist.");
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to fetch test data for cube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }



    public boolean updateTestData(Connection c, ApplicationID appId, String cubeName, String testData)
    {
        Long maxRev = getMaxRevision(c, appId, cubeName);

        if (maxRev == null)
        {
            throw new IllegalArgumentException("Cannot update test data, cube: " + cubeName + " does not exist in app: " + appId);
        }
        if (maxRev < 0)
        {
            throw new IllegalArgumentException("Cannot update test data, cube: " + cubeName + " is deleted in app: " + appId);
        }

        try (PreparedStatement stmt = c.prepareStatement(
                "UPDATE n_cube SET test_data_bin=? " +
                        "WHERE app_cd = ? AND n_cube_nm = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ? AND revision_number = ?"))
        {
            stmt.setBytes(1, testData == null ? null : testData.getBytes("UTF-8"));
            stmt.setString(2, appId.getApp());
            stmt.setString(3, cubeName);
            stmt.setString(4, appId.getVersion());
            stmt.setString(5, ReleaseStatus.SNAPSHOT.name());
            stmt.setString(6, appId.getTenant());
            stmt.setString(7, appId.getBranch());
            stmt.setLong(8, maxRev);
            return stmt.executeUpdate() == 1;
        }
        catch (Exception e)
        {
            String s = "Unable to update test data for NCube: " + cubeName + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean duplicateCube(Connection c, ApplicationID oldAppId, ApplicationID newAppId, String oldName, String newName, String username)
    {
        try
        {
            byte[] oldBytes = null;
            Long oldRevision = null;
            byte[] oldTestData = null;

            try (PreparedStatement stmt = createSelectSingleCubeStatement(c, oldAppId, oldName))
            {
                try (ResultSet rs = stmt.executeQuery())
                {
                    if (rs.next())
                    {
                        oldBytes = rs.getBytes("cube_value_bin");
                        oldRevision = rs.getLong("revision_number");
                        oldTestData = rs.getBytes("test_data_bin");
                    }
                    else
                    {
                        throw new IllegalArgumentException("Unable to duplicate cube because source cube does not exist.  AppId:  " + oldAppId + ", " + oldName);
                    }
                }
            }

            if (oldRevision < 0)
            {
                throw new IllegalArgumentException("Unable to duplicate deleted cube.  AppId:  " + oldAppId + ", " + oldName);
            }

            Long newRevision = null;
            String newHeadSha1 = null;

            try (PreparedStatement ps = createSelectSingleCubeStatement(c, newAppId, newName))
            {
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next())
                    {
                        newRevision = rs.getLong("revision_number");

                        Matcher m = Regexes.headSha1Pattern.matcher(StringUtilities.createString(rs.getBytes("cube_value_bin"), "UTF-8"));
                        if (m.find())
                        {
                            newHeadSha1 = m.group(1);
                        }
                    }
                }
            }

            if (newRevision != null && newRevision >= 0)
            {
                throw new IllegalArgumentException("Unable to duplicate cube, a cube already exists with the new name.  appId:  " + newAppId + ", name: " + newName);
            }

            byte[] newBytes = null;

            // If names are different we need to recalculate the sha-1
            if (!StringUtilities.equalsIgnoreCase(oldName, newName)) {
                String json = StringUtilities.createString(oldBytes, "UTF-8");
                NCube ncube = NCubeManager.ncubeFromJson(json);
                ncube.name = newName;
                ncube.setChanged(true);
                ncube.setHeadSha1(newHeadSha1);
                ncube.setApplicationID(newAppId);
                newBytes = StringUtilities.getBytes(ncube.toFormattedJson(), "UTF-8");
            }
            else if (oldAppId.equalsNotIncludingBranch(newAppId))
            {
                newBytes = oldBytes;
            }
            else
            {
                newBytes = removeHeadSha1(oldBytes);
            }

            String notes = "Cube duplicated from app: " + oldAppId + ", name: " + oldName;

            if (!insertCube(c, newAppId, newName, newRevision == null ? 0 : Math.abs(newRevision) + 1, newBytes, oldTestData, notes, username))
            {
                throw new IllegalStateException("Unable to duplicate cube: " + oldName + " -> " + newName + "', app: " + oldAppId);
            }

            return true;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to duplicate cube: " + oldName + ", app: " + oldAppId + ", new name: " + newName + ", app: " + newAppId + " due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

//    public int cleanUp(Connection c, ApplicationID appId)
//    {
//        DELETE FROM posts WHERE id IN (
//            SELECT * FROM (
//                    SELECT id FROM posts GROUP BY id HAVING ( COUNT(id) > 1 )
//            ) AS p
//        )
//
//        int count = 0;
//
//        String sql = "SELECT *" +
//                " FROM n_cube n, " +
//                "( " +
//                "  SELECT n_cube_nm, max(abs(revision_number)) AS max_rev " +
//                "  FROM n_cube " +
//                "  WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?" +
//                " GROUP BY n_cube_nm " +
//                ") m " +
//                "WHERE m.n_cube_nm = n.n_cube_nm AND (m.max_rev <> abs(n.revision_number) OR n.revision_number <> -1 OR n.revision_number <> 0) AND n.app_cd = ? AND n.version_no_cd = ? AND n.status_cd = ? AND n.tenant_cd = RPAD(?, 10, ' ') AND n.branch_id = ?";
//
//        try (PreparedStatement stmt = c.prepareStatement(sql))
//        {
//            try (ResultSet rs = stmt.executeQuery())
//            {
//                while(rs.next()) {
//                    count++;
//                }
//            }
//            return count;
//        }
//        catch(Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    public boolean renameCube(Connection c, ApplicationID appId, String oldName, String newName, String username)
    {
        try
        {
            byte[] oldBytes = null;
            Long oldRevision = null;
            byte[] oldTestData = null;

            try (PreparedStatement stmt = createSelectSingleCubeStatement(c, appId, oldName))
            {
                try (ResultSet rs = stmt.executeQuery())
                {
                    if (rs.next())
                    {
                        oldBytes = rs.getBytes("cube_value_bin");
                        oldRevision = rs.getLong("revision_number");
                        oldTestData = rs.getBytes("test_data_bin");
                    }
                    else
                    {
                        throw new IllegalArgumentException("Could not rename cube because cube does not exist.  AppId:  " + appId + ", " + oldName);
                    }
                }
            }

            if (oldRevision < 0)
            {
                throw new IllegalArgumentException("Deleted cubes cannot be renamed.  AppId:  " + appId + ", " + oldName + " -> " + newName);
            }

            Long newRevision = null;
            String newHeadSha1 = null;

            try (PreparedStatement ps = createSelectSingleCubeStatement(c, appId, newName))
            {
                try (ResultSet rs = ps.executeQuery())
                {
                    if (rs.next())
                    {
                        newRevision = rs.getLong("revision_number");

                        Matcher m = Regexes.headSha1Pattern.matcher(StringUtilities.createString(rs.getBytes("cube_value_bin"), "UTF-8"));
                        if (m.find())
                        {
                            newHeadSha1 = m.group(1);
                        }
                    }
                }
            }

            if (newRevision != null && newRevision >= 0)
            {
                throw new IllegalArgumentException("Unable to rename cube, a cube already exists with that name.  appId:  " + appId + ", name: " + newName);
            }

            String json = StringUtilities.createString(oldBytes, "UTF-8");
            NCube ncube = NCubeManager.ncubeFromJson(json);
            ncube.name = newName;
            ncube.setChanged(true);
            ncube.setHeadSha1(newHeadSha1);
            ncube.setApplicationID(appId);

            String notes = "Cube renamed:  " + oldName + " -> " + newName;
            byte[] cubeData = StringUtilities.getBytes(ncube.toFormattedJson(), "UTF-8");

            if (!insertCube(c, appId, newName, newRevision == null ? 0 : Math.abs(newRevision) + 1, cubeData, oldTestData, notes, username))
            {
                throw new IllegalStateException("Unable to rename cube: " + oldName + " -> " + newName + "', app: " + appId);
            }

            oldBytes = setChanged(oldBytes);

            if (!insertCube(c, appId, oldName, -(oldRevision + 1), oldBytes, oldTestData, notes, username))
            {
                throw new IllegalStateException("Unable to rename cube: " + oldName + " -> " + newName + ", app: " + appId);
            }

            return true;
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            String s = "Unable to rename cube: " + oldName + ", app: " + appId + ", new name: " + newName + " due to: " + e.getMessage();
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    private byte[] createNote(String user, java.sql.Date date, String notes)
    {
        return StringUtilities.getBytes(date + " [" + user + "] " + notes, "UTF-8");
    }

    private boolean insertCube(Connection c, ApplicationID appId, String name, Long revision, byte[] cubeData, byte[] testData, String notes, String username) throws SQLException
    {
        try (PreparedStatement insert = c.prepareStatement("INSERT INTO n_cube (n_cube_id, app_cd, n_cube_nm, cube_value_bin, version_no_cd, create_dt, create_hid, tenant_cd, branch_id, revision_number, test_data_bin, notes_bin) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"))
        {
            insert.setLong(1, UniqueIdGenerator.getUniqueId());
            insert.setString(2, appId.getApp());
            insert.setString(3, name);
            insert.setBytes(4, cubeData);
            insert.setString(5, appId.getVersion());
            java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
            insert.setDate(6, now);
            insert.setString(7, username);
            insert.setString(8, appId.getTenant());
            insert.setString(9, appId.getBranch());
            insert.setLong(10, revision);
            insert.setBytes(11, testData);
            insert.setBytes(12, createNote(username, now, notes));

            return insert.executeUpdate() == 1;
        }
    }

    public boolean doCubesExist(Connection c, ApplicationID appId)
    {
        String statement = "SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?";

        try (PreparedStatement ps = c.prepareStatement(statement))
        {
            ps.setString(1, appId.getApp());
            ps.setString(2, appId.getVersion());
            ps.setString(3, appId.getTenant());
            ps.setString(4, appId.getBranch());

            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            String s = "Error checking for existing cubes:  " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean doReleaseCubesExist(Connection c, ApplicationID appId)
    {
        String statement = "SELECT n_cube_id FROM n_cube WHERE app_cd = ? AND version_no_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?";

        try (PreparedStatement ps = c.prepareStatement(statement))
        {
            ps.setString(1, appId.getApp());
            ps.setString(2, appId.getVersion());
            ps.setString(3, ReleaseStatus.RELEASE.name());
            ps.setString(4, appId.getTenant());
            ps.setString(5, ApplicationID.HEAD);

            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            String s = "Error checking for release cubes:  " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public boolean doesCubeExist(Connection c, ApplicationID appId, String name)
    {
        String statement = "SELECT n_cube_id FROM n_cube WHERE n_cube_nm = ? AND app_cd = ? AND version_no_cd = ? AND tenant_cd = RPAD(?, 10, ' ') AND branch_id = ?";

        try (PreparedStatement stmt = c.prepareStatement(statement))
        {
            stmt.setString(1, name);
            stmt.setString(2, appId.getApp());
            stmt.setString(3, appId.getVersion());
            stmt.setString(4, appId.getTenant());
            stmt.setString(5, appId.getBranch());

            try (ResultSet rs = stmt.executeQuery())
            {
                return rs.next();
            }
        }
        catch (Exception e)
        {
            String s = "Error checking for cube:  " + name + ", app: " + appId;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    /**
     * Return an array [] of Strings containing all unique App names.
     */
    public Object[] getAppNames(Connection connection, String tenant, String status, String branch)
    {
        String sql = "SELECT DISTINCT app_cd FROM n_cube WHERE tenant_cd = RPAD(?, 10, ' ') and status_cd = ? and branch_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql))
        {
            List<String> records = new ArrayList<>();

            stmt.setString(1, tenant);
            stmt.setString(2, status);
            stmt.setString(3, branch);

            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    records.add(rs.getString(1));
                }
            }
            return records.toArray();
        }
        catch (Exception e)
        {
            String s = "Unable to fetch all app names from database for tenant: " + tenant + ", branch: " + branch;
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Object[] getAppVersions(Connection connection, String tenant, String app, String status, String branch)
    {
        final String sql = "SELECT DISTINCT version_no_cd FROM n_cube WHERE app_cd = ? AND status_cd = ? AND tenant_cd = RPAD(?, 10, ' ') and branch_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql))
        {
            stmt.setString(1, app);
            stmt.setString(2, status);
            stmt.setString(3, tenant);
            stmt.setString(4, branch);

            List<String> records = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    records.add(rs.getString(1));
                }
            }

            return records.toArray();
        }
        catch (Exception e)
        {
            String s = "Unable to fetch all versions for app: " + app + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    public Set<String> getBranches(Connection connection, String tenant)
    {
        final String sql = "SELECT DISTINCT branch_id FROM n_cube WHERE tenant_cd = RPAD(?, 10, ' ')";
        try (PreparedStatement stmt = connection.prepareStatement(sql))
        {
            stmt.setString(1, tenant);

            Set<String> branches = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    branches.add(rs.getString(1));
                }
            }

            return branches;
        }
        catch (Exception e)
        {
            String s = "Unable to fetch all branches for tenant: " + tenant + " from database";
            LOG.error(s, e);
            throw new RuntimeException(s, e);
        }
    }

    private static String convertPattern(String pattern)
    {
        if (StringUtilities.isEmpty(pattern))
        {
            return null;
        }
        else
        {
            pattern = pattern.replace('*', '%');
            pattern = pattern.replace('?', '_');
        }
        return pattern;
    }


    public Map commitBranch(Connection c, ApplicationID appId, Collection<NCubeInfoDto> dtos, String username)
    {
        Map<String, String> changes = new LinkedHashMap<>();
        ApplicationID headAppId = appId.asHead();

        for (NCubeInfoDto dto : dtos)
        {
            Long revision = Long.parseLong(dto.revision);
            copyBetweenBranches(c, appId, headAppId, dto.name, username, revision);
            replaceHeadSha1(c, appId, dto.name, dto.sha1, revision);
            changes.put(dto.name, dto.name);
        }
        return changes;
    }

    public int rollbackBranch(Connection c, ApplicationID appId, Object[] infoDtos)
    {
        int count = 0;
        for (Object dto : infoDtos)
        {
            NCubeInfoDto info = (NCubeInfoDto)dto;
            if (info.headSha1 == null) {
                deleteCube(c, appId, info.name, true, null);
                count++;
            } else if (rollbackCube(c, appId, info.name)) {
                count++;
            }
        }
        return count;
    }

    public Object[] updateBranch(Connection c, ApplicationID appId)
    {

        return new Object[0];
    }
}
