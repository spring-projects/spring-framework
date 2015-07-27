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

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.util.Assert;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;

/**
 * {@code WebConnectionHtmlUnitDriver} enables configuration of the
 * {@link WebConnection} for an {@link HtmlUnitDriver} instance.
 *
 * <p>This is useful because it allows a
 * {@link org.springframework.test.web.servlet.htmlunit.MockMvcWebConnection
 * MockMvcWebConnection} to be injected.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 * @see MockMvcHtmlUnitDriverBuilder
 */
public class WebConnectionHtmlUnitDriver extends HtmlUnitDriver {

	private WebClient webClient;


	public WebConnectionHtmlUnitDriver(BrowserVersion browserVersion) {
		super(browserVersion);
	}

	public WebConnectionHtmlUnitDriver() {
	}

	public WebConnectionHtmlUnitDriver(boolean enableJavascript) {
		super(enableJavascript);
	}

	public WebConnectionHtmlUnitDriver(Capabilities capabilities) {
		super(capabilities);
	}

	/**
	 * Modify the supplied {@link WebClient}, {@linkplain #configureWebClient
	 * configure} it, and retain a reference to it so that its {@link WebConnection}
	 * is {@linkplain #getWebConnection accessible} for later use.
	 * @param webClient the client to modify
	 * @return the modified client
	 * @see org.openqa.selenium.htmlunit.HtmlUnitDriver#modifyWebClient(WebClient)
	 */
	@Override
	protected final WebClient modifyWebClient(WebClient webClient) {
		this.webClient = super.modifyWebClient(webClient);
		this.webClient = configureWebClient(this.webClient);
		return this.webClient;
	}

	/**
	 * Configure the supplied {@link WebClient}.
	 * <p>The default implementation simply returns the supplied client
	 * unmodified.
	 * <p>Subclasses can override this method to customize the {@code WebClient}
	 * that the {@link HtmlUnitDriver} driver uses.
	 * @param client the client to configure
	 * @return the configured client
	 */
	protected WebClient configureWebClient(WebClient client) {
		return client;
	}

	/**
	 * Access the current {@link WebConnection} for the {@link WebClient}.
	 * @return the current {@code WebConnection}
	 */
	public WebConnection getWebConnection() {
		return this.webClient.getWebConnection();
	}

	/**
	 * Set the {@link WebConnection} to be used with the {@link WebClient}.
	 * @param webConnection the {@code WebConnection} to use; never {@code null}
	 */
	public void setWebConnection(WebConnection webConnection) {
		Assert.notNull(webConnection, "WebConnection must not be null");
		this.webClient.setWebConnection(webConnection);
	}

}