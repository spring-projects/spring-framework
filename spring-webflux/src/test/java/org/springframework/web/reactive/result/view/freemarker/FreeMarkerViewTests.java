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

package org.springframework.web.reactive.result.view.freemarker;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

import freemarker.template.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.context.ApplicationContextException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ZeroDemandResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class FreeMarkerViewTests {

	private static final String TEMPLATE_PATH =
			"classpath*:org/springframework/web/reactive/view/freemarker/";


	private final FreeMarkerView freeMarkerView = new FreeMarkerView();

	private final MockServerWebExchange exchange =
			MockServerWebExchange.from(MockServerHttpRequest.get("/path"));

	private final GenericApplicationContext context = new GenericApplicationContext();

	private Configuration freeMarkerConfig;


	@BeforeEach
	void setup() throws Exception {
		this.context.refresh();

		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setPreferFileSystemAccess(false);
		configurer.setTemplateLoaderPath(TEMPLATE_PATH);
		configurer.setResourceLoader(this.context);
		this.freeMarkerConfig = configurer.createConfiguration();
	}


	@Test
	void noFreeMarkerConfig() {
		freeMarkerView.setApplicationContext(this.context);
		freeMarkerView.setUrl("anythingButNull");

		assertThatExceptionOfType(ApplicationContextException.class)
			.isThrownBy(freeMarkerView::afterPropertiesSet)
			.withMessageContaining("Must define a single FreeMarkerConfig bean");
	}

	@Test
	void noTemplateName() {
		assertThatIllegalArgumentException()
			.isThrownBy(freeMarkerView::afterPropertiesSet)
			.withMessageContaining("Property 'url' is required");
	}

	@Test
	void checkResourceExists() throws Exception {
		freeMarkerView.setConfiguration(this.freeMarkerConfig);
		freeMarkerView.setUrl("test.ftl");

		assertThat(freeMarkerView.checkResourceExists(Locale.US)).isTrue();
	}

	@Test
	void resourceExists() {
		freeMarkerView.setConfiguration(this.freeMarkerConfig);
		freeMarkerView.setUrl("test.ftl");

		StepVerifier.create(freeMarkerView.resourceExists(Locale.US))
				.assertNext(b -> assertThat(b).isTrue())
				.verifyComplete();
	}

	@Test
	void resourceDoesNotExists() {
		freeMarkerView.setConfiguration(this.freeMarkerConfig);
		freeMarkerView.setUrl("foo-bar.ftl");

		StepVerifier.create(freeMarkerView.resourceExists(Locale.US))
				.assertNext(b -> assertThat(b).isFalse())
				.verifyComplete();
	}

	@Test
	void render() {
		freeMarkerView.setApplicationContext(this.context);
		freeMarkerView.setConfiguration(this.freeMarkerConfig);
		freeMarkerView.setUrl("test.ftl");

		Map<String, Object> model = Map.of("hello", "hi FreeMarker");
		freeMarkerView.render(model, null, this.exchange).block(Duration.ofMillis(5000));

		StepVerifier.create(this.exchange.getResponse().getBody())
				.consumeNextWith(buf -> assertThat(buf.toString(StandardCharsets.UTF_8))
						.isEqualTo("<html><body>hi FreeMarker</body></html>"))
				.expectComplete()
				.verify();
	}

	@Test // gh-22754
	void subscribeWithoutDemand() {
		ZeroDemandResponse response = new ZeroDemandResponse();
		ServerWebExchange exchange = new DefaultServerWebExchange(
				MockServerHttpRequest.get("/path").build(), response,
				new DefaultWebSessionManager(), ServerCodecConfigurer.create(),
				new AcceptHeaderLocaleContextResolver());

		freeMarkerView.setApplicationContext(this.context);
		freeMarkerView.setConfiguration(this.freeMarkerConfig);
		freeMarkerView.setUrl("test.ftl");

		Map<String, Object> model = Map.of("hello", "hi FreeMarker");
		freeMarkerView.render(model, null, exchange).subscribe();

		response.cancelWrite();
		response.checkForLeaks();
	}


	@SuppressWarnings("unused")
	private String handle() {
		return null;
	}

}
