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

package org.springframework.http.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.junit.Test;

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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class FormHttpMessageConverterTests {

	private final FormHttpMessageConverter converter = new AllEncompassingFormHttpMessageConverter();


	@Test
	public void canRead() {
		assertTrue(this.converter.canRead(MultiValueMap.class,
				new MediaType("application", "x-www-form-urlencoded")));
		assertFalse(this.converter.canRead(MultiValueMap.class,
				new MediaType("multipart", "form-data")));
	}

	@Test
	public void canWrite() {
		assertTrue(this.converter.canWrite(MultiValueMap.class,
				new MediaType("application", "x-www-form-urlencoded")));
		assertTrue(this.converter.canWrite(MultiValueMap.class,
				new MediaType("multipart", "form-data")));
		assertTrue(this.converter.canWrite(MultiValueMap.class,
				new MediaType("multipart", "form-data", StandardCharsets.UTF_8)));
		assertTrue(this.converter.canWrite(MultiValueMap.class, MediaType.ALL));
	}

	@Test
	public void readForm() throws Exception {
		String body = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.ISO_8859_1));
		inputMessage.getHeaders().setContentType(new MediaType("application", "x-www-form-urlencoded", StandardCharsets.ISO_8859_1));
		MultiValueMap<String, String> result = this.converter.read(null, inputMessage);

		assertEquals("Invalid result", 3, result.size());
		assertEquals("Invalid result", "value 1", result.getFirst("name 1"));
		List<String> values = result.get("name 2");
		assertEquals("Invalid result", 2, values.size());
		assertEquals("Invalid result", "value 2+1", values.get(0));
		assertEquals("Invalid result", "value 2+2", values.get(1));
		assertNull("Invalid result", result.getFirst("name 3"));
	}

	@Test
	public void writeForm() throws IOException {
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.set("name 1", "value 1");
		body.add("name 2", "value 2+1");
		body.add("name 2", "value 2+2");
		body.add("name 3", null);
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.write(body, MediaType.APPLICATION_FORM_URLENCODED, outputMessage);

		assertEquals("Invalid result", "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3",
				outputMessage.getBodyAsString(StandardCharsets.UTF_8));
		assertEquals("Invalid content-type", new MediaType("application", "x-www-form-urlencoded"),
				outputMessage.getHeaders().getContentType());
		assertEquals("Invalid content-length", outputMessage.getBodyAsBytes().length,
				outputMessage.getHeaders().getContentLength());
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
		entityHeaders.setContentType(MediaType.TEXT_XML);
		HttpEntity<Source> entity = new HttpEntity<>(xml, entityHeaders);
		parts.add("xml", entity);

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.write(parts, new MediaType("multipart", "form-data", StandardCharsets.UTF_8), outputMessage);

		final MediaType contentType = outputMessage.getHeaders().getContentType();
		assertNotNull("No boundary found", contentType.getParameter("boundary"));

		// see if Commons FileUpload can read what we wrote
		FileItemFactory fileItemFactory = new DiskFileItemFactory();
		FileUpload fileUpload = new FileUpload(fileItemFactory);
		RequestContext requestContext = new MockHttpOutputMessageRequestContext(outputMessage);
		List<FileItem> items = fileUpload.parseRequest(requestContext);
		assertEquals(6, items.size());
		FileItem item = items.get(0);
		assertTrue(item.isFormField());
		assertEquals("name 1", item.getFieldName());
		assertEquals("value 1", item.getString());

		item = items.get(1);
		assertTrue(item.isFormField());
		assertEquals("name 2", item.getFieldName());
		assertEquals("value 2+1", item.getString());

		item = items.get(2);
		assertTrue(item.isFormField());
		assertEquals("name 2", item.getFieldName());
		assertEquals("value 2+2", item.getString());

		item = items.get(3);
		assertFalse(item.isFormField());
		assertEquals("logo", item.getFieldName());
		assertEquals("logo.jpg", item.getName());
		assertEquals("image/jpeg", item.getContentType());
		assertEquals(logo.getFile().length(), item.getSize());

		item = items.get(4);
		assertFalse(item.isFormField());
		assertEquals("utf8", item.getFieldName());
		assertEquals("Hall\u00F6le.jpg", item.getName());
		assertEquals("image/jpeg", item.getContentType());
		assertEquals(logo.getFile().length(), item.getSize());

		item = items.get(5);
		assertEquals("xml", item.getFieldName());
		assertEquals("text/xml", item.getContentType());
		verify(outputMessage.getBody(), never()).close();
	}

	// SPR-13309

	@Test
	public void writeMultipartOrder() throws Exception {
		MyBean myBean = new MyBean();
		myBean.setString("foo");

		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("part1", myBean);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(MediaType.TEXT_XML);
		HttpEntity<MyBean> entity = new HttpEntity<>(myBean, entityHeaders);
		parts.add("part2", entity);

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.setMultipartCharset(StandardCharsets.UTF_8);
		this.converter.write(parts, new MediaType("multipart", "form-data", StandardCharsets.UTF_8), outputMessage);

		final MediaType contentType = outputMessage.getHeaders().getContentType();
		assertNotNull("No boundary found", contentType.getParameter("boundary"));

		// see if Commons FileUpload can read what we wrote
		FileItemFactory fileItemFactory = new DiskFileItemFactory();
		FileUpload fileUpload = new FileUpload(fileItemFactory);
		RequestContext requestContext = new MockHttpOutputMessageRequestContext(outputMessage);
		List<FileItem> items = fileUpload.parseRequest(requestContext);
		assertEquals(2, items.size());

		FileItem item = items.get(0);
		assertTrue(item.isFormField());
		assertEquals("part1", item.getFieldName());
		assertEquals("{\"string\":\"foo\"}", item.getString());

		item = items.get(1);
		assertTrue(item.isFormField());
		assertEquals("part2", item.getFieldName());

		// With developer builds we get: <MyBean><string>foo</string></MyBean>
		// But on CI server we get: <MyBean xmlns=""><string>foo</string></MyBean>
		// So... we make a compromise:
		assertThat(item.getString(),
				allOf(startsWith("<MyBean"), endsWith("><string>foo</string></MyBean>")));
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
