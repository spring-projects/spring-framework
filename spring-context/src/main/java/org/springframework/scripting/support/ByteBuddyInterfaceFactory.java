/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.scripting.support;

import java.lang.invoke.MethodHandles;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.MethodManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

class ByteBuddyInterfaceFactory implements InterfaceFactory {

	private static final ClassLoadingStrategy<ClassLoader> STRATEGY = ClassFileVersion.ofThisVm().isAtLeast(ClassFileVersion.JAVA_V9)
			? ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup())
			: ClassLoadingStrategy.Default.INJECTION;

	@Override
	public Class<?> createConfigInterface(BeanDefinition bd, @NonNull Class<?>[] interfaces) {
		DynamicType.Builder<?> builder = new ByteBuddy().with(TypeValidation.DISABLED).makeInterface();
		PropertyValue[] pvs = bd.getPropertyValues().getPropertyValues();
		for (PropertyValue pv : pvs) {
			String propertyName = pv.getName();
			builder = builder.defineMethod(
				"set" + StringUtils.capitalize(propertyName),
				void.class,
				Visibility.PUBLIC, MethodManifestation.ABSTRACT
			).withParameters(BeanUtils.findPropertyType(propertyName, interfaces)).withoutCode();
		}
		if (bd.getInitMethodName() != null) {
			builder = builder.defineMethod(
					bd.getInitMethodName(),
					void.class,
					Visibility.PUBLIC, MethodManifestation.ABSTRACT
			).withoutCode();
		}
		if (StringUtils.hasText(bd.getDestroyMethodName())) {
			builder = builder.defineMethod(
					bd.getDestroyMethodName(),
					void.class,
					Visibility.PUBLIC, MethodManifestation.ABSTRACT
			).withoutCode();
		}
		return builder.make()
				.load(ByteBuddyInterfaceFactory.class.getClassLoader(), STRATEGY)
				.getLoaded();
	}
}
