/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.servlet.mvc.annotation;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/** @author Arjen Poutsma */
public class UriTemplateServletAnnotationControllerTests {

	private DispatcherServlet servlet;

	@Test
	public void simple() throws Exception {
		initServlet(SimpleUriTemplateController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/42");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-42", response.getContentAsString());
	}

	@Test
	public void multiple() throws Exception {
		initServlet(MultipleUriTemplateController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42/bookings/21-other");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-42-21-other", response.getContentAsString());
	}

	@Test
	public void binding() throws Exception {
		initServlet(BindingUriTemplateController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42/dates/2008-11-18");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-42", response.getContentAsString());
	}

	@Test
	public void ambiguous() throws Exception {
		initServlet(AmbiguousUriTemplateController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/new");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("specific", response.getContentAsString());
	}

	@Test
	public void relative() throws Exception {
		initServlet(RelativePathUriTemplateController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42/bookings/21");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-42-21", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/hotels/42/bookings/21.html");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-42-21", response.getContentAsString());
	}

	@Test
	public void extension() throws Exception {
		initServlet(SimpleUriTemplateController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/42.xml");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-42", response.getContentAsString());

	}

	@Test
	public void typeConversionError() throws Exception {
		initServlet(SimpleUriTemplateController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo.xml");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("Invalid response status code", HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
	}

	@Test
	public void explicitSubPath() throws Exception {
		initServlet(ExplicitSubPathController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-42", response.getContentAsString());
	}

	@Test
	public void implicitSubPath() throws Exception {
		initServlet(ImplicitSubPathController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-42", response.getContentAsString());
	}

	@Test
	public void crud() throws Exception {
		initServlet(CrudController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("list", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/hotels/");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("list", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/hotels");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("create", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/hotels/42");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("show-42", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/hotels/42/");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("show-42", response.getContentAsString());

		request = new MockHttpServletRequest("PUT", "/hotels/42");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("createOrUpdate-42", response.getContentAsString());

		request = new MockHttpServletRequest("DELETE", "/hotels/42");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("remove-42", response.getContentAsString());
	}

	private void initServlet(final Class<?> controllerclass) throws ServletException {
		servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent)
					throws BeansException {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(controllerclass));
				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());
	}

	/*
	 * Controllers
	 */

	@Controller
	public static class SimpleUriTemplateController {

		@RequestMapping("/{root}")
		public void handle(@PathVariable("root") int root, Writer writer) throws IOException {
			assertEquals("Invalid path variable value", 42, root);
			writer.write("test-" + root);
		}

	}

	@Controller
	public static class MultipleUriTemplateController {

		@RequestMapping("/hotels/{hotel}/bookings/{booking}-{other}")
		public void handle(@PathVariable("hotel") String hotel,
				@PathVariable int booking,
				@PathVariable String other,
				Writer writer) throws IOException {
			assertEquals("Invalid path variable value", "42", hotel);
			assertEquals("Invalid path variable value", 21, booking);
			writer.write("test-" + hotel + "-" + booking + "-" + other);
		}

	}

	@Controller
	public static class BindingUriTemplateController {

		@InitBinder
		public void initBinder(WebDataBinder binder, @PathVariable("hotel") String hotel) {
			assertEquals("Invalid path variable value", "42", hotel);
			binder.initBeanPropertyAccess();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}

		@RequestMapping("/hotels/{hotel}/dates/{date}")
		public void handle(@PathVariable("hotel") String hotel, @PathVariable Date date, Writer writer)
				throws IOException {
			assertEquals("Invalid path variable value", "42", hotel);
			assertEquals("Invalid path variable value", new Date(108, 10, 18), date);
			writer.write("test-" + hotel);
		}

	}

	@Controller
	@RequestMapping("/hotels/{hotel}")
	public static class RelativePathUriTemplateController {

		@RequestMapping("bookings/{booking}")
		public void handle(@PathVariable("hotel") String hotel, @PathVariable int booking, Writer writer)
				throws IOException {
			assertEquals("Invalid path variable value", "42", hotel);
			assertEquals("Invalid path variable value", 21, booking);
			writer.write("test-" + hotel + "-" + booking);
		}

	}

	@Controller
	public static class AmbiguousUriTemplateController {

		@RequestMapping("/hotels/new")
		public void handleSpecific(Writer writer) throws IOException {
			writer.write("specific");
		}

		@RequestMapping("/hotels/{hotel}")
		public void handleVars(@PathVariable("hotel") String hotel, Writer writer) throws IOException {
			assertEquals("Invalid path variable value", "42", hotel);
			writer.write("variables");
		}

		@RequestMapping("/hotels/*")
		public void handleWildCard(Writer writer) throws IOException {
			writer.write("wildcard");
		}

	}

	@Controller
	@RequestMapping("/hotels/*")
	public static class ExplicitSubPathController {

		@RequestMapping("{hotel}")
		public void handleHotel(@PathVariable String hotel, Writer writer) throws IOException {
			writer.write("test-" + hotel);
		}

	}

	@Controller
	@RequestMapping("hotels")
	public static class ImplicitSubPathController {

		@RequestMapping("{hotel}")
		public void handleHotel(@PathVariable String hotel, Writer writer) throws IOException {
			writer.write("test-" + hotel);
		}
	}

	@Controller
	@RequestMapping("hotels")
	public static class CrudController {

		@RequestMapping(method = RequestMethod.GET)
		public void list(Writer writer) throws IOException {
			writer.write("list");
		}

		@RequestMapping(method = RequestMethod.POST)
		public void create(Writer writer) throws IOException {
			writer.write("create");
		}

		@RequestMapping(value = "{hotel}", method = RequestMethod.GET)
		public void show(@PathVariable String hotel, Writer writer) throws IOException {
			writer.write("show-" + hotel);
		}

		@RequestMapping(value = "{hotel}", method = RequestMethod.PUT)
		public void createOrUpdate(@PathVariable String hotel, Writer writer) throws IOException {
			writer.write("createOrUpdate-" + hotel);
		}

		@RequestMapping(value = "{hotel}", method = RequestMethod.DELETE)
		public void remove(@PathVariable String hotel, Writer writer) throws IOException {
			writer.write("remove-" + hotel);
		}

	}


}
