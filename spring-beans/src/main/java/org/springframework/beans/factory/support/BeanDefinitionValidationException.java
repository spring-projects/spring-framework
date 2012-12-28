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

package org.springframework.beans.factory.support;

import org.springframework.beans.FatalBeanException;

/**
 * Exception thrown when the validation of a bean definition failed.
 *
 * @author Juergen Hoeller
 * @since 21.11.2003
 * @see AbstractBeanDefinition#validate()
 */
@SuppressWarnings("serial")
public class BeanDefinitionValidationException extends FatalBeanException {

	/**
	 * Create a new BeanDefinitionValidationException with the specified message.
	 * @param msg the detail message
	 */
	public BeanDefinitionValidationException(String msg) {
		super(msg);
	}

	/**
	 * Create a new BeanDefinitionValidationException with the specified message
	 * and root cause.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public BeanDefinitionValidationException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
