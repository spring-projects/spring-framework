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

package org.springframework.aop.framework;

import org.springframework.core.NestedRuntimeException;

/**
 * Exception that gets thrown on illegal AOP configuration arguments.
 *
 * @author Rod Johnson
 * @since 13.03.2003
 */
@SuppressWarnings("serial")
public class AopConfigException extends NestedRuntimeException {

	/**
	 * Constructor for AopConfigException.
	 * @param msg the detail message
	 */
	public AopConfigException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for AopConfigException.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public AopConfigException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
