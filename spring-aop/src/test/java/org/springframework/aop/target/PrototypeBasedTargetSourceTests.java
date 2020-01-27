/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.target;

import org.junit.jupiter.api.Test;

import org.springframework.aop.TargetSource;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.SerializablePerson;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests relating to the abstract {@link AbstractPrototypeBasedTargetSource}
 * and not subclasses.
 *
 * @author Rod Johnson
 * @author Chris Beams
 */
public class PrototypeBasedTargetSourceTests {

	@Test
	public void testSerializability() throws Exception {
		MutablePropertyValues tsPvs = new MutablePropertyValues();
		tsPvs.add("targetBeanName", "person");
		RootBeanDefinition tsBd = new RootBeanDefinition(TestTargetSource.class);
		tsBd.setPropertyValues(tsPvs);

		MutablePropertyValues pvs = new MutablePropertyValues();
		RootBeanDefinition bd = new RootBeanDefinition(SerializablePerson.class);
		bd.setPropertyValues(pvs);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("ts", tsBd);
		bf.registerBeanDefinition("person", bd);

		TestTargetSource cpts = (TestTargetSource) bf.getBean("ts");
		TargetSource serialized = (TargetSource) SerializationTestUtils.serializeAndDeserialize(cpts);
		boolean condition = serialized instanceof SingletonTargetSource;
		assertThat(condition).as("Changed to SingletonTargetSource on deserialization").isTrue();
		SingletonTargetSource sts = (SingletonTargetSource) serialized;
		assertThat(sts.getTarget()).isNotNull();
	}


	private static class TestTargetSource extends AbstractPrototypeBasedTargetSource {

		private static final long serialVersionUID = 1L;

		/**
		 * Nonserializable test field to check that subclass
		 * state can't prevent serialization from working
		 */
		@SuppressWarnings("unused")
		private TestBean thisFieldIsNotSerializable = new TestBean();

		@Override
		public Object getTarget() throws Exception {
			return newPrototypeInstance();
		}

		@Override
		public void releaseTarget(Object target) throws Exception {
			// Do nothing
		}
	}

}
