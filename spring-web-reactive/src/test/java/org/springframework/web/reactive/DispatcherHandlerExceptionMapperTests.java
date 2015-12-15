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
package org.springframework.web.reactive;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.ResponseStatusException;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.Assert.assertEquals;

/**
 * @author Rossen Stoyanchev
 */
public class DispatcherHandlerExceptionMapperTests {

	private DispatcherHandlerExceptionMapper mapper;


	@Before
	public void setUp() throws Exception {
		this.mapper = new DispatcherHandlerExceptionMapper();
	}


	@Test
	public void handlerNotFound() throws Exception {
		Throwable ex = new HandlerNotFoundException(HttpMethod.GET, "/path", new HttpHeaders());
		ex = this.mapper.apply(ex);

		assertEquals(ResponseStatusException.class, ex.getClass());
		assertEquals(HttpStatus.NOT_FOUND, ((ResponseStatusException) ex).getHttpStatus());
	}


	@Test
	public void httpMediaTypeNotAcceptable() throws Exception {
		Throwable ex = new HttpMediaTypeNotAcceptableException(Collections.emptyList());
		ex = this.mapper.apply(ex);

		assertEquals(ResponseStatusException.class, ex.getClass());
		assertEquals(HttpStatus.NOT_ACCEPTABLE, ((ResponseStatusException) ex).getHttpStatus());
	}

	@Test
	public void responseStatusAnnotation() throws Exception {
		Throwable ex = new ResponseStatusAnnotatedException();
		ex = this.mapper.apply(ex);

		assertEquals(ResponseStatusException.class, ex.getClass());
		assertEquals(HttpStatus.BAD_REQUEST, ((ResponseStatusException) ex).getHttpStatus());
	}

	@Test
	public void responseStatusAnnotationOnRootCause() throws Exception {
		Throwable ex = new Exception(new ResponseStatusAnnotatedException());
		ex = this.mapper.apply(ex);

		assertEquals(ResponseStatusException.class, ex.getClass());
		assertEquals(HttpStatus.BAD_REQUEST, ((ResponseStatusException) ex).getHttpStatus());
	}


	@ResponseStatus(code = HttpStatus.BAD_REQUEST)
	private static class ResponseStatusAnnotatedException extends Exception {
	}

}
