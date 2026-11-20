/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import freemarker.cache.ClassTemplateLoader;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * Integration tests for view resolution with {@code @EnableWebFlux}.
 *
 * @author Sam Brannen
 * @since 6.1.11
 * @see org.springframework.web.servlet.config.annotation.ViewResolutionIntegrationTests
 */
class WebFluxViewResolutionIntegrationTests {

	private static final MediaType TEXT_HTML_UTF8 = MediaType.parseMediaType("text/html;charset=UTF-8");

	private static final MediaType TEXT_HTML_ISO_8859_1 = MediaType.parseMediaType("text/html;charset=ISO-8859-1");


	@Nested
	class FreeMarkerTests {

		private static final String EXPECTED_BODY = """
			<html>
			<body>
			<h1>Hello, Java Café</h1>
			<p>output_encoding: %s</p>
			</body>
			</html>
			""";

		private static final ClassTemplateLoader classTemplateLoader =
				new ClassTemplateLoader(WebFluxViewResolutionIntegrationTests.class, "");

		@Test
		void freemarkerWithInvalidConfig() {
			assertThatRuntimeException()
					.isThrownBy(() -> runTest(InvalidFreeMarkerWebFluxConfig.class))
					.withMessageContaining("In addition to a FreeMarker view resolver ");
		}

		@Test
		void freemarkerWithDefaults() throws Exception {
			MockServerHttpResponse response = runTest(FreeMarkerWebFluxConfig.class);
			StepVerifier.create(response.getBodyAsString()).expectNext(EXPECTED_BODY.formatted("UTF-8")).expectComplete().verify();
			assertThat(response.getHeaders().getContentType()).isEqualTo(TEXT_HTML_UTF8);
		}

		@Test
		void freemarkerWithExplicitDefaultEncoding() throws Exception {
			MockServerHttpResponse response = runTest(ExplicitDefaultEncodingConfig.class);
			StepVerifier.create(response.getBodyAsString()).expectNext(EXPECTED_BODY.formatted("UTF-8")).expectComplete().verify();
			assertThat(response.getHeaders().getContentType()).isEqualTo(TEXT_HTML_UTF8);
		}

		@Test
		void freemarkerWithExplicitDefaultEncodingAndContentType() throws Exception {
			MockServerHttpResponse response = runTest(ExplicitDefaultEncodingAndContentTypeConfig.class);
			StepVerifier.create(response.getBodyAsString()).expectNext(EXPECTED_BODY.formatted("ISO-8859-1")).expectComplete().verify();
			// When the Content-Type (supported media type) is explicitly set on the view resolver, it should be used.
			assertThat(response.getHeaders().getContentType()).isEqualTo(TEXT_HTML_ISO_8859_1);
		}


		@EnableWebFlux
		@Configuration(proxyBeanMethods = false)
		static class InvalidFreeMarkerWebFluxConfig implements WebFluxConfigurer {

			@Override
			public void configureViewResolvers(ViewResolverRegistry registry) {
				registry.freeMarker();
			}
		}


		@Configuration(proxyBeanMethods = false)
		static class FreeMarkerWebFluxConfig extends AbstractWebFluxConfig {

			@Override
			public void configureViewResolvers(ViewResolverRegistry registry) {
				registry.freeMarker();
			}

			@Bean
			public FreeMarkerConfigurer freeMarkerConfigurer() {
				FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
				configurer.setPreTemplateLoaders(classTemplateLoader);
				return configurer;
			}
		}


		@Configuration(proxyBeanMethods = false)
		static class ExplicitDefaultEncodingConfig extends AbstractWebFluxConfig {

			@Override
			public void configureViewResolvers(ViewResolverRegistry registry) {
				registry.freeMarker();
			}

			@Bean
			public FreeMarkerConfigurer freeMarkerConfigurer() {
				FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
				configurer.setPreTemplateLoaders(classTemplateLoader);
				configurer.setDefaultCharset(UTF_8);
				return configurer;
			}
		}


		@Configuration(proxyBeanMethods = false)
		static class ExplicitDefaultEncodingAndContentTypeConfig extends AbstractWebFluxConfig {

			@Autowired
			ApplicationContext applicationContext;

			@Override
			public void configureViewResolvers(ViewResolverRegistry registry) {
				FreeMarkerViewResolver resolver = new FreeMarkerViewResolver("", ".ftl");
				resolver.setSupportedMediaTypes(List.of(TEXT_HTML_ISO_8859_1));
				resolver.setApplicationContext(this.applicationContext);
				registry.viewResolver(resolver);
			}

			@Bean
			public FreeMarkerConfigurer freeMarkerConfigurer() {
				FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
				configurer.setPreTemplateLoaders(classTemplateLoader);
				configurer.setDefaultCharset(ISO_8859_1);
				return configurer;
			}

			@Override
			@Bean
			public SampleController sampleController() {
				return new SampleController("index_ISO-8859-1");
			}
		}
	}


	private static MockServerHttpResponse runTest(Class<?> configClass) throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		new DispatcherHandler(context).handle(exchange).block(Duration.ofSeconds(1));
		return exchange.getResponse();
	}


	@EnableWebFlux
	abstract static class AbstractWebFluxConfig implements WebFluxConfigurer {

		@Bean
		public SampleController sampleController() {
			return new SampleController("index_UTF-8");
		}
	}


	@Controller
	static class SampleController {

		private final String viewName;

		SampleController(String viewName) {
			this.viewName = viewName;
		}

		@GetMapping("/")
		String index(Map<String, Object> model) {
			model.put("hello", "Hello");
			return this.viewName;
		}
	}

}
