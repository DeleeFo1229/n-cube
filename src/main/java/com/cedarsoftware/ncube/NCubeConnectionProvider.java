package com.cedarsoftware.ncube;

import java.util.Map;

/**
 * Interface for a generic way to provide a database/persistence connection to the NCubeManager.
 * Additional managmen
 *
 * @author Chuck Rowland (pittsflyr@gmail.com)
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

public interface NCubeConnectionProvider
{
    /**
     * Defines keys to be used for the connection context.
     */
    public enum ContextKey
    {
        JDBC_CONNECTION,
        MONGO_CLIENT
    }

    /**
     * Returns an instantiated Map with ContextKey as the keys for each entry in the Map.
     * @return - Map
     */
    Map<ContextKey,Object> getConnectionContext();

    /**
     * Ensures the current database/persistence connection commits all write operations of the current transaction.
     * Ensures the current database/persistence connection is closed if appropriated.
     */
    void commitTransaction();

    /**
     * If the current database/persistence connection is valid, all write operations of the current transaction are rolled back.
     * Ensures the current database/persistence connection is closed if appropriated.
     */
    void rollbackTransaction();
}
