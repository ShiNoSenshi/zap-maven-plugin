package org.zaproxy.zapmavenplugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.text.SimpleDateFormat;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.Alert;

/**
 * Goal which touches a timestamp file.
 *
 * @goal process-zap
 * @phase post-integration-test
 */
public class ProcessZAP
    extends AbstractMojo
{
    /**
     * Location of the host of the ZAP proxy
     * @parameter expression="${zapProxyHost}" default-value="localhost"
     * @required
     */
    private String zapProxyHost;

    /**
     * Location of the port of the ZAP proxy
     * @parameter default-value="8080"
     * @required
     */
    private int zapProxyPort;

    /**
     * Location of the port of the ZAP proxy
     * @parameter
     * @required
     */
    private String targetURL;
    
    /**
     * Switch to spider the URL
     * @parameter default-value="true"
     */
    private boolean spiderURL;

    /**
     * Switch to scan the URL
     * @parameter default-value="true"
     */
    private boolean scanURL;

    /**
     * Save session of scan
     * @parameter default-value="true"
     */
    private boolean saveSession;

    /**
     * Switch to shutdown ZAP
     * @parameter default-value="true"
     */
    private boolean shutdownZAP;
    
    /**
     * Save session of scan
     * @parameter expression="${reportAlerts}" default-value="true"
     */
    private boolean reportAlerts;
    
    /**
     * Location to store the ZAP reports
     * @parameter default-value="${project.build.directory}/zap-reports"
     */
    private String reportsDirectory;
        
    /**
     * create a Timestamp
     * @return
     */
    private String dateTimeString() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(cal.getTime());
    }
    
    /**
     * create a temporary filename
     * @param prefix if null, then default "temp"
     * @param suffix if null, then default ".tmp"
     * @return
     */
    private String createTempFilename(String prefix, String suffix) {
    	StringBuilder sb = new StringBuilder("");
    	if (prefix != null)
    		sb.append(prefix);
    	else 
    		sb.append("temp");
    	
    	// append date time and random UUID 
    	sb.append(dateTimeString()).append("_").append(UUID.randomUUID().toString());
    	
    	if (suffix != null)
    		sb.append(suffix);
    	else
    		sb.append(".tmp");
    	
    	return sb.toString();
    }
    
    public void execute()
        throws MojoExecutionException
    {
    	try {
    		
    		ClientApi zapClient = new ClientApi(zapProxyHost, zapProxyPort);
            
            if (spiderURL) {
                getLog().info("Spider the site [" + targetURL + "]");
                zapClient.spiderUrl(targetURL);
            } else {
                getLog().info("skip spidering the site [" + targetURL + "]");
            }
            
            if (scanURL) {
                getLog().info("Scan the site [" + targetURL + "]");
                zapClient.activeScanUrl(targetURL);
            } else {
                getLog().info("skip scanning the site [" + targetURL + "]");
            }
            
            // filename to share between the session file and the report file
            String fileName = "";
            if (saveSession) {
            	
            	fileName = createTempFilename("ZAP","");
            	
                zapClient.saveSession(fileName);            
            } else {
                getLog().info("skip saveSession");
            }
            
            if (reportAlerts) {
            	
            	// reuse fileName of the session file
            	if ((fileName == null) || (fileName.length() == 0))
            		fileName = createTempFilename("ZAP","");

        		String fullFileName = fileName + ".xml";
            	fullFileName = FilenameUtils.concat(reportsDirectory, fullFileName);
            	
            	try {
            		String alerts = zapClient.getAllAlerts(false);
            		FileUtils.writeStringToFile(new File(fullFileName), alerts);
            	} catch (Exception e) {
            		System.out.print( e.getMessage() );
            	}
            }
            
            if (shutdownZAP) {
                getLog().info("Shutdown ZAProxy");
                zapClient.stopZap();
            } else {
                getLog().info("No shutdown of ZAP");
            }
            
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new MojoExecutionException("Processing with ZAP failed");
		}
    }
}
