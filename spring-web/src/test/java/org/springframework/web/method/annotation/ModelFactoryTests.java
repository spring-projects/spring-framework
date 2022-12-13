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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.DefaultSessionAttributeStore;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Text fixture for {@link ModelFactory} tests.
 *
 * @author Rossen Stoyanchev
 */
public class ModelFactoryTests {

	private NativeWebRequest webRequest;

	private SessionAttributesHandler attributeHandler;

	private SessionAttributeStore attributeStore;

	private TestController controller = new TestController();

	private ModelAndViewContainer mavContainer;


	@BeforeEach
	public void setUp() throws Exception {
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());
		this.attributeStore = new DefaultSessionAttributeStore();
		this.attributeHandler = new SessionAttributesHandler(TestController.class, this.attributeStore);
		this.controller = new TestController();
		this.mavContainer = new ModelAndViewContainer();
	}


	@Test
	public void modelAttributeMethod() throws Exception {
		ModelFactory modelFactory = createModelFactory("modelAttr", Model.class);
		HandlerMethod handlerMethod = createHandlerMethod("handle");
		modelFactory.initModel(this.webRequest, this.mavContainer, handlerMethod);

		assertThat(this.mavContainer.getModel().get("modelAttr")).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void modelAttributeMethodWithExplicitName() throws Exception {
		ModelFactory modelFactory = createModelFactory("modelAttrWithName");
		HandlerMethod handlerMethod = createHandlerMethod("handle");
		modelFactory.initModel(this.webRequest, this.mavContainer, handlerMethod);

		assertThat(this.mavContainer.getModel().get("name")).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void modelAttributeMethodWithNameByConvention() throws Exception {
		ModelFactory modelFactory = createModelFactory("modelAttrConvention");
		HandlerMethod handlerMethod = createHandlerMethod("handle");
		modelFactory.initModel(this.webRequest, this.mavContainer, handlerMethod);

		assertThat(this.mavContainer.getModel().get("boolean")).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void modelAttributeMethodWithNullReturnValue() throws Exception {
		ModelFactory modelFactory = createModelFactory("nullModelAttr");
		HandlerMethod handlerMethod = createHandlerMethod("handle");
		modelFactory.initModel(this.webRequest, this.mavContainer, handlerMethod);

		assertThat(this.mavContainer.containsAttribute("name")).isTrue();
		assertThat(this.mavContainer.getModel().get("name")).isNull();
	}

	@Test
	public void modelAttributeWithBindingDisabled() throws Exception {
		ModelFactory modelFactory = createModelFactory("modelAttrWithBindingDisabled");
		HandlerMethod handlerMethod = createHandlerMethod("handle");
		modelFactory.initModel(this.webRequest, this.mavContainer, handlerMethod);

		assertThat(this.mavContainer.containsAttribute("foo")).isTrue();
		assertThat(this.mavContainer.isBindingDisabled("foo")).isTrue();
	}

	@Test
	public void modelAttributeFromSessionWithBindingDisabled() throws Exception {
		Foo foo = new Foo();
		this.attributeStore.storeAttribute(this.webRequest, "foo", foo);

		ModelFactory modelFactory = createModelFactory("modelAttrWithBindingDisabled");
		HandlerMethod handlerMethod = createHandlerMethod("handle");
		modelFactory.initModel(this.webRequest, this.mavContainer, handlerMethod);

		assertThat(this.mavContainer.containsAttribute("foo")).isTrue();
		assertThat(this.mavContainer.getModel().get("foo")).isSameAs(foo);
		assertThat(this.mavContainer.isBindingDisabled("foo")).isTrue();
	}

	@Test
	public void sessionAttribute() throws Exception {
		this.attributeStore.storeAttribute(this.webRequest, "sessionAttr", "sessionAttrValue");

		ModelFactory modelFactory = createModelFactory("modelAttr", Model.class);
		HandlerMethod handlerMethod = createHandlerMethod("handle");
		modelFactory.initModel(this.webRequest, this.mavContainer, handlerMethod);

		assertThat(this.mavContainer.getModel().get("sessionAttr")).isEqualTo("sessionAttrValue");
	}

	@Test
	public void sessionAttributeNotPresent() throws Exception {
		ModelFactory modelFactory = new ModelFactory(null, null, this.attributeHandler);
		HandlerMethod handlerMethod = createHandlerMethod("handleSessionAttr", String.class);
		assertThatExceptionOfType(HttpSessionRequiredException.class).isThrownBy(() ->
				modelFactory.initModel(this.webRequest, this.mavContainer, handlerMethod));

		// Now add attribute and try again
		this.attributeStore.storeAttribute(this.webRequest, "sessionAttr", "sessionAttrValue");

		modelFactory.initModel(this.webRequest, this.mavContainer, handlerMethod);
		assertThat(this.mavContainer.getModel().get("sessionAttr")).isEqualTo("sessionAttrValue");
	}

	@Test
	public void updateModelBindingResult() throws Exception {
		String commandName = "attr1";
		Object command = new Object();
		ModelAndViewContainer container = new ModelAndViewContainer();
		container.addAttribute(commandName, command);

		WebDataBinder dataBinder = new WebDataBinder(command, commandName);
		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(this.webRequest, command, commandName)).willReturn(dataBinder);

		ModelFactory modelFactory = new ModelFactory(null, binderFactory, this.attributeHandler);
		modelFactory.updateModel(this.webRequest, container);

		assertThat(container.getModel().get(commandName)).isEqualTo(command);
		String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + commandName;
		assertThat(container.getModel().get(bindingResultKey)).isSameAs(dataBinder.getBindingResult());
		assertThat(container.getModel()).hasSize(2);
	}

	@Test
	public void updateModelSessionAttributesSaved() throws Exception {
		String attributeName = "sessionAttr";
		String attribute = "value";
		ModelAndViewContainer container = new ModelAndViewContainer();
		container.addAttribute(attributeName, attribute);

		WebDataBinder dataBinder = new WebDataBinder(attribute, attributeName);
		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(this.webRequest, attribute, attributeName)).willReturn(dataBinder);

		ModelFactory modelFactory = new ModelFactory(null, binderFactory, this.attributeHandler);
		modelFactory.updateModel(this.webRequest, container);

		assertThat(container.getModel().get(attributeName)).isEqualTo(attribute);
		assertThat(this.attributeStore.retrieveAttribute(this.webRequest, attributeName)).isEqualTo(attribute);
	}

	@Test
	public void updateModelSessionAttributesRemoved() throws Exception {
		String attributeName = "sessionAttr";
		String attribute = "value";
		ModelAndViewContainer container = new ModelAndViewContainer();
		container.addAttribute(attributeName, attribute);

		this.attributeStore.storeAttribute(this.webRequest, attributeName, attribute);

		WebDataBinder dataBinder = new WebDataBinder(attribute, attributeName);
		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(this.webRequest, attribute, attributeName)).willReturn(dataBinder);

		container.getSessionStatus().setComplete();

		ModelFactory modelFactory = new ModelFactory(null, binderFactory, this.attributeHandler);
		modelFactory.updateModel(this.webRequest, container);

		assertThat(container.getModel().get(attributeName)).isEqualTo(attribute);
		assertThat(this.attributeStore.retrieveAttribute(this.webRequest, attributeName)).isNull();
	}

	@Test  // SPR-12542
	public void updateModelWhenRedirecting() throws Exception {
		String attributeName = "sessionAttr";
		String attribute = "value";
		ModelAndViewContainer container = new ModelAndViewContainer();
		container.addAttribute(attributeName, attribute);

		String queryParam = "123";
		String queryParamName = "q";
		container.setRedirectModel(new ModelMap(queryParamName, queryParam));
		container.setRedirectModelScenario(true);

		WebDataBinder dataBinder = new WebDataBinder(attribute, attributeName);
		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(this.webRequest, attribute, attributeName)).willReturn(dataBinder);

		ModelFactory modelFactory = new ModelFactory(null, binderFactory, this.attributeHandler);
		modelFactory.updateModel(this.webRequest, container);

		assertThat(container.getModel().get(queryParamName)).isEqualTo(queryParam);
		assertThat(container.getModel()).hasSize(1);
		assertThat(this.attributeStore.retrieveAttribute(this.webRequest, attributeName)).isEqualTo(attribute);
	}


	private ModelFactory createModelFactory(String methodName, Class<?>... parameterTypes) throws Exception {
		HandlerMethodArgumentResolverComposite resolvers = new HandlerMethodArgumentResolverComposite();
		resolvers.addResolver(new ModelMethodProcessor());

		InvocableHandlerMethod modelMethod = createHandlerMethod(methodName, parameterTypes);
		modelMethod.setHandlerMethodArgumentResolvers(resolvers);
		modelMethod.setDataBinderFactory(null);
		modelMethod.setParameterNameDiscoverer(new DefaultParameterNameDiscoverer());

		return new ModelFactory(Collections.singletonList(modelMethod), null, this.attributeHandler);
	}

	private InvocableHandlerMethod createHandlerMethod(String methodName, Class<?>... paramTypes) throws Exception {
		Method method = this.controller.getClass().getMethod(methodName, paramTypes);
		return new InvocableHandlerMethod(this.controller, method);
	}


	@SessionAttributes({"sessionAttr", "foo"})
	static class TestController {

		@ModelAttribute
		public void modelAttr(Model model) {
			model.addAttribute("modelAttr", Boolean.TRUE);
		}

		@ModelAttribute("name")
		public Boolean modelAttrWithName() {
			return Boolean.TRUE;
		}

		@ModelAttribute
		public Boolean modelAttrConvention() {
			return Boolean.TRUE;
		}

		@ModelAttribute("name")
		public Boolean nullModelAttr() {
			return null;
		}

		@ModelAttribute(name="foo", binding=false)
		public Foo modelAttrWithBindingDisabled() {
			return new Foo();
		}

		public void handle() {
		}

		public void handleSessionAttr(@ModelAttribute("sessionAttr") String sessionAttr) {
		}
	}


	private static class Foo {
	}

}
