/*
 * Copyright 2007 the original author or authors.
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

package org.springframework.test;

import org.springframework.beans.Pet;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;

/**
 * Abstract JUnit 3.8 based unit test which verifies new functionality requested
 * in <a href="http://opensource.atlassian.com/projects/spring/browse/SPR-3550"
 * target="_blank">SPR-3350</a>.
 *
 * @author Sam Brannen
 * @since 2.5
 */
public abstract class AbstractSpr3350SingleSpringContextTests extends AbstractDependencyInjectionSpringContextTests {

	private Pet cat;


	public AbstractSpr3350SingleSpringContextTests() {
		super();
	}

	public AbstractSpr3350SingleSpringContextTests(String name) {
		super(name);
	}

	public final void setCat(final Pet cat) {
		this.cat = cat;
	}

	/**
	 * Forcing concrete subclasses to provide a config path appropriate to the
	 * configured
	 * {@link #createBeanDefinitionReader(org.springframework.context.support.GenericApplicationContext) BeanDefinitionReader}.
	 *
	 * @see org.springframework.test.AbstractSingleSpringContextTests#getConfigPath()
	 */
	protected abstract String getConfigPath();

	/**
	 * <p>
	 * Test which addresses the following issue raised in SPR-3350:
	 * </p>
	 * <p>
	 * {@link AbstractSingleSpringContextTests} always uses an
	 * {@link XmlBeanDefinitionReader} internally when creating the
	 * {@link ApplicationContext} inside
	 * {@link #createApplicationContext(String[])}. It would be nice to have
	 * the bean definition reader creation in a separate method so that
	 * subclasses can choose that individually without having to copy-n-paste
	 * code from createApplicationContext() to do the context creation and
	 * refresh. Consider JavaConfig where an Annotation based reader can be
	 * plugged in.
	 * </p>
	 */
	public final void testApplicationContextNotAutoCreated() {
		assertNotNull("The cat field should have been autowired.", this.cat);
		assertEquals("Garfield", this.cat.getName());
	}
}
