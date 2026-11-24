/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

		// The method names are intentionally not used as string literals here: the
		// JVM would intern them when loading this test class, before ReverseOrder,
		// which would make getDeclaredMethods() return them in sorted order.
		assertThat(methods.subList(0, 6)).extracting(Method::getName).isSorted();
	}

	@Test
	void addAllMethodsSortsObjectMethods() {
		List<Method> methods = new ArrayList<>();
		ReflectUtils.addAllMethods(Object.class, methods);

		assertThat(methods).extracting(Method::getName)
				.containsExactly("clone", "equals", "hashCode", "toString");
	}


	/**
	 * {@link Class#getDeclaredMethods()} returns methods in no particular
	 * order. HotSpot sorts them by the memory address of their names, not
	 * alphabetically.
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
