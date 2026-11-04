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

package org.springframework.web.servlet.resource;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * An extension of {@link ByteArrayResource} that a {@link ResourceTransformer}
 * can use to represent transformed content while preserving the original
 * resource's filename and last-modified timestamp.
 *
 * <p>If the original resource provides an ETag, generate a weak ETag from the
 * transformed content. Other HTTP response headers are not inherited.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class TransformedResource extends ByteArrayResource implements HttpResource {

	private final @Nullable String filename;

	private final long lastModified;

	private final boolean generateEtag;


	public TransformedResource(Resource original, byte[] transformedContent) {
		super(transformedContent);
		this.filename = original.getFilename();
		try {
			this.lastModified = original.lastModified();
		}
		catch (IOException ex) {
			// should never happen
			throw new IllegalArgumentException(ex);
		}
		this.generateEtag = (original instanceof HttpResource httpResource &&
				StringUtils.hasLength(httpResource.getResponseHeaders().getETag()));
	}


	@Override
	public @Nullable String getFilename() {
		return this.filename;
	}

	@Override
	public long lastModified() throws IOException {
		return this.lastModified;
	}

	/**
	 * Return independent, mutable HTTP response headers for the transformed resource.
	 * <p>If the original resource provides an ETag, generate a weak ETag from the
	 * current transformed content. Other original response headers are not inherited.
	 * @since 7.1
	 */
	@Override
	public HttpHeaders getResponseHeaders() {
		HttpHeaders headers = new HttpHeaders();
		if (this.generateEtag) {
			headers.setETag("W/\"" + DigestUtils.md5DigestAsHex(getByteArray()) + "\"");
		}
		return headers;
	}

}
