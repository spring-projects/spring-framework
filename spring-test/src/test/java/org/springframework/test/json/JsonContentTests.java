/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.json;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.converter.HttpMessageConverter;

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
		JsonConverterDelegate converter = JsonConverterDelegate.of(List.of(mock(HttpMessageConverter.class)));
		JsonContent content = new JsonContent(JSON, converter);
		assertThat(content.getJsonConverterDelegate()).isSameAs(converter);
	}

}
