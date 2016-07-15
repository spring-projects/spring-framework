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
package org.springframework.test.web.client;

import org.junit.Test;

import org.springframework.test.web.client.MockRestServiceServer.MockRestServiceServerBuilder;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link MockRestServiceServer}.
 * @author Rossen Stoyanchev
 */
public class MockRestServiceServerTests {

	private RestTemplate restTemplate = new RestTemplate();


	@Test
	public void buildMultipleTimes() throws Exception {
		MockRestServiceServerBuilder builder = MockRestServiceServer.bindTo(this.restTemplate);

		MockRestServiceServer server = builder.build();
		server.expect(requestTo("/foo")).andRespond(withSuccess());
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();

		server = builder.ignoreExpectOrder(true).build();
		server.expect(requestTo("/foo")).andRespond(withSuccess());
		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();

		server = builder.build();
		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
		server.verify();
	}

	@Test(expected = AssertionError.class)
	public void exactExpectOrder() throws Exception {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate)
				.ignoreExpectOrder(false).build();

		server.expect(requestTo("/foo")).andRespond(withSuccess());
		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
	}

	@Test
	public void ignoreExpectOrder() throws Exception {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate)
				.ignoreExpectOrder(true).build();

		server.expect(requestTo("/foo")).andRespond(withSuccess());
		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();
	}

	@Test
	public void resetAndReuseServer() throws Exception {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate).build();

		server.expect(requestTo("/foo")).andRespond(withSuccess());
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();
		server.reset();

		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
		server.verify();
	}

	@Test
	public void resetAndReuseServerWithUnorderedExpectationManager() throws Exception {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.restTemplate)
				.ignoreExpectOrder(true).build();

		server.expect(requestTo("/foo")).andRespond(withSuccess());
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();
		server.reset();

		server.expect(requestTo("/foo")).andRespond(withSuccess());
		server.expect(requestTo("/bar")).andRespond(withSuccess());
		this.restTemplate.getForObject("/bar", Void.class);
		this.restTemplate.getForObject("/foo", Void.class);
		server.verify();
	}

}
