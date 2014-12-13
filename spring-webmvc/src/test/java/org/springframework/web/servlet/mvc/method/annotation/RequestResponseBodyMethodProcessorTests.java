/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
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

import static org.junit.Assert.*;

/**
 * Test fixture for a {@link RequestResponseBodyMethodProcessor} with actual delegation
 * to HttpMessageConverter instances.
 *
 * <p>Also see {@link RequestResponseBodyMethodProcessorMockTests}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class RequestResponseBodyMethodProcessorTests {

	private MethodParameter paramGenericList;
	private MethodParameter paramSimpleBean;
	private MethodParameter paramMultiValueMap;
	private MethodParameter paramString;
	private MethodParameter returnTypeString;

	private ModelAndViewContainer mavContainer;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private ValidatingBinderFactory binderFactory;


	@Before
	public void setUp() throws Exception {
		Method method = getClass().getMethod("handle", List.class, SimpleBean.class, MultiValueMap.class, String.class);

		paramGenericList = new MethodParameter(method, 0);
		paramSimpleBean = new MethodParameter(method, 1);
		paramMultiValueMap = new MethodParameter(method, 2);
		paramString = new MethodParameter(method, 3);
		returnTypeString = new MethodParameter(method, -1);

		mavContainer = new ModelAndViewContainer();

		servletRequest = new MockHttpServletRequest();
		servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(servletRequest, servletResponse);

		this.binderFactory = new ValidatingBinderFactory();
	}

	@Test
	public void resolveArgumentParameterizedType() throws Exception {
		String content = "[{\"name\" : \"Jad\"}, {\"name\" : \"Robert\"}]";
		this.servletRequest.setContent(content.getBytes("UTF-8"));
		this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new MappingJackson2HttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		@SuppressWarnings("unchecked")
		List<SimpleBean> result = (List<SimpleBean>) processor.resolveArgument(
				paramGenericList, mavContainer, webRequest, binderFactory);

		assertNotNull(result);
		assertEquals("Jad", result.get(0).getName());
		assertEquals("Robert", result.get(1).getName());
	}

	@Test
	public void resolveArgumentRawTypeFromParameterizedType() throws Exception {
		String content = "fruit=apple&vegetable=kale";
		this.servletRequest.setContent(content.getBytes("UTF-8"));
		this.servletRequest.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new AllEncompassingFormHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		@SuppressWarnings("unchecked")
		MultiValueMap<String, String> result = (MultiValueMap<String, String>) processor.resolveArgument(
				paramMultiValueMap, mavContainer, webRequest, binderFactory);

		assertNotNull(result);
		assertEquals("apple", result.getFirst("fruit"));
		assertEquals("kale", result.getFirst("vegetable"));
	}

	@Test
	public void resolveArgumentClassJson() throws Exception {
		String content = "{\"name\" : \"Jad\"}";
		this.servletRequest.setContent(content.getBytes("UTF-8"));
		this.servletRequest.setContentType("application/json");

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new MappingJackson2HttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		SimpleBean result = (SimpleBean) processor.resolveArgument(
				paramSimpleBean, mavContainer, webRequest, binderFactory);

		assertNotNull(result);
		assertEquals("Jad", result.getName());
	}

	@Test
	public void resolveArgumentClassString() throws Exception {
		String content = "foobarbaz";
		this.servletRequest.setContent(content.getBytes("UTF-8"));
		this.servletRequest.setContentType("application/json");

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new StringHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		String result = (String) processor.resolveArgument(
				paramString, mavContainer, webRequest, binderFactory);

		assertNotNull(result);
		assertEquals("foobarbaz", result);
	}

	// SPR-9942

	@Test(expected = HttpMessageNotReadableException.class)
	public void resolveArgumentRequiredNoContent() throws Exception {
		this.servletRequest.setContent(new byte[0]);
		this.servletRequest.setContentType("text/plain");
		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new StringHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);
		processor.resolveArgument(paramString, mavContainer, webRequest, binderFactory);
	}

	// SPR-9964

	@Test
	public void resolveArgumentTypeVariable() throws Exception {
		Method method = MyParameterizedController.class.getMethod("handleDto", Identifiable.class);
		HandlerMethod handlerMethod = new HandlerMethod(new MySimpleParameterizedController(), method);
		MethodParameter methodParam = handlerMethod.getMethodParameters()[0];

		String content = "{\"name\" : \"Jad\"}";
		this.servletRequest.setContent(content.getBytes("UTF-8"));
		this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new MappingJackson2HttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		SimpleBean result = (SimpleBean) processor.resolveArgument(methodParam, mavContainer, webRequest, binderFactory);

		assertNotNull(result);
		assertEquals("Jad", result.getName());
	}

	// SPR-11225

	@Test
	public void resolveArgumentTypeVariableWithNonGenericConverter() throws Exception {
		Method method = MyParameterizedController.class.getMethod("handleDto", Identifiable.class);
		HandlerMethod handlerMethod = new HandlerMethod(new MySimpleParameterizedController(), method);
		MethodParameter methodParam = handlerMethod.getMethodParameters()[0];

		String content = "{\"name\" : \"Jad\"}";
		this.servletRequest.setContent(content.getBytes("UTF-8"));
		this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		HttpMessageConverter target = new MappingJackson2HttpMessageConverter();
		HttpMessageConverter proxy = ProxyFactory.getProxy(HttpMessageConverter.class, new SingletonTargetSource(target));
		converters.add(proxy);
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		SimpleBean result = (SimpleBean) processor.resolveArgument(methodParam, mavContainer, webRequest, binderFactory);

		assertNotNull(result);
		assertEquals("Jad", result.getName());
	}

	// SPR-9160

	@Test
	public void handleReturnValueSortByQuality() throws Exception {
		this.servletRequest.addHeader("Accept", "text/plain; q=0.5, application/json");

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new MappingJackson2HttpMessageConverter());
		converters.add(new StringHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		processor.writeWithMessageConverters("Foo", returnTypeString, webRequest);

		assertEquals("application/json;charset=UTF-8", servletResponse.getHeader("Content-Type"));
	}

	@Test
	public void handleReturnValueString() throws Exception {
		List<HttpMessageConverter<?>>converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new ByteArrayHttpMessageConverter());
		converters.add(new StringHttpMessageConverter());

		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);
		processor.handleReturnValue("Foo", returnTypeString, mavContainer, webRequest);

		assertEquals("text/plain;charset=ISO-8859-1", servletResponse.getHeader("Content-Type"));
		assertEquals("Foo", servletResponse.getContentAsString());
	}

	@Test
	public void handleReturnValueStringAcceptCharset() throws Exception {
		this.servletRequest.addHeader("Accept", "text/plain;charset=UTF-8");

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new ByteArrayHttpMessageConverter());
		converters.add(new StringHttpMessageConverter());
		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		processor.writeWithMessageConverters("Foo", returnTypeString, webRequest);

		assertEquals("text/plain;charset=UTF-8", servletResponse.getHeader("Content-Type"));
	}

	@Test
	public void supportsReturnTypeResponseBodyOnType() throws Exception {
		Method method = ResponseBodyController.class.getMethod("handle");
		MethodParameter returnType = new MethodParameter(method, -1);

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new StringHttpMessageConverter());

		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		assertTrue("Failed to recognize type-level @ResponseBody", processor.supportsReturnType(returnType));
	}

	@Test
	public void supportsReturnTypeRestController() throws Exception {
		Method method = TestRestController.class.getMethod("handle");
		MethodParameter returnType = new MethodParameter(method, -1);

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new StringHttpMessageConverter());

		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(converters);

		assertTrue("Failed to recognize type-level @RestController", processor.supportsReturnType(returnType));
	}

	@Test
	public void jacksonJsonViewWithResponseBodyAndJsonMessageConverter() throws Exception {
		Method method = JacksonViewController.class.getMethod("handleResponseBody");
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonViewController(), method);
		MethodParameter methodReturnType = handlerMethod.getReturnType();

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new MappingJackson2HttpMessageConverter());

		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(
				converters, null, Arrays.asList(new JsonViewResponseBodyAdvice()));

		Object returnValue = new JacksonViewController().handleResponseBody();
		processor.handleReturnValue(returnValue, methodReturnType, this.mavContainer, this.webRequest);

		String content = this.servletResponse.getContentAsString();
		assertFalse(content.contains("\"withView1\":\"with\""));
		assertTrue(content.contains("\"withView2\":\"with\""));
		assertFalse(content.contains("\"withoutView\":\"without\""));
	}

	@Test
	public void jacksonJsonViewWithResponseEntityAndJsonMessageConverter() throws Exception {
		Method method = JacksonViewController.class.getMethod("handleResponseEntity");
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonViewController(), method);
		MethodParameter methodReturnType = handlerMethod.getReturnType();

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new MappingJackson2HttpMessageConverter());

		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(
				converters, null, Arrays.asList(new JsonViewResponseBodyAdvice()));

		Object returnValue = new JacksonViewController().handleResponseEntity();
		processor.handleReturnValue(returnValue, methodReturnType, this.mavContainer, this.webRequest);

		String content = this.servletResponse.getContentAsString();
		assertFalse(content.contains("\"withView1\":\"with\""));
		assertTrue(content.contains("\"withView2\":\"with\""));
		assertFalse(content.contains("\"withoutView\":\"without\""));
	}

	// SPR-12149

	@Test
	public void jacksonJsonViewWithResponseBodyAndXmlMessageConverter() throws Exception {
		Method method = JacksonViewController.class.getMethod("handleResponseBody");
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonViewController(), method);
		MethodParameter methodReturnType = handlerMethod.getReturnType();

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new MappingJackson2XmlHttpMessageConverter());

		RequestResponseBodyMethodProcessor processor = new RequestResponseBodyMethodProcessor(
				converters, null, Arrays.asList(new JsonViewResponseBodyAdvice()));

		Object returnValue = new JacksonViewController().handleResponseBody();
		processor.handleReturnValue(returnValue, methodReturnType, this.mavContainer, this.webRequest);

		String content = this.servletResponse.getContentAsString();
		assertFalse(content.contains("<withView1>with</withView1>"));
		assertTrue(content.contains("<withView2>with</withView2>"));
		assertFalse(content.contains("<withoutView>without</withoutView>"));
	}

	// SPR-12149

	@Test
	public void jacksonJsonViewWithResponseEntityAndXmlMessageConverter() throws Exception {
		Method method = JacksonViewController.class.getMethod("handleResponseEntity");
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonViewController(), method);
		MethodParameter methodReturnType = handlerMethod.getReturnType();

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new MappingJackson2XmlHttpMessageConverter());

		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(
				converters, null, Arrays.asList(new JsonViewResponseBodyAdvice()));

		Object returnValue = new JacksonViewController().handleResponseEntity();
		processor.handleReturnValue(returnValue, methodReturnType, this.mavContainer, this.webRequest);

		String content = this.servletResponse.getContentAsString();
		assertFalse(content.contains("<withView1>with</withView1>"));
		assertTrue(content.contains("<withView2>with</withView2>"));
		assertFalse(content.contains("<withoutView>without</withoutView>"));
	}


	public String handle(
			@RequestBody List<SimpleBean> list,
			@RequestBody SimpleBean simpleBean,
			@RequestBody MultiValueMap<String, String> multiValueMap,
			@RequestBody String string) {

		return null;
	}


	private static abstract class MyParameterizedController<DTO extends Identifiable> {

		@SuppressWarnings("unused")
		public void handleDto(@RequestBody DTO dto) {}
	}


	private static class MySimpleParameterizedController extends MyParameterizedController<SimpleBean> {
	}


	private interface Identifiable extends Serializable {

		public Long getId();

		public void setId(Long id);
	}


	@SuppressWarnings({ "serial" })
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

		@SuppressWarnings("unused")
		public void setName(String name) {
			this.name = name;
		}
	}


	private final class ValidatingBinderFactory implements WebDataBinderFactory {

		@Override
		public WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName) throws Exception {
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
	}


	@RestController
	private static class TestRestController {

		@RequestMapping
		public String handle() {
			return "hello";
		}
	}

	private interface MyJacksonView1 {};
	private interface MyJacksonView2 {};

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

		public String getWithView2() {
			return withView2;
		}

		public void setWithView2(String withView2) {
			this.withView2 = withView2;
		}

		public String getWithoutView() {
			return withoutView;
		}

		public void setWithoutView(String withoutView) {
			this.withoutView = withoutView;
		}
	}

	private static class JacksonViewController {

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
			return new ResponseEntity<JacksonViewBean>(bean, HttpStatus.OK);
		}

	}

}
