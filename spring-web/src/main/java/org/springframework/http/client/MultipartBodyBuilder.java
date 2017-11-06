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

package org.springframework.http.client;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * A mutable builder for multipart form bodies. For example:
 * <pre class="code">
 *
 * MultipartBodyBuilder builder = new MultipartBodyBuilder();
 * builder.part("form field", "form value");
 *
 * Resource image = new ClassPathResource("image.jpg");
 * builder.part("image", image).header("Baz", "Qux");
 *
 * MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();
 * // use multipartBody with RestTemplate or WebClient
 * </pre>

 * @author Arjen Poutsma
 * @since 5.0.2
 * @see <a href="https://tools.ietf.org/html/rfc7578">RFC 7578</a>
 */
public final class MultipartBodyBuilder {

	private final LinkedMultiValueMap<String, DefaultPartBuilder> parts = new LinkedMultiValueMap<>();


	/**
	 * Creates a new, empty instance of the {@code MultipartBodyBuilder}.
	 */
	public MultipartBodyBuilder() {
	}


	/**
	 * Builds the multipart body.
	 * @return the built body
	 */
	public MultiValueMap<String, HttpEntity<?>> build() {
		MultiValueMap<String, HttpEntity<?>> result = new LinkedMultiValueMap<>(this.parts.size());
		for (Map.Entry<String, List<DefaultPartBuilder>> entry : this.parts.entrySet()) {
			for (DefaultPartBuilder builder : entry.getValue()) {
				HttpEntity<?> entity = builder.build();
				result.add(entry.getKey(), entity);
			}
		}
		return result;
	}

	/**
	 * Adds a part to this builder, allowing for further header customization with the returned
	 * {@link PartBuilder}.
	 * @param name the name of the part to add (may not be empty)
	 * @param part the part to add
	 * @return a builder that allows for further header customization
	 */
	public PartBuilder part(String name, Object part) {
		return part(name, part, null);
	}

	/**
	 * Adds a part to this builder, allowing for further header customization with the returned
	 * {@link PartBuilder}.
	 * @param name the name of the part to add (may not be empty)
	 * @param part the part to add
	 * @param contentType the {@code Content-Type} header for the part (may be {@code null})
	 * @return a builder that allows for further header customization
	 */
	public PartBuilder part(String name, Object part, @Nullable MediaType contentType) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(part, "'part' must not be null");

		Object partBody;
		HttpHeaders partHeaders = new HttpHeaders();

		if (part instanceof HttpEntity) {
			HttpEntity<?> other = (HttpEntity<?>) part;
			partBody = other.getBody();
			partHeaders.addAll(other.getHeaders());
		}
		else {
			partBody = part;
		}

		if (contentType != null) {
			partHeaders.setContentType(contentType);
		}
		DefaultPartBuilder builder = new DefaultPartBuilder(partBody, partHeaders);
		this.parts.add(name, builder);
		return builder;
	}


	/**
	 * Builder interface that allows for customization of part headers.
	 */
	public interface PartBuilder {

		/**
		 * Add the given part-specific header values under the given name.
		 * @param headerName the part header name
		 * @param headerValues the part header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		PartBuilder header(String headerName, String... headerValues);
	}


	private static class DefaultPartBuilder implements PartBuilder {

		@Nullable
		private final Object body;

		private final HttpHeaders headers;


		public DefaultPartBuilder(@Nullable Object body, HttpHeaders headers) {
			this.body = body;
			this.headers = headers;
		}

		@Override
		public PartBuilder header(String headerName, String... headerValues) {
			this.headers.addAll(headerName, Arrays.asList(headerValues));
			return this;
		}

		public HttpEntity<?> build() {
			return new HttpEntity<>(this.body, this.headers);
		}
	}

}
