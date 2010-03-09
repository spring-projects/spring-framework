/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.http.converter.multipart;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpOutputMessage;

/** @author Arjen Poutsma */
public class MultipartHttpMessageConverterTest {

	private MultipartHttpMessageConverter converter;

	@Before
	public void setUp() {
		converter = new MultipartHttpMessageConverter();
	}

	@Test
	public void canRead() {
		assertFalse(converter.canRead(MultipartMap.class, new MediaType("multipart","form-data")));
	}

	@Test
	public void canWrite() {
		assertTrue(converter.canWrite(MultipartMap.class, new MediaType("multipart","form-data")));
		assertTrue(converter.canWrite(MultipartMap.class, MediaType.ALL));
	}

	@Test
	public void write() throws Exception {
		MultipartMap body = new MultipartMap();
		body.addTextPart("name 1", "value 1");
		body.addTextPart("name 2", "value 2+1");
		body.addTextPart("name 2", "value 2+2");
		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		body.addBinaryPart("logo", logo);
		byte[] xml = "<root><child/></root>".getBytes("UTF-8");
		body.addPart("xml", xml, new MediaType("application", "xml"));

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(body, null, outputMessage);
		final MediaType contentType = outputMessage.getHeaders().getContentType();
		final byte[] result = outputMessage.getBodyAsBytes();
		assertNotNull(contentType);
		assertNotNull(contentType.getParameter("boundary"));

		// see if Commons FileUpload can read what we wrote
		FileItemFactory fileItemFactory = new DiskFileItemFactory();
		FileUpload fileUpload = new FileUpload(fileItemFactory);
		List items = fileUpload.parseRequest(new RequestContext() {
			public String getCharacterEncoding() {
				return null;
			}

			public String getContentType() {
				return contentType.toString();
			}

			public int getContentLength() {
				return result.length;
			}

			public InputStream getInputStream() throws IOException {
				return new ByteArrayInputStream(result);
			}
		});
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
		assertEquals("application/octet-stream", item.getContentType());
		assertEquals(logo.getFile().length(), item.getSize());

		item = (FileItem) items.get(4);
		assertEquals("xml", item.getFieldName());
		assertEquals("application/xml", item.getContentType());
		assertEquals(xml.length, item.getSize());
	}


}
