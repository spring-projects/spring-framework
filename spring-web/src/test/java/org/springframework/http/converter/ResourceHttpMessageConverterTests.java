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

package org.springframework.http.converter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Test;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.util.FileCopyUtils;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsInstanceOf.*;
import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 * @author Kazuki Shimizu
 */
public class ResourceHttpMessageConverterTests {

	private final ResourceHttpMessageConverter converter = new ResourceHttpMessageConverter();


	@Test
	public void canRead() {
		assertTrue(converter.canRead(Resource.class, new MediaType("application", "octet-stream")));
	}

	@Test
	public void canWrite() {
		assertTrue(converter.canWrite(Resource.class, new MediaType("application", "octet-stream")));
		assertTrue(converter.canWrite(Resource.class, MediaType.ALL));
	}

	@Test
	public void read() throws IOException {
		byte[] body = FileCopyUtils.copyToByteArray(getClass().getResourceAsStream("logo.jpg"));
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		inputMessage.getHeaders().setContentType(MediaType.IMAGE_JPEG);
		Resource actualResource = converter.read(Resource.class, inputMessage);
		assertThat(FileCopyUtils.copyToByteArray(actualResource.getInputStream()), is(body));
	}

	@Test  // SPR-13443
	public void readWithInputStreamResource() throws IOException {
		try (InputStream body = getClass().getResourceAsStream("logo.jpg") ) {
			MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
			inputMessage.getHeaders().setContentType(MediaType.IMAGE_JPEG);
			Resource actualResource = converter.read(InputStreamResource.class, inputMessage);
			assertThat(actualResource, instanceOf(InputStreamResource.class));
			assertThat(actualResource.getInputStream(), is(body));
		}
	}

	@Test
	public void write() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Resource body = new ClassPathResource("logo.jpg", getClass());
		converter.write(body, null, outputMessage);
		assertEquals("Invalid content-type", MediaType.IMAGE_JPEG,
				outputMessage.getHeaders().getContentType());
		assertEquals("Invalid content-length", body.getFile().length(), outputMessage.getHeaders().getContentLength());
	}

	@Test  // SPR-10848
	public void writeByteArrayNullMediaType() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		byte[] byteArray = {1, 2, 3};
		Resource body = new ByteArrayResource(byteArray);
		converter.write(body, null, outputMessage);
		assertTrue(Arrays.equals(byteArray, outputMessage.getBodyAsBytes()));
	}

}
