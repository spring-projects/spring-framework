/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.codec.multipart;

import java.nio.file.Path;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Default implementations of {@link Part} and subtypes.
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
abstract class DefaultParts {

	/**
	 * Create a new {@link FormFieldPart} with the given parameters.
	 * @param headers the part headers
	 * @param value the form field value
	 * @return the created part
	 */
	public static FormFieldPart formFieldPart(HttpHeaders headers, String value) {
		Assert.notNull(headers, "Headers must not be null");
		Assert.notNull(value, "Value must not be null");

		return new DefaultFormFieldPart(headers, value);
	}

	/**
	 * Create a new {@link Part} or {@link FilePart} with the given parameters.
	 * Returns {@link FilePart} if the {@code Content-Disposition} of the given
	 * headers contains a filename, or a "normal" {@link Part} otherwise
	 * @param headers the part headers
	 * @param content the content of the part
	 * @return {@link Part} or {@link FilePart}, depending on {@link HttpHeaders#getContentDisposition()}
	 */
	public static Part part(HttpHeaders headers, Flux<DataBuffer> content) {
		Assert.notNull(headers, "Headers must not be null");
		Assert.notNull(content, "Content must not be null");

		String filename = headers.getContentDisposition().getFilename();
		if (filename != null) {
			return new DefaultFilePart(headers, content);
		}
		else {
			return new DefaultPart(headers, content);
		}
	}


	/**
	 * Abstract base class.
	 */
	private static abstract class AbstractPart implements Part {

		private final HttpHeaders headers;


		protected AbstractPart(HttpHeaders headers) {
			Assert.notNull(headers, "HttpHeaders is required");
			this.headers = headers;
		}

		@Override
		public String name() {
			String name = headers().getContentDisposition().getName();
			Assert.state(name != null, "No name available");
			return name;
		}


		@Override
		public HttpHeaders headers() {
			return this.headers;
		}
	}


	/**
	 * Default implementation of {@link FormFieldPart}.
	 */
	private static class DefaultFormFieldPart extends AbstractPart implements FormFieldPart {

		private final String value;

		public DefaultFormFieldPart(HttpHeaders headers, String value) {
			super(headers);
			this.value = value;
		}

		@Override
		public Flux<DataBuffer> content() {
			return Flux.defer(() -> {
				byte[] bytes = this.value.getBytes(MultipartUtils.charset(headers()));
				return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
			});
		}

		@Override
		public String value() {
			return this.value;
		}

		@Override
		public String toString() {
			String name = headers().getContentDisposition().getName();
			if (name != null) {
				return "DefaultFormFieldPart{" + name() + "}";
			}
			else {
				return "DefaultFormFieldPart";
			}
		}
	}


	/**
	 * Default implementation of {@link Part}.
	 */
	private static class DefaultPart extends AbstractPart {

		private final Flux<DataBuffer> content;

		public DefaultPart(HttpHeaders headers, Flux<DataBuffer> content) {
			super(headers);
			this.content = content;
		}

		@Override
		public Flux<DataBuffer> content() {
			return this.content;
		}

		@Override
		public String toString() {
			String name = headers().getContentDisposition().getName();
			if (name != null) {
				return "DefaultPart{" + name + "}";
			}
			else {
				return "DefaultPart";
			}
		}

	}


	/**
	 * Default implementation of {@link FilePart}.
	 */
	private static class DefaultFilePart extends DefaultPart implements FilePart {

		public DefaultFilePart(HttpHeaders headers, Flux<DataBuffer> content) {
			super(headers, content);
		}

		@Override
		public String filename() {
			String filename = this.headers().getContentDisposition().getFilename();
			Assert.state(filename != null, "No filename found");
			return filename;
		}

		@Override
		public Mono<Void> transferTo(Path dest) {
			return DataBufferUtils.write(content(), dest);
		}

		@Override
		public String toString() {
			ContentDisposition contentDisposition = headers().getContentDisposition();
			String name = contentDisposition.getName();
			String filename = contentDisposition.getFilename();
			if (name != null) {
				return "DefaultFilePart{" + name() + " (" + filename + ")}";
			}
			else {
				return "DefaultFilePart{(" + filename + ")}";
			}
		}

	}

}
