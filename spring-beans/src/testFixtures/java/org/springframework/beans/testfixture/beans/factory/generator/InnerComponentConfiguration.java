/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.generator;

import org.springframework.core.env.Environment;

public class InnerComponentConfiguration {

	public class NoDependencyComponent {

		public NoDependencyComponent() {
		}
	}

	public class EnvironmentAwareComponent {

		final Environment environment;

		public EnvironmentAwareComponent(Environment environment) {
			this.environment = environment;
		}
	}

	public class NoDependencyComponentWithoutPublicConstructor {

		NoDependencyComponentWithoutPublicConstructor() {
		}
	}

	public class EnvironmentAwareComponentWithoutPublicConstructor {

		final Environment environment;

		EnvironmentAwareComponentWithoutPublicConstructor(Environment environment) {
			this.environment = environment;
		}
	}

}
