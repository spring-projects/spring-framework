/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.resttestclient.contextconfig;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;


@SpringJUnitConfig(WebConfig.class) // Specify the configuration to load
public class RestClientContextTests {

	RestTestClient client;

	@BeforeEach
	void setUp(WebApplicationContext context) {  // Inject the configuration
		// Create the `RestTestClient`
		client = RestTestClient.bindToApplicationContext(context).build();
	}

}
