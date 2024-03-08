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

package org.springframework.core.env;

/**
 * Parses a {@code String[]} of command line arguments in order to populate a
 * {@link CommandLineArgs} object.
 *
 * <h3>Working with option arguments</h3>
 * <p>Option arguments must adhere to the exact syntax:
 *
 * <pre class="code">--optName[=optValue]</pre>
 *
 * <p>That is, options must be prefixed with "{@code --}" and may or may not
 * specify a value. If a value is specified, the name and value must be separated
 * <em>without spaces</em> by an equals sign ("="). The value may optionally be
 * an empty string.
 *
 * <h4>Valid examples of option arguments</h4>
 * <pre class="code">
 * --foo
 * --foo=
 * --foo=""
 * --foo=bar
 * --foo="bar then baz"
 * --foo=bar,baz,biz</pre>
 *
 * <h4>Invalid examples of option arguments</h4>
 * <pre class="code">
 * -foo
 * --foo bar
 * --foo = bar
 * --foo=bar --foo=baz --foo=biz</pre>
 *
 * <h3>End of option arguments</h3>
 * <p>This parser supports the POSIX "end of options" delimiter, meaning that any
 * {@code "--"} (empty option name) in the command line signals that all remaining
 * arguments are non-option arguments. For example, {@code "--opt1=ignored"},
 * {@code "--opt2"}, and {@code "filename"} in the following command line are
 * considered non-option arguments.
 * <pre class="code">
 * --foo=bar -- --opt1=ignored -opt2 filename</pre>
 *
 * <h3>Working with non-option arguments</h3>
 * <p>Any arguments following the "end of options" delimiter ({@code --}) or
 * specified without the "{@code --}" option prefix will be considered as
 * "non-option arguments" and made available through the
 * {@link CommandLineArgs#getNonOptionArgs()} method.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 3.1
 * @see SimpleCommandLinePropertySource
 */
class SimpleCommandLineArgsParser {

	/**
	 * Parse the given {@code String} array based on the rules described {@linkplain
	 * SimpleCommandLineArgsParser above}, returning a fully-populated
	 * {@link CommandLineArgs} object.
	 * @param args command line arguments, typically from a {@code main()} method
	 */
	public CommandLineArgs parse(String... args) {
		CommandLineArgs commandLineArgs = new CommandLineArgs();
		boolean endOfOptions = false;
		for (String arg : args) {
			if (!endOfOptions && arg.startsWith("--")) {
				String optionText = arg.substring(2);
				int indexOfEqualsSign = optionText.indexOf('=');
				if (indexOfEqualsSign > -1) {
					String optionName = optionText.substring(0, indexOfEqualsSign);
					String optionValue = optionText.substring(indexOfEqualsSign + 1);
					if (optionName.isEmpty()) {
						throw new IllegalArgumentException("Invalid argument syntax: " + arg);
					}
					commandLineArgs.addOptionArg(optionName, optionValue);
				}
				else if (!optionText.isEmpty()){
					commandLineArgs.addOptionArg(optionText, null);
				}
				else {
					// '--' End of options delimiter, all remaining args are non-option arguments
					endOfOptions = true;
				}
			}
			else {
				commandLineArgs.addNonOptionArg(arg);
			}
		}
		return commandLineArgs;
	}

}
