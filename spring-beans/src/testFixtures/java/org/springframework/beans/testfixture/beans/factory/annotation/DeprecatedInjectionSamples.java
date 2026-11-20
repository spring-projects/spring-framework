/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public abstract class DeprecatedInjectionSamples {

	@Deprecated
	public static class DeprecatedEnvironment {}

	@Deprecated
	public static class DeprecatedSample {

		@Autowired
		Environment environment;

	}

	public static class DeprecatedFieldInjectionPointSample {

		@Autowired
		@Deprecated
		Environment environment;

	}

	public static class DeprecatedFieldInjectionTypeSample {

		@Autowired
		DeprecatedEnvironment environment;
	}

	public static class DeprecatedPrivateFieldInjectionTypeSample {

		@Autowired
		private DeprecatedEnvironment environment;
	}

	public static class DeprecatedMethodInjectionPointSample {

		@Autowired
		@Deprecated
		void setEnvironment(Environment environment) {}
	}

	public static class DeprecatedMethodInjectionTypeSample {

		@Autowired
		void setEnvironment(DeprecatedEnvironment environment) {}
	}

	public static class DeprecatedPrivateMethodInjectionTypeSample {

		@Autowired
		private void setEnvironment(DeprecatedEnvironment environment) {}
	}

}
