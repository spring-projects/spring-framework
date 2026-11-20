/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
