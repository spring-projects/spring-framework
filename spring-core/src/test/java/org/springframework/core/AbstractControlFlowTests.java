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

package org.springframework.core;

import junit.framework.TestCase;

/**
 * @author Rod Johnson
 */
public abstract class AbstractControlFlowTests extends TestCase {

	protected abstract ControlFlow createControlFlow();

	/*
	 * Class to test for boolean under(Class)
	 */
	public void testUnderClassAndMethod() {
		new One().test();
		new Two().testing();
		new Three().test();
	}

	/*
	public void testUnderPackage() {
		ControlFlow cflow = new ControlFlow();
		assertFalse(cflow.underPackage("org.springframework.aop"));
		assertTrue(cflow.underPackage("org.springframework.aop.support"));
		assertFalse(cflow.underPackage("com.interface21"));
	}
	*/


	public class One {

		public void test() {
			ControlFlow cflow = createControlFlow();
			assertTrue(cflow.under(One.class));
			assertTrue(cflow.under(AbstractControlFlowTests.class));
			assertFalse(cflow.under(Two.class));
			assertTrue(cflow.under(One.class, "test"));
			assertFalse(cflow.under(One.class, "hashCode"));
		}

	}


	public class Two {

		public void testing() {
			ControlFlow cflow = createControlFlow();
			assertTrue(cflow.under(Two.class));
			assertTrue(cflow.under(AbstractControlFlowTests.class));
			assertFalse(cflow.under(One.class));
			assertFalse(cflow.under(Two.class, "test"));
			assertTrue(cflow.under(Two.class, "testing"));
		}
	}


	public class Three {

		public void test() {
			testing();
		}

		private void testing() {
			ControlFlow cflow = createControlFlow();
			assertTrue(cflow.under(Three.class));
			assertTrue(cflow.under(AbstractControlFlowTests.class));
			assertFalse(cflow.under(One.class));
			assertTrue(cflow.under(Three.class, "test"));
			assertTrue(cflow.under(Three.class, "testing"));
		}
	}

}
