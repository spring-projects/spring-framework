/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.test.web.servlet.htmlunit.webdriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebConnectionBuilderSupport;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.WebApplicationContext;

/**
 * Convenience class for building an HtmlUnitDriver that will delegate to MockMvc and
 * optionally delegate to an actual connection for specific requests.
 *
 * By default localhost will delegate to MockMvc and any other URL will delegate
 *
 * @author Rob Winch
 * @since 4.2
 */
public class MockMvcHtmlUnitDriverBuilder extends MockMvcWebConnectionBuilderSupport<MockMvcHtmlUnitDriverBuilder> {
	private boolean javascriptEnabled = true;

	protected MockMvcHtmlUnitDriverBuilder(MockMvc mockMvc) {
		super(mockMvc);
	}

	protected MockMvcHtmlUnitDriverBuilder(WebApplicationContext context) {
		super(context);
	}

	protected MockMvcHtmlUnitDriverBuilder(WebApplicationContext context, MockMvcConfigurer configurer) {
		super(context, configurer);
	}

	/**
	 * Creates a new instance with a WebApplicationContext.
	 *
	 * @param context the WebApplicationContext to use. Cannot be null.
	 * @return the MockMvcHtmlUnitDriverBuilder to customize
	 */
	public static MockMvcHtmlUnitDriverBuilder webAppContextSetup(WebApplicationContext context) {
		return new MockMvcHtmlUnitDriverBuilder(context);
	}

	/**
	 * Creates a new instance using a WebApplicationContext
	 * @param context the WebApplicationContext to create a MockMvc instance from.
	 * @param configurer the MockMvcConfigurer to apply
	 * Cannot be null.
	 * @return the MockMvcHtmlUnitDriverBuilder to customize
	 */
	public static MockMvcHtmlUnitDriverBuilder webAppContextSetup(WebApplicationContext context, MockMvcConfigurer configurer) {
		return new MockMvcHtmlUnitDriverBuilder(context, configurer);
	}

	/**
	 * Creates a new instance with a MockMvc instance.
	 *
	 * @param mockMvc the MockMvc to use. Cannot be null.
	 * @return the MockMvcHtmlUnitDriverBuilder to customize
	 */
	public static MockMvcHtmlUnitDriverBuilder mockMvcSetup(MockMvc mockMvc) {
		return new MockMvcHtmlUnitDriverBuilder(mockMvc);
	}

	/**
	 * Specifies if JavaScript should be enabled or not. Default is true.
	 *
	 * @param javascriptEnabled if JavaScript should be enabled or not.
	 * @return the builder for further customizations
	 */
	public MockMvcHtmlUnitDriverBuilder javascriptEnabled(boolean javascriptEnabled) {
		this.javascriptEnabled = javascriptEnabled;
		return this;
	}

	/**
	 * Creates a new HtmlUnitDriver with the BrowserVersion set to CHROME. For additional
	 * configuration options, use configureDriver.
	 *
	 * @return the HtmlUnitDriver to use
	 * @see #configureDriver(WebConnectionHtmlUnitDriver)
	 */
	public HtmlUnitDriver createDriver() {
		return configureDriver(new WebConnectionHtmlUnitDriver(BrowserVersion.CHROME));
	}

	/**
	 * Configures an existing WebConnectionHtmlUnitDriver.
	 *
	 * @param driver the WebConnectionHtmlUnitDriver to configure
	 * @return the HtmlUnitDriver to use
	 */
	public HtmlUnitDriver configureDriver(WebConnectionHtmlUnitDriver driver) {
		driver.setJavascriptEnabled(javascriptEnabled);
		driver.setWebConnection(createConnection(driver.getWebConnection()));
		return driver;
	}
}