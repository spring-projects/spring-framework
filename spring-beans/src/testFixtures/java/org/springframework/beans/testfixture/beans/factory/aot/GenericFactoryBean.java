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

package org.springframework.beans.testfixture.beans.factory.aot;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;

/**
 * A public {@link FactoryBean} with a generic type.
 *
 * @author Stephane Nicoll
 */
public class GenericFactoryBean<T> implements FactoryBean<T> {

	private final Class<T> beanType;

	public GenericFactoryBean(Class<T> beanType) {
		this.beanType = beanType;
	}

	@Nullable
	@Override
	public T getObject() throws Exception {
		return BeanUtils.instantiateClass(this.beanType);
	}

	@Nullable
	@Override
	public Class<?> getObjectType() {
		return this.beanType;
	}
}
