/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockMultipartFile;
import org.springframework.web.testfixture.servlet.MockMultipartHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockPart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.array;
import static org.assertj.core.api.InstanceOfAssertFactories.optional;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.web.testfixture.method.MvcAnnotationPredicates.requestParam;
import static org.springframework.web.testfixture.method.MvcAnnotationPredicates.requestPart;

/**
 * Test fixture with {@link RequestParamMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
class RequestParamMethodArgumentResolverTests {

	private RequestParamMethodArgumentResolver resolver;

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private NativeWebRequest webRequest = new ServletWebRequest(request, new MockHttpServletResponse());

	private final ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();

	@BeforeEach
	void setup() {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.refresh();
		resolver = new RequestParamMethodArgumentResolver(context.getBeanFactory(), true);

		// Expose request to the current thread (for SpEL expressions)
		RequestContextHolder.setRequestAttributes(webRequest);
		context.close();
	}

	@Test
	void supportsParameter() {
		resolver = new RequestParamMethodArgumentResolver(null, true);

		MethodParameter param = this.testMethod.annot(requestParam().notRequired("bar")).arg(String.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(RequestParam.class).arg(String[].class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annot(requestParam().name("name")).arg(Map.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(RequestParam.class).arg(List.class, MultipartFile.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile[].class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(RequestParam.class).arg(Part.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(RequestParam.class).arg(List.class, Part.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(RequestParam.class).arg(Part[].class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annot(requestParam().noName()).arg(Map.class);
		assertThat(resolver.supportsParameter(param)).isFalse();

		param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotNotPresent().arg(MultipartFile.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotNotPresent(RequestParam.class).arg(List.class, MultipartFile.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotNotPresent(RequestParam.class).arg(Part.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annot(requestPart()).arg(MultipartFile.class);
		assertThat(resolver.supportsParameter(param)).isFalse();

		param = this.testMethod.annot(requestParam()).arg(String.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annot(requestParam().notRequired()).arg(String.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, Integer.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, MultipartFile.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		resolver = new RequestParamMethodArgumentResolver(null, false);

		param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		assertThat(resolver.supportsParameter(param)).isFalse();

		param = this.testMethod.annotPresent(RequestPart.class).arg(MultipartFile.class);
		assertThat(resolver.supportsParameter(param)).isFalse();

		param = this.testMethod.annotPresent(RequestParam.class).arg(Integer.class);
		assertThat(resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(RequestParam.class).arg(int.class);
		assertThat(resolver.supportsParameter(param)).isTrue();
	}

	@Test
	void resolveString() throws Exception {
		String expected = "foo";
		request.addParameter("name", expected);

		MethodParameter param = this.testMethod.annot(requestParam().notRequired("bar")).arg(String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void resolveStringArray() throws Exception {
		String[] expected = new String[] {"foo", "bar"};
		request.addParameter("name", expected);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(String[].class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).asInstanceOf(array(String[].class)).containsExactly(expected);
	}

	@Test  // gh-32577
	void resolveStringArrayWithEmptyArraySuffix() throws Exception {
		String[] expected = new String[] {"foo", "bar"};
		request.addParameter("name[]", expected[0]);
		request.addParameter("name[]", expected[1]);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(String[].class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).asInstanceOf(array(String[].class)).containsExactly(expected);
	}

	@Test
	void resolveMultipartFile() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("mfile", "Hello World".getBytes());
		request.addFile(expected);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).asInstanceOf(type(MultipartFile.class)).isEqualTo(expected);
	}

	@Test
	void resolveMultipartFileList() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected1 = new MockMultipartFile("mfilelist", "Hello World 1".getBytes());
		MultipartFile expected2 = new MockMultipartFile("mfilelist", "Hello World 2".getBytes());
		request.addFile(expected1);
		request.addFile(expected2);
		request.addFile(new MockMultipartFile("other", "Hello World 3".getBytes()));
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(List.class, MultipartFile.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).asInstanceOf(LIST).containsExactly(expected1, expected2);
	}

	@Test
	void resolveMultipartFileListMissing() {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(new MockMultipartFile("other", "Hello World 3".getBytes()));
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(List.class, MultipartFile.class);
		assertThatExceptionOfType(MissingServletRequestPartException.class).isThrownBy(() ->
				resolver.resolveArgument(param, null, webRequest, null));
	}

	@Test
	void resolveMultipartFileArray() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected1 = new MockMultipartFile("mfilearray", "Hello World 1".getBytes());
		MultipartFile expected2 = new MockMultipartFile("mfilearray", "Hello World 2".getBytes());
		request.addFile(expected1);
		request.addFile(expected2);
		request.addFile(new MockMultipartFile("other", "Hello World 3".getBytes()));
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile[].class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).asInstanceOf(array(MultipartFile[].class)).containsExactly(expected1, expected2);
	}

	@Test
	void resolveMultipartFileArrayMissing() {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.addFile(new MockMultipartFile("other", "Hello World 3".getBytes()));
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile[].class);
		assertThatExceptionOfType(MissingServletRequestPartException.class).isThrownBy(() ->
				resolver.resolveArgument(param, null, webRequest, null));
	}

	@Test
	void resolvePart() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockPart expected = new MockPart("pfile", "Hello World".getBytes());
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(expected);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Part.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).asInstanceOf(type(Part.class)).isEqualTo(expected);
	}

	@Test
	void resolvePartList() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		MockPart expected1 = new MockPart("pfilelist", "Hello World 1".getBytes());
		MockPart expected2 = new MockPart("pfilelist", "Hello World 2".getBytes());
		request.addPart(expected1);
		request.addPart(expected2);
		request.addPart(new MockPart("other", "Hello World 3".getBytes()));
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(List.class, Part.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).asInstanceOf(LIST).containsExactly(expected1, expected2);
	}

	@Test
	void resolvePartListMissing() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(new MockPart("other", "Hello World 3".getBytes()));
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(List.class, Part.class);
		assertThatExceptionOfType(MissingServletRequestPartException.class).isThrownBy(() ->
				resolver.resolveArgument(param, null, webRequest, null));
	}

	@Test
	void resolvePartArray() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockPart expected1 = new MockPart("pfilearray", "Hello World 1".getBytes());
		MockPart expected2 = new MockPart("pfilearray", "Hello World 2".getBytes());
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(expected1);
		request.addPart(expected2);
		request.addPart(new MockPart("other", "Hello World 3".getBytes()));
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Part[].class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).asInstanceOf(array(Part[].class)).containsExactly(expected1, expected2);
	}

	@Test
	void resolvePartArrayMissing() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(new MockPart("other", "Hello World 3".getBytes()));
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Part[].class);
		assertThatExceptionOfType(MissingServletRequestPartException.class).isThrownBy(() ->
				resolver.resolveArgument(param, null, webRequest, null));
	}

	@Test
	void resolveMultipartFileNotAnnot() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("multipartFileNotAnnot", "Hello World".getBytes());
		request.addFile(expected);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotNotPresent().arg(MultipartFile.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).asInstanceOf(type(MultipartFile.class)).isEqualTo(expected);
	}

	@Test
	void resolveMultipartFileListNotannot() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected1 = new MockMultipartFile("multipartFileList", "Hello World 1".getBytes());
		MultipartFile expected2 = new MockMultipartFile("multipartFileList", "Hello World 2".getBytes());
		request.addFile(expected1);
		request.addFile(expected2);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod
				.annotNotPresent(RequestParam.class).arg(List.class, MultipartFile.class);

		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).asInstanceOf(LIST).containsExactly(expected1, expected2);
	}

	@Test
	void isMultipartRequest() {
		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile.class);
		assertThatExceptionOfType(MultipartException.class).isThrownBy(() ->
				resolver.resolveArgument(param, null, webRequest, null));
	}

	@Test  // SPR-9079
	public void isMultipartRequestHttpPut() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("multipartFileList", "Hello World".getBytes());
		request.addFile(expected);
		request.setMethod("PUT");
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod
				.annotNotPresent(RequestParam.class).arg(List.class, MultipartFile.class);

		Object actual = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(actual).asInstanceOf(LIST).containsExactly(expected);
	}

	@Test
	void noMultipartContent() {
		request.setMethod("POST");
		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile.class);
		assertThatExceptionOfType(MultipartException.class).isThrownBy(() ->
				resolver.resolveArgument(param, null, webRequest, null));
	}

	@Test
	void missingMultipartFile() {
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultipartFile.class);
		assertThatExceptionOfType(MissingServletRequestPartException.class).isThrownBy(() ->
				resolver.resolveArgument(param, null, webRequest, null));
	}

	@Test
	void resolvePartNotAnnot() throws Exception {
		MockPart expected = new MockPart("part", "Hello World".getBytes());
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(expected);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(Part.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).asInstanceOf(type(Part.class)).isEqualTo(expected);
	}

	@Test
	void resolveDefaultValue() throws Exception {
		MethodParameter param = this.testMethod.annot(requestParam().notRequired("bar")).arg(String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).isEqualTo("bar");
	}

	@Test
	void missingRequestParam() {
		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(String[].class);
		assertThatExceptionOfType(MissingServletRequestParameterException.class).isThrownBy(() ->
				resolver.resolveArgument(param, null, webRequest, null));
	}

	@Test  // SPR-10578
	public void missingRequestParamEmptyValueConvertedToNull() throws Exception {
		WebDataBinder binder = new WebRequestDataBinder(null);
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));

		WebDataBinderFactory binderFactory = mock();
		given(binderFactory.createBinder(webRequest, null, "stringNotAnnot")).willReturn(binder);
		request.addParameter("stringNotAnnot", "");

		MethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		Object arg = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(arg).isNull();
	}

	@Test  // gh-31336
	public void missingRequestParamAfterConversionWithDefaultValue() throws Exception {
		WebDataBinder binder = new WebRequestDataBinder(null);

		WebDataBinderFactory binderFactory = mock();
		given(binderFactory.createBinder(webRequest, null, "booleanParam")).willReturn(binder);
		request.addParameter("booleanParam", " ");

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Boolean.class);
		Object arg = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(arg).isEqualTo(Boolean.FALSE);
	}

	@Test
	void missingRequestParamEmptyValueNotRequired() throws Exception {
		WebDataBinder binder = new WebRequestDataBinder(null);
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));

		WebDataBinderFactory binderFactory = mock();
		given(binderFactory.createBinder(webRequest, null, "name")).willReturn(binder);
		request.addParameter("name", "");

		MethodParameter param = this.testMethod.annot(requestParam().notRequired()).arg(String.class);
		Object arg = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(arg).isNull();
	}

	@Test  // gh-29550
	public void missingRequestParamEmptyValueNotRequiredWithDefaultValue() throws Exception {
		WebDataBinder binder = new WebRequestDataBinder(null);
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));

		WebDataBinderFactory binderFactory = mock();
		given(binderFactory.createBinder(webRequest, null, "name")).willReturn(binder);
		request.addParameter("name", "    ");

		MethodParameter param = this.testMethod.annot(requestParam().notRequired("bar")).arg(String.class);
		Object arg = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(arg).isEqualTo("bar");
	}

	@Test
	void resolveSimpleTypeParam() throws Exception {
		request.setParameter("stringNotAnnot", "plainValue");
		MethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).isEqualTo("plainValue");
	}

	@Test  // SPR-8561
	public void resolveSimpleTypeParamToNull() throws Exception {
		MethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).isNull();
	}

	@Test  // SPR-10180
	public void resolveEmptyValueToDefault() throws Exception {
		request.addParameter("name", "");
		MethodParameter param = this.testMethod.annot(requestParam().notRequired("bar")).arg(String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).isEqualTo("bar");
	}

	@Test
	void resolveEmptyValueWithoutDefault() throws Exception {
		request.addParameter("stringNotAnnot", "");
		MethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).isEqualTo("");
	}

	@Test
	void resolveEmptyValueRequiredWithoutDefault() throws Exception {
		request.addParameter("name", "");
		MethodParameter param = this.testMethod.annot(requestParam().notRequired()).arg(String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertThat(result).isEqualTo("");
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void resolveOptionalParamValue() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, Integer.class);
		Object result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(result).isEqualTo(Optional.empty());

		request.addParameter("name", "123");
		result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(result.getClass()).isEqualTo(Optional.class);
		assertThat(((Optional) result)).contains(123);
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void missingOptionalParamValue() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, Integer.class);
		Object result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(result).isEqualTo(Optional.empty());

		result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(result.getClass()).isEqualTo(Optional.class);
		assertThat(((Optional) result)).isNotPresent();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void resolveOptionalParamArray() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, Integer[].class);
		Object result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(result).isEqualTo(Optional.empty());

		request.addParameter("name", "123", "456");
		result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(result.getClass()).isEqualTo(Optional.class);
		assertThat((Integer[]) ((Optional) result).get()).isEqualTo(new Integer[] {123, 456});
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void missingOptionalParamArray() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, Integer[].class);
		Object result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(result).isEqualTo(Optional.empty());

		result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(result.getClass()).isEqualTo(Optional.class);
		assertThat(((Optional) result)).isNotPresent();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void resolveOptionalParamList() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, List.class);
		Object result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(result).isEqualTo(Optional.empty());

		request.addParameter("name", "123", "456");
		result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(result.getClass()).isEqualTo(Optional.class);
		assertThat(((Optional) result)).contains(Arrays.asList("123", "456"));
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void missingOptionalParamList() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, List.class);
		Object result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(result).isEqualTo(Optional.empty());

		result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(result.getClass()).isEqualTo(Optional.class);
		assertThat(((Optional) result)).isNotPresent();
	}

	@Test
	void resolveOptionalMultipartFile() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("mfile", "Hello World".getBytes());
		request.addFile(expected);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, MultipartFile.class);
		Object result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertThat(result).asInstanceOf(optional(MultipartFile.class)).contains(expected);
	}

	@Test
	void missingOptionalMultipartFile() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		request.setMethod("POST");
		request.setContentType("multipart/form-data");

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, MultipartFile.class);
		Object actual = resolver.resolveArgument(param, null, webRequest, binderFactory);

		assertThat(actual).isEqualTo(Optional.empty());
	}

	@Test
	void optionalMultipartFileWithoutMultipartRequest() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(Optional.class, MultipartFile.class);
		Object actual = resolver.resolveArgument(param, null, webRequest, binderFactory);

		assertThat(actual).isEqualTo(Optional.empty());
	}

	@Test
	void resolveNameFromSystemPropertyThroughPlaceholder() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		Integer expected = 100;
		request.addParameter("name", expected.toString());

		System.setProperty("systemProperty", "name");

		try {
			MethodParameter param = this.testMethod.annot(requestParam().name("${systemProperty}")).arg(Integer.class);
			Object result = resolver.resolveArgument(param, null, webRequest, binderFactory);
			assertThat(result).isInstanceOf(Integer.class);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	void missingParameterFromSystemPropertyThroughPlaceholder() {
		String expected = "name";
		System.setProperty("systemProperty", expected);

		MethodParameter param = this.testMethod.annot(requestParam().name("${systemProperty}")).arg(Integer.class);
		assertThatThrownBy(() ->
				resolver.resolveArgument(param, null, webRequest, null))
				.isInstanceOf(MissingServletRequestParameterException.class)
				.extracting("parameterName").isEqualTo(expected);

		System.clearProperty("systemProperty");
	}

	@Test
	void notNullablePrimitiveParameterFromSystemPropertyThroughPlaceholder() {
		String expected = "sysbar";
		System.setProperty("systemProperty", expected);

		MethodParameter param = this.testMethod.annot(requestParam().name("${systemProperty}").notRequired()).arg(int.class);
		assertThatThrownBy(() ->
				resolver.resolveArgument(param, null, webRequest, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(expected);

		System.clearProperty("systemProperty");
	}

	@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
	public void handle(
			@RequestParam(name = "name", defaultValue = "bar") String param1,
			@RequestParam("name") String[] param2,
			@RequestParam("name") Map<?, ?> param3,
			@RequestParam("mfile") MultipartFile param4,
			@RequestParam("mfilelist") List<MultipartFile> param5,
			@RequestParam("mfilearray") MultipartFile[] param6,
			@RequestParam("pfile") Part param7,
			@RequestParam("pfilelist") List<Part> param8,
			@RequestParam("pfilearray") Part[] param9,
			@RequestParam Map<?, ?> param10,
			String stringNotAnnot,
			MultipartFile multipartFileNotAnnot,
			List<MultipartFile> multipartFileList,
			Part part,
			@RequestPart MultipartFile requestPartAnnot,
			@RequestParam("name") String paramRequired,
			@RequestParam(name = "name", required = false) String paramNotRequired,
			@RequestParam("name") Optional<Integer> paramOptional,
			@RequestParam("name") Optional<Integer[]> paramOptionalArray,
			@RequestParam("name") Optional<List<?>> paramOptionalList,
			@RequestParam("mfile") Optional<MultipartFile> multipartFileOptional,
			@RequestParam(defaultValue = "false") Boolean booleanParam,
			@RequestParam("${systemProperty}") Integer placeholderParam,
			@RequestParam(name = "${systemProperty}", required = false) int primitivePlaceholderParam) {
	}

}
