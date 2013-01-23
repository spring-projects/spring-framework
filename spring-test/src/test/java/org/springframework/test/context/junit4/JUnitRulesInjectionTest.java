/*
 * Copyright 2004-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit4;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.tests.sample.beans.Pet;


/**
 * Verifies that injection works correctly for rule based JUnit 4 tests.
 * 
 * @author Philippe Marschall
 * @since 3.2.2
 */
@ContextConfiguration
@RunWith(Parameterized.class)
public class JUnitRulesInjectionTest {
	
	@ClassRule
	public static final SpringJUnitClassRule CLASS_RULE = new SpringJUnitClassRule();
	
	@Rule
	public MethodRule methodRule = new SpringJUnitMethodRule(CLASS_RULE);
	
	private String parameter;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private Pet pet;

	public JUnitRulesInjectionTest(final String parameter) {
		this.parameter = parameter;
	}
	
	@Test
	public void firstMethod() {
		assertNotNull("application context hasn't been injected", this.applicationContext);
		assertNotNull("pet hasn't been injected", this.pet);
		assertNotNull("parameter has not been set", this.parameter);
	}
	
	@Test
	public void secondMethod() {
		assertNotNull("application context hasn't been injected", this.applicationContext);
		assertNotNull("pet hasn't been injected", this.pet);
		assertNotNull("parameter has not been set", this.parameter);
	}
	
	@Parameters
	public static Collection<Object[]> testData() {
		return Arrays.asList(new Object[][] {//
				//
				{ "first" },//
				{ "second" } //
		});
	}
	
}
