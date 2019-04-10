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

package org.springframework.web.servlet.function;

import java.util.Optional;

import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.web.servlet.function.support.ServerRequestTransformer;

import static org.junit.Assert.*;

/**
 * @author Krzysztof Kocel
 */
public class RouterFunctionMappingIntegrationTests {

	private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");

	private final ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();

	@Test
	public void requestTransformer() throws Exception {

		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction = request -> {
			if(request.method() == HttpMethod.POST) {
				return Optional.of(handlerFunction);
			}else {
				return Optional.empty();
			}
		};

		RouterFunctionMapping mapping = new RouterFunctionMapping(routerFunction);
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(TestConfiguration.class);
		wac.refresh();
		mapping.setApplicationContext(wac);
		mapping.afterPropertiesSet();

		HandlerExecutionChain result = mapping.getHandler(this.request);

		assertEquals(handlerFunction, result.getHandler());
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public ServerRequestTransformer serverRequestTransformer() {
			return serverRequest -> ServerRequest.from(serverRequest).method(HttpMethod.POST).build();
		}
	}
}
