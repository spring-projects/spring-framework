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

package org.springframework.messaging.converter;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.MessageHeaders;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for
 * {@link org.springframework.messaging.converter.DefaultContentTypeResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultContentTypeResolverTests {

	private final DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();


	@Test
	public void resolve() {
		MessageHeaders headers = headers(MimeTypeUtils.APPLICATION_JSON);
		assertThat(this.resolver.resolve(headers)).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
	}

	@Test
	public void resolveStringContentType() {
		MessageHeaders headers = headers(MimeTypeUtils.APPLICATION_JSON_VALUE);
		assertThat(this.resolver.resolve(headers)).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
	}

	@Test
	public void resolveInvalidStringContentType() {
		MessageHeaders headers = headers("invalidContentType");
		assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> this.resolver.resolve(headers));
	}

	@Test
	public void resolveUnknownHeaderType() {
		MessageHeaders headers = headers(1);
		assertThatIllegalArgumentException().isThrownBy(() -> this.resolver.resolve(headers));
	}

	@Test
	public void resolveNoContentTypeHeader() {
		MessageHeaders headers = new MessageHeaders(Collections.emptyMap());
		assertThat(this.resolver.resolve(headers)).isNull();
	}

	@Test
	public void resolveDefaultMimeType() {
		this.resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
		MessageHeaders headers = new MessageHeaders(Collections.emptyMap());

		assertThat(this.resolver.resolve(headers)).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
	}

	@Test
	public void resolveDefaultMimeTypeWithNoHeader() {
		this.resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
		assertThat(this.resolver.resolve(null)).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
	}

	private MessageHeaders headers(Object mimeType) {
		return new MessageHeaders(Map.of(MessageHeaders.CONTENT_TYPE, mimeType));
	}

}
