/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.test.web.servlet.htmlunit.webdriver;

import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebConnectionBuilderSupport;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.WebApplicationContext;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;

/**
 * Convenience class for building an {@link HtmlUnitDriver} that delegates
 * to {@link MockMvc} and optionally delegates to an actual connection for
 * specific requests.
 *
 * <p>By default, the driver will delegate to {@code MockMvc} to handle
 * requests to {@code localhost} and to a {@link WebClient} to handle any
 * other URL (i.e. to perform an actual HTTP request).
 *
 * @author Rob Winch
 * @author Sam Brannen
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
	 * Create a new instance using the supplied {@link WebApplicationContext}.
	 * @param context the WebApplicationContext to use; never {@code null}
	 * @return the MockMvcHtmlUnitDriverBuilder to customize
	 */
	public static MockMvcHtmlUnitDriverBuilder webAppContextSetup(WebApplicationContext context) {
		return new MockMvcHtmlUnitDriverBuilder(context);
	}

	/**
	 * Create a new instance using the supplied {@link WebApplicationContext}
	 * and {@link MockMvcConfigurer}.
	 * @param context the WebApplicationContext to create a MockMvc instance from;
	 * never {@code null}
	 * @param configurer the MockMvcConfigurer to apply; never {@code null}
	 * @return the MockMvcHtmlUnitDriverBuilder to customize
	 */
	public static MockMvcHtmlUnitDriverBuilder webAppContextSetup(WebApplicationContext context,
			MockMvcConfigurer configurer) {
		return new MockMvcHtmlUnitDriverBuilder(context, configurer);
	}

	/**
	 * Create a new instance using the supplied {@link MockMvc} instance.
	 * @param mockMvc the MockMvc instance to use; never {@code null}
	 * @return the MockMvcHtmlUnitDriverBuilder to customize
	 */
	public static MockMvcHtmlUnitDriverBuilder mockMvcSetup(MockMvc mockMvc) {
		return new MockMvcHtmlUnitDriverBuilder(mockMvc);
	}

	/**
	 * Specify whether JavaScript should be enabled.
	 * <p>Default is {@code true}.
	 * @param javascriptEnabled if JavaScript should be enabled or not.
	 * @return the builder for further customizations
	 */
	public MockMvcHtmlUnitDriverBuilder javascriptEnabled(boolean javascriptEnabled) {
		this.javascriptEnabled = javascriptEnabled;
		return this;
	}

	/**
	 * Create a new {@link HtmlUnitDriver} with the {@link BrowserVersion}
	 * set to {@link BrowserVersion#CHROME CHROME}.
	 * <p>For additional configuration options, use {@link #configureDriver}.
	 * @return the {@code HtmlUnitDriver} to use
	 * @see #configureDriver(WebConnectionHtmlUnitDriver)
	 */
	public HtmlUnitDriver createDriver() {
		return configureDriver(new WebConnectionHtmlUnitDriver(BrowserVersion.CHROME));
	}

	/**
	 * Configure an existing {@link WebConnectionHtmlUnitDriver}.
	 * @param driver the WebConnectionHtmlUnitDriver to configure
	 * @return the {@code HtmlUnitDriver} to use
	 */
	public HtmlUnitDriver configureDriver(WebConnectionHtmlUnitDriver driver) {
		driver.setJavascriptEnabled(javascriptEnabled);
		driver.setWebConnection(createConnection(driver.getWebConnection()));
		return driver;
	}

}