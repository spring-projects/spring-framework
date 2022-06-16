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

package org.springframework.beans.factory.generator;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Thrown when a bean definition could not be generated.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
@SuppressWarnings("serial")
public class BeanDefinitionGenerationException extends RuntimeException {

	private final String beanName;

	private final BeanDefinition beanDefinition;

	public BeanDefinitionGenerationException(String beanName, BeanDefinition beanDefinition, String message, Throwable cause) {
		super(message, cause);
		this.beanName = beanName;
		this.beanDefinition = beanDefinition;
	}

	public BeanDefinitionGenerationException(String beanName, BeanDefinition beanDefinition, String message) {
		super(message);
		this.beanName = beanName;
		this.beanDefinition = beanDefinition;
	}

	/**
	 * Return the bean name that could not be generated.
	 * @return the bean name
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Return the bean definition that could not be generated.
	 * @return the bean definition
	 */
	public BeanDefinition getBeanDefinition() {
		return this.beanDefinition;
	}

}
