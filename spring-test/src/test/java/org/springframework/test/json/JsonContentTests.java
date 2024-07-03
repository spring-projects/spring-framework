/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.json;

import org.junit.jupiter.api.Test;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.http.HttpMessageContentConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JsonContent}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class JsonContentTests {

	private static final String JSON = "{\"name\":\"spring\", \"age\":100}";

	@Test
	void createWhenJsonIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> new JsonContent(null))
				.withMessageContaining("JSON must not be null");
	}

	@Test
	void assertThatShouldReturnJsonContentAssert() {
		JsonContent content = new JsonContent(JSON);
		assertThat(content.assertThat()).isInstanceOf(JsonContentAssert.class);
	}

	@Test
	void getJsonShouldReturnJson() {
		JsonContent content = new JsonContent(JSON);
		assertThat(content.getJson()).isEqualTo(JSON);
	}

	@Test
	void toStringShouldReturnString() {
		JsonContent content = new JsonContent(JSON);
		assertThat(content.toString()).isEqualTo("JsonContent " + JSON);
	}

	@Test
	void getJsonContentConverterShouldReturnConverter() {
		HttpMessageContentConverter contentConverter = HttpMessageContentConverter.of(mock(HttpMessageConverter.class));
		JsonContent content = new JsonContent(JSON, contentConverter);
		assertThat(content.getContentConverter()).isSameAs(contentConverter);
	}

}
