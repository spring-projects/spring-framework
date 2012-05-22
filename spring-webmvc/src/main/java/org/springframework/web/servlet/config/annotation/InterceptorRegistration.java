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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;

/**
 * Encapsulates a {@link HandlerInterceptor} and an optional list of URL patterns.
 * Results in the creation of a {@link MappedInterceptor} if URL patterns are provided.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class InterceptorRegistration {

	private final HandlerInterceptor interceptor;

	private final List<String> pathPatterns = new ArrayList<String>();

	/**
	 * Creates an {@link InterceptorRegistration} instance.
	 */
	public InterceptorRegistration(HandlerInterceptor interceptor) {
		Assert.notNull(interceptor, "Interceptor is required");
		this.interceptor = interceptor;
	}

	/**
	 * Add URL patterns to which the registered interceptor should apply to.
	 */
	public InterceptorRegistration addPathPatterns(String... patterns) {
		this.includePatterns.addAll(Arrays.asList(patterns));
		return this;
	}

	/**
	 * Returns the underlying interceptor. If URL patterns are provided the returned type is
	 * {@link MappedInterceptor}; otherwise {@link HandlerInterceptor}.
	 */
	protected Object getInterceptor() {
		if (this.includePatterns.isEmpty()) {
			return this.interceptor;
		}
		return new MappedInterceptor(toArray(this.includePatterns), toArray(this.excludePatterns), interceptor);
	}

	private static String[] toArray(List<String> list) {
		if (CollectionUtils.isEmpty(list)) {
			return null;
		}
		else {
			return list.toArray(new String[list.size()]);
		}
	}

}
