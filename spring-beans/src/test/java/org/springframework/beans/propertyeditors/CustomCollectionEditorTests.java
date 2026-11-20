/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void ctorWithNullCollectionType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new CustomCollectionEditor(null));
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void ctorWithNonCollectionType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new CustomCollectionEditor((Class) String.class));
	}

	@Test
	void withCollectionTypeThatDoesNotExposeAPublicNoArgCtor() {
		CustomCollectionEditor editor = new CustomCollectionEditor(CollectionTypeWithNoNoArgCtor.class);
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setValue("1"));
	}

	@Test
	void sunnyDaySetValue() {
		CustomCollectionEditor editor = new CustomCollectionEditor(ArrayList.class);
		editor.setValue(new int[] {0, 1, 2});
		Object value = editor.getValue();
		assertThat(value).isNotNull();
		assertThat(value).isInstanceOf(ArrayList.class);
		assertThat(value).asInstanceOf(LIST).containsExactly(0, 1, 2);
	}

	@Test
	void whenTargetTypeIsExactlyTheCollectionInterfaceUsesFallbackCollectionType() {
		CustomCollectionEditor editor = new CustomCollectionEditor(Collection.class);
		editor.setValue("0, 1, 2");
		Collection<?> value = (Collection<?>) editor.getValue();
		assertThat(value).isNotNull();
		assertThat(value).as("There must be 1 element in the converted collection").hasSize(1);
		assertThat(value).singleElement().isEqualTo("0, 1, 2");
	}

	@Test
	void sunnyDaySetAsTextYieldsSingleValue() {
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
