/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.cglib.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReflectUtils}.
 *
 * @author Aman Sharma
 */
class ReflectUtilsTests {

	@Test
	void addAllMethodsSortsDeclaredMethods() {
		List<Method> methods = new ArrayList<>();
		ReflectUtils.addAllMethods(ReverseOrder.class, methods);

		// The names are not spelled out here, because string literals would create them in sorted order
		// before the JVM loads ReverseOrder.
		assertThat(methods.subList(0, 6)).extracting(Method::getName).isSorted();
	}


	/**
	 * {@link Class#getDeclaredMethods()} returns methods in no particular order.
	 * HotSpot sorts them by the memory address of their names, not alphabetically.
	 */
	public static class ReverseOrder {

		public void methodOrderFoxtrot() {
		}

		public void methodOrderEcho() {
		}

		public void methodOrderDelta() {
		}

		public void methodOrderCharlie() {
		}

		public void methodOrderBravo() {
		}

		public void methodOrderAlpha() {
		}
	}

}
