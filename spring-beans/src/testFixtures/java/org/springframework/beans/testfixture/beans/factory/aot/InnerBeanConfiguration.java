/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.aot;

/**
 * A configuration with inner classes.
 *
 * @author Stephane Nicoll
 */
public class InnerBeanConfiguration {

	public static class Simple {

		public SimpleBean simpleBean() {
			return new SimpleBean();
		}

		public static class Another {

			public SimpleBean anotherBean() {
				return new SimpleBean();
			}

		}
	}
}
