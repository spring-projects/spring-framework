/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.io.support;

import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EncodedResource}.
 *
 * @author Sam Brannen
 * @since 3.2.14
 */
class EncodedResourceTests {

	private static final String UTF8 = "UTF-8";
	private static final String UTF16 = "UTF-16";
	private static final Charset UTF8_CS = Charset.forName(UTF8);
	private static final Charset UTF16_CS = Charset.forName(UTF16);

	private final Resource resource = new DescriptiveResource("test");


	@Test
	void equalsWithNullOtherObject() {
		assertThat(new EncodedResource(resource).equals(null)).isFalse();
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
