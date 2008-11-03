/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.web.jsf;

import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.VariableResolver;

import junit.framework.TestCase;

import org.springframework.beans.TestBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

/**
 * @author Juergen Hoeller
 * @since 02.08.2004
 */
public class DelegatingVariableResolverTests extends TestCase {

	public void testDelegatingVariableResolver() {
		final StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("bean1", TestBean.class, null);
		wac.registerSingleton("var1", TestBean.class, null);
		wac.refresh();
		TestBean bean1 = (TestBean) wac.getBean("bean1");

		// We need to override the getWebApplicationContext method here:
		// FacesContext and ExternalContext are hard to mock.
		DelegatingVariableResolver resolver = new DelegatingVariableResolver(new OriginalVariableResolver()) {
			protected WebApplicationContext getWebApplicationContext(FacesContext facesContext) {
				return wac;
			}
		};
		assertEquals(bean1, resolver.resolveVariable(null, "bean1"));
		assertEquals("val1", resolver.resolveVariable(null, "var1"));
	}

	public void testSpringBeanVariableResolver() {
		final StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("bean1", TestBean.class, null);
		wac.registerSingleton("var1", TestBean.class, null);
		wac.refresh();
		TestBean bean1 = (TestBean) wac.getBean("bean1");
		TestBean var1 = (TestBean) wac.getBean("var1");

		// We need to override the getWebApplicationContext method here:
		// FacesContext and ExternalContext are hard to mock.
		SpringBeanVariableResolver resolver = new SpringBeanVariableResolver(new OriginalVariableResolver()) {
			protected WebApplicationContext getWebApplicationContext(FacesContext facesContext) {
				return wac;
			}
		};
		assertEquals(bean1, resolver.resolveVariable(null, "bean1"));
		assertEquals(var1, resolver.resolveVariable(null, "var1"));
	}


	private static class OriginalVariableResolver extends VariableResolver {

		public Object resolveVariable(FacesContext facesContext, String name) throws EvaluationException {
			if ("var1".equals(name)) {
				return "val1";
			}
			return null;
		}
	}

}
