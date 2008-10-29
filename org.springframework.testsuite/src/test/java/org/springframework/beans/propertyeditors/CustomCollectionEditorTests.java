/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import junit.framework.TestCase;
import org.springframework.test.AssertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

/**
 * Unit tests for the {@link CustomCollectionEditor} class.
 *
 * @author Rick Evans
 */
public final class CustomCollectionEditorTests extends TestCase {

	public void testCtorWithNullCollectionType() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new CustomCollectionEditor(null);
			}
		}.runTest();
	}

	public void testCtorWithNonCollectionType() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new CustomCollectionEditor(String.class);
			}
		}.runTest();
	}

	public void testWithCollectionTypeThatDoesNotExposeAPublicNoArgCtor() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				CustomCollectionEditor editor = new CustomCollectionEditor(CollectionTypeWithNoNoArgCtor.class);
				editor.setValue("1");
			}
		}.runTest();
	}

	public void testSunnyDaySetValue() throws Exception {
		CustomCollectionEditor editor = new CustomCollectionEditor(ArrayList.class);
		editor.setValue(new int[] {0, 1, 2});
		Object value = editor.getValue();
		assertNotNull(value);
		assertTrue(value instanceof ArrayList);
		List list = (List) value;
		assertEquals("There must be 3 elements in the converted collection", 3, list.size());
		assertEquals(new Integer(0), list.get(0));
		assertEquals(new Integer(1), list.get(1));
		assertEquals(new Integer(2), list.get(2));
	}

	public void testWhenTargetTypeIsExactlyTheCollectionInterfaceUsesFallbackCollectionType() throws Exception {
		CustomCollectionEditor editor = new CustomCollectionEditor(Collection.class);
		editor.setValue("0, 1, 2");
		Collection value = (Collection) editor.getValue();
		assertNotNull(value);
		assertEquals("There must be 1 element in the converted collection", 1, value.size());
		assertEquals("0, 1, 2", value.iterator().next());
	}

	public void testSunnyDaySetAsTextYieldsSingleValue() throws Exception {
		CustomCollectionEditor editor = new CustomCollectionEditor(ArrayList.class);
		editor.setValue("0, 1, 2");
		Object value = editor.getValue();
		assertNotNull(value);
		assertTrue(value instanceof ArrayList);
		List list = (List) value;
		assertEquals("There must be 1 element in the converted collection", 1, list.size());
		assertEquals("0, 1, 2", list.get(0));
	}


	private static final class CollectionTypeWithNoNoArgCtor extends ArrayList {

		public CollectionTypeWithNoNoArgCtor(String anArg) {
		}
	}

}
