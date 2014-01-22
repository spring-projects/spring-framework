/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Map;

import org.springframework.util.Assert;

/**
 * Specialization of {@link MapPropertySource} designed for use with
 * {@linkplain AbstractEnvironment#getSystemEnvironment() system environment variables}.
 * Compensates for constraints in Bash and other shells that do not allow for variables
 * containing the period character; also allows for uppercase variations on property
 * names for more idiomatic shell use.
 *
 * <p>For example, a call to {@code getProperty("foo.bar")} will attempt to find a value
 * for the original property or any 'equivalent' property, returning the first found:
 * <ul>
 * <li>{@code foo.bar} - the original name</li>
 * <li>{@code foo_bar} - with underscores for periods (if any)</li>
 * <li>{@code FOO.BAR} - original, with upper case</li>
 * <li>{@code FOO_BAR} - with underscores and upper case</li>
 * </ul>
 *
 * The same applies for calls to {@link #containsProperty(String)}, which returns
 * {@code true} if any of the above properties are present, otherwise {@code false}.
 *
 * <p>This feature is particularly useful when specifying active or default profiles as
 * environment variables. The following is not allowable under Bash
 *
 * <pre class="code">spring.profiles.active=p1 java -classpath ... MyApp</pre>
 *
 * However, the following syntax is permitted and is also more conventional.
 *
 * <pre class="code">SPRING_PROFILES_ACTIVE=p1 java -classpath ... MyApp</pre>
 *
 * <p>Enable debug- or trace-level logging for this class (or package) for messages
 * explaining when these 'property name resolutions' occur.
 *
 * <p>This property source is included by default in {@link StandardEnvironment}
 * and all its subclasses.
 *
 * @author Chris Beams
 * @since 3.1
 * @see StandardEnvironment
 * @see AbstractEnvironment#getSystemEnvironment()
 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
 */
public class SystemEnvironmentPropertySource extends MapPropertySource {

	/**
	 * Create a new {@code SystemEnvironmentPropertySource} with the given name and
	 * delegating to the given {@code MapPropertySource}.
	 */
	public SystemEnvironmentPropertySource(String name, Map<String, Object> source) {
		super(name, source);
	}

	/**
	 * Return true if a property with the given name or any underscore/uppercase variant
	 * thereof exists in this property source.
	 */
	@Override
	public boolean containsProperty(String name) {
		return (getProperty(name) != null);
	}

	/**
	 * {@inheritDoc}
	 * <p>This implementation returns {@code true} if a property with the given name or
	 * any underscore/uppercase variant thereof exists in this property source.
	 */
	@Override
	public Object getProperty(String name) {
		Assert.notNull(name, "property name must not be null");
		String actualName = resolvePropertyName(name);
		if (logger.isDebugEnabled() && !name.equals(actualName)) {
			logger.debug(String.format("PropertySource [%s] does not contain '%s', but found equivalent '%s'",
					getName(), name, actualName));
		}
		return super.getProperty(actualName);
	}

	/**
	 * Check to see if this property source contains a property with the given name, or
	 * any underscore / uppercase variation thereof. Return the resolved name if one is
	 * found or otherwise the original name. Never returns {@code null}.
	 */
	private String resolvePropertyName(String name) {
		if (super.containsProperty(name)) {
			return name;
		}

		String usName = name.replace('.', '_');
		if (!name.equals(usName) && super.containsProperty(usName)) {
			return usName;
		}

		String ucName = name.toUpperCase();
		if (!name.equals(ucName)) {
			if (super.containsProperty(ucName)) {
				return ucName;
			}
			else {
				String usUcName = ucName.replace('.', '_');
				if (!ucName.equals(usUcName) && super.containsProperty(usUcName)) {
					return usUcName;
				}
			}
		}

		return name;
	}
}
