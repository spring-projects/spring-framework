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

package org.springframework.test.bean.override;

/**
 * Strategies for override instantiation, implemented in
 * {@link BeanOverrideBeanPostProcessor}.
 *
 * @author Simon Baslé
 * @since 6.2
 */
public enum BeanOverrideStrategy {

	/**
	 * Replace a given bean's definition, immediately preparing a singleton
	 * instance. Enforces the original bean definition to exist.
	 */
	REPLACE_DEFINITION,
	/**
	 * Replace a given bean's definition, immediately preparing a singleton
	 * instance. If the original bean definition does not exist, create the
	 * override definition instead of failing.
	 */
	REPLACE_OR_CREATE_DEFINITION,
	/**
	 * Intercept and wrap the actual bean instance upon creation, during
	 * {@link org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor#getEarlyBeanReference(Object, String)
	 * early bean definition}.
	 */
	WRAP_EARLY_BEAN;
}
