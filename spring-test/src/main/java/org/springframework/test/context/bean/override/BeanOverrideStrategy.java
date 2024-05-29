/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.bean.override;

/**
 * Strategies for bean override processing.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @since 6.2
 */
public enum BeanOverrideStrategy {

	/**
	 * Replace a given bean definition, immediately preparing a singleton instance.
	 * <p>Fails if the original bean definition exists. To create a new bean
	 * definition in such a case, use {@link #REPLACE_OR_CREATE_DEFINITION}.
	 */
	REPLACE_DEFINITION,

	/**
	 * Replace or create a given bean definition, immediately preparing a
	 * singleton instance.
	 * <p>Contrary to {@link #REPLACE_DEFINITION}, this creates a new bean
	 * definition if the target bean definition does not exist rather than
	 * failing.
	 */
	REPLACE_OR_CREATE_DEFINITION,

	/**
	 * Intercept and process an early bean reference rather than a bean
	 * definition, allowing variants of bean overriding to wrap the instance
	 * &mdash; for example, to delegate to actual methods in the context of a
	 * mocking "spy".
	 */
	WRAP_BEAN

}
