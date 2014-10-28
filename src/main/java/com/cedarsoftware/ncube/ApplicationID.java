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
    public static final String DEFAULT_TENANT = "NONE";
    public static final String DEFAULT_APP = "DEFAULT_APP";
    public static final String DEFAULT_VERSION = "999.99.9";
    private final String account;
    private final String app;
    private final String version;
    private final String status;

    public ApplicationID(String account, String app, String version, String status)
    {
        if (account == null)
        {
            throw new IllegalArgumentException("Account (tenant) cannot be null in ApplicationID constructor");
        }
        if (app == null)
        {
            throw new IllegalArgumentException("Application name cannot be null in ApplicationID constructor");
        }
        if (version == null)
        {
            throw new IllegalArgumentException("Version cannot be null in ApplicationID constructor");
        }
        if (status == null)
        {
            throw new IllegalArgumentException("Status cannot be null in ApplicationID constructor");
        }
        this.account = account;
        this.app = app;
        this.version = version;
        this.status = status;
    }

    public String getAccount()
    {
        return account;
    }

    public String getApp()
    {
        return app;
    }

    public String getVersion()
    {
        return version;
    }

    public String getStatus()
    {
        return status;
    }

    public String getAppStr(String name)
    {
        StringBuilder s = new StringBuilder();
        s.append(account == null ? DEFAULT_TENANT : account);
        s.append('/');
        s.append(app == null ? "null" : app);
        s.append('/');
        s.append(version);
        s.append('/');
        s.append(name);
        return s.toString().toLowerCase();
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof ApplicationID))
        {
            return false;
        }

        ApplicationID that = (ApplicationID) o;

        if (account != null ? !account.equals(that.account) : that.account != null)
        {
            return false;
        }
        if (app != null ? !app.equals(that.app) : that.app != null)
        {
            return false;
        }
        if (!status.equals(that.status))
        {
            return false;
        }
        if (!version.equals(that.version))
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result = account != null ? account.hashCode() : 0;
        result = 31 * result + (app != null ? app.hashCode() : 0);
        result = 31 * result + version.hashCode();
        result = 31 * result + status.hashCode();
        return result;
    }

    public String toString()
    {
        return getAppStr("");
    }
}
