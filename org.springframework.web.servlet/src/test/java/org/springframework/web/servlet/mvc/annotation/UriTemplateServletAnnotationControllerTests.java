package org.springframework.web.servlet.mvc.annotation;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletException;

import static org.junit.Assert.assertEquals;
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
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/** @author Arjen Poutsma */
public class UriTemplateServletAnnotationControllerTests {

	private DispatcherServlet servlet;

	@Test
	public void simple() throws Exception {
		initServlet(SimpleUriTemplateController.class);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42/bookings/21");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(request, response);
		assertEquals("test-42-21", response.getContentAsString());
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

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/12345");
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

	@Controller
	public static class SimpleUriTemplateController {

		@RequestMapping("/hotels/{hotel}/bookings/{booking}")
		public void handle(@PathVariable("hotel") String hotel, @PathVariable int booking, Writer writer) throws IOException {
			assertEquals("Invalid path variable value", "42", hotel);
			assertEquals("Invalid path variable value", 21, booking);
			writer.write("test-" + hotel + "-" + booking);
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
		public void handle(@PathVariable("hotel") String hotel, @PathVariable Date date, Writer writer) throws IOException {
			assertEquals("Invalid path variable value", "42", hotel);
			assertEquals("Invalid path variable value", new Date(108, 10, 18), date);
			writer.write("test-" + hotel);
		}

	}

	@Controller
	@RequestMapping("/hotels/{hotel}/**")
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
		@RequestMapping("/hotels/{hotel}")
		public void handleVars(Writer writer) throws IOException {
			writer.write("variables");
		}

		@RequestMapping("/hotels/12345")
		public void handleSpecific(Writer writer) throws IOException {
			writer.write("specific");
		}

	}

}
