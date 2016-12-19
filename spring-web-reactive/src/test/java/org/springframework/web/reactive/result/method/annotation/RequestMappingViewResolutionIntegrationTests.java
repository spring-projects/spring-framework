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

package org.springframework.web.reactive.result.method.annotation;

import java.net.URI;
import java.util.Optional;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebReactiveConfigurationSupport;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.http.RequestEntity.get;


/**
 * {@code @RequestMapping} integration tests with view resolution scenarios.
 *
 * @author Rossen Stoyanchev
 */
public class RequestMappingViewResolutionIntegrationTests extends AbstractRequestMappingIntegrationTests {


	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class);
		wac.refresh();
		return wac;
	}


	@Test
	public void html() throws Exception {
		String expected = "<html><body>Hello: Jason!</body></html>";
		assertEquals(expected, performGet("/html?name=Jason", MediaType.TEXT_HTML, String.class).getBody());
	}

	@Test
	public void etagCheckWithNotModifiedResponse() throws Exception {
		URI uri = new URI("http://localhost:" + this.port + "/html");
		RequestEntity<Void> request = get(uri).ifNoneMatch("\"deadb33f8badf00d\"").build();
		ResponseEntity<String> response = getRestTemplate().exchange(request, String.class);

		assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
		assertNull(response.getBody());
	}


	@Configuration
	@ComponentScan(resourcePattern = "**/RequestMappingViewResolutionIntegrationTests$*.class")
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig extends WebReactiveConfigurationSupport {

		@Override
		protected void configureViewResolvers(ViewResolverRegistry registry) {
			registry.freeMarker();
		}

		@Bean
		public FreeMarkerConfigurer freeMarkerConfig() {
			FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
			configurer.setPreferFileSystemAccess(false);
			configurer.setTemplateLoaderPath("classpath*:org/springframework/web/reactive/view/freemarker/");
			return configurer;
		}

	}

	@Controller
	@SuppressWarnings("unused")
	private static class TestController {

		@GetMapping("/html")
		public String getHtmlPage(@RequestParam Optional<String> name, Model model,
				ServerWebExchange exchange) {

			if (exchange.checkNotModified("deadb33f8badf00d")) {
				return null;
			}
			model.addAttribute("hello", "Hello: " + name.orElse("<no name>") + "!");
			return "test";
		}

	}

}
