/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.support;

import java.util.Map;

import junit.framework.TestCase;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class ManagedPropertiesTests extends TestCase {

	public void testMergeSunnyDay() {
		ManagedProperties parent = new ManagedProperties();
		parent.setProperty("one", "one");
		parent.setProperty("two", "two");
		ManagedProperties child = new ManagedProperties();
		child.setProperty("three", "three");
		child.setMergeEnabled(true);
		@SuppressWarnings("unchecked")
		Map<Object, Object> mergedMap = (Map<Object, Object>) child.merge(parent);
		assertEquals("merge() obviously did not work.", 3, mergedMap.size());
	}

	public void testMergeWithNullParent() {
		ManagedProperties child = new ManagedProperties();
		child.setMergeEnabled(true);
		assertSame(child, child.merge(null));
	}

	public void testMergeWithNonCompatibleParentType() {
		ManagedProperties map = new ManagedProperties();
		map.setMergeEnabled(true);
		try {
			map.merge("hello");
			fail("Must have failed by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testMergeNotAllowedWhenMergeNotEnabled() {
		ManagedProperties map = new ManagedProperties();
		try {
			map.merge(null);
			fail("Must have failed by this point (cannot merge() when the mergeEnabled property is false.");
		}
		catch (IllegalStateException expected) {
		}
	}

	public void testMergeEmptyChild() {
		ManagedProperties parent = new ManagedProperties();
		parent.setProperty("one", "one");
		parent.setProperty("two", "two");
		ManagedProperties child = new ManagedProperties();
		child.setMergeEnabled(true);
		@SuppressWarnings("unchecked")
		Map<Object, Object> mergedMap = (Map<Object, Object>) child.merge(parent);
		assertEquals("merge() obviously did not work.", 2, mergedMap.size());
	}

	public void testMergeChildValuesOverrideTheParents() {
		ManagedProperties parent = new ManagedProperties();
		parent.setProperty("one", "one");
		parent.setProperty("two", "two");
		ManagedProperties child = new ManagedProperties();
		child.setProperty("one", "fork");
		child.setMergeEnabled(true);
		@SuppressWarnings("unchecked")
		Map<Object, Object> mergedMap = (Map<Object, Object>) child.merge(parent);
		// child value for 'one' must override parent value...
		assertEquals("merge() obviously did not work.", 2, mergedMap.size());
		assertEquals("Parent value not being overridden during merge().", "fork", mergedMap.get("one"));
	}

}
