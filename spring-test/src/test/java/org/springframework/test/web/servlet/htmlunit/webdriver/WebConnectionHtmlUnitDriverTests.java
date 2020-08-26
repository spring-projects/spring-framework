/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.IOException;

import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openqa.selenium.WebDriverException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for {@link WebConnectionHtmlUnitDriver}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class WebConnectionHtmlUnitDriverTests {

	private final WebConnectionHtmlUnitDriver driver = new WebConnectionHtmlUnitDriver();

	@Mock
	private WebConnection connection;

	@BeforeEach
	void setup() throws Exception {
		given(this.connection.getResponse(any(WebRequest.class))).willThrow(new IOException(""));
	}


	@Test
	void getWebConnectionDefaultNotNull() {
		assertThat(this.driver.getWebConnection()).isNotNull();
	}

	@Test
	void setWebConnectionToNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.driver.setWebConnection(null));
	}

	@Test
	public void setWebConnection() {
		this.driver.setWebConnection(this.connection);
		assertThat(this.driver.getWebConnection()).isEqualTo(this.connection);
		assertThatExceptionOfType(WebDriverException.class).isThrownBy(() -> this.driver.get("https://example.com"));
	}

}
