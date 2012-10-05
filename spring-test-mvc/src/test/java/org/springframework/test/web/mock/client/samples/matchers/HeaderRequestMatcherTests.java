/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.test.web.mock.client.samples.matchers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.mock.client.match.RequestMatchers.header;
import static org.springframework.test.web.mock.client.match.RequestMatchers.requestTo;
import static org.springframework.test.web.mock.client.response.ResponseCreators.withSuccess;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.test.web.mock.Person;
import org.springframework.test.web.mock.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 * Examples of defining expectations on request headers.
 *
 * @author Rossen Stoyanchev
 */
public class HeaderRequestMatcherTests {

	private static final String RESPONSE_BODY = "{\"name\" : \"Ludwig van Beethoven\", \"someDouble\" : \"1.6035\"}";

	private MockRestServiceServer mockServer;

	private RestTemplate restTemplate;

	@Before
	public void setup() {
		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new StringHttpMessageConverter());
		converters.add(new MappingJacksonHttpMessageConverter());

		this.restTemplate = new RestTemplate();
		this.restTemplate.setMessageConverters(converters);

		this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
	}

	@Test
	public void testString() throws Exception {

		this.mockServer.expect(requestTo("/person/1"))
			.andExpect(header("Accept", "application/json"))
			.andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

		this.restTemplate.getForObject(new URI("/person/1"), Person.class);
		this.mockServer.verify();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testStringContains() throws Exception {

		this.mockServer.expect(requestTo("/person/1"))
			.andExpect(header("Accept", containsString("json")))
			.andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

		this.restTemplate.getForObject(new URI("/person/1"), Person.class);
		this.mockServer.verify();
	}

}
