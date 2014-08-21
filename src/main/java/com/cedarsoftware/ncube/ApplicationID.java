package com.cedarsoftware.ncube;

/**
 * This class binds together Account, App, and version.  These fields together
 * completely identify the application (and version) that a given n-cube belongs
 * to.
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
public class ApplicationID
{
    String account;
    String app;
    String version;

    public ApplicationID(String account, String app, String version)
    {
        this.account = account;
        this.app = app;
        this.version = version;
    }

    public String getAccount()
    {
        return account;
    }

    public void setAccount(String account)
    {
        this.account = account;
    }

    public String getApp()
    {
        return app;
    }

    public void setApp(String app)
    {
        this.app = app;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }
}
