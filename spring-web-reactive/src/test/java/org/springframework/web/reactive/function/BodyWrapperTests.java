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

package org.springframework.web.reactive.function;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.web.reactive.function.support.RequestWrapper;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Arjen Poutsma
 */
public class BodyWrapperTests {

	private Request.Body mockBody;

	private RequestWrapper.BodyWrapper wrapper;

	@Before
	public void setUp() throws Exception {
		mockBody = mock(Request.Body.class);
		wrapper = new RequestWrapper.BodyWrapper(mockBody);
	}

	@Test
	public void stream() throws Exception {
		DataBuffer buffer = new DefaultDataBufferFactory().allocateBuffer();
		Flux<DataBuffer> flux = Flux.just(buffer);
		when(mockBody.stream()).thenReturn(flux);

		assertSame(flux, wrapper.stream());
	}

	@Test
	public void convertTo() throws Exception {
		Flux<String> flux = Flux.just("foo", "bar");
		when(mockBody.convertTo(String.class)).thenReturn(flux);

		assertSame(flux, wrapper.convertTo(String.class));
	}

	@Test
	public void convertToMono() throws Exception {
		Mono<String> mono = Mono.just("foo");
		when(mockBody.convertToMono(String.class)).thenReturn(mono);

		assertSame(mono, wrapper.convertToMono(String.class));
	}

}