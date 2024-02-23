/*
 * Copyright 2002-2024 the original author or authors.
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
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;

/**
 * Tests for {@link CustomCollectionEditor}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
class CustomCollectionEditorTests {

	@Test
	void testCtorWithNullCollectionType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new CustomCollectionEditor(null));
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testCtorWithNonCollectionType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new CustomCollectionEditor((Class) String.class));
	}

	@Test
	void testWithCollectionTypeThatDoesNotExposeAPublicNoArgCtor() {
		CustomCollectionEditor editor = new CustomCollectionEditor(CollectionTypeWithNoNoArgCtor.class);
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setValue("1"));
	}

	@Test
	void testSunnyDaySetValue() {
		CustomCollectionEditor editor = new CustomCollectionEditor(ArrayList.class);
		editor.setValue(new int[] {0, 1, 2});
		Object value = editor.getValue();
		assertThat(value).isNotNull();
		assertThat(value).isInstanceOf(ArrayList.class);
		assertThat(value).asInstanceOf(LIST).containsExactly(0, 1, 2);
	}

	@Test
	void testWhenTargetTypeIsExactlyTheCollectionInterfaceUsesFallbackCollectionType() {
		CustomCollectionEditor editor = new CustomCollectionEditor(Collection.class);
		editor.setValue("0, 1, 2");
		Collection<?> value = (Collection<?>) editor.getValue();
		assertThat(value).isNotNull();
		assertThat(value).as("There must be 1 element in the converted collection").hasSize(1);
		assertThat(value).singleElement().isEqualTo("0, 1, 2");
	}

	@Test
	void testSunnyDaySetAsTextYieldsSingleValue() {
		CustomCollectionEditor editor = new CustomCollectionEditor(ArrayList.class);
		editor.setValue("0, 1, 2");
		Object value = editor.getValue();
		assertThat(value).isNotNull();
		assertThat(value).isInstanceOf(ArrayList.class);
		List<?> list = (List<?>) value;
		assertThat(list).as("There must be 1 element in the converted collection").hasSize(1);
		assertThat(list).singleElement().isEqualTo("0, 1, 2");
	}


	@SuppressWarnings({ "serial", "unused" })
	private static final class CollectionTypeWithNoNoArgCtor extends ArrayList<Object> {
		public CollectionTypeWithNoNoArgCtor(String anArg) {
		}
	}

}
