/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.view.jasperreports;

import org.junit.Test;

import org.springframework.context.support.StaticApplicationContext;

/**
 * @author Rob Harrop
 */
public abstract class AbstractConfigurableJasperReportsViewTests extends AbstractJasperReportsViewTests {

	@Test(expected = IllegalArgumentException.class)
	public void noConfiguredExporter() throws Exception {
		ConfigurableJasperReportsView view = new ConfigurableJasperReportsView();
		view.setUrl(COMPILED_REPORT);
		// Should not be able to set up view class without an exporter class.
		view.setApplicationContext(new StaticApplicationContext());
	}

}
