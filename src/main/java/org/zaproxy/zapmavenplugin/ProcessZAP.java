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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import net.sf.json.xml.XMLSerializer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.zaproxy.clientapi.core.ApiResponse;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ClientApiException;

/**
 * Goal which touches a timestamp file.
 *
 * @goal process-zap
 * @phase post-integration-test
 */
public class ProcessZAP extends AbstractMojo {
    private static final String NONE_FORMAT = "none";

    private static final String JSON_FORMAT = "json";

    private ClientApi zapClientAPI;
    private Proxy proxy;

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
     * Set the plugin to skip its execution.
     *
     * @parameter default-value="false"
     */
    private boolean skip;

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

    /**
     * Change the ZAP API status response to an integer
     *
     * @param response the ZAP APIresponse code
     * @return
     */
    private int statusToInt(ApiResponse response) {
        return Integer.parseInt(((ApiResponseElement)response).getValue());
    }

    /**
     * Search for all links and pages on the URL
     *
     * @param url the to investigate URL
     * @throws ClientApiException
     */
    private void spiderURL(String url) throws ClientApiException {
        zapClientAPI.spider.scan(url);

        while ( statusToInt(zapClientAPI.spider.status()) < 100) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                getLog().error(e.toString());
            }
        }
    }

    /**
     * Scan all pages found at url
     *
     * @param url the url to scan
     * @throws ClientApiException
     */
    private void scanURL(String url) throws ClientApiException {
        zapClientAPI.ascan.scan(url, "true", "false");

        while ( statusToInt(zapClientAPI.ascan.status()) < 100) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                getLog().error(e.toString());
            }
        }
    }

    /**
     * Get all alerts from ZAP proxy
     *
     * @param json true for json form, false for xml format
     * @return  all alerts from ZAProxy
     * @throws Exception
     */
    private String getAllAlerts(boolean json) throws Exception {
        URL url;
        String result = "";

        if (json) {
            url = new URL("http://zap/json/core/view/alerts");
        } else {
            url = new URL("http://zap/xml/core/view/alerts");
        }

        getLog().info("Open URL: " + url.toString());

        HttpURLConnection uc = (HttpURLConnection) url.openConnection(proxy);
        uc.connect();

        BufferedReader in = new BufferedReader(new InputStreamReader(
                uc.getInputStream()));
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            result = result + inputLine;
        }

        in.close();
        return result;

    }

    /**
     * Copies the html report from zap into the report directory
     * @param filename the filename without extention where the report should be placed
     * @throws Exception
     */
    private void writeHtmlReport(String filename) throws Exception {
        String fullFileName = filename + ".html";
        URL url = new URL("http://zap/html/core/view/alerts");

        getLog().info("Open URL: " + url.toString());

        HttpURLConnection uc = (HttpURLConnection) url.openConnection(proxy);
        uc.connect();

        BufferedReader in = new BufferedReader(new InputStreamReader(
                uc.getInputStream()));
        FileWriter fstream = new FileWriter(fullFileName);

        String line;
        String content = "";

        while ((line = in.readLine()) != null) {
            content += line;
            getLog().debug(line);
        }
        fstream.write(content);

        fstream.close();
        in.close();
    }

    /**
     * execute the whole shabang
     *
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping zap exection");
            return;
        }
        try {

            zapClientAPI = getZapClient();
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(zapProxyHost, zapProxyPort));

            if (spiderURL) {
                getLog().info("Spider the site [" + targetURL + "]");
                spiderURL(targetURL);
            } else {
                getLog().info("skip spidering the site [" + targetURL + "]");
            }

            if (scanURL) {
                getLog().info("Scan the site [" + targetURL + "]");
                scanURL(targetURL);
            } else {
                getLog().info("skip scanning the site [" + targetURL + "]");
            }

            // filename to share between the session file and the report file
            String fileName = "";
            if (saveSession) {

                fileName = createTempFilename("ZAP", "");

                zapClientAPI.core.saveSession(fileName);
            } else {
                getLog().info("skip saveSession");
            }

            if (reportAlerts) {

                // reuse fileName of the session file
                if ((fileName == null) || (fileName.length() == 0))
                    fileName = createTempFilename("ZAP", "");

                String fileName_no_extension = FilenameUtils.concat(reportsDirectory, fileName);

                try {
                    String alerts = getAllAlerts(true);
                    JSON jsonObj = JSONSerializer.toJSON(alerts);

                    writeXml(fileName_no_extension, jsonObj);
                    writeHtmlReport(fileName_no_extension);
                    if (JSON_FORMAT.equals(format)) {
                        writeJson(fileName_no_extension, jsonObj);
                    } else if (NONE_FORMAT.equals(format)) {
                        getLog().info("Only XML report will be generated");
                    } else {
                        getLog().info("This format is not supported ["+format+"] ; please choose 'none' or 'json'");
                    }
                } catch (Exception e) {
                    getLog().error(e.toString());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            getLog().error(e.toString());
            throw new MojoExecutionException("Processing with ZAP failed", e);
        } finally {
            if (shutdownZAP && (zapClientAPI != null)) {
                try {
                    getLog().info("Shutdown ZAProxy");
                    zapClientAPI.core.shutdown();
                } catch (Exception e) {
                    getLog().error(e.toString());
                    e.printStackTrace();
                }
            } else {
                getLog().info("No shutdown of ZAP");
            }
        }
    }

    protected ClientApi getZapClient() {
        return new ClientApi(zapProxyHost, zapProxyPort);
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
