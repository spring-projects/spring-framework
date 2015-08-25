/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.reactive.web.dispatch.method.annotation;


import java.net.URI;
import java.nio.charset.Charset;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.rx.Streams;

import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.reactive.web.dispatch.DispatcherHandler;
import org.springframework.reactive.web.http.AbstractHttpHandlerIntegrationTests;
import org.springframework.reactive.web.http.HttpHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.support.StaticWebApplicationContext;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author Rossen Stoyanchev
 */
public class RequestMappingIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private static final Charset UTF_8 = Charset.forName("UTF-8");


	@Override
	protected HttpHandler createHttpHandler() {

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("handlerMapping", RequestMappingHandlerMapping.class);
		wac.registerSingleton("handlerAdapter", RequestMappingHandlerAdapter.class);
		wac.registerSingleton("responseBodyResultHandler", ResponseBodyResultHandler.class);
		wac.registerSingleton("controller", TestController.class);
		wac.refresh();

		DispatcherHandler dispatcherHandler = new DispatcherHandler();
		dispatcherHandler.setApplicationContext(wac);
		return dispatcherHandler;
	}

	@Test
	public void helloWithQueryParam() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/param?name=George");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<byte[]> response = restTemplate.exchange(request, byte[].class);

		assertArrayEquals("Hello George!".getBytes(UTF_8), response.getBody());
	}


	@Controller
	@SuppressWarnings("unused")
	private static class TestController {

		@RequestMapping("/param")
		@ResponseBody
		public Publisher<String> handleWithParam(@RequestParam String name) {
			return Streams.just("Hello ", name, "!");
		}
	}

}
