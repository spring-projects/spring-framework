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

package org.springframework.web.reactive.result.view.freemarker;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

import freemarker.template.Configuration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import reactor.test.StepVerifier;

import org.springframework.context.ApplicationContextException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ModelMap;
import org.springframework.web.reactive.result.view.ZeroDemandResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;

import static org.junit.Assert.*;

/**
 * @author Rossen Stoyanchev
 */
public class FreeMarkerViewTests {

	private static final String TEMPLATE_PATH =
			"classpath*:org/springframework/web/reactive/view/freemarker/";


	private final MockServerWebExchange exchange =
			MockServerWebExchange.from(MockServerHttpRequest.get("/path"));

	private GenericApplicationContext context;

	private Configuration freeMarkerConfig;

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Before
	public void setup() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.refresh();

		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setPreferFileSystemAccess(false);
		configurer.setTemplateLoaderPath(TEMPLATE_PATH);
		configurer.setResourceLoader(this.context);
		this.freeMarkerConfig = configurer.createConfiguration();
	}


	@Test
	public void noFreeMarkerConfig() throws Exception {
		this.exception.expect(ApplicationContextException.class);
		this.exception.expectMessage("Must define a single FreeMarkerConfig bean");

		FreeMarkerView view = new FreeMarkerView();
		view.setApplicationContext(this.context);
		view.setUrl("anythingButNull");
		view.afterPropertiesSet();
	}

	@Test
	public void noTemplateName() throws Exception {
		this.exception.expect(IllegalArgumentException.class);
		this.exception.expectMessage("Property 'url' is required");

		FreeMarkerView freeMarkerView = new FreeMarkerView();
		freeMarkerView.afterPropertiesSet();
	}

	@Test
	public void checkResourceExists() throws Exception {
		FreeMarkerView view = new FreeMarkerView();
		view.setConfiguration(this.freeMarkerConfig);
		view.setUrl("test.ftl");

		assertTrue(view.checkResourceExists(Locale.US));
	}

	@Test
	public void render() {
		FreeMarkerView view = new FreeMarkerView();
		view.setConfiguration(this.freeMarkerConfig);
		view.setUrl("test.ftl");

		ModelMap model = new ExtendedModelMap();
		model.addAttribute("hello", "hi FreeMarker");
		view.render(model, null, this.exchange).block(Duration.ofMillis(5000));

		StepVerifier.create(this.exchange.getResponse().getBody())
				.consumeNextWith(buf -> assertEquals("<html><body>hi FreeMarker</body></html>", asString(buf)))
				.expectComplete()
				.verify();
	}

	@Test // gh-22754
	public void subscribeWithoutDemand() {
		ZeroDemandResponse response = new ZeroDemandResponse();
		ServerWebExchange exchange = new DefaultServerWebExchange(
				MockServerHttpRequest.get("/path").build(), response,
				new DefaultWebSessionManager(), ServerCodecConfigurer.create(),
				new AcceptHeaderLocaleContextResolver());

		FreeMarkerView view = new FreeMarkerView();
		view.setConfiguration(this.freeMarkerConfig);
		view.setUrl("test.ftl");

		ModelMap model = new ExtendedModelMap();
		model.addAttribute("hello", "hi FreeMarker");
		view.render(model, null, exchange).subscribe();

		response.cancelWrite();
		response.checkForLeaks();
	}


	private static String asString(DataBuffer dataBuffer) {
		ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
		final byte[] bytes = new byte[byteBuffer.remaining()];
		byteBuffer.get(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}


	@SuppressWarnings("unused")
	private String handle() {
		return null;
	}

}
