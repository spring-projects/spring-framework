/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.hypermedia;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * 
 * @author olivergierke
 */
public class MvcUriComponentsBuilderFactory implements MvcUris {

	private final List<? extends UriComponentsContributor> contributors;

	/**
	 * @param contributors
	 */
	public MvcUriComponentsBuilderFactory(
			List<? extends UriComponentsContributor> contributors) {
		this.contributors = contributors;
	}

	public UriComponentsBuilder from(Class<?> controller) {
		return from(controller, new Object[0]);
	}

	public UriComponentsBuilder from(Class<?> controller, Object... parameters) {
		return MvcUriComponentsBuilder.from(controller, parameters);
	}

	public UriComponentsBuilder from(Object invocationValue) {
		return MvcUriComponentsBuilder.from(invocationValue, contributors);
	}

	public UriComponentsBuilder from(Method method, Object... parameters) {
		return MvcUriComponentsBuilder.from(method, parameters, contributors);
	}
}
