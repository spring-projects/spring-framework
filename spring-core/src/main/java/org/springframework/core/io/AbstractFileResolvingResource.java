/*
 * Copyright 2002-2025 the original author or authors.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.util.ResourceUtils;

/**
 * Abstract base class for resources which resolve URLs into File references,
 * such as {@link UrlResource} or {@link ClassPathResource}.
 *
 * <p>Detects the "file" protocol as well as the JBoss "vfs" protocol in URLs,
 * resolving file system references accordingly.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class AbstractFileResolvingResource extends AbstractResource {

	@Override
	public boolean exists() {
		try {
			URL url = getURL();
			if (ResourceUtils.isFileURL(url)) {
				// Proceed with file system resolution
				return getFile().exists();
			}
			else {
				// Try a URL connection content-length header
				URLConnection con = url.openConnection();
				customizeConnection(con);

				HttpURLConnection httpCon = (con instanceof HttpURLConnection huc ? huc : null);
				if (httpCon != null) {
					httpCon.setRequestMethod("HEAD");
					int code = httpCon.getResponseCode();
					if (code == HttpURLConnection.HTTP_OK) {
						return true;
					}
					else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
						return false;
					}
					else if (code == HttpURLConnection.HTTP_BAD_METHOD) {
						con = url.openConnection();
						customizeConnection(con);
						if (con instanceof HttpURLConnection newHttpCon) {
							code = newHttpCon.getResponseCode();
							if (code == HttpURLConnection.HTTP_OK) {
								return true;
							}
							else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
								return false;
							}
							httpCon = newHttpCon;
						}
					}
				}

				if (con instanceof JarURLConnection jarCon) {
					// For JarURLConnection, do not check content-length but rather the
					// existence of the entry (or the jar root in case of no entryName).
					// getJarFile() called for enforced presence check of the jar file,
					// throwing a NoSuchFileException otherwise (turned to false below).
					JarFile jarFile = jarCon.getJarFile();
					try {
						return (jarCon.getEntryName() == null || jarCon.getJarEntry() != null);
					}
					finally {
						if (!jarCon.getUseCaches()) {
							jarFile.close();
						}
					}
				}
				else if (con.getContentLengthLong() > 0) {
					return true;
				}

				if (httpCon != null) {
					// No HTTP OK status, and no content-length header: give up
					httpCon.disconnect();
					return false;
				}
				else {
					// Fall back to stream existence: can we open the stream?
					getInputStream().close();
					return true;
				}
			}
		}
		catch (IOException ex) {
			return false;
		}
	}

	@Override
	public boolean isReadable() {
		try {
			return checkReadable(getURL());
		}
		catch (IOException ex) {
			return false;
		}
	}

	boolean checkReadable(URL url) {
		try {
			if (ResourceUtils.isFileURL(url)) {
				// Proceed with file system resolution
				File file = getFile();
				return (file.canRead() && !file.isDirectory());
			}
			else {
				// Try InputStream resolution for jar resources
				URLConnection con = url.openConnection();
				customizeConnection(con);
				if (con instanceof HttpURLConnection httpCon) {
					httpCon.setRequestMethod("HEAD");
					int code = httpCon.getResponseCode();
					if (code == HttpURLConnection.HTTP_BAD_METHOD) {
						con = url.openConnection();
						customizeConnection(con);
						if (!(con instanceof HttpURLConnection newHttpCon)) {
							return false;
						}
						code = newHttpCon.getResponseCode();
						httpCon = newHttpCon;
					}
					if (code != HttpURLConnection.HTTP_OK) {
						httpCon.disconnect();
						return false;
					}
				}
				else if (con instanceof JarURLConnection jarCon) {
					JarEntry jarEntry = jarCon.getJarEntry();
					if (jarEntry == null) {
						return false;
					}
					else {
						return !jarEntry.isDirectory();
					}
				}
				long contentLength = con.getContentLengthLong();
				if (contentLength > 0) {
					return true;
				}
				else if (contentLength == 0) {
					// Empty file or directory -> not considered readable...
					return false;
				}
				else {
					// Fall back to stream existence: can we open the stream?
					getInputStream().close();
					return true;
				}
			}
		}
		catch (IOException ex) {
			return false;
		}
	}

	@Override
	public boolean isFile() {
		try {
			URL url = getURL();
			if (url.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				return VfsResourceDelegate.getResource(url).isFile();
			}
			return ResourceUtils.URL_PROTOCOL_FILE.equals(url.getProtocol());
		}
		catch (IOException ex) {
			return false;
		}
	}

	/**
	 * This implementation returns a File reference for the underlying class path
	 * resource, provided that it refers to a file in the file system.
	 * @see org.springframework.util.ResourceUtils#getFile(java.net.URL, String)
	 */
	@Override
	public File getFile() throws IOException {
		URL url = getURL();
		if (url.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
			return VfsResourceDelegate.getResource(url).getFile();
		}
		return ResourceUtils.getFile(url, getDescription());
	}

	/**
	 * This implementation determines the underlying File
	 * (or jar file, in case of a resource in a jar/zip).
	 */
	@Override
	protected File getFileForLastModifiedCheck() throws IOException {
		URL url = getURL();
		if (ResourceUtils.isJarURL(url)) {
			URL actualUrl = ResourceUtils.extractArchiveURL(url);
			if (actualUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				return VfsResourceDelegate.getResource(actualUrl).getFile();
			}
			return ResourceUtils.getFile(actualUrl, "Jar URL");
		}
		else {
			return getFile();
		}
	}

	/**
	 * Determine whether the given {@link URI} represents a file in a file system.
	 * @since 5.0
	 * @see #getFile(URI)
	 */
	protected boolean isFile(URI uri) {
		try {
			if (uri.getScheme().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				return VfsResourceDelegate.getResource(uri).isFile();
			}
			return ResourceUtils.URL_PROTOCOL_FILE.equals(uri.getScheme());
		}
		catch (IOException ex) {
			return false;
		}
	}

	/**
	 * This implementation returns a File reference for the given URI-identified
	 * resource, provided that it refers to a file in the file system.
	 * @see org.springframework.util.ResourceUtils#getFile(java.net.URI, String)
	 */
	protected File getFile(URI uri) throws IOException {
		if (uri.getScheme().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
			return VfsResourceDelegate.getResource(uri).getFile();
		}
		return ResourceUtils.getFile(uri, getDescription());
	}

	/**
	 * This implementation returns a FileChannel for the given URI-identified
	 * resource, provided that it refers to a file in the file system.
	 * @since 5.0
	 * @see #getFile()
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		try {
			// Try file system channel
			return FileChannel.open(getFile().toPath(), StandardOpenOption.READ);
		}
		catch (FileNotFoundException | NoSuchFileException ex) {
			// Fall back to InputStream adaptation in superclass
			return super.readableChannel();
		}
	}

	@Override
	public long contentLength() throws IOException {
		URL url = getURL();
		if (ResourceUtils.isFileURL(url)) {
			// Proceed with file system resolution
			File file = getFile();
			long length = file.length();
			if (length == 0L && !file.exists()) {
				throw new FileNotFoundException(getDescription() +
						" cannot be resolved in the file system for checking its content length");
			}
			return length;
		}
		else {
			// Try a URL connection content-length header
			URLConnection con = url.openConnection();
			customizeConnection(con);
			if (con instanceof HttpURLConnection httpCon) {
				httpCon.setRequestMethod("HEAD");
			}
			long length = con.getContentLengthLong();
			if (length <= 0 && con instanceof HttpURLConnection httpCon &&
					httpCon.getResponseCode() == HttpURLConnection.HTTP_BAD_METHOD) {
				con = url.openConnection();
				customizeConnection(con);
				length = con.getContentLengthLong();
			}
			return length;
		}
	}

	@Override
	public long lastModified() throws IOException {
		URL url = getURL();
		boolean fileCheck = false;
		if (ResourceUtils.isFileURL(url) || ResourceUtils.isJarURL(url)) {
			// Proceed with file system resolution
			fileCheck = true;
			try {
				File fileToCheck = getFileForLastModifiedCheck();
				long lastModified = fileToCheck.lastModified();
				if (lastModified > 0L || fileToCheck.exists()) {
					return lastModified;
				}
			}
			catch (FileNotFoundException ex) {
				// Defensively fall back to URL connection check instead
			}
		}
		// Try a URL connection last-modified header
		URLConnection con = url.openConnection();
		customizeConnection(con);
		if (con instanceof HttpURLConnection httpCon) {
			httpCon.setRequestMethod("HEAD");
		}
		long lastModified = con.getLastModified();
		if (lastModified == 0) {
			if (con instanceof HttpURLConnection httpCon &&
					httpCon.getResponseCode() == HttpURLConnection.HTTP_BAD_METHOD) {
				con = url.openConnection();
				customizeConnection(con);
				lastModified = con.getLastModified();
			}
			if (fileCheck && con.getContentLengthLong() <= 0) {
				throw new FileNotFoundException(getDescription() +
						" cannot be resolved in the file system for checking its last-modified timestamp");
			}
		}
		return lastModified;
	}

	/**
	 * Customize the given {@link URLConnection} before fetching the resource.
	 * <p>Calls {@link ResourceUtils#useCachesIfNecessary(URLConnection)} and
	 * delegates to {@link #customizeConnection(HttpURLConnection)} if possible.
	 * Can be overridden in subclasses.
	 * @param con the URLConnection to customize
	 * @throws IOException if thrown from URLConnection methods
	 */
	protected void customizeConnection(URLConnection con) throws IOException {
		ResourceUtils.useCachesIfNecessary(con);
		if (con instanceof HttpURLConnection httpCon) {
			customizeConnection(httpCon);
		}
	}

	/**
	 * Customize the given {@link HttpURLConnection} before fetching the resource.
	 * <p>Can be overridden in subclasses for configuring request headers and timeouts.
	 * @param con the HttpURLConnection to customize
	 * @throws IOException if thrown from HttpURLConnection methods
	 */
	protected void customizeConnection(HttpURLConnection con) throws IOException {
	}


	/**
	 * Inner delegate class, avoiding a hard JBoss VFS API dependency at runtime.
	 */
	private static class VfsResourceDelegate {

		public static Resource getResource(URL url) throws IOException {
			return new VfsResource(VfsUtils.getRoot(url));
		}

		public static Resource getResource(URI uri) throws IOException {
			return new VfsResource(VfsUtils.getRoot(uri));
		}
	}

}
