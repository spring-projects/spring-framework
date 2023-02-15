/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.reactive.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolver that delegates to the chain, and if a resource is found, it then
 * attempts to find an encoded (e.g. gzip, brotli) variant that is acceptable
 * based on the "Accept-Encoding" request header.
 *
 * <p>The list of supported {@link #setContentCodings(List) contentCodings} can
 * be configured, in order of preference, and each coding must be associated
 * with {@link #setExtensions(Map) extensions}.
 *
 * <p>Note that this resolver must be ordered ahead of a
 * {@link VersionResourceResolver} with a content-based, version strategy to
 * ensure the version calculation is not impacted by the encoding.
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public class EncodedResourceResolver extends AbstractResourceResolver {

	/**
	 * The default content codings.
	 */
	public static final List<String> DEFAULT_CODINGS = Arrays.asList("br", "gzip");


	private final List<String> contentCodings = new ArrayList<>(DEFAULT_CODINGS);

	private final Map<String, String> extensions = new LinkedHashMap<>();


	public EncodedResourceResolver() {
		this.extensions.put("gzip", ".gz");
		this.extensions.put("br", ".br");
	}


	/**
	 * Configure the supported content codings in order of preference. The first
	 * coding that is present in the {@literal "Accept-Encoding"} header for a
	 * given request, and that has a file present with the associated extension,
	 * is used.
	 * <p><strong>Note:</strong> Each coding must be associated with a file
	 * extension via {@link #registerExtension} or {@link #setExtensions}. Also
	 * customizations to the list of codings here should be matched by
	 * customizations to the same list in {@link CachingResourceResolver} to
	 * ensure encoded variants of a resource are cached under separate keys.
	 * <p>By default this property is set to {@literal ["br", "gzip"]}.
	 * @param codings one or more supported content codings
	 */
	public void setContentCodings(List<String> codings) {
		Assert.notEmpty(codings, "At least one content coding expected");
		this.contentCodings.clear();
		this.contentCodings.addAll(codings);
	}

	/**
	 * Return a read-only list with the supported content codings.
	 */
	public List<String> getContentCodings() {
		return Collections.unmodifiableList(this.contentCodings);
	}

	/**
	 * Configure mappings from content codings to file extensions. A dot "."
	 * will be prepended in front of the extension value if not present.
	 * <p>By default this is configured with {@literal ["br" -> ".br"]} and
	 * {@literal ["gzip" -> ".gz"]}.
	 * @param extensions the extensions to use.
	 * @see #registerExtension(String, String)
	 */
	public void setExtensions(Map<String, String> extensions) {
		extensions.forEach(this::registerExtension);
	}

	/**
	 * Return a read-only map with coding-to-extension mappings.
	 */
	public Map<String, String> getExtensions() {
		return Collections.unmodifiableMap(this.extensions);
	}

	/**
	 * Java config friendly alternative to {@link #setExtensions(Map)}.
	 * @param coding the content coding
	 * @param extension the associated file extension
	 */
	public void registerExtension(String coding, String extension) {
		this.extensions.put(coding, (extension.startsWith(".") ? extension : "." + extension));
	}


	@Override
	protected Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
			String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveResource(exchange, requestPath, locations).map(resource -> {

			if (exchange == null) {
				return resource;
			}

			String acceptEncoding = getAcceptEncoding(exchange);
			if (acceptEncoding == null) {
				return resource;
			}

			for (String coding : this.contentCodings) {
				if (acceptEncoding.contains(coding)) {
					try {
						String extension = getExtension(coding);
						Resource encoded = new EncodedResource(resource, coding, extension);
						if (encoded.exists()) {
							return encoded;
						}
					}
					catch (IOException ex) {
						logger.trace(exchange.getLogPrefix() +
								"No " + coding + " resource for [" + resource.getFilename() + "]", ex);
					}
				}
			}

			return resource;
		});
	}

	@Nullable
	private String getAcceptEncoding(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		String header = request.getHeaders().getFirst(HttpHeaders.ACCEPT_ENCODING);
		return (header != null ? header.toLowerCase() : null);
	}

	private String getExtension(String coding) {
		String extension = this.extensions.get(coding);
		if (extension == null) {
			throw new IllegalStateException("No file extension associated with content coding " + coding);
		}
		return extension;
	}

	@Override
	protected Mono<String> resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveUrlPath(resourceUrlPath, locations);
	}


	/**
	 * An encoded {@link HttpResource}.
	 */
	static final class EncodedResource extends AbstractResource implements HttpResource {

		private final Resource original;

		private final String coding;

		private final Resource encoded;

		EncodedResource(Resource original, String coding, String extension) throws IOException {
			this.original = original;
			this.coding = coding;
			this.encoded = original.createRelative(original.getFilename() + extension);
		}

		@Override
		public boolean exists() {
			return this.encoded.exists();
		}

		@Override
		public boolean isReadable() {
			return this.encoded.isReadable();
		}

		@Override
		public boolean isOpen() {
			return this.encoded.isOpen();
		}

		@Override
		public boolean isFile() {
			return this.encoded.isFile();
		}

		@Override
		public URL getURL() throws IOException {
			return this.encoded.getURL();
		}

		@Override
		public URI getURI() throws IOException {
			return this.encoded.getURI();
		}

		@Override
		public File getFile() throws IOException {
			return this.encoded.getFile();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return this.encoded.getInputStream();
		}

		@Override
		public ReadableByteChannel readableChannel() throws IOException {
			return this.encoded.readableChannel();
		}

		@Override
		public byte[] getContentAsByteArray() throws IOException {
			return this.encoded.getContentAsByteArray();
		}

		@Override
		public String getContentAsString(Charset charset) throws IOException {
			return this.encoded.getContentAsString(charset);
		}

		@Override
		public long contentLength() throws IOException {
			return this.encoded.contentLength();
		}

		@Override
		public long lastModified() throws IOException {
			return this.encoded.lastModified();
		}

		@Override
		public Resource createRelative(String relativePath) throws IOException {
			return this.encoded.createRelative(relativePath);
		}

		@Override
		@Nullable
		public String getFilename() {
			return this.original.getFilename();
		}

		@Override
		public String getDescription() {
			return this.encoded.getDescription();
		}

		@Override
		public HttpHeaders getResponseHeaders() {
			HttpHeaders headers;
			if (this.original instanceof HttpResource httpResource) {
				headers = httpResource.getResponseHeaders();
			}
			else {
				headers = new HttpHeaders();
			}
			headers.add(HttpHeaders.CONTENT_ENCODING, this.coding);
			headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
			return headers;
		}
	}

}
