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

import org.springframework.asm.Type;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cglib.core.Signature;
import org.springframework.cglib.proxy.InterfaceMaker;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

class CglibInterfaceFactory implements InterfaceFactory {

	@Override
	public Class<?> createConfigInterface(BeanDefinition bd, @NonNull Class<?>[] interfaces) {
		InterfaceMaker maker = new InterfaceMaker();
		PropertyValue[] pvs = bd.getPropertyValues().getPropertyValues();
		for (PropertyValue pv : pvs) {
			String propertyName = pv.getName();
			Class<?> propertyType = BeanUtils.findPropertyType(propertyName, interfaces);
			String setterName = "set" + StringUtils.capitalize(propertyName);
			Signature signature = new Signature(setterName, Type.VOID_TYPE, new Type[] {Type.getType(propertyType)});
			maker.add(signature, new Type[0]);
		}
		if (bd.getInitMethodName() != null) {
			Signature signature = new Signature(bd.getInitMethodName(), Type.VOID_TYPE, new Type[0]);
			maker.add(signature, new Type[0]);
		}
		if (StringUtils.hasText(bd.getDestroyMethodName())) {
			Signature signature = new Signature(bd.getDestroyMethodName(), Type.VOID_TYPE, new Type[0]);
			maker.add(signature, new Type[0]);
		}
		return maker.create();
	}
}
