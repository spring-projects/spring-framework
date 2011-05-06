/*
 * Copyright 2002-2010 the original author or authors.
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

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
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
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.annotation.UriTemplateServletAnnotationControllerTests;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

/**
 * The origin of this test class is {@link UriTemplateServletAnnotationControllerTests} with the tests in this class
 * adapted to run against the HandlerMethod infrastructure rather than against the DefaultAnnotationHandlerMapping,
 * the AnnotationMethodHandlerAdapter, and the AnnotationMethodHandlerExceptionResolver. Tests that are not supported
 * are listed at the bottom.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class UriTemplateServletHandlerMethodTests {

	private DispatcherServlet servlet;

	@Test
	public void simple() throws Exception {
		initDispatcherServlet(SimpleUriTemplateController.class, null);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/42");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-42", response.getContentAsString());
	}

	@Test
	public void multiple() throws Exception {
		initDispatcherServlet(MultipleUriTemplateController.class, null);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42/bookings/21-other");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-42-21-other", response.getContentAsString());
	}

	@Test
	public void binding() throws Exception {
		initDispatcherServlet(BindingUriTemplateController.class, null);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42/dates/2008-11-18");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(200, response.getStatus());

		request = new MockHttpServletRequest("GET", "/hotels/42/dates/2008-foo-bar");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(400, response.getStatus());

		initDispatcherServlet(NonBindingUriTemplateController.class, null);
		request = new MockHttpServletRequest("GET", "/hotels/42/dates/2008-foo-bar");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(500, response.getStatus());
	}

	@Test
	public void ambiguous() throws Exception {
		initDispatcherServlet(AmbiguousUriTemplateController.class, null);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/new");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("specific", response.getContentAsString());
	}

	@Test
	public void relative() throws Exception {
		initDispatcherServlet(RelativePathUriTemplateController.class, null);

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
		initDispatcherServlet(SimpleUriTemplateController.class, null);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/42.xml");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-42", response.getContentAsString());

	}

	@Test
	public void typeConversionError() throws Exception {
		initDispatcherServlet(SimpleUriTemplateController.class, null);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo.xml");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("Invalid response status code", HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
	}

	@Test
	public void explicitSubPath() throws Exception {
		initDispatcherServlet(ExplicitSubPathController.class, null);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-42", response.getContentAsString());
	}

	@Test
	public void implicitSubPath() throws Exception {
		initDispatcherServlet(ImplicitSubPathController.class, null);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-42", response.getContentAsString());
	}

	@Test
	public void crud() throws Exception {
		initDispatcherServlet(CrudController.class, null);

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

	@Test
	public void methodNotSupported() throws Exception {
		initDispatcherServlet(MethodNotAllowedController.class, null);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(200, response.getStatus());

		request = new MockHttpServletRequest("POST", "/hotels/1");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(405, response.getStatus());

		request = new MockHttpServletRequest("GET", "/hotels");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(200, response.getStatus());

		request = new MockHttpServletRequest("POST", "/hotels");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals(405, response.getStatus());


	}

	@Test
	public void multiPaths() throws Exception {
		initDispatcherServlet(MultiPathController.class, null);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/category/page/5");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("handle4-page-5", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/category/page/5.html");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("handle4-page-5", response.getContentAsString());
	}

	@Test
	public void customRegex() throws Exception {
		initDispatcherServlet(CustomRegexController.class, null);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/42");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-42", response.getContentAsString());
	}

	/*
	 * See SPR-6640
	 */
	@Test
	public void menuTree() throws Exception {
		initDispatcherServlet(MenuTreeController.class, null);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/book/menu/type/M5");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("M5", response.getContentAsString());
	}

	/*
	 * See SPR-6876
	 */
	@Test
	public void variableNames() throws Exception {
		initDispatcherServlet(VariableNamesController.class, null);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/foo");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("foo-foo", response.getContentAsString());

		request = new MockHttpServletRequest("DELETE", "/test/bar");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("bar-bar", response.getContentAsString());
	}

	/*
	 * See SPR-6978
	 */
	@Test
	public void doIt() throws Exception {
		initDispatcherServlet(Spr6978Controller.class, null);
		
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo/100");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("loadEntity:foo:100", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/foo/100");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("publish:foo:100", response.getContentAsString());

		request = new MockHttpServletRequest("GET", "/module/100");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("loadModule:100", response.getContentAsString());

		request = new MockHttpServletRequest("POST", "/module/100");
		response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("publish:module:100", response.getContentAsString());

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
			assertEquals("Invalid path variable value", new GregorianCalendar(2008, 10, 18).getTime(), date);
			writer.write("test-" + hotel);
		}
	}

	@Controller
	public static class NonBindingUriTemplateController {

		@RequestMapping("/hotels/{hotel}/dates/{date}")
		public void handle(@PathVariable("hotel") String hotel, @PathVariable Date date, Writer writer)
				throws IOException {
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
	@RequestMapping("/hotels")
	public static class AmbiguousUriTemplateController {

		@RequestMapping("/{hotel}")
		public void handleVars(@PathVariable("hotel") String hotel, Writer writer) throws IOException {
			assertEquals("Invalid path variable value", "42", hotel);
			writer.write("variables");
		}

		@RequestMapping("/new")
		public void handleSpecific(Writer writer) throws IOException {
			writer.write("specific");
		}

		@RequestMapping("/*")
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
	public static class CustomRegexController {

		@RequestMapping("/{root:\\d+}")
		public void handle(@PathVariable("root") int root, Writer writer) throws IOException {
			assertEquals("Invalid path variable value", 42, root);
			writer.write("test-" + root);
		}

	}

	@Controller
	public static class DoubleController {

		@RequestMapping("/lat/{latitude}/long/{longitude}")
		public void testLatLong(@PathVariable Double latitude, @PathVariable Double longitude, Writer writer)
			throws IOException {
			writer.write("latitude-" + latitude + "-longitude-" + longitude);
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

		@RequestMapping(value = "/{hotel}", method = RequestMethod.GET)
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

	@Controller
	@RequestMapping("/hotels")
	public static class MethodNotAllowedController {

		@RequestMapping(method = RequestMethod.GET)
		public void list(Writer writer) {
		}

		@RequestMapping(method = RequestMethod.GET, value = "{hotelId}")
		public void show(@PathVariable long hotelId, Writer writer) {
		}

		@RequestMapping(method = RequestMethod.PUT, value = "{hotelId}")
		public void createOrUpdate(@PathVariable long hotelId, Writer writer) {
		}

		@RequestMapping(method = RequestMethod.DELETE, value = "/{hotelId}")
		public void remove(@PathVariable long hotelId, Writer writer) {
		}
	}

	@Controller
	@RequestMapping("/category")
	public static class MultiPathController {

		@RequestMapping(value = {"/{category}/page/{page}", "/**/{category}/page/{page}"})
		public void category(@PathVariable String category, @PathVariable int page, Writer writer) throws IOException {
			writer.write("handle1-");
			writer.write("category-" + category);
			writer.write("page-" + page);
		}

		@RequestMapping(value = {"/{category}", "/**/{category}"})
		public void category(@PathVariable String category, Writer writer) throws IOException {
			writer.write("handle2-");
			writer.write("category-" + category);
		}

		@RequestMapping(value = {""})
		public void category(Writer writer) throws IOException {
			writer.write("handle3");
		}

		@RequestMapping(value = {"/page/{page}"})
		public void category(@PathVariable int page, Writer writer) throws IOException {
			writer.write("handle4-");
			writer.write("page-" + page);
		}

	}

	@RequestMapping("/*/menu/**")
	public static class MenuTreeController {

		@RequestMapping("type/{var}")
		public void getFirstLevelFunctionNodes(@PathVariable("var") String var, Writer writer) throws IOException {
			writer.write(var);
		}
	}

	@Controller
	@RequestMapping("/test")
	public static class VariableNamesController {

		@RequestMapping(value = "/{foo}", method=RequestMethod.GET)
		public void foo(@PathVariable String foo, Writer writer) throws IOException {
			writer.write("foo-" + foo);
		}

		@RequestMapping(value = "/{bar}", method=RequestMethod.DELETE)
		public void bar(@PathVariable String bar, Writer writer) throws IOException {
			writer.write("bar-" + bar);
		}
	}

	@Controller
	public static class Spr6978Controller {

		@RequestMapping(value = "/{type}/{id}", method = RequestMethod.GET)
		public void loadEntity(@PathVariable final String type, @PathVariable final long id, Writer writer)
				throws IOException {
			writer.write("loadEntity:" + type + ":" + id);
		}

		@RequestMapping(value = "/module/{id}", method = RequestMethod.GET)
		public void loadModule(@PathVariable final long id, Writer writer) throws IOException {
			writer.write("loadModule:" + id);
		}

		@RequestMapping(value = "/{type}/{id}", method = RequestMethod.POST)
		public void publish(@PathVariable final String type, @PathVariable final long id, Writer writer)
				throws IOException {
			writer.write("publish:" + type + ":" + id);
		}
	}

	private interface BeanDefinitionRegistrar {
		public void register(GenericWebApplicationContext context);
	}

	@SuppressWarnings("serial")
	private void initDispatcherServlet(final Class<?> controllerClass, final BeanDefinitionRegistrar registrar)
			throws ServletException {

		servlet = new DispatcherServlet() {
			@Override
			protected WebApplicationContext createWebApplicationContext(WebApplicationContext parent) {
				GenericWebApplicationContext wac = new GenericWebApplicationContext();
				wac.registerBeanDefinition("controller", new RootBeanDefinition(controllerClass));
				
				Class<?> mappingType = RequestMappingHandlerMapping.class;
				wac.registerBeanDefinition("handlerMapping", new RootBeanDefinition(mappingType));
				
				Class<?> adapterType = RequestMappingHandlerAdapter.class;
				wac.registerBeanDefinition("handlerAdapter", new RootBeanDefinition(adapterType));
				
				Class<?> resolverType = ExceptionHandlerExceptionResolver.class;
				wac.registerBeanDefinition("requestMappingResolver", new RootBeanDefinition(resolverType));
				
				resolverType = ResponseStatusExceptionResolver.class;
				wac.registerBeanDefinition("responseStatusResolver", new RootBeanDefinition(resolverType));
				
				resolverType = DefaultHandlerExceptionResolver.class;
				wac.registerBeanDefinition("defaultResolver", new RootBeanDefinition(resolverType));

				if (registrar != null) {
					registrar.register(wac);
				}

				wac.refresh();
				return wac;
			}
		};
		servlet.init(new MockServletConfig());
	}

// @Ignore("ControllerClassNameHandlerMapping")	
//	public void controllerClassName() throws Exception {

//	@Ignore("useDefaultSuffixPattern property not supported")
//	public void doubles() throws Exception {

//	@Ignore("useDefaultSuffixPattern property not supported")
//	public void noDefaultSuffixPattern() throws Exception {
	
}
