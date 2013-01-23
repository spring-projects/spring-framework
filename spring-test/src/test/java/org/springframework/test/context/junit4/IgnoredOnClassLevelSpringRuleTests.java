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

import static org.junit.Assert.fail;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.TestExecutionListeners;


/**
 * Same as {@link EnabledAndIgnoredSpringRuleTests} but on a class level.
 *
 * @author Philippe Marschall
 * @since 3.2.2
 * @see EnabledAndIgnoredSpringRuleTests
 */
@TestExecutionListeners( {})
@IfProfileValue(name = EnabledAndIgnoredSpringRuleTests.NAME,
	value = EnabledAndIgnoredSpringRuleTests.VALUE + "X")
public class IgnoredOnClassLevelSpringRuleTests {
	
	@ClassRule
	public static final SpringJUnitClassRule CLASS_RULE = new SpringJUnitClassRule();
	
	@Rule
	public MethodRule methodRule = new SpringJUnitMethodRule(CLASS_RULE);
	
	@Test
	public void testIfProfileValueDisabled() {
		fail("The body of a disabled test should never be executed!");
	}

}
