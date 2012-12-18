package org.springframework.web.servlet.tags.form;

/**
 * Test related data types for {@code org.springframework.web.servlet.tags.form} package.
 *
 * @author Scott Andrews
 */
public class TestTypes { }

class BeanWithEnum {

	private TestEnum testEnum;

	public TestEnum getTestEnum() {
		return testEnum;
	}

	public void setTestEnum(TestEnum customEnum) {
		this.testEnum = customEnum;
	}

}

enum TestEnum {

	VALUE_1, VALUE_2;

	public String getEnumLabel() {
		return "Label: " + name();
	}

	public String getEnumValue() {
		return "Value: " + name();
	}

	public String toString() {
		return "TestEnum: " + name();
	}

}