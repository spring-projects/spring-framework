/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.io.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Holder that combines a {@link Resource} descriptor with a specific encoding
 * or {@code Charset} to be used for reading from the resource.
 *
 * <p>Used as an argument for operations that support reading content with
 * a specific encoding, typically via a {@code java.io.Reader}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Arjen Poutsma
 * @since 1.2.6
 * @see Resource#getInputStream()
 * @see java.io.Reader
 * @see java.nio.charset.Charset
 */
public class EncodedResource implements InputStreamSource {

	private final Resource resource;

	private final @Nullable String encoding;

	private final @Nullable Charset charset;


	/**
	 * Create a new {@code EncodedResource} for the given {@code Resource},
	 * not specifying an explicit encoding or {@code Charset}.
	 * @param resource the {@code Resource} to hold (never {@code null})
	 */
	public EncodedResource(Resource resource) {
		this(resource, null, null);
	}

	/**
	 * Create a new {@code EncodedResource} for the given {@code Resource},
	 * using the specified {@code encoding}.
	 * @param resource the {@code Resource} to hold (never {@code null})
	 * @param encoding the encoding to use for reading from the resource
	 */
	public EncodedResource(Resource resource, @Nullable String encoding) {
		this(resource, encoding, null);
	}

	/**
	 * Create a new {@code EncodedResource} for the given {@code Resource},
	 * using the specified {@code Charset}.
	 * @param resource the {@code Resource} to hold (never {@code null})
	 * @param charset the {@code Charset} to use for reading from the resource
	 */
	public EncodedResource(Resource resource, @Nullable Charset charset) {
		this(resource, null, charset);
	}

	private EncodedResource(Resource resource, @Nullable String encoding, @Nullable Charset charset) {
		super();
		Assert.notNull(resource, "Resource must not be null");
		this.resource = resource;
		this.encoding = encoding;
		this.charset = charset;
	}


	/**
	 * Return the {@code Resource} held by this {@code EncodedResource}.
	 */
	public final Resource getResource() {
		return this.resource;
	}

	/**
	 * Return the encoding to use for reading from the {@linkplain #getResource() resource},
	 * or {@code null} if none specified.
	 */
	public final @Nullable String getEncoding() {
		return this.encoding;
	}

	/**
	 * Return the {@code Charset} to use for reading from the {@linkplain #getResource() resource},
	 * or {@code null} if none specified.
	 */
	public final @Nullable Charset getCharset() {
		return this.charset;
	}

	/**
	 * Determine whether a {@link Reader} is required as opposed to an {@link InputStream},
	 * i.e. whether an {@linkplain #getEncoding() encoding} or a {@link #getCharset() Charset}
	 * has been specified.
	 * @see #getReader()
	 * @see #getInputStream()
	 */
	public boolean requiresReader() {
		return (this.encoding != null || this.charset != null);
	}

	/**
	 * Open a {@code java.io.Reader} for the specified resource, using the specified
	 * {@link #getCharset() Charset} or {@linkplain #getEncoding() encoding}
	 * (if any).
	 * @throws IOException if opening the Reader failed
	 * @see #requiresReader()
	 * @see #getInputStream()
	 */
	public Reader getReader() throws IOException {
		if (this.charset != null) {
			return new InputStreamReader(this.resource.getInputStream(), this.charset);
		}
		else if (this.encoding != null) {
			return new InputStreamReader(this.resource.getInputStream(), this.encoding);
		}
		else {
			return new InputStreamReader(this.resource.getInputStream());
		}
	}

	/**
	 * Open an {@code InputStream} for the specified resource, ignoring any specified
	 * {@link #getCharset() Charset} or {@linkplain #getEncoding() encoding}.
	 * @throws IOException if opening the InputStream failed
	 * @see #requiresReader()
	 * @see #getReader()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return this.resource.getInputStream();
	}

	/**
	 * Returns the contents of the specified resource as a string, using the specified
	 * {@link #getCharset() Charset} or {@linkplain #getEncoding() encoding} (if any).
	 * @throws IOException if opening the resource failed
	 * @since 6.0.5
	 * @see Resource#getContentAsString(Charset)
	 */
	public String getContentAsString() throws IOException {
		Charset charset;
		if (this.charset != null) {
			charset = this.charset;
		}
		else if (this.encoding != null) {
			charset = Charset.forName(this.encoding);
		}
		else {
			charset = Charset.defaultCharset();
		}
		return this.resource.getContentAsString(charset);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof EncodedResource that &&
				this.resource.equals(that.resource) &&
				ObjectUtils.nullSafeEquals(this.charset, that.charset) &&
				ObjectUtils.nullSafeEquals(this.encoding, that.encoding)));
	}

	@Override
	public int hashCode() {
		return this.resource.hashCode();
	}

	@Override
	public String toString() {
		return this.resource.toString();
	}

}
