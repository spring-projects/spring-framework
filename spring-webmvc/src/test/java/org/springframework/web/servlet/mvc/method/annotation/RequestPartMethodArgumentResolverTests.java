/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.Part;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

/**
 * Test fixture with {@link RequestPartMethodArgumentResolver} and mock {@link HttpMessageConverter}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class RequestPartMethodArgumentResolverTests {

	private HttpMessageConverter<SimpleBean> messageConverter;

	private RequestPartMethodArgumentResolver resolver;

	private MultipartFile multipartFile1;

	private MultipartFile multipartFile2;

	private CloseTrackingInputStream trackedStream;

	private MockMultipartHttpServletRequest multipartRequest;

	private NativeWebRequest webRequest;

	private MethodParameter paramRequestPart;
	private MethodParameter paramNamedRequestPart;
	private MethodParameter paramValidRequestPart;
	private MethodParameter paramMultipartFile;
	private MethodParameter paramMultipartFileList;
	private MethodParameter paramMultipartFileArray;
	private MethodParameter paramInt;
	private MethodParameter paramMultipartFileNotAnnot;
	private MethodParameter paramPart;
	private MethodParameter paramPartList;
	private MethodParameter paramPartArray;
	private MethodParameter paramRequestParamAnnot;
	private MethodParameter optionalMultipartFile;
	private MethodParameter optionalMultipartFileList;
	private MethodParameter optionalPart;
	private MethodParameter optionalPartList;
	private MethodParameter optionalRequestPart;


	@BeforeEach
	@SuppressWarnings("unchecked")
	public void setup() throws Exception {
		messageConverter = mock(HttpMessageConverter.class);
		given(messageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));

		resolver = new RequestPartMethodArgumentResolver(Collections.singletonList(messageConverter));
		reset(messageConverter);

		byte[] content = "doesn't matter as long as not empty".getBytes(StandardCharsets.UTF_8);
		multipartFile1 = new MockMultipartFile("requestPart", "", "text/plain", content) {
			@Override
			public InputStream getInputStream() throws IOException {
				CloseTrackingInputStream in = new CloseTrackingInputStream(super.getInputStream());
				trackedStream = in;
				return in;
			}
		};
		multipartFile2 = new MockMultipartFile("requestPart", "", "text/plain", content);
		multipartRequest = new MockMultipartHttpServletRequest();
		multipartRequest.addFile(multipartFile1);
		multipartRequest.addFile(multipartFile2);
		multipartRequest.addFile(new MockMultipartFile("otherPart", "", "text/plain", content));
		webRequest = new ServletWebRequest(multipartRequest, new MockHttpServletResponse());

		Method method = ReflectionUtils.findMethod(getClass(), "handle", (Class<?>[]) null);
		paramRequestPart = new SynthesizingMethodParameter(method, 0);
		paramRequestPart.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		paramNamedRequestPart = new SynthesizingMethodParameter(method, 1);
		paramValidRequestPart = new SynthesizingMethodParameter(method, 2);
		paramMultipartFile = new SynthesizingMethodParameter(method, 3);
		paramMultipartFileList = new SynthesizingMethodParameter(method, 4);
		paramMultipartFileArray = new SynthesizingMethodParameter(method, 5);
		paramInt = new SynthesizingMethodParameter(method, 6);
		paramMultipartFileNotAnnot = new SynthesizingMethodParameter(method, 7);
		paramMultipartFileNotAnnot.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		paramPart = new SynthesizingMethodParameter(method, 8);
		paramPart.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		paramPartList = new SynthesizingMethodParameter(method, 9);
		paramPartArray = new SynthesizingMethodParameter(method, 10);
		paramRequestParamAnnot = new SynthesizingMethodParameter(method, 11);
		optionalMultipartFile = new SynthesizingMethodParameter(method, 12);
		optionalMultipartFile.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		optionalMultipartFileList = new SynthesizingMethodParameter(method, 13);
		optionalMultipartFileList.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		optionalPart = new SynthesizingMethodParameter(method, 14);
		optionalPart.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		optionalPartList = new SynthesizingMethodParameter(method, 15);
		optionalPartList.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		optionalRequestPart = new SynthesizingMethodParameter(method, 16);
	}


	@Test
	public void supportsParameter() {
		assertThat(resolver.supportsParameter(paramRequestPart)).isTrue();
		assertThat(resolver.supportsParameter(paramNamedRequestPart)).isTrue();
		assertThat(resolver.supportsParameter(paramValidRequestPart)).isTrue();
		assertThat(resolver.supportsParameter(paramMultipartFile)).isTrue();
		assertThat(resolver.supportsParameter(paramMultipartFileList)).isTrue();
		assertThat(resolver.supportsParameter(paramMultipartFileArray)).isTrue();
		assertThat(resolver.supportsParameter(paramInt)).isFalse();
		assertThat(resolver.supportsParameter(paramMultipartFileNotAnnot)).isTrue();
		assertThat(resolver.supportsParameter(paramPart)).isTrue();
		assertThat(resolver.supportsParameter(paramPartList)).isTrue();
		assertThat(resolver.supportsParameter(paramPartArray)).isTrue();
		assertThat(resolver.supportsParameter(paramRequestParamAnnot)).isFalse();
		assertThat(resolver.supportsParameter(optionalMultipartFile)).isTrue();
		assertThat(resolver.supportsParameter(optionalMultipartFileList)).isTrue();
		assertThat(resolver.supportsParameter(optionalPart)).isTrue();
		assertThat(resolver.supportsParameter(optionalPartList)).isTrue();
		assertThat(resolver.supportsParameter(optionalRequestPart)).isTrue();
	}

	@Test
	public void resolveMultipartFile() throws Exception {
		Object actual = resolver.resolveArgument(paramMultipartFile, null, webRequest, null);
		assertThat(actual).isSameAs(multipartFile1);
	}

	@Test
	public void resolveMultipartFileList() throws Exception {
		Object actual = resolver.resolveArgument(paramMultipartFileList, null, webRequest, null);
		assertThat(actual instanceof List).isTrue();
		assertThat(actual).isEqualTo(Arrays.asList(multipartFile1, multipartFile2));
	}

	@Test
	public void resolveMultipartFileArray() throws Exception {
		Object actual = resolver.resolveArgument(paramMultipartFileArray, null, webRequest, null);
		assertThat(actual).isNotNull();
		assertThat(actual instanceof MultipartFile[]).isTrue();
		MultipartFile[] parts = (MultipartFile[]) actual;
		assertThat(parts.length).isEqualTo(2);
		assertThat(multipartFile1).isEqualTo(parts[0]);
		assertThat(multipartFile2).isEqualTo(parts[1]);
	}

	@Test
	public void resolveMultipartFileNotAnnotArgument() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("multipartFileNotAnnot", "Hello World".getBytes());
		request.addFile(expected);
		request.addFile(new MockMultipartFile("otherPart", "", "text/plain", "Hello World".getBytes()));
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramMultipartFileNotAnnot, null, webRequest, null);

		assertThat(result instanceof MultipartFile).isTrue();
		assertThat(result).as("Invalid result").isEqualTo(expected);
	}

	@Test
	public void resolvePartArgument() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		MockPart expected = new MockPart("part", "Hello World".getBytes());
		request.addPart(expected);
		request.addPart(new MockPart("otherPart", "Hello World".getBytes()));
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramPart, null, webRequest, null);
		assertThat(result instanceof Part).isTrue();
		assertThat(result).as("Invalid result").isEqualTo(expected);
	}

	@Test
	public void resolvePartListArgument() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		MockPart part1 = new MockPart("requestPart", "Hello World 1".getBytes());
		MockPart part2 = new MockPart("requestPart", "Hello World 2".getBytes());
		request.addPart(part1);
		request.addPart(part2);
		request.addPart(new MockPart("otherPart", "Hello World".getBytes()));
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramPartList, null, webRequest, null);
		assertThat(result instanceof List).isTrue();
		assertThat(result).isEqualTo(Arrays.asList(part1, part2));
	}

	@Test
	public void resolvePartArrayArgument() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		MockPart part1 = new MockPart("requestPart", "Hello World 1".getBytes());
		MockPart part2 = new MockPart("requestPart", "Hello World 2".getBytes());
		request.addPart(part1);
		request.addPart(part2);
		request.addPart(new MockPart("otherPart", "Hello World".getBytes()));
		webRequest = new ServletWebRequest(request);

		Object result = resolver.resolveArgument(paramPartArray, null, webRequest, null);
		assertThat(result instanceof Part[]).isTrue();
		Part[] parts = (Part[]) result;
		assertThat(parts.length).isEqualTo(2);
		assertThat(part1).isEqualTo(parts[0]);
		assertThat(part2).isEqualTo(parts[1]);
	}

	@Test
	public void resolveRequestPart() throws Exception {
		testResolveArgument(new SimpleBean("foo"), paramRequestPart);
	}

	@Test
	public void resolveNamedRequestPart() throws Exception {
		testResolveArgument(new SimpleBean("foo"), paramNamedRequestPart);
	}

	@Test
	public void resolveNamedRequestPartNotPresent() throws Exception {
		testResolveArgument(null, paramNamedRequestPart);
	}

	@Test
	public void resolveRequestPartNotValid() throws Exception {
		assertThatExceptionOfType(MethodArgumentNotValidException.class).isThrownBy(() ->
				testResolveArgument(new SimpleBean(null), paramValidRequestPart))
			.satisfies(ex -> {
				BindingResult bindingResult = ex.getBindingResult();
				assertThat(bindingResult.getObjectName()).isEqualTo("requestPart");
				assertThat(bindingResult.getErrorCount()).isEqualTo(1);
				assertThat(bindingResult.getFieldError("name")).isNotNull();
			});
	}

	@Test
	public void resolveRequestPartValid() throws Exception {
		testResolveArgument(new SimpleBean("foo"), paramValidRequestPart);
	}

	@Test
	public void resolveRequestPartRequired() throws Exception {
		assertThatExceptionOfType(MissingServletRequestPartException.class).isThrownBy(() ->
				testResolveArgument(null, paramValidRequestPart))
			.satisfies(ex -> assertThat(ex.getRequestPartName()).isEqualTo("requestPart"));
	}

	@Test
	public void resolveRequestPartNotRequired() throws Exception {
		testResolveArgument(new SimpleBean("foo"), paramValidRequestPart);
	}

	@Test // gh-26501
	public void resolveRequestPartWithoutContentType() throws Exception {
		MockMultipartHttpServletRequest servletRequest = new MockMultipartHttpServletRequest();
		servletRequest.addPart(new MockPart("requestPartString", "part value".getBytes(StandardCharsets.UTF_8)));
		ServletWebRequest webRequest = new ServletWebRequest(servletRequest, new MockHttpServletResponse());

		List<HttpMessageConverter<?>> converters = Collections.singletonList(new StringHttpMessageConverter());
		RequestPartMethodArgumentResolver resolver = new RequestPartMethodArgumentResolver(converters);
		MethodParameter parameter = ResolvableMethod.on(getClass()).named("handle").build().arg(String.class);

		Object actualValue = resolver.resolveArgument(
				parameter, new ModelAndViewContainer(), webRequest, new ValidatingBinderFactory());

		assertThat(actualValue).isEqualTo("part value");
	}

	@Test
	public void isMultipartRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		assertThatExceptionOfType(MultipartException.class).isThrownBy(() ->
				resolver.resolveArgument(paramMultipartFile, new ModelAndViewContainer(), new ServletWebRequest(request), null));
	}

	@Test  // SPR-9079
	public void isMultipartRequestPut() throws Exception {
		this.multipartRequest.setMethod("PUT");
		Object actualValue = resolver.resolveArgument(paramMultipartFile, null, webRequest, null);
		assertThat(actualValue).isSameAs(multipartFile1);
	}

	@Test
	public void resolveOptionalMultipartFileArgument() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("optionalMultipartFile", "Hello World".getBytes());
		request.addFile(expected);
		request.addFile(new MockMultipartFile("otherPart", "", "text/plain", "Hello World".getBytes()));
		webRequest = new ServletWebRequest(request);

		Object actualValue = resolver.resolveArgument(optionalMultipartFile, null, webRequest, null);
		boolean condition1 = actualValue instanceof Optional;
		assertThat(condition1).isTrue();
		assertThat(((Optional<?>) actualValue).get()).as("Invalid result").isEqualTo(expected);

		actualValue = resolver.resolveArgument(optionalMultipartFile, null, webRequest, null);
		assertThat(actualValue instanceof Optional).isTrue();
		assertThat(((Optional<?>) actualValue).get()).as("Invalid result").isEqualTo(expected);
	}

	@Test
	public void resolveOptionalMultipartFileArgumentNotPresent() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		webRequest = new ServletWebRequest(request);

		Object actualValue = resolver.resolveArgument(optionalMultipartFile, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());

		actualValue = resolver.resolveArgument(optionalMultipartFile, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());
	}

	@Test
	public void resolveOptionalMultipartFileArgumentWithoutMultipartRequest() throws Exception {
		webRequest = new ServletWebRequest(new MockHttpServletRequest());

		Object actualValue = resolver.resolveArgument(optionalMultipartFile, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());

		actualValue = resolver.resolveArgument(optionalMultipartFile, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());
	}

	@Test
	public void resolveOptionalMultipartFileList() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartFile expected = new MockMultipartFile("requestPart", "Hello World".getBytes());
		request.addFile(expected);
		request.addFile(new MockMultipartFile("otherPart", "", "text/plain", "Hello World".getBytes()));
		webRequest = new ServletWebRequest(request);

		Object actualValue = resolver.resolveArgument(optionalMultipartFileList, null, webRequest, null);
		boolean condition1 = actualValue instanceof Optional;
		assertThat(condition1).isTrue();
		assertThat(((Optional<?>) actualValue).get()).as("Invalid result").isEqualTo(Collections.singletonList(expected));

		actualValue = resolver.resolveArgument(optionalMultipartFileList, null, webRequest, null);
		assertThat(actualValue instanceof Optional).isTrue();
		assertThat(((Optional<?>) actualValue).get()).as("Invalid result").isEqualTo(Collections.singletonList(expected));
	}

	@Test
	public void resolveOptionalMultipartFileListNotPresent() throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		webRequest = new ServletWebRequest(request);

		Object actualValue = resolver.resolveArgument(optionalMultipartFileList, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());

		actualValue = resolver.resolveArgument(optionalMultipartFileList, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());
	}

	@Test
	public void resolveOptionalMultipartFileListWithoutMultipartRequest() throws Exception {
		webRequest = new ServletWebRequest(new MockHttpServletRequest());

		Object actualValue = resolver.resolveArgument(optionalMultipartFileList, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());

		actualValue = resolver.resolveArgument(optionalMultipartFileList, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());
	}

	@Test
	public void resolveOptionalPartArgument() throws Exception {
		MockPart expected = new MockPart("optionalPart", "Hello World".getBytes());
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(expected);
		request.addPart(new MockPart("otherPart", "Hello World".getBytes()));
		webRequest = new ServletWebRequest(request);

		Object actualValue = resolver.resolveArgument(optionalPart, null, webRequest, null);
		boolean condition1 = actualValue instanceof Optional;
		assertThat(condition1).isTrue();
		assertThat(((Optional<?>) actualValue).get()).as("Invalid result").isEqualTo(expected);

		actualValue = resolver.resolveArgument(optionalPart, null, webRequest, null);
		assertThat(actualValue instanceof Optional).isTrue();
		assertThat(((Optional<?>) actualValue).get()).as("Invalid result").isEqualTo(expected);
	}

	@Test
	public void resolveOptionalPartArgumentNotPresent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		webRequest = new ServletWebRequest(request);

		Object actualValue = resolver.resolveArgument(optionalPart, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());

		actualValue = resolver.resolveArgument(optionalPart, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());
	}

	@Test
	public void resolveOptionalPartArgumentWithoutMultipartRequest() throws Exception {
		webRequest = new ServletWebRequest(new MockHttpServletRequest());

		Object actualValue = resolver.resolveArgument(optionalPart, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());

		actualValue = resolver.resolveArgument(optionalPart, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());
	}

	@Test
	public void resolveOptionalPartList() throws Exception {
		MockPart expected = new MockPart("requestPart", "Hello World".getBytes());
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		request.addPart(expected);
		request.addPart(new MockPart("otherPart", "Hello World".getBytes()));
		webRequest = new ServletWebRequest(request);

		Object actualValue = resolver.resolveArgument(optionalPartList, null, webRequest, null);
		boolean condition1 = actualValue instanceof Optional;
		assertThat(condition1).isTrue();
		assertThat(((Optional<?>) actualValue).get()).as("Invalid result").isEqualTo(Collections.singletonList(expected));

		actualValue = resolver.resolveArgument(optionalPartList, null, webRequest, null);
		assertThat(actualValue instanceof Optional).isTrue();
		assertThat(((Optional<?>) actualValue).get()).as("Invalid result").isEqualTo(Collections.singletonList(expected));
	}

	@Test
	public void resolveOptionalPartListNotPresent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		webRequest = new ServletWebRequest(request);

		Object actualValue = resolver.resolveArgument(optionalPartList, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());

		actualValue = resolver.resolveArgument(optionalPartList, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());
	}

	@Test
	public void resolveOptionalPartListWithoutMultipartRequest() throws Exception {
		webRequest = new ServletWebRequest(new MockHttpServletRequest());

		Object actualValue = resolver.resolveArgument(optionalPartList, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());

		actualValue = resolver.resolveArgument(optionalPartList, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());
	}

	@Test
	public void resolveOptionalRequestPart() throws Exception {
		SimpleBean simpleBean = new SimpleBean("foo");
		given(messageConverter.canRead(SimpleBean.class, MediaType.TEXT_PLAIN)).willReturn(true);
		given(messageConverter.read(eq(SimpleBean.class), isA(HttpInputMessage.class))).willReturn(simpleBean);

		ModelAndViewContainer mavContainer = new ModelAndViewContainer();

		Object actualValue = resolver.resolveArgument(
				optionalRequestPart, mavContainer, webRequest, new ValidatingBinderFactory());
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.of(simpleBean));
		assertThat(mavContainer.isRequestHandled()).as("The requestHandled flag shouldn't change").isFalse();

		actualValue = resolver.resolveArgument(optionalRequestPart, mavContainer, webRequest, new ValidatingBinderFactory());
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.of(simpleBean));
		assertThat(mavContainer.isRequestHandled()).as("The requestHandled flag shouldn't change").isFalse();
	}

	@Test
	public void resolveOptionalRequestPartNotPresent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setContentType("multipart/form-data");
		webRequest = new ServletWebRequest(request);

		Object actualValue = resolver.resolveArgument(optionalRequestPart, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());

		actualValue = resolver.resolveArgument(optionalRequestPart, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());
	}

	@Test
	public void resolveOptionalRequestPartWithoutMultipartRequest() throws Exception {
		webRequest = new ServletWebRequest(new MockHttpServletRequest());

		Object actualValue = resolver.resolveArgument(optionalRequestPart, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());

		actualValue = resolver.resolveArgument(optionalRequestPart, null, webRequest, null);
		assertThat(actualValue).as("Invalid argument value").isEqualTo(Optional.empty());
	}


	private void testResolveArgument(SimpleBean argValue, MethodParameter parameter) throws Exception {
		given(messageConverter.canRead(SimpleBean.class, MediaType.TEXT_PLAIN)).willReturn(true);
		given(messageConverter.read(eq(SimpleBean.class), isA(HttpInputMessage.class))).willReturn(argValue);

		ModelAndViewContainer mavContainer = new ModelAndViewContainer();

		Object actualValue = resolver.resolveArgument(parameter, mavContainer, webRequest, new ValidatingBinderFactory());
		assertThat(actualValue).as("Invalid argument value").isEqualTo(argValue);
		assertThat(mavContainer.isRequestHandled()).as("The requestHandled flag shouldn't change").isFalse();
		assertThat(trackedStream != null && trackedStream.closed).isTrue();
	}


	private static class SimpleBean {

		@NotNull
		private final String name;

		public SimpleBean(String name) {
			this.name = name;
		}

		@SuppressWarnings("unused")
		public String getName() {
			return name;
		}
	}


	private static class ValidatingBinderFactory implements WebDataBinderFactory {

		@Override
		public WebDataBinder createBinder(NativeWebRequest webRequest, @Nullable Object target,
				String objectName) throws Exception {

			LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
			validator.afterPropertiesSet();
			WebDataBinder dataBinder = new WebDataBinder(target, objectName);
			dataBinder.setValidator(validator);
			return dataBinder;
		}
	}


	private static class CloseTrackingInputStream extends FilterInputStream {

		public boolean closed = false;

		public CloseTrackingInputStream(InputStream in) {
			super(in);
		}

		@Override
		public void close() {
			this.closed = true;
		}
	}


	@SuppressWarnings("unused")
	public void handle(
			@RequestPart SimpleBean requestPart,
			@RequestPart(value="requestPart", required=false) SimpleBean namedRequestPart,
			@Valid @RequestPart("requestPart") SimpleBean validRequestPart,
			@RequestPart("requestPart") MultipartFile multipartFile,
			@RequestPart("requestPart") List<MultipartFile> multipartFileList,
			@RequestPart("requestPart") MultipartFile[] multipartFileArray,
			int i,
			MultipartFile multipartFileNotAnnot,
			Part part,
			@RequestPart("requestPart") List<Part> partList,
			@RequestPart("requestPart") Part[] partArray,
			@RequestParam MultipartFile requestParamAnnot,
			Optional<MultipartFile> optionalMultipartFile,
			@RequestPart("requestPart") Optional<List<MultipartFile>> optionalMultipartFileList,
			Optional<Part> optionalPart,
			@RequestPart("requestPart") Optional<List<Part>> optionalPartList,
			@RequestPart("requestPart") Optional<SimpleBean> optionalRequestPart,
			@RequestPart("requestPartString") String requestPartString) {
	}

}
