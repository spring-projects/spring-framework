/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.aot;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Abstract base class for AOT tests.
 *
 * @author Sam Brannen
 * @since 6.0
 */
abstract class AbstractAotTests {

	static final String[] expectedSourceFilesForBasicSpringTests = {

		// BasicSpringJupiterSharedConfigTests -- not generated b/c already generated for AbstractSpringJupiterParameterizedClassTests.InheritedNestedTests.
		// BasicSpringJupiterTests -- not generated b/c already generated for AbstractSpringJupiterParameterizedClassTests.InheritedNestedTests.
		// BasicSpringJupiterTests.NestedTests -- not generated b/c already generated for BasicSpringJupiterParameterizedClassTests.NestedTests.

		// Global
		"org/springframework/test/context/aot/AotTestContextInitializers__Generated.java",
		"org/springframework/test/context/aot/AotTestAttributes__Generated.java",

		// AbstractSpringJupiterParameterizedClassTests.InheritedNestedTests
		"org/springframework/context/event/DefaultEventListenerFactory__TestContext001_BeanDefinitions.java",
		"org/springframework/context/event/EventListenerMethodProcessor__TestContext001_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/AbstractSpringJupiterParameterizedClassTests_InheritedNestedTests__TestContext001_ApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/AbstractSpringJupiterParameterizedClassTests_InheritedNestedTests__TestContext001_BeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/AbstractSpringJupiterParameterizedClassTests_InheritedNestedTests__TestContext001_ManagementApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/AbstractSpringJupiterParameterizedClassTests_InheritedNestedTests__TestContext001_ManagementBeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext001_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/management/ManagementConfiguration__TestContext001_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/management/ManagementMessageService__TestContext001_ManagementBeanDefinitions.java",
		"org/springframework/test/context/support/DynamicPropertyRegistrarBeanInitializer__TestContext001_BeanDefinitions.java",

		// AbstractSpringJupiterParameterizedClassTests.InheritedNestedTests.InheritedDoublyNestedTests
		"org/springframework/context/event/DefaultEventListenerFactory__TestContext002_BeanDefinitions.java",
		"org/springframework/context/event/EventListenerMethodProcessor__TestContext002_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/AbstractSpringJupiterParameterizedClassTests_InheritedNestedTests_InheritedDoublyNestedTests__TestContext002_ApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/AbstractSpringJupiterParameterizedClassTests_InheritedNestedTests_InheritedDoublyNestedTests__TestContext002_BeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/AbstractSpringJupiterParameterizedClassTests_InheritedNestedTests_InheritedDoublyNestedTests__TestContext002_ManagementApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/AbstractSpringJupiterParameterizedClassTests_InheritedNestedTests_InheritedDoublyNestedTests__TestContext002_ManagementBeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext002_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/management/ManagementConfiguration__TestContext002_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/management/ManagementMessageService__TestContext002_ManagementBeanDefinitions.java",
		"org/springframework/test/context/support/DynamicPropertyRegistrarBeanInitializer__TestContext002_BeanDefinitions.java",

		// BasicSpringJupiterImportedConfigTests
		"org/springframework/context/event/DefaultEventListenerFactory__TestContext003_BeanDefinitions.java",
		"org/springframework/context/event/EventListenerMethodProcessor__TestContext003_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterImportedConfigTests__TestContext003_ApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterImportedConfigTests__TestContext003_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterImportedConfigTests__TestContext003_BeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext003_BeanDefinitions.java",
		"org/springframework/test/context/support/DynamicPropertyRegistrarBeanInitializer__TestContext003_BeanDefinitions.java",

		// BasicSpringJupiterParameterizedClassTests.NestedTests
		"org/springframework/context/event/DefaultEventListenerFactory__TestContext004_BeanDefinitions.java",
		"org/springframework/context/event/EventListenerMethodProcessor__TestContext004_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterParameterizedClassTests_NestedTests__TestContext004_ApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterParameterizedClassTests_NestedTests__TestContext004_BeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterParameterizedClassTests_NestedTests__TestContext004_ManagementApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterParameterizedClassTests_NestedTests__TestContext004_ManagementBeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext004_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/management/ManagementConfiguration__TestContext004_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/management/ManagementMessageService__TestContext004_ManagementBeanDefinitions.java",
		"org/springframework/test/context/support/DynamicPropertyRegistrarBeanInitializer__TestContext004_BeanDefinitions.java",

		// BasicSpringJupiterParameterizedClassTests.NestedTests.DoublyNestedTests
		"org/springframework/context/event/DefaultEventListenerFactory__TestContext005_BeanDefinitions.java",
		"org/springframework/context/event/EventListenerMethodProcessor__TestContext005_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterParameterizedClassTests_NestedTests_DoublyNestedTests__TestContext005_ApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterParameterizedClassTests_NestedTests_DoublyNestedTests__TestContext005_BeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterParameterizedClassTests_NestedTests_DoublyNestedTests__TestContext005_ManagementApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterParameterizedClassTests_NestedTests_DoublyNestedTests__TestContext005_ManagementBeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext005_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/management/ManagementConfiguration__TestContext005_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/management/ManagementMessageService__TestContext005_ManagementBeanDefinitions.java",
		"org/springframework/test/context/support/DynamicPropertyRegistrarBeanInitializer__TestContext005_BeanDefinitions.java",

		// BasicSpringTestNGTests
		"org/springframework/context/event/DefaultEventListenerFactory__TestContext006_BeanDefinitions.java",
		"org/springframework/context/event/EventListenerMethodProcessor__TestContext006_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringTestNGTests__TestContext006_ApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringTestNGTests__TestContext006_BeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext006_BeanDefinitions.java",
		"org/springframework/test/context/support/DynamicPropertyRegistrarBeanInitializer__TestContext006_BeanDefinitions.java",

		// BasicSpringVintageTests
		"org/springframework/context/event/DefaultEventListenerFactory__TestContext007_BeanDefinitions.java",
		"org/springframework/context/event/EventListenerMethodProcessor__TestContext007_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringVintageTests__TestContext007_ApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringVintageTests__TestContext007_BeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext007_BeanDefinitions.java",
		"org/springframework/test/context/support/DynamicPropertyRegistrarBeanInitializer__TestContext007_BeanDefinitions.java",

		// DisabledInAotRuntimeMethodLevelTests
		"org/springframework/context/event/DefaultEventListenerFactory__TestContext008_BeanDefinitions.java",
		"org/springframework/context/event/EventListenerMethodProcessor__TestContext008_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/DisabledInAotRuntimeMethodLevelTests__TestContext008_ApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/DisabledInAotRuntimeMethodLevelTests__TestContext008_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/DisabledInAotRuntimeMethodLevelTests__TestContext008_BeanFactoryRegistrations.java",
		"org/springframework/test/context/support/DynamicPropertyRegistrarBeanInitializer__TestContext008_BeanDefinitions.java"
	};

	Stream<Class<?>> scan() {
		return new TestClassScanner(classpathRoots()).scan();
	}

	Stream<Class<?>> scan(String... packageNames) {
		return new TestClassScanner(classpathRoots()).scan(packageNames);
	}

	Set<Path> classpathRoots() {
		try {
			return Set.of(classpathRoot());
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	Path classpathRoot() {
		try {
			return Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	Path classpathRoot(Class<?> clazz) {
		try {
			return Paths.get(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}
