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

package org.springframework.core.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Resource} implementation for <code>java.net.URL</code> locators.
 * Obviously supports resolution as URL, and also as File in case of
 * the "file:" protocol.
 *
 * @author Juergen Hoeller
 * @since 28.12.2003
 * @see java.net.URL
 */
public class UrlResource extends AbstractResource {

	/**
	 * Original URL, used for actual access.
	 */
	private final URL url;

	/**
	 * Cleaned URL (with normalized path), used for comparisons.
	 */
	private final URL cleanedUrl;

	/**
	 * Original URI, if available; used for URI and File access.
	 */
	private final URI uri;


	/**
	 * Create a new UrlResource.
	 * @param url a URL
	 */
	public UrlResource(URL url) {
		Assert.notNull(url, "URL must not be null");
		this.url = url;
		this.cleanedUrl = getCleanedUrl(this.url, url.toString());
		this.uri = null;
	}

	/**
	 * Create a new UrlResource.
	 * @param uri a URI
	 * @throws MalformedURLException if the given URL path is not valid
	 */
	public UrlResource(URI uri) throws MalformedURLException {
		Assert.notNull(uri, "URI must not be null");
		this.url = uri.toURL();
		this.cleanedUrl = getCleanedUrl(this.url, uri.toString());
		this.uri = uri;
	}

	/**
	 * Create a new UrlResource.
	 * @param path a URL path
	 * @throws MalformedURLException if the given URL path is not valid
	 */
	public UrlResource(String path) throws MalformedURLException {
		Assert.notNull(path, "Path must not be null");
		this.url = new URL(path);
		this.cleanedUrl = getCleanedUrl(this.url, path);
		this.uri = null;
	}

	/**
	 * Determine a cleaned URL for the given original URL.
	 * @param originalUrl the original URL
	 * @param originalPath the original URL path
	 * @return the cleaned URL
	 * @see org.springframework.util.StringUtils#cleanPath
	 */
	private URL getCleanedUrl(URL originalUrl, String originalPath) {
		try {
			return new URL(StringUtils.cleanPath(originalPath));
		}
		catch (MalformedURLException ex) {
			// Cleaned URL path cannot be converted to URL
			// -> take original URL.
			return originalUrl;
		}
	}


	/**
	 * This implementation opens an InputStream for the given URL.
	 * It sets the "UseCaches" flag to <code>false</code>,
	 * mainly to avoid jar file locking on Windows.
	 * @see java.net.URL#openConnection()
	 * @see java.net.URLConnection#setUseCaches(boolean)
	 * @see java.net.URLConnection#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
		URLConnection con = this.url.openConnection();
		con.setUseCaches(false);
		return con.getInputStream();
	}

	/**
	 * This implementation returns the underlying URL reference.
	 */
	public URL getURL() throws IOException {
		return this.url;
	}

	/**
	 * This implementation returns the underlying URI directly,
	 * if possible.
	 */
	public URI getURI() throws IOException {
		if (this.uri != null) {
			return this.uri;
		}
		else {
			return super.getURI();
		}
	}

	/**
	 * This implementation returns a File reference for the underlying URL/URI,
	 * provided that it refers to a file in the file system.
	 * @see org.springframework.util.ResourceUtils#getFile(java.net.URL, String)
	 */
	public File getFile() throws IOException {
		if (this.uri != null) {
			return ResourceUtils.getFile(this.uri, getDescription());
		}
		else {
			return ResourceUtils.getFile(this.url, getDescription());
		}
	}

	/**
	 * This implementation determines the underlying File
	 * (or jar file, in case of a resource in a jar/zip).
	 */
	protected File getFileForLastModifiedCheck() throws IOException {
		if (ResourceUtils.isJarURL(this.url)) {
			URL actualUrl = ResourceUtils.extractJarFileURL(this.url);
			return ResourceUtils.getFile(actualUrl);
		}
		else {
			return getFile();
		}
	}

	/**
	 * This implementation creates a UrlResource, applying the given path
	 * relative to the path of the underlying URL of this resource descriptor.
	 * @see java.net.URL#URL(java.net.URL, String)
	 */
	public Resource createRelative(String relativePath) throws MalformedURLException {
		if (relativePath.startsWith("/")) {
			relativePath = relativePath.substring(1);
		}
		return new UrlResource(new URL(this.url, relativePath));
	}

	/**
	 * This implementation returns the name of the file that this URL refers to.
	 * @see java.net.URL#getFile()
	 * @see java.io.File#getName()
	 */
	public String getFilename() {
		return new File(this.url.getFile()).getName();
	}

	/**
	 * This implementation returns a description that includes the URL.
	 */
	public String getDescription() {
		return "URL [" + this.url + "]";
	}


	/**
	 * This implementation compares the underlying URL references.
	 */
	public boolean equals(Object obj) {
		return (obj == this ||
		    (obj instanceof UrlResource && this.cleanedUrl.equals(((UrlResource) obj).cleanedUrl)));
	}

	/**
	 * This implementation returns the hash code of the underlying URL reference.
	 */
	public int hashCode() {
		return this.cleanedUrl.hashCode();
	}

}
