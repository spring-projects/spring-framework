/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jmx.export;

import javax.management.ObjectName;

/**
 * A listener that allows application code to be notified when an MBean is
 * registered and unregistered via an {@link MBeanExporter}.
 *
 * @author Rob Harrop
 * @since 1.2.2
 * @see org.springframework.jmx.export.MBeanExporter#setListeners
 */
public interface MBeanExporterListener {

	/**
	 * Called by {@link MBeanExporter} after an MBean has been <i>successfully</i>
	 * registered with an {@link javax.management.MBeanServer}.
	 * @param objectName the {@code ObjectName} of the registered MBean
	 */
	void mbeanRegistered(ObjectName objectName);

	/**
	 * Called by {@link MBeanExporter} after an MBean has been <i>successfully</i>
	 * unregistered from an {@link javax.management.MBeanServer}.
	 * @param objectName the {@code ObjectName} of the unregistered MBean
	 */
	void mbeanUnregistered(ObjectName objectName);

}
