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

import java.util.Map;
import java.util.Properties;

/**
 * @author Rob Harrop
 */
public class JasperReportsMultiFormatViewWithCustomMappingsTests extends JasperReportsMultiFormatViewTests {

	protected AbstractJasperReportsView getViewImplementation() {
		JasperReportsMultiFormatView view = new JasperReportsMultiFormatView();
		view.setFormatKey("fmt");

		Properties props = new Properties();
		props.setProperty("csv", JasperReportsCsvView.class.getName());
		props.setProperty("comma-separated", JasperReportsCsvView.class.getName());
		props.setProperty("html", JasperReportsHtmlView.class.getName());

		view.setFormatMappings(props);
		return view;
	}

	protected String getDiscriminatorKey() {
		return "fmt";
	}

	protected void extendModel(Map model) {
		model.put(getDiscriminatorKey(), "comma-separated");
	}

}
