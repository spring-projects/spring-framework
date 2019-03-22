/*
 * Copyright 2002-2017 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import reactor.test.StepVerifier;

import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ModelMap;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Unit tests for {@link HttpMessageWriterView}.
 * @author Rossen Stoyanchev
 */
public class HttpMessageWriterViewTests {

	private HttpMessageWriterView view = new HttpMessageWriterView(new Jackson2JsonEncoder());

	private final ModelMap model = new ExtendedModelMap();

	private final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));


	@Test
	public void supportedMediaTypes() throws Exception {
		assertEquals(Arrays.asList(
				MediaType.parseMediaType("application/json;charset=UTF-8"),
				MediaType.parseMediaType("application/*+json;charset=UTF-8")),
				this.view.getSupportedMediaTypes());
	}

	@Test
	public void singleMatch() throws Exception {
		this.view.setModelKeys(Collections.singleton("foo2"));
		this.model.addAttribute("foo1", Collections.singleton("bar1"));
		this.model.addAttribute("foo2", Collections.singleton("bar2"));
		this.model.addAttribute("foo3", Collections.singleton("bar3"));

		assertEquals("[\"bar2\"]", doRender());
	}

	@Test
	public void noMatch() throws Exception {
		this.view.setModelKeys(Collections.singleton("foo2"));
		this.model.addAttribute("foo1", "bar1");

		assertEquals("", doRender());
	}

	@Test
	public void noMatchBecauseNotSupported() throws Exception {
		this.view = new HttpMessageWriterView(new Jaxb2XmlEncoder());
		this.view.setModelKeys(new HashSet<>(Collections.singletonList("foo1")));
		this.model.addAttribute("foo1", "bar1");

		assertEquals("", doRender());
	}

	@Test
	public void multipleMatches() throws Exception {
		this.view.setModelKeys(new HashSet<>(Arrays.asList("foo1", "foo2")));
		this.model.addAttribute("foo1", Collections.singleton("bar1"));
		this.model.addAttribute("foo2", Collections.singleton("bar2"));
		this.model.addAttribute("foo3", Collections.singleton("bar3"));

		assertEquals("{\"foo1\":[\"bar1\"],\"foo2\":[\"bar2\"]}", doRender());
	}

	@Test
	public void multipleMatchesNotSupported() throws Exception {
		this.view = new HttpMessageWriterView(CharSequenceEncoder.allMimeTypes());
		this.view.setModelKeys(new HashSet<>(Arrays.asList("foo1", "foo2")));
		this.model.addAttribute("foo1", "bar1");
		this.model.addAttribute("foo2", "bar2");

		try {
			doRender();
			fail();
		}
		catch (IllegalStateException ex) {
			String message = ex.getMessage();
			assertTrue(message, message.contains("Map rendering is not supported"));
		}
	}

	@Test
	public void render() throws Exception {
		Map<String, String> pojoData = new LinkedHashMap<>();
		pojoData.put("foo", "f");
		pojoData.put("bar", "b");
		this.model.addAttribute("pojoData", pojoData);
		this.view.setModelKeys(Collections.singleton("pojoData"));

		this.view.render(this.model, MediaType.APPLICATION_JSON, exchange).block(Duration.ZERO);

		StepVerifier.create(this.exchange.getResponse().getBody())
				.consumeNextWith(buf -> assertEquals("{\"foo\":\"f\",\"bar\":\"b\"}", dumpString(buf)))
				.expectComplete()
				.verify();
	}

	private String dumpString(DataBuffer buf) {
		return DataBufferTestUtils.dumpString(buf, StandardCharsets.UTF_8);
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
