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

package org.springframework.test.web.reactive.server;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.TypeRef;
import org.junit.jupiter.api.Test;

import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EncoderDecoderMappingProvider}.
 *
 * @author Stephane Nicoll
 */
class EncoderDecoderMappingProviderTests {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private final EncoderDecoderMappingProvider mappingProvider = new EncoderDecoderMappingProvider(
			new Jackson2JsonEncoder(objectMapper), new Jackson2JsonDecoder(objectMapper));


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
