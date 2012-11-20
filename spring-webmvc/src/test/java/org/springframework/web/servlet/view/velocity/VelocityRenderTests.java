/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.view.velocity;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.TypeSafeMatcher;
import org.junit.rules.ExpectedException;
import org.springframework.beans.TestBean;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.theme.FixedThemeResolver;
import org.springframework.web.util.NestedServletException;

/**
 * @author Dave Syer
 */
public class VelocityRenderTests {

	private StaticWebApplicationContext wac;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setUp() throws Exception {
		wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());

		final Template expectedTemplate = new Template();
		VelocityConfig vc = new VelocityConfig() {
			public VelocityEngine getVelocityEngine() {
				return new TestVelocityEngine("test.vm", expectedTemplate);
			}
		};
		wac.getDefaultListableBeanFactory().registerSingleton("velocityConfigurer", vc);
		wac.refresh();

		request = new MockHttpServletRequest();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, new FixedThemeResolver());
		response = new MockHttpServletResponse();
	}

	@Test
	public void testSimpleRender() throws Exception {

		VelocityConfigurer vc = new VelocityConfigurer();
		vc.setPreferFileSystemAccess(false);
		VelocityEngine ve = vc.createVelocityEngine();

		VelocityView view = new VelocityView();
		view.setBeanName("myView");
		view.setUrl("org/springframework/web/servlet/view/velocity/simple.vm");
		view.setVelocityEngine(ve);
		view.setApplicationContext(wac);


		Map<String,Object> model = new HashMap<String,Object>();
		model.put("command", new TestBean("juergen", 99));
		view.render(model, request, response);
		assertEquals("\nNAME\njuergen\n", response.getContentAsString().replace("\r\n", "\n"));

	}

	@Test
	@Ignore // This works with Velocity 1.6.2
	public void testSimpleRenderWithError() throws Exception {

		thrown.expect(NestedServletException.class);

		thrown.expect(new TypeSafeMatcher<Exception>() {
			public boolean matchesSafely(Exception item) {
				return item.getCause() instanceof MethodInvocationException;
			}
			public void describeTo(Description description) {
				description.appendText("exception has cause of MethodInvocationException");

			}
		});

		VelocityConfigurer vc = new VelocityConfigurer();
		vc.setPreferFileSystemAccess(false);
		vc.setVelocityPropertiesMap(Collections.<String,Object>singletonMap("runtime.references.strict", "true"));
		VelocityEngine ve = vc.createVelocityEngine();

		VelocityView view = new VelocityView();
		view.setBeanName("myView");
		view.setUrl("org/springframework/web/servlet/view/velocity/error.vm");
		view.setVelocityEngine(ve);
		view.setApplicationContext(wac);

		Map<String,Object> model = new HashMap<String,Object>();
		model.put("command", new TestBean("juergen", 99));
		view.render(model, request, response);

	}

	@Test
	public void testSimpleRenderWithIOError() throws Exception {

		thrown.expect(NestedServletException.class);

		thrown.expect(new TypeSafeMatcher<Exception>() {
			public boolean matchesSafely(Exception item) {
				return item.getCause() instanceof IOException;
			}
			public void describeTo(Description description) {
				description.appendText("exception has cause of IOException");

			}
		});

		VelocityConfigurer vc = new VelocityConfigurer();
		vc.setPreferFileSystemAccess(false);
		vc.setVelocityPropertiesMap(Collections.<String,Object>singletonMap("runtime.references.strict", "true"));
		VelocityEngine ve = vc.createVelocityEngine();

		VelocityView view = new VelocityView();
		view.setBeanName("myView");
		view.setUrl("org/springframework/web/servlet/view/velocity/ioerror.vm");
		view.setVelocityEngine(ve);
		view.setApplicationContext(wac);

		Map<String,Object> model = new HashMap<String,Object>();
		model.put("command", new TestBean("juergen", 99));
		view.render(model, request, response);

	}

}
