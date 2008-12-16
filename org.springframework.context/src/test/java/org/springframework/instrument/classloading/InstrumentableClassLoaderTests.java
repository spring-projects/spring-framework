/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.instrument.classloading;

import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.springframework.util.ClassUtils;

/**
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class InstrumentableClassLoaderTests {

	@Test
	public void testDefaultLoadTimeWeaver() {
		ClassLoader loader = new SimpleInstrumentableClassLoader(ClassUtils.getDefaultClassLoader());
		ReflectiveLoadTimeWeaver handler = new ReflectiveLoadTimeWeaver(loader);
		assertSame(loader, handler.getInstrumentableClassLoader());
	}

}
