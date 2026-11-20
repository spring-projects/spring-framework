/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

/**
 * Utilities for MBean tests.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
public class MBeanTestUtils {

	/**
	 * Reset the {@link MBeanServerFactory} to a known consistent state. This involves
	 * {@linkplain #releaseMBeanServer(MBeanServer) releasing} all currently registered
	 * MBeanServers.
	 */
	public static synchronized void resetMBeanServers() {
		for (MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
			releaseMBeanServer(server);
		}
	}

	/**
	 * Attempt to release the supplied {@link MBeanServer}.
	 * <p>Ignores any {@link IllegalArgumentException} thrown by
	 * {@link MBeanServerFactory#releaseMBeanServer(MBeanServer)} whose error
	 * message contains the text "not in list".
	 */
	public static void releaseMBeanServer(MBeanServer server) {
		try {
			MBeanServerFactory.releaseMBeanServer(server);
		}
		catch (IllegalArgumentException ex) {
			if (!ex.getMessage().contains("not in list")) {
				throw ex;
			}
		}
	}

}
