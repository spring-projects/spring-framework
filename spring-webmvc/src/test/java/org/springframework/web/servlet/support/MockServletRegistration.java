/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletSecurityElement;

class MockServletRegistration implements ServletRegistration.Dynamic {

	private int loadOnStartup;

	private Set<String> mappings = new LinkedHashSet<>();

	private String roleName;

	private boolean asyncSupported = false;

	public int getLoadOnStartup() {
		return loadOnStartup;
	}

	@Override
	public void setLoadOnStartup(int loadOnStartup) {
		this.loadOnStartup = loadOnStartup;
	}

	@Override
	public void setRunAsRole(String roleName) {
		this.roleName = roleName;
	}

	@Override
	public Set<String> addMapping(String... urlPatterns) {
		mappings.addAll(Arrays.asList(urlPatterns));
		return mappings;
	}

	@Override
	public Collection<String> getMappings() {
		return mappings;
	}

	@Override
	public String getRunAsRole() {
		return roleName;
	}

	@Override
	public void setAsyncSupported(boolean isAsyncSupported) {
		this.asyncSupported = isAsyncSupported;
	}

	public boolean isAsyncSupported() {
		return this.asyncSupported;
	}

	// not implemented

	@Override
	public String getName() {
		return null;
	}

	@Override
	public void setMultipartConfig(MultipartConfigElement multipartConfig) {
	}

	@Override
	public Set<String> setServletSecurity(ServletSecurityElement constraint) {
		return null;
	}

	@Override
	public String getClassName() {
		return null;
	}

	@Override
	public boolean setInitParameter(String name, String value) {
		return false;
	}

	@Override
	public String getInitParameter(String name) {
		return null;
	}

	@Override
	public Set<String> setInitParameters(Map<String, String> initParameters) {
		return null;
	}

	@Override
	public Map<String, String> getInitParameters() {
		return null;
	}
}
