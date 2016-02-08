/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.reactive.view.freemarker;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

import freemarker.template.Configuration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import reactor.core.publisher.Flux;
import reactor.core.test.TestSubscriber;

import org.springframework.context.ApplicationContextException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Rossen Stoyanchev
 */
public class FreeMarkerViewTests {

	public static final String TEMPLATE_PATH = "classpath*:org/springframework/web/reactive/view/freemarker/";

	private static final Charset UTF_8 = Charset.forName("UTF-8");


	private ServerWebExchange exchange;

	private GenericApplicationContext context;

	private Configuration freeMarkerConfig;

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Before
	public void setUp() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.refresh();

		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setPreferFileSystemAccess(false);
		configurer.setTemplateLoaderPath(TEMPLATE_PATH);
		configurer.setResourceLoader(this.context);
		this.freeMarkerConfig = configurer.createConfiguration();

		FreeMarkerView fv = new FreeMarkerView();
		fv.setApplicationContext(this.context);

		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/path"));
		MockServerHttpResponse response = new MockServerHttpResponse();
		WebSessionManager manager = new DefaultWebSessionManager();
		this.exchange = new DefaultServerWebExchange(request, response, manager);
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
	public void render() throws Exception {
		FreeMarkerView view = new FreeMarkerView();
		view.setConfiguration(this.freeMarkerConfig);
		view.setUrl("test.ftl");

		ModelMap model = new ExtendedModelMap();
		model.addAttribute("hello", "hi FreeMarker");
		HandlerResult result = new HandlerResult(new Object(), "", ResolvableType.NONE, model);
		Flux<DataBuffer> flux = view.render(result, Optional.empty(), this.exchange);

		TestSubscriber<DataBuffer> subscriber = new TestSubscriber<>();
		subscriber.bindTo(flux).assertValuesWith(dataBuffer ->
					assertEquals("<html><body>hi FreeMarker</body></html>", asString(dataBuffer)));
	}



	private static String asString(DataBuffer dataBuffer) {
		ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
		final byte[] bytes = new byte[byteBuffer.remaining()];
		byteBuffer.get(bytes);
		return new String(bytes, UTF_8);
	}

}
