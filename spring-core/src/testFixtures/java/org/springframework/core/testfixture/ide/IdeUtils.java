/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture.ide;

/**
 * Test utilities related to IDEs.
 *
 * @author Sam Brannen
 * @since 6.2
 */
public class IdeUtils {

	/**
	 * Determine if the current code is running in the Eclipse IDE.
	 */
	public static boolean runningInEclipse() {
		return StackWalker.getInstance().walk(stream -> stream.anyMatch(
				stackFrame -> stackFrame.getClassName().startsWith("org.eclipse.jdt")));
	}

}
