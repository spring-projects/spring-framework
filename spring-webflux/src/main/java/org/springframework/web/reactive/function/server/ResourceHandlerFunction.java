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

package org.springframework.web.reactive.function.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.EnumSet;
import java.util.Set;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;

/**
 * Resource-based implementation of {@link HandlerFunction}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class ResourceHandlerFunction implements HandlerFunction<ServerResponse> {


	private static final Set<HttpMethod> SUPPORTED_METHODS =
			EnumSet.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);


	private final Resource resource;

	public ResourceHandlerFunction(Resource resource) {
		this.resource = resource;
	}

	@Override
	public Mono<ServerResponse> handle(ServerRequest request) {
		switch (request.method()) {
			case GET:
				return ServerResponse.ok()
						.body(BodyInserters.fromResource(this.resource));
			case HEAD:
				Resource headResource = new HeadMethodResource(this.resource);
				return ServerResponse.ok()
						.body(BodyInserters.fromResource(headResource));
			case OPTIONS:
				return ServerResponse.ok()
						.allow(SUPPORTED_METHODS)
						.body(BodyInserters.empty());
			default:
				return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED)
						.allow(SUPPORTED_METHODS)
						.body(BodyInserters.empty());
		}
	}

	private static class HeadMethodResource implements Resource {

		private static final byte[] EMPTY = new byte[0];

		private final Resource delegate;

		public HeadMethodResource(Resource delegate) {
			this.delegate = delegate;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(EMPTY);
		}

		// delegation

		@Override
		public boolean exists() {
			return this.delegate.exists();
		}

		@Override
		public URL getURL() throws IOException {
			return this.delegate.getURL();
		}

		@Override
		public URI getURI() throws IOException {
			return this.delegate.getURI();
		}

		@Override
		public File getFile() throws IOException {
			return this.delegate.getFile();
		}

		@Override
		public long contentLength() throws IOException {
			return this.delegate.contentLength();
		}

		@Override
		public long lastModified() throws IOException {
			return this.delegate.lastModified();
		}

		@Override
		public Resource createRelative(String relativePath) throws IOException {
			return this.delegate.createRelative(relativePath);
		}

		@Override
		public String getFilename() {
			return this.delegate.getFilename();
		}

		@Override
		public String getDescription() {
			return this.delegate.getDescription();
		}

	}


}
