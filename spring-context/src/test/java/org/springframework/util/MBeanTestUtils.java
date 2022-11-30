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

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.EnumSet;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.junit.jupiter.api.condition.JRE;

/**
 * Utilities for MBean tests.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
public class MBeanTestUtils {

	/**
	 * Resets {@link MBeanServerFactory} to a known consistent state. This involves
	 * releasing all currently registered MBeanServers.
	 * <p>On JDK 8 - JDK 16, this method also resets the platformMBeanServer field
	 * in {@link ManagementFactory} to {@code null}.
	 */
	public static synchronized void resetMBeanServers() throws Exception {
		for (MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
			try {
				MBeanServerFactory.releaseMBeanServer(server);
			}
			catch (IllegalArgumentException ex) {
				if (!ex.getMessage().contains("not in list")) {
					throw ex;
				}
			}
		}

		if (!isCurrentJreWithinRange(JRE.JAVA_16, JRE.OTHER)) {
			Field field = ManagementFactory.class.getDeclaredField("platformMBeanServer");
			field.setAccessible(true);
			field.set(null, null);
		}
	}

	static boolean isCurrentJreWithinRange(JRE min, JRE max) {
		return EnumSet.range(min, max).contains(JRE.currentVersion());
	}

}
