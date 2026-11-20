/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.aot;

/**
 * A hierarchy where the exposed type of a bean is a partial signature.
 *
 * @author Stephane Nicoll
 */
public class TestHierarchy {

	public interface One {
	}

	public interface Two {
	}

	public static class Implementation implements One, Two {
	}

	public static One oneBean() {
		return new Implementation();
	}

}
