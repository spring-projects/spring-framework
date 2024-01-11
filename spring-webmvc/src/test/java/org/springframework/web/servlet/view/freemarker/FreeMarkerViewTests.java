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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import jakarta.servlet.http.HttpServletResponse;
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
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 14.03.2004
 */
class FreeMarkerViewTests {

	private static final String TEMPLATE_NAME = "templateName";


	private final FreeMarkerView freeMarkerView = new FreeMarkerView();


	@Test
	void noFreeMarkerConfig() {
		WebApplicationContext wac = mock();
		given(wac.getBeansOfType(FreeMarkerConfig.class, true, false)).willReturn(new HashMap<>());
		given(wac.getServletContext()).willReturn(new MockServletContext());

		freeMarkerView.setUrl("anythingButNull");

		assertThatExceptionOfType(ApplicationContextException.class)
			.isThrownBy(() -> freeMarkerView.setApplicationContext(wac))
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
		WebApplicationContext wac = mock();
		MockServletContext sc = new MockServletContext();

		Map<String, FreeMarkerConfig> configs = new HashMap<>();
		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setConfiguration(new TestConfiguration());
		configs.put("configurer", configurer);
		given(wac.getBeansOfType(FreeMarkerConfig.class, true, false)).willReturn(configs);
		given(wac.getServletContext()).willReturn(sc);

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
		WebApplicationContext wac = mock();
		MockServletContext sc = new MockServletContext();

		Map<String, FreeMarkerConfig> configs = new HashMap<>();
		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setConfiguration(new TestConfiguration());
		configs.put("configurer", configurer);
		given(wac.getBeansOfType(FreeMarkerConfig.class, true, false)).willReturn(configs);
		given(wac.getServletContext()).willReturn(sc);

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
	void requestAttributeVisible() throws Exception {
		WebApplicationContext wac = mock();
		MockServletContext sc = new MockServletContext();

		Map<String, FreeMarkerConfig> configs = new HashMap<>();
		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setConfiguration(new TestConfiguration());
		configs.put("configurer", configurer);
		given(wac.getBeansOfType(FreeMarkerConfig.class, true, false)).willReturn(configs);
		given(wac.getServletContext()).willReturn(sc);

		freeMarkerView.setUrl(TEMPLATE_NAME);
		freeMarkerView.setApplicationContext(wac);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.US);
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		HttpServletResponse response = new MockHttpServletResponse();

		request.setAttribute("myattr", "myvalue");
		freeMarkerView.render(null, request, response);
	}

	@Test
	void freeMarkerViewResolver() throws Exception {
		MockServletContext sc = new MockServletContext();

		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setConfiguration(new TestConfiguration());

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


	private static class TestConfiguration extends Configuration {

		TestConfiguration() {
			super(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		}

		@Override
		public Template getTemplate(String name, final Locale locale) throws IOException {
			if (name.equals(TEMPLATE_NAME) || name.equals("templates/test.ftl")) {
				return new Template(name, new StringReader("test"), this) {
					@Override
					public void process(Object model, Writer writer) {
						assertThat(locale).isEqualTo(Locale.US);
						assertThat(model).asInstanceOf(type(SimpleHash.class)).satisfies(
								fmModel -> assertThat(fmModel.get("myattr")).asString().isEqualTo("myvalue"));
					}
				};
			}
			else {
				throw new FileNotFoundException();
			}
		}
	}

}
