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

package org.springframework.web.client.reactive;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;

/**
 *
 * @author Rob Winch
 *
 */
public class DefaultHttpRequestBuilderTests {
	private DefaultHttpRequestBuilder builder;

	@Before
	public void setup() {
		builder = new DefaultHttpRequestBuilder(HttpMethod.GET, "https://example.com/foo");
	}

	@Test
	public void apply() {
		RequestPostProcessor postProcessor = mock(RequestPostProcessor.class);

		builder.apply(postProcessor);

		verify(postProcessor).postProcess(builder);
	}

	@Test(expected = IllegalArgumentException.class)
	public void applyNullPostProcessorThrowsIllegalArgumentException() {
		builder.apply(null);
	}
}
