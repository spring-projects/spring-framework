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

package org.springframework.jmx.export.naming;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Interface that allows infrastructure components to provide their own
 * {@code ObjectName}s to the {@code MBeanExporter}.
 *
 * <p><b>Note:</b> This interface is mainly intended for internal usage.
 *
 * @author Rob Harrop
 * @since 1.2.2
 * @see org.springframework.jmx.export.MBeanExporter
 */
public interface SelfNaming {

	/**
	 * Return the {@code ObjectName} for the implementing object.
	 * @throws MalformedObjectNameException if thrown by the ObjectName constructor
	 * @see javax.management.ObjectName#ObjectName(String)
	 * @see javax.management.ObjectName#getInstance(String)
	 * @see org.springframework.jmx.support.ObjectNameManager#getInstance(String)
	 */
	ObjectName getObjectName() throws MalformedObjectNameException;

}
