/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.MultipartConfigElement;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Test access to parts of a multipart request with {@link RequestPart}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 */
class RequestPartIntegrationTests {

	private RestTemplate restTemplate;

	private static Server server;

	private static String baseUrl;

	private static Path tempDirectory;


	@BeforeAll
	static void startServer() throws Exception {
		// Let server pick its own random, available port.
		server = new Server(0);

		tempDirectory = Files.createTempDirectory("RequestPartIntegrationTests");

		ServletContextHandler handler = new ServletContextHandler();
		handler.setContextPath("/");
		ServletHolder standardResolverServlet = new ServletHolder(DispatcherServlet.class);
		standardResolverServlet.setInitParameter("contextConfigLocation", StandardMultipartResolverTestConfig.class.getName());
		standardResolverServlet.setInitParameter("contextClass", AnnotationConfigWebApplicationContext.class.getName());
		standardResolverServlet.getRegistration().setMultipartConfig(new MultipartConfigElement(tempDirectory.toString()));
		handler.addServlet(standardResolverServlet, "/standard-resolver/*");

		server.setHandler(handler);
		server.start();

		Connector[] connectors = server.getConnectors();
		NetworkConnector connector = (NetworkConnector) connectors[0];
		baseUrl = "http://localhost:" + connector.getLocalPort();
	}

	@AfterAll
	static void stopServer() throws Exception {
		try {
			if (server != null) {
				server.stop();
			}
		}
		finally {
			FileSystemUtils.deleteRecursively(tempDirectory);
		}
	}

	@BeforeEach
	void setup() {
		ByteArrayHttpMessageConverter emptyBodyConverter = new ByteArrayHttpMessageConverter();
		emptyBodyConverter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));

		List<HttpMessageConverter<?>> converters = new ArrayList<>(3);
		converters.add(emptyBodyConverter);
		converters.add(new ByteArrayHttpMessageConverter());
		converters.add(new ResourceHttpMessageConverter());
		converters.add(new JacksonJsonHttpMessageConverter());

		AllEncompassingFormHttpMessageConverter converter = new AllEncompassingFormHttpMessageConverter();
		converter.setPartConverters(converters);

		restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
		restTemplate.setMessageConverters(Collections.singletonList(converter));
	}


	@Test
	void standardMultipartResolver() {
		testCreate(baseUrl + "/standard-resolver/test", "Jason");
		testCreate(baseUrl + "/standard-resolver/test", "Arjen");
	}

	@Test  // SPR-13319
	void standardMultipartResolverWithEncodedFileName() {
		String boundaryText = MimeTypeUtils.generateMultipartBoundaryString();
		Map<String, String> params = Collections.singletonMap("boundary", boundaryText);

		String content =
				"--" + boundaryText + "\r\n" +
				"Content-Disposition: form-data; name=\"file\"; filename*=\"utf-8''%C3%A9l%C3%A8ve.txt\"\r\n" +
				"Content-Type: text/plain\r\n" +
				"Content-Length: 7\r\n" +
				"\r\n" +
				"content\r\n" +
				"--" + boundaryText + "--\r\n ";

		RequestEntity<byte[]> requestEntity =
				RequestEntity.post(URI.create(baseUrl + "/standard-resolver/spr13319"))
						.contentType(new MediaType(MediaType.MULTIPART_FORM_DATA, params))
						.body(content.getBytes(StandardCharsets.US_ASCII));

		ByteArrayHttpMessageConverter converter = new ByteArrayHttpMessageConverter();
		converter.setSupportedMediaTypes(Collections.singletonList(MediaType.MULTIPART_FORM_DATA));
		this.restTemplate.setMessageConverters(Collections.singletonList(converter));

		ResponseEntity<Void> responseEntity = restTemplate.exchange(requestEntity, Void.class);
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	private void testCreate(String url, String basename) {
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("json-data", new HttpEntity<>(new TestData(basename)));
		parts.add("file-data", new ClassPathResource("logo.jpg", getClass()));
		parts.add("empty-data", new HttpEntity<>(new byte[0])); // SPR-12860

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType("application", "octet-stream", StandardCharsets.ISO_8859_1));
		parts.add("iso-8859-1-data", new HttpEntity<>(new byte[] {(byte) 0xC4}, headers)); // SPR-13096

		URI location = restTemplate.postForLocation(url, parts);
		assertThat(location.toString()).isEqualTo(("http://localhost:8080/test/" + basename + "/logo.jpg"));
	}


	@Configuration
	@EnableWebMvc
	static class RequestPartTestConfig implements WebMvcConfigurer {

		@Bean
		public RequestPartTestController controller() {
			return new RequestPartTestController();
		}
	}


	@Configuration
	@SuppressWarnings("unused")
	static class StandardMultipartResolverTestConfig extends RequestPartTestConfig {

		@Bean
		public MultipartResolver multipartResolver() {
			return new StandardServletMultipartResolver();
		}
	}


	@Controller
	@SuppressWarnings("unused")
	private static class RequestPartTestController {

		@RequestMapping(value = "/test", method = POST, consumes = {"multipart/mixed", "multipart/form-data"})
		public ResponseEntity<Object> create(@RequestPart(name = "json-data") TestData testData,
				@RequestPart("file-data") Optional<MultipartFile> file,
				@RequestPart(name = "empty-data", required = false) TestData emptyData,
				@RequestPart(name = "iso-8859-1-data") byte[] iso88591Data) {

			assertThat(iso88591Data).isEqualTo(new byte[]{(byte) 0xC4});

			String url = "http://localhost:8080/test/" + testData.getName() + "/" + file.get().getOriginalFilename();
			HttpHeaders headers = new HttpHeaders();
			headers.setLocation(URI.create(url));
			return new ResponseEntity<>(headers, HttpStatus.CREATED);
		}

		@RequestMapping(value = "/spr13319", method = POST, consumes = "multipart/form-data")
		public ResponseEntity<Void> create(@RequestPart("file") MultipartFile multipartFile) {
			assertThat(multipartFile.getOriginalFilename()).isEqualTo("élève.txt");
			return ResponseEntity.ok().build();
		}
	}


	@SuppressWarnings("unused")
	private static class TestData {

		private String name;

		public TestData() {
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
