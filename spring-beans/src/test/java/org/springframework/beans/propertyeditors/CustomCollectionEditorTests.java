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

package org.springframework.beans.propertyeditors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link CustomCollectionEditor} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class CustomCollectionEditorTests {

	@Test
	public void testCtorWithNullCollectionType() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new CustomCollectionEditor(null));
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testCtorWithNonCollectionType() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new CustomCollectionEditor((Class) String.class));
	}

	@Test
	public void testWithCollectionTypeThatDoesNotExposeAPublicNoArgCtor() throws Exception {
		CustomCollectionEditor editor = new CustomCollectionEditor(CollectionTypeWithNoNoArgCtor.class);
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setValue("1"));
	}

	@Test
	public void testSunnyDaySetValue() throws Exception {
		CustomCollectionEditor editor = new CustomCollectionEditor(ArrayList.class);
		editor.setValue(new int[] {0, 1, 2});
		Object value = editor.getValue();
		assertThat(value).isNotNull();
		boolean condition = value instanceof ArrayList;
		assertThat(condition).isTrue();
		List<?> list = (List<?>) value;
		assertThat(list.size()).as("There must be 3 elements in the converted collection").isEqualTo(3);
		assertThat(list.get(0)).isEqualTo(new Integer(0));
		assertThat(list.get(1)).isEqualTo(new Integer(1));
		assertThat(list.get(2)).isEqualTo(new Integer(2));
	}

	@Test
	public void testWhenTargetTypeIsExactlyTheCollectionInterfaceUsesFallbackCollectionType() throws Exception {
		CustomCollectionEditor editor = new CustomCollectionEditor(Collection.class);
		editor.setValue("0, 1, 2");
		Collection<?> value = (Collection<?>) editor.getValue();
		assertThat(value).isNotNull();
		assertThat(value.size()).as("There must be 1 element in the converted collection").isEqualTo(1);
		assertThat(value.iterator().next()).isEqualTo("0, 1, 2");
	}

	@Test
	public void testSunnyDaySetAsTextYieldsSingleValue() throws Exception {
		CustomCollectionEditor editor = new CustomCollectionEditor(ArrayList.class);
		editor.setValue("0, 1, 2");
		Object value = editor.getValue();
		assertThat(value).isNotNull();
		boolean condition = value instanceof ArrayList;
		assertThat(condition).isTrue();
		List<?> list = (List<?>) value;
		assertThat(list.size()).as("There must be 1 element in the converted collection").isEqualTo(1);
		assertThat(list.get(0)).isEqualTo("0, 1, 2");
	}


	@SuppressWarnings({ "serial", "unused" })
	private static final class CollectionTypeWithNoNoArgCtor extends ArrayList<Object> {
		public CollectionTypeWithNoNoArgCtor(String anArg) {
		}
	}

}
