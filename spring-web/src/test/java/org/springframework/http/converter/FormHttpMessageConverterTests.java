/*
 * Copyright 2002-2011 the original author or authors.
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
import java.nio.charset.Charset;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.junit.Before;
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

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class FormHttpMessageConverterTests {

	private FormHttpMessageConverter converter;

	@Before
	public void setUp() {
		converter = new AllEncompassingFormHttpMessageConverter();
	}

	@Test
	public void canRead() {
		assertTrue(converter.canRead(MultiValueMap.class, new MediaType("application", "x-www-form-urlencoded")));
		assertFalse(converter.canRead(MultiValueMap.class, new MediaType("multipart", "form-data")));
	}

	@Test
	public void canWrite() {
		assertTrue(converter.canWrite(MultiValueMap.class, new MediaType("application", "x-www-form-urlencoded")));
		assertTrue(converter.canWrite(MultiValueMap.class, new MediaType("multipart", "form-data")));
		assertTrue(converter.canWrite(MultiValueMap.class, MediaType.ALL));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readForm() throws Exception {
		String body = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3";
		Charset iso88591 = Charset.forName("ISO-8859-1");
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(iso88591));
		inputMessage.getHeaders().setContentType(new MediaType("application", "x-www-form-urlencoded", iso88591));
		MultiValueMap<String, String> result = converter.read(null, inputMessage);

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
		MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();
		body.set("name 1", "value 1");
		body.add("name 2", "value 2+1");
		body.add("name 2", "value 2+2");
		body.add("name 3", null);
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(body, MediaType.APPLICATION_FORM_URLENCODED, outputMessage);
		assertEquals("Invalid result", "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3",
				outputMessage.getBodyAsString(Charset.forName("UTF-8")));
		assertEquals("Invalid content-type", new MediaType("application", "x-www-form-urlencoded"),
				outputMessage.getHeaders().getContentType());
		assertEquals("Invalid content-length", outputMessage.getBodyAsBytes().length,
				outputMessage.getHeaders().getContentLength());
	}

	@Test
	public void writeMultipart() throws Exception {
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
		parts.add("name 1", "value 1");
		parts.add("name 2", "value 2+1");
		parts.add("name 2", "value 2+2");

		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		parts.add("logo", logo);
		Source xml = new StreamSource(new StringReader("<root><child/></root>"));
		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(MediaType.TEXT_XML);
		HttpEntity<Source> entity = new HttpEntity<Source>(xml, entityHeaders);
		parts.add("xml", entity);

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(parts, MediaType.MULTIPART_FORM_DATA, outputMessage);

		final MediaType contentType = outputMessage.getHeaders().getContentType();
		assertNotNull("No boundary found", contentType.getParameter("boundary"));

		// see if Commons FileUpload can read what we wrote
		FileItemFactory fileItemFactory = new DiskFileItemFactory();
		FileUpload fileUpload = new FileUpload(fileItemFactory);
		List items = fileUpload.parseRequest(new MockHttpOutputMessageRequestContext(outputMessage));
		assertEquals(5, items.size());
		FileItem item = (FileItem) items.get(0);
		assertTrue(item.isFormField());
		assertEquals("name 1", item.getFieldName());
		assertEquals("value 1", item.getString());

		item = (FileItem) items.get(1);
		assertTrue(item.isFormField());
		assertEquals("name 2", item.getFieldName());
		assertEquals("value 2+1", item.getString());

		item = (FileItem) items.get(2);
		assertTrue(item.isFormField());
		assertEquals("name 2", item.getFieldName());
		assertEquals("value 2+2", item.getString());

		item = (FileItem) items.get(3);
		assertFalse(item.isFormField());
		assertEquals("logo", item.getFieldName());
		assertEquals("logo.jpg", item.getName());
		assertEquals("image/jpeg", item.getContentType());
		assertEquals(logo.getFile().length(), item.getSize());

		item = (FileItem) items.get(4);
		assertEquals("xml", item.getFieldName());
		assertEquals("text/xml", item.getContentType());
	}

	private static class MockHttpOutputMessageRequestContext implements RequestContext {

		private final MockHttpOutputMessage outputMessage;

		private MockHttpOutputMessageRequestContext(MockHttpOutputMessage outputMessage) {
			this.outputMessage = outputMessage;
		}

		public String getCharacterEncoding() {
			MediaType contentType = outputMessage.getHeaders().getContentType();
			return contentType != null && contentType.getCharSet() != null ? contentType.getCharSet().name() : null;
		}

		public String getContentType() {
			MediaType contentType = outputMessage.getHeaders().getContentType();
			return contentType != null ? contentType.toString() : null;
		}

		public int getContentLength() {
			return outputMessage.getBodyAsBytes().length;
		}

		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(outputMessage.getBodyAsBytes());
		}
	}

}
