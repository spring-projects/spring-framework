/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jmx.support;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link RegistrationPolicy}.
 *
 * @author Chris Beams
 */
public class RegistrationPolicyTests {

	@Test
	@SuppressWarnings("deprecation")
	public void convertRegistrationBehaviorToRegistrationPolicy() {
		assertThat(
				RegistrationPolicy.valueOf(MBeanRegistrationSupport.REGISTRATION_FAIL_ON_EXISTING),
				equalTo(RegistrationPolicy.FAIL_ON_EXISTING));
		assertThat(
				RegistrationPolicy.valueOf(MBeanRegistrationSupport.REGISTRATION_IGNORE_EXISTING),
				equalTo(RegistrationPolicy.IGNORE_EXISTING));
		assertThat(
				RegistrationPolicy.valueOf(MBeanRegistrationSupport.REGISTRATION_REPLACE_EXISTING),
				equalTo(RegistrationPolicy.REPLACE_EXISTING));

		try {
			RegistrationPolicy.valueOf(Integer.MAX_VALUE);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			assertTrue(ex.getMessage().startsWith("Unknown MBean registration behavior"));
		}
	}

}
