/*
 * Copyright 2010-2011 the original author or authors.
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

package org.springframework.cache.support;

import java.lang.reflect.Method;

import org.springframework.cache.KeyGenerator;

/**
 * Default key generator. Returns 0 if no param is given, the param itself if only one is given or a hash code computed
 * from all given params hash code. Uses a constant (53) for null objects.
 * 
 * @author Costin Leau
 */
public class DefaultKeyGenerator implements KeyGenerator<Object> {

	public Object extract(Object target, Method method, Object... params) {
		if (params.length == 1) {
			return (params[0] == null ? 53 : params[0]);
		}

		if (params.length == 0) {
			return 0;
		}

		int hashCode = 17;

		for (Object object : params) {
			hashCode = 31 * hashCode + (object == null ? 53 : object.hashCode());
		}

		return Integer.valueOf(hashCode);
	}
}
