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
package org.springframework.web.servlet.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.ProblemCollector;
import org.springframework.context.config.AbstractFeatureSpecification;
import org.springframework.context.config.FeatureSpecificationExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;

/**
 * Specifies the Spring MVC "interceptors" container feature. The feature
 * registers one or more {@link MappedInterceptor} bean definitions. A
 * MappedInterceptor encapsulates an interceptor and one or more (optional)
 * path patterns to which the interceptor is mapped. The interceptor can be 
 * of type {@link HandlerInterceptor} or {@link WebRequestInterceptor}. 
 * An interceptor can also be provided without path patterns in which case
 * it applies globally to all handler invocations.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class MvcInterceptors extends AbstractFeatureSpecification {

	private static final Class<? extends FeatureSpecificationExecutor> EXECUTOR_TYPE = MvcInterceptorsExecutor.class;

	private Map<Object, String[]> interceptorMappings = new LinkedHashMap<Object, String[]>();

	/**
	 * Creates an MvcInterceptors instance.
	 */
	public MvcInterceptors() {
		super(EXECUTOR_TYPE);
	}

	/**
	 * Add one or more {@link HandlerInterceptor HandlerInterceptors} that should 
	 * intercept all handler invocations.
	 * 
	 * @param interceptors one or more interceptors 
	 */
	public MvcInterceptors globalInterceptors(HandlerInterceptor... interceptors) {
		addInterceptorMappings(null, interceptors);
		return this;
	}

	/**
	 * Add one or more {@link WebRequestInterceptor WebRequestInterceptors} that should 
	 * intercept all handler invocations.
	 * 
	 * @param interceptors one or more interceptors
	 */
	public MvcInterceptors globalInterceptors(WebRequestInterceptor... interceptors) {
		addInterceptorMappings(null, interceptors);
		return this;
	}

	/**
	 * Add one or more interceptors by bean name that should intercept all handler 
	 * invocations.
	 * 
	 * @param interceptors interceptor bean names
	 */
	public MvcInterceptors globalInterceptors(String... interceptors) {
		addInterceptorMappings(null, interceptors);
		return this;
	}

	/**
	 * Add one or more {@link HandlerInterceptor HandlerInterceptors} and map  
	 * them to the specified path patterns. 
	 * 
	 * @param pathPatterns the pathPatterns to map the interceptor to
	 * @param interceptors the interceptors
	 */
	public MvcInterceptors mappedInterceptors(String[] pathPatterns, HandlerInterceptor... interceptors) {
		addInterceptorMappings(pathPatterns, interceptors);
		return this;
	}

	/**
	 * Add one or more {@link WebRequestInterceptor WebRequestInterceptors} and 
	 * map them to the specified path patterns. 
	 * 
	 * @param pathPatterns the pathPatterns to map the interceptor to
	 * @param interceptors the interceptors
	 */
	public MvcInterceptors mappedInterceptors(String[] pathPatterns, WebRequestInterceptor... interceptors) {
		addInterceptorMappings(pathPatterns, interceptors);
		return this;
	}

	/**
	 * Add one or more interceptors by bean name and map them to the specified 
	 * path patterns. 
	 * 
	 * @param pathPatterns the pathPatterns to map to
	 * @param interceptors the interceptors
	 */
	public MvcInterceptors mappedInterceptors(String[] pathPatterns, String... interceptors) {
		addInterceptorMappings(pathPatterns, interceptors);
		return this;
	}

	void interceptor(String[] pathPatterns, BeanDefinitionHolder interceptor) {
		addInterceptorMappings(pathPatterns, new Object[] { interceptor });
	}

	Map<Object, String[]> interceptorMappings() {
		return Collections.unmodifiableMap(interceptorMappings);
	}

	private void addInterceptorMappings(String[] pathPatterns, Object[] interceptors) {
		for (Object interceptor : interceptors) {
			interceptorMappings.put(interceptor, pathPatterns);
		}
	}

	@Override
	protected void doValidate(ProblemCollector problems) {
		if (interceptorMappings.size() == 0) {
			problems.error("No interceptors defined.");
		}
		for (Object interceptor : interceptorMappings.keySet()) {
			if (interceptor == null) {
				problems.error("Null interceptor provided.");
			}
			if (interceptorMappings.get(interceptor) != null) {
				for (String pattern : interceptorMappings.get(interceptor)) {
					if (!StringUtils.hasText(pattern)) {
						problems.error("Empty path pattern specified for " + interceptor);
					}
				}
			}
		}
	}

}
