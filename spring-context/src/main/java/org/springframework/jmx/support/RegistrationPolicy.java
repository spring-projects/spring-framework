/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jmx.support;

/**
 * Indicates registration behavior when attempting to register an MBean that already
 * exists.
 *
 * @author Phillip Webb
 * @author Chris Beams
 * @since 3.2
 */
public enum RegistrationPolicy {

	/**
	 * Registration should fail when attempting to register an MBean under a name that
	 * already exists.
	 */
	FAIL_ON_EXISTING,

	/**
	 * Registration should ignore the affected MBean when attempting to register an MBean
	 * under a name that already exists.
	 */
	IGNORE_EXISTING,

	/**
	 * Registration should replace the affected MBean when attempting to register an MBean
	 * under a name that already exists.
	 */
	REPLACE_EXISTING;

	/**
	 * Translate from an {@link MBeanRegistrationSupport} registration behavior constant
	 * to a {@link RegistrationPolicy} enum value.
	 * @param registrationBehavior one of the now-deprecated REGISTRATION_* constants
	 * available in {@link MBeanRegistrationSupport}.
	 */
	@SuppressWarnings("deprecation")
	static RegistrationPolicy valueOf(int registrationBehavior) {
		switch (registrationBehavior) {
			case MBeanRegistrationSupport.REGISTRATION_IGNORE_EXISTING:
				return RegistrationPolicy.IGNORE_EXISTING;
			case MBeanRegistrationSupport.REGISTRATION_REPLACE_EXISTING:
				return RegistrationPolicy.REPLACE_EXISTING;
			case MBeanRegistrationSupport.REGISTRATION_FAIL_ON_EXISTING:
				return RegistrationPolicy.FAIL_ON_EXISTING;
		}
		throw new IllegalArgumentException(
				"Unknown MBean registration behavior: " + registrationBehavior);
	}

}
