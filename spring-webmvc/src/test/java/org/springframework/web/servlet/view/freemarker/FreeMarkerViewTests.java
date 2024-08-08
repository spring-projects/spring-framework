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

package org.springframework.web.servlet.view.freemarker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import freemarker.core.Environment;
import freemarker.ext.jakarta.servlet.AllHttpScopesHashModel;
import freemarker.ext.jakarta.servlet.FreemarkerServlet;
import freemarker.ext.jakarta.servlet.HttpRequestHashModel;
import freemarker.ext.jakarta.servlet.HttpSessionHashModel;
import freemarker.ext.jakarta.servlet.ServletContextHashModel;
import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModelEx;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContextException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link FreeMarkerView}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @since 14.03.2004
 */
class FreeMarkerViewTests {

	private static final String TEMPLATE_NAME = "templateName";


	private final WebApplicationContext wac = mock();

	private final ServletContext servletContext = new MockServletContext();

	private final FreeMarkerView freeMarkerView = new FreeMarkerView();

	@BeforeEach
	void setup() {
		given(this.wac.getServletContext()).willReturn(this.servletContext);
	}


	@Test
	void noFreeMarkerConfig() {
		given(this.wac.getBeansOfType(FreeMarkerConfig.class, true, false)).willReturn(new HashMap<>());

		freeMarkerView.setUrl("anythingButNull");

		assertThatExceptionOfType(ApplicationContextException.class)
				.isThrownBy(() -> freeMarkerView.setApplicationContext(this.wac))
				.withMessageContaining("Must define a single FreeMarkerConfig bean");
	}

	@Test
	void noTemplateName() {
		assertThatIllegalArgumentException()
				.isThrownBy(freeMarkerView::afterPropertiesSet)
				.withMessageContaining("Property 'url' is required");
	}

	@Test
	void validTemplateName() throws Exception {
		configureFreemarker(new TestConfiguration());
		freeMarkerView.setUrl(TEMPLATE_NAME);
		freeMarkerView.setApplicationContext(wac);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.US);
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		HttpServletResponse response = new MockHttpServletResponse();

		Map<String, Object> model = Map.of("myattr", "myvalue");
		freeMarkerView.render(model, request, response);

		assertThat(response.getContentType()).isEqualTo(AbstractView.DEFAULT_CONTENT_TYPE);
	}

	@Test
	void keepExistingContentType() throws Exception {
		configureFreemarker(new TestConfiguration());
		freeMarkerView.setUrl(TEMPLATE_NAME);
		freeMarkerView.setApplicationContext(wac);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.US);
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		HttpServletResponse response = new MockHttpServletResponse();
		response.setContentType("myContentType");

		Map<String, Object> model = Map.of("myattr", "myvalue");
		freeMarkerView.render(model, request, response);

		assertThat(response.getContentType()).isEqualTo("myContentType");
	}

	@Test
	void freemarkerModelHasJspTagLibs() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = new MockHttpServletResponse();
		Map<String, Object> model = Collections.emptyMap();
		testFreemarkerModel(request, response, model, dataModel -> {
			assertThat(dataModel.containsKey(FreemarkerServlet.KEY_JSP_TAGLIBS)).isTrue();
			assertThat(dataModel.get(FreemarkerServlet.KEY_JSP_TAGLIBS)).isNotNull();
		});
	}

	@Test
	void freemarkerModelHasHttpServletContext() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = new MockHttpServletResponse();
		Map<String, Object> model = Collections.emptyMap();
		testFreemarkerModel(request, response, model, dataModel -> {
			assertThat(dataModel.containsKey(FreemarkerServlet.KEY_APPLICATION)).isTrue();
			assertThat(dataModel.get(FreemarkerServlet.KEY_APPLICATION)).isInstanceOf(ServletContextHashModel.class);
		});
	}

	@Test
	void freemarkerModelHasHttpSession() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = new MockHttpServletResponse();
		Map<String, Object> model = Collections.emptyMap();
		testFreemarkerModel(request, response, model, dataModel -> {
			assertThat(dataModel.containsKey(FreemarkerServlet.KEY_SESSION)).isTrue();
			assertThat(dataModel.get(FreemarkerServlet.KEY_SESSION)).isInstanceOf(HttpSessionHashModel.class);
		});
	}

	@Test
	void freemarkerModelHasHttpServletRequest() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = new MockHttpServletResponse();
		Map<String, Object> model = Collections.emptyMap();
		testFreemarkerModel(request, response, model, dataModel -> {
			assertThat(dataModel.containsKey(FreemarkerServlet.KEY_REQUEST)).isTrue();
			assertThat(dataModel.get(FreemarkerServlet.KEY_REQUEST)).isInstanceOf(HttpRequestHashModel.class);
		});
	}

	@Test
	void freemarkerModelHasRequestAttributes() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("req1", "value1");
		request.addParameter("req2", "value2");

		testFreemarkerModel(request, new MockHttpServletResponse(), Collections.emptyMap(), dataModel -> {
			assertThat(dataModel.containsKey(FreemarkerServlet.KEY_REQUEST_PARAMETERS)).isTrue();
			assertThat((TemplateHashModelEx) dataModel.get(FreemarkerServlet.KEY_REQUEST_PARAMETERS)).satisfies(requestParameters -> {
				assertThat(requestParameters.get("req1")).isInstanceOf(SimpleScalar.class).hasToString("value1");
				assertThat(requestParameters.get("req2")).isInstanceOf(SimpleScalar.class).hasToString("value2");
			});
		});
	}

	@Test
	void freeMarkerViewResolver() throws Exception {
		MockServletContext sc = new MockServletContext();

		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setConfiguration(new TestConfiguration());
		configurer.setServletContext(sc);

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getBeanFactory().registerSingleton("configurer", configurer);
		wac.refresh();

		FreeMarkerViewResolver vr = new FreeMarkerViewResolver("templates/", ".ftl");
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("test", Locale.CANADA);
		assertThat(view).asInstanceOf(type(FreeMarkerView.class))
				.extracting(FreeMarkerView::getUrl)
				.isEqualTo("templates/test.ftl");

		view = vr.resolveViewName("non-existing", Locale.CANADA);
		assertThat(view).isNull();

		view = vr.resolveViewName("redirect:myRedirectUrl", Locale.getDefault());
		assertThat(view).asInstanceOf(type(RedirectView.class))
				.extracting(RedirectView::getUrl)
				.isEqualTo("myRedirectUrl");

		view = vr.resolveViewName("forward:myForwardUrl", Locale.getDefault());
		assertThat(view).asInstanceOf(type(InternalResourceView.class))
				.extracting(InternalResourceView::getUrl)
				.isEqualTo("myForwardUrl");
	}


	private void testFreemarkerModel(HttpServletRequest request, HttpServletResponse response, Map<String, Object> model,
			ThrowingConsumer<AllHttpScopesHashModel> dataModelAssertions) throws Exception {

		AtomicBoolean consumerCalled = new AtomicBoolean();
		Consumer<Object> delegate = object -> {
			consumerCalled.set(true);
			assertThat(object).isInstanceOf(AllHttpScopesHashModel.class)
					.asInstanceOf(InstanceOfAssertFactories.type(AllHttpScopesHashModel.class))
					.satisfies(dataModelAssertions);
		};

		configureFreemarker(new TestConfiguration(delegate));

		freeMarkerView.setUrl(TEMPLATE_NAME);
		freeMarkerView.setApplicationContext(wac);

		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());

		freeMarkerView.render(model, request, response);
		assertThat(consumerCalled).isTrue();

	}

	private void configureFreemarker(Configuration configuration) {
		Map<String, FreeMarkerConfig> configs = new HashMap<>();
		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setConfiguration(configuration);
		configurer.setServletContext(this.servletContext);
		configs.put("configurer", configurer);
		given(wac.getBeansOfType(FreeMarkerConfig.class, true, false)).willReturn(configs);
	}

	private static class TestConfiguration extends Configuration {

		private final Consumer<Object> modelAssertions;

		TestConfiguration(Consumer<Object> modelAssertions) {
			super(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
			this.modelAssertions = modelAssertions;
		}

		TestConfiguration() {
			this(model -> {});
		}

		@Override
		public Template getTemplate(String name, final Locale locale) throws IOException {
			if (name.equals(TEMPLATE_NAME) || name.equals("templates/test.ftl")) {
				return new Template(name, new StringReader("test"), this) {
					@Override
					public Environment createProcessingEnvironment(Object dataModel, Writer out) throws TemplateException, IOException {
						modelAssertions.accept(dataModel);
						return super.createProcessingEnvironment(dataModel, out);
					}
				};
			}
			else {
				throw new FileNotFoundException();
			}
		}
	}

}
