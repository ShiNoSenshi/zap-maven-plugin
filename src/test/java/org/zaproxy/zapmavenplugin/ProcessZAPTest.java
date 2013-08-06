package org.zaproxy.zapmavenplugin;

import org.apache.maven.shared.tools.test.ReflectiveSetter;
import org.junit.Before;
import org.junit.Test;
import org.zaproxy.clientapi.core.ClientApi;

/**
 * Test module for the start ZAP module
 *
 * User: david_000
 * Date: 12/07/13
 * Time: 22:43
 * To change this template use File | Settings | File Templates.
 */
public class ProcessZAPTest {

    private static final String ZAP_PROXY_HOST = "ZAP_PROXY_HOST";
    private static final int ZAP_PROXY_PORT = 8080;

    final ClientApi clientApi = new ClientApi(ZAP_PROXY_HOST, ZAP_PROXY_PORT);

    private ProcessZAP processZap;

    @Before
    public void setup() {
        processZap = new ProcessZAP() {
            @Override
            protected ClientApi getZapClient() {
                return clientApi;
            }
        };
    }

    @Test
    public void doNothing() throws Throwable {
        prepareStartZap(processZap);
        processZap.execute();
    }

    @Test
    public void skipExecution() throws Throwable {
        prepareToSkipExecution();
        processZap.execute();
    }

    private void prepareStartZap(ProcessZAP processZap) throws Throwable {
        ReflectiveSetter setter = new ReflectiveSetter(ProcessZAP.class);
        setter.setProperty("zapProxyHost", ZAP_PROXY_HOST, processZap);
        setter.setProperty("zapProxyPort", ZAP_PROXY_PORT, processZap);
        setter.setProperty("spiderURL", false, processZap);
        setter.setProperty("scanURL", false, processZap);
        setter.setProperty("saveSession", false, processZap);
        setter.setProperty("reportAlerts", false, processZap);
        setter.setProperty("skip", false, processZap);
    }

    private void prepareToSkipExecution() throws Throwable {
        ReflectiveSetter setter = new ReflectiveSetter(ProcessZAP.class);
        setter.setProperty("skip", true, processZap);
    }

}
