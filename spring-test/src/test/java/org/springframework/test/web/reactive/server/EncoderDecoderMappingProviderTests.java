/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server;

import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.TypeRef;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EncoderDecoderMappingProvider}.
 *
 * @author Stephane Nicoll
 */
class EncoderDecoderMappingProviderTests {

	private static final JsonMapper jsonMapper = new JsonMapper();

	private final EncoderDecoderMappingProvider mappingProvider = new EncoderDecoderMappingProvider(
			new JacksonJsonEncoder(jsonMapper), new JacksonJsonDecoder(jsonMapper));


	@Test
	void mapType() {
		Data data = this.mappingProvider.map(jsonData("test", 42), Data.class, Configuration.defaultConfiguration());
		assertThat(data).isEqualTo(new Data("test", 42));
	}

	@Test
	void mapGenericType() {
		List<?> jsonData = List.of(jsonData("first", 1), jsonData("second", 2), jsonData("third", 3));
		List<Data> data = this.mappingProvider.map(jsonData, new TypeRef<List<Data>>() {}, Configuration.defaultConfiguration());
		assertThat(data).containsExactly(new Data("first", 1), new Data("second", 2), new Data("third", 3));
	}

	private Map<String, Object> jsonData(String name, int counter) {
		return Map.of("name", name, "counter", counter);
	}


	record Data(String name, int counter) {}

}
