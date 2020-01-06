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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.web.servlet.ModelAndView;
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
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
		boolean condition = response instanceof EntityResponse;
		assertThat(condition).isTrue();
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
	public void head() throws IOException, ServletException {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("HEAD", "/");
		ServerRequest request = new DefaultServerRequest(servletRequest, Collections.singletonList(messageConverter));

		ServerResponse response = this.handlerFunction.handle(request);
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
		boolean condition = response instanceof EntityResponse;
		assertThat(condition).isTrue();
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
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("OPTIONS", "/");
		ServerRequest request = new DefaultServerRequest(servletRequest, Collections.singletonList(messageConverter));

		ServerResponse response = this.handlerFunction.handle(request);
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.headers().getAllow()).isEqualTo(EnumSet.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS));

		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		ModelAndView mav = response.writeTo(servletRequest, servletResponse, this.context);
		assertThat(mav).isNull();

		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getHeader("Allow")).isEqualTo("GET,HEAD,OPTIONS");
		byte[] actualBytes = servletResponse.getContentAsByteArray();
		assertThat(actualBytes.length).isEqualTo(0);
	}

}
