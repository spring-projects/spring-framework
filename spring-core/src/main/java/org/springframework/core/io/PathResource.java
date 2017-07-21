/*
 * Copyright 2002-2017 the original author or authors.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.springframework.util.Assert;

/**
 * {@link Resource} implementation for {@code java.nio.file.Path} handles.
 * Supports resolution as File, and also as URL.
 * Implements the extended {@link WritableResource} interface.
 *
 * @author Philippe Marschall
 * @author Juergen Hoeller
 * @since 4.0
 * @see FileSystemResource
 * @see java.nio.file.Path
 * @see java.nio.file.Files
 */
public class PathResource extends AbstractResource implements WritableResource {

	private final Path path;


	/**
	 * Create a new PathResource from a Path handle.
	 * <p>Note: Unlike {@link FileSystemResource}, when building relative resources
	 * via {@link #createRelative}, the relative path will be built <i>underneath</i>
	 * the given root: e.g. Paths.get("C:/dir1/"), relative path "dir2" -> "C:/dir1/dir2"!
	 * @param path a Path handle
	 */
	public PathResource(Path path) {
		Assert.notNull(path, "Path must not be null");
		this.path = path.normalize();
	}

	/**
	 * Create a new PathResource from a Path handle.
	 * <p>Note: Unlike {@link FileSystemResource}, when building relative resources
	 * via {@link #createRelative}, the relative path will be built <i>underneath</i>
	 * the given root: e.g. Paths.get("C:/dir1/"), relative path "dir2" -> "C:/dir1/dir2"!
	 * @param path a path
	 * @see java.nio.file.Paths#get(String, String...)
	 */
	public PathResource(String path) {
		Assert.notNull(path, "Path must not be null");
		this.path = Paths.get(path).normalize();
	}

	/**
	 * Create a new PathResource from a Path handle.
	 * <p>Note: Unlike {@link FileSystemResource}, when building relative resources
	 * via {@link #createRelative}, the relative path will be built <i>underneath</i>
	 * the given root: e.g. Paths.get("C:/dir1/"), relative path "dir2" -> "C:/dir1/dir2"!
	 * @see java.nio.file.Paths#get(URI)
	 * @param uri a path URI
	 */
	public PathResource(URI uri) {
		Assert.notNull(uri, "URI must not be null");
		this.path = Paths.get(uri).normalize();
	}


	/**
	 * Return the file path for this resource.
	 */
	public final String getPath() {
		return this.path.toString();
	}

	/**
	 * This implementation returns whether the underlying file exists.
	 * @see org.springframework.core.io.PathResource#exists()
	 */
	@Override
	public boolean exists() {
		return Files.exists(this.path);
	}

	/**
	 * This implementation checks whether the underlying file is marked as readable
	 * (and corresponds to an actual file with content, not to a directory).
	 * @see java.nio.file.Files#isReadable(Path)
	 * @see java.nio.file.Files#isDirectory(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public boolean isReadable() {
		return (Files.isReadable(this.path) && !Files.isDirectory(this.path));
	}

	/**
	 * This implementation opens a InputStream for the underlying file.
	 * @see java.nio.file.spi.FileSystemProvider#newInputStream(Path, OpenOption...)
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		if (!exists()) {
			throw new FileNotFoundException(getPath() + " (no such file or directory)");
		}
		if (Files.isDirectory(this.path)) {
			throw new FileNotFoundException(getPath() + " (is a directory)");
		}
		return Files.newInputStream(this.path);
	}

	/**
	 * This implementation checks whether the underlying file is marked as writable
	 * (and corresponds to an actual file with content, not to a directory).
	 * @see java.nio.file.Files#isWritable(Path)
	 * @see java.nio.file.Files#isDirectory(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public boolean isWritable() {
		return (Files.isWritable(this.path) && !Files.isDirectory(this.path));
	}

	/**
	 * This implementation opens a OutputStream for the underlying file.
	 * @see java.nio.file.spi.FileSystemProvider#newOutputStream(Path, OpenOption...)
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		if (Files.isDirectory(this.path)) {
			throw new FileNotFoundException(getPath() + " (is a directory)");
		}
		return Files.newOutputStream(this.path);
	}

	/**
	 * This implementation returns a URL for the underlying file.
	 * @see java.nio.file.Path#toUri()
	 * @see java.net.URI#toURL()
	 */
	@Override
	public URL getURL() throws IOException {
		return this.path.toUri().toURL();
	}

	/**
	 * This implementation returns a URI for the underlying file.
	 * @see java.nio.file.Path#toUri()
	 */
	@Override
	public URI getURI() throws IOException {
		return this.path.toUri();
	}

	/**
	 * This implementation always indicates a file.
	 */
	@Override
	public boolean isFile() {
		return true;
	}

	/**
	 * This implementation returns the underlying File reference.
	 */
	@Override
	public File getFile() throws IOException {
		try {
			return this.path.toFile();
		}
		catch (UnsupportedOperationException ex) {
			// Only paths on the default file system can be converted to a File:
			// Do exception translation for cases where conversion is not possible.
			throw new FileNotFoundException(this.path + " cannot be resolved to absolute file path");
		}
	}

	/**
	 * This implementation opens a Channel for the underlying file.
	 * @see Files#newByteChannel(Path, OpenOption...)
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		return Files.newByteChannel(this.path, StandardOpenOption.READ);
	}

	/**
	 * This implementation opens a Channel for the underlying file.
	 * @see Files#newByteChannel(Path, OpenOption...)
	 */
	@Override
	public WritableByteChannel writableChannel() throws IOException {
		return Files.newByteChannel(this.path, StandardOpenOption.WRITE);
	}

	/**
	 * This implementation returns the underlying File's length.
	 */
	@Override
	public long contentLength() throws IOException {
		return Files.size(this.path);
	}

	/**
	 * This implementation returns the underlying File's timestamp.
	 * @see java.nio.file.Files#getLastModifiedTime(Path, java.nio.file.LinkOption...)
	 */
	@Override
	public long lastModified() throws IOException {
		// We can not use the superclass method since it uses conversion to a File and
		// only a Path on the default file system can be converted to a File...
		return Files.getLastModifiedTime(this.path).toMillis();
	}

	/**
	 * This implementation creates a PathResource, applying the given path
	 * relative to the path of the underlying file of this resource descriptor.
	 * @see java.nio.file.Path#resolve(String)
	 */
	@Override
	public Resource createRelative(String relativePath) throws IOException {
		return new PathResource(this.path.resolve(relativePath));
	}

	/**
	 * This implementation returns the name of the file.
	 * @see java.nio.file.Path#getFileName()
	 */
	@Override
	public String getFilename() {
		return this.path.getFileName().toString();
	}

	@Override
	public String getDescription() {
		return "path [" + this.path.toAbsolutePath() + "]";
	}


	/**
	 * This implementation compares the underlying Path references.
	 */
	@Override
	public boolean equals(Object obj) {
		return (this == obj ||
			(obj instanceof PathResource && this.path.equals(((PathResource) obj).path)));
	}

	/**
	 * This implementation returns the hash code of the underlying Path reference.
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
