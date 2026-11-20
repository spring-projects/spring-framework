/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.nativex;

import java.util.function.Consumer;

import org.springframework.aot.hint.RuntimeHints;

/**
 * Write {@link RuntimeHints} as GraalVM native configuration.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Janne Valkealahti
 * @author Brian Clozel
 * @since 6.0
 * @see <a href="https://www.graalvm.org/jdk23/reference-manual/native-image/overview/BuildConfiguration/">Native Image Build Configuration</a>
 */
public abstract class NativeConfigurationWriter {

	/**
	 * Write the GraalVM native configuration from the provided hints.
	 * @param hints the hints to handle
	 */
	public void write(RuntimeHints hints) {
		if (hasAnyHint(hints)) {
			writeTo("reachability-metadata.json",
					writer -> new RuntimeHintsWriter().write(writer, hints));
		}
	}

	private boolean hasAnyHint(RuntimeHints hints) {
		return (hints.proxies().jdkProxyHints().findAny().isPresent() ||
				hints.reflection().typeHints().findAny().isPresent() ||
				hints.reflection().lambdaHints().findAny().isPresent() ||
				hints.resources().resourcePatternHints().findAny().isPresent() ||
				hints.resources().resourceBundleHints().findAny().isPresent() ||
				hints.jni().typeHints().findAny().isPresent() ||
				hasAnyDeprecatedHint(hints));
	}

	@SuppressWarnings("removal")
	private boolean hasAnyDeprecatedHint(RuntimeHints hints) {
		return hints.serialization().javaSerializationHints().findAny().isPresent();
	}

	/**
	 * Write the specified GraalVM native configuration file, using the
	 * provided {@link BasicJsonWriter}.
	 * @param fileName the name of the file
	 * @param writer a consumer for the writer to use
	 */
	protected abstract void writeTo(String fileName, Consumer<BasicJsonWriter> writer);

}
