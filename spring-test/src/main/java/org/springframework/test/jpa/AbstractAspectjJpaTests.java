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

import org.aspectj.weaver.loadtime.ClassPreProcessorAgentAdapter;

import org.springframework.instrument.classloading.ResourceOverridingShadowingClassLoader;

/**
 * Subclass of {@link AbstractJpaTests} that activates AspectJ load-time weaving and
 * allows for specifying a custom location for AspectJ's <code>aop.xml</code> file.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of Spring 3.0, in favor of using the listener-based test context framework
 * ({@link org.springframework.test.context.junit38.AbstractJUnit38SpringContextTests})
 */
@Deprecated
public abstract class AbstractAspectjJpaTests extends AbstractJpaTests {

	/**
	 * Default location of the <code>aop.xml</code> file in the class path:
	 * "META-INF/aop.xml"
	 */
	public static final String DEFAULT_AOP_XML_LOCATION = "META-INF/aop.xml";


	@Override
	protected void customizeResourceOverridingShadowingClassLoader(ClassLoader shadowingClassLoader) {
		ResourceOverridingShadowingClassLoader orxl = (ResourceOverridingShadowingClassLoader) shadowingClassLoader;
		orxl.override(DEFAULT_AOP_XML_LOCATION, getActualAopXmlLocation());
		orxl.addTransformer(new ClassPreProcessorAgentAdapter());
	}

	/**
	 * Return the actual location of the <code>aop.xml</code> file
	 * in the class path. The default is "META-INF/aop.xml".
	 * <p>Override this method to point to a specific <code>aop.xml</code>
	 * file within your test suite, allowing for different config files
	 * to co-exist within the same class path.
	 */
	protected String getActualAopXmlLocation() {
		return DEFAULT_AOP_XML_LOCATION;
	}

}
