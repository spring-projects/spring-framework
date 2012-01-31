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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.junit.Test;

/**
 * Unit tests for the {@link OC4JClassPreprocessorAdapter} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public final class OC4JClassPreprocessorAdapterTests {

	@Test
	public void testClassNameIsUnMangledPriorToTransformation() throws IllegalClassFormatException {
		final byte[] classBytes = "CAFEBABE".getBytes();
		final ClassLoader classLoader = getClass().getClassLoader();

		ClassFileTransformer transformer = createMock(ClassFileTransformer.class);

		expect(
				transformer.transform(eq(classLoader), eq("com/foo/Bar"), (Class<?>)isNull(), (ProtectionDomain)isNull(), isA(byte[].class))
			).andReturn(classBytes);
		replay(transformer);

		OC4JClassPreprocessorAdapter processor = new OC4JClassPreprocessorAdapter(transformer);
		byte[] bytes = processor.processClass("com.foo.Bar", classBytes, 0, 0, null, classLoader);
		assertNotNull(bytes);

		verify(transformer);
	}
}