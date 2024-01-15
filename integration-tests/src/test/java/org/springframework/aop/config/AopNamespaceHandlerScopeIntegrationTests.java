/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aop.config;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for scoped proxy use in conjunction with aop: namespace.
 * Deemed an integration test because web mocks and application contexts are required.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see org.springframework.aop.config.AopNamespaceHandlerTests
 */
@SpringJUnitWebConfig
class AopNamespaceHandlerScopeIntegrationTests {

	@Autowired
	ITestBean singletonScoped;

	@Autowired
	ITestBean requestScoped;

	@Autowired
	ITestBean sessionScoped;

	@Autowired
	ITestBean sessionScopedAlias;

	@Autowired
	ITestBean testBean;


	@Test
	void testSingletonScoping() throws Exception {
		assertThat(AopUtils.isAopProxy(singletonScoped)).as("Should be AOP proxy").isTrue();
		boolean condition = singletonScoped instanceof TestBean;
		assertThat(condition).as("Should be target class proxy").isTrue();
		String rob = "Rob Harrop";
		String bram = "Bram Smeets";
		assertThat(singletonScoped.getName()).isEqualTo(rob);
		singletonScoped.setName(bram);
		assertThat(singletonScoped.getName()).isEqualTo(bram);
		ITestBean deserialized = SerializationTestUtils.serializeAndDeserialize(singletonScoped);
		assertThat(deserialized.getName()).isEqualTo(bram);
	}

	@Test
	void testRequestScoping() {
		MockHttpServletRequest oldRequest = new MockHttpServletRequest();
		MockHttpServletRequest newRequest = new MockHttpServletRequest();

		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(oldRequest));

		assertThat(AopUtils.isAopProxy(requestScoped)).as("Should be AOP proxy").isTrue();
		boolean condition = requestScoped instanceof TestBean;
		assertThat(condition).as("Should be target class proxy").isTrue();

		assertThat(AopUtils.isAopProxy(testBean)).as("Should be AOP proxy").isTrue();
		boolean condition1 = testBean instanceof TestBean;
		assertThat(condition1).as("Regular bean should be JDK proxy").isFalse();

		String rob = "Rob Harrop";
		String bram = "Bram Smeets";

		assertThat(requestScoped.getName()).isEqualTo(rob);
		requestScoped.setName(bram);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(newRequest));
		assertThat(requestScoped.getName()).isEqualTo(rob);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(oldRequest));
		assertThat(requestScoped.getName()).isEqualTo(bram);

		assertThat(((Advised) requestScoped).getAdvisors()).as("Should have advisors").isNotEmpty();
	}

	@Test
	void testSessionScoping() {
		MockHttpSession oldSession = new MockHttpSession();
		MockHttpSession newSession = new MockHttpSession();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setSession(oldSession);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		assertThat(AopUtils.isAopProxy(sessionScoped)).as("Should be AOP proxy").isTrue();
		boolean condition1 = sessionScoped instanceof TestBean;
		assertThat(condition1).as("Should not be target class proxy").isFalse();

		assertThat(sessionScopedAlias).isSameAs(sessionScoped);

		assertThat(AopUtils.isAopProxy(testBean)).as("Should be AOP proxy").isTrue();
		boolean condition = testBean instanceof TestBean;
		assertThat(condition).as("Regular bean should be JDK proxy").isFalse();

		String rob = "Rob Harrop";
		String bram = "Bram Smeets";

		assertThat(sessionScoped.getName()).isEqualTo(rob);
		sessionScoped.setName(bram);
		request.setSession(newSession);
		assertThat(sessionScoped.getName()).isEqualTo(rob);
		request.setSession(oldSession);
		assertThat(sessionScoped.getName()).isEqualTo(bram);

		assertThat(((Advised) sessionScoped).getAdvisors()).as("Should have advisors").isNotEmpty();
	}

}
