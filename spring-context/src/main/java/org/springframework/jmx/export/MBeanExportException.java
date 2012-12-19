/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.jmx.export;

import org.springframework.jmx.JmxException;

/**
 * Exception thrown in case of failure when exporting an MBean.
 *
 * @author Rob Harrop
 * @since 2.0
 * @see MBeanExportOperations
 */
@SuppressWarnings("serial")
public class MBeanExportException extends JmxException {

	/**
	 * Create a new {@code MBeanExportException} with the
	 * specified error message.
	 * @param msg the detail message
	 */
	public MBeanExportException(String msg) {
		super(msg);
	}

	/**
	 * Create a new {@code MBeanExportException} with the
	 * specified error message and root cause.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public MBeanExportException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
