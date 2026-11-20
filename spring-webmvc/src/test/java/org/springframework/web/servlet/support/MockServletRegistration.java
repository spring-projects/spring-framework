/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
