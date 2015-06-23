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
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.util.Assert;

/**
 * <p>
 * Allows configuring the WebConnection for an HtmlUnitDriver instance. This is useful
 * because it allows a MockMvcWebConnection to be injected.
 * </p>
 *
 * @author Rob Winch
 * @since 4.2
 * @see MockMvcHtmlUnitDriverBuilder
 */
public class WebConnectionHtmlUnitDriver extends HtmlUnitDriver {
	private WebClient webClient;

	public WebConnectionHtmlUnitDriver(BrowserVersion version) {
		super(version);
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
	 * Captures the WebClient that is used so that its WebConnection is accessible.
	 *
	 * @param client The client to modify
	 * @return The modified client
	 */
	@Override
	protected final WebClient modifyWebClient(WebClient client) {
		webClient = super.modifyWebClient(client);
		webClient = configureWebClient(webClient);
		return webClient;
	}

	/**
	 * Subclasses can override this method to customise the WebClient that the HtmlUnit
	 * driver uses.
	 *
	 * @param client The client to modify
	 * @return The modified client
	 */
	protected WebClient configureWebClient(WebClient client) {
		return client;
	}

	/**
	 * Allows accessing the current WebConnection
	 *
	 * @return the current WebConnection
	 */
	public WebConnection getWebConnection() {
		return webClient.getWebConnection();
	}

	/**
	 * Sets the WebConnection to be used.
	 *
	 * @param webConnection the WebConnection to use. Cannot be null.
	 */
	public void setWebConnection(WebConnection webConnection) {
		Assert.notNull(webConnection, "webConnection cannot be null");
		this.webClient.setWebConnection(webConnection);
	}
}