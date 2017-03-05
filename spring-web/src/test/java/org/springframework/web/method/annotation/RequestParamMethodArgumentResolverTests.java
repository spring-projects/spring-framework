/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.servlet.http.Part;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockMultipartFile;
import org.springframework.mock.web.test.MockMultipartHttpServletRequest;
import org.springframework.mock.web.test.MockPart;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

/**
 * Test fixture with {@link org.springframework.web.method.annotation.RequestParamMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class RequestParamMethodArgumentResolverTests {

	private RequestParamMethodArgumentResolver resolver;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest request;

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();

	@Before
	public void setUp() throws Exception {
		resolver = new RequestParamMethodArgumentResolver(null, true);
		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request, new MockHttpServletResponse());
	}

	@Test
	public void supportsParameter() {
		resolver = new RequestParamMethodArgumentResolver(null, true);

		MethodParameter param = this.testMethod.annotated(RequestParam.class, value("bar")).arg(String.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class).arg(String[].class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class, name("name")).arg(Map.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class).arg(MultipartFile.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class).arg(List.class, MultipartFile.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class).arg(MultipartFile[].class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class).arg(Part.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class).arg(List.class, Part.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class).arg(Part[].class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class, name("")).arg(Map.class);
		assertFalse(resolver.supportsParameter(param));

		param = this.testMethod.notAnnotated(RequestParam.class).arg(String.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.notAnnotated().arg(MultipartFile.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.notAnnotated(RequestParam.class).arg(List.class, MultipartFile.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.notAnnotated(RequestParam.class).arg(Part.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestPart.class).arg(MultipartFile.class);
		assertFalse(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class, required(), value("")).arg(String.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class, required().negate()).arg(String.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class).arg(Optional.class, Integer.class);
		assertTrue(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestParam.class).arg(Optional.class, MultipartFile.class);
		assertTrue(resolver.supportsParameter(param));

		resolver = new RequestParamMethodArgumentResolver(null, false);

		param = this.testMethod.notAnnotated(RequestParam.class).arg(String.class);
		assertFalse(resolver.supportsParameter(param));

		param = this.testMethod.annotated(RequestPart.class).arg(MultipartFile.class);
		assertFalse(resolver.supportsParameter(param));
	}

	@Test
	public void resolveString() throws Exception {
		String expected = "foo";
		request.addParameter("name", expected);

		MethodParameter param = this.testMethod.annotated(RequestParam.class, value("bar")).arg(String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertTrue(result instanceof String);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveStringArray() throws Exception {
		String[] expected = new String[] {"foo", "bar"};
		request.addParameter("name", expected);

		MethodParameter param = this.testMethod.annotated(RequestParam.class).arg(String[].class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertTrue(result instanceof String[]);
		assertArrayEquals("Invalid result", expected, (String[]) result);
	}

	@Test
	public void resolveMultipartFile() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("mfile", "Hello World".getBytes());
		request.addFile(expected);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotated(RequestParam.class).arg(MultipartFile.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertTrue(result instanceof MultipartFile);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveMultipartFileList() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected1 = new MockMultipartFile("mfilelist", "Hello World 1".getBytes());
		MultipartFile expected2 = new MockMultipartFile("mfilelist", "Hello World 2".getBytes());
		request.addFile(expected1);
		request.addFile(expected2);
		request.addFile(new MockMultipartFile("other", "Hello World 3".getBytes()));
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod
				.annotated(RequestParam.class).arg(List.class, MultipartFile.class);

		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertTrue(result instanceof List);
		assertEquals(Arrays.asList(expected1, expected2), result);
	}

	@Test
	public void resolveMultipartFileArray() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected1 = new MockMultipartFile("mfilearray", "Hello World 1".getBytes());
		MultipartFile expected2 = new MockMultipartFile("mfilearray", "Hello World 2".getBytes());
		request.addFile(expected1);
		request.addFile(expected2);
		request.addFile(new MockMultipartFile("other", "Hello World 3".getBytes()));
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotated(RequestParam.class).arg(MultipartFile[].class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertTrue(result instanceof MultipartFile[]);
		MultipartFile[] parts = (MultipartFile[]) result;
		assertEquals(2, parts.length);
		assertEquals(parts[0], expected1);
		assertEquals(parts[1], expected2);
	}

	@Test
	public void resolvePart() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockPart expected = new MockPart("pfile", "Hello World".getBytes());
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(expected);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotated(RequestParam.class).arg(Part.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertTrue(result instanceof Part);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolvePartList() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		MockPart expected1 = new MockPart("pfilelist", "Hello World 1".getBytes());
		MockPart expected2 = new MockPart("pfilelist", "Hello World 2".getBytes());
		request.addPart(expected1);
		request.addPart(expected2);
		request.addPart(new MockPart("other", "Hello World 3".getBytes()));
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotated(RequestParam.class).arg(List.class, Part.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertTrue(result instanceof List);
		assertEquals(Arrays.asList(expected1, expected2), result);
	}

	@Test
	public void resolvePartArray() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockPart expected1 = new MockPart("pfilearray", "Hello World 1".getBytes());
		MockPart expected2 = new MockPart("pfilearray", "Hello World 2".getBytes());
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(expected1);
		request.addPart(expected2);
		request.addPart(new MockPart("other", "Hello World 3".getBytes()));
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.annotated(RequestParam.class).arg(Part[].class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertTrue(result instanceof Part[]);
		Part[] parts = (Part[]) result;
		assertEquals(2, parts.length);
		assertEquals(parts[0], expected1);
		assertEquals(parts[1], expected2);
	}

	@Test
	public void resolveMultipartFileNotAnnot() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("multipartFileNotAnnot", "Hello World".getBytes());
		request.addFile(expected);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.notAnnotated().arg(MultipartFile.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertTrue(result instanceof MultipartFile);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveMultipartFileListNotAnnotated() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected1 = new MockMultipartFile("multipartFileList", "Hello World 1".getBytes());
		MultipartFile expected2 = new MockMultipartFile("multipartFileList", "Hello World 2".getBytes());
		request.addFile(expected1);
		request.addFile(expected2);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod
				.notAnnotated(RequestParam.class).arg(List.class, MultipartFile.class);

		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertTrue(result instanceof List);
		assertEquals(Arrays.asList(expected1, expected2), result);
	}

	@Test(expected = MultipartException.class)
	public void isMultipartRequest() throws Exception {
		MethodParameter param = this.testMethod.annotated(RequestParam.class).arg(MultipartFile.class);
		resolver.resolveArgument(param, null, webRequest, null);
		fail("Expected exception: request is not a multipart request");
	}

	@Test  // SPR-9079
	public void isMultipartRequestHttpPut() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("multipartFileList", "Hello World".getBytes());
		request.addFile(expected);
		request.setMethod("PUT");
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod
				.notAnnotated(RequestParam.class).arg(List.class, MultipartFile.class);

		Object actual = resolver.resolveArgument(param, null, webRequest, null);
		assertTrue(actual instanceof List);
		assertEquals(expected, ((List<?>) actual).get(0));
	}

	@Test(expected = MultipartException.class)
	public void noMultipartContent() throws Exception {
		request.setMethod("POST");
		MethodParameter param = this.testMethod.annotated(RequestParam.class).arg(MultipartFile.class);
		resolver.resolveArgument(param, null, webRequest, null);
		fail("Expected exception: no multipart content");
	}

	@Test(expected = MissingServletRequestPartException.class)
	public void missingMultipartFile() throws Exception {
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		MethodParameter param = this.testMethod.annotated(RequestParam.class).arg(MultipartFile.class);
		resolver.resolveArgument(param, null, webRequest, null);
		fail("Expected exception: no such part found");
	}

	@Test
	public void resolvePartNotAnnot() throws Exception {
		MockPart expected = new MockPart("part", "Hello World".getBytes());
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(expected);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod.notAnnotated(RequestParam.class).arg(Part.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertTrue(result instanceof Part);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveDefaultValue() throws Exception {
		MethodParameter param = this.testMethod.annotated(RequestParam.class, value("bar")).arg(String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertTrue(result instanceof String);
		assertEquals("Invalid result", "bar", result);
	}

	@Test(expected = MissingServletRequestParameterException.class)
	public void missingRequestParam() throws Exception {
		MethodParameter param = this.testMethod.annotated(RequestParam.class).arg(String[].class);
		resolver.resolveArgument(param, null, webRequest, null);
		fail("Expected exception");
	}

	@Test  // SPR-10578
	public void missingRequestParamEmptyValueConvertedToNull() throws Exception {
		WebDataBinder binder = new WebRequestDataBinder(null);
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));

		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(webRequest, null, "stringNotAnnot")).willReturn(binder);

		this.request.addParameter("stringNotAnnot", "");

		MethodParameter param = this.testMethod.notAnnotated(RequestParam.class).arg(String.class);
		Object arg = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertNull(arg);
	}

	@Test
	public void missingRequestParamEmptyValueNotRequired() throws Exception {
		WebDataBinder binder = new WebRequestDataBinder(null);
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));

		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(webRequest, null, "name")).willReturn(binder);

		this.request.addParameter("name", "");

		MethodParameter param = this.testMethod.annotated(RequestParam.class, required().negate()).arg(String.class);
		Object arg = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertNull(arg);
	}

	@Test
	public void resolveSimpleTypeParam() throws Exception {
		request.setParameter("stringNotAnnot", "plainValue");
		MethodParameter param = this.testMethod.notAnnotated(RequestParam.class).arg(String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);

		assertTrue(result instanceof String);
		assertEquals("plainValue", result);
	}

	@Test  // SPR-8561
	public void resolveSimpleTypeParamToNull() throws Exception {
		MethodParameter param = this.testMethod.notAnnotated(RequestParam.class).arg(String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertNull(result);
	}

	@Test  // SPR-10180
	public void resolveEmptyValueToDefault() throws Exception {
		this.request.addParameter("name", "");
		MethodParameter param = this.testMethod.annotated(RequestParam.class, value("bar")).arg(String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertEquals("bar", result);
	}

	@Test
	public void resolveEmptyValueWithoutDefault() throws Exception {
		this.request.addParameter("stringNotAnnot", "");
		MethodParameter param = this.testMethod.notAnnotated(RequestParam.class).arg(String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertEquals("", result);
	}

	@Test
	public void resolveEmptyValueRequiredWithoutDefault() throws Exception {
		this.request.addParameter("name", "");
		MethodParameter param = this.testMethod.annotated(RequestParam.class, required(), value("")).arg(String.class);
		Object result = resolver.resolveArgument(param, null, webRequest, null);
		assertEquals("", result);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void resolveOptionalParamValue() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		MethodParameter param = this.testMethod
				.annotated(RequestParam.class).arg(Optional.class, Integer.class);

		Object result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertEquals(Optional.empty(), result);

		this.request.addParameter("name", "123");
		result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertEquals(Optional.class, result.getClass());
		assertEquals(123, ((Optional) result).get());
	}

	@Test
	public void resolveOptionalMultipartFile() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("mfile", "Hello World".getBytes());
		request.addFile(expected);
		webRequest = new ServletWebRequest(request);

		MethodParameter param = this.testMethod
				.annotated(RequestParam.class).arg(Optional.class, MultipartFile.class);

		Object result = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertTrue(result instanceof Optional);
		assertEquals("Invalid result", expected, ((Optional<?>) result).get());
	}

	@Test
	public void missingOptionalMultipartFile() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		request.setMethod("POST");
		request.setContentType("multipart/form-data");

		MethodParameter param = this.testMethod
				.annotated(RequestParam.class).arg(Optional.class, MultipartFile.class);

		Object actual = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertEquals(Optional.empty(), actual);
	}

	@Test
	public void optionalMultipartFileWithoutMultipartRequest() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		MethodParameter param = this.testMethod
				.annotated(RequestParam.class).arg(Optional.class, MultipartFile.class);

		Object actual = resolver.resolveArgument(param, null, webRequest, binderFactory);
		assertEquals(Optional.empty(), actual);
	}

	private Predicate<RequestParam> name(String name) {
		return a -> name.equals(a.name());
	}

	private Predicate<RequestParam> required() {
		return RequestParam::required;
	}

	private Predicate<RequestParam> value(String value) {
		return !value.isEmpty() ?
				requestParam -> value.equals(requestParam.defaultValue()) :
				requestParam -> ValueConstants.DEFAULT_NONE.equals(requestParam.defaultValue());
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
			@RequestParam("mfile") Optional<MultipartFile> multipartFileOptional) {
	}

}
