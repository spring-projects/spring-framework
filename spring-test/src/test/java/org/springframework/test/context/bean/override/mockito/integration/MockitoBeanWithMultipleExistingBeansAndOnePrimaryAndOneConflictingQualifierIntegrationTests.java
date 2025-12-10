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

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.BDDMockito.then;
import static org.springframework.test.mockito.MockitoAssertions.assertIsMock;
import static org.springframework.test.mockito.MockitoAssertions.assertIsNotMock;

/**
 * Tests that {@link MockitoBean @MockitoBean} can be used to mock a bean when
 * there are multiple candidates; one is primary; and the field name matches
 * the name of a candidate which is not the primary candidate.
 *
 * @author Sam Brannen
 * @since 6.2.3
 * @see MockitoBeanWithMultipleExistingBeansAndOnePrimaryIntegrationTests
 * @see MockitoBeanWithMultipleExistingBeansAndExplicitBeanNameIntegrationTests
 * @see MockitoBeanWithMultipleExistingBeansAndExplicitQualifierIntegrationTests
 */
@ExtendWith(SpringExtension.class)
class MockitoBeanWithMultipleExistingBeansAndOnePrimaryAndOneConflictingQualifierIntegrationTests {

	// The name of this field must be "baseService" to match the name of the non-primary candidate.
	@MockitoBean
	BaseService baseService;

	@Autowired
	Client client;


	@Test  // gh-34374
	void test(ApplicationContext context) {
		assertIsMock(baseService, "baseService field");
		assertIsMock(context.getBean("extendedService"), "extendedService bean");
		assertIsNotMock(context.getBean("baseService"), "baseService bean");

		client.callService();

		then(baseService).should().doSomething();
	}


	@Configuration(proxyBeanMethods = false)
	@Import({ BaseService.class, ExtendedService.class, Client.class })
	static class Config {
	}

	@Component("baseService")
	static class BaseService {

		public void doSomething() {
		}
	}

	@Primary
	@Component("extendedService")
	static class ExtendedService extends BaseService {
	}

	@Component("client")
	static class Client {

		private final BaseService baseService;

		public Client(BaseService baseService) {
			this.baseService = baseService;
		}

		public void callService() {
			this.baseService.doSomething();
		}
	}

}
