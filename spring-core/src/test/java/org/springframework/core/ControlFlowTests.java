/*
 * Copyright 2002-2014 the original author or authors.
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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @author Sam Brannen
 */
public class ControlFlowTests {

	@Test
	public void underClassAndMethod() {
		new One().test();
		new Two().testing();
		new Three().test();
	}

	static class One {

		void test() {
			ControlFlow cflow = ControlFlowFactory.createControlFlow();
			assertTrue(cflow.under(One.class));
			assertTrue(cflow.under(ControlFlowTests.class));
			assertFalse(cflow.under(Two.class));
			assertTrue(cflow.under(One.class, "test"));
			assertFalse(cflow.under(One.class, "hashCode"));
		}
	}

	static class Two {

		void testing() {
			ControlFlow cflow = ControlFlowFactory.createControlFlow();
			assertTrue(cflow.under(Two.class));
			assertTrue(cflow.under(ControlFlowTests.class));
			assertFalse(cflow.under(One.class));
			assertFalse(cflow.under(Two.class, "test"));
			assertTrue(cflow.under(Two.class, "testing"));
		}
	}

	static class Three {

		void test() {
			testing();
		}

		private void testing() {
			ControlFlow cflow = ControlFlowFactory.createControlFlow();
			assertTrue(cflow.under(Three.class));
			assertTrue(cflow.under(ControlFlowTests.class));
			assertFalse(cflow.under(One.class));
			assertTrue(cflow.under(Three.class, "test"));
			assertTrue(cflow.under(Three.class, "testing"));
		}
	}

}
