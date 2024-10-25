package org.springframework.cglib.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BeanMap}.
 *
 * @author gudrb33333
 */
public class BeanMapTests {
	private BeanMap beanMap;

	private static class TestBean {
		private String name = "Jane Morris Goodall";
		private int age = 90;

		public String getName() {
			return name;
		}

		public int getAge() {
			return age;
		}
	}

	@BeforeEach
	public void setUp() {
		TestBean testBean = new TestBean();
		beanMap = BeanMap.create(testBean);
	}

	@Test
	public void entrySetCorrectSize() {
		Set entrySet = beanMap.entrySet();
		assertEquals(2, entrySet.size());
	}

	@Test
	public void entrySetFieldsCorrectly() {
		Set entrySet = beanMap.entrySet();
		for (Object entryObject : entrySet) {
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObject;

			String key = (String) entry.getKey();
			Object value = entry.getValue();

			if ("name".equals(key)) {
				assertEquals("Jane Morris Goodall", value);
			}
			else if ("age".equals(key)) {
				assertEquals(90, value);
			}
			else {
				fail("Unexpected key: " + key);
			}
		}
	}
}
