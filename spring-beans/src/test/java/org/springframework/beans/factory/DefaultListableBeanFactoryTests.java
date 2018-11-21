/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.beans.factory;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.lang.Nullable;

/**
 * Tests properties population and autowire behavior.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Sam Brannen
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class DefaultListableBeanFactoryTests extends AbstractDefaultListableBeanFactoryTests {

	public DefaultListableBeanFactory getInstance() {
		return new DefaultListableBeanFactory();
	}

	public DefaultListableBeanFactory getInstance(@Nullable BeanFactory parentBeanFactory) {
		return new DefaultListableBeanFactory(parentBeanFactory);
	}
}
