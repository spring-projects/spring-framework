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

package org.springframework.core.io.support.jboss;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.URI;

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VFSUtils;

import org.springframework.core.io.Resource;
import org.springframework.core.io.AbstractResource;
import org.springframework.util.Assert;

/**
 * VFS based Resource.
 *
 * @author Ales Justin
 */
class VfsResource extends AbstractResource {

	private VirtualFile file;

	public VfsResource(VirtualFile file) {
		Assert.notNull(file, "The file cannot be null.");
		this.file = file;
	}

	public boolean exists() {
		try {
			return file.exists();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isOpen() {
		return false;
	}

	public boolean isReadable() {
		try {
			return file.getSize() > 0;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public long lastModified() {
		try {
			return file.getLastModified();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public URL getURL() throws IOException {
		try {
			return file.toURL();
		}
		catch (URISyntaxException e) {
			IOException ioe = new IOException(e.getMessage());
			ioe.initCause(e);
			throw ioe;
		}
	}

	public URI getURI() throws IOException {
		try {
			return file.toURI();
		}
		catch (URISyntaxException e) {
			IOException ioe = new IOException(e.getMessage());
			ioe.initCause(e);
			throw ioe;
		}
	}

	public File getFile() throws IOException {
		if (VFSUtils.isNestedFile(file)) {
			throw new IOException("This resource is a nested resource: " + file);
		}

		try {
			return new File(VFSUtils.getCompatibleURI(file));
		}
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			IOException ioe = new IOException(e.getMessage());
			ioe.initCause(e);
			throw ioe;
		}
	}

	@SuppressWarnings("deprecation")
	public Resource createRelative(String relativePath) throws IOException {
		return new VfsResource(file.findChild(relativePath));
	}

	public String getFilename() {
		return file.getName();
	}

	public String getDescription() {
		return file.toString();
	}

	public InputStream getInputStream() throws IOException {
		return file.openStream();
	}

	public String toString() {
		return getDescription();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof VfsResource) {
			return file.equals(((VfsResource)obj).file);
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.file.hashCode();
	}
}
