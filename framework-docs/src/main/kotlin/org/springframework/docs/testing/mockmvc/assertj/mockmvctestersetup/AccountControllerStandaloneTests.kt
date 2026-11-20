/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.mockmvc.assertj.mockmvctestersetup

import org.springframework.test.web.servlet.assertj.MockMvcTester

// tag::snippet[]
class AccountControllerStandaloneTests {

	val mockMvc = MockMvcTester.of(AccountController())

	// ...

}
// end::snippet[]