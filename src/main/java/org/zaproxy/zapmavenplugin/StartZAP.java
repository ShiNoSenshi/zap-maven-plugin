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
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.zaproxy.clientapi.core.ClientApi;


/**
 * Goal which touches a timestamp file.
 *
 * @goal start-zap
 * @phase pre-integration-test
 */
public class StartZAP
    extends AbstractMojo
{
    /**
     * Location of the ZAProxy program.
     * @parameter
     * @required
     */
    private String zapProgram;
    
    /**
     * Location of the host of the ZAP proxy
     * @parameter default-value="localhost"
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
     * New session when you don't want to start ZAProxy.
     * @parameter default-value="false"
     */
    private boolean newSession;

    /**
     * Sleep to wait to start ZAProxy
     * @parameter default-value="4000"
     */
    private int zapSleep;
    
    public void execute()
        throws MojoExecutionException
    {
    	try {
            if (newSession) {
                ClientApi zapClient = new ClientApi(zapProxyHost, zapProxyPort);
                File tempFile = File.createTempFile("ZAP", null);
                getLog().info("Create Session with temporary file [" + tempFile.getPath() + "]");
                zapClient.newSession(tempFile.getPath());
            } else {
                File pf = new File(zapProgram);
                Runtime runtime = java.lang.Runtime.getRuntime();
                getLog().info("Start ZAProxy [" + zapProgram + "]");
                getLog().info("Using working directory [" + pf.getParentFile().getPath() + "]");
                final Process ps = runtime.exec(zapProgram, null, pf.getParentFile());
                
                // Consommation de la sortie standard de l'application externe dans un Thread separe
                new Thread() {
                    public void run() {
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
                            String line = "";
                            try {
                                while((line = reader.readLine()) != null) {
                                    // Traitement du flux de sortie de l'application si besoin est
                                    getLog().info(line);
                                }
                            } finally {
                                reader.close();
                            }
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();

                // Consommation de la sortie d'erreur de l'application externe dans un Thread separe
                new Thread() {
                    public void run() {
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getErrorStream()));
                            String line = "";
                            try {
                                while((line = reader.readLine()) != null) {
                                    // Traitement du flux d'erreur de l'application si besoin est
                                    getLog().info(line);
                                }
                            } finally {
                                reader.close();
                            }
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();       
                
            }
            Thread.currentThread().sleep(zapSleep);
        } catch(Exception e) {
                e.printStackTrace();
                throw new MojoExecutionException("Unable to start ZAP [" + zapProgram + "]");
        }

    }
}
