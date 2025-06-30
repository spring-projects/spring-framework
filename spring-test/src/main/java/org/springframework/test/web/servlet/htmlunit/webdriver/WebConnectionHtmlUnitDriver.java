/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.servlet.htmlunit.webdriver;

import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.htmlunit.WebConnection;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import org.springframework.util.Assert;

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
 * @author Juergen Hoeller
 * @since 4.2
 * @see MockMvcHtmlUnitDriverBuilder
 */
public class WebConnectionHtmlUnitDriver extends HtmlUnitDriver {

	public WebConnectionHtmlUnitDriver() {
	}

	public WebConnectionHtmlUnitDriver(BrowserVersion browserVersion) {
		super(browserVersion);
	}

	public WebConnectionHtmlUnitDriver(boolean enableJavascript) {
		super(enableJavascript);
	}

	public WebConnectionHtmlUnitDriver(Capabilities capabilities) {
		super(capabilities);
	}


	/**
	 * Modify the supplied {@link WebClient} and retain a reference to it so that its
	 * {@link WebConnection} is {@linkplain #getWebConnection accessible} for later use.
	 * <p>Delegates to {@link HtmlUnitDriver#modifyWebClient} for default behavior
	 * and to {@link #modifyWebClientInternal} for further customization.
	 * @param webClient the client to modify
	 * @return the modified client
	 * @see HtmlUnitDriver#modifyWebClient(WebClient)
	 * @see #modifyWebClientInternal(WebClient)
	 */
	@Override
	protected final WebClient modifyWebClient(WebClient webClient) {
		return modifyWebClientInternal(super.modifyWebClient(webClient));
	}

	/**
	 * Modify the supplied {@link WebClient}.
	 * <p>The default implementation simply returns the supplied client unmodified.
	 * <p>Subclasses can override this method to customize the {@code WebClient}
	 * that the {@link HtmlUnitDriver} uses.
	 * @param webClient the client to modify
	 * @return the modified client
	 */
	protected WebClient modifyWebClientInternal(WebClient webClient) {
		return webClient;
	}

	/**
	 * Return the current {@link WebClient} in a public fashion.
	 * @since 4.3
	 */
	@Override
	public WebClient getWebClient() {
		return super.getWebClient();
	}

	/**
	 * Set the {@link WebConnection} to be used with the {@link WebClient}.
	 * @param webConnection the {@code WebConnection} to use
	 */
	public void setWebConnection(WebConnection webConnection) {
		Assert.notNull(webConnection, "WebConnection must not be null");
		getWebClient().setWebConnection(webConnection);
	}

	/**
	 * Access the current {@link WebConnection} for the {@link WebClient}.
	 * @return the current {@code WebConnection}
	 */
	public WebConnection getWebConnection() {
		return getWebClient().getWebConnection();
	}

}
