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
import static org.junit.Assert.assertNull;

import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.FlashAttributes;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.support.FlashStatus;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.annotation.FlashAttributesHandler;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * Test controllers with @{@link FlashAttributes} through the DispatcherServlet.
 * 
 * @author Rossen Stoyanchev
 */
public class FlashAttributesServletTests extends AbstractServletHandlerMethodTests {

	private static final String MESSAGE_KEY = "message";

	@Test
	public void successMessage() throws Exception {

		initServletWithModelExposingViewResolver(MessageController.class);

		MockHttpServletRequest req = new MockHttpServletRequest("GET", "/message");
		MockHttpServletResponse res = new MockHttpServletResponse();
		getServlet().service(req, res);
		
		assertEquals(200, res.getStatus());
		assertNull(getModelAttribute(req, MESSAGE_KEY));
		assertNull(getFlashAttribute(req, MESSAGE_KEY));

		req.setMethod("POST");
		getServlet().service(req, res);

		assertEquals(200, res.getStatus());
		assertEquals("Yay!", ((Message) getModelAttribute(req, MESSAGE_KEY)).getText());
		assertEquals("Yay!", ((Message) getFlashAttribute(req, MESSAGE_KEY)).getText());

		req.setMethod("GET");
		getServlet().service(req, res);

		assertEquals(200, res.getStatus());
		assertEquals("Yay!", ((Message) getModelAttribute(req, MESSAGE_KEY)).getText());
		assertNull(getFlashAttribute(req, MESSAGE_KEY));
	}

	@Test
	public void successMessageAcrossControllers() throws Exception {
		
		initServletWithModelExposingViewResolver(MessageController.class, SecondMessageController.class);

		MockHttpServletRequest req = new MockHttpServletRequest("POST", "/message");
		MockHttpServletResponse res = new MockHttpServletResponse();
		getServlet().service(req, res);
		
		req.setParameter("another", "true");
		getServlet().service(req, res);

		assertEquals(200, res.getStatus());
		assertEquals("Nay!", ((Message) getModelAttribute(req, MESSAGE_KEY)).getText());
		assertEquals("Nay!", ((Message) getFlashAttribute(req, MESSAGE_KEY)).getText());

		req.setMethod("GET");
		req.setRequestURI("/second/message");
		getServlet().service(req, res);

		assertEquals(200, res.getStatus());
		assertEquals("Nay!", ((Message) getModelAttribute(req, MESSAGE_KEY)).getText());
		assertNull(getFlashAttribute(req, MESSAGE_KEY));
	}

	@Controller
	@FlashAttributes("message")
	static class MessageController {

		@RequestMapping(value="/message", method=RequestMethod.GET)
		public void message(Model model) {
		}
		
		@RequestMapping(value="/message", method=RequestMethod.POST)
		public String sendMessage(Model model, FlashStatus status) {
			status.setActive();
			model.addAttribute(Message.success("Yay!"));
			return "redirect:/message";
		}
		
		@RequestMapping(value="/message", method=RequestMethod.POST, params="another")
		public String sendMessageToSecondController(Model model, FlashStatus status) {
			status.setActive();
			model.addAttribute(Message.error("Nay!"));
			return "redirect:/second/message";
		}
	}

	@Controller
	static class SecondMessageController {

		@RequestMapping(value="/second/message", method=RequestMethod.GET)
		public void message(Model model) {
		}
	}

	private static class Message {
		
		private final MessageType type;
		
		private final String text;

		private Message(MessageType type, String text) {
			this.type = type;
			this.text = text;
		}
		
		public static Message success(String text) {
			return new Message(MessageType.success, text);
		}

		public static Message error(String text) {
			return new Message(MessageType.error, text);
		}

		public MessageType getType() {
			return type;
		}

		public String getText() {
			return text;
		}
		
		public String toString() {
			return type + ": " + text;
		}
	
	}
	
	private static enum MessageType {
		info, success, warning, error
	}

	@SuppressWarnings("unchecked")
	private Object getModelAttribute(MockHttpServletRequest req, String key) {
		Map<String, ?> model = (Map<String, ?>) req.getAttribute(ModelExposingViewResolver.REQUEST_ATTRIBITE_MODEL);
		return model.get(key);
	}
	
	@SuppressWarnings("unchecked")
	private Object getFlashAttribute(MockHttpServletRequest req, String key) {
		String flashAttributesKey = FlashAttributesHandler.FLASH_ATTRIBUTES_SESSION_KEY;
		Map<String, Object> attrs = (Map<String, Object>) req.getSession().getAttribute(flashAttributesKey);
		return (attrs != null) ? attrs.get(key) : null;
	}

	private WebApplicationContext initServletWithModelExposingViewResolver(Class<?>... controllerClasses) 
			throws ServletException {
		
		return initServlet(new ApplicationContextInitializer<GenericWebApplicationContext>() {
			public void initialize(GenericWebApplicationContext wac) {
				wac.registerBeanDefinition("viewResolver", new RootBeanDefinition(ModelExposingViewResolver.class));
			}
		}, controllerClasses);
	}

	static class ModelExposingViewResolver implements ViewResolver {
		
		static String REQUEST_ATTRIBITE_MODEL = "ModelExposingViewResolver.model";

		public View resolveViewName(final String viewName, Locale locale) throws Exception {
			return new View() {
				public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
					request.setAttribute(REQUEST_ATTRIBITE_MODEL, model);
				}
				public String getContentType() {
					return null;
				}
			};
		}
	}

}
