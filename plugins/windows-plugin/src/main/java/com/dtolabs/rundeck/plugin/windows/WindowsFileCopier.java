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
* WindowsFileCopier
*/

package com.dtolabs.rundeck.plugin.windows;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.impl.common.BaseFileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.plugins.Plugin;

import java.net.UnknownHostException;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ByteArrayInputStream;

import java.util.logging.Level;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;

/**
 * WindowsFileCopier provider for the FileCopier service onto Windows
 */
@Plugin (name = "windows-file-copier", service = "FileCopier")
public class WindowsFileCopier implements FileCopier {

	public static final String WINDOWS_FILE_COPIER = "[windows file copier] ";

	public String copyFileStream(final ExecutionContext context, final InputStream input, final INodeEntry node)
      throws FileCopierException {
    	final String resultFilePath = localGenerateRemoteFilepathForNode(node, "windows");

        context.getExecutionListener().log(Constants.WARN_LEVEL,
          WINDOWS_FILE_COPIER + " copy inputstream to node "
          + node.getNodename() + ": " + resultFilePath);

        copy(context, input, node, resultFilePath);

        return resultFilePath;
    }

    public String copyFile(final ExecutionContext context, final File file, final INodeEntry node)
      throws FileCopierException {
        // hack to remove the ".bat" on Windows.
    	String targetFilePath, resultFilePath;
        try {
	        if (!file.getName().endsWith(".bat")) {
	        	// Wrap non-bat files in a .bat file which contains the command
	        	// "cmd /c non-bat-file.ext" in order to execute it.
	        	
	        	// Copy over a file which contains a script that invokes the
	        	// original non-bat file using the command "cmd /c non-bat-file.ext".
	        	targetFilePath = localGenerateRemoteFilepathForNode(node, file.getName());
	            if (targetFilePath.endsWith(".bat")) {
	            	targetFilePath = targetFilePath.substring(0, targetFilePath.length() - 4);
	            }
	            resultFilePath
	              = copyScriptContent(context, "cmd /c " + targetFilePath, node);
	        } else {
	        	targetFilePath = resultFilePath
	        	  = localGenerateRemoteFilepathForNode(node, file.getName());
	        }
	
	        context.getExecutionListener().log(Constants.WARN_LEVEL,
	          WINDOWS_FILE_COPIER + " copy local file to node "
	          + node.getNodename() + ": " + targetFilePath);
	
	    	copy(context, new FileInputStream(file), node, targetFilePath);
        } catch (FileNotFoundException fnfe) {
        	throw new FileCopierException(fnfe);
        }

        return resultFilePath;
    }

    public String copyScriptContent(final ExecutionContext context, final String script, final INodeEntry node)
      throws FileCopierException {
    	final String resultFilePath = localGenerateRemoteFilepathForNode(node, "windows");

        final int linecount = script != null ? script.split("(\\r?\\n)").length : 0;
        context.getExecutionListener().log(Constants.WARN_LEVEL,
          WINDOWS_FILE_COPIER + " copy [" + linecount + " lines] to node "
          + node.getNodename() + ": " + resultFilePath);

        try {
        	String charset = "UTF-8";
        	copy(context, new ByteArrayInputStream( script.getBytes( charset ) ), node, resultFilePath);
        } catch (java.io.UnsupportedEncodingException uee) {
        	throw new FileCopierException(uee);
        }

        return resultFilePath;
    }
    
    private String localGenerateRemoteFilepathForNode(INodeEntry node, String name) {
    	return BaseFileCopier.generateRemoteFilepathForNode(node, name); //.replaceAll("/", "\\");
    }
    
    private void copy(final ExecutionContext context,
    				  InputStream input,
    				  final INodeEntry node,
    				  String remoteFilePath)
      throws FileCopierException {
    	DcomService dcomService = null;
		try {
			dcomService
			  = new DcomService(context, new WindowsNodeAttributes(node));

			//TODO: Determine timeout correctly!
			dcomService.copy(input, remoteFilePath, 100);
		} catch (Exception e) {
	    	throw new FileCopierException(e);
		} finally {
			try {
				if (dcomService != null) {
					dcomService.destroySession();
				}
			} catch (Exception e) {
		        context.getExecutionListener().log(
		          Constants.WARN_LEVEL,
		          String.format("%s (%s:'%s')", WINDOWS_FILE_COPIER,
	          					"error destroying remote DCOM session",
	          					e.getMessage()));
			}
		}
    }

    private class DcomService {
    	private final ExecutionContext context;
		private JIComServer comServer = null;
		private IJIDispatch dispatch = null;
		private IJIComObject unknown = null;
		private JISession session = null;
	
		public DcomService(final ExecutionContext context,
						   WindowsNodeAttributes windowsNodeAttributes)
		  throws JIException, UnknownHostException {
			// to debug DCOM change this level
			JISystem.getLogger().setLevel(Level.OFF);
			
			this.context = context;
	
			JISystem.setAutoRegisteration(true);
			session = JISession.createSession(windowsNodeAttributes.getDomainName(),
											  windowsNodeAttributes.getUserName(),
											  windowsNodeAttributes.getPassword());
			comServer = new JIComServer(JIProgId.valueOf("Scripting.FileSystemObject"), 
										windowsNodeAttributes.getHostName(),
										session);
			unknown = comServer.createInstance();
			dispatch = (IJIDispatch)JIObjectFactory.narrowObject(
						 unknown.queryInterface(IJIDispatch.IID));
		}

		public void destroySession() throws Exception {
			JISession.destroySession(session);
		}

		public void copy(InputStream input, String remoteFilePath, long timeOuMs)
		  throws Exception {
			StreamThread streamThread
			  = new StreamThread(input, remoteFilePath, timeOuMs);
			streamThread.start();
			streamThread.join(timeOuMs);
			
			if (streamThread.getThreadException() != null) {
				throw streamThread.getThreadException();
			}
		}
	
		private class StreamThread extends Thread {
			private final InputStream input;
			private final String remoteFilePath;
			private final long timeOuMs;
			private Exception threadException = null;

			public StreamThread(InputStream input, String remoteFilePath, long timeOuMs) {
				this.input = input;
				this.remoteFilePath = remoteFilePath;
				this.timeOuMs = timeOuMs;
			}
			
			public Exception getThreadException() {
				return threadException;
			}
	
			public void run() {
				IJIDispatch textFileObject = null;
				try {
					JIVariant[] jvArr
					  = dispatch.callMethodA("CreateTextFile",
							  				 new Object[] { new JIString(remoteFilePath),
                              								new JIVariant(true) } );
					textFileObject
					  = (IJIDispatch)JIObjectFactory.narrowObject(
						  jvArr[0].getObjectAsComObject());

					int nBytesRead;
					byte buffer[] = new byte[1024];			
					while ((nBytesRead = input.read(buffer)) != -1) {
						String data = new String(buffer, 0, nBytesRead, "UTF-8"); 
						JIVariant[] jvArray
						  = textFileObject.callMethodA(
							  "Write", new Object[] { new JIString(data) });
					}
				} catch (Exception e) {
					threadException = e;
				} finally {
					try {
						if (textFileObject != null) {
							textFileObject.callMethodA("Close");
						}
					} catch (Exception e) {				    	
				        context.getExecutionListener().log(
				          Constants.WARN_LEVEL,
				          String.format("%s (%s:'%s')", WINDOWS_FILE_COPIER,
				          				"error closing remote file stream",
				          				e.getMessage()));
					}
				}
			}
		}
    }

}
