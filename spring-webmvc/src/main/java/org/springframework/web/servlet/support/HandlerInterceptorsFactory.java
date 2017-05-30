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

package org.springframework.web.servlet.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;

/**
 * Factory bean to generate list of HandlerMappings and MappedHandlers.
 *
 * MappedHandlers can be specified by url path to interceptors.
 *
 *
 * <pre>
 *  &lt;bean class="HandlerInterceptorsFactory" &gt;
 *    &lt;property name="handlerInterceptors" ref="list-of-global-interceptors" &gt;
 *    &lt;property name="interceptorsByPath" &gt;
 *      &lt;map&gt;
 *          &lt;entry key="/path/*" value-ref="list-of-interceptors"/&gt;
 *      &lt;/map&gt;
 *    &lt;/property&gt;
 *    &lt;property name="additionalMappedInterceptors" ref="list-of-mapped-interceptors" &gt;
 *  &lt;/bean&gt;
 * </pre>
 *
 * @author Tadaya Tsuyukubo
 * @since 3.2
 */
public class HandlerInterceptorsFactory extends AbstractFactoryBean<List<Object>> {

	private List<HandlerInterceptor> handlerInterceptors = new ArrayList<HandlerInterceptor>();

	private Map<String, List<HandlerInterceptor>> interceptorsByPath =
			new LinkedHashMap<String, List<HandlerInterceptor>>();

	private List<MappedInterceptor> additionalMappedInterceptors = new ArrayList<MappedInterceptor>();

	@Override
	public Class<?> getObjectType() {
		return List.class;
	}

	public HandlerInterceptorsFactory() {
		super();
	}

	@Override
	protected List<Object> createInstance() throws Exception {

		final List<MappedInterceptor> mappedInterceptors = getMappedInterceptors();

		final List<Object> interceptors = new ArrayList<Object>();
		interceptors.addAll(mappedInterceptors);
		interceptors.addAll(additionalMappedInterceptors);
		interceptors.addAll(handlerInterceptors); // regular interceptors
		return interceptors;

	}

	private List<MappedInterceptor> getMappedInterceptors() {

		// convert (path -> interceptors) to (interceptor -> paths)
		final Map<HandlerInterceptor, List<String>> map = new LinkedHashMap<HandlerInterceptor, List<String>>();
		for (Map.Entry<String, List<HandlerInterceptor>> entry : interceptorsByPath.entrySet()) {
			final String path = entry.getKey();
			final List<HandlerInterceptor> interceptors = entry.getValue();

			for (HandlerInterceptor interceptor : interceptors) {

				List<String> paths = map.get(interceptor);
				if (paths == null) {
					paths = new ArrayList<String>();
					map.put(interceptor, paths);
				}

				paths.add(path);
			}
		}

		final List<MappedInterceptor> result = new ArrayList<MappedInterceptor>();

		// create mapped interceptors
		for (Map.Entry<HandlerInterceptor, List<String>> entry : map.entrySet()) {
			final HandlerInterceptor interceptor = entry.getKey();
			final List<String> paths = entry.getValue();

			final MappedInterceptor mappedInterceptor =
					new MappedInterceptor(paths.toArray(new String[paths.size()]), interceptor);

			result.add(mappedInterceptor);
		}

		return result;

	}

	public void setInterceptorsByPath(Map<String, List<HandlerInterceptor>> interceptorsByPath) {
		this.interceptorsByPath = interceptorsByPath;
	}

	public void setHandlerInterceptors(List<HandlerInterceptor> handlerInterceptors) {
		this.handlerInterceptors = handlerInterceptors;
	}

	public void setAdditionalMappedInterceptors(List<MappedInterceptor> additionalMappedInterceptors) {
		this.additionalMappedInterceptors = additionalMappedInterceptors;
	}
}
