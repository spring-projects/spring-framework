/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;

class MockDynamic implements ServletRegistration.Dynamic {

	private int loadOnStartup;

	private Set<String> mappings = new LinkedHashSet<String>();

	private String roleName;

	public int getLoadOnStartup() {
		return loadOnStartup;
	}

	public void setLoadOnStartup(int loadOnStartup) {
		this.loadOnStartup = loadOnStartup;
	}

	public void setRunAsRole(String roleName) {
		this.roleName = roleName;
	}

	public Set<String> addMapping(String... urlPatterns) {
		mappings.addAll(Arrays.asList(urlPatterns));
		return mappings;
	}

	public Collection<String> getMappings() {
		return mappings;
	}

	public String getRunAsRole() {
		return roleName;
	}

	// not implemented
	public void setAsyncSupported(boolean isAsyncSupported) {
	}

	public String getName() {
		return null;
	}

	public void setMultipartConfig(MultipartConfigElement multipartConfig) {
	}

	public Set<String> setServletSecurity(ServletSecurityElement constraint) {
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
