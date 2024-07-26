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

package org.springframework.mock.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;

import org.springframework.lang.Nullable;

/**
 * Mock implementation of {@link FilterRegistration}.
 *
 * @author Rossen Stoyanchev
 * @since 6.2
 */
public class MockFilterRegistration implements FilterRegistration.Dynamic {

	private final String name;

	private final String className;

	private final Map<String, String> initParameters = new LinkedHashMap<>();

	private final List<String> servletNames = new ArrayList<>();

	private final List<String> urlPatterns = new ArrayList<>();

	private boolean asyncSupported;


	public MockFilterRegistration(String className) {
		this(className, "");
	}

	public MockFilterRegistration(String className, String name) {
		this.name = name;
		this.className = className;
	}


	@Override
	public String getName() {
		return this.name;
	}

	@Nullable
	@Override
	public String getClassName() {
		return this.className;
	}

	@Override
	public boolean setInitParameter(String name, String value) {
		return (this.initParameters.putIfAbsent(name, value) != null);
	}

	@Nullable
	@Override
	public String getInitParameter(String name) {
		return this.initParameters.get(name);
	}

	@Override
	public Set<String> setInitParameters(Map<String, String> initParameters) {
		Set<String> existingParameterNames = new LinkedHashSet<>();
		for (Map.Entry<String, String> entry : initParameters.entrySet()) {
			if (this.initParameters.get(entry.getKey()) != null) {
				existingParameterNames.add(entry.getKey());
			}
		}
		if (existingParameterNames.isEmpty()) {
			this.initParameters.putAll(initParameters);
		}
		return existingParameterNames;
	}

	@Override
	public Map<String, String> getInitParameters() {
		return Collections.unmodifiableMap(this.initParameters);
	}

	@Override
	public void addMappingForServletNames(
			EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... servletNames) {

		this.servletNames.addAll(Arrays.asList(servletNames));
	}

	@Override
	public Collection<String> getServletNameMappings() {
		return Collections.unmodifiableCollection(this.servletNames);
	}

	@Override
	public void addMappingForUrlPatterns(
			EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPatterns) {

		this.urlPatterns.addAll(Arrays.asList(urlPatterns));
	}

	@Override
	public Collection<String> getUrlPatternMappings() {
		return Collections.unmodifiableCollection(this.urlPatterns);
	}

	@Override
	public void setAsyncSupported(boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	public boolean isAsyncSupported() {
		return this.asyncSupported;
	}

}
