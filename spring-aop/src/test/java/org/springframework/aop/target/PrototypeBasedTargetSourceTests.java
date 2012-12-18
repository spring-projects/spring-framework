/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.aop.target;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.aop.TargetSource;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import test.beans.SerializablePerson;
import test.beans.TestBean;
import test.util.SerializationTestUtils;

/**
 * Unit tests relating to the abstract {@link AbstractPrototypeBasedTargetSource}
 * and not subclasses.
 *
 * @author Rod Johnson
 * @author Chris Beams
 */
public final class PrototypeBasedTargetSourceTests {

	@Test
	public void testSerializability() throws Exception {
		MutablePropertyValues tsPvs = new MutablePropertyValues();
		tsPvs.add("targetBeanName", "person");
		RootBeanDefinition tsBd = new RootBeanDefinition(TestTargetSource.class, tsPvs);

		MutablePropertyValues pvs = new MutablePropertyValues();
		RootBeanDefinition bd = new RootBeanDefinition(SerializablePerson.class, pvs);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("ts", tsBd);
		bf.registerBeanDefinition("person", bd);

		TestTargetSource cpts = (TestTargetSource) bf.getBean("ts");
		TargetSource serialized = (TargetSource) SerializationTestUtils.serializeAndDeserialize(cpts);
		assertTrue("Changed to SingletonTargetSource on deserialization",
				serialized instanceof SingletonTargetSource);
		SingletonTargetSource sts = (SingletonTargetSource) serialized;
		assertNotNull(sts.getTarget());
	}


	private static class TestTargetSource extends AbstractPrototypeBasedTargetSource {

		/**
		 * Nonserializable test field to check that subclass
		 * state can't prevent serialization from working
		 */
		private TestBean thisFieldIsNotSerializable = new TestBean();

		public Object getTarget() throws Exception {
			return newPrototypeInstance();
		}

		public void releaseTarget(Object target) throws Exception {
			// Do nothing
		}
	}

}