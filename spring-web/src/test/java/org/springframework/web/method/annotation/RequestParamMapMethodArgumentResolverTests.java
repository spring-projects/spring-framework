/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.Part;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockMultipartFile;
import org.springframework.web.testfixture.servlet.MockMultipartHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockPart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.method.MvcAnnotationPredicates.requestParam;

/**
 * Test fixture with {@link RequestParamMapMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class RequestParamMapMethodArgumentResolverTests {

	private RequestParamMapMethodArgumentResolver resolver = new RequestParamMapMethodArgumentResolver();

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private NativeWebRequest webRequest = new ServletWebRequest(request, new MockHttpServletResponse());

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	public void supportsParameter() {
		MethodParameter param = this.testMethod.annot(requestParam().noName()).arg(Map.class, String.class, String.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(RequestParam.class).arg(MultiValueMap.class, String.class, String.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annot(requestParam().name("name")).arg(Map.class, String.class, String.class);
		assertThat(resolver.supportsParameter(param)).isFalse();

		param = this.testMethod.annotNotPresent(RequestParam.class).arg(Map.class, String.class, String.class);
		assertThat(resolver.supportsParameter(param)).isFalse();
	}

	@Test
	public void resolveMapOfString() throws Exception {
		String name = "foo";
		String value = "bar";
		request.addParameter(name, value);
		Map<String, String> expected = Collections.singletonMap(name, value);

		MethodParameter param = this.testMethod.annot(requestParam().noName()).arg(Map.class, String.class, String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);

		boolean condition = result instanceof Map;
		assertThat(condition).isTrue();
		assertThat(result).as("Invalid result").isEqualTo(expected);
	}

	@Test
	public void resolveMultiValueMapOfString() throws Exception {
		String name = "foo";
		String value1 = "bar";
		String value2 = "baz";
		request.addParameter(name, value1, value2);

		MultiValueMap<String, String> expected = new LinkedMultiValueMap<>(1);
		expected.add(name, value1);
		expected.add(name, value2);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultiValueMap.class, String.class, String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);

		boolean condition = result instanceof MultiValueMap;
		assertThat(condition).isTrue();
		assertThat(result).as("Invalid result").isEqualTo(expected);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void resolveMapOfMultipartFile() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected1 = new MockMultipartFile("mfile", "Hello World".getBytes());
		MultipartFile expected2 = new MockMultipartFile("other", "Hello World 3".getBytes());
		request.addFile(expected1);
		request.addFile(expected2);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annot(requestParam().noName()).arg(Map.class, String.class, MultipartFile.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);

		boolean condition = result instanceof Map;
		assertThat(condition).isTrue();
		Map<String, MultipartFile> resultMap = (Map<String, MultipartFile>) result;
		assertThat(resultMap.size()).isEqualTo(2);
		assertThat(resultMap.get("mfile")).isEqualTo(expected1);
		assertThat(resultMap.get("other")).isEqualTo(expected2);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void resolveMultiValueMapOfMultipartFile() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected1 = new MockMultipartFile("mfilelist", "Hello World 1".getBytes());
		MultipartFile expected2 = new MockMultipartFile("mfilelist", "Hello World 2".getBytes());
		MultipartFile expected3 = new MockMultipartFile("other", "Hello World 3".getBytes());
		request.addFile(expected1);
		request.addFile(expected2);
		request.addFile(expected3);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annot(requestParam().noName()).arg(MultiValueMap.class, String.class, MultipartFile.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);

		boolean condition = result instanceof MultiValueMap;
		assertThat(condition).isTrue();
		MultiValueMap<String, MultipartFile> resultMap = (MultiValueMap<String, MultipartFile>) result;
		assertThat(resultMap.size()).isEqualTo(2);
		assertThat(resultMap.get("mfilelist").size()).isEqualTo(2);
		assertThat(resultMap.get("mfilelist").get(0)).isEqualTo(expected1);
		assertThat(resultMap.get("mfilelist").get(1)).isEqualTo(expected2);
		assertThat(resultMap.get("other").size()).isEqualTo(1);
		assertThat(resultMap.get("other").get(0)).isEqualTo(expected3);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void resolveMapOfPart() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("multipart/form-data");
		Part expected1 = new MockPart("mfile", "Hello World".getBytes());
		Part expected2 = new MockPart("other", "Hello World 3".getBytes());
		request.addPart(expected1);
		request.addPart(expected2);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annot(requestParam().noName()).arg(Map.class, String.class, Part.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);

		boolean condition = result instanceof Map;
		assertThat(condition).isTrue();
		Map<String, Part> resultMap = (Map<String, Part>) result;
		assertThat(resultMap.size()).isEqualTo(2);
		assertThat(resultMap.get("mfile")).isEqualTo(expected1);
		assertThat(resultMap.get("other")).isEqualTo(expected2);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void resolveMultiValueMapOfPart() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContentType("multipart/form-data");
		Part expected1 = new MockPart("mfilelist", "Hello World 1".getBytes());
		Part expected2 = new MockPart("mfilelist", "Hello World 2".getBytes());
		Part expected3 = new MockPart("other", "Hello World 3".getBytes());
		request.addPart(expected1);
		request.addPart(expected2);
		request.addPart(expected3);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annot(requestParam().noName()).arg(MultiValueMap.class, String.class, Part.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);

		boolean condition = result instanceof MultiValueMap;
		assertThat(condition).isTrue();
		MultiValueMap<String, Part> resultMap = (MultiValueMap<String, Part>) result;
		assertThat(resultMap.size()).isEqualTo(2);
		assertThat(resultMap.get("mfilelist").size()).isEqualTo(2);
		assertThat(resultMap.get("mfilelist").get(0)).isEqualTo(expected1);
		assertThat(resultMap.get("mfilelist").get(1)).isEqualTo(expected2);
		assertThat(resultMap.get("other").size()).isEqualTo(1);
		assertThat(resultMap.get("other").get(0)).isEqualTo(expected3);
	}


	public void handle(
			@RequestParam Map<String, String> param1,
			@RequestParam MultiValueMap<String, String> param2,
			@RequestParam Map<String, MultipartFile> param3,
			@RequestParam MultiValueMap<String, MultipartFile> param4,
			@RequestParam Map<String, Part> param5,
			@RequestParam MultiValueMap<String, Part> param6,
			@RequestParam("name") Map<String, String> param7,
			Map<String, String> param8) {
	}

}
