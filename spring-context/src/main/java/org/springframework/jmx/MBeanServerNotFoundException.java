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

package org.springframework.jmx;

/**
 * Exception thrown when we cannot locate an instance of an {@code MBeanServer},
 * or when more than one instance is found.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see org.springframework.jmx.support.JmxUtils#locateMBeanServer
 */
@SuppressWarnings("serial")
public class MBeanServerNotFoundException extends JmxException {

	/**
	 * Create a new {@code MBeanServerNotFoundException} with the
	 * supplied error message.
	 * @param msg the error message
	 */
	public MBeanServerNotFoundException(String msg) {
		super(msg);
	}

	/**
	 * Create a new {@code MBeanServerNotFoundException} with the
	 * specified error message and root cause.
	 * @param msg the error message
	 * @param cause the root cause
	 */
	public MBeanServerNotFoundException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
