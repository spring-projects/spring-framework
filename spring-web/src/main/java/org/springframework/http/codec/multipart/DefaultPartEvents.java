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

package org.springframework.http.codec.multipart;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Default implementations of {@link PartEvent} and subtypes.
 *
 * @author Arjen Poutsma
 * @since 6.0
 */
abstract class DefaultPartEvents {

	public static FormPartEvent form(HttpHeaders headers) {
		Assert.notNull(headers, "Headers must not be null");
		return new DefaultFormFieldPartEvent(headers);
	}

	public static FormPartEvent form(HttpHeaders headers, String value) {
		Assert.notNull(headers, "Headers must not be null");
		Assert.notNull(value, "Value must not be null");
		return new DefaultFormFieldPartEvent(headers, value);
	}

	public static FilePartEvent file(HttpHeaders headers, DataBuffer dataBuffer, boolean isLast) {
		Assert.notNull(headers, "Headers must not be null");
		Assert.notNull(dataBuffer, "DataBuffer must not be null");
		return new DefaultFilePartEvent(headers, dataBuffer, isLast);
	}

	public static FilePartEvent file(HttpHeaders headers) {
		Assert.notNull(headers, "Headers must not be null");
		return new DefaultFilePartEvent(headers);
	}

	public static PartEvent create(HttpHeaders headers, DataBuffer dataBuffer, boolean isLast) {
		Assert.notNull(headers, "Headers must not be null");
		Assert.notNull(dataBuffer, "DataBuffer must not be null");
		if (headers.getContentDisposition().getFilename() != null) {
			return file(headers, dataBuffer, isLast);
		}
		else {
			return new DefaultPartEvent(headers, dataBuffer, isLast);
		}
	}

	public static PartEvent create(HttpHeaders headers) {
		Assert.notNull(headers, "Headers must not be null");
		if (headers.getContentDisposition().getFilename() != null) {
			return file(headers);
		}
		else {
			return new DefaultPartEvent(headers);
		}
	}



	private abstract static class AbstractPartEvent implements PartEvent {

		private final HttpHeaders headers;


		protected AbstractPartEvent(HttpHeaders headers) {
			this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
		}

		@Override
		public HttpHeaders headers() {
			return this.headers;
		}
	}


	/**
	 * Default implementation of {@link PartEvent}.
	 */
	private static class DefaultPartEvent extends AbstractPartEvent {

		private static final DataBuffer EMPTY = DefaultDataBufferFactory.sharedInstance.allocateBuffer(0);

		private final DataBuffer content;

		private final boolean last;


		public DefaultPartEvent(HttpHeaders headers) {
			this(headers, EMPTY, true);
		}

		public DefaultPartEvent(HttpHeaders headers, DataBuffer content, boolean last) {
			super(headers);
			this.content = content;
			this.last = last;
		}

		@Override
		public DataBuffer content() {
			return this.content;
		}

		@Override
		public boolean isLast() {
			return this.last;
		}

	}

	/**
	 * Default implementation of {@link FormPartEvent}.
	 */
	private static final class DefaultFormFieldPartEvent extends AbstractPartEvent implements FormPartEvent {

		private static final String EMPTY = "";

		private final String value;


		public DefaultFormFieldPartEvent(HttpHeaders headers) {
			this(headers, EMPTY);
		}

		public DefaultFormFieldPartEvent(HttpHeaders headers, String value) {
			super(headers);
			this.value = value;
		}

		@Override
		public String value() {
			return this.value;
		}

		@Override
		public DataBuffer content() {
			byte[] bytes = this.value.getBytes(MultipartUtils.charset(headers()));
			return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
		}

		@Override
		public boolean isLast() {
			return true;
		}
	}

	/**
	 * Default implementation of {@link FilePartEvent}.
	 */
	private static class DefaultFilePartEvent extends DefaultPartEvent implements FilePartEvent {

		public DefaultFilePartEvent(HttpHeaders headers) {
			super(headers);
		}

		public DefaultFilePartEvent(HttpHeaders headers, DataBuffer content, boolean last) {
			super(headers, content, last);
		}
	}
}
