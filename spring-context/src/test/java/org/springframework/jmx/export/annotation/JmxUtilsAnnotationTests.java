/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.jmx.export.annotation;

import javax.management.MXBean;

import org.junit.jupiter.api.Test;

import org.springframework.jmx.support.JmxUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
public class JmxUtilsAnnotationTests {

	@Test
	public void notMXBean() throws Exception {
		assertThat(JmxUtils.isMBean(FooNotX.class)).as("MXBean annotation not detected correctly").isFalse();
	}

	@Test
	public void annotatedMXBean() throws Exception {
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
