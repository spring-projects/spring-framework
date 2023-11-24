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

package org.springframework.http.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUpload;
import org.apache.tomcat.util.http.fileupload.RequestContext;
import org.apache.tomcat.util.http.fileupload.UploadContext;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.testfixture.http.MockHttpInputMessage;
import org.springframework.web.testfixture.http.MockHttpOutputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.MULTIPART_MIXED;
import static org.springframework.http.MediaType.MULTIPART_RELATED;
import static org.springframework.http.MediaType.TEXT_XML;

/**
 * Unit tests for {@link FormHttpMessageConverter} and
 * {@link AllEncompassingFormHttpMessageConverter}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Sebastien Deleuze
 */
public class FormHttpMessageConverterTests {

	private final FormHttpMessageConverter converter = new AllEncompassingFormHttpMessageConverter();


	@Test
	public void canRead() {
		assertCanRead(MultiValueMap.class, null);
		assertCanRead(APPLICATION_FORM_URLENCODED);

		assertCannotRead(String.class, null);
		assertCannotRead(String.class, APPLICATION_FORM_URLENCODED);
	}

	@Test
	public void cannotReadMultipart() {
		// Without custom multipart types supported
		asssertCannotReadMultipart();

		// Should still be the case with custom multipart types supported
		asssertCannotReadMultipart();
	}

	@Test
	public void canWrite() {
		assertCanWrite(APPLICATION_FORM_URLENCODED);
		assertCanWrite(MULTIPART_FORM_DATA);
		assertCanWrite(MULTIPART_MIXED);
		assertCanWrite(MULTIPART_RELATED);
		assertCanWrite(new MediaType("multipart", "form-data", StandardCharsets.UTF_8));
		assertCanWrite(MediaType.ALL);
		assertCanWrite(null);
	}

	@Test
	public void setSupportedMediaTypes() {
		this.converter.setSupportedMediaTypes(List.of(MULTIPART_FORM_DATA));
		assertCannotWrite(MULTIPART_MIXED);

		this.converter.setSupportedMediaTypes(List.of(MULTIPART_MIXED));
		assertCanWrite(MULTIPART_MIXED);
	}

	@Test
	public void addSupportedMediaTypes() {
		this.converter.setSupportedMediaTypes(List.of(MULTIPART_FORM_DATA));
		assertCannotWrite(MULTIPART_MIXED);

		this.converter.addSupportedMediaTypes(MULTIPART_RELATED);
		assertCanWrite(MULTIPART_RELATED);
	}

	@Test
	public void readForm() throws Exception {
		String body = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.ISO_8859_1));
		inputMessage.getHeaders().setContentType(
				new MediaType("application", "x-www-form-urlencoded", StandardCharsets.ISO_8859_1));
		MultiValueMap<String, String> result = this.converter.read(null, inputMessage);

		assertThat(result).as("Invalid result").hasSize(3);
		assertThat(result.getFirst("name 1")).as("Invalid result").isEqualTo("value 1");
		List<String> values = result.get("name 2");
		assertThat(values).as("Invalid result").hasSize(2);
		assertThat(values.get(0)).as("Invalid result").isEqualTo("value 2+1");
		assertThat(values.get(1)).as("Invalid result").isEqualTo("value 2+2");
		assertThat(result.getFirst("name 3")).as("Invalid result").isNull();
	}

	@Test
	public void writeForm() throws IOException {
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.set("name 1", "value 1");
		body.add("name 2", "value 2+1");
		body.add("name 2", "value 2+2");
		body.add("name 3", null);
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.write(body, APPLICATION_FORM_URLENCODED, outputMessage);

		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8))
				.as("Invalid result").isEqualTo("name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3");
		assertThat(outputMessage.getHeaders().getContentType().toString())
				.as("Invalid content-type").isEqualTo("application/x-www-form-urlencoded;charset=UTF-8");
		assertThat(outputMessage.getHeaders().getContentLength())
				.as("Invalid content-length").isEqualTo(outputMessage.getBodyAsBytes().length);
	}

	@Test
	public void writeMultipart() throws Exception {

		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("name 1", "value 1");
		parts.add("name 2", "value 2+1");
		parts.add("name 2", "value 2+2");
		parts.add("name 3", null);

		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		parts.add("logo", logo);

		// SPR-12108
		Resource utf8 = new ClassPathResource("/org/springframework/http/converter/logo.jpg") {
			@Override
			public String getFilename() {
				return "Hall\u00F6le.jpg";
			}
		};
		parts.add("utf8", utf8);

		MyBean myBean = new MyBean();
		myBean.setString("foo");
		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(APPLICATION_JSON);
		HttpEntity<MyBean> entity = new HttpEntity<>(myBean, entityHeaders);
		parts.add("json", entity);

		Map<String, String> parameters = new LinkedHashMap<>(2);
		parameters.put("charset", StandardCharsets.UTF_8.name());
		parameters.put("foo", "bar");

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.write(parts, new MediaType("multipart", "form-data", parameters), outputMessage);

		final MediaType contentType = outputMessage.getHeaders().getContentType();
		assertThat(contentType.getParameters()).containsKeys("charset", "boundary", "foo"); // gh-21568, gh-25839

		// see if Commons FileUpload can read what we wrote
		FileUpload fileUpload = new FileUpload();
		fileUpload.setFileItemFactory(new DiskFileItemFactory());
		RequestContext requestContext = new MockHttpOutputMessageRequestContext(outputMessage);
		List<FileItem> items = fileUpload.parseRequest(requestContext);
		assertThat(items).hasSize(6);
		FileItem item = items.get(0);
		assertThat(item.isFormField()).isTrue();
		assertThat(item.getFieldName()).isEqualTo("name 1");
		assertThat(item.getString()).isEqualTo("value 1");

		item = items.get(1);
		assertThat(item.isFormField()).isTrue();
		assertThat(item.getFieldName()).isEqualTo("name 2");
		assertThat(item.getString()).isEqualTo("value 2+1");

		item = items.get(2);
		assertThat(item.isFormField()).isTrue();
		assertThat(item.getFieldName()).isEqualTo("name 2");
		assertThat(item.getString()).isEqualTo("value 2+2");

		item = items.get(3);
		assertThat(item.isFormField()).isFalse();
		assertThat(item.getFieldName()).isEqualTo("logo");
		assertThat(item.getName()).isEqualTo("logo.jpg");
		assertThat(item.getContentType()).isEqualTo("image/jpeg");
		assertThat(item.getSize()).isEqualTo(logo.getFile().length());

		item = items.get(4);
		assertThat(item.isFormField()).isFalse();
		assertThat(item.getFieldName()).isEqualTo("utf8");
		assertThat(item.getName()).isEqualTo("Hall\u00F6le.jpg");
		assertThat(item.getContentType()).isEqualTo("image/jpeg");
		assertThat(item.getSize()).isEqualTo(logo.getFile().length());

		item = items.get(5);
		assertThat(item.getFieldName()).isEqualTo("json");
		assertThat(item.getContentType()).isEqualTo("application/json");
	}

	@Test
	public void writeMultipartWithSourceHttpMessageConverter() throws Exception {

		converter.setPartConverters(List.of(
				new StringHttpMessageConverter(),
				new ResourceHttpMessageConverter(),
				new SourceHttpMessageConverter<>()));

		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("name 1", "value 1");
		parts.add("name 2", "value 2+1");
		parts.add("name 2", "value 2+2");
		parts.add("name 3", null);

		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		parts.add("logo", logo);

		// SPR-12108
		Resource utf8 = new ClassPathResource("/org/springframework/http/converter/logo.jpg") {
			@Override
			public String getFilename() {
				return "Hall\u00F6le.jpg";
			}
		};
		parts.add("utf8", utf8);

		Source xml = new StreamSource(new StringReader("<root><child/></root>"));
		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(TEXT_XML);
		HttpEntity<Source> entity = new HttpEntity<>(xml, entityHeaders);
		parts.add("xml", entity);

		Map<String, String> parameters = new LinkedHashMap<>(2);
		parameters.put("charset", StandardCharsets.UTF_8.name());
		parameters.put("foo", "bar");

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.write(parts, new MediaType("multipart", "form-data", parameters), outputMessage);

		final MediaType contentType = outputMessage.getHeaders().getContentType();
		assertThat(contentType.getParameters()).containsKeys("charset", "boundary", "foo"); // gh-21568, gh-25839

		// see if Commons FileUpload can read what we wrote
		FileUpload fileUpload = new FileUpload();
		fileUpload.setFileItemFactory(new DiskFileItemFactory());
		RequestContext requestContext = new MockHttpOutputMessageRequestContext(outputMessage);
		List<FileItem> items = fileUpload.parseRequest(requestContext);
		assertThat(items).hasSize(6);
		FileItem item = items.get(0);
		assertThat(item.isFormField()).isTrue();
		assertThat(item.getFieldName()).isEqualTo("name 1");
		assertThat(item.getString()).isEqualTo("value 1");

		item = items.get(1);
		assertThat(item.isFormField()).isTrue();
		assertThat(item.getFieldName()).isEqualTo("name 2");
		assertThat(item.getString()).isEqualTo("value 2+1");

		item = items.get(2);
		assertThat(item.isFormField()).isTrue();
		assertThat(item.getFieldName()).isEqualTo("name 2");
		assertThat(item.getString()).isEqualTo("value 2+2");

		item = items.get(3);
		assertThat(item.isFormField()).isFalse();
		assertThat(item.getFieldName()).isEqualTo("logo");
		assertThat(item.getName()).isEqualTo("logo.jpg");
		assertThat(item.getContentType()).isEqualTo("image/jpeg");
		assertThat(item.getSize()).isEqualTo(logo.getFile().length());

		item = items.get(4);
		assertThat(item.isFormField()).isFalse();
		assertThat(item.getFieldName()).isEqualTo("utf8");
		assertThat(item.getName()).isEqualTo("Hall\u00F6le.jpg");
		assertThat(item.getContentType()).isEqualTo("image/jpeg");
		assertThat(item.getSize()).isEqualTo(logo.getFile().length());

		item = items.get(5);
		assertThat(item.getFieldName()).isEqualTo("xml");
		assertThat(item.getContentType()).isEqualTo("text/xml");
	}

	@Test  // SPR-13309
	public void writeMultipartOrder() throws Exception {
		MyBean myBean = new MyBean();
		myBean.setString("foo");

		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("part1", myBean);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(TEXT_XML);
		HttpEntity<MyBean> entity = new HttpEntity<>(myBean, entityHeaders);
		parts.add("part2", entity);

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.setMultipartCharset(StandardCharsets.UTF_8);
		this.converter.write(parts, new MediaType("multipart", "form-data", StandardCharsets.UTF_8), outputMessage);

		final MediaType contentType = outputMessage.getHeaders().getContentType();
		assertThat(contentType.getParameter("boundary")).as("No boundary found").isNotNull();

		// see if Commons FileUpload can read what we wrote
		FileUpload fileUpload = new FileUpload();
		fileUpload.setFileItemFactory(new DiskFileItemFactory());
		RequestContext requestContext = new MockHttpOutputMessageRequestContext(outputMessage);
		List<FileItem> items = fileUpload.parseRequest(requestContext);
		assertThat(items).hasSize(2);

		FileItem item = items.get(0);
		assertThat(item.isFormField()).isTrue();
		assertThat(item.getFieldName()).isEqualTo("part1");
		assertThat(item.getString()).isEqualTo("{\"string\":\"foo\"}");

		item = items.get(1);
		assertThat(item.isFormField()).isTrue();
		assertThat(item.getFieldName()).isEqualTo("part2");

		// With developer builds we get: <MyBean><string>foo</string></MyBean>
		// But on CI server we get: <MyBean xmlns=""><string>foo</string></MyBean>
		// So... we make a compromise:
		assertThat(item.getString())
				.startsWith("<MyBean")
				.endsWith("><string>foo</string></MyBean>");
	}

	@Test
	public void writeMultipartCharset() throws Exception {
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		parts.add("logo", logo);

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.write(parts, MULTIPART_FORM_DATA, outputMessage);

		MediaType contentType = outputMessage.getHeaders().getContentType();
		Map<String, String> parameters = contentType.getParameters();
		assertThat(parameters).containsOnlyKeys("boundary");

		this.converter.setCharset(StandardCharsets.ISO_8859_1);

		outputMessage = new MockHttpOutputMessage();
		this.converter.write(parts, MULTIPART_FORM_DATA, outputMessage);

		parameters = outputMessage.getHeaders().getContentType().getParameters();
		assertThat(parameters).containsOnlyKeys("boundary", "charset");
		assertThat(parameters).containsEntry("charset", "ISO-8859-1");
	}

	private void assertCanRead(MediaType mediaType) {
		assertCanRead(MultiValueMap.class, mediaType);
	}

	private void assertCanRead(Class<?> clazz, MediaType mediaType) {
		assertThat(this.converter.canRead(clazz, mediaType)).as(clazz.getSimpleName() + " : " + mediaType).isTrue();
	}

	private void asssertCannotReadMultipart() {
		assertCannotRead(new MediaType("multipart", "*"));
		assertCannotRead(MULTIPART_FORM_DATA);
		assertCannotRead(MULTIPART_MIXED);
		assertCannotRead(MULTIPART_RELATED);
	}

	private void assertCannotRead(MediaType mediaType) {
		assertCannotRead(MultiValueMap.class, mediaType);
	}

	private void assertCannotRead(Class<?> clazz, MediaType mediaType) {
		assertThat(this.converter.canRead(clazz, mediaType)).as(clazz.getSimpleName() + " : " + mediaType).isFalse();
	}

	private void assertCanWrite(MediaType mediaType) {
		Class<?> clazz = MultiValueMap.class;
		assertThat(this.converter.canWrite(clazz, mediaType)).as(clazz.getSimpleName() + " : " + mediaType).isTrue();
	}

	private void assertCannotWrite(MediaType mediaType) {
		Class<?> clazz = MultiValueMap.class;
		assertThat(this.converter.canWrite(clazz, mediaType)).as(clazz.getSimpleName() + " : " + mediaType).isFalse();
	}


	private static class MockHttpOutputMessageRequestContext implements UploadContext {

		private final MockHttpOutputMessage outputMessage;

		private final byte[] body;

		private MockHttpOutputMessageRequestContext(MockHttpOutputMessage outputMessage) {
			this.outputMessage = outputMessage;
			this.body = this.outputMessage.getBodyAsBytes();
		}

		@Override
		public String getCharacterEncoding() {
			MediaType type = this.outputMessage.getHeaders().getContentType();
			return (type != null && type.getCharset() != null ? type.getCharset().name() : null);
		}

		@Override
		public String getContentType() {
			MediaType type = this.outputMessage.getHeaders().getContentType();
			return (type != null ? type.toString() : null);
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(body);
		}

		@Override
		public long contentLength() {
			return body.length;
		}
	}


	public static class MyBean {

		private String string;

		public String getString() {
			return this.string;
		}

		public void setString(String string) {
			this.string = string;
		}
	}

}
