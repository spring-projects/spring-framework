/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.core.env;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

/**
 * Unit tests for {@link DefaultEnvironment} proving that it (a) searches
 * standard property sources (b) in the correct order.
 *
 * @see AbstractEnvironment#getProperty(String)
 * @author Chris Beams
 * @since 3.1
 */
public class EnvironmentPropertyResolutionSearchTests {

	@Test @SuppressWarnings({ "unchecked", "serial", "rawtypes" })
	public void propertySourcesHaveLIFOSearchOrder() {
		ConfigurableEnvironment env = new AbstractEnvironment() { };
		env.addPropertySource("ps1", new HashMap() {{ put("pName", "ps1Value"); }});
		assertThat(env.getProperty("pName"), equalTo("ps1Value"));
		env.addPropertySource("ps2", new HashMap() {{ put("pName", "ps2Value"); }});
		assertThat(env.getProperty("pName"), equalTo("ps2Value"));
		env.addPropertySource("ps3", new HashMap() {{ put("pName", "ps3Value"); }});
		assertThat(env.getProperty("pName"), equalTo("ps3Value"));
	}

	@Test
	public void resolveFromDefaultPropertySources() throws Exception {
		String key = "x";
		String localPropsValue = "local";
		String sysPropsValue = "sys";
		String envVarsValue = "env";

		Map<String, String> systemEnvironment = getModifiableSystemEnvironment();
		Properties systemProperties = System.getProperties();
		Properties localProperties = new Properties();

		DefaultEnvironment env = new DefaultEnvironment();
		env.addPropertySource("localProperties", localProperties);

		// set all properties
		systemEnvironment.put(key, envVarsValue);
		systemProperties.setProperty(key, sysPropsValue);
		localProperties.setProperty(key, localPropsValue);

		// local properties should have highest resolution precedence
		assertThat(env.getProperty(key), equalTo(localPropsValue));

		// system properties should be next in line
		localProperties.remove(key);
		assertThat(env.getProperty(key), equalTo(sysPropsValue));

		// system environment variables should be final fallback
		systemProperties.remove(key);
		assertThat(env.getProperty(key), equalTo(envVarsValue));

		// with no propertysource containing the key in question, should return null
		systemEnvironment.remove(key);
		assertThat(env.getProperty(key), equalTo(null));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> getModifiableSystemEnvironment() throws Exception {
		Class<?>[] classes = Collections.class.getDeclaredClasses();
		Map<String, String> env = System.getenv();
		for (Class<?> cl : classes) {
			if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
				Field field = cl.getDeclaredField("m");
				field.setAccessible(true);
				Object obj = field.get(env);
				return (Map<String, String>) obj;
			}
		}
		throw new IllegalStateException();
	}

}
