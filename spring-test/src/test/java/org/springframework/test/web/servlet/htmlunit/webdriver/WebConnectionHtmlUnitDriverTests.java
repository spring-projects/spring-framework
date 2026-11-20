/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.htmlunit.webdriver;

import java.io.IOException;

import org.htmlunit.WebConnection;
import org.htmlunit.WebRequest;
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
 * Tests for {@link WebConnectionHtmlUnitDriver}.
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
	void setWebConnection() {
		this.driver.setWebConnection(this.connection);
		assertThat(this.driver.getWebConnection()).isEqualTo(this.connection);
		assertThatExceptionOfType(WebDriverException.class).isThrownBy(() -> this.driver.get("https://example.com"));
	}

}
