package com.cedarsoftware.ncube;

/**
 * Class used to carry the NCube meta-information
 * to the client.
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
public interface NCubePersister extends NCubeReadOnlyPersister
{
    void createCube(ApplicationID appId, NCube cube, String username);
    void updateCube(ApplicationID appId, NCube cube, String username);
    boolean deleteCube(ApplicationID appId, String cubeName, boolean allowDelete, String username);
    boolean renameCube(ApplicationID appId, NCube oldCube, String newName);

    void restoreCube(ApplicationID appId, String cubeName, String username);

    boolean updateNotes(ApplicationID appId, String cubeName, String notes);

    int createSnapshotVersion(ApplicationID appId, String newVersion);
    int changeVersionValue(ApplicationID appId, String newVersion);
    int releaseCubes(ApplicationID appId);

    boolean updateTestData(ApplicationID appId, String cubeName, String testData);
}
