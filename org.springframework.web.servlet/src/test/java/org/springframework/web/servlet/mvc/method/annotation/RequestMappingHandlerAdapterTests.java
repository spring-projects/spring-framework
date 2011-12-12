/*
 * Copyright 2002-2011 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ModelMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.ModelAndView;

/**
 * Unit tests for {@link RequestMappingHandlerAdapter}.
 *
 * @author Rossen Stoyanchev
 *
 * @see ServletAnnotationControllerHandlerMethodTests
 * @see HandlerMethodAnnotationDetectionTests
 * @see RequestMappingHandlerAdapterIntegrationTests
 */
public class RequestMappingHandlerAdapterTests {

	private static int RESOLVER_COUNT;
	
	private static int INIT_BINDER_RESOLVER_COUNT;
	
	private static int HANDLER_COUNT;

	private RequestMappingHandlerAdapter handlerAdapter;
	
	private MockHttpServletRequest request;
	
	private MockHttpServletResponse response;

	@BeforeClass
	public static void setupOnce() {
		RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
		adapter.afterPropertiesSet();
		
		RESOLVER_COUNT = adapter.getArgumentResolvers().getResolvers().size();
		INIT_BINDER_RESOLVER_COUNT = adapter.getInitBinderArgumentResolvers().getResolvers().size();
		HANDLER_COUNT = adapter.getReturnValueHandlers().getHandlers().size();
	}
	
	@Before
	public void setup() throws Exception {
		this.handlerAdapter = new RequestMappingHandlerAdapter();
		this.handlerAdapter.setApplicationContext(new GenericWebApplicationContext());
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
	}

	@Test
	public void cacheControlWithoutSessionAttributes() throws Exception {
		HandlerMethod handlerMethod = handlerMethod(new SimpleController(), "handle");
		this.handlerAdapter.afterPropertiesSet();
		this.handlerAdapter.setCacheSeconds(100);
		this.handlerAdapter.handle(this.request, this.response, handlerMethod);

		assertTrue(response.getHeader("Cache-Control").toString().contains("max-age"));
	}

	@Test
	public void cacheControlWithSessionAttributes() throws Exception {
		SessionAttributeController handler = new SessionAttributeController();
		this.handlerAdapter.afterPropertiesSet();
		this.handlerAdapter.setCacheSeconds(100);
		this.handlerAdapter.handle(this.request, this.response, handlerMethod(handler, "handle"));

		assertEquals("no-cache", this.response.getHeader("Cache-Control"));
	}

	@Test
	public void setAlwaysUseRedirectAttributes() throws Exception {
		HandlerMethodArgumentResolver redirectAttributesResolver = new RedirectAttributesMethodArgumentResolver();
		HandlerMethodArgumentResolver modelResolver = new ModelMethodProcessor();
		HandlerMethodReturnValueHandler viewHandler = new ViewNameMethodReturnValueHandler();
		
		this.handlerAdapter.setArgumentResolvers(Arrays.asList(redirectAttributesResolver, modelResolver));
		this.handlerAdapter.setReturnValueHandlers(Arrays.asList(viewHandler));
		this.handlerAdapter.setIgnoreDefaultModelOnRedirect(true);
		this.handlerAdapter.afterPropertiesSet();

		this.request.setAttribute(FlashMapManager.OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());

		HandlerMethod handlerMethod = handlerMethod(new RedirectAttributeController(), "handle", Model.class);
		ModelAndView mav = this.handlerAdapter.handle(request, response, handlerMethod);

		assertTrue("Without RedirectAttributes arg, model should be empty", mav.getModel().isEmpty());
	}

	@Test
	public void setCustomArgumentResolvers() throws Exception {
		HandlerMethodArgumentResolver resolver = new ServletRequestMethodArgumentResolver();
		this.handlerAdapter.setCustomArgumentResolvers(Arrays.asList(resolver));
		this.handlerAdapter.afterPropertiesSet();
		
		assertTrue(this.handlerAdapter.getArgumentResolvers().getResolvers().contains(resolver));
		assertMethodProcessorCount(RESOLVER_COUNT + 1, INIT_BINDER_RESOLVER_COUNT + 1, HANDLER_COUNT);
	}

	@Test
	public void setArgumentResolvers() throws Exception {
		HandlerMethodArgumentResolver resolver = new ServletRequestMethodArgumentResolver();
		this.handlerAdapter.setArgumentResolvers(Arrays.asList(resolver));
		this.handlerAdapter.afterPropertiesSet();

		assertMethodProcessorCount(1, INIT_BINDER_RESOLVER_COUNT, HANDLER_COUNT);
	}

	@Test
	public void setInitBinderArgumentResolvers() throws Exception {
		HandlerMethodArgumentResolver resolver = new ServletRequestMethodArgumentResolver();
		this.handlerAdapter.setInitBinderArgumentResolvers(Arrays.<HandlerMethodArgumentResolver>asList(resolver));
		this.handlerAdapter.afterPropertiesSet();
		
		assertMethodProcessorCount(RESOLVER_COUNT, 1, HANDLER_COUNT);
	}

	@Test
	public void setCustomReturnValueHandlers() {
		HandlerMethodReturnValueHandler handler = new ViewNameMethodReturnValueHandler();
		this.handlerAdapter.setCustomReturnValueHandlers(Arrays.asList(handler));
		this.handlerAdapter.afterPropertiesSet();

		assertTrue(this.handlerAdapter.getReturnValueHandlers().getHandlers().contains(handler));
		assertMethodProcessorCount(RESOLVER_COUNT, INIT_BINDER_RESOLVER_COUNT, HANDLER_COUNT + 1);
	}
	
	@Test
	public void setReturnValueHandlers() {
		HandlerMethodReturnValueHandler handler = new ModelMethodProcessor();
		this.handlerAdapter.setReturnValueHandlers(Arrays.asList(handler));
		this.handlerAdapter.afterPropertiesSet();

		assertMethodProcessorCount(RESOLVER_COUNT, INIT_BINDER_RESOLVER_COUNT, 1);
	}

	private HandlerMethod handlerMethod(Object handler, String methodName, Class<?>... paramTypes) throws Exception {
		Method method = handler.getClass().getDeclaredMethod(methodName, paramTypes);
		return new InvocableHandlerMethod(handler, method);
	}

	

	private void assertMethodProcessorCount(int resolverCount, int initBinderResolverCount, int handlerCount) {
		assertEquals(resolverCount, this.handlerAdapter.getArgumentResolvers().getResolvers().size());
		assertEquals(initBinderResolverCount, this.handlerAdapter.getInitBinderArgumentResolvers().getResolvers().size());
		assertEquals(handlerCount, this.handlerAdapter.getReturnValueHandlers().getHandlers().size());
	}

	@SuppressWarnings("unused")
	private static class SimpleController {

		public String handle() {
			return null;
		}
	}

	@SessionAttributes("attr1")
	private static class SessionAttributeController {
		
		@SuppressWarnings("unused")
		public void handle() {
		}
	}

	@SuppressWarnings("unused")
	private static class RedirectAttributeController {
		
		public String handle(Model model) {
			model.addAttribute("someAttr", "someAttrValue");
			return "redirect:/path";
		}
	}
	
}