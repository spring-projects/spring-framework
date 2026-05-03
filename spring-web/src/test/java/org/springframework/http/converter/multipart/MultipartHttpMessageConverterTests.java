/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.http.converter.multipart;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileUpload;
import org.apache.tomcat.util.http.fileupload.RequestContext;
import org.apache.tomcat.util.http.fileupload.UploadContext;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.testfixture.http.MockHttpInputMessage;
import org.springframework.web.testfixture.http.MockHttpOutputMessage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.MULTIPART_MIXED;
import static org.springframework.http.MediaType.MULTIPART_RELATED;
import static org.springframework.http.MediaType.TEXT_XML;


/**
 * Tests for {@link MultipartHttpMessageConverter}.
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Sebastien Deleuze
 */
class MultipartHttpMessageConverterTests {

	private MultipartHttpMessageConverter converter = new MultipartHttpMessageConverter(
			List.of(new StringHttpMessageConverter(), new ByteArrayHttpMessageConverter(),
					new ResourceHttpMessageConverter(), new JacksonJsonHttpMessageConverter())
	);


	@Test
	void canRead() {
		assertCanRead(MULTIPART_FORM_DATA);
		assertCanRead(MULTIPART_MIXED);
		assertCanRead(MULTIPART_RELATED);
		assertCanRead(ResolvableType.forClass(LinkedMultiValueMap.class), MULTIPART_FORM_DATA);
		assertCanRead(ResolvableType.forClassWithGenerics(LinkedMultiValueMap.class, String.class, Part.class), MULTIPART_FORM_DATA);

		assertCannotRead(ResolvableType.forClassWithGenerics(LinkedMultiValueMap.class, String.class, Object.class), MULTIPART_FORM_DATA);
	}

	@Test
	void canWrite() {
		assertCanWrite(MULTIPART_FORM_DATA);
		assertCanWrite(MULTIPART_MIXED);
		assertCanWrite(MULTIPART_RELATED);
		assertCanWrite(new MediaType("multipart", "form-data", UTF_8));
		assertCanWrite(MediaType.ALL);
		assertCanWrite(null);
		assertCanWrite(ResolvableType.forClassWithGenerics(LinkedMultiValueMap.class, String.class, Object.class), MULTIPART_FORM_DATA);
	}

	@Test
	void setSupportedMediaTypes() {
		this.converter.setSupportedMediaTypes(List.of(MULTIPART_FORM_DATA));
		assertCannotWrite(MULTIPART_MIXED);

		this.converter.setSupportedMediaTypes(List.of(MULTIPART_MIXED));
		assertCanWrite(MULTIPART_MIXED);
	}

	@Test
	void addSupportedMediaTypes() {
		this.converter.setSupportedMediaTypes(List.of(MULTIPART_FORM_DATA));
		assertCannotWrite(MULTIPART_MIXED);

		this.converter.addSupportedMediaTypes(MULTIPART_RELATED);
		assertCanWrite(MULTIPART_RELATED);
	}

	@Test
	void applyDefaultCharsetToPartConverters() {
		this.converter.getPartConverters().forEach(converter -> {
			if (converter instanceof AbstractHttpMessageConverter<?> abstractConverter) {
				assertThat(abstractConverter.getDefaultCharset()).isIn(null, StandardCharsets.UTF_8);
			}
		});
	}

	@Test
	void customCharsetAppliedToPartConverters() {
		this.converter.setCharset(StandardCharsets.UTF_16);
		this.converter.getPartConverters().forEach(converter -> {
			if (converter instanceof AbstractHttpMessageConverter<?> abstractConverter) {
				assertThat(abstractConverter.getDefaultCharset()).isIn(null, StandardCharsets.UTF_16);
			}
		});
	}


	private void assertCanRead(MediaType mediaType) {
		assertCanRead(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class), mediaType);
	}

	private void assertCanRead(ResolvableType type, MediaType mediaType) {
		assertThat(this.converter.canRead(type, mediaType)).as(type + " : " + mediaType).isTrue();
	}

	private void assertCannotRead(ResolvableType type, MediaType mediaType) {
		assertThat(this.converter.canRead(type, mediaType)).as(type + " : " + mediaType).isFalse();
	}

	private void assertCanWrite(ResolvableType type, MediaType mediaType) {
		assertThat(this.converter.canWrite(type, MultiValueMap.class, mediaType)).as(type + " : " + mediaType).isTrue();
	}

	private void assertCanWrite(MediaType mediaType) {
		assertCanWrite(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class), mediaType);
	}

	private void assertCannotWrite(MediaType mediaType) {
		Class<?> clazz = MultiValueMap.class;
		assertThat(this.converter.canWrite(clazz, mediaType)).as(clazz.getSimpleName() + " : " + mediaType).isFalse();
	}


	@Nested
	class ReadingTests {

		@Test
		void readMultipartFiles() throws Exception {
			MockHttpInputMessage response = createMultipartResponse("files.multipart", "----WebKitFormBoundaryG8fJ50opQOML0oGD");
			MultiValueMap<String, Part> result = converter.read(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class), response, null);

			assertThat(result).containsOnlyKeys("file2");
			assertThat(result.get("file2")).anyMatch(isFilePart("a.txt"))
					.anyMatch(isFilePart("b.txt"));
		}

		@Test
		void readMultipartBrowser() throws Exception {
			MockHttpInputMessage response = createMultipartResponse("firefox.multipart", "---------------------------18399284482060392383840973206");
			MultiValueMap<String, Part> result = converter.read(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class), response, null);

			assertThat(result).containsOnlyKeys("file1", "file2", "text1", "text2");
			assertThat(result.get("file1")).anyMatch(isFilePart("a.txt"));
			assertThat(result.get("file2")).anyMatch(isFilePart("a.txt"))
					.anyMatch(isFilePart("b.txt"));
			assertThat(result.get("text1")).anyMatch(isFormData("text1", "a"));
			assertThat(result.get("text2")).anyMatch(isFormData("text2", "b"));
		}

		@Test
		void readMultipartInvalid() throws Exception {
			MockHttpInputMessage response = createMultipartResponse("garbage-1.multipart", "boundary");
			assertThatThrownBy(() -> converter.read(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class), response, null))
					.isInstanceOf(HttpMessageConversionException.class).hasMessage("Cannot decode multipart body");
		}

		@Test
		void readMultipartMaxPartsExceeded() throws Exception {
			MockHttpInputMessage response = createMultipartResponse("files.multipart", "----WebKitFormBoundaryG8fJ50opQOML0oGD");
			converter.setMaxParts(1);
			assertThatThrownBy(() -> converter.read(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class), response, null))
					.isInstanceOf(HttpMessageConversionException.class).hasMessage("Maximum number of parts exceeded: 1");
		}

		@Test
		void readMultipartToFiles() throws Exception {
			MockHttpInputMessage response = createMultipartResponse("files.multipart", "----WebKitFormBoundaryG8fJ50opQOML0oGD");
			converter.setMaxInMemorySize(1);
			MultiValueMap<String, Part> result = converter.read(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class), response, null);
			assertThat(result).containsOnlyKeys("file2");
		}

		@Test
		void readMultipartMaxInMemoryExceeded() throws Exception {
			MockHttpInputMessage response = createMultipartResponse("firefox.multipart", "---------------------------18399284482060392383840973206");
			converter.setMaxInMemorySize(1);
			assertThatThrownBy(() -> converter.read(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class), response, null))
					.isInstanceOf(HttpMessageConversionException.class).hasMessage("Form field value exceeded the memory usage limit of 1 bytes");
		}

		@Test
		void readMultipartMaxDiskUsageExceeded() throws Exception {
			MockHttpInputMessage response = createMultipartResponse("firefox.multipart", "---------------------------18399284482060392383840973206");
			converter.setMaxInMemorySize(30);
			converter.setMaxDiskUsagePerPart(35);
			assertThatThrownBy(() -> converter.read(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class), response, null))
					.isInstanceOf(HttpMessageConversionException.class).hasMessage("Part exceeded the disk usage limit of 35 bytes");
		}

		@Test
		void readMultipartUnnamedPart() throws Exception {
			MockHttpInputMessage response = createMultipartResponse("simple.multipart", "simple-boundary");
			assertThatThrownBy(() -> converter.read(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class), response, null))
					.isInstanceOf(HttpMessageConversionException.class).hasMessage("Part #1 is unnamed");
		}


		private MockHttpInputMessage createMultipartResponse(String fileName, String boundary) throws Exception {
			InputStream stream = createStream(fileName);
			MockHttpInputMessage response = new MockHttpInputMessage(stream);
			response.getHeaders().setContentType(
					new MediaType("multipart", "form-data", singletonMap("boundary", boundary)));
			return response;
		}

		private InputStream createStream(String fileName) throws IOException {
			Resource resource = new ClassPathResource("/org/springframework/http/multipart/" + fileName);
			return resource.getInputStream();
		}

		private Predicate<Part> isFilePart(String fileName) {
			return part -> part instanceof FilePart filePart &&
					filePart.filename().equals(fileName);
		}

		private Predicate<Part> isFormData(String name, String value) {
			return part -> part instanceof FormFieldPart formFieldPart &&
					formFieldPart.name().equals(name) &&
					formFieldPart.value().equals(value);
		}

	}

	@Nested
	class WritingTests {

		@Test
		void writeMultipart() throws Exception {
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
			parameters.put("charset", UTF_8.name());
			parameters.put("foo", "bar");

			StreamingMockHttpOutputMessage outputMessage = new StreamingMockHttpOutputMessage();
			converter.write(parts, new MediaType("multipart", "form-data", parameters), outputMessage);

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

			assertThat(outputMessage.wasRepeatable()).isTrue();
		}

		@Test
		void writeMultipartWithSourceHttpMessageConverter() throws Exception {

			converter = new MultipartHttpMessageConverter(List.of(
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
			parameters.put("charset", UTF_8.name());
			parameters.put("foo", "bar");

			StreamingMockHttpOutputMessage outputMessage = new StreamingMockHttpOutputMessage();
			converter.write(parts, new MediaType("multipart", "form-data", parameters), outputMessage);

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

			assertThat(outputMessage.wasRepeatable()).isFalse();
		}

		@Test  // SPR-13309
		void writeMultipartOrder() throws Exception {
			MyBean myBean = new MyBean();
			myBean.setString("foo");

			MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
			parts.add("part1", myBean);

			HttpHeaders entityHeaders = new HttpHeaders();
			entityHeaders.setContentType(APPLICATION_JSON);
			HttpEntity<MyBean> entity = new HttpEntity<>(myBean, entityHeaders);
			parts.add("part2", entity);

			MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
			converter.setMultipartCharset(UTF_8);
			converter.write(parts, new MediaType("multipart", "form-data", UTF_8), outputMessage);

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

			assertThat(item.getString())
					.contains("{\"string\":\"foo\"}");
		}

		@Test
		void writeMultipartCharset() throws Exception {
			MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
			Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
			parts.add("logo", logo);

			MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
			converter.write(parts, MULTIPART_FORM_DATA, outputMessage);

			MediaType contentType = outputMessage.getHeaders().getContentType();
			Map<String, String> parameters = contentType.getParameters();
			assertThat(parameters).containsOnlyKeys("boundary");

			converter.setCharset(StandardCharsets.ISO_8859_1);

			outputMessage = new MockHttpOutputMessage();
			converter.write(parts, MULTIPART_FORM_DATA, outputMessage);

			parameters = outputMessage.getHeaders().getContentType().getParameters();
			assertThat(parameters).containsOnlyKeys("boundary", "charset");
			assertThat(parameters).containsEntry("charset", "ISO-8859-1");
		}

	}


	private static class StreamingMockHttpOutputMessage extends MockHttpOutputMessage implements StreamingHttpOutputMessage {

		private boolean repeatable;

		public boolean wasRepeatable() {
			return this.repeatable;
		}

		@Override
		public void setBody(Body body) {
			try {
				this.repeatable = body.repeatable();
				body.writeTo(getBody());
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
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
		public InputStream getInputStream() {
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
