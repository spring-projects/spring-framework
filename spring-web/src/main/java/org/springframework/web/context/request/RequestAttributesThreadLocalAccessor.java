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

package org.springframework.web.context.request;

import io.micrometer.context.ThreadLocalAccessor;

/**
 * Adapt {@link RequestContextHolder} to the {@link ThreadLocalAccessor} contract
 * to assist the Micrometer Context Propagation library with
 * {@link RequestAttributes} propagation.
 *
 * @author Tadaya Tsuyukubo
 * @since 6.2
 */
public class RequestAttributesThreadLocalAccessor implements ThreadLocalAccessor<RequestAttributes> {

	/**
	 * Key under which this accessor is registered in
	 * {@link io.micrometer.context.ContextRegistry}.
	 */
	public static final String KEY = RequestAttributesThreadLocalAccessor.class.getName() + ".KEY";

	@Override
	public Object key() {
		return KEY;
	}

	@Override
	public RequestAttributes getValue() {
		return RequestContextHolder.getRequestAttributes();
	}

	@Override
	public void setValue(RequestAttributes value) {
		RequestContextHolder.setRequestAttributes(value);
	}

	@Override
	public void setValue() {
		RequestContextHolder.resetRequestAttributes();
	}

}
