package org.zaproxy.zapmavenplugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.maven.shared.tools.test.ReflectiveSetter;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ClientApiException;
import org.zaproxy.clientapi.gen.Core;

/**
 * Test module for the start ZAP module
 *
 * User: david_000
 * Date: 12/07/13
 * Time: 22:43
 * To change this template use File | Settings | File Templates.
 */
public class StartZAPTest {

    private static final boolean NEW_SESSION = true;
    private static final boolean NO_NEW_SESSION = false;
    private static final String ZAP_PROXY_HOST = "ZAP_PROXY_HOST";
    private static final int ZAP_PROXY_PORT = 8080;
    private static final int ZAP_SLEEP = 0;

    final ClientApi clientApi = new ClientApi(ZAP_PROXY_HOST, ZAP_PROXY_PORT);
    Runtime runtimeMock = EasyMock.createMock(Runtime.class);

    private StartZAP startZap;

    @Before
    public void setup() {
        startZap = new StartZAP() {
            @Override
            protected ClientApi getZapClient() {
                return clientApi;
            }

            @Override
            protected Runtime getRuntime() {
                return runtimeMock;
            };
        };
    }

    @Test
    public void startNewSession() throws Throwable {
        Core coreMock = prepareCoreForNewSession();
        prepareStartZap(startZap, NEW_SESSION);


        EasyMock.replay(coreMock);
        startZap.execute();
        EasyMock.verify(coreMock);
    }

    @Test
    public void startServer() throws Throwable {
        prepareStartZap(startZap, NO_NEW_SESSION);
        EasyMock.expect(runtimeMock.exec(EasyMock.eq(zapApiPath()), EasyMock.anyObject(String[].class), EasyMock.anyObject(File.class))).andReturn(null);

        EasyMock.replay(runtimeMock);
        startZap.execute();
        EasyMock.verify(runtimeMock);
    }

    @Test
    public void skipExecution() throws Throwable {
        prepareToSkipExecution();
        startZap.execute();
    }

    private String zapApiPath() throws IOException, FileNotFoundException {
        InputStream is = getClass().getResourceAsStream("/config.properties");
        Properties p = new Properties();
        p.load(new InputStreamReader(is));
        return p.getProperty("zapApiPath");
    }

    private Core prepareCoreForNewSession() throws ClientApiException {
        Core coreMock = EasyMock.createMock(Core.class);
        EasyMock.expect(coreMock.newSession(EasyMock.anyString())).andReturn(ApiResponseElement.OK);
        clientApi.core = coreMock;
        return coreMock;
    }

    private void prepareStartZap(StartZAP startZap, boolean newSession) throws Throwable {
        ReflectiveSetter setter = new ReflectiveSetter(StartZAP.class);
        setter.setProperty("newSession", newSession, startZap);
        setter.setProperty("zapProxyHost", ZAP_PROXY_HOST, startZap);
        setter.setProperty("zapProxyPort", ZAP_PROXY_PORT, startZap);
        setter.setProperty("zapSleep", ZAP_SLEEP, startZap);
        setter.setProperty("zapProgram", zapApiPath(), startZap);
        setter.setProperty("skip", false, startZap);
    }

    private void prepareToSkipExecution() throws Throwable {
        ReflectiveSetter setter = new ReflectiveSetter(StartZAP.class);
        setter.setProperty("skip", true, startZap);
    }
}
