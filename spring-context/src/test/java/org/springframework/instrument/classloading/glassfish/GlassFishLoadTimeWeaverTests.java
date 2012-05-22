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

package org.springframework.instrument.classloading.glassfish;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.spi.ClassTransformer;

import org.easymock.ArgumentsMatcher;
import org.easymock.MockControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.instrument.classloading.LoadTimeWeaver;

// converting away from old-style EasyMock APIs was problematic with this class
@SuppressWarnings("deprecation")
@Ignore
public class GlassFishLoadTimeWeaverTests {

	private MockControl loaderCtrl;
	private GlassFishClassLoaderAdapter loader;
	private LoadTimeWeaver ltw;

	public class DummyInstrumentableClassLoader extends SecureClassLoader {

		String INSTR_CL_NAME = GlassFishClassLoaderAdapter.INSTRUMENTABLE_CLASSLOADER_GLASSFISH_V2;

		public DummyInstrumentableClassLoader() {
			super();
		}

		public DummyInstrumentableClassLoader(ClassLoader parent) {
			super(parent);
		}

		private List<ClassTransformer> v2Transformers = new ArrayList<ClassTransformer>();
		private List<ClassFileTransformer> v3Transformers = new ArrayList<ClassFileTransformer>();

		public void addTransformer(ClassTransformer transformer) {
			v2Transformers.add(transformer);
		}

		public void addTransformer(ClassFileTransformer transformer) {
			v3Transformers.add(transformer);
		}

		public ClassLoader copy() {
			return new DummyInstrumentableClassLoader();
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			if (INSTR_CL_NAME.equals(name)) {
				return this.getClass();
			}

			return getClass().getClassLoader().loadClass(name);
		}
	}

	@Before
	public void setUp() throws Exception {
		ltw = new GlassFishLoadTimeWeaver(new DummyInstrumentableClassLoader());
	}

	@After
	public void tearDown() throws Exception {
		loaderCtrl.verify();
		ltw = null;
	}

	@Test
	public void testGlassFishLoadTimeWeaver() {
		try {
			ltw = new GlassFishLoadTimeWeaver();
			fail("expected exception");
		} catch (IllegalArgumentException ex) {
			// expected
		}

	}

	@Test
	public void testGlassFishLoadTimeWeaverClassLoader() {
		try {
			ltw = new GlassFishLoadTimeWeaver(null);
			fail("expected exception");
		} catch (RuntimeException e) {
			// expected
		}

		ClassLoader cl1 = new URLClassLoader(new URL[0]);
		ClassLoader cl2 = new URLClassLoader(new URL[0], cl1);
		ClassLoader cl3 = new DummyInstrumentableClassLoader(cl2);
		ClassLoader cl4 = new URLClassLoader(new URL[0], cl3);

		ltw = new GlassFishLoadTimeWeaver(cl4);
		assertSame(cl3, ltw.getInstrumentableClassLoader());

		cl1 = new URLClassLoader(new URL[0]);
		cl2 = new URLClassLoader(new URL[0], cl1);
		cl3 = new DummyInstrumentableClassLoader(cl2);
		cl4 = new DummyInstrumentableClassLoader(cl3);

		ltw = new GlassFishLoadTimeWeaver(cl4);
		assertSame(cl4, ltw.getInstrumentableClassLoader());
	}

	@Test
	public void testAddTransformer() {
		ClassFileTransformer transformer = MockControl.createNiceControl(ClassFileTransformer.class).getMock();
		loaderCtrl.reset();
		loader.addTransformer(transformer);
		loaderCtrl.setMatcher(new ArgumentsMatcher() {

			public boolean matches(Object[] arg0, Object[] arg1) {
				for (int i = 0; i < arg0.length; i++) {
					if (arg0 != null && arg0.getClass() != arg1.getClass())
						return false;
				}
				return true;
			}

			public String toString(Object[] arg0) {
				return Arrays.toString(arg0);
			}

		});

		loaderCtrl.replay();

		ltw.addTransformer(transformer);
	}

	@Test
	public void testGetThrowawayClassLoader() {
		loaderCtrl.reset();
		ClassLoader cl = new URLClassLoader(new URL[0]);
		loaderCtrl.expectAndReturn(loader.getClassLoader(), cl);
		loaderCtrl.replay();

		assertSame(ltw.getThrowawayClassLoader(), cl);
	}
}
