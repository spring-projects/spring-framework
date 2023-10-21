/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.method.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Controller interceptor by delegating to a list of registered
 * {@link ControllerInterceptor ControllerInterceptors}
 *
 * @author Bruce
 */
public class ControllerInterceptorComposite implements ControllerInterceptor {

	private final List<ControllerInterceptor> allInterceptors = new ArrayList<>();


	public void add(ControllerInterceptor interceptor) {
		allInterceptors.add(interceptor);
		AnnotationAwareOrderComparator.sort(this.allInterceptors);
	}

	public void addAll(Collection<ControllerInterceptor> interceptors) {
		allInterceptors.addAll(interceptors);
		AnnotationAwareOrderComparator.sort(this.allInterceptors);
	}

	@Override
	public void beforeInvoke(HandlerMethod handlerMethod, Method bridged, Object[] args, HttpServletRequest req) {
		for (ControllerInterceptor interceptor : allInterceptors) {
			interceptor.beforeInvoke(handlerMethod, bridged, args, req);
		}
	}

	@Override
	public Object afterInvoke(HandlerMethod handlerMethod, Method bridged, Object[] args, Object returnValue, HttpServletRequest req) {
		Object returnVal = returnValue;
		for (ControllerInterceptor interceptor : allInterceptors) {
			returnVal = interceptor.afterInvoke(handlerMethod, bridged, args, returnVal, req);
		}
		return returnVal;
	}

}
