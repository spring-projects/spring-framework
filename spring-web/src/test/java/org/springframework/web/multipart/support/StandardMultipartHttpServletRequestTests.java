/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.multipart.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import org.springframework.http.MockHttpOutputMessage;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockPart;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link StandardMultipartHttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 */
public class StandardMultipartHttpServletRequestTests {

	@Test
	public void filename() throws Exception {
		String disposition = "form-data; name=\"file\"; filename=\"myFile.txt\"";
		StandardMultipartHttpServletRequest request = requestWithPart("file", disposition, "");

		MultipartFile multipartFile = request.getFile("file");
		assertNotNull(multipartFile);
		assertEquals("myFile.txt", multipartFile.getOriginalFilename());
	}

	@Test  // SPR-13319
	public void filenameRfc5987() throws Exception {
		String disposition = "form-data; name=\"file\"; filename*=\"UTF-8''foo-%c3%a4-%e2%82%ac.html\"";
		StandardMultipartHttpServletRequest request = requestWithPart("file", disposition, "");

		MultipartFile multipartFile = request.getFile("file");
		assertNotNull(multipartFile);
		assertEquals("foo-ä-€.html", multipartFile.getOriginalFilename());
	}

	@Test  // SPR-15205
	public void filenameRfc2047() throws Exception {
		String disposition = "form-data; name=\"file\"; filename=\"=?UTF-8?Q?Declara=C3=A7=C3=A3o.pdf?=\"";
		StandardMultipartHttpServletRequest request = requestWithPart("file", disposition, "");

		MultipartFile multipartFile = request.getFile("file");
		assertNotNull(multipartFile);
		assertEquals("Declaração.pdf", multipartFile.getOriginalFilename());
	}

	@Test
	public void multipartFileResource() throws IOException {
		String name = "file";
		String disposition = "form-data; name=\"" + name + "\"; filename=\"myFile.txt\"";
		StandardMultipartHttpServletRequest request = requestWithPart(name, disposition, "myBody");
		MultipartFile multipartFile = request.getFile(name);

		assertNotNull(multipartFile);

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add(name, multipartFile.getResource());

		MockHttpOutputMessage output = new MockHttpOutputMessage();
		new FormHttpMessageConverter().write(map, null, output);

		assertThat(output.getBodyAsString(StandardCharsets.UTF_8), containsString(
				"Content-Disposition: form-data; name=\"file\"; filename=\"myFile.txt\"\r\n" +
						"Content-Type: text/plain\r\n" +
						"Content-Length: 6\r\n" +
						"\r\n" +
						"myBody\r\n"));
	}


	private StandardMultipartHttpServletRequest requestWithPart(String name, String disposition, String content) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockPart part = new MockPart(name, null, content.getBytes(StandardCharsets.UTF_8));
		part.getHeaders().set("Content-Disposition", disposition);
		request.addPart(part);
		return new StandardMultipartHttpServletRequest(request);
	}

}
