package com.cedarsoftware.ncube;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This adapter could be replaced by an adapting proxy.  Then you could
 * implement the interface and the class and not need this class.
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
public class NCubeJdbcPersisterAdapter implements NCubePersister
{
    private final NCubeJdbcPersister persister = new NCubeJdbcPersister();
    private final JdbcConnectionProvider connectionProvider;

    public NCubeJdbcPersisterAdapter(JdbcConnectionProvider provider)
    {
        connectionProvider = provider;
    }

    public void updateCube(ApplicationID appId, NCube cube, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            persister.updateCube(c, appId, cube, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public NCube loadCubeById(long cubeId)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.loadCubeById(c, cubeId);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public NCube loadCube(ApplicationID appId, String name)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.loadCube(c, appId, name);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public NCube loadCubeBySha1(ApplicationID appId, String name, String sha1)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.loadCubeBySha1(c, appId, name, sha1);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public void restoreCubes(ApplicationID appId, Object[] names, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            persister.restoreCubes(c, appId, names, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public List<NCubeInfoDto> getRevisions(ApplicationID appId, String cubeName)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getRevisions(c, appId, cubeName);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public Set<String> getBranches(String tenant)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getBranches(c, tenant);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean deleteBranch(ApplicationID branchId)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.deleteBranch(c, branchId);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean deleteCubes(ApplicationID appId, Object[] cubeNames, boolean allowDelete, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.deleteCubes(c, appId, cubeNames, allowDelete, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public List<String> getAppNames(String tenant, String status, String branch)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getAppNames(c, tenant, status, branch);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public List<String> getAppVersions(String tenant, String app, String status, String branch)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getAppVersions(c, tenant, app, status, branch);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }


    public int changeVersionValue(ApplicationID appId, String newVersion)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.changeVersionValue(c, appId, newVersion);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public int releaseCubes(ApplicationID appId, String newSnapVer)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.releaseCubes(c, appId, newSnapVer);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public int createBranch(ApplicationID appId)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.createBranch(c, appId);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean renameCube(ApplicationID appId, String oldName, String newName, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.renameCube(c, appId, oldName, newName, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean mergeAcceptTheirs(ApplicationID appId, String cubeName, String branchSha1, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.mergeAcceptTheirs(c, appId, cubeName, branchSha1, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean mergeAcceptMine(ApplicationID appId, String cubeName, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.mergeAcceptMine(c, appId, cubeName, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean duplicateCube(ApplicationID oldAppId, ApplicationID newAppId, String oldName, String newName, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.duplicateCube(c, oldAppId, newAppId, oldName, newName, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean updateNotes(ApplicationID appId, String cubeName, String notes)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.updateNotes(c, appId, cubeName, notes);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean updateTestData(ApplicationID appId, String cubeName, String testData)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.updateTestData(c, appId, cubeName, testData);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public String getTestData(ApplicationID appId, String cubeName)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.getTestData(c, appId, cubeName);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public NCubeInfoDto commitMergedCubeToHead(ApplicationID appId, NCube cube, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.commitMergedCubeToHead(c, appId, cube, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public NCubeInfoDto commitMergedCubeToBranch(ApplicationID appId, NCube cube, String headSha1, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.commitMergedCubeToBranch(c, appId, cube, headSha1, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public NCubeInfoDto commitCube(ApplicationID appId, Long cubeId, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.commitCube(c, appId, cubeId, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public int rollbackCubes(ApplicationID appId, Object[] names, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.rollbackCubes(c, appId, names, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public NCubeInfoDto updateCube(ApplicationID appId, Long cubeId, String username)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.updateCube(c, appId, cubeId, username);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public boolean updateBranchCubeHeadSha1(Long cubeId, String headSha1)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.updateBranchCubeHeadSha1(c, cubeId, headSha1);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }

    public List<NCubeInfoDto> search(ApplicationID appId, String cubeNamePattern, String searchValue, Map options)
    {
        Connection c = connectionProvider.getConnection();
        try
        {
            return persister.search(c, appId, cubeNamePattern, searchValue, options);
        }
        finally
        {
            connectionProvider.releaseConnection(c);
        }
    }
}
