/*
 * Copyright 2002-2023 the original author or authors.
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
		// Global
		"org/springframework/test/context/aot/AotTestContextInitializers__Generated.java",
		"org/springframework/test/context/aot/AotTestAttributes__Generated.java",
		// BasicSpringJupiterImportedConfigTests
		"org/springframework/context/event/DefaultEventListenerFactory__TestContext001_BeanDefinitions.java",
		"org/springframework/context/event/EventListenerMethodProcessor__TestContext001_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterImportedConfigTests__TestContext001_ApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterImportedConfigTests__TestContext001_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterImportedConfigTests__TestContext001_BeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext001_BeanDefinitions.java",
		// BasicSpringJupiterSharedConfigTests
		"org/springframework/context/event/DefaultEventListenerFactory__TestContext002_BeanDefinitions.java",
		"org/springframework/context/event/EventListenerMethodProcessor__TestContext002_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterSharedConfigTests__TestContext002_ApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterSharedConfigTests__TestContext002_BeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterSharedConfigTests__TestContext002_ManagementApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterSharedConfigTests__TestContext002_ManagementBeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext002_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/management/ManagementConfiguration__TestContext002_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/management/ManagementMessageService__TestContext002_ManagementBeanDefinitions.java",
		// BasicSpringJupiterTests -- not generated b/c already generated for BasicSpringJupiterSharedConfigTests.
		// BasicSpringJupiterTests.NestedTests
		"org/springframework/context/event/DefaultEventListenerFactory__TestContext003_BeanDefinitions.java",
		"org/springframework/context/event/EventListenerMethodProcessor__TestContext003_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterTests_NestedTests__TestContext003_ApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterTests_NestedTests__TestContext003_BeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterTests_NestedTests__TestContext003_ManagementApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterTests_NestedTests__TestContext003_ManagementBeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext003_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/management/ManagementConfiguration__TestContext003_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/management/ManagementMessageService__TestContext003_ManagementBeanDefinitions.java",
		// BasicSpringTestNGTests
		"org/springframework/context/event/DefaultEventListenerFactory__TestContext004_BeanDefinitions.java",
		"org/springframework/context/event/EventListenerMethodProcessor__TestContext004_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringTestNGTests__TestContext004_ApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringTestNGTests__TestContext004_BeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext004_BeanDefinitions.java",
		// BasicSpringVintageTests
		"org/springframework/context/event/DefaultEventListenerFactory__TestContext005_BeanDefinitions.java",
		"org/springframework/context/event/EventListenerMethodProcessor__TestContext005_BeanDefinitions.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringVintageTests__TestContext005_ApplicationContextInitializer.java",
		"org/springframework/test/context/aot/samples/basic/BasicSpringVintageTests__TestContext005_BeanFactoryRegistrations.java",
		"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext005_BeanDefinitions.java"
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
