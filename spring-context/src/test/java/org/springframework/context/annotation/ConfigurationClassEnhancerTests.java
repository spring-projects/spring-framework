/*
 * Copyright 2002-2024 the original author or authors.
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
import java.security.SecureClassLoader;

import org.junit.jupiter.api.Test;

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
		CustomClassLoader classLoader = new CustomClassLoader(parentClassLoader);
		Class<?> myClass = parentClassLoader.loadClass(MyConfig.class.getName());
		configurationClassEnhancer.enhance(myClass, parentClassLoader);
		Class<?> myReloadedClass = classLoader.loadClass(MyConfig.class.getName());
		Class<?> enhancedReloadedClass = configurationClassEnhancer.enhance(myReloadedClass, classLoader);
		assertThat(enhancedReloadedClass.getClassLoader()).isEqualTo(classLoader);
	}


	@Configuration
	static class MyConfig {

		@Bean
		public String myBean() {
			return "bean";
		}
	}


	static class CustomClassLoader extends SecureClassLoader implements SmartClassLoader {

		CustomClassLoader(ClassLoader parent) {
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
	}

}
