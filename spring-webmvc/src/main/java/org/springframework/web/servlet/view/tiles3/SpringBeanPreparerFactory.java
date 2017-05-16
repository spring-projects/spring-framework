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

package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.TilesException;
import org.apache.tiles.preparer.ViewPreparer;

import org.springframework.web.context.WebApplicationContext;

/**
 * Tiles {@link org.apache.tiles.preparer.PreparerFactory} implementation
 * that expects preparer bean names and obtains preparer beans from the
 * Spring ApplicationContext. The full bean creation process will be in
 * the control of the Spring application context in this case, allowing
 * for the use of scoped beans etc.
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @see SimpleSpringPreparerFactory
 */
public class SpringBeanPreparerFactory extends AbstractSpringPreparerFactory {

	@Override
	protected ViewPreparer getPreparer(String name, WebApplicationContext context) throws TilesException {
		return context.getBean(name, ViewPreparer.class);
	}

}
