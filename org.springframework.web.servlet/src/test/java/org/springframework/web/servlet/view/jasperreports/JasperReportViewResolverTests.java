/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.web.servlet.view.jasperreports;

import java.util.Locale;

import junit.framework.TestCase;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.web.servlet.view.velocity.VelocityView;

/**
 * @author Rob Harrop
 */
public class JasperReportViewResolverTests extends TestCase {

	public void testResolveView() throws Exception {
		StaticApplicationContext ctx = new StaticApplicationContext();

		String prefix = "org/springframework/ui/jasperreports/";
		String suffix = ".jasper";
		String viewName = "DataSourceReport";

		JasperReportsViewResolver viewResolver = new JasperReportsViewResolver();
		viewResolver.setViewClass(JasperReportsHtmlView.class);
		viewResolver.setPrefix(prefix);
		viewResolver.setSuffix(suffix);
		viewResolver.setApplicationContext(ctx);

		AbstractJasperReportsView view =
				(AbstractJasperReportsView) viewResolver.resolveViewName(viewName, Locale.ENGLISH);
		assertNotNull("View should not be null", view);
		assertEquals("Incorrect URL", prefix + viewName + suffix, view.getUrl());
	}

	public void testSetIncorrectViewClass() {
		try {
			new JasperReportsViewResolver().setViewClass(VelocityView.class);
			fail("Should not be able to set view class to a class that does not extend AbstractJasperReportsView");
		}
		catch (IllegalArgumentException ex) {
			// success
		}
	}

	public void testWithViewNamesAndEndsWithPattern() throws Exception {
		doViewNamesTest(new String[]{"DataSource*"});
	}

	public void testWithViewNamesAndStartsWithPattern() throws Exception {
		doViewNamesTest(new String[]{"*Report"});
	}

	public void testWithViewNamesAndStatic() throws Exception {
		doViewNamesTest(new String[]{"DataSourceReport"});
	}

	private void doViewNamesTest(String[] viewNames) throws Exception {
		 StaticApplicationContext ctx = new StaticApplicationContext();

		String prefix = "org/springframework/ui/jasperreports/";
		String suffix = ".jasper";
		String viewName = "DataSourceReport";

		JasperReportsViewResolver viewResolver = new JasperReportsViewResolver();
		viewResolver.setViewClass(JasperReportsHtmlView.class);
		viewResolver.setPrefix(prefix);
		viewResolver.setSuffix(suffix);
		viewResolver.setViewNames(viewNames);
		viewResolver.setApplicationContext(ctx);

		AbstractJasperReportsView view =
				(AbstractJasperReportsView) viewResolver.resolveViewName(viewName, Locale.ENGLISH);
		assertNotNull("View should not be null", view);
		assertEquals("Incorrect URL", prefix + viewName + suffix, view.getUrl());
		assertNull(viewResolver.resolveViewName("foo", Locale.ENGLISH));
	}
}
