/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.*;

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

import org.junit.Test;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.tests.sample.beans.BooleanTestBean;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.IndexedTestBean;
import org.springframework.tests.sample.beans.NumberTestBean;
import org.springframework.tests.sample.beans.TestBean;


/**
 * Unit tests for the various PropertyEditors in Spring.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Rob Harrop
 * @author Arjen Poutsma
 * @author Chris Beams
 *
 * @since 10.06.2003
 */
public class CustomEditorTests {

	@Test
	public void testComplexObject() {
		TestBean tb = new TestBean();
		String newName = "Rod";
		String tbString = "Kerry_34";

		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(ITestBean.class, new TestBeanEditor());
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("age", new Integer(55)));
		pvs.addPropertyValue(new PropertyValue("name", newName));
		pvs.addPropertyValue(new PropertyValue("touchy", "valid"));
		pvs.addPropertyValue(new PropertyValue("spouse", tbString));
		bw.setPropertyValues(pvs);
		assertTrue("spouse is non-null", tb.getSpouse() != null);
		assertTrue("spouse name is Kerry and age is 34",
				tb.getSpouse().getName().equals("Kerry") && tb.getSpouse().getAge() == 34);
	}

	@Test
	public void testComplexObjectWithOldValueAccess() {
		TestBean tb = new TestBean();
		String newName = "Rod";
		String tbString = "Kerry_34";

		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.setExtractOldValueForEditor(true);
		bw.registerCustomEditor(ITestBean.class, new OldValueAccessingTestBeanEditor());
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue(new PropertyValue("age", new Integer(55)));
		pvs.addPropertyValue(new PropertyValue("name", newName));
		pvs.addPropertyValue(new PropertyValue("touchy", "valid"));
		pvs.addPropertyValue(new PropertyValue("spouse", tbString));

		bw.setPropertyValues(pvs);
		assertTrue("spouse is non-null", tb.getSpouse() != null);
		assertTrue("spouse name is Kerry and age is 34",
				tb.getSpouse().getName().equals("Kerry") && tb.getSpouse().getAge() == 34);
		ITestBean spouse = tb.getSpouse();

		bw.setPropertyValues(pvs);
		assertSame("Should have remained same object", spouse, tb.getSpouse());
	}

	@Test
	public void testCustomEditorForSingleProperty() {
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
		assertEquals("prefixvalue", bw.getPropertyValue("name"));
		assertEquals("prefixvalue", tb.getName());
		assertEquals("value", bw.getPropertyValue("touchy"));
		assertEquals("value", tb.getTouchy());
	}

	@Test
	public void testCustomEditorForAllStringProperties() {
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
		assertEquals("prefixvalue", bw.getPropertyValue("name"));
		assertEquals("prefixvalue", tb.getName());
		assertEquals("prefixvalue", bw.getPropertyValue("touchy"));
		assertEquals("prefixvalue", tb.getTouchy());
	}

	@Test
	public void testCustomEditorForSingleNestedProperty() {
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
		assertEquals("prefixvalue", bw.getPropertyValue("spouse.name"));
		assertEquals("prefixvalue", tb.getSpouse().getName());
		assertEquals("value", bw.getPropertyValue("touchy"));
		assertEquals("value", tb.getTouchy());
	}

	@Test
	public void testCustomEditorForAllNestedStringProperties() {
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
		assertEquals("prefixvalue", bw.getPropertyValue("spouse.name"));
		assertEquals("prefixvalue", tb.getSpouse().getName());
		assertEquals("prefixvalue", bw.getPropertyValue("touchy"));
		assertEquals("prefixvalue", tb.getTouchy());
	}

	@Test
	public void testDefaultBooleanEditorForPrimitiveType() {
		BooleanTestBean tb = new BooleanTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);

		bw.setPropertyValue("bool1", "true");
		assertTrue("Correct bool1 value", Boolean.TRUE.equals(bw.getPropertyValue("bool1")));
		assertTrue("Correct bool1 value", tb.isBool1());

		bw.setPropertyValue("bool1", "false");
		assertTrue("Correct bool1 value", Boolean.FALSE.equals(bw.getPropertyValue("bool1")));
		assertTrue("Correct bool1 value", !tb.isBool1());

		bw.setPropertyValue("bool1", "  true  ");
		assertTrue("Correct bool1 value", tb.isBool1());

		bw.setPropertyValue("bool1", "  false  ");
		assertTrue("Correct bool1 value", !tb.isBool1());

		bw.setPropertyValue("bool1", "on");
		assertTrue("Correct bool1 value", tb.isBool1());

		bw.setPropertyValue("bool1", "off");
		assertTrue("Correct bool1 value", !tb.isBool1());

		bw.setPropertyValue("bool1", "yes");
		assertTrue("Correct bool1 value", tb.isBool1());

		bw.setPropertyValue("bool1", "no");
		assertTrue("Correct bool1 value", !tb.isBool1());

		bw.setPropertyValue("bool1", "1");
		assertTrue("Correct bool1 value", tb.isBool1());

		bw.setPropertyValue("bool1", "0");
		assertTrue("Correct bool1 value", !tb.isBool1());

		try {
			bw.setPropertyValue("bool1", "argh");
			fail("Should have thrown BeansException");
		}
		catch (BeansException ex) {
			// expected
		}
	}

	@Test
	public void testDefaultBooleanEditorForWrapperType() {
		BooleanTestBean tb = new BooleanTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);

		bw.setPropertyValue("bool2", "true");
		assertTrue("Correct bool2 value", Boolean.TRUE.equals(bw.getPropertyValue("bool2")));
		assertTrue("Correct bool2 value", tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "false");
		assertTrue("Correct bool2 value", Boolean.FALSE.equals(bw.getPropertyValue("bool2")));
		assertTrue("Correct bool2 value", !tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "on");
		assertTrue("Correct bool2 value", tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "off");
		assertTrue("Correct bool2 value", !tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "yes");
		assertTrue("Correct bool2 value", tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "no");
		assertTrue("Correct bool2 value", !tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "1");
		assertTrue("Correct bool2 value", tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "0");
		assertTrue("Correct bool2 value", !tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "");
		assertNull("Correct bool2 value", tb.getBool2());
	}

	@Test
	public void testCustomBooleanEditorWithAllowEmpty() {
		BooleanTestBean tb = new BooleanTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(Boolean.class, new CustomBooleanEditor(true));

		bw.setPropertyValue("bool2", "true");
		assertTrue("Correct bool2 value", Boolean.TRUE.equals(bw.getPropertyValue("bool2")));
		assertTrue("Correct bool2 value", tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "false");
		assertTrue("Correct bool2 value", Boolean.FALSE.equals(bw.getPropertyValue("bool2")));
		assertTrue("Correct bool2 value", !tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "on");
		assertTrue("Correct bool2 value", tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "off");
		assertTrue("Correct bool2 value", !tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "yes");
		assertTrue("Correct bool2 value", tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "no");
		assertTrue("Correct bool2 value", !tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "1");
		assertTrue("Correct bool2 value", tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "0");
		assertTrue("Correct bool2 value", !tb.getBool2().booleanValue());

		bw.setPropertyValue("bool2", "");
		assertTrue("Correct bool2 value", bw.getPropertyValue("bool2") == null);
		assertTrue("Correct bool2 value", tb.getBool2() == null);
	}

	@Test
	public void testCustomBooleanEditorWithSpecialTrueAndFalseStrings() throws Exception {
		final String trueString = "pechorin";
		final String falseString = "nash";

		CustomBooleanEditor editor = new CustomBooleanEditor(trueString, falseString, false);

		editor.setAsText(trueString);
		assertTrue(((Boolean) editor.getValue()).booleanValue());
		assertEquals(trueString, editor.getAsText());
		editor.setAsText(falseString);
		assertFalse(((Boolean) editor.getValue()).booleanValue());
		assertEquals(falseString, editor.getAsText());

		editor.setAsText(trueString.toUpperCase());
		assertTrue(((Boolean) editor.getValue()).booleanValue());
		assertEquals(trueString, editor.getAsText());
		editor.setAsText(falseString.toUpperCase());
		assertFalse(((Boolean) editor.getValue()).booleanValue());
		assertEquals(falseString, editor.getAsText());
	}

	@Test
	public void testDefaultNumberEditor() {
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

		assertTrue("Correct short1 value", new Short("1").equals(bw.getPropertyValue("short1")));
		assertTrue("Correct short1 value", tb.getShort1() == 1);
		assertTrue("Correct short2 value", new Short("2").equals(bw.getPropertyValue("short2")));
		assertTrue("Correct short2 value", new Short("2").equals(tb.getShort2()));
		assertTrue("Correct int1 value", new Integer("7").equals(bw.getPropertyValue("int1")));
		assertTrue("Correct int1 value", tb.getInt1() == 7);
		assertTrue("Correct int2 value", new Integer("8").equals(bw.getPropertyValue("int2")));
		assertTrue("Correct int2 value", new Integer("8").equals(tb.getInt2()));
		assertTrue("Correct long1 value", new Long("5").equals(bw.getPropertyValue("long1")));
		assertTrue("Correct long1 value", tb.getLong1() == 5);
		assertTrue("Correct long2 value", new Long("6").equals(bw.getPropertyValue("long2")));
		assertTrue("Correct long2 value", new Long("6").equals(tb.getLong2()));
		assertTrue("Correct bigInteger value", new BigInteger("3").equals(bw.getPropertyValue("bigInteger")));
		assertTrue("Correct bigInteger value", new BigInteger("3").equals(tb.getBigInteger()));
		assertTrue("Correct float1 value", new Float("7.1").equals(bw.getPropertyValue("float1")));
		assertTrue("Correct float1 value", new Float("7.1").equals(new Float(tb.getFloat1())));
		assertTrue("Correct float2 value", new Float("8.1").equals(bw.getPropertyValue("float2")));
		assertTrue("Correct float2 value", new Float("8.1").equals(tb.getFloat2()));
		assertTrue("Correct double1 value", new Double("5.1").equals(bw.getPropertyValue("double1")));
		assertTrue("Correct double1 value", tb.getDouble1() == 5.1);
		assertTrue("Correct double2 value", new Double("6.1").equals(bw.getPropertyValue("double2")));
		assertTrue("Correct double2 value", new Double("6.1").equals(tb.getDouble2()));
		assertTrue("Correct bigDecimal value", new BigDecimal("4.5").equals(bw.getPropertyValue("bigDecimal")));
		assertTrue("Correct bigDecimal value", new BigDecimal("4.5").equals(tb.getBigDecimal()));
	}

	@Test
	public void testCustomNumberEditorWithoutAllowEmpty() {
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

		assertTrue("Correct short1 value", new Short("1").equals(bw.getPropertyValue("short1")));
		assertTrue("Correct short1 value", tb.getShort1() == 1);
		assertTrue("Correct short2 value", new Short("2").equals(bw.getPropertyValue("short2")));
		assertTrue("Correct short2 value", new Short("2").equals(tb.getShort2()));
		assertTrue("Correct int1 value", new Integer("7").equals(bw.getPropertyValue("int1")));
		assertTrue("Correct int1 value", tb.getInt1() == 7);
		assertTrue("Correct int2 value", new Integer("8").equals(bw.getPropertyValue("int2")));
		assertTrue("Correct int2 value", new Integer("8").equals(tb.getInt2()));
		assertTrue("Correct long1 value", new Long("5").equals(bw.getPropertyValue("long1")));
		assertTrue("Correct long1 value", tb.getLong1() == 5);
		assertTrue("Correct long2 value", new Long("6").equals(bw.getPropertyValue("long2")));
		assertTrue("Correct long2 value", new Long("6").equals(tb.getLong2()));
		assertTrue("Correct bigInteger value", new BigInteger("3").equals(bw.getPropertyValue("bigInteger")));
		assertTrue("Correct bigInteger value", new BigInteger("3").equals(tb.getBigInteger()));
		assertTrue("Correct float1 value", new Float("7.1").equals(bw.getPropertyValue("float1")));
		assertTrue("Correct float1 value", new Float("7.1").equals(new Float(tb.getFloat1())));
		assertTrue("Correct float2 value", new Float("8.1").equals(bw.getPropertyValue("float2")));
		assertTrue("Correct float2 value", new Float("8.1").equals(tb.getFloat2()));
		assertTrue("Correct double1 value", new Double("5.1").equals(bw.getPropertyValue("double1")));
		assertTrue("Correct double1 value", tb.getDouble1() == 5.1);
		assertTrue("Correct double2 value", new Double("6.1").equals(bw.getPropertyValue("double2")));
		assertTrue("Correct double2 value", new Double("6.1").equals(tb.getDouble2()));
		assertTrue("Correct bigDecimal value", new BigDecimal("4.5").equals(bw.getPropertyValue("bigDecimal")));
		assertTrue("Correct bigDecimal value", new BigDecimal("4.5").equals(tb.getBigDecimal()));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCustomNumberEditorCtorWithNullNumberType() throws Exception {
		new CustomNumberEditor(null, true);
	}

	@Test
	public void testCustomNumberEditorWithAllowEmpty() {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.GERMAN);
		NumberTestBean tb = new NumberTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(long.class, new CustomNumberEditor(Long.class, nf, true));
		bw.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, nf, true));

		bw.setPropertyValue("long1", "5");
		bw.setPropertyValue("long2", "6");
		assertTrue("Correct long1 value", new Long("5").equals(bw.getPropertyValue("long1")));
		assertTrue("Correct long1 value", tb.getLong1() == 5);
		assertTrue("Correct long2 value", new Long("6").equals(bw.getPropertyValue("long2")));
		assertTrue("Correct long2 value", new Long("6").equals(tb.getLong2()));

		bw.setPropertyValue("long2", "");
		assertTrue("Correct long2 value", bw.getPropertyValue("long2") == null);
		assertTrue("Correct long2 value", tb.getLong2() == null);

		try {
			bw.setPropertyValue("long1", "");
			fail("Should have thrown BeansException");
		}
		catch (BeansException ex) {
			// expected
			assertTrue("Correct long1 value", new Long("5").equals(bw.getPropertyValue("long1")));
			assertTrue("Correct long1 value", tb.getLong1() == 5);
		}
	}

	@Test
	public void testCustomNumberEditorWithFrenchBigDecimal() throws Exception {
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRENCH);
		NumberTestBean tb = new NumberTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, nf, true));
		bw.setPropertyValue("bigDecimal", "1000");
		assertEquals(1000.0f, tb.getBigDecimal().floatValue(), 0f);
		bw.setPropertyValue("bigDecimal", "1000,5");
		assertEquals(1000.5f, tb.getBigDecimal().floatValue(), 0f);
		bw.setPropertyValue("bigDecimal", "1 000,5");
		assertEquals(1000.5f, tb.getBigDecimal().floatValue(), 0f);
	}

	@Test
	public void testParseShortGreaterThanMaxValueWithoutNumberFormat() {
		try {
			CustomNumberEditor editor = new CustomNumberEditor(Short.class, true);
			editor.setAsText(String.valueOf(Short.MAX_VALUE + 1));
			fail(Short.MAX_VALUE + 1 + " is greater than max value");
		} catch (NumberFormatException ex) {
			// expected
		}
	}

	@Test
	public void testByteArrayPropertyEditor() {
		PrimitiveArrayBean bean = new PrimitiveArrayBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.setPropertyValue("byteArray", "myvalue");
		assertEquals("myvalue", new String(bean.getByteArray()));
	}

	@Test
	public void testCharArrayPropertyEditor() {
		PrimitiveArrayBean bean = new PrimitiveArrayBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.setPropertyValue("charArray", "myvalue");
		assertEquals("myvalue", new String(bean.getCharArray()));
	}

	@Test
	public void testCharacterEditor() {
		CharBean cb = new CharBean();
		BeanWrapper bw = new BeanWrapperImpl(cb);

		bw.setPropertyValue("myChar", new Character('c'));
		assertEquals('c', cb.getMyChar());

		bw.setPropertyValue("myChar", "c");
		assertEquals('c', cb.getMyChar());

		bw.setPropertyValue("myChar", "\u0041");
		assertEquals('A', cb.getMyChar());

		bw.setPropertyValue("myChar", "\\u0022");
		assertEquals('"', cb.getMyChar());

		CharacterEditor editor = new CharacterEditor(false);
		editor.setAsText("M");
		assertEquals("M", editor.getAsText());
	}

	@Test
	public void testCharacterEditorWithAllowEmpty() {
		CharBean cb = new CharBean();
		BeanWrapper bw = new BeanWrapperImpl(cb);
		bw.registerCustomEditor(Character.class, new CharacterEditor(true));

		bw.setPropertyValue("myCharacter", new Character('c'));
		assertEquals(new Character('c'), cb.getMyCharacter());

		bw.setPropertyValue("myCharacter", "c");
		assertEquals(new Character('c'), cb.getMyCharacter());

		bw.setPropertyValue("myCharacter", "\u0041");
		assertEquals(new Character('A'), cb.getMyCharacter());

		bw.setPropertyValue("myCharacter", " ");
		assertEquals(new Character(' '), cb.getMyCharacter());

		bw.setPropertyValue("myCharacter", "");
		assertNull(cb.getMyCharacter());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCharacterEditorSetAsTextWithStringLongerThanOneCharacter() throws Exception {
		PropertyEditor charEditor = new CharacterEditor(false);
		charEditor.setAsText("ColdWaterCanyon");
	}

	@Test
	public void testCharacterEditorGetAsTextReturnsEmptyStringIfValueIsNull() throws Exception {
		PropertyEditor charEditor = new CharacterEditor(false);
		assertEquals("", charEditor.getAsText());
		charEditor = new CharacterEditor(true);
		charEditor.setAsText(null);
		assertEquals("", charEditor.getAsText());
		charEditor.setAsText("");
		assertEquals("", charEditor.getAsText());
		charEditor.setAsText(" ");
		assertEquals(" ", charEditor.getAsText());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCharacterEditorSetAsTextWithNullNotAllowingEmptyAsNull() throws Exception {
		PropertyEditor charEditor = new CharacterEditor(false);
		charEditor.setAsText(null);
	}

	@Test
	public void testClassEditor() {
		PropertyEditor classEditor = new ClassEditor();
		classEditor.setAsText(TestBean.class.getName());
		assertEquals(TestBean.class, classEditor.getValue());
		assertEquals(TestBean.class.getName(), classEditor.getAsText());

		classEditor.setAsText(null);
		assertEquals("", classEditor.getAsText());
		classEditor.setAsText("");
		assertEquals("", classEditor.getAsText());
		classEditor.setAsText("\t  ");
		assertEquals("", classEditor.getAsText());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testClassEditorWithNonExistentClass() throws Exception {
		PropertyEditor classEditor = new ClassEditor();
		classEditor.setAsText("hairdresser.on.Fire");
	}

	@Test
	public void testClassEditorWithArray() {
		PropertyEditor classEditor = new ClassEditor();
		classEditor.setAsText("org.springframework.tests.sample.beans.TestBean[]");
		assertEquals(TestBean[].class, classEditor.getValue());
		assertEquals("org.springframework.tests.sample.beans.TestBean[]", classEditor.getAsText());
	}

	/*
	* SPR_2165 - ClassEditor is inconsistent with multidimensional arrays
	*/
	@Test
	public void testGetAsTextWithTwoDimensionalArray() throws Exception {
		String[][] chessboard = new String[8][8];
		ClassEditor editor = new ClassEditor();
		editor.setValue(chessboard.getClass());
		assertEquals("java.lang.String[][]", editor.getAsText());
	}

	/*
	 * SPR_2165 - ClassEditor is inconsistent with multidimensional arrays
	 */
	@Test
	public void testGetAsTextWithRidiculousMultiDimensionalArray() throws Exception {
		String[][][][][] ridiculousChessboard = new String[8][4][0][1][3];
		ClassEditor editor = new ClassEditor();
		editor.setValue(ridiculousChessboard.getClass());
		assertEquals("java.lang.String[][][][][]", editor.getAsText());
	}

	@Test
	public void testFileEditor() {
		PropertyEditor fileEditor = new FileEditor();
		fileEditor.setAsText("file:myfile.txt");
		assertEquals(new File("myfile.txt"), fileEditor.getValue());
		assertEquals((new File("myfile.txt")).getPath(), fileEditor.getAsText());
	}

	@Test
	public void testFileEditorWithRelativePath() {
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
	public void testFileEditorWithAbsolutePath() {
		PropertyEditor fileEditor = new FileEditor();
		// testing on Windows
		if (new File("C:/myfile.txt").isAbsolute()) {
			fileEditor.setAsText("C:/myfile.txt");
			assertEquals(new File("C:/myfile.txt"), fileEditor.getValue());
		}
		// testing on Unix
		if (new File("/myfile.txt").isAbsolute()) {
			fileEditor.setAsText("/myfile.txt");
			assertEquals(new File("/myfile.txt"), fileEditor.getValue());
		}
	}

	@Test
	public void testLocaleEditor() {
		PropertyEditor localeEditor = new LocaleEditor();
		localeEditor.setAsText("en_CA");
		assertEquals(Locale.CANADA, localeEditor.getValue());
		assertEquals("en_CA", localeEditor.getAsText());

		localeEditor = new LocaleEditor();
		assertEquals("", localeEditor.getAsText());
	}

	@Test
	public void testPatternEditor() {
		final String REGEX = "a.*";

		PropertyEditor patternEditor = new PatternEditor();
		patternEditor.setAsText(REGEX);
		assertEquals(Pattern.compile(REGEX).pattern(), ((Pattern) patternEditor.getValue()).pattern());
		assertEquals(REGEX, patternEditor.getAsText());

		patternEditor = new PatternEditor();
		assertEquals("", patternEditor.getAsText());

		patternEditor = new PatternEditor();
		patternEditor.setAsText(null);
		assertEquals("", patternEditor.getAsText());
	}

	@Test
	public void testCustomBooleanEditor() {
		CustomBooleanEditor editor = new CustomBooleanEditor(false);
		editor.setAsText("true");
		assertEquals(Boolean.TRUE, editor.getValue());
		assertEquals("true", editor.getAsText());
		editor.setAsText("false");
		assertEquals(Boolean.FALSE, editor.getValue());
		assertEquals("false", editor.getAsText());
		editor.setValue(null);
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testCustomBooleanEditorWithEmptyAsNull() {
		CustomBooleanEditor editor = new CustomBooleanEditor(true);
		editor.setAsText("true");
		assertEquals(Boolean.TRUE, editor.getValue());
		assertEquals("true", editor.getAsText());
		editor.setAsText("false");
		assertEquals(Boolean.FALSE, editor.getValue());
		assertEquals("false", editor.getAsText());
		editor.setValue(null);
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testCustomDateEditor() {
		CustomDateEditor editor = new CustomDateEditor(null, false);
		editor.setValue(null);
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testCustomDateEditorWithEmptyAsNull() {
		CustomDateEditor editor = new CustomDateEditor(null, true);
		editor.setValue(null);
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testCustomDateEditorWithExactDateLength() {
		int maxLength = 10;
		String validDate = "01/01/2005";
		String invalidDate = "01/01/05";

		assertTrue(validDate.length() == maxLength);
		assertFalse(invalidDate.length() == maxLength);

		CustomDateEditor editor = new CustomDateEditor(new SimpleDateFormat("MM/dd/yyyy"), true, maxLength);

		try {
			editor.setAsText(validDate);
		}
		catch (IllegalArgumentException ex) {
			fail("Exception shouldn't be thrown because this is a valid date");
		}

		try {
			editor.setAsText(invalidDate);
			fail("Exception should be thrown because this is an invalid date");
		}
		catch (IllegalArgumentException ex) {
			// expected
			assertTrue(ex.getMessage().indexOf("10") != -1);
		}
	}

	@Test
	public void testCustomNumberEditor() {
		CustomNumberEditor editor = new CustomNumberEditor(Integer.class, false);
		editor.setAsText("5");
		assertEquals(new Integer(5), editor.getValue());
		assertEquals("5", editor.getAsText());
		editor.setValue(null);
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testCustomNumberEditorWithHex() {
		CustomNumberEditor editor = new CustomNumberEditor(Integer.class, false);
		editor.setAsText("0x" + Integer.toHexString(64));
		assertEquals(new Integer(64), editor.getValue());
	}

	@Test
	public void testCustomNumberEditorWithEmptyAsNull() {
		CustomNumberEditor editor = new CustomNumberEditor(Integer.class, true);
		editor.setAsText("5");
		assertEquals(new Integer(5), editor.getValue());
		assertEquals("5", editor.getAsText());
		editor.setAsText("");
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
		editor.setValue(null);
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testStringTrimmerEditor() {
		StringTrimmerEditor editor = new StringTrimmerEditor(false);
		editor.setAsText("test");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText(" test ");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText("");
		assertEquals("", editor.getValue());
		assertEquals("", editor.getAsText());
		editor.setValue(null);
		assertEquals("", editor.getAsText());
		editor.setAsText(null);
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testStringTrimmerEditorWithEmptyAsNull() {
		StringTrimmerEditor editor = new StringTrimmerEditor(true);
		editor.setAsText("test");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText(" test ");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText("  ");
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
		editor.setValue(null);
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testStringTrimmerEditorWithCharsToDelete() {
		StringTrimmerEditor editor = new StringTrimmerEditor("\r\n\f", false);
		editor.setAsText("te\ns\ft");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText(" test ");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText("");
		assertEquals("", editor.getValue());
		assertEquals("", editor.getAsText());
		editor.setValue(null);
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testStringTrimmerEditorWithCharsToDeleteAndEmptyAsNull() {
		StringTrimmerEditor editor = new StringTrimmerEditor("\r\n\f", true);
		editor.setAsText("te\ns\ft");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText(" test ");
		assertEquals("test", editor.getValue());
		assertEquals("test", editor.getAsText());
		editor.setAsText(" \n\f ");
		assertEquals(null, editor.getValue());
		assertEquals("", editor.getAsText());
		editor.setValue(null);
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testIndexedPropertiesWithCustomEditorForType() {
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
		assertEquals("name0", tb0.getName());
		assertEquals("name1", tb1.getName());
		assertEquals("name2", tb2.getName());
		assertEquals("name3", tb3.getName());
		assertEquals("name4", tb4.getName());
		assertEquals("name5", tb5.getName());
		assertEquals("name0", bw.getPropertyValue("array[0].name"));
		assertEquals("name1", bw.getPropertyValue("array[1].name"));
		assertEquals("name2", bw.getPropertyValue("list[0].name"));
		assertEquals("name3", bw.getPropertyValue("list[1].name"));
		assertEquals("name4", bw.getPropertyValue("map[key1].name"));
		assertEquals("name5", bw.getPropertyValue("map[key2].name"));
		assertEquals("name4", bw.getPropertyValue("map['key1'].name"));
		assertEquals("name5", bw.getPropertyValue("map[\"key2\"].name"));

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].name", "name5");
		pvs.add("array[1].name", "name4");
		pvs.add("list[0].name", "name3");
		pvs.add("list[1].name", "name2");
		pvs.add("map[key1].name", "name1");
		pvs.add("map['key2'].name", "name0");
		bw.setPropertyValues(pvs);
		assertEquals("prefixname5", tb0.getName());
		assertEquals("prefixname4", tb1.getName());
		assertEquals("prefixname3", tb2.getName());
		assertEquals("prefixname2", tb3.getName());
		assertEquals("prefixname1", tb4.getName());
		assertEquals("prefixname0", tb5.getName());
		assertEquals("prefixname5", bw.getPropertyValue("array[0].name"));
		assertEquals("prefixname4", bw.getPropertyValue("array[1].name"));
		assertEquals("prefixname3", bw.getPropertyValue("list[0].name"));
		assertEquals("prefixname2", bw.getPropertyValue("list[1].name"));
		assertEquals("prefixname1", bw.getPropertyValue("map[\"key1\"].name"));
		assertEquals("prefixname0", bw.getPropertyValue("map['key2'].name"));
	}

	@Test
	public void testIndexedPropertiesWithCustomEditorForProperty() {
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
		assertEquals("name0", tb0.getName());
		assertEquals("name1", tb1.getName());
		assertEquals("name2", tb2.getName());
		assertEquals("name3", tb3.getName());
		assertEquals("name4", tb4.getName());
		assertEquals("name5", tb5.getName());
		assertEquals("name0", bw.getPropertyValue("array[0].name"));
		assertEquals("name1", bw.getPropertyValue("array[1].name"));
		assertEquals("name2", bw.getPropertyValue("list[0].name"));
		assertEquals("name3", bw.getPropertyValue("list[1].name"));
		assertEquals("name4", bw.getPropertyValue("map[key1].name"));
		assertEquals("name5", bw.getPropertyValue("map[key2].name"));
		assertEquals("name4", bw.getPropertyValue("map['key1'].name"));
		assertEquals("name5", bw.getPropertyValue("map[\"key2\"].name"));

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].name", "name5");
		pvs.add("array[1].name", "name4");
		pvs.add("list[0].name", "name3");
		pvs.add("list[1].name", "name2");
		pvs.add("map[key1].name", "name1");
		pvs.add("map['key2'].name", "name0");
		bw.setPropertyValues(pvs);
		assertEquals("arrayname5", tb0.getName());
		assertEquals("arrayname4", tb1.getName());
		assertEquals("listname3", tb2.getName());
		assertEquals("listname2", tb3.getName());
		assertEquals("mapname1", tb4.getName());
		assertEquals("mapname0", tb5.getName());
		assertEquals("arrayname5", bw.getPropertyValue("array[0].name"));
		assertEquals("arrayname4", bw.getPropertyValue("array[1].name"));
		assertEquals("listname3", bw.getPropertyValue("list[0].name"));
		assertEquals("listname2", bw.getPropertyValue("list[1].name"));
		assertEquals("mapname1", bw.getPropertyValue("map[\"key1\"].name"));
		assertEquals("mapname0", bw.getPropertyValue("map['key2'].name"));
	}

	@Test
	public void testIndexedPropertiesWithIndividualCustomEditorForProperty() {
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
		assertEquals("name0", tb0.getName());
		assertEquals("name1", tb1.getName());
		assertEquals("name2", tb2.getName());
		assertEquals("name3", tb3.getName());
		assertEquals("name4", tb4.getName());
		assertEquals("name5", tb5.getName());
		assertEquals("name0", bw.getPropertyValue("array[0].name"));
		assertEquals("name1", bw.getPropertyValue("array[1].name"));
		assertEquals("name2", bw.getPropertyValue("list[0].name"));
		assertEquals("name3", bw.getPropertyValue("list[1].name"));
		assertEquals("name4", bw.getPropertyValue("map[key1].name"));
		assertEquals("name5", bw.getPropertyValue("map[key2].name"));
		assertEquals("name4", bw.getPropertyValue("map['key1'].name"));
		assertEquals("name5", bw.getPropertyValue("map[\"key2\"].name"));

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].name", "name5");
		pvs.add("array[1].name", "name4");
		pvs.add("list[0].name", "name3");
		pvs.add("list[1].name", "name2");
		pvs.add("map[key1].name", "name1");
		pvs.add("map['key2'].name", "name0");
		bw.setPropertyValues(pvs);
		assertEquals("array0name5", tb0.getName());
		assertEquals("array1name4", tb1.getName());
		assertEquals("list0name3", tb2.getName());
		assertEquals("list1name2", tb3.getName());
		assertEquals("mapkey1name1", tb4.getName());
		assertEquals("mapkey2name0", tb5.getName());
		assertEquals("array0name5", bw.getPropertyValue("array[0].name"));
		assertEquals("array1name4", bw.getPropertyValue("array[1].name"));
		assertEquals("list0name3", bw.getPropertyValue("list[0].name"));
		assertEquals("list1name2", bw.getPropertyValue("list[1].name"));
		assertEquals("mapkey1name1", bw.getPropertyValue("map[\"key1\"].name"));
		assertEquals("mapkey2name0", bw.getPropertyValue("map['key2'].name"));
	}

	@Test
	public void testNestedIndexedPropertiesWithCustomEditorForProperty() {
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
		assertEquals("name0", tb0.getName());
		assertEquals("name1", tb1.getName());
		assertEquals("name2", tb2.getName());
		assertEquals("name3", tb3.getName());
		assertEquals("name4", tb4.getName());
		assertEquals("name5", tb5.getName());
		assertEquals("name0", bw.getPropertyValue("array[0].nestedIndexedBean.array[0].name"));
		assertEquals("name1", bw.getPropertyValue("array[1].nestedIndexedBean.array[1].name"));
		assertEquals("name2", bw.getPropertyValue("list[0].nestedIndexedBean.list[0].name"));
		assertEquals("name3", bw.getPropertyValue("list[1].nestedIndexedBean.list[1].name"));
		assertEquals("name4", bw.getPropertyValue("map[key1].nestedIndexedBean.map[key1].name"));
		assertEquals("name5", bw.getPropertyValue("map['key2'].nestedIndexedBean.map[\"key2\"].name"));

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("array[0].nestedIndexedBean.array[0].name", "name5");
		pvs.add("array[1].nestedIndexedBean.array[1].name", "name4");
		pvs.add("list[0].nestedIndexedBean.list[0].name", "name3");
		pvs.add("list[1].nestedIndexedBean.list[1].name", "name2");
		pvs.add("map[key1].nestedIndexedBean.map[\"key1\"].name", "name1");
		pvs.add("map['key2'].nestedIndexedBean.map[key2].name", "name0");
		bw.setPropertyValues(pvs);
		assertEquals("arrayname5", tb0.getNestedIndexedBean().getArray()[0].getName());
		assertEquals("arrayname4", tb1.getNestedIndexedBean().getArray()[1].getName());
		assertEquals("listname3", ((TestBean) tb2.getNestedIndexedBean().getList().get(0)).getName());
		assertEquals("listname2", ((TestBean) tb3.getNestedIndexedBean().getList().get(1)).getName());
		assertEquals("mapname1", ((TestBean) tb4.getNestedIndexedBean().getMap().get("key1")).getName());
		assertEquals("mapname0", ((TestBean) tb5.getNestedIndexedBean().getMap().get("key2")).getName());
		assertEquals("arrayname5", bw.getPropertyValue("array[0].nestedIndexedBean.array[0].name"));
		assertEquals("arrayname4", bw.getPropertyValue("array[1].nestedIndexedBean.array[1].name"));
		assertEquals("listname3", bw.getPropertyValue("list[0].nestedIndexedBean.list[0].name"));
		assertEquals("listname2", bw.getPropertyValue("list[1].nestedIndexedBean.list[1].name"));
		assertEquals("mapname1", bw.getPropertyValue("map['key1'].nestedIndexedBean.map[key1].name"));
		assertEquals("mapname0", bw.getPropertyValue("map[key2].nestedIndexedBean.map[\"key2\"].name"));
	}

	@Test
	public void testNestedIndexedPropertiesWithIndexedCustomEditorForProperty() {
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
		assertEquals("arrayname5", tb0.getNestedIndexedBean().getArray()[0].getName());
		assertEquals("name4", tb1.getNestedIndexedBean().getArray()[1].getName());
		assertEquals("name3", ((TestBean) tb2.getNestedIndexedBean().getList().get(0)).getName());
		assertEquals("listname2", ((TestBean) tb3.getNestedIndexedBean().getList().get(1)).getName());
		assertEquals("mapname1", ((TestBean) tb4.getNestedIndexedBean().getMap().get("key1")).getName());
		assertEquals("name0", ((TestBean) tb5.getNestedIndexedBean().getMap().get("key2")).getName());
	}

	@Test
	public void testIndexedPropertiesWithDirectAccessAndPropertyEditors() {
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
		assertEquals("arraya", bean.getArray()[0].getName());
		assertEquals("arrayb", bean.getArray()[1].getName());
		assertEquals("listc", ((TestBean) bean.getList().get(0)).getName());
		assertEquals("listd", ((TestBean) bean.getList().get(1)).getName());
		assertEquals("mape", ((TestBean) bean.getMap().get("key1")).getName());
		assertEquals("mapf", ((TestBean) bean.getMap().get("key2")).getName());
	}

	@Test
	public void testIndexedPropertiesWithDirectAccessAndSpecificPropertyEditors() {
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
		assertEquals("array0a", bean.getArray()[0].getName());
		assertEquals("array1b", bean.getArray()[1].getName());
		assertEquals("list0c", ((TestBean) bean.getList().get(0)).getName());
		assertEquals("list1d", ((TestBean) bean.getList().get(1)).getName());
		assertEquals("mapkey1e", ((TestBean) bean.getMap().get("key1")).getName());
		assertEquals("mapkey2f", ((TestBean) bean.getMap().get("key2")).getName());
	}

	@Test
	public void testIndexedPropertiesWithListPropertyEditor() {
		IndexedTestBean bean = new IndexedTestBean();
		BeanWrapper bw = new BeanWrapperImpl(bean);
		bw.registerCustomEditor(List.class, "list", new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				List<TestBean> result = new ArrayList<TestBean>();
				result.add(new TestBean("list" + text, 99));
				setValue(result);
			}
		});
		bw.setPropertyValue("list", "1");
		assertEquals("list1", ((TestBean) bean.getList().get(0)).getName());
		bw.setPropertyValue("list[0]", "test");
		assertEquals("test", bean.getList().get(0));
	}

	@Test
	public void testConversionToOldCollections() throws PropertyVetoException {
		OldCollectionsBean tb = new OldCollectionsBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(Vector.class, new CustomCollectionEditor(Vector.class));
		bw.registerCustomEditor(Hashtable.class, new CustomMapEditor(Hashtable.class));

		bw.setPropertyValue("vector", new String[] {"a", "b"});
		assertEquals(2, tb.getVector().size());
		assertEquals("a", tb.getVector().get(0));
		assertEquals("b", tb.getVector().get(1));

		bw.setPropertyValue("hashtable", Collections.singletonMap("foo", "bar"));
		assertEquals(1, tb.getHashtable().size());
		assertEquals("bar", tb.getHashtable().get("foo"));
	}

	@Test
	public void testUninitializedArrayPropertyWithCustomEditor() {
		IndexedTestBean bean = new IndexedTestBean(false);
		BeanWrapper bw = new BeanWrapperImpl(bean);
		PropertyEditor pe = new CustomNumberEditor(Integer.class, true);
		bw.registerCustomEditor(null, "list.age", pe);
		TestBean tb = new TestBean();
		bw.setPropertyValue("list", new ArrayList<Object>());
		bw.setPropertyValue("list[0]", tb);
		assertEquals(tb, bean.getList().get(0));
		assertEquals(pe, bw.findCustomEditor(int.class, "list.age"));
		assertEquals(pe, bw.findCustomEditor(null, "list.age"));
		assertEquals(pe, bw.findCustomEditor(int.class, "list[0].age"));
		assertEquals(pe, bw.findCustomEditor(null, "list[0].age"));
	}

	@Test
	public void testArrayToArrayConversion() throws PropertyVetoException {
		IndexedTestBean tb = new IndexedTestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(TestBean.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(new TestBean(text, 99));
			}
		});
		bw.setPropertyValue("array", new String[] {"a", "b"});
		assertEquals(2, tb.getArray().length);
		assertEquals("a", tb.getArray()[0].getName());
		assertEquals("b", tb.getArray()[1].getName());
	}

	@Test
	public void testArrayToStringConversion() throws PropertyVetoException {
		TestBean tb = new TestBean();
		BeanWrapper bw = new BeanWrapperImpl(tb);
		bw.registerCustomEditor(String.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue("-" + text + "-");
			}
		});
		bw.setPropertyValue("name", new String[] {"a", "b"});
		assertEquals("-a,b-", tb.getName());
	}

	@Test
	public void testClassArrayEditorSunnyDay() throws Exception {
		ClassArrayEditor classArrayEditor = new ClassArrayEditor();
		classArrayEditor.setAsText("java.lang.String,java.util.HashMap");
		Class<?>[] classes = (Class<?>[]) classArrayEditor.getValue();
		assertEquals(2, classes.length);
		assertEquals(String.class, classes[0]);
		assertEquals(HashMap.class, classes[1]);
		assertEquals("java.lang.String,java.util.HashMap", classArrayEditor.getAsText());
		// ensure setAsText can consume the return value of getAsText
		classArrayEditor.setAsText(classArrayEditor.getAsText());
	}

	@Test
	public void testClassArrayEditorSunnyDayWithArrayTypes() throws Exception {
		ClassArrayEditor classArrayEditor = new ClassArrayEditor();
		classArrayEditor.setAsText("java.lang.String[],java.util.Map[],int[],float[][][]");
		Class<?>[] classes = (Class<?>[]) classArrayEditor.getValue();
		assertEquals(4, classes.length);
		assertEquals(String[].class, classes[0]);
		assertEquals(Map[].class, classes[1]);
		assertEquals(int[].class, classes[2]);
		assertEquals(float[][][].class, classes[3]);
		assertEquals("java.lang.String[],java.util.Map[],int[],float[][][]", classArrayEditor.getAsText());
		// ensure setAsText can consume the return value of getAsText
		classArrayEditor.setAsText(classArrayEditor.getAsText());
	}

	@Test
	public void testClassArrayEditorSetAsTextWithNull() throws Exception {
		ClassArrayEditor editor = new ClassArrayEditor();
		editor.setAsText(null);
		assertNull(editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testClassArrayEditorSetAsTextWithEmptyString() throws Exception {
		ClassArrayEditor editor = new ClassArrayEditor();
		editor.setAsText("");
		assertNull(editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testClassArrayEditorSetAsTextWithWhitespaceString() throws Exception {
		ClassArrayEditor editor = new ClassArrayEditor();
		editor.setAsText("\n");
		assertNull(editor.getValue());
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testCharsetEditor() throws Exception {
		CharsetEditor editor = new CharsetEditor();
		String name = "UTF-8";
		editor.setAsText(name);
		Charset charset = Charset.forName(name);
		assertEquals("Invalid Charset conversion", charset, editor.getValue());
		editor.setValue(charset);
		assertEquals("Invalid Charset conversion", name, editor.getAsText());
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
