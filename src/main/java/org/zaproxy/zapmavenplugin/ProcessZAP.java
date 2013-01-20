package org.zaproxy.zapmavenplugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.text.SimpleDateFormat;

import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import net.sf.json.xml.XMLSerializer;

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
public class ProcessZAP extends AbstractMojo {
    private static final String NONE_FORMAT = "none";

    private static final String JSON_FORMAT = "json";

    /**
     * Location of the host of the ZAP proxy
     * 
     * @parameter expression="${zapProxyHost}" default-value="localhost"
     * @required
     */
    private String zapProxyHost;

    /**
     * Location of the port of the ZAP proxy
     * 
     * @parameter default-value="8080"
     * @required
     */
    private int zapProxyPort;

    /**
     * Location of the port of the ZAP proxy
     * 
     * @parameter
     * @required
     */
    private String targetURL;

    /**
     * Switch to spider the URL
     * 
     * @parameter default-value="true"
     */
    private boolean spiderURL;

    /**
     * Switch to scan the URL
     * 
     * @parameter default-value="true"
     */
    private boolean scanURL;

    /**
     * Save session of scan
     * 
     * @parameter default-value="true"
     */
    private boolean saveSession;

    /**
     * Switch to shutdown ZAP
     * 
     * @parameter default-value="true"
     */
    private boolean shutdownZAP;

    /**
     * Save session of scan
     * 
     * @parameter expression="${reportAlerts}" default-value="true"
     */
    private boolean reportAlerts;

    /**
     * Location to store the ZAP reports
     * 
     * @parameter default-value="${project.build.directory}/zap-reports"
     */
    private String reportsDirectory;

    /**
     * Set the output format type, in addition to the XML report. Must be one of "none" or "json".
     * 
     * @parameter default-value="none"
     */
    private String format;

    /**
     * create a Timestamp
     * 
     * @return
     */
    private String dateTimeString() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(cal.getTime());
    }

    /**
     * create a temporary filename
     * 
     * @param prefix
     *            if null, then default "temp"
     * @param suffix
     *            if null, then default ".tmp"
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

    public void execute() throws MojoExecutionException {
        ClientApi zapClient = null;
        try {

            zapClient = new ClientApi(zapProxyHost, zapProxyPort);

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

                fileName = createTempFilename("ZAP", "");

                zapClient.saveSession(fileName);
            } else {
                getLog().info("skip saveSession");
            }

            if (reportAlerts) {

                // reuse fileName of the session file
                if ((fileName == null) || (fileName.length() == 0))
                    fileName = createTempFilename("ZAP", "");

                String fileName_no_extension = FilenameUtils.concat(reportsDirectory, fileName);

                try {
                    String alerts = zapClient.getAllAlerts(false);
                    JSON jsonObj = JSONSerializer.toJSON(alerts);

                    writeXml(fileName_no_extension, jsonObj);
                    if (JSON_FORMAT.equals(format)) {
                        writeJson(fileName_no_extension, jsonObj);
                    } else if (NONE_FORMAT.equals(format)) {
                        getLog().info("Only XML report will be generated");
                    } else {
                        getLog().info("This format is not supported ["+format+"] ; please choose 'none' or 'json'");
                    }
                } catch (Exception e) {
                    System.out.print(e.getMessage());
                }
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new MojoExecutionException("Processing with ZAP failed");
        } finally {
            if (shutdownZAP && (zapClient != null)) {
                try {
                    getLog().info("Shutdown ZAProxy");
                    zapClient.stopZap();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                getLog().info("No shutdown of ZAP");
            }
        }
    }

    /**
     * Converts the given JSON string into XML.
     * 
     * @param json
     *            the JSON string to be converted
     * @return XML representing the given JSON string
     */
    private String convert2XML(String json) {
        // convert to XML
        XMLSerializer serializer = new XMLSerializer();
        serializer.setArrayName("zap-report");
        serializer.setElementName("alerts");
        JSON jsonObj = JSONSerializer.toJSON(json);
        return serializer.write(jsonObj);
    }

    private void writeXml(String filename, JSON json) throws IOException {
        String fullFileName = filename + ".xml";
        XMLSerializer serializer = new XMLSerializer();
        serializer.setArrayName("zap-report");
        serializer.setElementName("alerts");
        String xml = serializer.write(json);
        FileUtils.writeStringToFile(new File(fullFileName), xml);
    }

    private void writeJson(String filename, JSON json) throws IOException {
        String fullFileName = filename + ".json";
        FileUtils.writeStringToFile(new File(fullFileName), json.toString());
    }

}
