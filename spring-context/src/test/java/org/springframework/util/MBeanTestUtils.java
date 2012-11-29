/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.util;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

/**
 * Utilities for MBean tests.
 *
 * @author Phillip Webb
 */
public class MBeanTestUtils {

	/**
	 * Resets MBeanServerFactory and ManagementFactory to a known consistent state.
	 * This involves releasing all currently registered MBeanServers and resetting
	 * the platformMBeanServer to null.
	 */
	public static void resetMBeanServers() throws Exception {
		for (MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
			MBeanServerFactory.releaseMBeanServer(server);
		}
		Field field = ManagementFactory.class.getDeclaredField("platformMBeanServer");
		field.setAccessible(true);
		field.set(null, null);
	}

}
