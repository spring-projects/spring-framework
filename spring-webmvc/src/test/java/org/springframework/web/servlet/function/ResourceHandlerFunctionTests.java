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

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class ResourceHandlerFunctionTests {

	private final Resource resource = new ClassPathResource("response.txt", getClass());

	private final ResourceHandlerFunction handlerFunction = new ResourceHandlerFunction(this.resource);

	private ServerResponse.Context context;

	private ResourceHttpMessageConverter messageConverter;

	@Before
	public void createContext() {
		this.messageConverter = new ResourceHttpMessageConverter();
		this.context = new ServerResponse.Context() {
			@Override
			public List<HttpMessageConverter<?>> messageConverters() {
				return Collections.singletonList(messageConverter);
			}

		};
	}


	@Test
	public void get() throws IOException, ServletException {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/");
		ServerRequest request = new DefaultServerRequest(servletRequest, Collections.singletonList(messageConverter));

		ServerResponse response = this.handlerFunction.handle(request);
		assertEquals(HttpStatus.OK, response.statusCode());
		assertTrue(response instanceof EntityResponse);
		@SuppressWarnings("unchecked")
		EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
		assertEquals(this.resource, entityResponse.entity());

		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		ModelAndView mav = response.writeTo(servletRequest, servletResponse, this.context);
		assertNull(mav);

		assertEquals(200, servletResponse.getStatus());
		byte[] expectedBytes = Files.readAllBytes(this.resource.getFile().toPath());
		byte[] actualBytes = servletResponse.getContentAsByteArray();
		assertArrayEquals(expectedBytes, actualBytes);
		assertEquals(MediaType.TEXT_PLAIN_VALUE, servletResponse.getContentType());
		assertEquals(this.resource.contentLength(),servletResponse.getContentLength());
	}

	@Test
	public void head() throws IOException, ServletException {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("HEAD", "/");
		ServerRequest request = new DefaultServerRequest(servletRequest, Collections.singletonList(messageConverter));

		ServerResponse response = this.handlerFunction.handle(request);
		assertEquals(HttpStatus.OK, response.statusCode());
		assertTrue(response instanceof EntityResponse);
		@SuppressWarnings("unchecked")
		EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
		assertEquals(this.resource.getFilename(), entityResponse.entity().getFilename());


		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		ModelAndView mav = response.writeTo(servletRequest, servletResponse, this.context);
		assertNull(mav);

		assertEquals(200, servletResponse.getStatus());
		byte[] actualBytes = servletResponse.getContentAsByteArray();
		assertEquals(0, actualBytes.length);
		assertEquals(MediaType.TEXT_PLAIN_VALUE, servletResponse.getContentType());
		assertEquals(this.resource.contentLength(),servletResponse.getContentLength());
	}


	@Test
	public void options() throws ServletException, IOException {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("OPTIONS", "/");
		ServerRequest request = new DefaultServerRequest(servletRequest, Collections.singletonList(messageConverter));

		ServerResponse response = this.handlerFunction.handle(request);
		assertEquals(HttpStatus.OK, response.statusCode());
		assertEquals(EnumSet.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS), response.headers().getAllow());

		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		ModelAndView mav = response.writeTo(servletRequest, servletResponse, this.context);
		assertNull(mav);

		assertEquals(200, servletResponse.getStatus());
		assertEquals("GET,HEAD,OPTIONS", servletResponse.getHeader("Allow"));
		byte[] actualBytes = servletResponse.getContentAsByteArray();
		assertEquals(0, actualBytes.length);
	}

}
