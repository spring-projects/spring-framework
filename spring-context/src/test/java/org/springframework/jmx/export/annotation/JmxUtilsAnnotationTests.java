/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.annotation;

import javax.management.MXBean;

import org.junit.jupiter.api.Test;

import org.springframework.jmx.support.JmxUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class JmxUtilsAnnotationTests {

	@Test
	void notMXBean() {
		assertThat(JmxUtils.isMBean(FooNotX.class)).as("MXBean annotation not detected correctly").isFalse();
	}

	@Test
	void annotatedMXBean() {
		assertThat(JmxUtils.isMBean(FooX.class)).as("MXBean annotation not detected correctly").isTrue();
	}


	@MXBean(false)
	public interface FooNotMXBean {
		String getName();
	}

	public static class FooNotX implements FooNotMXBean {

		@Override
		public String getName() {
			return "Rob Harrop";
		}
	}

	@MXBean(true)
	public interface FooIfc {
		String getName();
	}

	public static class FooX implements FooIfc {

		@Override
		public String getName() {
			return "Rob Harrop";
		}
	}

}
