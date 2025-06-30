/*
 * Copyright 2002-present the original author or authors.
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
import java.util.Collection;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.Test;

import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.testfixture.http.MockHttpOutputMessage;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockPart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link StandardMultipartHttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
class StandardMultipartHttpServletRequestTests {

	@Test
	void filename() {
		String disposition = "form-data; name=\"file\"; filename=\"myFile.txt\"";
		StandardMultipartHttpServletRequest request = requestWithPart("file", disposition, "");

		MultipartFile multipartFile = request.getFile("file");
		assertThat(multipartFile).isNotNull();
		assertThat(multipartFile.getOriginalFilename()).isEqualTo("myFile.txt");
	}

	@Test  // SPR-13319
	void filenameRfc5987() {
		String disposition = "form-data; name=\"file\"; filename*=\"UTF-8''foo-%c3%a4-%e2%82%ac.html\"";
		StandardMultipartHttpServletRequest request = requestWithPart("file", disposition, "");

		MultipartFile multipartFile = request.getFile("file");
		assertThat(multipartFile).isNotNull();
		assertThat(multipartFile.getOriginalFilename()).isEqualTo("foo-ä-€.html");
	}

	@Test  // SPR-15205
	void filenameRfc2047() {
		String disposition = "form-data; name=\"file\"; filename=\"=?UTF-8?Q?Declara=C3=A7=C3=A3o.pdf?=\"";
		StandardMultipartHttpServletRequest request = requestWithPart("file", disposition, "");

		MultipartFile multipartFile = request.getFile("file");
		assertThat(multipartFile).isNotNull();
		assertThat(multipartFile.getOriginalFilename()).isEqualTo("Declaração.pdf");
	}

	@Test
	void multipartFileResource() throws IOException {
		String name = "file";
		String disposition = "form-data; name=\"" + name + "\"; filename=\"myFile.txt\"";
		StandardMultipartHttpServletRequest request = requestWithPart(name, disposition, "myBody");
		MultipartFile multipartFile = request.getFile(name);

		assertThat(multipartFile).isNotNull();

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add(name, multipartFile.getResource());

		MockHttpOutputMessage output = new MockHttpOutputMessage();
		new FormHttpMessageConverter().write(map, null, output);

		assertThat(output.getBodyAsString(StandardCharsets.UTF_8)).contains("""
				Content-Disposition: form-data; name="file"; filename="myFile.txt"
				Content-Type: text/plain
				Content-Length: 6

				myBody
				""".replace("\n", "\r\n"));
	}

	@Test
	void plainSizeExceededServletException() {
		ServletException ex = new ServletException("Request size exceeded");

		assertThatExceptionOfType(MaxUploadSizeExceededException.class)
				.isThrownBy(() -> requestWithException(ex)).withCause(ex);
	}

	@Test  // gh-28759
	void jetty94MaxRequestSizeException() {
		ServletException ex = new ServletException(new IllegalStateException("Request exceeds maxRequestSize"));

		assertThatExceptionOfType(MaxUploadSizeExceededException.class)
				.isThrownBy(() -> requestWithException(ex)).withCause(ex);
	}

	@Test  // gh-31850
	void jetty12MaxLengthExceededException() {
		ServletException ex = new ServletException(new RuntimeException("400: bad multipart",
				new IllegalStateException("max length exceeded")));

		assertThatExceptionOfType(MaxUploadSizeExceededException.class)
				.isThrownBy(() -> requestWithException(ex)).withCause(ex);
	}

	@Test  // gh-32549
	void undertowRequestTooBigException() {
		IOException ex = new IOException("Connection terminated as request was larger than 10000");

		assertThatExceptionOfType(MaxUploadSizeExceededException.class)
				.isThrownBy(() -> requestWithException(ex)).withCause(ex);
	}


	private static StandardMultipartHttpServletRequest requestWithPart(String name, String disposition, String content) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockPart part = new MockPart(name, null, content.getBytes(StandardCharsets.UTF_8));
		part.getHeaders().set("Content-Disposition", disposition);
		request.addPart(part);
		return new StandardMultipartHttpServletRequest(request);
	}

	private static StandardMultipartHttpServletRequest requestWithException(ServletException ex) {
		MockHttpServletRequest request = new MockHttpServletRequest() {
			@Override
			public Collection<Part> getParts() throws ServletException {
				throw ex;
			}
		};
		return new StandardMultipartHttpServletRequest(request);
	}

	private static StandardMultipartHttpServletRequest requestWithException(IOException ex) {
		MockHttpServletRequest request = new MockHttpServletRequest() {
			@Override
			public Collection<Part> getParts() throws IOException {
				throw ex;
			}
		};
		return new StandardMultipartHttpServletRequest(request);
	}

}
