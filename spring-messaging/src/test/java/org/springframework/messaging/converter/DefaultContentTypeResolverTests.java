/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * Tests for {@link DefaultContentTypeResolver}.
 *
 * @author Rossen Stoyanchev
 */
class DefaultContentTypeResolverTests {

	private final DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();


	@Test
	void resolve() {
		MessageHeaders headers = headers(MimeTypeUtils.APPLICATION_JSON);
		assertThat(this.resolver.resolve(headers)).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
	}

	@Test
	void resolveStringContentType() {
		MessageHeaders headers = headers(MimeTypeUtils.APPLICATION_JSON_VALUE);
		assertThat(this.resolver.resolve(headers)).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
	}

	@Test
	void resolveInvalidStringContentType() {
		MessageHeaders headers = headers("invalidContentType");
		assertThatExceptionOfType(InvalidMimeTypeException.class).isThrownBy(() -> this.resolver.resolve(headers));
	}

	@Test
	void resolveUnknownHeaderType() {
		MessageHeaders headers = headers(1);
		assertThatIllegalArgumentException().isThrownBy(() -> this.resolver.resolve(headers));
	}

	@Test
	void resolveNoContentTypeHeader() {
		MessageHeaders headers = new MessageHeaders(Collections.emptyMap());
		assertThat(this.resolver.resolve(headers)).isNull();
	}

	@Test
	void resolveDefaultMimeType() {
		this.resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
		MessageHeaders headers = new MessageHeaders(Collections.emptyMap());

		assertThat(this.resolver.resolve(headers)).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
	}

	@Test
	void resolveDefaultMimeTypeWithNoHeader() {
		this.resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
		assertThat(this.resolver.resolve(null)).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
	}

	private MessageHeaders headers(Object mimeType) {
		return new MessageHeaders(Map.of(MessageHeaders.CONTENT_TYPE, mimeType));
	}

}
