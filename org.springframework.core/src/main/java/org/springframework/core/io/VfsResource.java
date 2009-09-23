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

package org.springframework.core.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.jboss.virtual.VFS;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;

import org.springframework.core.NestedIOException;
import org.springframework.util.Assert;

/**
 * VFS based {@link Resource} implementation.
 *
 * @author Ales Justin
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.jboss.virtual.VirtualFile
 */
public class VfsResource extends AbstractResource {

	private final VirtualFile file;


	public VfsResource(VirtualFile file) {
		Assert.notNull(file, "VirtualFile must not be null");
		this.file = file;
	}


	public boolean exists() {
		try {
			return this.file.exists();
		}
		catch (IOException ex) {
			return false;
		}
	}

	public boolean isReadable() {
		try {
			return (this.file.getSize() > 0);
		}
		catch (IOException e) {
			return false;
		}
	}

	public long lastModified() throws IOException {
		return this.file.getLastModified();
	}

	public InputStream getInputStream() throws IOException {
		return this.file.openStream();
	}

	public URL getURL() throws IOException {
		try {
			return this.file.toURL();
		}
		catch (Exception ex) {
			throw new NestedIOException("Failed to obtain URL for file " + this.file, ex);
		}
	}

	public URI getURI() throws IOException {
		try {
			return this.file.toURI();
		}
		catch (Exception ex) {
			throw new NestedIOException("Failed to obtain URI for " + this.file, ex);
		}
	}

	public File getFile() throws IOException {
		if (VFSUtils.isNestedFile(this.file)) {
			throw new IOException("File resolution not supported for nested resource: " + this.file);
		}
		try {
			return new File(VFSUtils.getCompatibleURI(file));
		}
		catch (Exception ex) {
			throw new NestedIOException("Failed to obtain File reference for " + this.file, ex);
		}
	}

	public Resource createRelative(String relativePath) throws IOException {
		return new VfsResource(VFS.getRoot(new URL(getURL(), relativePath)));
	}

	public String getFilename() {
		return this.file.getName();
	}

	public String getDescription() {
		return this.file.toString();
	}


	@Override
	public boolean equals(Object obj) {
		return (obj == this || (obj instanceof VfsResource && this.file.equals(((VfsResource) obj).file)));
	}

	@Override
	public int hashCode() {
		return this.file.hashCode();
	}

}
