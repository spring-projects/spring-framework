/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.rsocket.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetadataArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class MetadataArgumentResolverTests extends RSocketServiceArgumentResolverTestSupport {

	@Override
	protected RSocketServiceArgumentResolver initResolver() {
		return new MetadataArgumentResolver();
	}

	@Test
	void metadata() {
		MethodParameter param1 = initMethodParameter(Service.class, "execute", 0);
		MethodParameter param2 = initMethodParameter(Service.class, "execute", 1);
		MethodParameter param3 = initMethodParameter(Service.class, "execute", 2);
		MethodParameter param4 = initMethodParameter(Service.class, "execute", 3);

		assertThat(execute("foo", param1)).isTrue();
		assertThat(execute(MimeTypeUtils.APPLICATION_JSON, param2)).isTrue();

		assertThat(execute("bar", param3)).isTrue();
		assertThat(execute(MimeTypeUtils.APPLICATION_XML, param4)).isTrue();

		Map<Object, MimeType> expected = new LinkedHashMap<>();
		expected.put("foo", MimeTypeUtils.APPLICATION_JSON);
		expected.put("bar", MimeTypeUtils.APPLICATION_XML);

		assertThat(getRequestValues().getMetadata()).containsExactlyEntriesOf(expected);
	}


	@SuppressWarnings("unused")
	private interface Service {

		void execute(String metadata1, MimeType mimeType1, String metadata2, MimeType mimeType2);

		void executeNotAnnotated(String foo, String bar);

	}

}
