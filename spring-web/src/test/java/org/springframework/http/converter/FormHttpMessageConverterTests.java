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

package org.springframework.http.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.MULTIPART_MIXED;
import static org.springframework.http.MediaType.TEXT_XML;

/**
 * Unit tests for {@link FormHttpMessageConverter} and
 * {@link AllEncompassingFormHttpMessageConverter}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class FormHttpMessageConverterTests {

	private static final MediaType MULTIPART_RELATED = new MediaType("multipart", "related");

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

		this.converter.addSupportedMediaTypes(MULTIPART_RELATED);

		// Should still be the case with custom multipart types supported
		asssertCannotReadMultipart();
	}

	@Test
	public void canWrite() {
		assertCanWrite(APPLICATION_FORM_URLENCODED);
		assertCanWrite(MULTIPART_FORM_DATA);
		assertCanWrite(MULTIPART_MIXED);
		assertCanWrite(new MediaType("multipart", "form-data", StandardCharsets.UTF_8));
		assertCanWrite(MediaType.ALL);
		assertCanWrite(null);
	}

	@Test
	public void setSupportedMediaTypes() {
		assertCannotWrite(MULTIPART_RELATED);

		List<MediaType> supportedMediaTypes = new ArrayList<>(this.converter.getSupportedMediaTypes());
		supportedMediaTypes.add(MULTIPART_RELATED);
		this.converter.setSupportedMediaTypes(supportedMediaTypes);

		assertCanWrite(MULTIPART_RELATED);
	}

	@Test
	public void addSupportedMediaTypes() {
		assertCannotWrite(MULTIPART_RELATED);

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

		assertThat(result.size()).as("Invalid result").isEqualTo(3);
		assertThat(result.getFirst("name 1")).as("Invalid result").isEqualTo("value 1");
		List<String> values = result.get("name 2");
		assertThat(values.size()).as("Invalid result").isEqualTo(2);
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

		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).as("Invalid result").isEqualTo("name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3");
		assertThat(outputMessage.getHeaders().getContentType().toString()).as("Invalid content-type").isEqualTo("application/x-www-form-urlencoded;charset=UTF-8");
		assertThat(outputMessage.getHeaders().getContentLength()).as("Invalid content-length").isEqualTo(outputMessage.getBodyAsBytes().length);
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
		FileItemFactory fileItemFactory = new DiskFileItemFactory();
		FileUpload fileUpload = new FileUpload(fileItemFactory);
		RequestContext requestContext = new MockHttpOutputMessageRequestContext(outputMessage);
		List<FileItem> items = fileUpload.parseRequest(requestContext);
		assertThat(items.size()).isEqualTo(6);
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
		verify(outputMessage.getBody(), never()).close();
	}

	@Test // SPR-13309
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
		FileItemFactory fileItemFactory = new DiskFileItemFactory();
		FileUpload fileUpload = new FileUpload(fileItemFactory);
		RequestContext requestContext = new MockHttpOutputMessageRequestContext(outputMessage);
		List<FileItem> items = fileUpload.parseRequest(requestContext);
		assertThat(items.size()).isEqualTo(2);

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


	private static class MockHttpOutputMessageRequestContext implements RequestContext {

		private final MockHttpOutputMessage outputMessage;


		private MockHttpOutputMessageRequestContext(MockHttpOutputMessage outputMessage) {
			this.outputMessage = outputMessage;
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
		@Deprecated
		public int getContentLength() {
			return this.outputMessage.getBodyAsBytes().length;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(this.outputMessage.getBodyAsBytes());
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
