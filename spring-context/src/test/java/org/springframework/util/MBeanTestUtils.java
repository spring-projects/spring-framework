/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	public static synchronized void resetMBeanServers() throws Exception {
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
