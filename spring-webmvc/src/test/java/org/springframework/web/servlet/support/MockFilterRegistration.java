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

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;

class MockFilterRegistration implements Dynamic {

	private boolean asyncSupported = false;

	private Map<String, EnumSet<DispatcherType>> mappings = new HashMap<String, EnumSet<DispatcherType>>();


	public Map<String, EnumSet<DispatcherType>> getMappings() {
		return this.mappings;
	}

	public boolean isAsyncSupported() {
		return this.asyncSupported;
	}

	public void setAsyncSupported(boolean isAsyncSupported) {
		this.asyncSupported = isAsyncSupported;
	}

	public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes,
			boolean isMatchAfter, String... servletNames) {

		for (String servletName : servletNames) {
			this.mappings.put(servletName, dispatcherTypes);
		}
	}

	// Not implemented

	public String getName() {
		return null;
	}

	public Collection<String> getServletNameMappings() {
		return null;
	}

	public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes,
			boolean isMatchAfter, String... urlPatterns) {
	}

	public Collection<String> getUrlPatternMappings() {
		return null;
	}

	public String getClassName() {
		return null;
	}

	public boolean setInitParameter(String name, String value) {
		return false;
	}

	public String getInitParameter(String name) {
		return null;
	}

	public Set<String> setInitParameters(Map<String, String> initParameters) {
		return null;
	}

	public Map<String, String> getInitParameters() {
		return null;
	}

}
