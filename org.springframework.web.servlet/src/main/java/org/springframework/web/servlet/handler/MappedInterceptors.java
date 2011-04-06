/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

public final class MappedInterceptors {

	private MappedInterceptor[] mappedInterceptors;

	public MappedInterceptors(MappedInterceptor[] mappedInterceptors) {
		this.mappedInterceptors = mappedInterceptors;
	}

	public static MappedInterceptors createFromDeclaredBeans(ListableBeanFactory beanFactory) {
		Map<String, MappedInterceptor> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory,
				MappedInterceptor.class, true, false);

		if (!beans.isEmpty()) {
			return new MappedInterceptors(beans.values().toArray(new MappedInterceptor[beans.size()]));
		}
		else {
			return null;
		}
	}

	public HandlerInterceptor[] getInterceptors(String lookupPath, PathMatcher pathMatcher) {
		Set<HandlerInterceptor> interceptors = new LinkedHashSet<HandlerInterceptor>();
		for (MappedInterceptor interceptor : this.mappedInterceptors) {
			if (matches(interceptor, lookupPath, pathMatcher)) {
				interceptors.add(interceptor.getInterceptor());
			}
		}
		return interceptors.toArray(new HandlerInterceptor[interceptors.size()]);
	}

	private boolean matches(MappedInterceptor interceptor, String lookupPath, PathMatcher pathMatcher) {
		String[] pathPatterns = interceptor.getPathPatterns();
		if (pathPatterns != null) {
			for (String pattern : pathPatterns) {
				if (pathMatcher.match(pattern, lookupPath)) {
					return true;
				}
			}
			return false;
		}
		else {
			return true;
		}
	}

}
