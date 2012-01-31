/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.scripting.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.scripting.ScriptSource} implementation
 * based on Spring's {@link org.springframework.core.io.Resource}
 * abstraction. Loads the script text from the underlying Resource's
 * {@link org.springframework.core.io.Resource#getFile() File} or
 * {@link org.springframework.core.io.Resource#getInputStream() InputStream},
 * and tracks the last-modified timestamp of the file (if possible).
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.core.io.Resource#getInputStream()
 * @see org.springframework.core.io.Resource#getFile()
 * @see org.springframework.core.io.ResourceLoader
 */
public class ResourceScriptSource implements ScriptSource {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private final Resource resource;

	private long lastModified = -1;

	private final Object lastModifiedMonitor = new Object();

	private String encoding = "UTF-8";

	/**
	 * Create a new ResourceScriptSource for the given resource.
	 * @param resource the Resource to load the script from
	 */
	public ResourceScriptSource(Resource resource) {
		Assert.notNull(resource, "Resource must not be null");
		this.resource = resource;
	}

	/**
	 * Return the {@link org.springframework.core.io.Resource} to load the
	 * script from.
	 */
	public final Resource getResource() {
		return this.resource;
	}

	public String getScriptAsString() throws IOException {
		synchronized (this.lastModifiedMonitor) {
			this.lastModified = retrieveLastModifiedTime();
		}

		InputStream stream = this.resource.getInputStream();
		Reader reader = (StringUtils.hasText(encoding) ? new InputStreamReader(stream, encoding)
				: new InputStreamReader(stream));

		return FileCopyUtils.copyToString(reader);
	}

	public boolean isModified() {
		synchronized (this.lastModifiedMonitor) {
			return (this.lastModified < 0 || retrieveLastModifiedTime() > this.lastModified);
		}
	}

	/**
	 * Retrieve the current last-modified timestamp of the underlying resource.
	 * @return the current timestamp, or 0 if not determinable
	 */
	protected long retrieveLastModifiedTime() {
		try {
			return getResource().lastModified();
		} catch (IOException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug(getResource() + " could not be resolved in the file system - "
						+ "current timestamp not available for script modification check", ex);
			}
			return 0;
		}
	}

	public String suggestedClassName() {
		return StringUtils.stripFilenameExtension(getResource().getFilename());
	}

	/**
	 * Sets the encoding used for reading the script resource. The default value is "UTF-8".
	 * A null value, implies the platform default.
	 * 
	 * @param encoding charset encoding used for reading the script.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	@Override
	public String toString() {
		return this.resource.toString();
	}
}