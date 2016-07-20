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

package org.springframework.web.portlet.context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.portlet.PortletContext;

import org.springframework.core.io.AbstractFileResolvingResource;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * {@link org.springframework.core.io.Resource} implementation for
 * {@link javax.portlet.PortletContext} resources, interpreting
 * relative paths within the portlet application root directory.
 *
 * <p>Always supports stream access and URL access, but only allows
 * {@code java.io.File} access when the portlet application archive
 * is expanded.
 *
 * @author Juergen Hoeller
 * @author John A. Lewis
 * @since 2.0
 * @see javax.portlet.PortletContext#getResourceAsStream
 * @see javax.portlet.PortletContext#getRealPath
 */
public class PortletContextResource extends AbstractFileResolvingResource implements ContextResource {

	private final PortletContext portletContext;

	private final String path;


	/**
	 * Create a new PortletContextResource.
	 * <p>The Portlet spec requires that resource paths start with a slash,
	 * even if many containers accept paths without leading slash too.
	 * Consequently, the given path will be prepended with a slash if it
	 * doesn't already start with one.
	 * @param portletContext the PortletContext to load from
	 * @param path the path of the resource
	 */
	public PortletContextResource(PortletContext portletContext, String path) {
		// check PortletContext
		Assert.notNull(portletContext, "Cannot resolve PortletContextResource without PortletContext");
		this.portletContext = portletContext;

		// check path
		Assert.notNull(path, "Path is required");
		String pathToUse = StringUtils.cleanPath(path);
		if (!pathToUse.startsWith("/")) {
			pathToUse = "/" + pathToUse;
		}
		this.path = pathToUse;
	}


	/**
	 * Return the PortletContext for this resource.
	 */
	public final PortletContext getPortletContext() {
		return this.portletContext;
	}

	/**
	 * Return the path for this resource.
	 */
	public final String getPath() {
		return this.path;
	}

	/**
	 * This implementation checks {@code PortletContext.getResource}.
	 * @see javax.portlet.PortletContext#getResource(String)
	 */
	@Override
	public boolean exists() {
		try {
			URL url = this.portletContext.getResource(this.path);
			return (url != null);
		}
		catch (MalformedURLException ex) {
			return false;
		}
	}

	/**
	 * This implementation delegates to {@code PortletContext.getResourceAsStream},
	 * which returns {@code null} in case of a non-readable resource (e.g. a directory).
	 * @see javax.portlet.PortletContext#getResourceAsStream(String)
	 */
	@Override
	public boolean isReadable() {
		InputStream is = this.portletContext.getResourceAsStream(this.path);
		if (is != null) {
			try {
				is.close();
			}
			catch (IOException ex) {
				// ignore
			}
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * This implementation delegates to {@code PortletContext.getResourceAsStream},
	 * but throws a FileNotFoundException if not found.
	 * @see javax.portlet.PortletContext#getResourceAsStream(String)
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is = this.portletContext.getResourceAsStream(this.path);
		if (is == null) {
			throw new FileNotFoundException("Could not open " + getDescription());
		}
		return is;
	}

	/**
	 * This implementation delegates to {@code PortletContext.getResource},
	 * but throws a FileNotFoundException if no resource found.
	 * @see javax.portlet.PortletContext#getResource(String)
	 */
	@Override
	public URL getURL() throws IOException {
		URL url = this.portletContext.getResource(this.path);
		if (url == null) {
			throw new FileNotFoundException(
					getDescription() + " cannot be resolved to URL because it does not exist");
		}
		return url;
	}

	/**
	 * This implementation resolves "file:" URLs or alternatively delegates to
	 * {@code PortletContext.getRealPath}, throwing a FileNotFoundException
	 * if not found or not resolvable.
	 * @see javax.portlet.PortletContext#getResource(String)
	 * @see javax.portlet.PortletContext#getRealPath(String)
	 */
	@Override
	public File getFile() throws IOException {
		URL url = getURL();
		if (ResourceUtils.isFileURL(url)) {
			// Proceed with file system resolution...
			return super.getFile();
		}
		else {
			String realPath = PortletUtils.getRealPath(this.portletContext, this.path);
			return new File(realPath);
		}
	}

	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new PortletContextResource(this.portletContext, pathToUse);
	}

	@Override
	public String getFilename() {
		return StringUtils.getFilename(this.path);
	}

	@Override
	public String getDescription() {
		return "PortletContext resource [" + this.path + "]";
	}

	@Override
	public String getPathWithinContext() {
		return this.path;
	}


	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof PortletContextResource) {
			PortletContextResource otherRes = (PortletContextResource) obj;
			return (this.portletContext.equals(otherRes.portletContext) && this.path.equals(otherRes.path));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
