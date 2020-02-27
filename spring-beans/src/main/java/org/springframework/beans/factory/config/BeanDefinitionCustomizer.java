/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.factory.config;

/**
 * Callback for customizing a given bean definition.
 * Designed for use with a lambda expression or method reference.
 *
 * @author Juergen Hoeller
 * @since 5.0
 * @see org.springframework.beans.factory.support.BeanDefinitionBuilder#applyCustomizers
 */
@FunctionalInterface
public interface BeanDefinitionCustomizer {

	/**
	 * Customize the given bean definition.
	 */
	void customize(BeanDefinition bd);

}
