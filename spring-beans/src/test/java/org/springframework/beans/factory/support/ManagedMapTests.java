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
public class ManagedMapTests extends TestCase {

	public void testMergeSunnyDay() {
		ManagedMap<String, String> parent = new ManagedMap<String, String>();
		parent.put("one", "one");
		parent.put("two", "two");
		ManagedMap<String, String> child = new ManagedMap<String, String>();
		child.put("three", "three");
		child.setMergeEnabled(true);
		@SuppressWarnings("unchecked")
		Map<String, String> mergedMap = (Map<String, String>) child.merge(parent);
		assertEquals("merge() obviously did not work.", 3, mergedMap.size());
	}

	public void testMergeWithNullParent() {
		ManagedMap<String, String> child = new ManagedMap<String, String>();
		child.setMergeEnabled(true);
		assertSame(child, child.merge(null));
	}

	public void testMergeWithNonCompatibleParentType() {
		ManagedMap<String, String> map = new ManagedMap<String, String>();
		map.setMergeEnabled(true);
		try {
			map.merge("hello");
			fail("Must have failed by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testMergeNotAllowedWhenMergeNotEnabled() {
		ManagedMap<String, String> map = new ManagedMap<String, String>();
		try {
			map.merge(null);
			fail("Must have failed by this point (cannot merge() when the mergeEnabled property is false.");
		}
		catch (IllegalStateException expected) {
		}
	}

	public void testMergeEmptyChild() {
		ManagedMap<String, String> parent = new ManagedMap<String, String>();
		parent.put("one", "one");
		parent.put("two", "two");
		ManagedMap<String, String> child = new ManagedMap<String, String>();
		child.setMergeEnabled(true);
		@SuppressWarnings("unchecked")
		Map<String, String> mergedMap = (Map<String, String>) child.merge(parent);
		assertEquals("merge() obviously did not work.", 2, mergedMap.size());
	}

	public void testMergeChildValuesOverrideTheParents() {
		ManagedMap<String, String> parent = new ManagedMap<String, String>();
		parent.put("one", "one");
		parent.put("two", "two");
		ManagedMap<String, String> child = new ManagedMap<String, String>();
		child.put("one", "fork");
		child.setMergeEnabled(true);
		@SuppressWarnings("unchecked")
		Map<String, String> mergedMap = (Map<String, String>) child.merge(parent);
		// child value for 'one' must override parent value...
		assertEquals("merge() obviously did not work.", 2, mergedMap.size());
		assertEquals("Parent value not being overridden during merge().", "fork", mergedMap.get("one"));
	}

}
