/*
 * Copyright 2002-2023 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.xmlunit.assertj.XmlAssert;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link RequestResponseBodyMethodProcessor} with actual delegation to
 * {@link HttpMessageConverter} instances.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @see RequestResponseBodyMethodProcessorMockTests
 */
@SuppressWarnings("unused")
class RequestResponseBodyMethodProcessorTests {

	private final ModelAndViewContainer container = new ModelAndViewContainer();

	private final MockHttpServletRequest servletRequest = new MockHttpServletRequest();

	private final MockHttpServletResponse servletResponse = new MockHttpServletResponse();

	private final NativeWebRequest request = new ServletWebRequest(servletRequest, servletResponse);

	private final ValidatingBinderFactory factory = new ValidatingBinderFactory();

	private final Method method = ReflectionUtils.findMethod(getClass(), "handle",
			List.class, SimpleBean.class, MultiValueMap.class, String.class);

	private final MethodParameter paramGenericList = new MethodParameter(method, 0);
	private final MethodParameter paramSimpleBean = new MethodParameter(method, 1);
	private final MethodParameter paramMultiValueMap = new MethodParameter(method, 2);
	private final MethodParameter paramString = new MethodParameter(method, 3);
	private final MethodParameter returnTypeString = new MethodParameter(method, -1);


	@Test
	void resolveArgumentParameterizedType() throws Exception {
		String content = "[{\"name\" : \"Jad\"}, {\"name\" : \"Robert\"}]";
		this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
		this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2HttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		@SuppressWarnings("unchecked")
		List<SimpleBean> result = (List<SimpleBean>) processor.resolveArgument(
				paramGenericList, container, request, factory);

		assertThat(result).map(SimpleBean::getName).containsExactly("Jad", "Robert");
	}

	@Test
	void resolveArgumentRawTypeFromParameterizedType() throws Exception {
		String content = "fruit=apple&vegetable=kale";
		this.servletRequest.setMethod("GET");
		this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
		this.servletRequest.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);

		List<HttpMessageConverter<?>> converters = List.of(new AllEncompassingFormHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		@SuppressWarnings("unchecked")
		MultiValueMap<String, String> result = (MultiValueMap<String, String>) processor.resolveArgument(
				paramMultiValueMap, container, request, factory);

		assertThat(result).isNotNull();
		assertThat(result.getFirst("fruit")).isEqualTo("apple");
		assertThat(result.getFirst("vegetable")).isEqualTo("kale");
	}

	@Test
	void resolveArgumentClassJson() throws Exception {
		String content = "{\"name\" : \"Jad\"}";
		this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
		this.servletRequest.setContentType("application/json");

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2HttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		SimpleBean result = (SimpleBean) processor.resolveArgument(
				paramSimpleBean, container, request, factory);

		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("Jad");
	}

	@Test
	void resolveArgumentClassString() throws Exception {
		String content = "foobarbaz";
		this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
		this.servletRequest.setContentType("application/json");

		List<HttpMessageConverter<?>> converters = List.of(new StringHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		String result = (String) processor.resolveArgument(
				paramString, container, request, factory);

		assertThat(result).isEqualTo("foobarbaz");
	}

	@Test // SPR-9942
	void resolveArgumentRequiredNoContent() {
		this.servletRequest.setContent(new byte[0]);
		this.servletRequest.setContentType("text/plain");
		List<HttpMessageConverter<?>> converters = List.of(new StringHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);
		assertThatExceptionOfType(HttpMessageNotReadableException.class).isThrownBy(() ->
				processor.resolveArgument(paramString, container, request, factory));
	}

	@Test  // SPR-12778
	void resolveArgumentRequiredNoContentDefaultValue() throws Exception {
		this.servletRequest.setContent(new byte[0]);
		this.servletRequest.setContentType("text/plain");
		List<HttpMessageConverter<?>> converters = List.of(new StringHttpMessageConverter());
		List<Object> advice = List.of(new EmptyRequestBodyAdvice());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters, advice);
		String arg = (String) processor.resolveArgument(paramString, container, request, factory);
		assertThat(arg).isEqualTo("default value for empty body");
	}

	@Test  // SPR-9964
	void resolveArgumentTypeVariable() throws Exception {
		Method method = MyParameterizedController.class.getMethod("handleDto", Identifiable.class);
		HandlerMethod handlerMethod = new HandlerMethod(new MySimpleParameterizedController(), method);
		MethodParameter methodParam = handlerMethod.getMethodParameters()[0];

		String content = "{\"name\" : \"Jad\"}";
		this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
		this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2HttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		SimpleBean result = (SimpleBean) processor.resolveArgument(methodParam, container, request, factory);

		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("Jad");
	}

	@Test  // SPR-14470
	void resolveParameterizedWithTypeVariableArgument() throws Exception {
		Method method = MyParameterizedControllerWithList.class.getMethod("handleDto", List.class);
		HandlerMethod handlerMethod = new HandlerMethod(new MySimpleParameterizedControllerWithList(), method);
		MethodParameter methodParam = handlerMethod.getMethodParameters()[0];

		String content = "[{\"name\" : \"Jad\"}, {\"name\" : \"Robert\"}]";
		this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
		this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2HttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		@SuppressWarnings("unchecked")
		List<SimpleBean> result = (List<SimpleBean>) processor.resolveArgument(
				methodParam, container, request, factory);

		assertThat(result).map(SimpleBean::getName).containsExactly("Jad", "Robert");
	}

	@Test  // SPR-11225
	void resolveArgumentTypeVariableWithNonGenericConverter() throws Exception {
		Method method = MyParameterizedController.class.getMethod("handleDto", Identifiable.class);
		HandlerMethod handlerMethod = new HandlerMethod(new MySimpleParameterizedController(), method);
		MethodParameter methodParam = handlerMethod.getMethodParameters()[0];

		String content = "{\"name\" : \"Jad\"}";
		this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
		this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

		HttpMessageConverter<Object> target = new MappingJackson2HttpMessageConverter();
		HttpMessageConverter<?> proxy = ProxyFactory.getProxy(HttpMessageConverter.class, new SingletonTargetSource(target));
		List<HttpMessageConverter<?>> converters = List.of(proxy);
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		SimpleBean result = (SimpleBean) processor.resolveArgument(methodParam, container, request, factory);

		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("Jad");
	}

	@Test  // SPR-9160
	void handleReturnValueSortByQuality() throws Exception {
		this.servletRequest.addHeader("Accept", "text/plain; q=0.5, application/json");

		List<HttpMessageConverter<?>> converters =
				List.of(new MappingJackson2HttpMessageConverter(), new StringHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		processor.writeWithMessageConverters("Foo", returnTypeString, request);

		assertThat(servletResponse.getHeader("Content-Type")).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
	}

	@Test
	void handleReturnValueString() throws Exception {
		List<HttpMessageConverter<?>> converters =
					List.of(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);
		processor.handleReturnValue("Foo", returnTypeString, container, request);

		assertThat(servletResponse.getHeader("Content-Type")).isEqualTo("text/plain;charset=ISO-8859-1");
		assertThat(servletResponse.getContentAsString()).isEqualTo("Foo");
	}

	@Test  // SPR-13423
	void handleReturnValueCharSequence() throws Exception {
		Method method = ResponseBodyController.class.getMethod("handleWithCharSequence");
		MethodParameter returnType = new MethodParameter(method, -1);

		List<HttpMessageConverter<?>> converters =
				List.of(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);
		processor.handleReturnValue(new StringBuilder("Foo"), returnType, container, request);

		assertThat(servletResponse.getHeader("Content-Type")).isEqualTo("text/plain;charset=ISO-8859-1");
		assertThat(servletResponse.getContentAsString()).isEqualTo("Foo");
	}

	@Test
	void handleReturnValueStringAcceptCharset() throws Exception {
		this.servletRequest.addHeader("Accept", "text/plain;charset=UTF-8");

		List<HttpMessageConverter<?>> converters =
				List.of(new ByteArrayHttpMessageConverter(), new StringHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		processor.writeWithMessageConverters("Foo", returnTypeString, request);

		assertThat(servletResponse.getHeader("Content-Type")).isEqualTo("text/plain;charset=UTF-8");
	}

	@Test // SPR-12894
	void handleReturnValueImage() throws Exception {
		this.servletRequest.addHeader("Accept", "*/*");

		Method method = getClass().getDeclaredMethod("getImage");
		MethodParameter returnType = new MethodParameter(method, -1);

		List<HttpMessageConverter<?>> converters = List.of(new ResourceHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		ClassPathResource resource = new ClassPathResource("logo.jpg", getClass());
		processor.writeWithMessageConverters(resource, returnType, this.request);

		assertThat(this.servletResponse.getHeader("Content-Type")).isEqualTo("image/jpeg");
	}

	@Test // gh-26212
	void handleReturnValueWithObjectMapperByTypeRegistration() throws Exception {
		MediaType halFormsMediaType = MediaType.parseMediaType("application/prs.hal-forms+json");
		MediaType halMediaType = MediaType.parseMediaType("application/hal+json");

		this.servletRequest.addHeader("Accept", halFormsMediaType + "," + halMediaType);

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

		SimpleBean simpleBean = new SimpleBean();
		simpleBean.setId(12L);
		simpleBean.setName("Jason");

		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.registerObjectMappersForType(SimpleBean.class, map -> map.put(halMediaType, objectMapper));
		RequestResponseBodyMethodProcessor processor =
				new RequestResponseBodyMethodProcessor(List.of(converter));
		MethodParameter returnType = new MethodParameter(getClass().getDeclaredMethod("getSimpleBean"), -1);
		processor.writeWithMessageConverters(simpleBean, returnType, this.request);

		assertThat(this.servletResponse.getHeader("Content-Type")).isEqualTo(halMediaType.toString());
		JSONAssert.assertEquals("""
				{ "id" : 12, "name" : "Jason" }""", this.servletResponse.getContentAsString(), true);
	}

	@Test
	void problemDetailDefaultMediaType() throws Exception {
		testProblemDetailMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
	}

	@Test
	void problemDetailWhenJsonRequested() throws Exception {
		this.servletRequest.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
		testProblemDetailMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
	}

	@Test // gh-29588
	void problemDetailWhenJsonAndProblemJsonRequested() throws Exception {
		this.servletRequest.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE + "," + MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		testProblemDetailMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
	}

	@Test
	void problemDetailWhenNoMatchingMediaTypeRequested() throws Exception {
		this.servletRequest.addHeader("Accept", MediaType.APPLICATION_PDF_VALUE);
		testProblemDetailMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
	}

	private void testProblemDetailMediaType(String expectedContentType) throws Exception {
		MyProblemDetail problemDetail = new MyProblemDetail(HttpStatus.BAD_REQUEST);

		this.servletRequest.setRequestURI("/path");

		RequestResponseBodyMethodProcessor processor =
				new RequestResponseBodyMethodProcessor(List.of(
						new MappingJackson2HttpMessageConverter(), new MappingJackson2XmlHttpMessageConverter()));

		MethodParameter returnType =
				new MethodParameter(getClass().getDeclaredMethod("handleAndReturnProblemDetail"), -1);

		processor.handleReturnValue(problemDetail, returnType, this.container, this.request);

		assertThat(this.servletResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(this.servletResponse.getContentType()).isEqualTo(expectedContentType);

		if (expectedContentType.equals(MediaType.APPLICATION_PROBLEM_XML_VALUE)) {
			XmlAssert.assertThat(this.servletResponse.getContentAsString()).and("""
						<problem xmlns="urn:ietf:rfc:7807">
							<type>about:blank</type>
							<title>Bad Request</title>
							<status>400</status>
							<instance>/path</instance>
						</problem>""")
					.ignoreWhitespace()
					.areIdentical();
		}
		else {
			JSONAssert.assertEquals("""
					{
						"type":     "about:blank",
						"title":    "Bad Request",
						"status":   400,
						"instance": "/path"
					}""", this.servletResponse.getContentAsString(), false);
		}
	}

	@Test
	void problemDetailWhenProblemXmlRequested() throws Exception {
		this.servletRequest.addHeader("Accept", MediaType.APPLICATION_PROBLEM_XML_VALUE);
		testProblemDetailMediaType(MediaType.APPLICATION_PROBLEM_XML_VALUE);
	}

	@Test // SPR-13135
	void handleReturnValueWithInvalidReturnType() throws Exception {
		Method method = getClass().getDeclaredMethod("handleAndReturnOutputStream");
		MethodParameter returnType = new MethodParameter(method, -1);
		List<HttpMessageConverter<?>> converters = List.of(new StringHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);
		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class).isThrownBy(() ->
				processor.writeWithMessageConverters(new ByteArrayOutputStream(), returnType, this.request));
	}

	@Test
	void addContentDispositionHeader() throws Exception {
		ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
		factory.addMediaType("pdf", new MediaType("application", "pdf"));
		factory.afterPropertiesSet();

		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(
				List.of(new StringHttpMessageConverter()), factory.getObject());

		assertContentDisposition(processor, false, "/hello.json", "safe extension");
		assertContentDisposition(processor, false, "/hello.pdf", "registered extension");
		assertContentDisposition(processor, true, "/hello.dataless", "unknown extension");

		// path parameters
		assertContentDisposition(processor, false, "/hello.json;a=b", "path param shouldn't cause issue");
		assertContentDisposition(processor, true, "/hello.json;a=b;setup.dataless", "unknown ext in path params");
		assertContentDisposition(processor, true, "/hello.dataless;a=b;setup.json", "unknown ext in filename");
		assertContentDisposition(processor, false, "/hello.json;a=b;setup.json", "safe extensions");
		assertContentDisposition(processor, true, "/hello.json;jsessionid=foo.bar", "jsessionid shouldn't cause issue");

		// encoded dot
		assertContentDisposition(processor, true, "/hello%2Edataless;a=b;setup.json", "encoded dot in filename");
		assertContentDisposition(processor, true, "/hello.json;a=b;setup%2Edataless", "encoded dot in path params");
		assertContentDisposition(processor, true, "/hello.dataless%3Bsetup.bat", "encoded dot in path params");

		this.servletRequest.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/hello.bat");
		assertContentDisposition(processor, true, "/bonjour", "forwarded URL");
		this.servletRequest.removeAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE);
	}

	@Test
	void addContentDispositionHeaderToErrorResponse() throws Exception {
		ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();
		factory.addMediaType("pdf", new MediaType("application", "pdf"));
		factory.afterPropertiesSet();

		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(
				List.of(new StringHttpMessageConverter()), factory.getObject());

		this.servletRequest.setRequestURI("/hello.dataless");
		this.servletResponse.setStatus(400);

		processor.handleReturnValue("body", this.returnTypeString, this.container, this.request);

		String header = servletResponse.getHeader("Content-Disposition");
		assertThat(header).isEqualTo("inline;filename=f.txt");
	}

	@Test
	void supportsReturnTypeResponseBodyOnType() throws Exception {
		Method method = ResponseBodyController.class.getMethod("handle");
		MethodParameter returnType = new MethodParameter(method, -1);

		List<HttpMessageConverter<?>> converters = List.of(new StringHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		assertThat(processor.supportsReturnType(returnType)).as("Failed to recognize type-level @ResponseBody").isTrue();
	}

	@Test
	void supportsReturnTypeRestController() throws Exception {
		Method method = TestRestController.class.getMethod("handle");
		MethodParameter returnType = new MethodParameter(method, -1);

		List<HttpMessageConverter<?>> converters = List.of(new StringHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		assertThat(processor.supportsReturnType(returnType)).as("Failed to recognize type-level @RestController").isTrue();
	}

	@Test
	void jacksonJsonViewWithResponseBodyAndJsonMessageConverter() throws Exception {
		Method method = JacksonController.class.getMethod("handleResponseBody");
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
		MethodParameter methodReturnType = handlerMethod.getReturnType();

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2HttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(
				converters, null, List.of(new JsonViewResponseBodyAdvice()));

		Object returnValue = new JacksonController().handleResponseBody();
		processor.handleReturnValue(returnValue, methodReturnType, this.container, this.request);

		assertThat(this.servletResponse.getContentAsString())
				.doesNotContain("\"withView1\":\"with\"")
				.contains("\"withView2\":\"with\"")
				.doesNotContain("\"withoutView\":\"without\"");
	}

	@Test
	void jacksonJsonViewWithResponseEntityAndJsonMessageConverter() throws Exception {
		Method method = JacksonController.class.getMethod("handleResponseEntity");
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
		MethodParameter methodReturnType = handlerMethod.getReturnType();

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2HttpMessageConverter());
		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(
				converters, null, List.of(new JsonViewResponseBodyAdvice()));

		Object returnValue = new JacksonController().handleResponseEntity();
		processor.handleReturnValue(returnValue, methodReturnType, this.container, this.request);

		assertThat(this.servletResponse.getContentAsString())
				.doesNotContain("\"withView1\":\"with\"")
				.contains("\"withView2\":\"with\"")
				.doesNotContain("\"withoutView\":\"without\"");
	}

	@Test  // SPR-12149
	void jacksonJsonViewWithResponseBodyAndXmlMessageConverter() throws Exception {
		Method method = JacksonController.class.getMethod("handleResponseBody");
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
		MethodParameter methodReturnType = handlerMethod.getReturnType();

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2XmlHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(
				converters, null, List.of(new JsonViewResponseBodyAdvice()));

		Object returnValue = new JacksonController().handleResponseBody();
		processor.handleReturnValue(returnValue, methodReturnType, this.container, this.request);

		assertThat(this.servletResponse.getContentAsString())
				.doesNotContain("<withView1>with</withView1>")
				.contains("<withView2>with</withView2>")
				.doesNotContain("<withoutView>without</withoutView>");
	}

	@Test  // SPR-12149
	void jacksonJsonViewWithResponseEntityAndXmlMessageConverter() throws Exception {
		Method method = JacksonController.class.getMethod("handleResponseEntity");
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
		MethodParameter methodReturnType = handlerMethod.getReturnType();

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2XmlHttpMessageConverter());
		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(
				converters, null, List.of(new JsonViewResponseBodyAdvice()));

		Object returnValue = new JacksonController().handleResponseEntity();
		processor.handleReturnValue(returnValue, methodReturnType, this.container, this.request);

		assertThat(this.servletResponse.getContentAsString())
				.doesNotContain("<withView1>with</withView1>")
				.contains("<withView2>with</withView2>")
				.doesNotContain("<withoutView>without</withoutView>");
	}

	@Test  // SPR-12501
	void resolveArgumentWithJacksonJsonView() throws Exception {
		String content = "{\"withView1\" : \"with\", \"withView2\" : \"with\", \"withoutView\" : \"without\"}";
		this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
		this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

		Method method = JacksonController.class.getMethod("handleRequestBody", JacksonViewBean.class);
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
		MethodParameter methodParameter = handlerMethod.getMethodParameters()[0];

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2HttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(
				converters, null, List.of(new JsonViewRequestBodyAdvice()));

		JacksonViewBean result = (JacksonViewBean)
				processor.resolveArgument(methodParameter, this.container, this.request, this.factory);

		assertThat(result).isNotNull();
		assertThat(result.getWithView1()).isEqualTo("with");
		assertThat(result.getWithView2()).isNull();
		assertThat(result.getWithoutView()).isNull();
	}

	@Test  // SPR-12501
	void resolveHttpEntityArgumentWithJacksonJsonView() throws Exception {
		String content = "{\"withView1\" : \"with\", \"withView2\" : \"with\", \"withoutView\" : \"without\"}";
		this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
		this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

		Method method = JacksonController.class.getMethod("handleHttpEntity", HttpEntity.class);
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
		MethodParameter methodParameter = handlerMethod.getMethodParameters()[0];

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2HttpMessageConverter());
		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(
				converters, null, List.of(new JsonViewRequestBodyAdvice()));

		@SuppressWarnings("unchecked")
		HttpEntity<JacksonViewBean> result = (HttpEntity<JacksonViewBean>)
				processor.resolveArgument( methodParameter, this.container, this.request, this.factory);

		assertThat(result).isNotNull();
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getWithView1()).isEqualTo("with");
		assertThat(result.getBody().getWithView2()).isNull();
		assertThat(result.getBody().getWithoutView()).isNull();
	}

	@Test  // SPR-12501
	void resolveArgumentWithJacksonJsonViewAndXmlMessageConverter() throws Exception {
		String content = "<root>" +
				"<withView1>with</withView1>" +
				"<withView2>with</withView2>" +
				"<withoutView>without</withoutView></root>";
		this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
		this.servletRequest.setContentType(MediaType.APPLICATION_XML_VALUE);

		Method method = JacksonController.class.getMethod("handleRequestBody", JacksonViewBean.class);
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
		MethodParameter methodParameter = handlerMethod.getMethodParameters()[0];

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2XmlHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(
				converters, null, List.of(new JsonViewRequestBodyAdvice()));

		JacksonViewBean result = (JacksonViewBean)
				processor.resolveArgument(methodParameter, this.container, this.request, this.factory);

		assertThat(result).isNotNull();
		assertThat(result.getWithView1()).isEqualTo("with");
		assertThat(result.getWithView2()).isNull();
		assertThat(result.getWithoutView()).isNull();
	}

	@Test  // SPR-12501
	void resolveHttpEntityArgumentWithJacksonJsonViewAndXmlMessageConverter() throws Exception {
		String content = "<root>" +
				"<withView1>with</withView1>" +
				"<withView2>with</withView2>" +
				"<withoutView>without</withoutView></root>";
		this.servletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));
		this.servletRequest.setContentType(MediaType.APPLICATION_XML_VALUE);

		Method method = JacksonController.class.getMethod("handleHttpEntity", HttpEntity.class);
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
		MethodParameter methodParameter = handlerMethod.getMethodParameters()[0];

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2XmlHttpMessageConverter());
		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(
				converters, null, List.of(new JsonViewRequestBodyAdvice()));

		@SuppressWarnings("unchecked")
		HttpEntity<JacksonViewBean> result = (HttpEntity<JacksonViewBean>)
				processor.resolveArgument(methodParameter, this.container, this.request, this.factory);

		assertThat(result).isNotNull();
		assertThat(result.getBody()).isNotNull();
		assertThat(result.getBody().getWithView1()).isEqualTo("with");
		assertThat(result.getBody().getWithView2()).isNull();
		assertThat(result.getBody().getWithoutView()).isNull();
	}

	@Test  // SPR-12811
	void jacksonTypeInfoList() throws Exception {
		Method method = JacksonController.class.getMethod("handleTypeInfoList");
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
		MethodParameter methodReturnType = handlerMethod.getReturnType();

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2HttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		Object returnValue = new JacksonController().handleTypeInfoList();
		processor.handleReturnValue(returnValue, methodReturnType, this.container, this.request);

		assertThat(this.servletResponse.getContentAsString())
				.contains("\"type\":\"foo\"")
				.contains("\"type\":\"bar\"");
	}

	@Test  // SPR-13318
	void jacksonSubType() throws Exception {
		Method method = JacksonController.class.getMethod("handleSubType");
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
		MethodParameter methodReturnType = handlerMethod.getReturnType();

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2HttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		Object returnValue = new JacksonController().handleSubType();
		processor.handleReturnValue(returnValue, methodReturnType, this.container, this.request);

		assertThat(this.servletResponse.getContentAsString())
				.contains("\"id\":123")
				.contains("\"name\":\"foo\"");
	}

	@Test  // SPR-13318
	void jacksonSubTypeList() throws Exception {
		Method method = JacksonController.class.getMethod("handleSubTypeList");
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
		MethodParameter methodReturnType = handlerMethod.getReturnType();

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2HttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		Object returnValue = new JacksonController().handleSubTypeList();
		processor.handleReturnValue(returnValue, methodReturnType, this.container, this.request);

		assertThat(this.servletResponse.getContentAsString())
				.contains("\"id\":123")
				.contains("\"name\":\"foo\"")
				.contains("\"id\":456")
				.contains("\"name\":\"bar\"");
	}

	@Test  // SPR-14520
	void resolveArgumentTypeVariableWithGenericInterface() throws Exception {
		this.servletRequest.setContent("\"foo\"".getBytes(StandardCharsets.UTF_8));
		this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

		Method method = MyControllerImplementingInterface.class.getMethod("handle", Object.class);
		HandlerMethod handlerMethod = new HandlerMethod(new MyControllerImplementingInterface(), method);
		MethodParameter methodParameter = handlerMethod.getMethodParameters()[0];

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2HttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		assertThat(processor.supportsParameter(methodParameter)).isTrue();
		String value = (String) processor.readWithMessageConverters(
				this.request, methodParameter, methodParameter.getGenericParameterType());
		assertThat(value).isEqualTo("foo");
	}

	@Test  // gh-24127
	void resolveArgumentTypeVariableWithGenericInterfaceAndSubclass() throws Exception {
		this.servletRequest.setContent("\"foo\"".getBytes(StandardCharsets.UTF_8));
		this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

		Method method = SubControllerImplementingInterface.class.getMethod("handle", Object.class);
		HandlerMethod handlerMethod = new HandlerMethod(new SubControllerImplementingInterface(), method);
		MethodParameter methodParameter = handlerMethod.getMethodParameters()[0];

		List<HttpMessageConverter<?>> converters = List.of(new MappingJackson2HttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		assertThat(processor.supportsParameter(methodParameter)).isTrue();
		String value = (String) processor.readWithMessageConverters(
				this.request, methodParameter, methodParameter.getGenericParameterType());
		assertThat(value).isEqualTo("foo");
	}

	private void assertContentDisposition(RequestResponseBodyMethodProcessor processor,
			boolean expectContentDisposition, String requestURI, String comment) throws Exception {

		this.servletRequest.setRequestURI(requestURI);
		processor.handleReturnValue("body", this.returnTypeString, this.container, this.request);

		String header = servletResponse.getHeader("Content-Disposition");
		if (expectContentDisposition) {
			assertThat(header)
					.as("Expected 'Content-Disposition' header. Use case: '" + comment + "'")
					.isEqualTo("inline;filename=f.txt");
		}
		else {
			assertThat(header)
					.as("Did not expect 'Content-Disposition' header. Use case: '" + comment + "'")
					.isNull();
		}

		this.servletRequest.clearAttributes();
		this.servletResponse.setCommitted(false);
		this.servletResponse.reset();
	}


	@SuppressWarnings("ConstantConditions")
	String handle(
			@RequestBody List<SimpleBean> list,
			@RequestBody SimpleBean simpleBean,
			@RequestBody MultiValueMap<String, String> multiValueMap,
			@RequestBody String string) {

		return null;
	}

	@SuppressWarnings("ConstantConditions")
	Resource getImage() {
		return null;
	}

	@SuppressWarnings("ConstantConditions")
	MyProblemDetail handleAndReturnProblemDetail() {
		return null;
	}

	@SuppressWarnings("ConstantConditions")
	@RequestMapping
	OutputStream handleAndReturnOutputStream() {
		return null;
	}

	@SuppressWarnings("ConstantConditions")
	SimpleBean getSimpleBean() {
		return null;
	}


	private static class MyProblemDetail extends ProblemDetail {

		public MyProblemDetail(HttpStatus status) {
			super(status.value());
		}

	}


	private static abstract class MyParameterizedController<DTO extends Identifiable> {

		@SuppressWarnings("unused")
		public void handleDto(@RequestBody DTO dto) {}
	}


	private static class MySimpleParameterizedController extends MyParameterizedController<SimpleBean> {
	}


	private interface Identifiable extends Serializable {

		Long getId();

		void setId(Long id);
	}


	@SuppressWarnings("unused")
	private static abstract class MyParameterizedControllerWithList<DTO extends Identifiable> {

		public void handleDto(@RequestBody List<DTO> dto) {
		}
	}


	@SuppressWarnings("unused")
	private static class MySimpleParameterizedControllerWithList extends MyParameterizedControllerWithList<SimpleBean> {
	}


	@SuppressWarnings({"serial", "NotNullFieldNotInitialized"})
	private static class SimpleBean implements Identifiable {

		private Long id;

		private String name;

		@Override
		public Long getId() {
			return id;
		}

		@Override
		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


	private static final class ValidatingBinderFactory implements WebDataBinderFactory {

		@Override
		public WebDataBinder createBinder(NativeWebRequest request, @Nullable Object target, String objectName) {
			LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
			validator.afterPropertiesSet();
			WebDataBinder dataBinder = new WebDataBinder(target, objectName);
			dataBinder.setValidator(validator);
			return dataBinder;
		}
	}


	@ResponseBody
	private static class ResponseBodyController {

		@RequestMapping
		public String handle() {
			return "hello";
		}

		@SuppressWarnings("ConstantConditions")
		@RequestMapping
		public CharSequence handleWithCharSequence() {
			return null;
		}
	}


	@RestController
	private static class TestRestController {

		@RequestMapping
		public String handle() {
			return "hello";
		}
	}


	private interface MyJacksonView1 {}

	private interface MyJacksonView2 {}


	@SuppressWarnings("NotNullFieldNotInitialized")
	private static class JacksonViewBean {

		@JsonView(MyJacksonView1.class)
		private String withView1;

		@JsonView(MyJacksonView2.class)
		private String withView2;

		private String withoutView;

		public String getWithView1() {
			return withView1;
		}

		public void setWithView1(String withView1) {
			this.withView1 = withView1;
		}

		@Nullable
		public String getWithView2() {
			return withView2;
		}

		public void setWithView2(String withView2) {
			this.withView2 = withView2;
		}

		@Nullable
		public String getWithoutView() {
			return withoutView;
		}

		public void setWithoutView(String withoutView) {
			this.withoutView = withoutView;
		}
	}


	@SuppressWarnings("NotNullFieldNotInitialized")
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
	public static class ParentClass {

		private String parentProperty;

		public ParentClass() {
		}

		public ParentClass(String parentProperty) {
			this.parentProperty = parentProperty;
		}

		public String getParentProperty() {
			return parentProperty;
		}

		public void setParentProperty(String parentProperty) {
			this.parentProperty = parentProperty;
		}
	}


	@JsonTypeName("foo")
	public static class Foo extends ParentClass {

		public Foo() {
		}

		public Foo(String parentProperty) {
			super(parentProperty);
		}
	}


	@JsonTypeName("bar")
	public static class Bar extends ParentClass {

		public Bar() {
		}

		public Bar(String parentProperty) {
			super(parentProperty);
		}
	}


	private static class BaseController<T> {

		@RequestMapping
		@ResponseBody
		@SuppressWarnings("unchecked")
		public List<T> handleTypeInfoList() {
			return List.of((T) new Foo("foo"), (T) new Bar("bar"));
		}
	}


	private static class JacksonController extends BaseController<ParentClass> {

		@RequestMapping
		@ResponseBody
		@JsonView(MyJacksonView2.class)
		public JacksonViewBean handleResponseBody() {
			JacksonViewBean bean = new JacksonViewBean();
			bean.setWithView1("with");
			bean.setWithView2("with");
			bean.setWithoutView("without");
			return bean;
		}

		@RequestMapping
		@JsonView(MyJacksonView2.class)
		public ResponseEntity<JacksonViewBean> handleResponseEntity() {
			JacksonViewBean bean = new JacksonViewBean();
			bean.setWithView1("with");
			bean.setWithView2("with");
			bean.setWithoutView("without");
			ModelAndView mav = new ModelAndView(new MappingJackson2JsonView());
			mav.addObject("bean", bean);
			return new ResponseEntity<>(bean, HttpStatus.OK);
		}

		@RequestMapping
		@ResponseBody
		public JacksonViewBean handleRequestBody(@JsonView(MyJacksonView1.class) @RequestBody JacksonViewBean bean) {
			return bean;
		}

		@SuppressWarnings("ConstantConditions")
		@RequestMapping
		@ResponseBody
		public JacksonViewBean handleHttpEntity(@JsonView(MyJacksonView1.class) HttpEntity<JacksonViewBean> entity) {
			return entity.getBody();
		}

		@RequestMapping
		@ResponseBody
		public Identifiable handleSubType() {
			SimpleBean foo = new SimpleBean();
			foo.setId(123L);
			foo.setName("foo");
			return foo;
		}

		@RequestMapping
		@ResponseBody
		public List<Identifiable> handleSubTypeList() {
			SimpleBean foo = new SimpleBean();
			foo.setId(123L);
			foo.setName("foo");
			SimpleBean bar = new SimpleBean();
			bar.setId(456L);
			bar.setName("bar");
			return Arrays.asList(foo, bar);
		}

		@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
		@ResponseBody
		public String defaultCharset() {
			return "foo";
		}
	}


	private static class EmptyRequestBodyAdvice implements RequestBodyAdvice {

		@Override
		public boolean supports(MethodParameter methodParameter, Type targetType,
				Class<? extends HttpMessageConverter<?>> converterType) {

			return StringHttpMessageConverter.class.equals(converterType);
		}

		@Override
		public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
				Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

			return inputMessage;
		}

		@Override
		public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
				Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

			return body;
		}

		@Override
		public Object handleEmptyBody(@Nullable Object body, HttpInputMessage inputMessage, MethodParameter parameter,
				Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

			return "default value for empty body";
		}
	}


	interface MappingInterface<A> {

		default A handle(@RequestBody A arg) {
			return arg;
		}
	}


	static class MyControllerImplementingInterface implements MappingInterface<String> {
	}


	static class SubControllerImplementingInterface extends MyControllerImplementingInterface {

		@Override
		public String handle(String arg) {
			return arg;
		}
	}

}
