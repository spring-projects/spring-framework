/*
 * Copyright 2002-2017 the original author or authors.
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

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockPart;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link StandardMultipartHttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 */
public class StandardMultipartHttpServletRequestTests {

	@Test
	public void filename() throws Exception {
		StandardMultipartHttpServletRequest request = getRequest(
				"file", "form-data; name=\"file\"; filename=\"myFile.txt\"");

		MultipartFile multipartFile = request.getFile("file");
		assertNotNull(multipartFile);
		assertEquals("myFile.txt", multipartFile.getOriginalFilename());
	}

	@Test  // SPR-13319
	public void filenameRfc5987() throws Exception {
		StandardMultipartHttpServletRequest request = getRequest(
				"file", "form-data; name=\"file\"; filename*=\"UTF-8''foo-%c3%a4-%e2%82%ac.html\"");

		MultipartFile multipartFile = request.getFile("file");
		assertNotNull(multipartFile);
		assertEquals("foo-ä-€.html", multipartFile.getOriginalFilename());
	}

	@Test  // SPR-15205
	public void filenameRfc2047() throws Exception {
		StandardMultipartHttpServletRequest request = getRequest(
				"file", "form-data; name=\"file\"; filename=\"=?UTF-8?Q?Declara=C3=A7=C3=A3o.pdf?=\"");

		MultipartFile multipartFile = request.getFile("file");
		assertNotNull(multipartFile);
		assertEquals("Declaração.pdf", multipartFile.getOriginalFilename());
	}


	private StandardMultipartHttpServletRequest getRequest(String name, String dispositionValue) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockPart part = new MockPart(name, null);
		part.getHeaders().set("Content-Disposition", dispositionValue);
		request.addPart(part);
		return new StandardMultipartHttpServletRequest(request);
	}

}
