/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.mock.web.portlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequestDispatcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.web.util.WebUtils;

/**
 * Mock implementation of the {@link javax.portlet.PortletContext} interface.
 *
 * @author John A. Lewis
 * @author Juergen Hoeller
 * @since 2.0
 */
public class MockPortletContext implements PortletContext {

	private static final String TEMP_DIR_SYSTEM_PROPERTY = "java.io.tmpdir";


	private final Log logger = LogFactory.getLog(getClass());

	private final String resourceBasePath;

	private final ResourceLoader resourceLoader;

	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

	private final Map<String, String> initParameters = new LinkedHashMap<String, String>();

	private String portletContextName = "MockPortletContext";

	private Set<String> containerRuntimeOptions = new LinkedHashSet<String>();


	/**
	 * Create a new MockPortletContext with no base path and a
	 * DefaultResourceLoader (i.e. the classpath root as WAR root).
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public MockPortletContext() {
		this("", null);
	}

	/**
	 * Create a new MockPortletContext using a DefaultResourceLoader.
	 * @param resourceBasePath the WAR root directory (should not end with a slash)
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public MockPortletContext(String resourceBasePath) {
		this(resourceBasePath, null);
	}

	/**
	 * Create a new MockPortletContext, using the specified ResourceLoader
	 * and no base path.
	 * @param resourceLoader the ResourceLoader to use (or null for the default)
	 */
	public MockPortletContext(ResourceLoader resourceLoader) {
		this("", resourceLoader);
	}

	/**
	 * Create a new MockPortletContext.
	 * @param resourceBasePath the WAR root directory (should not end with a slash)
	 * @param resourceLoader the ResourceLoader to use (or null for the default)
	 */
	public MockPortletContext(String resourceBasePath, ResourceLoader resourceLoader) {
		this.resourceBasePath = (resourceBasePath != null ? resourceBasePath : "");
		this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());

		// Use JVM temp dir as PortletContext temp dir.
		String tempDir = System.getProperty(TEMP_DIR_SYSTEM_PROPERTY);
		if (tempDir != null) {
			this.attributes.put(WebUtils.TEMP_DIR_CONTEXT_ATTRIBUTE, new File(tempDir));
		}
	}

	/**
	 * Build a full resource location for the given path,
	 * prepending the resource base path of this MockPortletContext.
	 * @param path the path as specified
	 * @return the full resource path
	 */
	protected String getResourceLocation(String path) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return this.resourceBasePath + path;
	}


	@Override
	public String getServerInfo() {
		return "MockPortal/1.0";
	}

	@Override
	public PortletRequestDispatcher getRequestDispatcher(String path) {
		if (!path.startsWith("/")) {
			throw new IllegalArgumentException(
					"PortletRequestDispatcher path at PortletContext level must start with '/'");
		}
		return new MockPortletRequestDispatcher(path);
	}

	@Override
	public PortletRequestDispatcher getNamedDispatcher(String path) {
		return null;
	}

	@Override
	public InputStream getResourceAsStream(String path) {
		Resource resource = this.resourceLoader.getResource(getResourceLocation(path));
		try {
			return resource.getInputStream();
		}
		catch (IOException ex) {
			logger.info("Couldn't open InputStream for " + resource, ex);
			return null;
		}
	}

	@Override
	public int getMajorVersion() {
		return 2;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public String getMimeType(String filePath) {
		return null;
	}

	@Override
	public String getRealPath(String path) {
		Resource resource = this.resourceLoader.getResource(getResourceLocation(path));
		try {
			return resource.getFile().getAbsolutePath();
		}
		catch (IOException ex) {
			logger.info("Couldn't determine real path of resource " + resource, ex);
			return null;
		}
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		Resource resource = this.resourceLoader.getResource(getResourceLocation(path));
		try {
			File file = resource.getFile();
			String[] fileList = file.list();
			String prefix = (path.endsWith("/") ? path : path + "/");
			Set<String> resourcePaths = new HashSet<String>(fileList.length);
			for (String fileEntry : fileList) {
				resourcePaths.add(prefix + fileEntry);
			}
			return resourcePaths;
		}
		catch (IOException ex) {
			logger.info("Couldn't get resource paths for " + resource, ex);
			return null;
		}
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		Resource resource = this.resourceLoader.getResource(getResourceLocation(path));
		try {
			return resource.getURL();
		}
		catch (IOException ex) {
			logger.info("Couldn't get URL for " + resource, ex);
			return null;
		}
	}

	@Override
	public Object getAttribute(String name) {
		return this.attributes.get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(this.attributes.keySet());
	}

	@Override
	public void setAttribute(String name, Object value) {
		if (value != null) {
			this.attributes.put(name, value);
		}
		else {
			this.attributes.remove(name);
		}
	}

	@Override
	public void removeAttribute(String name) {
		this.attributes.remove(name);
	}

	public void addInitParameter(String name, String value) {
		Assert.notNull(name, "Parameter name must not be null");
		this.initParameters.put(name, value);
	}

	@Override
	public String getInitParameter(String name) {
		Assert.notNull(name, "Parameter name must not be null");
		return this.initParameters.get(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(this.initParameters.keySet());
	}

	@Override
	public void log(String message) {
		logger.info(message);
	}

	@Override
	public void log(String message, Throwable t) {
		logger.info(message, t);
	}

	public void setPortletContextName(String portletContextName) {
		this.portletContextName = portletContextName;
	}

	@Override
	public String getPortletContextName() {
		return this.portletContextName;
	}

	public void addContainerRuntimeOption(String key) {
		this.containerRuntimeOptions.add(key);
	}

	@Override
	public Enumeration<String> getContainerRuntimeOptions() {
		return Collections.enumeration(this.containerRuntimeOptions);
	}

}
