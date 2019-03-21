/*
 * Copyright 2002-2011 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple representation of command line arguments, broken into "option arguments" and
 * "non-option arguments".
 *
 * @author Chris Beams
 * @since 3.1
 * @see SimpleCommandLineArgsParser
 */
class CommandLineArgs {

	private final Map<String, List<String>> optionArgs = new HashMap<String, List<String>>();
	private final List<String> nonOptionArgs = new ArrayList<String>();

	/**
	 * Add an option argument for the given option name and add the given value to the
	 * list of values associated with this option (of which there may be zero or more).
	 * The given value may be {@code null}, indicating that the option was specified
	 * without an associated value (e.g. "--foo" vs. "--foo=bar").
	 */
	public void addOptionArg(String optionName, String optionValue) {
		if (!this.optionArgs.containsKey(optionName)) {
			this.optionArgs.put(optionName, new ArrayList<String>());
		}
		if (optionValue != null) {
			this.optionArgs.get(optionName).add(optionValue);
		}
	}

	/**
	 * Return the set of all option arguments present on the command line.
	 */
	public Set<String> getOptionNames() {
		return Collections.unmodifiableSet(this.optionArgs.keySet());
	}

	/**
	 * Return whether the option with the given name was present on the command line.
	 */
	public boolean containsOption(String optionName) {
		return this.optionArgs.containsKey(optionName);
	}

	/**
	 * Return the list of values associated with the given option. {@code null} signifies
	 * that the option was not present; empty list signifies that no values were associated
	 * with this option.
	 */
	public List<String> getOptionValues(String optionName) {
		return this.optionArgs.get(optionName);
	}

	/**
	 * Add the given value to the list of non-option arguments.
	 */
	public void addNonOptionArg(String value) {
		this.nonOptionArgs.add(value);
	}

	/**
	 * Return the list of non-option arguments specified on the command line.
	 */
	public List<String> getNonOptionArgs() {
		return Collections.unmodifiableList(this.nonOptionArgs);
	}

}
