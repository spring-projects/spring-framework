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

package org.springframework.beans.factory.config;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 31.07.2004
 */
public final class CustomEditorConfigurerTests {

	@Test
	public void testCustomEditorConfigurerWithPropertyEditorRegistrar() throws ParseException {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		CustomEditorConfigurer cec = new CustomEditorConfigurer();
		final DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.GERMAN);
		cec.setPropertyEditorRegistrars(new PropertyEditorRegistrar[] {
				new PropertyEditorRegistrar() {
					@Override
					public void registerCustomEditors(PropertyEditorRegistry registry) {
						registry.registerCustomEditor(Date.class, new CustomDateEditor(df, true));
					}
				}});
		cec.postProcessBeanFactory(bf);

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("date", "2.12.1975");
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setPropertyValues(pvs);
		bf.registerBeanDefinition("tb1", bd1);
		pvs = new MutablePropertyValues();
		pvs.add("someMap[myKey]", new TypedStringValue("2.12.1975", Date.class));
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setPropertyValues(pvs);
		bf.registerBeanDefinition("tb2", bd2);

		TestBean tb1 = (TestBean) bf.getBean("tb1");
		assertEquals(df.parse("2.12.1975"), tb1.getDate());
		TestBean tb2 = (TestBean) bf.getBean("tb2");
		assertEquals(df.parse("2.12.1975"), tb2.getSomeMap().get("myKey"));
	}

	@Test
	public void testCustomEditorConfigurerWithEditorAsClass() throws ParseException {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		CustomEditorConfigurer cec = new CustomEditorConfigurer();
		Map<Class<?>, Class<? extends PropertyEditor>> editors = new HashMap<Class<?>, Class<? extends PropertyEditor>>();
		editors.put(Date.class, MyDateEditor.class);
		cec.setCustomEditors(editors);
		cec.postProcessBeanFactory(bf);

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("date", "2.12.1975");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		bf.registerBeanDefinition("tb", bd);

		TestBean tb = (TestBean) bf.getBean("tb");
		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.GERMAN);
		assertEquals(df.parse("2.12.1975"), tb.getDate());
	}

	@Test
	public void testCustomEditorConfigurerWithRequiredTypeArray() throws ParseException {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		CustomEditorConfigurer cec = new CustomEditorConfigurer();
		Map<Class<?>, Class<? extends PropertyEditor>> editors = new HashMap<Class<?>, Class<? extends PropertyEditor>>();
		editors.put(String[].class, MyTestEditor.class);
		cec.setCustomEditors(editors);
		cec.postProcessBeanFactory(bf);

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("stringArray", "xxx");
		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
		bd.setPropertyValues(pvs);
		bf.registerBeanDefinition("tb", bd);

		TestBean tb = (TestBean) bf.getBean("tb");
		assertTrue(tb.getStringArray() != null && tb.getStringArray().length == 1);
		assertEquals("test", tb.getStringArray()[0]);
	}


	public static class MyDateEditor extends CustomDateEditor {

		public MyDateEditor() {
			super(DateFormat.getDateInstance(DateFormat.SHORT, Locale.GERMAN), true);
		}
	}


	public static class MyTestEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) {
			setValue(new String[] {"test"});
		}
	}

}
