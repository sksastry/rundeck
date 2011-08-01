/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * WindowsNodeAttributes
 */

package com.dtolabs.rundeck.plugin.windows;

import com.dtolabs.rundeck.core.common.INodeEntry;

public class WindowsNodeAttributes {

	private final String hostName;
	private final String domainName;
	private final String userName;
	private final String keyFilePath;
	private final String password;

	public WindowsNodeAttributes(final INodeEntry node) throws Exception {
		String error;
		String password;
    	if (null == node.getAttributes()) {
            error = "failed to parse node attributes";
        } else if (null == (hostName = node.getHostname())) {
            error = "failed to parse node attribute 'hostname'";
        } else if (null == (domainName = node.getAttributes().get("domain"))) {
            error = "failed to parse node attribute 'domain'";
        } else if (null == (userName = node.getUsername())) {
            error = "failed to parse node attribute 'username'";
        } else if (null == (keyFilePath = node.getAttributes().get("key-file-path"))) {
            error = "failed to parse node attribute 'key-file-path'";
        } else if (null == (password = node.getAttributes().get("password"))) {
            error = "failed to parse node attribute 'password'";
        } else {
    		this.password = CryptUtil.decryptString(userName, password, keyFilePath);
    		return;
        }
    	throw new Exception(error);
	}
	
	public String getHostName() {
		return hostName;
	}
	
	public String getDomainName() {
		return domainName;
	}
	
	public String getUserName() {
		return userName;
	}
	
	public String getPassword() {
		return password;
	}

}