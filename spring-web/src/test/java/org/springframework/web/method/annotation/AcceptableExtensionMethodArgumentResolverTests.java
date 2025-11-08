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

package org.springframework.web.method.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.AcceptableExtension;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockMultipartFile;
import org.springframework.web.testfixture.servlet.MockMultipartHttpServletRequest;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AcceptableExtensionMethodArgumentResolver}.
 *
 * @author Aleksei Iakhnenko
 */
class AcceptableExtensionMethodArgumentResolverTests {

	private AcceptableExtensionMethodArgumentResolver resolver;

	@BeforeEach
	void setUp() {
		this.resolver = new AcceptableExtensionMethodArgumentResolver();
	}

	@Test
	void supportsParameterWithAcceptableExtensionAnnotation() throws Exception {
		MethodParameter parameter = getMethodParameter("handleFileUpload", 0);
		assertThat(this.resolver.supportsParameter(parameter)).isTrue();
	}

	@Test
	void doesNotSupportParameterWithoutAcceptableExtensionAnnotation() throws Exception {
		MethodParameter parameter = getMethodParameter("handleFileUploadWithoutAnnotation", 0);
		assertThat(this.resolver.supportsParameter(parameter)).isFalse();
	}

	@Test
	void resolveArgumentWithValidExtension() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.jpg", "image/jpeg", "content".getBytes());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(file);

		NativeWebRequest webRequest = new ServletWebRequest(request);
		MethodParameter parameter = getMethodParameter("handleFileUpload", 0);

		Object result = this.resolver.resolveArgument(parameter, null, webRequest, null);

		assertThat(result).isInstanceOf(MultipartFile.class);
		MultipartFile resolvedFile = (MultipartFile) result;
		assertThat(resolvedFile.getOriginalFilename()).isEqualTo("test.jpg");
	}

	@Test
	void resolveArgumentWithInvalidExtensionThrowsException() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.exe", "application/octet-stream", "content".getBytes());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(file);

		NativeWebRequest webRequest = new ServletWebRequest(request);
		MethodParameter parameter = getMethodParameter("handleFileUpload", 0);

		assertThatThrownBy(() -> this.resolver.resolveArgument(parameter, null, webRequest, null))
				.isInstanceOf(MultipartException.class)
				.hasMessageContaining("Invalid file extension")
				.hasMessageContaining("Allowed: [jpg, png, pdf]")
				.hasMessageContaining("received: exe");
	}

	@Test
	void resolveArgumentWithEmptyExtensionsArrayAcceptsAnyExtension() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"document", "test.xyz", "application/octet-stream", "content".getBytes());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(file);

		NativeWebRequest webRequest = new ServletWebRequest(request);
		MethodParameter parameter = getMethodParameter("handleFileUploadWithEmptyExtensions", 0);

		Object result = this.resolver.resolveArgument(parameter, null, webRequest, null);

		assertThat(result).isInstanceOf(MultipartFile.class);
		MultipartFile resolvedFile = (MultipartFile) result;
		assertThat(resolvedFile.getOriginalFilename()).isEqualTo("test.xyz");
	}

	@Test
	void resolveArgumentWithEmptyFile() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.jpg", "image/jpeg", new byte[0]);

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(file);

		NativeWebRequest webRequest = new ServletWebRequest(request);
		MethodParameter parameter = getMethodParameter("handleFileUpload", 0);

		Object result = this.resolver.resolveArgument(parameter, null, webRequest, null);

		assertThat(result).isInstanceOf(MultipartFile.class);
		MultipartFile resolvedFile = (MultipartFile) result;
		assertThat(resolvedFile.isEmpty()).isTrue();
	}

	@Test
	void resolveArgumentWithNullFilename() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", null, "image/jpeg", "content".getBytes());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(file);

		NativeWebRequest webRequest = new ServletWebRequest(request);
		MethodParameter parameter = getMethodParameter("handleFileUpload", 0);

		Object result = this.resolver.resolveArgument(parameter, null, webRequest, null);

		assertThat(result).isInstanceOf(MultipartFile.class);
	}

	@Test
	void resolveArgumentWithEmptyFilename() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "", "image/jpeg", "content".getBytes());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(file);

		NativeWebRequest webRequest = new ServletWebRequest(request);
		MethodParameter parameter = getMethodParameter("handleFileUpload", 0);

		Object result = this.resolver.resolveArgument(parameter, null, webRequest, null);

		assertThat(result).isInstanceOf(MultipartFile.class);
	}

	@Test
	void resolveArgumentWithFilenameWithoutExtension() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "testfile", "application/octet-stream", "content".getBytes());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(file);

		NativeWebRequest webRequest = new ServletWebRequest(request);
		MethodParameter parameter = getMethodParameter("handleFileUpload", 0);

		Object result = this.resolver.resolveArgument(parameter, null, webRequest, null);

		assertThat(result).isInstanceOf(MultipartFile.class);
	}

	@Test
	void resolveArgumentWithCaseInsensitiveExtension() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.JPG", "image/jpeg", "content".getBytes());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(file);

		NativeWebRequest webRequest = new ServletWebRequest(request);
		MethodParameter parameter = getMethodParameter("handleFileUpload", 0);

		Object result = this.resolver.resolveArgument(parameter, null, webRequest, null);

		assertThat(result).isInstanceOf(MultipartFile.class);
		MultipartFile resolvedFile = (MultipartFile) result;
		assertThat(resolvedFile.getOriginalFilename()).isEqualTo("test.JPG");
	}

	@Test
	void resolveArgumentWithCustomMessage() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"avatar", "profile.exe", "application/octet-stream", "content".getBytes());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(file);

		NativeWebRequest webRequest = new ServletWebRequest(request);
		MethodParameter parameter = getMethodParameter("handleAvatarUpload", 0);

		assertThatThrownBy(() -> this.resolver.resolveArgument(parameter, null, webRequest, null))
				.isInstanceOf(MultipartException.class)
				.hasMessageContaining("Please upload only image files");
	}

	@Test
	void resolveArgumentWithRequestParamName() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"uploadedFile", "test.png", "image/png", "content".getBytes());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(file);

		NativeWebRequest webRequest = new ServletWebRequest(request);
		MethodParameter parameter = getMethodParameter("handleFileUploadWithCustomName", 0);

		Object result = this.resolver.resolveArgument(parameter, null, webRequest, null);

		assertThat(result).isInstanceOf(MultipartFile.class);
		MultipartFile resolvedFile = (MultipartFile) result;
		assertThat(resolvedFile.getOriginalFilename()).isEqualTo("test.png");
	}

	@Test
	void resolveArgumentWithNonMultipartRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		NativeWebRequest webRequest = new ServletWebRequest(request);
		MethodParameter parameter = getMethodParameter("handleFileUpload", 0);

		Object result = this.resolver.resolveArgument(parameter, null, webRequest, null);

		assertThat(result).isNull();
	}

	@Test
	void resolveArgumentWithoutHttpServletRequest() throws Exception {
		MethodParameter parameter = getMethodParameter("handleFileUpload", 0);

		// Simulate a scenario where getNativeRequest returns null
		NativeWebRequest emptyWebRequest = new NativeWebRequest() {
			@Override
			public Object getNativeRequest() {
				return new Object(); // Not an HttpServletRequest
			}

			@Override
			public Object getNativeResponse() {
				return null;
			}

			@Override
			public <T> T getNativeRequest(Class<T> requiredType) {
				return null;
			}

			@Override
			public <T> T getNativeResponse(Class<T> requiredType) {
				return null;
			}

			@Override
			public String getHeader(String headerName) {
				return null;
			}

			@Override
			public String[] getHeaderValues(String headerName) {
				return new String[0];
			}

			@Override
			public java.util.Iterator<String> getHeaderNames() {
				return java.util.Collections.emptyIterator();
			}

			@Override
			public String getParameter(String paramName) {
				return null;
			}

			@Override
			public String[] getParameterValues(String paramName) {
				return new String[0];
			}

			@Override
			public java.util.Iterator<String> getParameterNames() {
				return java.util.Collections.emptyIterator();
			}

			@Override
			public java.util.Map<String, String[]> getParameterMap() {
				return java.util.Collections.emptyMap();
			}

			@Override
			public java.util.Locale getLocale() {
				return java.util.Locale.getDefault();
			}

			@Override
			public String getContextPath() {
				return "";
			}

			@Override
			public String getRemoteUser() {
				return null;
			}

			@Override
			public java.security.Principal getUserPrincipal() {
				return null;
			}

			@Override
			public boolean isUserInRole(String role) {
				return false;
			}

			@Override
			public boolean isSecure() {
				return false;
			}

			@Override
			public boolean checkNotModified(long lastModifiedTimestamp) {
				return false;
			}

			@Override
			public boolean checkNotModified(String etag) {
				return false;
			}

			@Override
			public boolean checkNotModified(String etag, long lastModifiedTimestamp) {
				return false;
			}

			@Override
			public String getDescription(boolean includeClientInfo) {
				return "";
			}

			@Override
			public Object getAttribute(String name, int scope) {
				return null;
			}

			@Override
			public void setAttribute(String name, Object value, int scope) {
			}

			@Override
			public void removeAttribute(String name, int scope) {
			}

			@Override
			public String[] getAttributeNames(int scope) {
				return new String[0];
			}

			@Override
			public void registerDestructionCallback(String name, Runnable callback, int scope) {
			}

			@Override
			public Object resolveReference(String key) {
				return null;
			}

			@Override
			public String getSessionId() {
				return null;
			}

			@Override
			public Object getSessionMutex() {
				return null;
			}
		};

		Object result = this.resolver.resolveArgument(parameter, null, emptyWebRequest, null);

		assertThat(result).isNull();
	}

	@Test
	void resolveArgumentWithMultipleDots() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "my.test.file.jpg", "image/jpeg", "content".getBytes());

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(file);

		NativeWebRequest webRequest = new ServletWebRequest(request);
		MethodParameter parameter = getMethodParameter("handleFileUpload", 0);

		Object result = this.resolver.resolveArgument(parameter, null, webRequest, null);

		assertThat(result).isInstanceOf(MultipartFile.class);
	}

	private MethodParameter getMethodParameter(String methodName, int parameterIndex) throws Exception {
		Method method = TestController.class.getMethod(methodName, MultipartFile.class);
		return new MethodParameter(method, parameterIndex);
	}

	// Test controller class with various method signatures
	@SuppressWarnings("unused")
	static class TestController {

		public void handleFileUpload(
				@AcceptableExtension(extensions = {"jpg", "png", "pdf"}, message = "Invalid file extension")
				@RequestParam("file") MultipartFile file) {
		}

		public void handleFileUploadWithoutAnnotation(@RequestParam("file") MultipartFile file) {
		}

		public void handleFileUploadWithEmptyExtensions(
				@AcceptableExtension
				@RequestParam("document") MultipartFile document) {
		}

		public void handleAvatarUpload(
				@AcceptableExtension(extensions = {"jpg", "png", "gif"}, message = "Please upload only image files")
				@RequestParam("avatar") MultipartFile avatar) {
		}

		public void handleFileUploadWithCustomName(
				@AcceptableExtension(extensions = {"jpg", "png", "pdf"})
				@RequestParam(name = "uploadedFile") MultipartFile file) {
		}
	}
}