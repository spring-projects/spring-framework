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

package org.springframework.web.servlet.mvc.method.annotation;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.MultipartConfigElement;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.SocketUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Test access to parts of a multipart request with {@link RequestPart}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class RequestPartIntegrationTests {

	private RestTemplate restTemplate;

	private static Server server;

	private static String baseUrl;


	@BeforeClass
	public static void startServer() throws Exception {
		int port = SocketUtils.findAvailableTcpPort();
		baseUrl = "http://localhost:" + port;

		server = new Server(port);
		ServletContextHandler handler = new ServletContextHandler();
		handler.setContextPath("/");

		Class<?> config = CommonsMultipartResolverTestConfig.class;
		ServletHolder commonsResolverServlet = new ServletHolder(DispatcherServlet.class);
		commonsResolverServlet.setInitParameter("contextConfigLocation", config.getName());
		commonsResolverServlet.setInitParameter("contextClass", AnnotationConfigWebApplicationContext.class.getName());
		handler.addServlet(commonsResolverServlet, "/commons-resolver/*");

		config = StandardMultipartResolverTestConfig.class;
		ServletHolder standardResolverServlet = new ServletHolder(DispatcherServlet.class);
		standardResolverServlet.setInitParameter("contextConfigLocation", config.getName());
		standardResolverServlet.setInitParameter("contextClass", AnnotationConfigWebApplicationContext.class.getName());
		standardResolverServlet.getRegistration().setMultipartConfig(new MultipartConfigElement(""));
		handler.addServlet(standardResolverServlet, "/standard-resolver/*");

		server.setHandler(handler);
		server.start();
	}

	@Before
	public void setUp() {
		ByteArrayHttpMessageConverter emptyBodyConverter = new ByteArrayHttpMessageConverter();
		emptyBodyConverter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON));

		List<HttpMessageConverter<?>> converters = new ArrayList<>(3);
		converters.add(emptyBodyConverter);
		converters.add(new ResourceHttpMessageConverter());
		converters.add(new MappingJackson2HttpMessageConverter());

		AllEncompassingFormHttpMessageConverter converter = new AllEncompassingFormHttpMessageConverter();
		converter.setPartConverters(converters);

		restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
		restTemplate.setMessageConverters(Arrays.<HttpMessageConverter<?>>asList(converter));
	}

	@AfterClass
	public static void stopServer() throws Exception {
		if (server != null) {
			server.stop();
		}
	}


	@Test
	public void commonsMultipartResolver() throws Exception {
		testCreate(baseUrl + "/commons-resolver/test");
	}

	@Test
	public void standardMultipartResolver() throws Exception {
		testCreate(baseUrl + "/standard-resolver/test");
	}

	private void testCreate(String url) {
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
		parts.add("json-data", new HttpEntity<TestData>(new TestData("Jason")));
		parts.add("file-data", new ClassPathResource("logo.jpg", this.getClass()));
		parts.add("empty-data", new HttpEntity<byte[]>(new byte[0])); // SPR-12860

		URI location = restTemplate.postForLocation(url, parts);
		assertEquals("http://localhost:8080/test/Jason/logo.jpg", location.toString());
	}


	@Configuration
	@EnableWebMvc
	static class RequestPartTestConfig extends WebMvcConfigurerAdapter {

		@Bean
		public RequestPartTestController controller() {
			return new RequestPartTestController();
		}
	}

	@Configuration
	static class CommonsMultipartResolverTestConfig extends RequestPartTestConfig {

		@Bean
		public MultipartResolver multipartResolver() {
			return new CommonsMultipartResolver();
		}
	}

	@Configuration
	static class StandardMultipartResolverTestConfig extends RequestPartTestConfig {

		@Bean
		public MultipartResolver multipartResolver() {
			return new StandardServletMultipartResolver();
		}
	}

	@SuppressWarnings("unused")
	@Controller
	private static class RequestPartTestController {

		@RequestMapping(value = "/test", method = RequestMethod.POST, consumes = { "multipart/mixed", "multipart/form-data" })
		public ResponseEntity<Object> create(@RequestPart("json-data") TestData testData,
				@RequestPart("file-data") MultipartFile file,
				@RequestPart(value = "empty-data", required = false) TestData emptyData) {

			String url = "http://localhost:8080/test/" + testData.getName() + "/" + file.getOriginalFilename();
			HttpHeaders headers = new HttpHeaders();
			headers.setLocation(URI.create(url));
			return new ResponseEntity<Object>(headers, HttpStatus.CREATED);
		}
	}

	@SuppressWarnings("unused")
	private static class TestData {

		private String name;

		public TestData() {
			super();
		}

		public TestData(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
