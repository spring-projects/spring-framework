/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.PathPatternsParameterizedTest;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 * @since 3.1
 */
class UriTemplateServletAnnotationControllerHandlerMethodTests extends AbstractServletHandlerMethodTests {

	@SuppressWarnings("unused")
	static Stream<Boolean> pathPatternsArguments() {
		return Stream.of(true, false);
	}


	@PathPatternsParameterizedTest
	void simple(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(SimpleUriTemplateController.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/42");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("test-42-7");
	}

	@PathPatternsParameterizedTest // gh-25864
	void literalMappingWithPathParams(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(MultipleUriTemplateController.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/data");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentAsString()).isEqualTo("test");

		if (!usePathPatterns) {
			request = new MockHttpServletRequest("GET", "/data;foo=bar");
			response = new MockHttpServletResponse();
			getServlet().service(request, response);
			assertThat(response.getStatus()).isEqualTo(404);
		}

		request = new MockHttpServletRequest("GET", "/data;jsessionid=123");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentAsString()).isEqualTo("test");
	}

	@PathPatternsParameterizedTest
	void multiple(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(MultipleUriTemplateController.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42;q=24/bookings/21-other;q=12");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentAsString()).isEqualTo("test-42-q24-21-other-q12");
	}

	@PathPatternsParameterizedTest
	void pathVarsInModel(boolean usePathPatterns) throws Exception {
		final Map<String, Object> pathVars = new HashMap<>();
		pathVars.put("hotel", "42");
		pathVars.put("booking", 21);
		pathVars.put("other", "other");

		WebApplicationContext wac = initDispatcherServlet(ViewRenderingController.class, usePathPatterns, context -> {
			RootBeanDefinition beanDef = new RootBeanDefinition(ModelValidatingViewResolver.class);
			beanDef.getConstructorArgumentValues().addGenericArgumentValue(pathVars);
			context.registerBeanDefinition("viewResolver", beanDef);
		});

		HttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42;q=1,2/bookings/21-other;q=3;r=R");
		getServlet().service(request, new MockHttpServletResponse());

		ModelValidatingViewResolver resolver = wac.getBean(ModelValidatingViewResolver.class);
		assertThat(resolver.validatedAttrCount).isEqualTo(3);
	}

	@PathPatternsParameterizedTest
	void binding(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(BindingUriTemplateController.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42/dates/2008-11-18");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);

		request = new MockHttpServletRequest("GET", "/hotels/42/dates/2008-foo-bar");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(400);

		initDispatcherServlet(NonBindingUriTemplateController.class, usePathPatterns);
		request = new MockHttpServletRequest("GET", "/hotels/42/dates/2008-foo-bar");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(500);
	}

	@PathPatternsParameterizedTest
	void ambiguous(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(AmbiguousUriTemplateController.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/new");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("specific");
	}

	@PathPatternsParameterizedTest
	void relative(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(RelativePathUriTemplateController.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42/bookings/21");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("test-42-21");
	}

	@PathPatternsParameterizedTest
	void extension(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(SimpleUriTemplateController.class, usePathPatterns, wac -> {
			if (!usePathPatterns) {
				RootBeanDefinition mappingDef = new RootBeanDefinition(RequestMappingHandlerMapping.class);
				mappingDef.getPropertyValues().add("useSuffixPatternMatch", true);
				mappingDef.getPropertyValues().add("removeSemicolonContent", "false");
				wac.registerBeanDefinition("handlerMapping", mappingDef);
			}
		});

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/42;jsessionid=c0o7fszeb1;q=24.xml");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString())
				.isEqualTo(!usePathPatterns ? "test-42-24" : "test-42-24.xml");
	}

	@PathPatternsParameterizedTest
	void typeConversionError(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(SimpleUriTemplateController.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo.xml");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).as("Invalid response status code").isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
	}

	@PathPatternsParameterizedTest
	void explicitSubPath(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(ExplicitSubPathController.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("test-42");
	}

	@PathPatternsParameterizedTest
	void implicitSubPath(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(ImplicitSubPathController.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/42");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("test-42");
	}

	@PathPatternsParameterizedTest
	void crud(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(CrudController.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("list");

		request = new MockHttpServletRequest("POST", "/hotels");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("create");

		request = new MockHttpServletRequest("GET", "/hotels/42");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("show-42");

		request = new MockHttpServletRequest("PUT", "/hotels/42");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("createOrUpdate-42");

		request = new MockHttpServletRequest("DELETE", "/hotels/42");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("remove-42");
	}

	@PathPatternsParameterizedTest
	void methodNotSupported(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(MethodNotAllowedController.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels/1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);

		request = new MockHttpServletRequest("POST", "/hotels/1");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(405);

		request = new MockHttpServletRequest("GET", "/hotels");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);

		request = new MockHttpServletRequest("POST", "/hotels");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(405);
	}

	@PathPatternsParameterizedTest
	void multiPaths(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(MultiPathController.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/category/page/5");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("handle4-page-5");
	}

	@PathPatternsParameterizedTest
	void customRegex(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(CustomRegexController.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/42;q=1;q=2");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getContentAsString())
				.isEqualTo(!usePathPatterns ? "test-42-;q=1;q=2-[1, 2]" : "test-42--[1, 2]");
	}

	@PathPatternsParameterizedTest // gh-11306
	void menuTree(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(MenuTreeController.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/book/menu/type/M5");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("M5");
	}

	@PathPatternsParameterizedTest // gh-11542
	void variableNames(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(VariableNamesController.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/foo");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("foo-foo");

		request = new MockHttpServletRequest("DELETE", "/test/bar");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("bar-bar");
	}

	@PathPatternsParameterizedTest // gh-13187
	void variableNamesWithUrlExtension(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(VariableNamesController.class, usePathPatterns, wac -> {
			if (!usePathPatterns) {
				RootBeanDefinition mappingDef = new RootBeanDefinition(RequestMappingHandlerMapping.class);
				mappingDef.getPropertyValues().add("useSuffixPatternMatch", true);
				wac.registerBeanDefinition("handlerMapping", mappingDef);
			}
		});

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test/foo.json");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo(!usePathPatterns ? "foo-foo" : "foo-foo.json");
	}

	@PathPatternsParameterizedTest // gh-11643
	void doIt(boolean usePathPatterns) throws Exception {
		initDispatcherServlet(Spr6978Controller.class, usePathPatterns);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo/100");
		MockHttpServletResponse response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("loadEntity:foo:100");

		request = new MockHttpServletRequest("POST", "/foo/100");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("publish:foo:100");

		request = new MockHttpServletRequest("GET", "/module/100");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("loadModule:100");

		request = new MockHttpServletRequest("POST", "/module/100");
		response = new MockHttpServletResponse();
		getServlet().service(request, response);
		assertThat(response.getContentAsString()).isEqualTo("publish:module:100");

	}


	/*
	 * Controllers
	 */

	@Controller
	public static class SimpleUriTemplateController {

		@RequestMapping("/{root}")
		void handle(@PathVariable("root") int root, @MatrixVariable(required=false, defaultValue="7") String q,
				Writer writer) throws IOException {

			assertThat(root).as("Invalid path variable value").isEqualTo(42);
			writer.write("test-" + root + "-" + q);
		}

	}

	@Controller
	public static class MultipleUriTemplateController {

		@RequestMapping("/hotels/{hotel}/bookings/{booking}-{other}")
		void handle(@PathVariable("hotel") String hotel,
				@PathVariable int booking,
				@PathVariable String other,
				@MatrixVariable(name = "q", pathVar = "hotel") int qHotel,
				@MatrixVariable(name = "q", pathVar = "other") int qOther,
				Writer writer) throws IOException {
			assertThat(hotel).as("Invalid path variable value").isEqualTo("42");
			assertThat(booking).as("Invalid path variable value").isEqualTo(21);
			writer.write("test-" + hotel + "-q" + qHotel + "-" + booking + "-" + other + "-q" + qOther);
		}

		@RequestMapping("/data")
		void handleWithLiteralMapping(Writer writer) throws IOException {
			writer.write("test");
		}
	}

	@Controller
	public static class ViewRenderingController {

		@RequestMapping("/hotels/{hotel}/bookings/{booking}-{other}")
		void handle(@PathVariable("hotel") String hotel, @PathVariable int booking,
				@PathVariable String other, @MatrixVariable MultiValueMap<String, String> params) {

			assertThat(hotel).as("Invalid path variable value").isEqualTo("42");
			assertThat(booking).as("Invalid path variable value").isEqualTo(21);
			assertThat(params.get("q")).containsExactlyInAnyOrder("1", "2", "3");
			assertThat(params.getFirst("r")).isEqualTo("R");
		}

	}

	@Controller
	public static class BindingUriTemplateController {

		@InitBinder
		void initBinder(WebDataBinder binder, @PathVariable("hotel") String hotel) {
			assertThat(hotel).as("Invalid path variable value").isEqualTo("42");
			binder.initBeanPropertyAccess();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setLenient(false);
			binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
		}

		@RequestMapping("/hotels/{hotel}/dates/{date}")
		void handle(@PathVariable("hotel") String hotel, @PathVariable Date date, Writer writer)
				throws IOException {
			assertThat(hotel).as("Invalid path variable value").isEqualTo("42");
			assertThat(date).as("Invalid path variable value").isEqualTo(new GregorianCalendar(2008, 10, 18).getTime());
			writer.write("test-" + hotel);
		}
	}

	@Controller
	public static class NonBindingUriTemplateController {

		@RequestMapping("/hotels/{hotel}/dates/{date}")
		void handle(@PathVariable("hotel") String hotel, @PathVariable Date date, Writer writer)
				throws IOException {
		}

	}

	@Controller
	@RequestMapping("/hotels/{hotel}")
	public static class RelativePathUriTemplateController {

		@RequestMapping("bookings/{booking}")
		void handle(@PathVariable("hotel") String hotel, @PathVariable int booking, Writer writer)
				throws IOException {
			assertThat(hotel).as("Invalid path variable value").isEqualTo("42");
			assertThat(booking).as("Invalid path variable value").isEqualTo(21);
			writer.write("test-" + hotel + "-" + booking);
		}

	}

	@Controller
	@RequestMapping("/hotels")
	public static class AmbiguousUriTemplateController {

		@RequestMapping("/{hotel}")
		void handleVars(@PathVariable("hotel") String hotel, Writer writer) throws IOException {
			assertThat(hotel).as("Invalid path variable value").isEqualTo("42");
			writer.write("variables");
		}

		@RequestMapping("/new")
		void handleSpecific(Writer writer) throws IOException {
			writer.write("specific");
		}

		@RequestMapping("/*")
		void handleWildCard(Writer writer) throws IOException {
			writer.write("wildcard");
		}

	}

	@Controller
	@RequestMapping("/hotels/*")
	public static class ExplicitSubPathController {

		@RequestMapping("{hotel}")
		void handleHotel(@PathVariable String hotel, Writer writer) throws IOException {
			writer.write("test-" + hotel);
		}

	}

	@Controller
	@RequestMapping("hotels")
	public static class ImplicitSubPathController {

		@RequestMapping("{hotel}")
		void handleHotel(@PathVariable String hotel, Writer writer) throws IOException {
			writer.write("test-" + hotel);
		}
	}

	@Controller
	public static class CustomRegexController {

		@RequestMapping("/{root:\\d+}{params}")
		void handle(@PathVariable("root") int root, @PathVariable("params") String paramString,
				@MatrixVariable List<Integer> q, Writer writer) throws IOException {

			assertThat(root).as("Invalid path variable value").isEqualTo(42);
			writer.write("test-" + root + "-" + paramString + "-" + q);
		}
	}

	@Controller
	public static class DoubleController {

		@RequestMapping("/lat/{latitude}/long/{longitude}")
		void testLatLong(@PathVariable Double latitude, @PathVariable Double longitude, Writer writer)
			throws IOException {
			writer.write("latitude-" + latitude + "-longitude-" + longitude);
		}
	}


	@Controller
	@RequestMapping("hotels")
	public static class CrudController {

		@RequestMapping(method = RequestMethod.GET)
		void list(Writer writer) throws IOException {
			writer.write("list");
		}

		@RequestMapping(method = RequestMethod.POST)
		void create(Writer writer) throws IOException {
			writer.write("create");
		}

		@RequestMapping(value = "/{hotel}", method = RequestMethod.GET)
		void show(@PathVariable String hotel, Writer writer) throws IOException {
			writer.write("show-" + hotel);
		}

		@RequestMapping(value = "{hotel}", method = RequestMethod.PUT)
		void createOrUpdate(@PathVariable String hotel, Writer writer) throws IOException {
			writer.write("createOrUpdate-" + hotel);
		}

		@RequestMapping(value = "{hotel}", method = RequestMethod.DELETE)
		void remove(@PathVariable String hotel, Writer writer) throws IOException {
			writer.write("remove-" + hotel);
		}

	}

	@Controller
	@RequestMapping("/hotels")
	public static class MethodNotAllowedController {

		@RequestMapping(method = RequestMethod.GET)
		void list(Writer writer) {
		}

		@RequestMapping(method = RequestMethod.GET, value = "{hotelId}")
		void show(@PathVariable long hotelId, Writer writer) {
		}

		@RequestMapping(method = RequestMethod.PUT, value = "{hotelId}")
		void createOrUpdate(@PathVariable long hotelId, Writer writer) {
		}

		@RequestMapping(method = RequestMethod.DELETE, value = "/{hotelId}")
		void remove(@PathVariable long hotelId, Writer writer) {
		}
	}

	@Controller
	@RequestMapping("/category")
	public static class MultiPathController {

		@RequestMapping(value = {"/{category}/page/{page}", "/*/{category}/page/{page}"})
		void category(@PathVariable String category, @PathVariable int page, Writer writer) throws IOException {
			writer.write("handle1-");
			writer.write("category-" + category);
			writer.write("page-" + page);
		}

		@RequestMapping(value = {"/{category}", "/*/{category}"})
		void category(@PathVariable String category, Writer writer) throws IOException {
			writer.write("handle2-");
			writer.write("category-" + category);
		}

		@RequestMapping(value = {""})
		void category(Writer writer) throws IOException {
			writer.write("handle3");
		}

		@RequestMapping(value = {"/page/{page}"})
		void category(@PathVariable int page, Writer writer) throws IOException {
			writer.write("handle4-");
			writer.write("page-" + page);
		}

	}

	@Controller
	@RequestMapping("/*/menu/") // was /*/menu/**
	public static class MenuTreeController {

		@RequestMapping("type/{var}")
		void getFirstLevelFunctionNodes(@PathVariable("var") String var, Writer writer) throws IOException {
			writer.write(var);
		}
	}

	@Controller
	@RequestMapping("/test")
	public static class VariableNamesController {

		@RequestMapping(value = "/{foo}", method=RequestMethod.GET)
		void foo(@PathVariable String foo, Writer writer) throws IOException {
			writer.write("foo-" + foo);
		}

		@RequestMapping(value = "/{bar}", method=RequestMethod.DELETE)
		void bar(@PathVariable String bar, Writer writer) throws IOException {
			writer.write("bar-" + bar);
		}
	}

	@Controller
	public static class Spr6978Controller {

		@RequestMapping(value = "/{type}/{id}", method = RequestMethod.GET)
		void loadEntity(@PathVariable final String type, @PathVariable final long id, Writer writer)
				throws IOException {
			writer.write("loadEntity:" + type + ":" + id);
		}

		@RequestMapping(value = "/module/{id}", method = RequestMethod.GET)
		void loadModule(@PathVariable final long id, Writer writer) throws IOException {
			writer.write("loadModule:" + id);
		}

		@RequestMapping(value = "/{type}/{id}", method = RequestMethod.POST)
		void publish(@PathVariable final String type, @PathVariable final long id, Writer writer)
				throws IOException {
			writer.write("publish:" + type + ":" + id);
		}
	}

	public static class ModelValidatingViewResolver implements ViewResolver {

		private final Map<String, Object> attrsToValidate;

		int validatedAttrCount;

		public ModelValidatingViewResolver(Map<String, Object> attrsToValidate) {
			this.attrsToValidate = attrsToValidate;
		}

		@Override
		public View resolveViewName(final String viewName, Locale locale) {
			return new AbstractView () {
				@Override
				public String getContentType() {
					return null;
				}
				@Override
				protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
						HttpServletResponse response) {
					for (String key : attrsToValidate.keySet()) {
						assertThat(model.containsKey(key)).as("Model should contain attribute named " + key).isTrue();
						assertThat(model.get(key)).isEqualTo(attrsToValidate.get(key));
						validatedAttrCount++;
					}
				}
			};
		}
	}

// @Disabled("ControllerClassNameHandlerMapping")
//	void controllerClassName() throws Exception {

//	@Disabled("useDefaultSuffixPattern property not supported")
//	void doubles() throws Exception {

//	@Disabled("useDefaultSuffixPattern property not supported")
//	void noDefaultSuffixPattern() throws Exception {

}
