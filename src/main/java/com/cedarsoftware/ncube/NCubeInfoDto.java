package com.cedarsoftware.ncube;

import java.util.Date;

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
public class NCubeInfoDto
{
	public String id;
	public String tenant;
	public String app;
	public String version;
	public String status;
	public String name;
	public String notes;
	public String revision;
	public Date createDate;
	public String createHid;

	public ApplicationID getApplicationID()
	{
		return new ApplicationID(tenant, app, version, status);
	}

	public String toString()
	{
		return tenant + "/" + app + "/" + version + "/" + status + "/" + name + "/" + revision + ", id=" + id;
	}
}
