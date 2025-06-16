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

package org.springframework.web.reactive.result.view;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link HttpMessageWriterView}.
 *
 * @author Rossen Stoyanchev
 */
class HttpMessageWriterViewTests {

	private HttpMessageWriterView view = new HttpMessageWriterView(new JacksonJsonEncoder());

	private final Map<String, Object> model = new HashMap<>();

	private final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));


	@Test
	void supportedMediaTypes() {
		assertThat(this.view.getSupportedMediaTypes()).containsExactly(
				MediaType.APPLICATION_JSON,
				MediaType.parseMediaType("application/*+json"),
				MediaType.APPLICATION_NDJSON);
	}

	@Test
	void singleMatch() throws Exception {
		this.view.setModelKeys(Set.of("foo2"));
		this.model.put("foo1", Set.of("bar1"));
		this.model.put("foo2", Set.of("bar2"));
		this.model.put("foo3", Set.of("bar3"));

		assertThat(doRender()).isEqualTo("[\"bar2\"]");
	}

	@Test
	void noMatch() throws Exception {
		this.view.setModelKeys(Set.of("foo2"));
		this.model.put("foo1", "bar1");

		assertThat(doRender()).isEmpty();
	}

	@Test
	void noMatchBecauseNotSupported() throws Exception {
		this.view = new HttpMessageWriterView(new Jaxb2XmlEncoder());
		this.view.setModelKeys(Set.of("foo1"));
		this.model.put("foo1", "bar1");

		assertThat(doRender()).isEmpty();
	}

	@Test
	void multipleMatches() throws Exception {
		this.view.setModelKeys(Set.of("foo1", "foo2"));
		this.model.put("foo1", Set.of("bar1"));
		this.model.put("foo2", Set.of("bar2"));
		this.model.put("foo3", Set.of("bar3"));

		assertThat(doRender()).isEqualTo("{\"foo1\":[\"bar1\"],\"foo2\":[\"bar2\"]}");
	}

	@Test
	void multipleMatchesNotSupported() throws Exception {
		this.view = new HttpMessageWriterView(CharSequenceEncoder.allMimeTypes());
		this.view.setModelKeys(Set.of("foo1", "foo2"));
		this.model.put("foo1", "bar1");
		this.model.put("foo2", "bar2");

		assertThatIllegalStateException()
				.isThrownBy(this::doRender)
				.withMessageContaining("Map rendering is not supported");
	}

	@Test
	void render() throws Exception {
		Map<String, String> pojoData = new LinkedHashMap<>();
		pojoData.put("foo", "f");
		pojoData.put("bar", "b");
		this.model.put("pojoData", pojoData);
		this.view.setModelKeys(Set.of("pojoData"));

		this.view.render(this.model, MediaType.APPLICATION_JSON, exchange).block(Duration.ZERO);

		StepVerifier.create(this.exchange.getResponse().getBody())
				.consumeNextWith(buf -> assertThat(buf.toString(UTF_8)).isEqualTo("{\"foo\":\"f\",\"bar\":\"b\"}"))
				.expectComplete()
				.verify();
	}

	private String doRender() {
		this.view.render(this.model, MediaType.APPLICATION_JSON, this.exchange).block(Duration.ZERO);
		return this.exchange.getResponse().getBodyAsString().block(Duration.ZERO);
	}



	@SuppressWarnings("unused")
	private String handle() {
		return null;
	}

}
