/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.io.support;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Holder that combines a {@link org.springframework.core.io.Resource}
 * with a specific encoding to be used for reading from the resource.
 *
 * <p>Used as argument for operations that support to read content with
 * a specific encoding (usually through a {@code java.io.Reader}.
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 * @see java.io.Reader
 */
public class EncodedResource {

	private final Resource resource;

	private final String encoding;


	/**
	 * Create a new EncodedResource for the given Resource,
	 * not specifying a specific encoding.
	 * @param resource the Resource to hold
	 */
	public EncodedResource(Resource resource) {
		this(resource, null);
	}

	/**
	 * Create a new EncodedResource for the given Resource,
	 * using the specified encoding.
	 * @param resource the Resource to hold
	 * @param encoding the encoding to use for reading from the resource
	 */
	public EncodedResource(Resource resource, String encoding) {
		Assert.notNull(resource, "Resource must not be null");
		this.resource = resource;
		this.encoding = encoding;
	}


	/**
	 * Return the Resource held.
	 */
	public final Resource getResource() {
		return this.resource;
	}

	/**
	 * Return the encoding to use for reading from the resource,
	 * or {@code null} if none specified.
	 */
	public final String getEncoding() {
		return this.encoding;
	}

	/**
	 * Open a {@code java.io.Reader} for the specified resource,
	 * using the specified encoding (if any).
	 * @throws IOException if opening the Reader failed
	 */
	public Reader getReader() throws IOException {
		if (this.encoding != null) {
			return new InputStreamReader(this.resource.getInputStream(), this.encoding);
		}
		else {
			return new InputStreamReader(this.resource.getInputStream());
		}
	}


	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof EncodedResource) {
			EncodedResource otherRes = (EncodedResource) obj;
			return (this.resource.equals(otherRes.resource) &&
					ObjectUtils.nullSafeEquals(this.encoding, otherRes.encoding));
		}
		return false;
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
