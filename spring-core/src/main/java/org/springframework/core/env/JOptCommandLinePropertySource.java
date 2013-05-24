/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * {@link CommandLinePropertySource} implementation backed by a JOpt {@link OptionSet}.
 *
 * <h2>Typical usage</h2>
 * Configure and execute an {@code OptionParser} against the {@code String[]} of arguments
 * supplied to the {@code main} method, and create a {@link JOptCommandLinePropertySource}
 * using the resulting {@code OptionSet} object:
 * <pre class="code">
 * public static void main(String[] args) {
 *     OptionParser parser = new OptionParser();
 *     parser.accepts("option1");
 *     parser.accepts("option2").withRequiredArg();
 *     OptionSet options = parser.parse(args);
 *     PropertySource<?> ps = new JOptCommandLinePropertySource(options);
 *     // ...
 * }</pre>
 *
 * See {@link CommandLinePropertySource} for complete general usage examples.
 *
 * <h3>Requirements</h3>
 *
 * <p>Use of this class requires adding the jopt-simple JAR to your application classpath.
 * Versions 3.0 and better are supported.
 *
 * @author Chris Beams
 * @since 3.1
 * @see CommandLinePropertySource
 * @see joptsimple.OptionParser
 * @see joptsimple.OptionSet
 */
public class JOptCommandLinePropertySource extends CommandLinePropertySource<OptionSet> {

	/**
	 * Create a new {@code JOptCommandLinePropertySource} having the default name
	 * and backed by the given {@code OptionSet}.
	 * @see CommandLinePropertySource#COMMAND_LINE_PROPERTY_SOURCE_NAME
	 * @see CommandLinePropertySource#CommandLinePropertySource(Object)
	 */
	public JOptCommandLinePropertySource(OptionSet options) {
		super(options);
	}

	/**
	 * Create a new {@code JOptCommandLinePropertySource} having the given name
	 * and backed by the given {@code OptionSet}.
	 */
	public JOptCommandLinePropertySource(String name, OptionSet options) {
		super(name, options);
	}

	@Override
	protected boolean containsOption(String name) {
		return this.source.has(name);
	}
	
	@Override
	public String[] getPropertyNames() {
		List<String> names = new ArrayList<>();
		for (OptionSpec<?> spec : source.specs()) {
			List<String> aliases = new ArrayList<>(spec.options());
			if (!aliases.isEmpty()) {
				// Only the longest name is used for enumerating
				names.add(aliases.get(aliases.size()-1));				
			}
		}
		return names.toArray(new String[names.size()]);
	}
	
	@Override
	public List<String> getOptionValues(String name) {
		List<?> argValues = this.source.valuesOf(name);
		List<String> stringArgValues = new ArrayList<String>();
		for(Object argValue : argValues) {
			if (!(argValue instanceof String)) {
				throw new IllegalArgumentException("argument values must be of type String");
			}
			stringArgValues.add((String)argValue);
		}
		if (stringArgValues.size() == 0) {
			if (this.source.has(name)) {
				return Collections.emptyList();
			}
			else {
				return null;
			}
		}
		return Collections.unmodifiableList(stringArgValues);
	}

	@Override
	protected List<String> getNonOptionArgs() {
		return this.source.nonOptionArguments();
	}

}
