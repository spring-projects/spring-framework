/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.context.annotation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.core.OverridingClassLoader;
import org.springframework.core.SmartClassLoader;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Phillip Webb
 * @author Juergen Hoeller
 */
class ConfigurationClassEnhancerTests {

	@Test
	void enhanceReloadedClass() throws Exception {
		ConfigurationClassEnhancer configurationClassEnhancer = new ConfigurationClassEnhancer();

		ClassLoader parentClassLoader = getClass().getClassLoader();
		ClassLoader classLoader = new CustomSmartClassLoader(parentClassLoader);
		Class<?> myClass = parentClassLoader.loadClass(MyConfig.class.getName());
		Class<?> enhancedClass = configurationClassEnhancer.enhance(myClass, parentClassLoader);
		assertThat(myClass).isAssignableFrom(enhancedClass);

		myClass = classLoader.loadClass(MyConfig.class.getName());
		enhancedClass = configurationClassEnhancer.enhance(myClass, classLoader);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader);
		assertThat(myClass).isAssignableFrom(enhancedClass);
	}

	@Test
	void withPublicClass() {
		ConfigurationClassEnhancer configurationClassEnhancer = new ConfigurationClassEnhancer();

		ClassLoader classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader());
		Class<?> enhancedClass = configurationClassEnhancer.enhance(MyConfigWithPublicClass.class, classLoader);
		assertThat(MyConfigWithPublicClass.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader);

		classLoader = new OverridingClassLoader(getClass().getClassLoader());
		enhancedClass = configurationClassEnhancer.enhance(MyConfigWithPublicClass.class, classLoader);
		assertThat(MyConfigWithPublicClass.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader.getParent());

		classLoader = new CustomSmartClassLoader(getClass().getClassLoader());
		enhancedClass = configurationClassEnhancer.enhance(MyConfigWithPublicClass.class, classLoader);
		assertThat(MyConfigWithPublicClass.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader.getParent());

		classLoader = new BasicSmartClassLoader(getClass().getClassLoader());
		enhancedClass = configurationClassEnhancer.enhance(MyConfigWithPublicClass.class, classLoader);
		assertThat(MyConfigWithPublicClass.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader);
	}

	@Test
	void withNonPublicClass() {
		ConfigurationClassEnhancer configurationClassEnhancer = new ConfigurationClassEnhancer();

		ClassLoader classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader());
		Class<?> enhancedClass = configurationClassEnhancer.enhance(MyConfigWithNonPublicClass.class, classLoader);
		assertThat(MyConfigWithNonPublicClass.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader.getParent());

		classLoader = new OverridingClassLoader(getClass().getClassLoader());
		enhancedClass = configurationClassEnhancer.enhance(MyConfigWithNonPublicClass.class, classLoader);
		assertThat(MyConfigWithNonPublicClass.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader.getParent());

		classLoader = new CustomSmartClassLoader(getClass().getClassLoader());
		enhancedClass = configurationClassEnhancer.enhance(MyConfigWithNonPublicClass.class, classLoader);
		assertThat(MyConfigWithNonPublicClass.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader.getParent());

		classLoader = new BasicSmartClassLoader(getClass().getClassLoader());
		enhancedClass = configurationClassEnhancer.enhance(MyConfigWithNonPublicClass.class, classLoader);
		assertThat(MyConfigWithNonPublicClass.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader.getParent());
	}

	@Test
	void withNonPublicConstructor() {
		ConfigurationClassEnhancer configurationClassEnhancer = new ConfigurationClassEnhancer();

		ClassLoader classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader());
		Class<?> enhancedClass = configurationClassEnhancer.enhance(MyConfigWithNonPublicConstructor.class, classLoader);
		assertThat(MyConfigWithNonPublicConstructor.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader.getParent());

		classLoader = new OverridingClassLoader(getClass().getClassLoader());
		enhancedClass = configurationClassEnhancer.enhance(MyConfigWithNonPublicConstructor.class, classLoader);
		assertThat(MyConfigWithNonPublicConstructor.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader.getParent());

		classLoader = new CustomSmartClassLoader(getClass().getClassLoader());
		enhancedClass = configurationClassEnhancer.enhance(MyConfigWithNonPublicConstructor.class, classLoader);
		assertThat(MyConfigWithNonPublicConstructor.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader.getParent());

		classLoader = new BasicSmartClassLoader(getClass().getClassLoader());
		enhancedClass = configurationClassEnhancer.enhance(MyConfigWithNonPublicConstructor.class, classLoader);
		assertThat(MyConfigWithNonPublicConstructor.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader.getParent());
	}

	@Test
	void withNonPublicMethod() {
		ConfigurationClassEnhancer configurationClassEnhancer = new ConfigurationClassEnhancer();

		ClassLoader classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader());
		Class<?> enhancedClass = configurationClassEnhancer.enhance(MyConfigWithNonPublicMethod.class, classLoader);
		assertThat(MyConfigWithNonPublicMethod.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader.getParent());

		classLoader = new OverridingClassLoader(getClass().getClassLoader());
		enhancedClass = configurationClassEnhancer.enhance(MyConfigWithNonPublicMethod.class, classLoader);
		assertThat(MyConfigWithNonPublicMethod.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader.getParent());

		classLoader = new CustomSmartClassLoader(getClass().getClassLoader());
		enhancedClass = configurationClassEnhancer.enhance(MyConfigWithNonPublicMethod.class, classLoader);
		assertThat(MyConfigWithNonPublicMethod.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader.getParent());

		classLoader = new BasicSmartClassLoader(getClass().getClassLoader());
		enhancedClass = configurationClassEnhancer.enhance(MyConfigWithNonPublicMethod.class, classLoader);
		assertThat(MyConfigWithNonPublicMethod.class).isAssignableFrom(enhancedClass);
		assertThat(enhancedClass.getClassLoader()).isEqualTo(classLoader.getParent());
	}


	@Configuration
	static class MyConfig {

		@Bean
		String myBean() {
			return "bean";
		}
	}


	@Configuration
	public static class MyConfigWithPublicClass {

		@Bean
		public String myBean() {
			return "bean";
		}
	}


	@Configuration
	static class MyConfigWithNonPublicClass {

		@Bean
		public String myBean() {
			return "bean";
		}
	}


	@Configuration
	public static class MyConfigWithNonPublicConstructor {

		MyConfigWithNonPublicConstructor() {
		}

		@Bean
		public String myBean() {
			return "bean";
		}
	}


	@Configuration
	public static class MyConfigWithNonPublicMethod {

		@Bean
		String myBean() {
			return "bean";
		}
	}


	static class CustomSmartClassLoader extends SecureClassLoader implements SmartClassLoader {

		CustomSmartClassLoader(ClassLoader parent) {
			super(parent);
		}

		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (name.contains("MyConfig")) {
				String path = name.replace('.', '/').concat(".class");
				try (InputStream in = super.getResourceAsStream(path)) {
					byte[] bytes = StreamUtils.copyToByteArray(in);
					if (bytes.length > 0) {
						return defineClass(name, bytes, 0, bytes.length);
					}
				}
				catch (IOException ex) {
					throw new IllegalStateException(ex);
				}
			}
			return super.loadClass(name, resolve);
		}

		@Override
		public boolean isClassReloadable(Class<?> clazz) {
			return clazz.getName().contains("MyConfig");
		}

		@Override
		public ClassLoader getOriginalClassLoader() {
			return getParent();
		}

		@Override
		public Class<?> publicDefineClass(String name, byte[] b, @Nullable ProtectionDomain protectionDomain) {
			return defineClass(name, b, 0, b.length, protectionDomain);
		}
	}


	static class BasicSmartClassLoader extends SecureClassLoader implements SmartClassLoader {

		BasicSmartClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		public Class<?> publicDefineClass(String name, byte[] b, @Nullable ProtectionDomain protectionDomain) {
			return defineClass(name, b, 0, b.length, protectionDomain);
		}
	}

}
