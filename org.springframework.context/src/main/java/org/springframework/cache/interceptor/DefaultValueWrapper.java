/*
 * Copyright 2011 the original author or authors.
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

package org.springframework.cache.interceptor;

import org.springframework.cache.Cache.ValueWrapper;

/**
 * Default implementation for {@link org.springframework.cache.Cache.ValueWrapper}.
 * 
 * @author Costin Leau
 */
public class DefaultValueWrapper implements ValueWrapper {

	private final Object value;

	public DefaultValueWrapper(Object value) {
		this.value = value;
	}

	public Object get() {
		return value;
	}
}
