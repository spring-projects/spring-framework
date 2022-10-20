/*
 * Copyright 2002-2020 the original author or authors.
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
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.PathPatternsTestUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class ResourceHandlerFunctionTests {

	private final Resource resource = new ClassPathResource("response.txt", getClass());

	private final ResourceHandlerFunction handlerFunction = new ResourceHandlerFunction(this.resource);

	private ServerResponse.Context context;

	private ResourceHttpMessageConverter messageConverter;

	@BeforeEach
	public void createContext() {
		this.messageConverter = new ResourceHttpMessageConverter();
		ResourceRegionHttpMessageConverter regionConverter = new ResourceRegionHttpMessageConverter();
		this.context = () -> Arrays.asList(messageConverter, regionConverter);
	}


	@Test
	public void get() throws IOException, ServletException {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		ServerRequest request = new DefaultServerRequest(servletRequest, Collections.singletonList(messageConverter));

		ServerResponse response = this.handlerFunction.handle(request);
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response).isInstanceOf(EntityResponse.class);
		@SuppressWarnings("unchecked")
		EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
		assertThat(entityResponse.entity()).isEqualTo(this.resource);

		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		ModelAndView mav = response.writeTo(servletRequest, servletResponse, this.context);
		assertThat(mav).isNull();

		assertThat(servletResponse.getStatus()).isEqualTo(200);
		byte[] expectedBytes = Files.readAllBytes(this.resource.getFile().toPath());
		byte[] actualBytes = servletResponse.getContentAsByteArray();
		assertThat(actualBytes).isEqualTo(expectedBytes);
		assertThat(servletResponse.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
		assertThat(servletResponse.getContentLength()).isEqualTo(this.resource.contentLength());
	}

	@Test
	public void getRange() throws IOException, ServletException {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.addHeader("Range", "bytes=0-5");
		ServerRequest request = new DefaultServerRequest(servletRequest, Collections.singletonList(messageConverter));

		ServerResponse response = this.handlerFunction.handle(request);
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response).isInstanceOf(EntityResponse.class);
		@SuppressWarnings("unchecked")
		EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
		assertThat(entityResponse.entity()).isEqualTo(this.resource);

		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		ModelAndView mav = response.writeTo(servletRequest, servletResponse, this.context);
		assertThat(mav).isNull();

		assertThat(servletResponse.getStatus()).isEqualTo(206);
		byte[] expectedBytes = new byte[6];
		try (InputStream is = this.resource.getInputStream()) {
			is.read(expectedBytes);
		}
		byte[] actualBytes = servletResponse.getContentAsByteArray();
		assertThat(actualBytes).isEqualTo(expectedBytes);
		assertThat(servletResponse.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
		assertThat(servletResponse.getContentLength()).isEqualTo(6);
		assertThat(servletResponse.getHeader(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
	}

	@Test
	public void getInvalidRange() throws IOException, ServletException {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
		servletRequest.addHeader("Range", "bytes=0-10, 0-10, 0-10, 0-10, 0-10, 0-10");
		ServerRequest request = new DefaultServerRequest(servletRequest, Collections.singletonList(messageConverter));

		ServerResponse response = this.handlerFunction.handle(request);
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response).isInstanceOf(EntityResponse.class);
		@SuppressWarnings("unchecked")
		EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
		assertThat(entityResponse.entity()).isEqualTo(this.resource);

		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		ModelAndView mav = response.writeTo(servletRequest, servletResponse, this.context);
		assertThat(mav).isNull();

		assertThat(servletResponse.getStatus()).isEqualTo(416);
		byte[] expectedBytes = Files.readAllBytes(this.resource.getFile().toPath());
		byte[] actualBytes = servletResponse.getContentAsByteArray();
		assertThat(actualBytes).isEqualTo(expectedBytes);
		assertThat(servletResponse.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
		assertThat(servletResponse.getContentLength()).isEqualTo(this.resource.contentLength());
		assertThat(servletResponse.getHeader(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
	}

	@Test
	public void head() throws IOException, ServletException {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("HEAD", "/", true);
		ServerRequest request = new DefaultServerRequest(servletRequest, Collections.singletonList(messageConverter));

		ServerResponse response = this.handlerFunction.handle(request);
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response).isInstanceOf(EntityResponse.class);
		@SuppressWarnings("unchecked")
		EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
		assertThat(entityResponse.entity().getFilename()).isEqualTo(this.resource.getFilename());


		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		ModelAndView mav = response.writeTo(servletRequest, servletResponse, this.context);
		assertThat(mav).isNull();

		assertThat(servletResponse.getStatus()).isEqualTo(200);
		byte[] actualBytes = servletResponse.getContentAsByteArray();
		assertThat(actualBytes.length).isEqualTo(0);
		assertThat(servletResponse.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
		assertThat(servletResponse.getContentLength()).isEqualTo(this.resource.contentLength());
	}

	@Test
	public void options() throws ServletException, IOException {
		MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("OPTIONS", "/", true);
		ServerRequest request = new DefaultServerRequest(servletRequest, Collections.singletonList(messageConverter));

		ServerResponse response = this.handlerFunction.handle(request);
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.headers().getAllow()).isEqualTo(Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS));

		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		ModelAndView mav = response.writeTo(servletRequest, servletResponse, this.context);
		assertThat(mav).isNull();

		assertThat(servletResponse.getStatus()).isEqualTo(200);
		String allowHeader = servletResponse.getHeader("Allow");
		String[] methods = StringUtils.tokenizeToStringArray(allowHeader, ",");
		assertThat(methods).containsExactlyInAnyOrder("GET","HEAD","OPTIONS");
		byte[] actualBytes = servletResponse.getContentAsByteArray();
		assertThat(actualBytes.length).isEqualTo(0);
	}

}
