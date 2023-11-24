/*
 * Copyright 2002-2023 the original author or authors.
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

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.beans.PropertyVetoException;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.testfixture.beans.BooleanTestBean;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.IndexedTestBean;
import org.springframework.beans.testfixture.beans.NumberTestBean;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for the various PropertyEditors in Spring.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Rob Harrop
 * @author Arjen Poutsma
 * @author Chris Beams
 * @since 10.06.2003
 */
class CustomEditorTests {

	@Test
	void testComplexObject() {
		TestBean tb = new TestBean();
		String newName = "Rod";
		String tbString = "Kerry_34";

		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(ITestBean.class, new TestBeanEditor());
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("age", 55));
		pvs.addPropertyValue(new PropertyValue("name", newName));
		pvs.addPropertyValue(new PropertyValue("touchy", "valid"));
		pvs.addPropertyValue(new PropertyValue("spouse", tbString));
		bw.setPropertyValues(pvs);
		assertThat(tb.getSpouse()).as("spouse is non-null").isNotNull();
		assertThat(tb.getSpouse().getName().equals("Kerry") && tb.getSpouse().getAge() == 34).as("spouse name is Kerry and age is 34").isTrue();
	}

	@Test
	void testComplexObjectWithOldValueAccess() {
		TestBean tb = new TestBean();
		String newName = "Rod";
		String tbString = "Kerry_34";

		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.setExtractOldValueForEditor(true);
		bw.registerCustomEditor(ITestBean.class, new OldValueAccessingTestBeanEditor());
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("age", 55));
		pvs.addPropertyValue(new PropertyValue("name", newName));
		pvs.addPropertyValue(new PropertyValue("touchy", "valid"));
		pvs.addPropertyValue(new PropertyValue("spouse", tbString));

		bw.setPropertyValues(pvs);
		assertThat(tb.getSpouse()).as("spouse is non-null").isNotNull();
		assertThat(tb.getSpouse().getName().equals("Kerry") && tb.getSpouse().getAge() == 34).as("spouse name is Kerry and age is 34").isTrue();
		ITestBean spouse = tb.getSpouse();

		bw.setPropertyValues(pvs);
		assertThat(tb.getSpouse()).as("Should have remained same object").isSameAs(spouse);
	}

	@Test
	void testCustomEditorForSingleProperty() {
		TestBean tb = new TestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(String.class, "name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("prefix" + text);
			}
		});
		bw.setPropertyValue("name", "value");
		bw.setPropertyValue("touchy", "value");
		assertThat(bw.getPropertyValue("name")).isEqualTo("prefixvalue");
		assertThat(tb.getName()).isEqualTo("prefixvalue");
		assertThat(bw.getPropertyValue("touchy")).isEqualTo("value");
		assertThat(tb.getTouchy()).isEqualTo("value");
	}

	@Test
	void testCustomEditorForAllStringProperties() {
		TestBean tb = new TestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(String.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("prefix" + text);
			}
		});
		bw.setPropertyValue("name", "value");
		bw.setPropertyValue("touchy", "value");
		assertThat(bw.getPropertyValue("name")).isEqualTo("prefixvalue");
		assertThat(tb.getName()).isEqualTo("prefixvalue");
		assertThat(bw.getPropertyValue("touchy")).isEqualTo("prefixvalue");
		assertThat(tb.getTouchy()).isEqualTo("prefixvalue");
	}

	@Test
	void testCustomEditorForSingleNestedProperty() {
		TestBean tb = new TestBean();
		tb.setSpouse(new TestBean());
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(String.class, "spouse.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("prefix" + text);
			}
		});
		bw.setPropertyValue("spouse.name", "value");
		bw.setPropertyValue("touchy", "value");
		assertThat(bw.getPropertyValue("spouse.name")).isEqualTo("prefixvalue");
		assertThat(tb.getSpouse().getName()).isEqualTo("prefixvalue");
		assertThat(bw.getPropertyValue("touchy")).isEqualTo("value");
		assertThat(tb.getTouchy()).isEqualTo("value");
	}

	@Test
	void testCustomEditorForAllNestedStringProperties() {
		TestBean tb = new TestBean();
		tb.setSpouse(new TestBean());
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(String.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("prefix" + text);
			}
		});
		bw.setPropertyValue("spouse.name", "value");
		bw.setPropertyValue("touchy", "value");
		assertThat(bw.getPropertyValue("spouse.name")).isEqualTo("prefixvalue");
		assertThat(tb.getSpouse().getName()).isEqualTo("prefixvalue");
		assertThat(bw.getPropertyValue("touchy")).isEqualTo("prefixvalue");
		assertThat(tb.getTouchy()).isEqualTo("prefixvalue");
	}

	@Test
	void testDefaultBooleanEditorForPrimitiveType() {
		BooleanTestBean tb = new BooleanTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);

		bw.setPropertyValue("bool1", "true");
		assertThat(Boolean.TRUE.equals(bw.getPropertyValue("bool1"))).as("Correct bool1 value").isTrue();
		assertThat(tb.isBool1()).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "false");
		assertThat(Boolean.FALSE.equals(bw.getPropertyValue("bool1"))).as("Correct bool1 value").isTrue();
		assertThat(!tb.isBool1()).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "  true  ");
		assertThat(tb.isBool1()).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "  false  ");
		assertThat(!tb.isBool1()).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "on");
		assertThat(tb.isBool1()).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "off");
		assertThat(!tb.isBool1()).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "yes");
		assertThat(tb.isBool1()).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "no");
		assertThat(!tb.isBool1()).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "1");
		assertThat(tb.isBool1()).as("Correct bool1 value").isTrue();

		bw.setPropertyValue("bool1", "0");
		assertThat(!tb.isBool1()).as("Correct bool1 value").isTrue();

		assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
				bw.setPropertyValue("bool1", "argh"));
	}

	@Test
	void testDefaultBooleanEditorForWrapperType() {
		BooleanTestBean tb = new BooleanTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);

		bw.setPropertyValue("bool2", "true");
		assertThat(Boolean.TRUE.equals(bw.getPropertyValue("bool2"))).as("Correct bool2 value").isTrue();
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "false");
		assertThat(Boolean.FALSE.equals(bw.getPropertyValue("bool2"))).as("Correct bool2 value").isTrue();
		assertThat(!tb.getBool2()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "on");
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "off");
		assertThat(!tb.getBool2()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "yes");
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "no");
		assertThat(!tb.getBool2()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "1");
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "0");
		assertThat(!tb.getBool2()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "");
		assertThat(tb.getBool2()).as("Correct bool2 value").isNull();
	}

	@Test
	void testCustomBooleanEditorWithAllowEmpty() {
		BooleanTestBean tb = new BooleanTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(Boolean.class, new CustomBooleanEditor(true));

		bw.setPropertyValue("bool2", "true");
		assertThat(Boolean.TRUE.equals(bw.getPropertyValue("bool2"))).as("Correct bool2 value").isTrue();
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "false");
		assertThat(Boolean.FALSE.equals(bw.getPropertyValue("bool2"))).as("Correct bool2 value").isTrue();
		assertThat(!tb.getBool2()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "on");
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "off");
		assertThat(!tb.getBool2()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "yes");
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "no");
		assertThat(!tb.getBool2()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "1");
		assertThat(tb.getBool2().booleanValue()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "0");
		assertThat(!tb.getBool2()).as("Correct bool2 value").isTrue();

		bw.setPropertyValue("bool2", "");
		assertThat(bw.getPropertyValue("bool2")).as("Correct bool2 value").isNull();
		assertThat(tb.getBool2()).as("Correct bool2 value").isNull();
	}

	@Test
	void testCustomBooleanEditorWithSpecialTrueAndFalseStrings() {
		String trueString = "pechorin";
		String falseString = "nash";

		CustomBooleanEditor editor = new CustomBooleanEditor(trueString, falseString, false);

		editor.setAsText(trueString);
		assertThat(((Boolean) editor.getValue()).booleanValue()).isTrue();
		assertThat(editor.getAsText()).isEqualTo(trueString);
		editor.setAsText(falseString);
		assertThat(((Boolean) editor.getValue()).booleanValue()).isFalse();
		assertThat(editor.getAsText()).isEqualTo(falseString);

		editor.setAsText(trueString.toUpperCase());
		assertThat(((Boolean) editor.getValue()).booleanValue()).isTrue();
		assertThat(editor.getAsText()).isEqualTo(trueString);
		editor.setAsText(falseString.toUpperCase());
		assertThat(((Boolean) editor.getValue()).booleanValue()).isFalse();
		assertThat(editor.getAsText()).isEqualTo(falseString);
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText(null));
	}

	@Test
	void testDefaultNumberEditor() {
		NumberTestBean tb = new NumberTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);

		bw.setPropertyValue("short1", "1");
		bw.setPropertyValue("short2", "2");
		bw.setPropertyValue("int1", "7");
		bw.setPropertyValue("int2", "8");
		bw.setPropertyValue("long1", "5");
		bw.setPropertyValue("long2", "6");
		bw.setPropertyValue("bigInteger", "3");
		bw.setPropertyValue("float1", "7.1");
		bw.setPropertyValue("float2", "8.1");
		bw.setPropertyValue("double1", "5.1");
		bw.setPropertyValue("double2", "6.1");
		bw.setPropertyValue("bigDecimal", "4.5");

		assertThat(Short.valueOf("1")).as("Correct short1 value").isEqualTo(bw.getPropertyValue("short1"));
		assertThat(tb.getShort1()).as("Correct short1 value").isOne();
		assertThat(Short.valueOf("2")).as("Correct short2 value").isEqualTo(bw.getPropertyValue("short2"));
		assertThat(Short.valueOf("2")).as("Correct short2 value").isEqualTo(tb.getShort2());
		assertThat(Integer.valueOf("7")).as("Correct int1 value").isEqualTo(bw.getPropertyValue("int1"));
		assertThat(tb.getInt1()).as("Correct int1 value").isEqualTo(7);
		assertThat(Integer.valueOf("8")).as("Correct int2 value").isEqualTo(bw.getPropertyValue("int2"));
		assertThat(Integer.valueOf("8")).as("Correct int2 value").isEqualTo(tb.getInt2());
		assertThat(Long.valueOf("5")).as("Correct long1 value").isEqualTo(bw.getPropertyValue("long1"));
		assertThat(tb.getLong1()).as("Correct long1 value").isEqualTo(5);
		assertThat(Long.valueOf("6")).as("Correct long2 value").isEqualTo(bw.getPropertyValue("long2"));
		assertThat(Long.valueOf("6")).as("Correct long2 value").isEqualTo(tb.getLong2());
		assertThat(new BigInteger("3")).as("Correct bigInteger value").isEqualTo(bw.getPropertyValue("bigInteger"));
		assertThat(new BigInteger("3")).as("Correct bigInteger value").isEqualTo(tb.getBigInteger());
		assertThat(Float.valueOf("7.1")).as("Correct float1 value").isEqualTo(bw.getPropertyValue("float1"));
		assertThat(Float.valueOf("7.1")).as("Correct float1 value").isEqualTo(tb.getFloat1());
		assertThat(Float.valueOf("8.1")).as("Correct float2 value").isEqualTo(bw.getPropertyValue("float2"));
		assertThat(Float.valueOf("8.1")).as("Correct float2 value").isEqualTo(tb.getFloat2());
		assertThat(Double.valueOf("5.1")).as("Correct double1 value").isEqualTo(bw.getPropertyValue("double1"));
		assertThat(tb.getDouble1()).as("Correct double1 value").isEqualTo(5.1);
		assertThat(Double.valueOf("6.1")).as("Correct double2 value").isEqualTo(bw.getPropertyValue("double2"));
		assertThat(Double.valueOf("6.1")).as("Correct double2 value").isEqualTo(tb.getDouble2());
		assertThat(new BigDecimal("4.5")).as("Correct bigDecimal value").isEqualTo(bw.getPropertyValue("bigDecimal"));
		assertThat(new BigDecimal("4.5")).as("Correct bigDecimal value").isEqualTo(tb.getBigDecimal());
	}

	@Test
	void testCustomNumberEditorWithoutAllowEmpty() {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.GERMAN);
		NumberTestBean tb = new NumberTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(short.class, new CustomNumberEditor(Short.class, nf, false));
		bw.registerCustomEditor(Short.class, new CustomNumberEditor(Short.class, nf, false));
		bw.registerCustomEditor(int.class, new CustomNumberEditor(Integer.class, nf, false));
		bw.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, nf, false));
		bw.registerCustomEditor(long.class, new CustomNumberEditor(Long.class, nf, false));
		bw.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, nf, false));
		bw.registerCustomEditor(BigInteger.class, new CustomNumberEditor(BigInteger.class, nf, false));
		bw.registerCustomEditor(float.class, new CustomNumberEditor(Float.class, nf, false));
		bw.registerCustomEditor(Float.class, new CustomNumberEditor(Float.class, nf, false));
		bw.registerCustomEditor(double.class, new CustomNumberEditor(Double.class, nf, false));
		bw.registerCustomEditor(Double.class, new CustomNumberEditor(Double.class, nf, false));
		bw.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, nf, false));

		bw.setPropertyValue("short1", "1");
		bw.setPropertyValue("short2", "2");
		bw.setPropertyValue("int1", "7");
		bw.setPropertyValue("int2", "8");
		bw.setPropertyValue("long1", "5");
		bw.setPropertyValue("long2", "6");
		bw.setPropertyValue("bigInteger", "3");
		bw.setPropertyValue("float1", "7,1");
		bw.setPropertyValue("float2", "8,1");
		bw.setPropertyValue("double1", "5,1");
		bw.setPropertyValue("double2", "6,1");
		bw.setPropertyValue("bigDecimal", "4,5");

		assertThat(bw.getPropertyValue("short1")).as("Correct short1 value").isEqualTo(Short.valueOf("1"));
		assertThat(tb.getShort1()).as("Correct short1 value").isOne();
		assertThat(bw.getPropertyValue("short2")).as("Correct short2 value").isEqualTo(Short.valueOf("2"));
		assertThat(tb.getShort2()).as("Correct short2 value").isEqualTo(Short.valueOf("2"));
		assertThat(bw.getPropertyValue("int1")).as("Correct int1 value").isEqualTo(Integer.valueOf("7"));
		assertThat(tb.getInt1()).as("Correct int1 value").isEqualTo(7);
		assertThat(bw.getPropertyValue("int2")).as("Correct int2 value").isEqualTo(Integer.valueOf("8"));
		assertThat(tb.getInt2()).as("Correct int2 value").isEqualTo(Integer.valueOf("8"));
		assertThat(bw.getPropertyValue("long1")).as("Correct long1 value").isEqualTo(Long.valueOf("5"));
		assertThat(tb.getLong1()).as("Correct long1 value").isEqualTo(5);
		assertThat(bw.getPropertyValue("long2")).as("Correct long2 value").isEqualTo(Long.valueOf("6"));
		assertThat(tb.getLong2()).as("Correct long2 value").isEqualTo(Long.valueOf("6"));
		assertThat(new BigInteger("3")).as("Correct bigInteger value").isEqualTo(bw.getPropertyValue("bigInteger"));
		assertThat(new BigInteger("3")).as("Correct bigInteger value").isEqualTo(tb.getBigInteger());
		assertThat(bw.getPropertyValue("float1")).as("Correct float1 value").isEqualTo(Float.valueOf("7.1"));
		assertThat(Float.valueOf(tb.getFloat1())).as("Correct float1 value").isEqualTo(Float.valueOf("7.1"));
		assertThat(bw.getPropertyValue("float2")).as("Correct float2 value").isEqualTo(Float.valueOf("8.1"));
		assertThat(tb.getFloat2()).as("Correct float2 value").isEqualTo(Float.valueOf("8.1"));
		assertThat(bw.getPropertyValue("double1")).as("Correct double1 value").isEqualTo(Double.valueOf("5.1"));
		assertThat(tb.getDouble1()).as("Correct double1 value").isEqualTo(5.1);
		assertThat(bw.getPropertyValue("double2")).as("Correct double2 value").isEqualTo(Double.valueOf("6.1"));
		assertThat(tb.getDouble2()).as("Correct double2 value").isEqualTo(Double.valueOf("6.1"));
		assertThat(new BigDecimal("4.5")).as("Correct bigDecimal value").isEqualTo(bw.getPropertyValue("bigDecimal"));
		assertThat(new BigDecimal("4.5")).as("Correct bigDecimal value").isEqualTo(tb.getBigDecimal());
	}

	@Test
	void testCustomNumberEditorWithAllowEmpty() {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.GERMAN);
		NumberTestBean tb = new NumberTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(long.class, new CustomNumberEditor(Long.class, nf, true));
		bw.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, nf, true));

		bw.setPropertyValue("long1", "5");
		bw.setPropertyValue("long2", "6");
		assertThat(Long.valueOf("5").equals(bw.getPropertyValue("long1"))).as("Correct long1 value").isTrue();
		assertThat(tb.getLong1()).as("Correct long1 value").isEqualTo(5);
		assertThat(Long.valueOf("6").equals(bw.getPropertyValue("long2"))).as("Correct long2 value").isTrue();
		assertThat(Long.valueOf("6").equals(tb.getLong2())).as("Correct long2 value").isTrue();

		bw.setPropertyValue("long2", "");
		assertThat(bw.getPropertyValue("long2")).as("Correct long2 value").isNull();
		assertThat(tb.getLong2()).as("Correct long2 value").isNull();
		assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
				bw.setPropertyValue("long1", ""));
		assertThat(bw.getPropertyValue("long1")).isEqualTo(5L);
		assertThat(tb.getLong1()).isEqualTo(5);
	}

	@Test
	void testCustomNumberEditorWithFrenchBigDecimal() {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRENCH);
		NumberTestBean tb = new NumberTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, nf, true));
		bw.setPropertyValue("bigDecimal", "1000");
		assertThat(tb.getBigDecimal().floatValue()).isCloseTo(1000.0f, within(0f));

		bw.setPropertyValue("bigDecimal", "1000,5");
		assertThat(tb.getBigDecimal().floatValue()).isCloseTo(1000.5f, within(0f));

		bw.setPropertyValue("bigDecimal", "1 000,5");
		assertThat(tb.getBigDecimal().floatValue()).isCloseTo(1000.5f, within(0f));

	}

	@Test
	void testParseShortGreaterThanMaxValueWithoutNumberFormat() {
		CustomNumberEditor editor = new CustomNumberEditor(Short.class, true);
		assertThatExceptionOfType(NumberFormatException.class).as("greater than Short.MAX_VALUE + 1").isThrownBy(() ->
			editor.setAsText(String.valueOf(Short.MAX_VALUE + 1)));
	}

	@Test
	void testByteArrayPropertyEditor() {
		PrimitiveArrayBean bean = new PrimitiveArrayBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.setPropertyValue("byteArray", "myvalue");
		assertThat(new String(bean.getByteArray())).isEqualTo("myvalue");
	}

	@Test
	void testCharArrayPropertyEditor() {
		PrimitiveArrayBean bean = new PrimitiveArrayBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.setPropertyValue("charArray", "myvalue");
		assertThat(new String(bean.getCharArray())).isEqualTo("myvalue");
	}

	@Test
	void testCharacterEditor() {
		CharBean cb = new CharBean();
		BeanWrapper bw = new BeanWrapperImpl(cb);

		bw.setPropertyValue("myChar", 'c');
		assertThat(cb.getMyChar()).isEqualTo('c');

		bw.setPropertyValue("myChar", "c");
		assertThat(cb.getMyChar()).isEqualTo('c');

		bw.setPropertyValue("myChar", "\u0041");
		assertThat(cb.getMyChar()).isEqualTo('A');

		bw.setPropertyValue("myChar", "\\u0022");
		assertThat(cb.getMyChar()).isEqualTo('"');

		CharacterEditor editor = new CharacterEditor(false);
		editor.setAsText("M");
		assertThat(editor.getAsText()).isEqualTo("M");
	}

	@Test
	void testCharacterEditorWithAllowEmpty() {
		CharBean cb = new CharBean();
		BeanWrapper bw = new BeanWrapperImpl(cb);
		bw.registerCustomEditor(Character.class, new CharacterEditor(true));

		bw.setPropertyValue("myCharacter", 'c');
		assertThat(cb.getMyCharacter()).isEqualTo(Character.valueOf('c'));

		bw.setPropertyValue("myCharacter", "c");
		assertThat(cb.getMyCharacter()).isEqualTo(Character.valueOf('c'));

		bw.setPropertyValue("myCharacter", "\u0041");
		assertThat(cb.getMyCharacter()).isEqualTo(Character.valueOf('A'));

		bw.setPropertyValue("myCharacter", " ");
		assertThat(cb.getMyCharacter()).isEqualTo(Character.valueOf(' '));

		bw.setPropertyValue("myCharacter", "");
		assertThat(cb.getMyCharacter()).isNull();
	}

	@Test
	void testCharacterEditorSetAsTextWithStringLongerThanOneCharacter() {
		PropertyEditor charEditor = new CharacterEditor(false);
		assertThatIllegalArgumentException().isThrownBy(() ->
				charEditor.setAsText("ColdWaterCanyon"));
	}

	@Test
	void testCharacterEditorGetAsTextReturnsEmptyStringIfValueIsNull() {
		PropertyEditor charEditor = new CharacterEditor(false);
		assertThat(charEditor.getAsText()).isEmpty();
		charEditor = new CharacterEditor(true);
		charEditor.setAsText(null);
		assertThat(charEditor.getAsText()).isEmpty();
		charEditor.setAsText("");
		assertThat(charEditor.getAsText()).isEmpty();
		charEditor.setAsText(" ");
		assertThat(charEditor.getAsText()).isEqualTo(" ");
	}

	@Test
	void testCharacterEditorSetAsTextWithNullNotAllowingEmptyAsNull() {
		PropertyEditor charEditor = new CharacterEditor(false);
		assertThatIllegalArgumentException().isThrownBy(() ->
				charEditor.setAsText(null));
	}

	@Test
	void testClassEditor() {
		PropertyEditor classEditor = new ClassEditor();
		classEditor.setAsText(TestBean.class.getName());
		assertThat(classEditor.getValue()).isEqualTo(TestBean.class);
		assertThat(classEditor.getAsText()).isEqualTo(TestBean.class.getName());

		classEditor.setAsText(null);
		assertThat(classEditor.getAsText()).isEmpty();
		classEditor.setAsText("");
		assertThat(classEditor.getAsText()).isEmpty();
		classEditor.setAsText("\t  ");
		assertThat(classEditor.getAsText()).isEmpty();
	}

	@Test
	void testClassEditorWithNonExistentClass() {
		PropertyEditor classEditor = new ClassEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				classEditor.setAsText("hairdresser.on.Fire"));
	}

	@Test
	void testClassEditorWithArray() {
		PropertyEditor classEditor = new ClassEditor();
		classEditor.setAsText("org.springframework.beans.testfixture.beans.TestBean[]");
		assertThat(classEditor.getValue()).isEqualTo(TestBean[].class);
		assertThat(classEditor.getAsText()).isEqualTo("org.springframework.beans.testfixture.beans.TestBean[]");
	}

	/*
	* SPR_2165 - ClassEditor is inconsistent with multidimensional arrays
	*/
	@Test
	void testGetAsTextWithTwoDimensionalArray() {
		String[][] chessboard = new String[8][8];
		ClassEditor editor = new ClassEditor();
		editor.setValue(chessboard.getClass());
		assertThat(editor.getAsText()).isEqualTo("java.lang.String[][]");
	}

	/*
	 * SPR_2165 - ClassEditor is inconsistent with multidimensional arrays
	 */
	@Test
	void testGetAsTextWithRidiculousMultiDimensionalArray() {
		String[][][][][] ridiculousChessboard = new String[8][4][0][1][3];
		ClassEditor editor = new ClassEditor();
		editor.setValue(ridiculousChessboard.getClass());
		assertThat(editor.getAsText()).isEqualTo("java.lang.String[][][][][]");
	}

	@Test
	void testFileEditor() {
		PropertyEditor fileEditor = new FileEditor();
		fileEditor.setAsText("file:myfile.txt");
		assertThat(fileEditor.getValue()).isEqualTo(new File("myfile.txt"));
		assertThat(fileEditor.getAsText()).isEqualTo((new File("myfile.txt")).getPath());
	}

	@Test
	void testFileEditorWithRelativePath() {
		PropertyEditor fileEditor = new FileEditor();
		try {
			fileEditor.setAsText("myfile.txt");
		}
		catch (IllegalArgumentException ex) {
			// expected: should get resolved as class path resource,
			// and there is no such resource in the class path...
		}
	}

	@Test
	void testFileEditorWithAbsolutePath() {
		PropertyEditor fileEditor = new FileEditor();
		// testing on Windows
		if (new File("C:/myfile.txt").isAbsolute()) {
			fileEditor.setAsText("C:/myfile.txt");
			assertThat(fileEditor.getValue()).isEqualTo(new File("C:/myfile.txt"));
		}
		// testing on Unix
		if (new File("/myfile.txt").isAbsolute()) {
			fileEditor.setAsText("/myfile.txt");
			assertThat(fileEditor.getValue()).isEqualTo(new File("/myfile.txt"));
		}
	}

	@Test
	void testLocaleEditor() {
		PropertyEditor localeEditor = new LocaleEditor();
		localeEditor.setAsText("en_CA");
		assertThat(localeEditor.getValue()).isEqualTo(Locale.CANADA);
		assertThat(localeEditor.getAsText()).isEqualTo("en_CA");

		localeEditor = new LocaleEditor();
		assertThat(localeEditor.getAsText()).isEmpty();
	}

	@Test
	void testPatternEditor() {
		final String REGEX = "a.*";

		PropertyEditor patternEditor = new PatternEditor();
		patternEditor.setAsText(REGEX);
		assertThat(((Pattern) patternEditor.getValue()).pattern()).isEqualTo(Pattern.compile(REGEX).pattern());
		assertThat(patternEditor.getAsText()).isEqualTo(REGEX);

		patternEditor = new PatternEditor();
		assertThat(patternEditor.getAsText()).isEmpty();

		patternEditor = new PatternEditor();
		patternEditor.setAsText(null);
		assertThat(patternEditor.getAsText()).isEmpty();
	}

	@Test
	void testCustomBooleanEditor() {
		CustomBooleanEditor editor = new CustomBooleanEditor(false);

		editor.setAsText("true");
		assertThat(editor.getValue()).isEqualTo(Boolean.TRUE);
		assertThat(editor.getAsText()).isEqualTo("true");

		editor.setAsText("false");
		assertThat(editor.getValue()).isEqualTo(Boolean.FALSE);
		assertThat(editor.getAsText()).isEqualTo("false");

		editor.setValue(null);
		assertThat(editor.getValue()).isNull();
		assertThat(editor.getAsText()).isEmpty();

		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText(null));
	}

	@Test
	void testCustomBooleanEditorWithEmptyAsNull() {
		CustomBooleanEditor editor = new CustomBooleanEditor(true);

		editor.setAsText("true");
		assertThat(editor.getValue()).isEqualTo(Boolean.TRUE);
		assertThat(editor.getAsText()).isEqualTo("true");

		editor.setAsText("false");
		assertThat(editor.getValue()).isEqualTo(Boolean.FALSE);
		assertThat(editor.getAsText()).isEqualTo("false");

		editor.setValue(null);
		assertThat(editor.getValue()).isNull();
		assertThat(editor.getAsText()).isEmpty();
	}

	@Test
	void testCustomDateEditor() {
		CustomDateEditor editor = new CustomDateEditor(new SimpleDateFormat("MM/dd/yyyy"), false);
		editor.setValue(null);
		assertThat(editor.getValue()).isNull();
		assertThat(editor.getAsText()).isEmpty();
	}

	@Test
	void testCustomDateEditorWithEmptyAsNull() {
		CustomDateEditor editor = new CustomDateEditor(new SimpleDateFormat("MM/dd/yyyy"), true);
		editor.setValue(null);
		assertThat(editor.getValue()).isNull();
		assertThat(editor.getAsText()).isEmpty();
	}

	@Test
	void testCustomDateEditorWithExactDateLength() {
		int maxLength = 10;
		String validDate = "01/01/2005";
		String invalidDate = "01/01/05";

		assertThat(validDate).hasSize(maxLength);
		assertThat(invalidDate.length()).isNotEqualTo(maxLength);

		CustomDateEditor editor = new CustomDateEditor(new SimpleDateFormat("MM/dd/yyyy"), true, maxLength);
		editor.setAsText(validDate);
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText(invalidDate))
			.withMessageContaining("10");
	}

	@Test
	void testCustomNumberEditor() {
		CustomNumberEditor editor = new CustomNumberEditor(Integer.class, false);
		editor.setAsText("5");
		assertThat(editor.getValue()).isEqualTo(5);
		assertThat(editor.getAsText()).isEqualTo("5");
		editor.setValue(null);
		assertThat(editor.getValue()).isNull();
		assertThat(editor.getAsText()).isEmpty();
	}

	@Test
	void testCustomNumberEditorWithHex() {
		CustomNumberEditor editor = new CustomNumberEditor(Integer.class, false);
		editor.setAsText("0x" + Integer.toHexString(64));
		assertThat(editor.getValue()).isEqualTo(64);
	}

	@Test
	void testCustomNumberEditorWithEmptyAsNull() {
		CustomNumberEditor editor = new CustomNumberEditor(Integer.class, true);
		editor.setAsText("5");
		assertThat(editor.getValue()).isEqualTo(5);
		assertThat(editor.getAsText()).isEqualTo("5");
		editor.setAsText("");
		assertThat(editor.getValue()).isNull();
		assertThat(editor.getAsText()).isEmpty();
		editor.setValue(null);
		assertThat(editor.getValue()).isNull();
		assertThat(editor.getAsText()).isEmpty();
	}

	@Test
	void testStringTrimmerEditor() {
		StringTrimmerEditor editor = new StringTrimmerEditor(false);
		editor.setAsText("test");
		assertThat(editor.getValue()).isEqualTo("test");
		assertThat(editor.getAsText()).isEqualTo("test");
		editor.setAsText(" test ");
		assertThat(editor.getValue()).isEqualTo("test");
		assertThat(editor.getAsText()).isEqualTo("test");
		editor.setAsText("");
		assertThat(editor.getValue()).isEqualTo("");
		assertThat(editor.getAsText()).isEmpty();
		editor.setValue(null);
		assertThat(editor.getAsText()).isEmpty();
		editor.setAsText(null);
		assertThat(editor.getAsText()).isEmpty();
	}

	@Test
	void testStringTrimmerEditorWithEmptyAsNull() {
		StringTrimmerEditor editor = new StringTrimmerEditor(true);
		editor.setAsText("test");
		assertThat(editor.getValue()).isEqualTo("test");
		assertThat(editor.getAsText()).isEqualTo("test");
		editor.setAsText(" test ");
		assertThat(editor.getValue()).isEqualTo("test");
		assertThat(editor.getAsText()).isEqualTo("test");
		editor.setAsText("  ");
		assertThat(editor.getValue()).isNull();
		assertThat(editor.getAsText()).isEmpty();
		editor.setValue(null);
		assertThat(editor.getAsText()).isEmpty();
	}

	@Test
	void testStringTrimmerEditorWithCharsToDelete() {
		StringTrimmerEditor editor = new StringTrimmerEditor("\r\n\f", false);
		editor.setAsText("te\ns\ft");
		assertThat(editor.getValue()).isEqualTo("test");
		assertThat(editor.getAsText()).isEqualTo("test");
		editor.setAsText(" test ");
		assertThat(editor.getValue()).isEqualTo("test");
		assertThat(editor.getAsText()).isEqualTo("test");
		editor.setAsText("");
		assertThat(editor.getValue()).isEqualTo("");
		assertThat(editor.getAsText()).isEmpty();
		editor.setValue(null);
		assertThat(editor.getAsText()).isEmpty();
	}

	@Test
	void testStringTrimmerEditorWithCharsToDeleteAndEmptyAsNull() {
		StringTrimmerEditor editor = new StringTrimmerEditor("\r\n\f", true);
		editor.setAsText("te\ns\ft");
		assertThat(editor.getValue()).isEqualTo("test");
		assertThat(editor.getAsText()).isEqualTo("test");
		editor.setAsText(" test ");
		assertThat(editor.getValue()).isEqualTo("test");
		assertThat(editor.getAsText()).isEqualTo("test");
		editor.setAsText(" \n\f ");
		assertThat(editor.getValue()).isNull();
		assertThat(editor.getAsText()).isEmpty();
		editor.setValue(null);
		assertThat(editor.getAsText()).isEmpty();
	}

	@Test
	void testIndexedPropertiesWithCustomEditorForType() {
		IndexedTestBean bean = new IndexedTestBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(String.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("prefix" + text);
			}
		});
		TestBean tb0 = bean.getArray()[0];
		TestBean tb1 = bean.getArray()[1];
		TestBean tb2 = ((TestBean) bean.getList().get(0));
		TestBean tb3 = ((TestBean) bean.getList().get(1));
		TestBean tb4 = ((TestBean) bean.getMap().get("key1"));
		TestBean tb5 = ((TestBean) bean.getMap().get("key2"));
		assertThat(tb0.getName()).isEqualTo("name0");
		assertThat(tb1.getName()).isEqualTo("name1");
		assertThat(tb2.getName()).isEqualTo("name2");
		assertThat(tb3.getName()).isEqualTo("name3");
		assertThat(tb4.getName()).isEqualTo("name4");
		assertThat(tb5.getName()).isEqualTo("name5");
		assertThat(bw.getPropertyValue("array[0].name")).isEqualTo("name0");
		assertThat(bw.getPropertyValue("array[1].name")).isEqualTo("name1");
		assertThat(bw.getPropertyValue("list[0].name")).isEqualTo("name2");
		assertThat(bw.getPropertyValue("list[1].name")).isEqualTo("name3");
		assertThat(bw.getPropertyValue("map[key1].name")).isEqualTo("name4");
		assertThat(bw.getPropertyValue("map[key2].name")).isEqualTo("name5");
		assertThat(bw.getPropertyValue("map['key1'].name")).isEqualTo("name4");
		assertThat(bw.getPropertyValue("map[\"key2\"].name")).isEqualTo("name5");

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].name", "name5");
		pvs.add("array[1].name", "name4");
		pvs.add("list[0].name", "name3");
		pvs.add("list[1].name", "name2");
		pvs.add("map[key1].name", "name1");
		pvs.add("map['key2'].name", "name0");
		bw.setPropertyValues(pvs);
		assertThat(tb0.getName()).isEqualTo("prefixname5");
		assertThat(tb1.getName()).isEqualTo("prefixname4");
		assertThat(tb2.getName()).isEqualTo("prefixname3");
		assertThat(tb3.getName()).isEqualTo("prefixname2");
		assertThat(tb4.getName()).isEqualTo("prefixname1");
		assertThat(tb5.getName()).isEqualTo("prefixname0");
		assertThat(bw.getPropertyValue("array[0].name")).isEqualTo("prefixname5");
		assertThat(bw.getPropertyValue("array[1].name")).isEqualTo("prefixname4");
		assertThat(bw.getPropertyValue("list[0].name")).isEqualTo("prefixname3");
		assertThat(bw.getPropertyValue("list[1].name")).isEqualTo("prefixname2");
		assertThat(bw.getPropertyValue("map[\"key1\"].name")).isEqualTo("prefixname1");
		assertThat(bw.getPropertyValue("map['key2'].name")).isEqualTo("prefixname0");
	}

	@Test
	void testIndexedPropertiesWithCustomEditorForProperty() {
		IndexedTestBean bean = new IndexedTestBean(false);
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(String.class, "array.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("array" + text);
			}
		});
		bw.registerCustomEditor(String.class, "list.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("list" + text);
			}
		});
		bw.registerCustomEditor(String.class, "map.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("map" + text);
			}
		});
		bean.populate();

		TestBean tb0 = bean.getArray()[0];
		TestBean tb1 = bean.getArray()[1];
		TestBean tb2 = ((TestBean) bean.getList().get(0));
		TestBean tb3 = ((TestBean) bean.getList().get(1));
		TestBean tb4 = ((TestBean) bean.getMap().get("key1"));
		TestBean tb5 = ((TestBean) bean.getMap().get("key2"));
		assertThat(tb0.getName()).isEqualTo("name0");
		assertThat(tb1.getName()).isEqualTo("name1");
		assertThat(tb2.getName()).isEqualTo("name2");
		assertThat(tb3.getName()).isEqualTo("name3");
		assertThat(tb4.getName()).isEqualTo("name4");
		assertThat(tb5.getName()).isEqualTo("name5");
		assertThat(bw.getPropertyValue("array[0].name")).isEqualTo("name0");
		assertThat(bw.getPropertyValue("array[1].name")).isEqualTo("name1");
		assertThat(bw.getPropertyValue("list[0].name")).isEqualTo("name2");
		assertThat(bw.getPropertyValue("list[1].name")).isEqualTo("name3");
		assertThat(bw.getPropertyValue("map[key1].name")).isEqualTo("name4");
		assertThat(bw.getPropertyValue("map[key2].name")).isEqualTo("name5");
		assertThat(bw.getPropertyValue("map['key1'].name")).isEqualTo("name4");
		assertThat(bw.getPropertyValue("map[\"key2\"].name")).isEqualTo("name5");

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].name", "name5");
		pvs.add("array[1].name", "name4");
		pvs.add("list[0].name", "name3");
		pvs.add("list[1].name", "name2");
		pvs.add("map[key1].name", "name1");
		pvs.add("map['key2'].name", "name0");
		bw.setPropertyValues(pvs);
		assertThat(tb0.getName()).isEqualTo("arrayname5");
		assertThat(tb1.getName()).isEqualTo("arrayname4");
		assertThat(tb2.getName()).isEqualTo("listname3");
		assertThat(tb3.getName()).isEqualTo("listname2");
		assertThat(tb4.getName()).isEqualTo("mapname1");
		assertThat(tb5.getName()).isEqualTo("mapname0");
		assertThat(bw.getPropertyValue("array[0].name")).isEqualTo("arrayname5");
		assertThat(bw.getPropertyValue("array[1].name")).isEqualTo("arrayname4");
		assertThat(bw.getPropertyValue("list[0].name")).isEqualTo("listname3");
		assertThat(bw.getPropertyValue("list[1].name")).isEqualTo("listname2");
		assertThat(bw.getPropertyValue("map[\"key1\"].name")).isEqualTo("mapname1");
		assertThat(bw.getPropertyValue("map['key2'].name")).isEqualTo("mapname0");
	}

	@Test
	void testIndexedPropertiesWithIndividualCustomEditorForProperty() {
		IndexedTestBean bean = new IndexedTestBean(false);
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(String.class, "array[0].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("array0" + text);
			}
		});
		bw.registerCustomEditor(String.class, "array[1].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("array1" + text);
			}
		});
		bw.registerCustomEditor(String.class, "list[0].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("list0" + text);
			}
		});
		bw.registerCustomEditor(String.class, "list[1].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("list1" + text);
			}
		});
		bw.registerCustomEditor(String.class, "map[key1].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("mapkey1" + text);
			}
		});
		bw.registerCustomEditor(String.class, "map[key2].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("mapkey2" + text);
			}
		});
		bean.populate();

		TestBean tb0 = bean.getArray()[0];
		TestBean tb1 = bean.getArray()[1];
		TestBean tb2 = ((TestBean) bean.getList().get(0));
		TestBean tb3 = ((TestBean) bean.getList().get(1));
		TestBean tb4 = ((TestBean) bean.getMap().get("key1"));
		TestBean tb5 = ((TestBean) bean.getMap().get("key2"));
		assertThat(tb0.getName()).isEqualTo("name0");
		assertThat(tb1.getName()).isEqualTo("name1");
		assertThat(tb2.getName()).isEqualTo("name2");
		assertThat(tb3.getName()).isEqualTo("name3");
		assertThat(tb4.getName()).isEqualTo("name4");
		assertThat(tb5.getName()).isEqualTo("name5");
		assertThat(bw.getPropertyValue("array[0].name")).isEqualTo("name0");
		assertThat(bw.getPropertyValue("array[1].name")).isEqualTo("name1");
		assertThat(bw.getPropertyValue("list[0].name")).isEqualTo("name2");
		assertThat(bw.getPropertyValue("list[1].name")).isEqualTo("name3");
		assertThat(bw.getPropertyValue("map[key1].name")).isEqualTo("name4");
		assertThat(bw.getPropertyValue("map[key2].name")).isEqualTo("name5");
		assertThat(bw.getPropertyValue("map['key1'].name")).isEqualTo("name4");
		assertThat(bw.getPropertyValue("map[\"key2\"].name")).isEqualTo("name5");

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].name", "name5");
		pvs.add("array[1].name", "name4");
		pvs.add("list[0].name", "name3");
		pvs.add("list[1].name", "name2");
		pvs.add("map[key1].name", "name1");
		pvs.add("map['key2'].name", "name0");
		bw.setPropertyValues(pvs);
		assertThat(tb0.getName()).isEqualTo("array0name5");
		assertThat(tb1.getName()).isEqualTo("array1name4");
		assertThat(tb2.getName()).isEqualTo("list0name3");
		assertThat(tb3.getName()).isEqualTo("list1name2");
		assertThat(tb4.getName()).isEqualTo("mapkey1name1");
		assertThat(tb5.getName()).isEqualTo("mapkey2name0");
		assertThat(bw.getPropertyValue("array[0].name")).isEqualTo("array0name5");
		assertThat(bw.getPropertyValue("array[1].name")).isEqualTo("array1name4");
		assertThat(bw.getPropertyValue("list[0].name")).isEqualTo("list0name3");
		assertThat(bw.getPropertyValue("list[1].name")).isEqualTo("list1name2");
		assertThat(bw.getPropertyValue("map[\"key1\"].name")).isEqualTo("mapkey1name1");
		assertThat(bw.getPropertyValue("map['key2'].name")).isEqualTo("mapkey2name0");
	}

	@Test
	void testNestedIndexedPropertiesWithCustomEditorForProperty() {
		IndexedTestBean bean = new IndexedTestBean();
		TestBean tb0 = bean.getArray()[0];
		TestBean tb1 = bean.getArray()[1];
		TestBean tb2 = ((TestBean) bean.getList().get(0));
		TestBean tb3 = ((TestBean) bean.getList().get(1));
		TestBean tb4 = ((TestBean) bean.getMap().get("key1"));
		TestBean tb5 = ((TestBean) bean.getMap().get("key2"));
		tb0.setNestedIndexedBean(new IndexedTestBean());
		tb1.setNestedIndexedBean(new IndexedTestBean());
		tb2.setNestedIndexedBean(new IndexedTestBean());
		tb3.setNestedIndexedBean(new IndexedTestBean());
		tb4.setNestedIndexedBean(new IndexedTestBean());
		tb5.setNestedIndexedBean(new IndexedTestBean());
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(String.class, "array.nestedIndexedBean.array.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("array" + text);
			}

			@Override
			public String getAsText() {
				return ((String) getValue()).substring(5);
			}
		});
		bw.registerCustomEditor(String.class, "list.nestedIndexedBean.list.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("list" + text);
			}

			@Override
			public String getAsText() {
				return ((String) getValue()).substring(4);
			}
		});
		bw.registerCustomEditor(String.class, "map.nestedIndexedBean.map.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("map" + text);
			}

			@Override
			public String getAsText() {
				return ((String) getValue()).substring(4);
			}
		});
		assertThat(tb0.getName()).isEqualTo("name0");
		assertThat(tb1.getName()).isEqualTo("name1");
		assertThat(tb2.getName()).isEqualTo("name2");
		assertThat(tb3.getName()).isEqualTo("name3");
		assertThat(tb4.getName()).isEqualTo("name4");
		assertThat(tb5.getName()).isEqualTo("name5");
		assertThat(bw.getPropertyValue("array[0].nestedIndexedBean.array[0].name")).isEqualTo("name0");
		assertThat(bw.getPropertyValue("array[1].nestedIndexedBean.array[1].name")).isEqualTo("name1");
		assertThat(bw.getPropertyValue("list[0].nestedIndexedBean.list[0].name")).isEqualTo("name2");
		assertThat(bw.getPropertyValue("list[1].nestedIndexedBean.list[1].name")).isEqualTo("name3");
		assertThat(bw.getPropertyValue("map[key1].nestedIndexedBean.map[key1].name")).isEqualTo("name4");
		assertThat(bw.getPropertyValue("map['key2'].nestedIndexedBean.map[\"key2\"].name")).isEqualTo("name5");

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].nestedIndexedBean.array[0].name", "name5");
		pvs.add("array[1].nestedIndexedBean.array[1].name", "name4");
		pvs.add("list[0].nestedIndexedBean.list[0].name", "name3");
		pvs.add("list[1].nestedIndexedBean.list[1].name", "name2");
		pvs.add("map[key1].nestedIndexedBean.map[\"key1\"].name", "name1");
		pvs.add("map['key2'].nestedIndexedBean.map[key2].name", "name0");
		bw.setPropertyValues(pvs);
		assertThat(tb0.getNestedIndexedBean().getArray()[0].getName()).isEqualTo("arrayname5");
		assertThat(tb1.getNestedIndexedBean().getArray()[1].getName()).isEqualTo("arrayname4");
		assertThat(((TestBean) tb2.getNestedIndexedBean().getList().get(0)).getName()).isEqualTo("listname3");
		assertThat(((TestBean) tb3.getNestedIndexedBean().getList().get(1)).getName()).isEqualTo("listname2");
		assertThat(((TestBean) tb4.getNestedIndexedBean().getMap().get("key1")).getName()).isEqualTo("mapname1");
		assertThat(((TestBean) tb5.getNestedIndexedBean().getMap().get("key2")).getName()).isEqualTo("mapname0");
		assertThat(bw.getPropertyValue("array[0].nestedIndexedBean.array[0].name")).isEqualTo("arrayname5");
		assertThat(bw.getPropertyValue("array[1].nestedIndexedBean.array[1].name")).isEqualTo("arrayname4");
		assertThat(bw.getPropertyValue("list[0].nestedIndexedBean.list[0].name")).isEqualTo("listname3");
		assertThat(bw.getPropertyValue("list[1].nestedIndexedBean.list[1].name")).isEqualTo("listname2");
		assertThat(bw.getPropertyValue("map['key1'].nestedIndexedBean.map[key1].name")).isEqualTo("mapname1");
		assertThat(bw.getPropertyValue("map[key2].nestedIndexedBean.map[\"key2\"].name")).isEqualTo("mapname0");
	}

	@Test
	void testNestedIndexedPropertiesWithIndexedCustomEditorForProperty() {
		IndexedTestBean bean = new IndexedTestBean();
		TestBean tb0 = bean.getArray()[0];
		TestBean tb1 = bean.getArray()[1];
		TestBean tb2 = ((TestBean) bean.getList().get(0));
		TestBean tb3 = ((TestBean) bean.getList().get(1));
		TestBean tb4 = ((TestBean) bean.getMap().get("key1"));
		TestBean tb5 = ((TestBean) bean.getMap().get("key2"));
		tb0.setNestedIndexedBean(new IndexedTestBean());
		tb1.setNestedIndexedBean(new IndexedTestBean());
		tb2.setNestedIndexedBean(new IndexedTestBean());
		tb3.setNestedIndexedBean(new IndexedTestBean());
		tb4.setNestedIndexedBean(new IndexedTestBean());
		tb5.setNestedIndexedBean(new IndexedTestBean());
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(String.class, "array[0].nestedIndexedBean.array[0].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("array" + text);
			}
		});
		bw.registerCustomEditor(String.class, "list.nestedIndexedBean.list[1].name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("list" + text);
			}
		});
		bw.registerCustomEditor(String.class, "map[key1].nestedIndexedBean.map.name", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("map" + text);
			}
		});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].nestedIndexedBean.array[0].name", "name5");
		pvs.add("array[1].nestedIndexedBean.array[1].name", "name4");
		pvs.add("list[0].nestedIndexedBean.list[0].name", "name3");
		pvs.add("list[1].nestedIndexedBean.list[1].name", "name2");
		pvs.add("map[key1].nestedIndexedBean.map[\"key1\"].name", "name1");
		pvs.add("map['key2'].nestedIndexedBean.map[key2].name", "name0");
		bw.setPropertyValues(pvs);
		assertThat(tb0.getNestedIndexedBean().getArray()[0].getName()).isEqualTo("arrayname5");
		assertThat(tb1.getNestedIndexedBean().getArray()[1].getName()).isEqualTo("name4");
		assertThat(((TestBean) tb2.getNestedIndexedBean().getList().get(0)).getName()).isEqualTo("name3");
		assertThat(((TestBean) tb3.getNestedIndexedBean().getList().get(1)).getName()).isEqualTo("listname2");
		assertThat(((TestBean) tb4.getNestedIndexedBean().getMap().get("key1")).getName()).isEqualTo("mapname1");
		assertThat(((TestBean) tb5.getNestedIndexedBean().getMap().get("key2")).getName()).isEqualTo("name0");
	}

	@Test
	void testIndexedPropertiesWithDirectAccessAndPropertyEditors() {
		IndexedTestBean bean = new IndexedTestBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(TestBean.class, "array", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("array" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		bw.registerCustomEditor(TestBean.class, "list", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("list" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		bw.registerCustomEditor(TestBean.class, "map", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("map" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0]", "a");
		pvs.add("array[1]", "b");
		pvs.add("list[0]", "c");
		pvs.add("list[1]", "d");
		pvs.add("map[key1]", "e");
		pvs.add("map['key2']", "f");
		bw.setPropertyValues(pvs);
		assertThat(bean.getArray()[0].getName()).isEqualTo("arraya");
		assertThat(bean.getArray()[1].getName()).isEqualTo("arrayb");
		assertThat(((TestBean) bean.getList().get(0)).getName()).isEqualTo("listc");
		assertThat(((TestBean) bean.getList().get(1)).getName()).isEqualTo("listd");
		assertThat(((TestBean) bean.getMap().get("key1")).getName()).isEqualTo("mape");
		assertThat(((TestBean) bean.getMap().get("key2")).getName()).isEqualTo("mapf");
	}

	@Test
	void testIndexedPropertiesWithDirectAccessAndSpecificPropertyEditors() {
		IndexedTestBean bean = new IndexedTestBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(TestBean.class, "array[0]", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("array0" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		bw.registerCustomEditor(TestBean.class, "array[1]", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("array1" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		bw.registerCustomEditor(TestBean.class, "list[0]", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("list0" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		bw.registerCustomEditor(TestBean.class, "list[1]", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("list1" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		bw.registerCustomEditor(TestBean.class, "map[key1]", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("mapkey1" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});
		bw.registerCustomEditor(TestBean.class, "map[key2]", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean("mapkey2" + text, 99));
			}

			@Override
			public String getAsText() {
				return ((TestBean) getValue()).getName();
			}
		});

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0]", "a");
		pvs.add("array[1]", "b");
		pvs.add("list[0]", "c");
		pvs.add("list[1]", "d");
		pvs.add("map[key1]", "e");
		pvs.add("map['key2']", "f");
		bw.setPropertyValues(pvs);
		assertThat(bean.getArray()[0].getName()).isEqualTo("array0a");
		assertThat(bean.getArray()[1].getName()).isEqualTo("array1b");
		assertThat(((TestBean) bean.getList().get(0)).getName()).isEqualTo("list0c");
		assertThat(((TestBean) bean.getList().get(1)).getName()).isEqualTo("list1d");
		assertThat(((TestBean) bean.getMap().get("key1")).getName()).isEqualTo("mapkey1e");
		assertThat(((TestBean) bean.getMap().get("key2")).getName()).isEqualTo("mapkey2f");
	}

	@Test
	void testIndexedPropertiesWithListPropertyEditor() {
		IndexedTestBean bean = new IndexedTestBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(List.class, "list", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				List<TestBean> result = new ArrayList<>();
				result.add(new TestBean("list" + text, 99));
				setValue(result);
			}
		});
		bw.setPropertyValue("list", "1");
		assertThat(((TestBean) bean.getList().get(0)).getName()).isEqualTo("list1");
		bw.setPropertyValue("list[0]", "test");
		assertThat(bean.getList().get(0)).isEqualTo("test");
	}

	@Test
	void testConversionToOldCollections() throws PropertyVetoException {
		OldCollectionsBean tb = new OldCollectionsBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(Vector.class, new CustomCollectionEditor(Vector.class));
		bw.registerCustomEditor(Hashtable.class, new CustomMapEditor(Hashtable.class));

		bw.setPropertyValue("vector", new String[] {"a", "b"});
		assertThat(tb.getVector()).hasSize(2);
		assertThat(tb.getVector().get(0)).isEqualTo("a");
		assertThat(tb.getVector().get(1)).isEqualTo("b");

		bw.setPropertyValue("hashtable", Collections.singletonMap("foo", "bar"));
		assertThat(tb.getHashtable()).hasSize(1);
		assertThat(tb.getHashtable().get("foo")).isEqualTo("bar");
	}

	@Test
	void testUninitializedArrayPropertyWithCustomEditor() {
		IndexedTestBean bean = new IndexedTestBean(false);
		BeanWrapper bw = new BeanWrapperImpl(bean);
		PropertyEditor pe = new CustomNumberEditor(Integer.class, true);
		bw.registerCustomEditor(null, "list.age", pe);
		TestBean tb = new TestBean();
		bw.setPropertyValue("list", new ArrayList<>());
		bw.setPropertyValue("list[0]", tb);
		assertThat(bean.getList().get(0)).isEqualTo(tb);
		assertThat(bw.findCustomEditor(int.class, "list.age")).isEqualTo(pe);
		assertThat(bw.findCustomEditor(null, "list.age")).isEqualTo(pe);
		assertThat(bw.findCustomEditor(int.class, "list[0].age")).isEqualTo(pe);
		assertThat(bw.findCustomEditor(null, "list[0].age")).isEqualTo(pe);
	}

	@Test
	void testArrayToArrayConversion() throws PropertyVetoException {
		IndexedTestBean tb = new IndexedTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(TestBean.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean(text, 99));
			}
		});
		bw.setPropertyValue("array", new String[] {"a", "b"});
		assertThat(tb.getArray()).hasSize(2);
		assertThat(tb.getArray()[0].getName()).isEqualTo("a");
		assertThat(tb.getArray()[1].getName()).isEqualTo("b");
	}

	@Test
	void testArrayToStringConversion() throws PropertyVetoException {
		TestBean tb = new TestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(String.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("-" + text + "-");
			}
		});
		bw.setPropertyValue("name", new String[]{"a", "b"});
		assertThat(tb.getName()).isEqualTo("-a,b-");
	}

	@Test
	void testClassArrayEditorSunnyDay() {
		ClassArrayEditor classArrayEditor = new ClassArrayEditor();
		classArrayEditor.setAsText("java.lang.String,java.util.HashMap");
		Class<?>[] classes = (Class<?>[]) classArrayEditor.getValue();
		assertThat(classes).hasSize(2);
		assertThat(classes[0]).isEqualTo(String.class);
		assertThat(classes[1]).isEqualTo(HashMap.class);
		assertThat(classArrayEditor.getAsText()).isEqualTo("java.lang.String,java.util.HashMap");
		// ensure setAsText can consume the return value of getAsText
		classArrayEditor.setAsText(classArrayEditor.getAsText());
	}

	@Test
	void testClassArrayEditorSunnyDayWithArrayTypes() {
		ClassArrayEditor classArrayEditor = new ClassArrayEditor();
		classArrayEditor.setAsText("java.lang.String[],java.util.Map[],int[],float[][][]");
		Class<?>[] classes = (Class<?>[]) classArrayEditor.getValue();
		assertThat(classes).hasSize(4);
		assertThat(classes[0]).isEqualTo(String[].class);
		assertThat(classes[1]).isEqualTo(Map[].class);
		assertThat(classes[2]).isEqualTo(int[].class);
		assertThat(classes[3]).isEqualTo(float[][][].class);
		assertThat(classArrayEditor.getAsText()).isEqualTo("java.lang.String[],java.util.Map[],int[],float[][][]");
		// ensure setAsText can consume the return value of getAsText
		classArrayEditor.setAsText(classArrayEditor.getAsText());
	}

	@Test
	void testClassArrayEditorSetAsTextWithNull() {
		ClassArrayEditor editor = new ClassArrayEditor();
		editor.setAsText(null);
		assertThat(editor.getValue()).isNull();
		assertThat(editor.getAsText()).isEmpty();
	}

	@Test
	void testClassArrayEditorSetAsTextWithEmptyString() {
		ClassArrayEditor editor = new ClassArrayEditor();
		editor.setAsText("");
		assertThat(editor.getValue()).isNull();
		assertThat(editor.getAsText()).isEmpty();
	}

	@Test
	void testClassArrayEditorSetAsTextWithWhitespaceString() {
		ClassArrayEditor editor = new ClassArrayEditor();
		editor.setAsText("\n");
		assertThat(editor.getValue()).isNull();
		assertThat(editor.getAsText()).isEmpty();
	}

	@Test
	void testCharsetEditor() {
		CharsetEditor editor = new CharsetEditor();
		String name = "UTF-8";
		editor.setAsText(name);
		Charset charset = Charset.forName(name);
		assertThat(editor.getValue()).as("Invalid Charset conversion").isEqualTo(charset);
		editor.setValue(charset);
		assertThat(editor.getAsText()).as("Invalid Charset conversion").isEqualTo(name);
	}


	private static class TestBeanEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) {
			TestBean tb = new TestBean();
			StringTokenizer st = new StringTokenizer(text, "_");
			tb.setName(st.nextToken());
			tb.setAge(Integer.parseInt(st.nextToken()));
			setValue(tb);
		}
	}


	private static class OldValueAccessingTestBeanEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) {
			TestBean tb = new TestBean();
			StringTokenizer st = new StringTokenizer(text, "_");
			tb.setName(st.nextToken());
			tb.setAge(Integer.parseInt(st.nextToken()));
			if (!tb.equals(getValue())) {
				setValue(tb);
			}
		}
	}


	@SuppressWarnings("unused")
	private static class PrimitiveArrayBean {

		private byte[] byteArray;

		private char[] charArray;

		public byte[] getByteArray() {
			return byteArray;
		}

		public void setByteArray(byte[] byteArray) {
			this.byteArray = byteArray;
		}

		public char[] getCharArray() {
			return charArray;
		}

		public void setCharArray(char[] charArray) {
			this.charArray = charArray;
		}
	}


	@SuppressWarnings("unused")
	private static class CharBean {

		private char myChar;

		private Character myCharacter;

		public char getMyChar() {
			return myChar;
		}

		public void setMyChar(char myChar) {
			this.myChar = myChar;
		}

		public Character getMyCharacter() {
			return myCharacter;
		}

		public void setMyCharacter(Character myCharacter) {
			this.myCharacter = myCharacter;
		}
	}


	@SuppressWarnings("unused")
	private static class OldCollectionsBean {

		private Vector<?> vector;

		private Hashtable<?, ?> hashtable;

		public Vector<?> getVector() {
			return vector;
		}

		public void setVector(Vector<?> vector) {
			this.vector = vector;
		}

		public Hashtable<?, ?> getHashtable() {
			return hashtable;
		}

		public void setHashtable(Hashtable<?, ?> hashtable) {
			this.hashtable = hashtable;
		}
	}

}
