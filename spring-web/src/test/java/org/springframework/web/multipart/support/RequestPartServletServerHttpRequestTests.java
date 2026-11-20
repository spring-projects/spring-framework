/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.multipart.support;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.testfixture.servlet.MockMultipartFile;
import org.springframework.web.testfixture.servlet.MockMultipartHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockPart;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
class RequestPartServletServerHttpRequestTests {

	private final MockMultipartHttpServletRequest mockRequest = new MockMultipartHttpServletRequest();


	@Test
	void getMethod() throws Exception {
		this.mockRequest.addFile(new MockMultipartFile("part", "", "", "content".getBytes(StandardCharsets.UTF_8)));
		ServerHttpRequest request = new RequestPartServletServerHttpRequest(this.mockRequest, "part");
		this.mockRequest.setMethod("POST");

		assertThat(request.getMethod()).isEqualTo(HttpMethod.POST);
	}

	@Test
	void getURI() throws Exception {
		this.mockRequest.addFile(new MockMultipartFile("part", "", "application/json", "content".getBytes(StandardCharsets.UTF_8)));
		ServerHttpRequest request = new RequestPartServletServerHttpRequest(this.mockRequest, "part");

		URI uri = URI.create("https://example.com/path?query");
		this.mockRequest.setScheme(uri.getScheme());
		this.mockRequest.setServerName(uri.getHost());
		this.mockRequest.setServerPort(uri.getPort());
		this.mockRequest.setRequestURI(uri.getPath());
		this.mockRequest.setQueryString(uri.getQuery());
		assertThat(request.getURI()).isEqualTo(uri);
	}

	@Test
	void getContentType() throws Exception {
		MultipartFile part = new MockMultipartFile("part", "", "application/json", "content".getBytes(StandardCharsets.UTF_8));
		this.mockRequest.addFile(part);
		ServerHttpRequest request = new RequestPartServletServerHttpRequest(this.mockRequest, "part");

		HttpHeaders headers = request.getHeaders();
		assertThat(headers).isNotNull();
		assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	void getBody() throws Exception {
		byte[] bytes = "content".getBytes(StandardCharsets.UTF_8);
		MultipartFile part = new MockMultipartFile("part", "", "application/json", bytes);
		this.mockRequest.addFile(part);
		ServerHttpRequest request = new RequestPartServletServerHttpRequest(this.mockRequest, "part");

		byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
		assertThat(result).isEqualTo(bytes);
	}

	@Test  // SPR-13317
	void getBodyWithWrappedRequest() throws Exception {
		byte[] bytes = "content".getBytes(StandardCharsets.UTF_8);
		MultipartFile part = new MockMultipartFile("part", "", "application/json", bytes);
		this.mockRequest.addFile(part);
		HttpServletRequest wrapped = new HttpServletRequestWrapper(this.mockRequest);
		ServerHttpRequest request = new RequestPartServletServerHttpRequest(wrapped, "part");

		byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
		assertThat(result).isEqualTo(bytes);
	}

	@Test  // SPR-13096
	void getBodyViaRequestParameter() throws Exception {
		MockMultipartHttpServletRequest mockRequest = new MockMultipartHttpServletRequest() {
			@Override
			public HttpHeaders getMultipartHeaders(String paramOrFileName) {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(new MediaType("application", "octet-stream", StandardCharsets.ISO_8859_1));
				return headers;
			}
		};

		byte[] bytes = {(byte) 0xC4};
		mockRequest.setParameter("part", new String(bytes, StandardCharsets.ISO_8859_1));
		ServerHttpRequest request = new RequestPartServletServerHttpRequest(mockRequest, "part");
		byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
		assertThat(result).isEqualTo(bytes);
	}

	@Test
	void getBodyViaRequestParameterWithRequestEncoding() throws Exception {
		MockMultipartHttpServletRequest mockRequest = new MockMultipartHttpServletRequest() {
			@Override
			public HttpHeaders getMultipartHeaders(String paramOrFileName) {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
				return headers;
			}
		};

		byte[] bytes = {(byte) 0xC4};
		mockRequest.setParameter("part", new String(bytes, StandardCharsets.ISO_8859_1));
		mockRequest.setCharacterEncoding("iso-8859-1");
		ServerHttpRequest request = new RequestPartServletServerHttpRequest(mockRequest, "part");
		byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
		assertThat(result).isEqualTo(bytes);
	}

	@Test  // gh-25829
	void getBodyViaRequestPart() throws Exception {
		byte[] bytes = "content".getBytes(StandardCharsets.UTF_8);
		MockPart mockPart = new MockPart("part", bytes);
		mockPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		this.mockRequest.addPart(mockPart);
		ServerHttpRequest request = new RequestPartServletServerHttpRequest(this.mockRequest, "part");

		byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
		assertThat(result).isEqualTo(bytes);
	}

}
