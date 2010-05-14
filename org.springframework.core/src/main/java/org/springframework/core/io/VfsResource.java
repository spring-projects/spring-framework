/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.core.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.springframework.core.NestedIOException;
import org.springframework.util.Assert;

/**
 * VFS based {@link Resource} implementation.
 *
 * @author Ales Justin
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 3.0
 * @see org.jboss.virtual.VirtualFile
 * @see org.jboss.vfs.VirtualFile
 */
public class VfsResource extends AbstractResource {

	private final Object resource;

	public VfsResource(Object resources) {
		Assert.notNull(resources, "VirtualFile must not be null");
		this.resource = resources;
	}

	public boolean exists() {
		return VfsUtils.exists(resource);
	}

	public boolean isReadable() {
		return VfsUtils.isReadable(resource);
	}

	public long lastModified() throws IOException {
		return VfsUtils.getLastModified(resource);
	}

	public InputStream getInputStream() throws IOException {
		return VfsUtils.getInputStream(resource);
	}

	public URL getURL() throws IOException {
		try {
			return VfsUtils.getURL(resource);
		} catch (Exception ex) {
			throw new NestedIOException("Failed to obtain URL for file " + this.resource, ex);
		}
	}

	public URI getURI() throws IOException {
		try {
			return VfsUtils.getURI(resource);
		} catch (Exception ex) {
			throw new NestedIOException("Failed to obtain URI for " + this.resource, ex);
		}
	}

	public File getFile() throws IOException {
		return VfsUtils.getFile(resource);
	}

	public Resource createRelative(String relativePath) throws IOException {
		if (!relativePath.startsWith(".") && relativePath.contains("/")) {
			try {
				return new VfsResource(VfsUtils.getChild(resource, relativePath));
			} catch (IOException ex) {
				// fall back to #getRelative
			}
		}

		return new VfsResource(VfsUtils.getRelative(new URL(getURL(), relativePath)));
	}

	public String getFilename() {
		return VfsUtils.getName(resource);
	}

	public String getDescription() {
		return this.resource.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return (obj == this || (obj instanceof VfsResource && this.resource.equals(((VfsResource) obj).resource)));
	}

	@Override
	public int hashCode() {
		return this.resource.hashCode();
	}
}