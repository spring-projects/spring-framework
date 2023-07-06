/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.service.invoker;

import org.junit.jupiter.api.BeforeEach;

/**
 * Tests for {@link HttpServiceMethod} with a test {@link TestHttpClientAdapter} that
 * stubs the client invocations.
 * <p>
 * The tests do not create or invoke {@code HttpServiceMethod} directly but rather use
 * {@link HttpServiceProxyFactory} to create a service proxy in order to use a strongly
 * typed interface without the need for class casts.
 *
 * @author Olga Maciaszek-Sharma
 */
public class HttpClientServiceMethodTests extends ReactiveHttpServiceMethodTests {

	@BeforeEach
	void setUp(){
		this.client = new TestHttpClientAdapter();
		this.proxyFactory = HttpServiceProxyFactory.builder((HttpClientAdapter) this.client).build();
	}

}
