/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.core.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Resource} implementation for {@code java.net.URL} locators.
 * Supports resolution as a {@code URL} and also as a {@code File} in
 * case of the {@code "file:"} protocol.
 *
 * @author Juergen Hoeller
 * @since 28.12.2003
 * @see java.net.URL
 */
public class UrlResource extends AbstractFileResolvingResource {

	/**
	 * Original URI, if available; used for URI and File access.
	 */
	@Nullable
	private final URI uri;

	/**
	 * Original URL, used for actual access.
	 */
	private final URL url;

	/**
	 * Cleaned URL (with normalized path), used for comparisons.
	 */
	@Nullable
	private volatile URL cleanedUrl;


	/**
	 * Create a new {@code UrlResource} based on the given URL object.
	 * @param url a URL
	 * @see #UrlResource(URI)
	 * @see #UrlResource(String)
	 */
	public UrlResource(URL url) {
		Assert.notNull(url, "URL must not be null");
		this.uri = null;
		this.url = url;
	}

	/**
	 * Create a new {@code UrlResource} based on the given URI object.
	 * @param uri a URI
	 * @throws MalformedURLException if the given URL path is not valid
	 * @since 2.5
	 */
	public UrlResource(URI uri) throws MalformedURLException {
		Assert.notNull(uri, "URI must not be null");
		this.uri = uri;
		this.url = uri.toURL();
	}

	/**
	 * Create a new {@code UrlResource} based on a URL path.
	 * <p>Note: The given path needs to be pre-encoded if necessary.
	 * @param path a URL path
	 * @throws MalformedURLException if the given URL path is not valid
	 * @see java.net.URL#URL(String)
	 */
	public UrlResource(String path) throws MalformedURLException {
		Assert.notNull(path, "Path must not be null");
		this.uri = null;
		this.url = new URL(path);
		this.cleanedUrl = getCleanedUrl(this.url, path);
	}

	/**
	 * Create a new {@code UrlResource} based on a URI specification.
	 * <p>The given parts will automatically get encoded if necessary.
	 * @param protocol the URL protocol to use (e.g. "jar" or "file" - without colon);
	 * also known as "scheme"
	 * @param location the location (e.g. the file path within that protocol);
	 * also known as "scheme-specific part"
	 * @throws MalformedURLException if the given URL specification is not valid
	 * @see java.net.URI#URI(String, String, String)
	 */
	public UrlResource(String protocol, String location) throws MalformedURLException  {
		this(protocol, location, null);
	}

	/**
	 * Create a new {@code UrlResource} based on a URI specification.
	 * <p>The given parts will automatically get encoded if necessary.
	 * @param protocol the URL protocol to use (e.g. "jar" or "file" - without colon);
	 * also known as "scheme"
	 * @param location the location (e.g. the file path within that protocol);
	 * also known as "scheme-specific part"
	 * @param fragment the fragment within that location (e.g. anchor on an HTML page,
	 * as following after a "#" separator)
	 * @throws MalformedURLException if the given URL specification is not valid
	 * @see java.net.URI#URI(String, String, String)
	 */
	public UrlResource(String protocol, String location, @Nullable String fragment) throws MalformedURLException  {
		try {
			this.uri = new URI(protocol, location, fragment);
			this.url = this.uri.toURL();
		}
		catch (URISyntaxException ex) {
			MalformedURLException exToThrow = new MalformedURLException(ex.getMessage());
			exToThrow.initCause(ex);
			throw exToThrow;
		}
	}


	/**
	 * Determine a cleaned URL for the given original URL.
	 * @param originalUrl the original URL
	 * @param originalPath the original URL path
	 * @return the cleaned URL (possibly the original URL as-is)
	 * @see org.springframework.util.StringUtils#cleanPath
	 */
	private static URL getCleanedUrl(URL originalUrl, String originalPath) {
		String cleanedPath = StringUtils.cleanPath(originalPath);
		if (!cleanedPath.equals(originalPath)) {
			try {
				return new URL(cleanedPath);
			}
			catch (MalformedURLException ex) {
				// Cleaned URL path cannot be converted to URL -> take original URL.
			}
		}
		return originalUrl;
	}

	/**
	 * Lazily determine a cleaned URL for the given original URL.
	 * @see #getCleanedUrl(URL, String)
	 */
	private URL getCleanedUrl() {
		URL cleanedUrl = this.cleanedUrl;
		if (cleanedUrl != null) {
			return cleanedUrl;
		}
		cleanedUrl = getCleanedUrl(this.url, (this.uri != null ? this.uri : this.url).toString());
		this.cleanedUrl = cleanedUrl;
		return cleanedUrl;
	}


	/**
	 * This implementation opens an InputStream for the given URL.
	 * <p>It sets the {@code useCaches} flag to {@code false},
	 * mainly to avoid jar file locking on Windows.
	 * @see java.net.URL#openConnection()
	 * @see java.net.URLConnection#setUseCaches(boolean)
	 * @see java.net.URLConnection#getInputStream()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		URLConnection con = this.url.openConnection();
		customizeConnection(con);
		try {
			return con.getInputStream();
		}
		catch (IOException ex) {
			// Close the HTTP connection (if applicable).
			if (con instanceof HttpURLConnection) {
				((HttpURLConnection) con).disconnect();
			}
			throw ex;
		}
	}

	/**
	 * This implementation returns the underlying URL reference.
	 */
	@Override
	public URL getURL() {
		return this.url;
	}

	/**
	 * This implementation returns the underlying URI directly,
	 * if possible.
	 */
	@Override
	public URI getURI() throws IOException {
		if (this.uri != null) {
			return this.uri;
		}
		else {
			return super.getURI();
		}
	}

	@Override
	public boolean isFile() {
		if (this.uri != null) {
			return super.isFile(this.uri);
		}
		else {
			return super.isFile();
		}
	}

	/**
	 * This implementation returns a File reference for the underlying URL/URI,
	 * provided that it refers to a file in the file system.
	 * @see org.springframework.util.ResourceUtils#getFile(java.net.URL, String)
	 */
	@Override
	public File getFile() throws IOException {
		if (this.uri != null) {
			return super.getFile(this.uri);
		}
		else {
			return super.getFile();
		}
	}

	/**
	 * This implementation creates a {@code UrlResource}, delegating to
	 * {@link #createRelativeURL(String)} for adapting the relative path.
	 * @see #createRelativeURL(String)
	 */
	@Override
	public Resource createRelative(String relativePath) throws MalformedURLException {
		return new UrlResource(createRelativeURL(relativePath));
	}

	/**
	 * This delegate creates a {@code java.net.URL}, applying the given path
	 * relative to the path of the underlying URL of this resource descriptor.
	 * A leading slash will get dropped; a "#" symbol will get encoded.
	 * @since 5.2
	 * @see #createRelative(String)
	 * @see java.net.URL#URL(java.net.URL, String)
	 */
	protected URL createRelativeURL(String relativePath) throws MalformedURLException {
		if (relativePath.startsWith("/")) {
			relativePath = relativePath.substring(1);
		}
		// # can appear in filenames, java.net.URL should not treat it as a fragment
		relativePath = StringUtils.replace(relativePath, "#", "%23");
		// Use the URL constructor for applying the relative path as a URL spec
		return new URL(this.url, relativePath);
	}

	/**
	 * This implementation returns the name of the file that this URL refers to.
	 * @see java.net.URL#getPath()
	 */
	@Override
	public String getFilename() {
		return StringUtils.getFilename(getCleanedUrl().getPath());
	}

	/**
	 * This implementation returns a description that includes the URL.
	 */
	@Override
	public String getDescription() {
		return "URL [" + this.url + "]";
	}


	/**
	 * This implementation compares the underlying URL references.
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof UrlResource &&
				getCleanedUrl().equals(((UrlResource) other).getCleanedUrl())));
	}

	/**
	 * This implementation returns the hash code of the underlying URL reference.
	 */
	@Override
	public int hashCode() {
		return getCleanedUrl().hashCode();
	}

}
