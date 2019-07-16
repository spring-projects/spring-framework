package org.springframework.core.style;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultValueStylerTest {

	private DefaultValueStyler styler = new DefaultValueStyler();

	@Test
	public void style() throws NoSuchMethodException {
		assertThat(styler.style(null)).isEqualTo("[null]");

		assertThat(styler.style("str")).isEqualTo("'str'");

		assertThat(styler.style(String.class)).isEqualTo("String");

		assertThat(styler.style(String.class.getMethod("toString"))).isEqualTo("toString@String");
	}

	@Test
	public void style_plainObject() {
		final Object obj = new Object();

		assertThat(styler.style(obj)).isEqualTo(String.valueOf(obj));
	}

	@Test
	public void style_map() {
		Map<String, Integer> map = Collections.emptyMap();
		assertThat(styler.style(map)).isEqualTo("map[[empty]]");

		map = Collections.singletonMap("key", 1);
		assertThat(styler.style(map)).isEqualTo("map['key' -> 1]");

		map = new HashMap<>();
		map.put("key1", 1);
		map.put("key2", 2);
		assertThat(styler.style(map)).isEqualTo("map['key1' -> 1, 'key2' -> 2]");
	}

	@Test
	public void style_entry() {
		final Map<String, Integer> map = new LinkedHashMap<>();
		map.put("key1", 1);
		map.put("key2", 2);

		final Iterator<Map.Entry<String, Integer>> entries = map.entrySet().iterator();

		assertThat(styler.style(entries.next())).isEqualTo("'key1' -> 1");
		assertThat(styler.style(entries.next())).isEqualTo("'key2' -> 2");
	}

	@Test
	public void style_collection() {
		List<Integer> list = Collections.emptyList();
		assertThat(styler.style(list)).isEqualTo("list[[empty]]");

		list = Collections.singletonList(1);
		assertThat(styler.style(list)).isEqualTo("list[1]");

		list = Arrays.asList(1, 2);
		assertThat(styler.style(list)).isEqualTo("list[1, 2]");
	}

	@Test
	public void style_primitiveArray() {
		int[] array = new int[0];
		assertThat(styler.style(array)).isEqualTo("array<Object>[[empty]]");

		array = new int[]{1};
		assertThat(styler.style(array)).isEqualTo("array<Integer>[1]");

		array = new int[]{1, 2};
		assertThat(styler.style(array)).isEqualTo("array<Integer>[1, 2]");
	}

	@Test
	public void style_objectArray() {
		String[] array = new String[0];
		assertThat(styler.style(array)).isEqualTo("array<String>[[empty]]");

		array = new String[]{"str1"};
		assertThat(styler.style(array)).isEqualTo("array<String>['str1']");

		array = new String[]{"str1", "str2"};
		assertThat(styler.style(array)).isEqualTo("array<String>['str1', 'str2']");
	}

}