/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.mockmvc.assertj.mockmvctestersetup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.context.WebApplicationContext;

// tag::snippet[]
@SpringJUnitWebConfig(ApplicationWebConfiguration.class)
class AccountControllerIntegrationTests {

	private final MockMvcTester mockMvc;

	AccountControllerIntegrationTests(@Autowired WebApplicationContext wac) {
		this.mockMvc = MockMvcTester.from(wac);
	}

	// ...

}
// end::snippet[]
