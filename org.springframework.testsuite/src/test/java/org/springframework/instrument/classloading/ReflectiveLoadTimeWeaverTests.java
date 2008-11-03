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

import junit.framework.TestCase;
import org.springframework.test.AssertThrows;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * Unit tests for the {@link ReflectiveLoadTimeWeaver} class.
 *
 * @author Rick Evans
 */
public final class ReflectiveLoadTimeWeaverTests extends TestCase {

	public void testCtorWithNullClassLoader() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new ReflectiveLoadTimeWeaver(null);
			}
		}.runTest();
	}

	public void testCtorWithClassLoaderThatDoesNotExposeAnAddTransformerMethod() throws Exception {
		new AssertThrows(IllegalStateException.class) {
			public void test() throws Exception {
				new ReflectiveLoadTimeWeaver(getClass().getClassLoader());
			}
		}.runTest();
	}

	public void testCtorWithClassLoaderThatDoesNotExposeAGetThrowawayClassLoaderMethodIsOkay() throws Exception {
		JustAddTransformerClassLoader classLoader = new JustAddTransformerClassLoader();
		ReflectiveLoadTimeWeaver weaver = new ReflectiveLoadTimeWeaver(classLoader);
		weaver.addTransformer(new ClassFileTransformer() {
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
				return "CAFEDEAD".getBytes();
			}
		});
		assertEquals(1, classLoader.getNumTimesGetThrowawayClassLoaderCalled());
	}

	public void testAddTransformerWithNullTransformer() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new ReflectiveLoadTimeWeaver(new JustAddTransformerClassLoader()).addTransformer(null);
			}
		}.runTest();
	}

	public void testGetThrowawayClassLoaderWithClassLoaderThatDoesNotExposeAGetThrowawayClassLoaderMethodYieldsFallbackClassLoader() throws Exception {
		ReflectiveLoadTimeWeaver weaver = new ReflectiveLoadTimeWeaver(new JustAddTransformerClassLoader());
		ClassLoader throwawayClassLoader = weaver.getThrowawayClassLoader();
		assertNotNull(throwawayClassLoader);
	}

	public void testGetThrowawayClassLoaderWithTotallyCompliantClassLoader() throws Exception {
		TotallyCompliantClassLoader classLoader = new TotallyCompliantClassLoader();
		ReflectiveLoadTimeWeaver weaver = new ReflectiveLoadTimeWeaver(classLoader);
		ClassLoader throwawayClassLoader = weaver.getThrowawayClassLoader();
		assertNotNull(throwawayClassLoader);
		assertEquals(1, classLoader.getNumTimesGetThrowawayClassLoaderCalled());
	}


	public static class JustAddTransformerClassLoader extends ClassLoader {

		private int numTimesAddTransformerCalled = 0;


		public int getNumTimesGetThrowawayClassLoaderCalled() {
			return this.numTimesAddTransformerCalled;
		}


		public void addTransformer(ClassFileTransformer transformer) {
			++this.numTimesAddTransformerCalled;
		}

	}


	public static final class TotallyCompliantClassLoader extends JustAddTransformerClassLoader {

		private int numTimesGetThrowawayClassLoaderCalled = 0;


		public int getNumTimesGetThrowawayClassLoaderCalled() {
			return this.numTimesGetThrowawayClassLoaderCalled;
		}


		public ClassLoader getThrowawayClassLoader() {
			++this.numTimesGetThrowawayClassLoaderCalled;
			return getClass().getClassLoader();
		}

	}

}
