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

package org.springframework.test.jpa;

import org.springframework.instrument.classloading.ResourceOverridingShadowingClassLoader;

/**
 * Subclass of ShadowingClassLoader that overrides attempts to
 * locate {@code orm.xml}.
 *
 * <p>This class must <b>not</b> be an inner class of AbstractJpaTests
 * to avoid it being loaded until first used.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
class OrmXmlOverridingShadowingClassLoader extends ResourceOverridingShadowingClassLoader {

	/**
	 * Default location of the {@code orm.xml} file in the class path:
	 * "META-INF/orm.xml"
	 */
	public static final String DEFAULT_ORM_XML_LOCATION = "META-INF/orm.xml";


	public OrmXmlOverridingShadowingClassLoader(ClassLoader loader, String realOrmXmlLocation) {
		super(loader);

		// Automatically exclude classes from these well-known persistence providers.
		// Do NOT exclude Hibernate classes --
		// this causes class casts due to use of CGLIB by Hibernate.
		// Same goes for OpenJPA which will not enhance the domain classes.
		excludePackage("oracle.toplink.essentials");
		excludePackage("junit");

		override(DEFAULT_ORM_XML_LOCATION, realOrmXmlLocation);
	}

}
