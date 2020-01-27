/*
 * Copyright 2002-2019 the original author or authors.
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

import javax.servlet.http.HttpServletResponse;

import freemarker.ext.servlet.AllHttpScopesHashModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 14.03.2004
 */
public class FreeMarkerViewTests {

	@Test
	public void noFreeMarkerConfig() throws Exception {
		FreeMarkerView fv = new FreeMarkerView();

		WebApplicationContext wac = mock(WebApplicationContext.class);
		given(wac.getBeansOfType(FreeMarkerConfig.class, true, false)).willReturn(new HashMap<>());
		given(wac.getServletContext()).willReturn(new MockServletContext());

		fv.setUrl("anythingButNull");

		assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() ->
				fv.setApplicationContext(wac))
			.withMessageContaining("FreeMarkerConfig");
	}

	@Test
	public void noTemplateName() throws Exception {
		FreeMarkerView fv = new FreeMarkerView();

		assertThatIllegalArgumentException().isThrownBy(() ->
				fv.afterPropertiesSet())
			.withMessageContaining("url");
	}

	@Test
	public void validTemplateName() throws Exception {
		FreeMarkerView fv = new FreeMarkerView();

		WebApplicationContext wac = mock(WebApplicationContext.class);
		MockServletContext sc = new MockServletContext();

		Map<String, FreeMarkerConfig> configs = new HashMap<>();
		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setConfiguration(new TestConfiguration());
		configurer.setServletContext(sc);
		configs.put("configurer", configurer);
		given(wac.getBeansOfType(FreeMarkerConfig.class, true, false)).willReturn(configs);
		given(wac.getServletContext()).willReturn(sc);

		fv.setUrl("templateName");
		fv.setApplicationContext(wac);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.US);
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		HttpServletResponse response = new MockHttpServletResponse();

		Map<String, Object> model = new HashMap<>();
		model.put("myattr", "myvalue");
		fv.render(model, request, response);

		assertThat(response.getContentType()).isEqualTo(AbstractView.DEFAULT_CONTENT_TYPE);
	}

	@Test
	public void keepExistingContentType() throws Exception {
		FreeMarkerView fv = new FreeMarkerView();

		WebApplicationContext wac = mock(WebApplicationContext.class);
		MockServletContext sc = new MockServletContext();

		Map<String, FreeMarkerConfig> configs = new HashMap<>();
		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setConfiguration(new TestConfiguration());
		configurer.setServletContext(sc);
		configs.put("configurer", configurer);
		given(wac.getBeansOfType(FreeMarkerConfig.class, true, false)).willReturn(configs);
		given(wac.getServletContext()).willReturn(sc);

		fv.setUrl("templateName");
		fv.setApplicationContext(wac);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.US);
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		HttpServletResponse response = new MockHttpServletResponse();
		response.setContentType("myContentType");

		Map<String, Object> model = new HashMap<>();
		model.put("myattr", "myvalue");
		fv.render(model, request, response);

		assertThat(response.getContentType()).isEqualTo("myContentType");
	}

	@Test
	public void freeMarkerViewResolver() throws Exception {
		MockServletContext sc = new MockServletContext();

		FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
		configurer.setConfiguration(new TestConfiguration());
		configurer.setServletContext(sc);

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(sc);
		wac.getBeanFactory().registerSingleton("configurer", configurer);
		wac.refresh();

		FreeMarkerViewResolver vr = new FreeMarkerViewResolver("prefix_", "_suffix");
		vr.setApplicationContext(wac);

		View view = vr.resolveViewName("test", Locale.CANADA);
		assertThat(view.getClass()).as("Correct view class").isEqualTo(FreeMarkerView.class);
		assertThat(((FreeMarkerView) view).getUrl()).as("Correct URL").isEqualTo("prefix_test_suffix");

		view = vr.resolveViewName("non-existing", Locale.CANADA);
		assertThat(view).isNull();

		view = vr.resolveViewName("redirect:myUrl", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(RedirectView.class);
		assertThat(((RedirectView) view).getUrl()).as("Correct URL").isEqualTo("myUrl");

		view = vr.resolveViewName("forward:myUrl", Locale.getDefault());
		assertThat(view.getClass()).as("Correct view class").isEqualTo(InternalResourceView.class);
		assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("myUrl");
	}


	private class TestConfiguration extends Configuration {

		TestConfiguration() {
			super(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		}

		@Override
		public Template getTemplate(String name, final Locale locale) throws IOException {
			if (name.equals("templateName") || name.equals("prefix_test_suffix")) {
				return new Template(name, new StringReader("test"), this) {
					@Override
					public void process(Object model, Writer writer) throws TemplateException, IOException {
						assertThat(locale).isEqualTo(Locale.US);
						boolean condition = model instanceof AllHttpScopesHashModel;
						assertThat(condition).isTrue();
						AllHttpScopesHashModel fmModel = (AllHttpScopesHashModel) model;
						assertThat(fmModel.get("myattr").toString()).isEqualTo("myvalue");
					}
				};
			}
			else {
				throw new FileNotFoundException();
			}
		}
	}

}
