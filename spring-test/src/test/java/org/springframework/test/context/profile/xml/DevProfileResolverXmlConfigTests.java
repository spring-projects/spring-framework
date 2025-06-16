/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.context.profile.xml;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ActiveProfilesResolver;

/**
 * @author Michail Nikolaev
 * @since 4.0
 */
@ActiveProfiles(resolver = DevProfileResolverXmlConfigTests.class, inheritProfiles = false)
class DevProfileResolverXmlConfigTests extends DevProfileXmlConfigTests implements ActiveProfilesResolver {

	@Override
	public String[] resolve(Class<?> testClass) {
		return new String[] { "dev" };
	}

}
