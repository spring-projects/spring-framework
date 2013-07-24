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

package org.springframework.web.servlet.view.jasperreports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.ui.jasperreports.PersonBean;
import org.springframework.ui.jasperreports.ProductBean;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public abstract class AbstractJasperReportsTests {

	protected static final String COMPILED_REPORT = "/org/springframework/ui/jasperreports/DataSourceReport.jasper";

	protected static final String UNCOMPILED_REPORT = "/org/springframework/ui/jasperreports/DataSourceReport.jrxml";

	protected static final String SUB_REPORT_PARENT = "/org/springframework/ui/jasperreports/subReportParent.jrxml";

	protected static final boolean canCompileReport = ClassUtils.isPresent(
			"org.eclipse.jdt.internal.compiler.Compiler", AbstractJasperReportsTests.class.getClassLoader());


	protected MockHttpServletRequest request;

	protected MockHttpServletResponse response;


	@BeforeClass
	public static void assumptions() {
		Assume.canLoadNativeDirFonts();
		Assume.group(TestGroup.CUSTOM_COMPILATION);
	}

	@Before
	public void setUp() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();

		request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
		request.addPreferredLocale(Locale.GERMAN);
	}


	protected Map<String, Object> getModel() {
		Map model = new HashMap();
		model.put("ReportTitle", "Dear Lord!");
		model.put("dataSource", new JRBeanCollectionDataSource(getData()));
		extendModel(model);
		return model;
	}

	/**
	 * Subclasses can extend the model if they need to.
	 */
	protected void extendModel(Map<String, Object> model) {
	}

	protected List getData() {
		List list = new ArrayList();
		for (int x = 0; x < 10; x++) {
			PersonBean bean = new PersonBean();
			bean.setId(x);
			bean.setName("Rob Harrop");
			bean.setStreet("foo");
			list.add(bean);
		}
		return list;
	}

	protected List getProductData() {
		List list = new ArrayList();
		for (int x = 0; x < 10; x++) {
			ProductBean bean = new ProductBean();
			bean.setId(x);
			bean.setName("Foo Bar");
			bean.setPrice(1.9f);
			bean.setQuantity(1.0f);

			list.add(bean);
		}
		return list;
	}

}
