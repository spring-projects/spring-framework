/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.agent;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.util.StringUtils;

/**
 * Java Agent that records method invocations related to {@link RuntimeHints} metadata.
 * <p>This agent uses {@link java.lang.instrument.ClassFileTransformer class transformers}
 * that modify bytecode to intercept and record method invocations at runtime.
 * <p>By default, this agent only instruments code in the {@code org.springframework} package.
 * Instrumented packages can be configured by passing an argument string to the {@code -javaagent}
 * option, as a comma-separated list of packages to instrument prefixed with {@code "+"}
 * and packages to ignore prefixed with {@code "-"}:
 * <pre class="code">
 *   -javaagent:/path/to/spring-core-test.jar=+org.springframework,-io.spring,+org.example")
 * </pre>
 *
 * @author Brian Clozel
 * @since 6.0
 * @see InvocationsRecorderClassTransformer
 * @deprecated as of 7.0 in favor of the {@code -XX:MissingRegistrationReportingMode=Warn} and
 * {@code -XX:MissingRegistrationReportingMode=Exit} JVM flags with GraalVM.
 */
@Deprecated(since = "7.0", forRemoval = true)
public final class RuntimeHintsAgent {

	private static boolean loaded = false;

	private RuntimeHintsAgent() {
	}

	public static void premain(@Nullable String agentArgs, Instrumentation inst) {
		loaded = true;
		ParsedArguments arguments = ParsedArguments.parse(agentArgs);
		InvocationsRecorderClassTransformer transformer = new InvocationsRecorderClassTransformer(
				arguments.getInstrumentedPackages(), arguments.getIgnoredPackages());
		inst.addTransformer(transformer);
	}

	/**
	 * Static accessor for detecting whether the agent is loaded in the current JVM.
	 * @return whether the agent is active for the current JVM
	 */
	public static boolean isLoaded() {
		return loaded;
	}


	private static final class ParsedArguments {

		List<String> instrumentedPackages;

		List<String> ignoredPackages;

		private ParsedArguments(List<String> instrumentedPackages, List<String> ignoredPackages) {
			this.instrumentedPackages = instrumentedPackages;
			this.ignoredPackages = ignoredPackages;
		}

		public String[] getInstrumentedPackages() {
			return this.instrumentedPackages.toArray(new String[0]);
		}

		public String[] getIgnoredPackages() {
			return this.ignoredPackages.toArray(new String[0]);
		}

		static ParsedArguments parse(@Nullable String agentArgs) {
			List<String> included = new ArrayList<>();
			List<String> excluded = new ArrayList<>();
			if (StringUtils.hasText(agentArgs)) {
				for(String argument : agentArgs.split(",")) {
					if (argument.startsWith("+")) {
						included.add(argument.substring(1));
					}
					else if (argument.startsWith("-")) {
						excluded.add(argument.substring(1));
					}
					else {
						throw new IllegalArgumentException("Cannot parse agent arguments ["+agentArgs+"]");
					}
				}
			}
			else {
				included.add("org.springframework");
			}
			return new ParsedArguments(included, excluded);
		}
	}

}
