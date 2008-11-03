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

package org.springframework.instrument.classloading.oc4j;

import junit.framework.TestCase;
import org.easymock.AbstractMatcher;
import org.easymock.MockControl;
import org.springframework.test.AssertThrows;

import java.lang.instrument.ClassFileTransformer;

/**
 * Unit tests for the {@link OC4JClassPreprocessorAdapter} class.
 *
 * @author Rick Evans
 */
public final class OC4JClassPreprocessorAdapterTests extends TestCase {

	public void testClassNameIsUnMangledPriorToTransformation() throws Exception {
		final byte[] classBytes = "CAFEBABE".getBytes();
		final ClassLoader classLoader = getClass().getClassLoader();

		MockControl mockTransformer = MockControl.createControl(ClassFileTransformer.class);
		ClassFileTransformer transformer = (ClassFileTransformer) mockTransformer.getMock();

		transformer.transform(classLoader, "com/foo/Bar", null, null, classBytes);
		mockTransformer.setMatcher(new AbstractMatcher() {
			public boolean matches(Object[] expected, Object[] actual) {
				return expected[1].equals(actual[1]);
			}
		});
		mockTransformer.setReturnValue(classBytes);

		mockTransformer.replay();

		OC4JClassPreprocessorAdapter processor = new OC4JClassPreprocessorAdapter(transformer);
		byte[] bytes = processor.processClass("com.foo.Bar", classBytes, 0, 0, null, classLoader);
		assertNotNull(bytes);

		mockTransformer.verify();
	}

	public void testCtorWithNullClassFileTransformer() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new OC4JClassPreprocessorAdapter(null);
			}
		}.runTest();
	}

}
