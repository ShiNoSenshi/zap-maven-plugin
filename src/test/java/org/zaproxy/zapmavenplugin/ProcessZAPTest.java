package org.zaproxy.zapmavenplugin;

import org.apache.maven.shared.tools.test.ReflectiveSetter;
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

	private ProcessZAP processZap = new ProcessZAP() {
		@Override
		protected ClientApi getZapClient() {
			return clientApi;
		}
	};
	
	
	@Test
	public void doNothing() throws Throwable {
		prepareStartZap(processZap);
		processZap.execute();
	}

	private void prepareStartZap(ProcessZAP startZap) throws Throwable {
		ReflectiveSetter setter = new ReflectiveSetter(ProcessZAP.class);
		setter.setProperty("zapProxyHost", ZAP_PROXY_HOST, startZap);
		setter.setProperty("zapProxyPort", ZAP_PROXY_PORT, startZap);
		setter.setProperty("spiderURL", false, startZap);
		setter.setProperty("scanURL", false, startZap);
		setter.setProperty("saveSession", false, startZap);
		setter.setProperty("reportAlerts", false, startZap);
	}
	
	
}
