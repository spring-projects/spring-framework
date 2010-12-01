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

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.springframework.core.env.AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME;
import static org.springframework.core.env.AbstractEnvironment.DEFAULT_PROFILE_NAME;
import static org.springframework.core.env.AbstractEnvironment.DEFAULT_PROFILE_PROPERTY_NAME;
import static org.springframework.core.env.DefaultEnvironmentTests.CollectionMatchers.isEmpty;

import java.lang.reflect.Field;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.matchers.TypeSafeMatcher;

/**
 * Unit tests for {@link DefaultEnvironment}.
 *
 * @author Chris Beams
 */
public class DefaultEnvironmentTests {

	private static final String ALLOWED_PROPERTY_NAME = "theanswer";
	private static final String ALLOWED_PROPERTY_VALUE = "42";

	private static final String DISALLOWED_PROPERTY_NAME = "verboten";
	private static final String DISALLOWED_PROPERTY_VALUE = "secret";

	private static final String STRING_PROPERTY_NAME = "stringPropName";
	private static final String STRING_PROPERTY_VALUE = "stringPropValue";
	private static final Object NON_STRING_PROPERTY_NAME = new Object();
	private static final Object NON_STRING_PROPERTY_VALUE = new Object();

	private ConfigurableEnvironment environment;
	private Properties testProperties;

	@Before
	public void setUp() {
		environment = new DefaultEnvironment();
		testProperties = new Properties();
		environment.addPropertySource("testProperties", testProperties);
	}

	@Test @SuppressWarnings({ "unchecked", "rawtypes", "serial" })
	public void getPropertySources_manipulatePropertySourceOrder() {
		AbstractEnvironment env = new AbstractEnvironment() { };
		env.addPropertySource("system", new HashMap() {{ put("foo", "systemValue"); }});
		env.addPropertySource("local", new HashMap() {{ put("foo", "localValue"); }});

		// 'local' was added (pushed) last so has precedence
		assertThat(env.getProperty("foo"), equalTo("localValue"));

		// put 'system' at the front of the list
		LinkedList<PropertySource<?>> propertySources = env.getPropertySources();
		propertySources.push(propertySources.remove(propertySources.indexOf(PropertySource.named("system"))));

		// 'system' now has precedence
		assertThat(env.getProperty("foo"), equalTo("systemValue"));

		assertThat(propertySources.size(), is(2));
	}

	@Test @SuppressWarnings({ "unchecked", "rawtypes", "serial" })
	public void getPropertySources_replacePropertySource() {
		AbstractEnvironment env = new AbstractEnvironment() { };
		env.addPropertySource("system", new HashMap() {{ put("foo", "systemValue"); }});
		env.addPropertySource("local", new HashMap() {{ put("foo", "localValue"); }});

		// 'local' was added (pushed) last so has precedence
		assertThat(env.getProperty("foo"), equalTo("localValue"));

		// replace 'local' with new property source
		LinkedList<PropertySource<?>> propertySources = env.getPropertySources();
		int localIndex = propertySources.indexOf(PropertySource.named("local"));
		MapPropertySource newSource = new MapPropertySource("new", new HashMap() {{ put("foo", "newValue"); }});
		propertySources.set(localIndex, newSource);

		// 'system' now has precedence
		assertThat(env.getProperty("foo"), equalTo("newValue"));

		assertThat(propertySources.size(), is(2));
	}

	@Test
	public void getProperty() {
		assertThat(environment.getProperty("foo"), nullValue());
		testProperties.put("foo", "bar");
		assertThat(environment.getProperty("foo"), is("bar"));
	}

	@Test
	public void getProperty_withExplicitNullValue() {
		// java.util.Properties does not allow null values (because Hashtable does not)
		Map<String, String> nullableProperties = new HashMap<String, String>();
		environment.addPropertySource("nullableProperties", nullableProperties);
		nullableProperties.put("foo", null);
		assertThat(environment.getProperty("foo"), nullValue());
	}

	@Test
	public void getProperty_withStringArrayConversion() {
		testProperties.put("foo", "bar,baz");
		assertThat(environment.getProperty("foo", String[].class), equalTo(new String[] { "bar", "baz" }));
	}

	@Test
	public void getProperty_withNonConvertibleTargetType() {
		testProperties.put("foo", "bar");

		class TestType { }

		try {
			environment.getProperty("foo", TestType.class);
			fail("Expected IllegalArgumentException due to non-convertible types");
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

	@Test
	public void getRequiredProperty() {
		testProperties.put("exists", "xyz");
		assertThat(environment.getRequiredProperty("exists"), is("xyz"));

		try {
			environment.getRequiredProperty("bogus");
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

	@Test
	public void getRequiredProperty_withStringArrayConversion() {
		testProperties.put("exists", "abc,123");
		assertThat(environment.getRequiredProperty("exists", String[].class), equalTo(new String[] { "abc", "123" }));

		try {
			environment.getRequiredProperty("bogus", String[].class);
			fail("expected IllegalArgumentException");
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

	@Test @SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	public void asProperties() {
		ConfigurableEnvironment env = new AbstractEnvironment() { };
		assertThat(env.asProperties(), notNullValue());

		env.addPropertySource("lowestPrecedence", new HashMap() {{ put("common", "lowCommon"); put("lowKey", "lowVal"); }});
		env.addPropertySource("middlePrecedence", new HashMap() {{ put("common", "midCommon"); put("midKey", "midVal"); }});
		env.addPropertySource("highestPrecedence", new HashMap() {{ put("common", "highCommon"); put("highKey", "highVal"); }});

		Properties props = env.asProperties();
		assertThat(props.getProperty("common"), is("highCommon"));
		assertThat(props.getProperty("lowKey"), is("lowVal"));
		assertThat(props.getProperty("midKey"), is("midVal"));
		assertThat(props.getProperty("highKey"), is("highVal"));
		assertThat(props.size(), is(4));
	}

	@Test
	public void activeProfiles() {
		assertThat(environment.getActiveProfiles(), isEmpty());
		environment.setActiveProfiles("local", "embedded");
		Set<String> activeProfiles = environment.getActiveProfiles();
		assertThat(activeProfiles, hasItems("local", "embedded"));
		assertThat(activeProfiles.size(), is(2));
		try {
			environment.getActiveProfiles().add("bogus");
			fail("activeProfiles should be unmodifiable");
		} catch (UnsupportedOperationException ex) {
			// expected
		}
		environment.setActiveProfiles("foo");
		assertThat(activeProfiles, hasItem("foo"));
		assertThat(environment.getActiveProfiles().size(), is(1));
	}

	@Test
	public void systemPropertiesEmpty() {
		assertThat(environment.getActiveProfiles(), isEmpty());

		System.setProperty(ACTIVE_PROFILES_PROPERTY_NAME, "");
		assertThat(environment.getActiveProfiles(), isEmpty());

		System.getProperties().remove(ACTIVE_PROFILES_PROPERTY_NAME);
	}

	@Test
	public void systemPropertiesResoloutionOfProfiles() {
		assertThat(environment.getActiveProfiles(), isEmpty());

		System.setProperty(ACTIVE_PROFILES_PROPERTY_NAME, "foo");
		assertThat(environment.getActiveProfiles(), hasItem("foo"));

		// clean up
		System.getProperties().remove(ACTIVE_PROFILES_PROPERTY_NAME);
	}

	@Test
	public void systemPropertiesResoloutionOfMultipleProfiles() {
		assertThat(environment.getActiveProfiles(), isEmpty());

		System.setProperty(ACTIVE_PROFILES_PROPERTY_NAME, "foo,bar");
		assertThat(environment.getActiveProfiles(), hasItems("foo", "bar"));

		System.setProperty(ACTIVE_PROFILES_PROPERTY_NAME, " bar , baz "); // notice whitespace
		assertThat(environment.getActiveProfiles(), not(hasItems("foo", "bar")));
		assertThat(environment.getActiveProfiles(), hasItems("bar", "baz"));

		System.getProperties().remove(ACTIVE_PROFILES_PROPERTY_NAME);
	}

	@Test
	public void environmentResolutionOfDefaultSpringProfileProperty_noneSet() {
		assertThat(environment.getDefaultProfile(), equalTo(DEFAULT_PROFILE_NAME));
	}

	@Test
	public void environmentResolutionOfDefaultSpringProfileProperty_isSet() {
		testProperties.setProperty(DEFAULT_PROFILE_PROPERTY_NAME, "custom-default");
		assertThat(environment.getDefaultProfile(), equalTo("custom-default"));
	}

	@Test
	public void systemPropertiesAccess() {
		System.setProperty(ALLOWED_PROPERTY_NAME, ALLOWED_PROPERTY_VALUE);
		System.setProperty(DISALLOWED_PROPERTY_NAME, DISALLOWED_PROPERTY_VALUE);
		System.getProperties().put(STRING_PROPERTY_NAME, NON_STRING_PROPERTY_VALUE);
		System.getProperties().put(NON_STRING_PROPERTY_NAME, STRING_PROPERTY_VALUE);

		{
			Map<?, ?> systemProperties = environment.getSystemProperties();
			assertThat(systemProperties, notNullValue());
			assertSame(systemProperties, System.getProperties());
			assertThat(systemProperties.get(ALLOWED_PROPERTY_NAME), equalTo((Object)ALLOWED_PROPERTY_VALUE));
			assertThat(systemProperties.get(DISALLOWED_PROPERTY_NAME), equalTo((Object)DISALLOWED_PROPERTY_VALUE));

			// non-string keys and values work fine... until the security manager is introduced below
			assertThat(systemProperties.get(STRING_PROPERTY_NAME), equalTo(NON_STRING_PROPERTY_VALUE));
			assertThat(systemProperties.get(NON_STRING_PROPERTY_NAME), equalTo((Object)STRING_PROPERTY_VALUE));
		}

		SecurityManager oldSecurityManager = System.getSecurityManager();
		SecurityManager securityManager = new SecurityManager() {
			@Override
			public void checkPropertiesAccess() {
				// see http://download.oracle.com/javase/1.5.0/docs/api/java/lang/System.html#getProperties()
				throw new AccessControlException("Accessing the system properties is disallowed");
			}
			@Override
			public void checkPropertyAccess(String key) {
				// see http://download.oracle.com/javase/1.5.0/docs/api/java/lang/System.html#getProperty(java.lang.String)
				if (DISALLOWED_PROPERTY_NAME.equals(key)) {
					throw new AccessControlException(
							format("Accessing the system property [%s] is disallowed", DISALLOWED_PROPERTY_NAME));
				}
			}
			@Override
			public void checkPermission(Permission perm) {
				// allow everything else
			}
		};
		System.setSecurityManager(securityManager);

		{
			Map<?, ?> systemProperties = environment.getSystemProperties();
			assertThat(systemProperties, notNullValue());
			assertThat(systemProperties, instanceOf(ReadOnlySystemAttributesMap.class));
			assertThat((String)systemProperties.get(ALLOWED_PROPERTY_NAME), equalTo(ALLOWED_PROPERTY_VALUE));
			assertThat(systemProperties.get(DISALLOWED_PROPERTY_NAME), equalTo(null));

			// nothing we can do here in terms of warning the user that there was
			// actually a (non-string) value available. By this point, we only
			// have access to calling System.getProperty(), which itself returns null
			// if the value is non-string.  So we're stuck with returning a potentially
			// misleading null.
			assertThat(systemProperties.get(STRING_PROPERTY_NAME), nullValue());

			// in the case of a non-string *key*, however, we can do better.  Alert
			// the user that under these very special conditions (non-object key +
			// SecurityManager that disallows access to system properties), they
			// cannot do what they're attempting.
			try {
				systemProperties.get(NON_STRING_PROPERTY_NAME);
				fail("Expected IllegalStateException when searching with non-string key against ReadOnlySystemAttributesMap");
			} catch (IllegalStateException ex) {
				// expected
			}
		}

		System.setSecurityManager(oldSecurityManager);
		System.clearProperty(ALLOWED_PROPERTY_NAME);
		System.clearProperty(DISALLOWED_PROPERTY_NAME);
		System.getProperties().remove(STRING_PROPERTY_NAME);
		System.getProperties().remove(NON_STRING_PROPERTY_NAME);
	}

	@Test
	public void systemEnvironmentAccess() throws Exception {
		getModifiableSystemEnvironment().put(ALLOWED_PROPERTY_NAME, ALLOWED_PROPERTY_VALUE);
		getModifiableSystemEnvironment().put(DISALLOWED_PROPERTY_NAME, DISALLOWED_PROPERTY_VALUE);

		{
			Map<String, String> systemEnvironment = environment.getSystemEnvironment();
			assertThat(systemEnvironment, notNullValue());
			assertSame(systemEnvironment, System.getenv());
		}

		SecurityManager oldSecurityManager = System.getSecurityManager();
		SecurityManager securityManager = new SecurityManager() {
			@Override
			public void checkPermission(Permission perm) {
				//see http://download.oracle.com/javase/1.5.0/docs/api/java/lang/System.html#getenv()
				if ("getenv.*".equals(perm.getName())) {
					throw new AccessControlException("Accessing the system environment is disallowed");
				}
				//see http://download.oracle.com/javase/1.5.0/docs/api/java/lang/System.html#getenv(java.lang.String)
				if (("getenv."+DISALLOWED_PROPERTY_NAME).equals(perm.getName())) {
					throw new AccessControlException(
							format("Accessing the system environment variable [%s] is disallowed", DISALLOWED_PROPERTY_NAME));
				}
			}
		};
		System.setSecurityManager(securityManager);

		{
			Map<String, String> systemEnvironment = environment.getSystemEnvironment();
			assertThat(systemEnvironment, notNullValue());
			assertThat(systemEnvironment, instanceOf(ReadOnlySystemAttributesMap.class));
			assertThat(systemEnvironment.get(ALLOWED_PROPERTY_NAME), equalTo(ALLOWED_PROPERTY_VALUE));
			assertThat(systemEnvironment.get(DISALLOWED_PROPERTY_NAME), nullValue());
		}

		System.setSecurityManager(oldSecurityManager);
		getModifiableSystemEnvironment().remove(ALLOWED_PROPERTY_NAME);
		getModifiableSystemEnvironment().remove(DISALLOWED_PROPERTY_NAME);
	}

	@Test
	public void resolvePlaceholders() {
		AbstractEnvironment env = new AbstractEnvironment() { };
		Properties testProperties = new Properties();
		testProperties.setProperty("foo", "bar");
		env.addPropertySource("testProperties", testProperties);
		String resolved = env.resolvePlaceholders("pre-${foo}-${unresolvable}-post");
		assertThat(resolved, is("pre-bar-${unresolvable}-post"));
	}

	@Test
	public void resolveRequiredPlaceholders() {
		AbstractEnvironment env = new AbstractEnvironment() { };
		Properties testProperties = new Properties();
		testProperties.setProperty("foo", "bar");
		env.addPropertySource("testProperties", testProperties);
		try {
			env.resolveRequiredPlaceholders("pre-${foo}-${unresolvable}-post");
			fail("expected exception");
		} catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(), is("Could not resolve placeholder 'unresolvable'"));
		}
	}

	public static class CollectionMatchers {
		public static Matcher<Collection<?>> isEmpty() {

			return new TypeSafeMatcher<Collection<?>>() {

				@Override
				public boolean matchesSafely(Collection<?> collection) {
					return collection.isEmpty();
				}

				public void describeTo(Description desc) {
					desc.appendText("an empty collection");
				}
			};
		}
	}

	// TODO SPR-7508: duplicated from EnvironmentPropertyResolutionSearchTests
	@SuppressWarnings("unchecked")
	private static Map<String, String> getModifiableSystemEnvironment() throws Exception {
		Class<?>[] classes = Collections.class.getDeclaredClasses();
		Map<String, String> systemEnv = System.getenv();
		for (Class<?> cl : classes) {
			if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
				Field field = cl.getDeclaredField("m");
				field.setAccessible(true);
				Object obj = field.get(systemEnv);
				return (Map<String, String>) obj;
			}
		}
		throw new IllegalStateException();
	}
}
