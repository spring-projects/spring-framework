/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.support;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EncodedResource}.
 *
 * @author Sam Brannen
 * @since 3.2.14
 */
class EncodedResourceTests {

	private static final String UTF8 = "UTF-8";
	private static final String UTF16 = "UTF-16";
	private static final Charset UTF8_CS = StandardCharsets.UTF_8;
	private static final Charset UTF16_CS = StandardCharsets.UTF_16;

	private final Resource resource = new DescriptiveResource("test");


	@Test
	void equalsWithNullOtherObject() {
		assertThat(new EncodedResource(resource)).isNotEqualTo(null);
	}

	@Test
	void equalsWithSameEncoding() {
		EncodedResource er1 = new EncodedResource(resource, UTF8);
		EncodedResource er2 = new EncodedResource(resource, UTF8);
		assertThat(er2).isEqualTo(er1);
	}

	@Test
	void equalsWithDifferentEncoding() {
		EncodedResource er1 = new EncodedResource(resource, UTF8);
		EncodedResource er2 = new EncodedResource(resource, UTF16);
		assertThat(er2).isNotEqualTo(er1);
	}

	@Test
	void equalsWithSameCharset() {
		EncodedResource er1 = new EncodedResource(resource, UTF8_CS);
		EncodedResource er2 = new EncodedResource(resource, UTF8_CS);
		assertThat(er2).isEqualTo(er1);
	}

	@Test
	void equalsWithDifferentCharset() {
		EncodedResource er1 = new EncodedResource(resource, UTF8_CS);
		EncodedResource er2 = new EncodedResource(resource, UTF16_CS);
		assertThat(er2).isNotEqualTo(er1);
	}

	@Test
	void equalsWithEncodingAndCharset() {
		EncodedResource er1 = new EncodedResource(resource, UTF8);
		EncodedResource er2 = new EncodedResource(resource, UTF8_CS);
		assertThat(er2).isNotEqualTo(er1);
	}

}
