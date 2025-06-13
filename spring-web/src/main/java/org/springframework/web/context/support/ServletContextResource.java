/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.context.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import jakarta.servlet.ServletContext;
import org.jspecify.annotations.Nullable;

import org.springframework.core.io.AbstractFileResolvingResource;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * {@link org.springframework.core.io.Resource} implementation for
 * {@link jakarta.servlet.ServletContext} resources, interpreting
 * relative paths within the web application root directory.
 *
 * <p>Always supports stream access and URL access, but only allows
 * {@code java.io.File} access when the web application archive
 * is expanded.
 *
 * @author Juergen Hoeller
 * @since 28.12.2003
 * @see jakarta.servlet.ServletContext#getResourceAsStream
 * @see jakarta.servlet.ServletContext#getResource
 * @see jakarta.servlet.ServletContext#getRealPath
 */
public class ServletContextResource extends AbstractFileResolvingResource implements ContextResource {

	private final ServletContext servletContext;

	private final String path;


	/**
	 * Create a new {@code ServletContextResource} for the given path.
	 * <p>The Servlet spec requires that resource paths start with a slash,
	 * even if many containers accept paths without leading slash too.
	 * Consequently, the given path will be prepended with a slash if it
	 * doesn't already start with one.
	 * @param servletContext the ServletContext to load from
	 * @param path the path of the resource
	 */
	public ServletContextResource(ServletContext servletContext, String path) {
		// check ServletContext
		Assert.notNull(servletContext, "Cannot resolve ServletContextResource without ServletContext");
		this.servletContext = servletContext;

		// check path
		Assert.notNull(path, "Path is required");
		String pathToUse = StringUtils.cleanPath(path);
		if (!pathToUse.startsWith("/")) {
			pathToUse = "/" + pathToUse;
		}
		this.path = pathToUse;
	}


	/**
	 * Return the ServletContext for this resource.
	 */
	public final ServletContext getServletContext() {
		return this.servletContext;
	}

	/**
	 * Return the path for this resource.
	 */
	public final String getPath() {
		return this.path;
	}


	/**
	 * This implementation checks {@code ServletContext.getResource}.
	 * @see jakarta.servlet.ServletContext#getResource(String)
	 */
	@Override
	public boolean exists() {
		try {
			URL url = this.servletContext.getResource(this.path);
			return (url != null);
		}
		catch (MalformedURLException ex) {
			return false;
		}
	}

	/**
	 * This implementation delegates to {@code ServletContext.getResourceAsStream},
	 * which returns {@code null} in case of a non-readable resource (for example, a directory).
	 * @see jakarta.servlet.ServletContext#getResourceAsStream(String)
	 */
	@Override
	public boolean isReadable() {
		InputStream is = this.servletContext.getResourceAsStream(this.path);
		if (is != null) {
			try {
				is.close();
			}
			catch (IOException ignored) {
			}
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean isFile() {
		try {
			URL url = this.servletContext.getResource(this.path);
			if (url != null && ResourceUtils.isFileURL(url)) {
				return true;
			}
			else {
				String realPath = this.servletContext.getRealPath(this.path);
				if (realPath == null) {
					return false;
				}
				File file = new File(realPath);
				return (file.exists() && file.isFile());
			}
		}
		catch (IOException ex) {
			return false;
		}
	}

	/**
	 * This implementation delegates to {@code ServletContext.getResourceAsStream},
	 * but throws a FileNotFoundException if no resource found.
	 * @see jakarta.servlet.ServletContext#getResourceAsStream(String)
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is = this.servletContext.getResourceAsStream(this.path);
		if (is == null) {
			throw new FileNotFoundException("Could not open " + getDescription());
		}
		return is;
	}

	/**
	 * This implementation delegates to {@code ServletContext.getResource},
	 * but throws a FileNotFoundException if no resource found.
	 * @see jakarta.servlet.ServletContext#getResource(String)
	 */
	@Override
	public URL getURL() throws IOException {
		URL url = this.servletContext.getResource(this.path);
		if (url == null) {
			throw new FileNotFoundException(
					getDescription() + " cannot be resolved to URL because it does not exist");
		}
		return url;
	}

	/**
	 * This implementation resolves "file:" URLs or alternatively delegates to
	 * {@code ServletContext.getRealPath}, throwing a FileNotFoundException
	 * if not found or not resolvable.
	 * @see jakarta.servlet.ServletContext#getResource(String)
	 * @see jakarta.servlet.ServletContext#getRealPath(String)
	 */
	@Override
	public File getFile() throws IOException {
		URL url = this.servletContext.getResource(this.path);
		if (url != null && ResourceUtils.isFileURL(url)) {
			// Proceed with file system resolution...
			return super.getFile();
		}
		else {
			String realPath = WebUtils.getRealPath(this.servletContext, this.path);
			return new File(realPath);
		}
	}

	/**
	 * This implementation creates a ServletContextResource, applying the given path
	 * relative to the path of the underlying file of this resource descriptor.
	 * @see org.springframework.util.StringUtils#applyRelativePath(String, String)
	 */
	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new ServletContextResource(this.servletContext, pathToUse);
	}

	/**
	 * This implementation returns the name of the file that this ServletContext
	 * resource refers to.
	 * @see org.springframework.util.StringUtils#getFilename(String)
	 */
	@Override
	public @Nullable String getFilename() {
		return StringUtils.getFilename(this.path);
	}

	/**
	 * This implementation returns a description that includes the ServletContext
	 * resource location.
	 */
	@Override
	public String getDescription() {
		return "ServletContext resource [" + this.path + "]";
	}

	@Override
	public String getPathWithinContext() {
		return this.path;
	}


	/**
	 * This implementation compares the underlying ServletContext resource locations.
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ServletContextResource that &&
				this.path.equals(that.path) && this.servletContext.equals(that.servletContext)));
	}

	/**
	 * This implementation returns the hash code of the underlying
	 * ServletContext resource location.
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
