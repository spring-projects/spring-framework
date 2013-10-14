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

package org.springframework.core;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.util.MultiValueMap;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests for {@link ResolvableType}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("rawtypes")
@RunWith(MockitoJUnitRunner.class)
public class ResolvableTypeTests {


	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Captor
	private ArgumentCaptor<TypeVariable<?>> typeVariableCaptor;


	@Test
	public void noneReturnValues() throws Exception {
		ResolvableType none = ResolvableType.NONE;
		assertThat(none.as(Object.class), equalTo(ResolvableType.NONE));
		assertThat(none.asCollection(), equalTo(ResolvableType.NONE));
		assertThat(none.asMap(), equalTo(ResolvableType.NONE));
		assertThat(none.getComponentType(), equalTo(ResolvableType.NONE));
		assertThat(none.getGeneric(0), equalTo(ResolvableType.NONE));
		assertThat(none.getGenerics().length, equalTo(0));
		assertThat(none.getInterfaces().length, equalTo(0));
		assertThat(none.getSuperType(), equalTo(ResolvableType.NONE));
		assertThat(none.getType(), nullValue());
		assertThat(none.hasGenerics(), equalTo(false));
		assertThat(none.isArray(), equalTo(false));
		assertThat(none.resolve(), nullValue());
		assertThat(none.resolve(String.class), equalTo((Class) String.class));
		assertThat(none.resolveGeneric(0), nullValue());
		assertThat(none.resolveGenerics().length, equalTo(0));
		assertThat(none.resolveVariable(mock(TypeVariable.class)), nullValue());
		assertThat(none.toString(), equalTo("?"));
		assertThat(none.isAssignableFrom(ResolvableType.forClass(Object.class)), equalTo(false));
	}

	@Test
	public void forClass() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		assertThat(type.getType(), equalTo((Type) ExtendsList.class));
	}

	@Test
	public void forClassMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Source class must not be null");
		ResolvableType.forClass(null);
	}

	@Test
	public void forField() throws Exception {
		Field field = Fields.class.getField("charSequenceList");
		ResolvableType type = ResolvableType.forField(field);
		assertThat(type.getType(), equalTo(field.getGenericType()));
	}

	@Test
	public void forPrivateField() throws Exception {
		Field field = Fields.class.getDeclaredField("privateField");
		ResolvableType type = ResolvableType.forField(field);
		assertThat(type.getType(), equalTo(field.getGenericType()));
		assertThat(type.resolve(), equalTo((Class) List.class));
	}

	@Test
	public void forFieldMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Field must not be null");
		ResolvableType.forField(null);
	}

	@Test
	public void forConstructorParameter() throws Exception {
		Constructor<Constructors> constructor = Constructors.class.getConstructor(List.class);
		ResolvableType type = ResolvableType.forConstructorParameter(constructor, 0);
		assertThat(type.getType(), equalTo(constructor.getGenericParameterTypes()[0]));
	}

	@Test
	public void forConstructorParameterMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Constructor must not be null");
		ResolvableType.forConstructorParameter(null, 0);
	}

	@Test
	public void forMethodParameterByIndex() throws Exception {
		Method method = Methods.class.getMethod("charSequenceParameter", List.class);
		ResolvableType type = ResolvableType.forMethodParameter(method, 0);
		assertThat(type.getType(), equalTo(method.getGenericParameterTypes()[0]));
	}

	@Test
	public void forMethodParameterByIndexMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Method must not be null");
		ResolvableType.forMethodParameter(null, 0);
	}

	@Test
	public void forMethodParameter() throws Exception {
		Method method = Methods.class.getMethod("charSequenceParameter", List.class);
		MethodParameter methodParameter = MethodParameter.forMethodOrConstructor(method, 0);
		ResolvableType type = ResolvableType.forMethodParameter(methodParameter);
		assertThat(type.getType(), equalTo(method.getGenericParameterTypes()[0]));
	}

	@Test
	public void forMethodParameterWithNesting() throws Exception {
		Method method = Methods.class.getMethod("nested", Map.class);
		MethodParameter methodParameter = MethodParameter.forMethodOrConstructor(method, 0);
		methodParameter.increaseNestingLevel();
		ResolvableType type = ResolvableType.forMethodParameter(methodParameter);
		assertThat(type.resolve(), equalTo((Class) Map.class));
		assertThat(type.getGeneric(0).resolve(), equalTo((Class) Byte.class));
		assertThat(type.getGeneric(1).resolve(), equalTo((Class) Long.class));
	}

	@Test
	public void forMethodParameterWithNestingAndLevels() throws Exception {
		Method method = Methods.class.getMethod("nested", Map.class);
		MethodParameter methodParameter = MethodParameter.forMethodOrConstructor(method, 0);
		methodParameter.increaseNestingLevel();
		methodParameter.setTypeIndexForCurrentLevel(0);
		ResolvableType type = ResolvableType.forMethodParameter(methodParameter);
		assertThat(type.resolve(), equalTo((Class) Map.class));
		assertThat(type.getGeneric(0).resolve(), equalTo((Class) String.class));
		assertThat(type.getGeneric(1).resolve(), equalTo((Class) Integer.class));
	}

	@Test
	public void forMethodParameterMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("MethodParameter must not be null");
		ResolvableType.forMethodParameter(null);
	}

	@Test
	public void forMethodReturn() throws Exception {
		Method method = Methods.class.getMethod("charSequenceReturn");
		ResolvableType type = ResolvableType.forMethodReturnType(method);
		assertThat(type.getType(), equalTo(method.getGenericReturnType()));
	}

	@Test
	public void forMethodReturnMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Method must not be null");
		ResolvableType.forMethodReturnType(null);
	}

	@Test
	public void classType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("classType"));
		assertThat(type.getType().getClass(), equalTo((Class) Class.class));
	}

	@Test
	public void paramaterizedType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("parameterizedType"));
		assertThat(type.getType(), instanceOf(ParameterizedType.class));
	}

	@Test
	public void arrayClassType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("arrayClassType"));
		assertThat(type.getType(), instanceOf(Class.class));
		assertThat(((Class) type.getType()).isArray(), equalTo(true));
	}

	@Test
	public void genericArrayType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("genericArrayType"));
		assertThat(type.getType(), instanceOf(GenericArrayType.class));
	}

	@Test
	public void wildcardType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("wildcardType"));
		assertThat(type.getType(), instanceOf(ParameterizedType.class));
		assertThat(type.getGeneric().getType(), instanceOf(WildcardType.class));
	}

	@Test
	public void typeVariableType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("typeVariableType"));
		assertThat(type.getType(), instanceOf(TypeVariable.class));
	}

	@Test
	public void getComponentTypeForClassArray() throws Exception {
		Field field = Fields.class.getField("arrayClassType");
		ResolvableType type = ResolvableType.forField(field);
		assertThat(type.isArray(), equalTo(true));
		assertThat(type.getComponentType().getType(),
				equalTo((Type) ((Class) field.getGenericType()).getComponentType()));
	}

	@Test
	public void getComponentTypeForGenericArrayType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("genericArrayType"));
		assertThat(type.isArray(), equalTo(true));
		assertThat(type.getComponentType().getType(),
				equalTo(((GenericArrayType) type.getType()).getGenericComponentType()));
	}

	@Test
	public void getComponentTypeForVariableThatResolvesToGenericArray() throws Exception {
		ResolvableType type = ResolvableType.forClass(ListOfGenericArray.class).asCollection().getGeneric();
		assertThat(type.isArray(), equalTo(true));
		assertThat(type.getType(), instanceOf(TypeVariable.class));
		assertThat(type.getComponentType().getType().toString(),
				equalTo("java.util.List<java.lang.String>"));
	}

	@Test
	public void getComponentTypeForNonArray() throws Exception {
		ResolvableType type = ResolvableType.forClass(String.class);
		assertThat(type.isArray(), equalTo(false));
		assertThat(type.getComponentType(), equalTo(ResolvableType.NONE));
	}

	@Test
	public void asCollection() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).asCollection();
		assertThat(type.resolve(), equalTo((Class) Collection.class));
		assertThat(type.resolveGeneric(), equalTo((Class) CharSequence.class));
	}

	@Test
	public void asMap() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsMap.class).asMap();
		assertThat(type.resolve(), equalTo((Class) Map.class));
		assertThat(type.resolveGeneric(0), equalTo((Class) String.class));
		assertThat(type.resolveGeneric(1), equalTo((Class) Integer.class));
	}

	@Test
	public void asFromInterface() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(List.class);
		assertThat(type.getType().toString(), equalTo("java.util.List<E>"));
	}

	@Test
	public void asFromInheritedInterface() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(Collection.class);
		assertThat(type.getType().toString(), equalTo("java.util.Collection<E>"));
	}

	@Test
	public void asFromSuperType() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(ArrayList.class);
		assertThat(type.getType().toString(), equalTo("java.util.ArrayList<java.lang.CharSequence>"));
	}

	@Test
	public void asFromInheritedSuperType() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(List.class);
		assertThat(type.getType().toString(), equalTo("java.util.List<E>"));
	}

	@Test
	public void asNotFound() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(Map.class);
		assertThat(type, sameInstance(ResolvableType.NONE));
	}

	@Test
	public void asSelf() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		assertThat(type.as(ExtendsList.class), equalTo(type));
	}

	@Test
	public void getSuperType() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).getSuperType();
		assertThat(type.resolve(), equalTo((Class) ArrayList.class));
		type = type.getSuperType();
		assertThat(type.resolve(), equalTo((Class) AbstractList.class));
		type = type.getSuperType();
		assertThat(type.resolve(), equalTo((Class) AbstractCollection.class));
		type = type.getSuperType();
		assertThat(type.resolve(), equalTo((Class) Object.class));
	}

	@Test
	public void getInterfaces() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		assertThat(type.getInterfaces().length, equalTo(0));
		SortedSet<String> interfaces = new TreeSet<String>();
		for (ResolvableType interfaceType : type.getSuperType().getInterfaces()) {
			interfaces.add(interfaceType.toString());
		}
		assertThat(interfaces.toString(), equalTo(
				  "["
				+ "java.io.Serializable, "
				+ "java.lang.Cloneable, "
				+ "java.util.List<java.lang.CharSequence>, "
				+ "java.util.RandomAccess"
				+ "]"));
	}

	@Test
	public void noSuperType() throws Exception {
		assertThat(ResolvableType.forClass(Object.class).getSuperType(),
				equalTo(ResolvableType.NONE));
	}

	@Test
	public void noInterfaces() throws Exception {
		assertThat(ResolvableType.forClass(Object.class).getInterfaces().length,
				equalTo(0));
	}

	@Test
	public void nested() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("nested"));
		type = type.getNested(2);
		assertThat(type.resolve(), equalTo((Class) Map.class));
		assertThat(type.getGeneric(0).resolve(), equalTo((Class) Byte.class));
		assertThat(type.getGeneric(1).resolve(), equalTo((Class) Long.class));
	}

	@Test
	public void nestedWithIndexes() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("nested"));
		type = type.getNested(2, Collections.singletonMap(2, 0));
		assertThat(type.resolve(), equalTo((Class) Map.class));
		assertThat(type.getGeneric(0).resolve(), equalTo((Class) String.class));
		assertThat(type.getGeneric(1).resolve(), equalTo((Class) Integer.class));
	}

	@Test
	public void nestedWithArray() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("genericArrayType"));
		type = type.getNested(2);
		assertThat(type.resolve(), equalTo((Class) List.class));
		assertThat(type.resolveGeneric(), equalTo((Class) String.class));
	}

	@Test
	public void getGeneric() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringList"));
		assertThat(type.getGeneric().getType(), equalTo((Type) String.class));
	}

	@Test
	public void getGenericByIndex() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringIntegerMultiValueMap"));
		assertThat(type.getGeneric(0).getType(), equalTo((Type) String.class));
		assertThat(type.getGeneric(1).getType(), equalTo((Type) Integer.class));
	}

	@Test
	public void getGenericOfGeneric() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringListList"));
		assertThat(type.getGeneric().getType().toString(), equalTo("java.util.List<java.lang.String>"));
		assertThat(type.getGeneric().getGeneric().getType(), equalTo((Type) String.class));
	}

	@Test
	public void getGenericOfGenericByIndexes() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringListList"));
		assertThat(type.getGeneric(0, 0).getType(), equalTo((Type) String.class));
	}

	@Test
	public void getGenericOutOfBounds() throws Exception {
		ResolvableType type = ResolvableType.forClass(List.class, ExtendsList.class);
		assertThat(type.getGeneric(0), not(equalTo(ResolvableType.NONE)));
		assertThat(type.getGeneric(1), equalTo(ResolvableType.NONE));
		assertThat(type.getGeneric(0, 1), equalTo(ResolvableType.NONE));
	}

	@Test
	public void hasGenerics() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		assertThat(type.hasGenerics(), equalTo(false));
		assertThat(type.asCollection().hasGenerics(), equalTo(true));
	}

	@Test
	public void getGenerics() throws Exception {
		ResolvableType type = ResolvableType.forClass(List.class, ExtendsList.class);
		ResolvableType[] generics = type.getGenerics();
		assertThat(generics.length, equalTo(1));
		assertThat(generics[0].resolve(), equalTo((Class) CharSequence.class));
	}

	@Test
	public void noGetGenerics() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		ResolvableType[] generics = type.getGenerics();
		assertThat(generics.length, equalTo(0));
	}

	@Test
	public void getResolvedGenerics() throws Exception {
		ResolvableType type = ResolvableType.forClass(List.class, ExtendsList.class);
		Class<?>[] generics = type.resolveGenerics();
		assertThat(generics.length, equalTo(1));
		assertThat(generics[0], equalTo((Class) CharSequence.class));
	}

	@Test
	public void resolveClassType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("classType"));
		assertThat(type.resolve(), equalTo((Class) List.class));
	}

	@Test
	public void resolveParameterizedType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("parameterizedType"));
		assertThat(type.resolve(), equalTo((Class) List.class));
	}

	@Test
	public void resolveArrayClassType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("arrayClassType"));
		assertThat(type.resolve(), equalTo((Class) List[].class));
	}

	@Test
	public void resolveGenericArrayType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("genericArrayType"));
		assertThat(type.resolve(), equalTo((Class) List[].class));
		assertThat(type.getComponentType().resolve(), equalTo((Class) List.class));
		assertThat(type.getComponentType().getGeneric().resolve(), equalTo((Class) String.class));
	}

	@Test
	public void resolveGenericMultiArrayType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("genericMultiArrayType"));
		assertThat(type.resolve(), equalTo((Class) List[][][].class));
		assertThat(type.getComponentType().resolve(), equalTo((Class) List[][].class));
	}

	@Test
	public void resolveGenericArrayFromGeneric() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringArrayList"));
		ResolvableType generic = type.asCollection().getGeneric();
		assertThat(generic.getType().toString(), equalTo("E"));
		assertThat(generic.isArray(), equalTo(true));
		assertThat(generic.resolve(), equalTo((Class) String[].class));
	}

	@Test
	public void resolveWildcardTypeUpperBounds() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("wildcardType"));
		assertThat(type.getGeneric().resolve(), equalTo((Class) Number.class));
	}

	@Test
	public void resolveWildcardLowerBounds() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("wildcardSuperType"));
		assertThat(type.getGeneric().resolve(), equalTo((Class) Number.class));
	}

	@Test
	public void resolveVariableFromFieldType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringList"));
		assertThat(type.resolve(), equalTo((Class) List.class));
		assertThat(type.getGeneric().resolve(), equalTo((Class) String.class));
	}

	@Test
	public void resolveVariableFromFieldTypeUnknown() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("parameterizedType"));
		assertThat(type.resolve(), equalTo((Class) List.class));
		assertThat(type.getGeneric().resolve(), nullValue());
	}

	@Test
	public void resolveVariableFromInheritedField() throws Exception {
		ResolvableType type = ResolvableType.forField(
				Fields.class.getField("stringIntegerMultiValueMap")).as(Map.class);
		assertThat(type.getGeneric(0).resolve(), equalTo((Class) String.class));
		assertThat(type.getGeneric(1).resolve(), equalTo((Class) List.class));
		assertThat(type.getGeneric(1, 0).resolve(), equalTo((Class) Integer.class));
	}

	@Test
	public void resolveVariableFromInheritedFieldSwitched() throws Exception {
		ResolvableType type = ResolvableType.forField(
				Fields.class.getField("stringIntegerMultiValueMapSwitched")).as(Map.class);
		assertThat(type.getGeneric(0).resolve(), equalTo((Class) String.class));
		assertThat(type.getGeneric(1).resolve(), equalTo((Class) List.class));
		assertThat(type.getGeneric(1, 0).resolve(), equalTo((Class) Integer.class));
	}

	@Test
	public void doesResolveFromOuterOwner() throws Exception {
		ResolvableType type = ResolvableType.forField(
				Fields.class.getField("listOfListOfUnknown")).as(Collection.class);
		ResolvableType generic = type.getGeneric(0);
		assertThat(generic.resolve(), equalTo((Class) List.class));
		assertThat(generic.as(Collection.class).getGeneric(0).as(Collection.class).resolve(), nullValue());
	}

	@Test
	public void resolveBoundedTypeVariableResult() throws Exception {
		ResolvableType type = ResolvableType.forMethodReturnType(Methods.class.getMethod("boundedTypeVaraibleResult"));
		assertThat(type.resolve(), equalTo((Class) CharSequence.class));
	}

	@Test
	public void resolveVariableNotFound() throws Exception {
		ResolvableType type = ResolvableType.forMethodReturnType(Methods.class.getMethod("typedReturn"));
		assertThat(type.resolve(), nullValue());
	}

	@Test
	public void resolveTypeVaraibleFromMethodReturn() throws Exception {
		ResolvableType type = ResolvableType.forMethodReturnType(Methods.class.getMethod("typedReturn"));
		assertThat(type.resolve(), nullValue());
	}

	@Test
	public void resolveTypeVaraibleFromMethodReturnWithInstanceClass() throws Exception {
		ResolvableType type = ResolvableType.forMethodReturnType(
				Methods.class.getMethod("typedReturn"), TypedMethods.class);
		assertThat(type.resolve(), equalTo((Class) String.class));
	}

	@Test
	public void resolveTypeVaraibleFromSimpleInterfaceType() {
		ResolvableType type = ResolvableType.forClass(
				MySimpleInterfaceType.class).as(MyInterfaceType.class);
		assertThat(type.resolveGeneric(), equalTo((Class) String.class));
	}

	@Test
	public void resolveTypeVaraibleFromSimpleCollectionInterfaceType() {
		ResolvableType type = ResolvableType.forClass(
				MyCollectionInterfaceType.class).as(MyInterfaceType.class);
		assertThat(type.resolveGeneric(), equalTo((Class) Collection.class));
		assertThat(type.resolveGeneric(0, 0), equalTo((Class) String.class));
	}

	@Test
	public void resolveTypeVaraibleFromSimpleSuperclassType() {
		ResolvableType type = ResolvableType.forClass(
				MySimpleSuperclassType.class).as(MySuperclassType.class);
		assertThat(type.resolveGeneric(), equalTo((Class) String.class));
	}

	@Test
	public void resolveTypeVaraibleFromSimpleCollectionSuperclassType() {
		ResolvableType type = ResolvableType.forClass(
				MyCollectionSuperclassType.class).as(MySuperclassType.class);
		assertThat(type.resolveGeneric(), equalTo((Class) Collection.class));
		assertThat(type.resolveGeneric(0, 0), equalTo((Class) String.class));
	}

	@Test
	public void resolveTypeVariableFromFieldTypeWithImplementsClass() throws Exception {
		ResolvableType type = ResolvableType.forField(
				Fields.class.getField("parameterizedType"), TypedFields.class);
		assertThat(type.resolve(), equalTo((Class) List.class));
		assertThat(type.getGeneric().resolve(), equalTo((Class) String.class));
	}

	@Test
	public void resolveTypeVariableFromSuperType() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		assertThat(type.resolve(), equalTo((Class) ExtendsList.class));
		assertThat(type.asCollection().resolveGeneric(),
				equalTo((Class) CharSequence.class));
	}

	@Test
	public void resolveTypeVariableFromClassWithImplementsClass() throws Exception {
		ResolvableType type = ResolvableType.forClass(
				MySuperclassType.class, MyCollectionSuperclassType.class);
		assertThat(type.resolveGeneric(), equalTo((Class) Collection.class));
		assertThat(type.resolveGeneric(0, 0), equalTo((Class) String.class));
	}

	@Test
	public void resolveTypeVariableFromConstructorParameter() throws Exception {
		Constructor<?> constructor = Constructors.class.getConstructor(List.class);
		ResolvableType type = ResolvableType.forConstructorParameter(constructor, 0);
		assertThat(type.resolve(), equalTo((Class) List.class));
		assertThat(type.resolveGeneric(0), equalTo((Class) CharSequence.class));
	}

	@Test
	public void resolveUnknownTypeVariableFromConstructorParameter() throws Exception {
		Constructor<?> constructor = Constructors.class.getConstructor(Map.class);
		ResolvableType type = ResolvableType.forConstructorParameter(constructor, 0);
		assertThat(type.resolve(), equalTo((Class) Map.class));
		assertThat(type.resolveGeneric(0), nullValue());
	}

	@Test
	public void resolveTypeVariableFromConstructorParameterWithImplementsClass() throws Exception {
		Constructor<?> constructor = Constructors.class.getConstructor(Map.class);
		ResolvableType type = ResolvableType.forConstructorParameter(
				constructor, 0, TypedConstructors.class);
		assertThat(type.resolve(), equalTo((Class) Map.class));
		assertThat(type.resolveGeneric(0), equalTo((Class) String.class));
	}

	@Test
	public void resolveTypeVariableFromMethodParameter() throws Exception {
		Method method = Methods.class.getMethod("typedParameter", Object.class);
		ResolvableType type = ResolvableType.forMethodParameter(method, 0);
		assertThat(type.resolve(), nullValue());
		assertThat(type.getType().toString(), equalTo("T"));
	}

	@Test
	public void resolveTypeVariableFromMethodParameterWithImplementsClass() throws Exception {
		Method method = Methods.class.getMethod("typedParameter", Object.class);
		ResolvableType type = ResolvableType.forMethodParameter(method, 0, TypedMethods.class);
		assertThat(type.resolve(), equalTo((Class) String.class));
		assertThat(type.getType().toString(), equalTo("T"));
	}

	@Test
	public void resolveTypeVariableFromMethodParameterType() throws Exception {
		Method method = Methods.class.getMethod("typedParameter", Object.class);
		MethodParameter methodParameter = MethodParameter.forMethodOrConstructor(method, 0);
		ResolvableType type = ResolvableType.forMethodParameter(methodParameter);
		assertThat(type.resolve(), nullValue());
		assertThat(type.getType().toString(), equalTo("T"));
	}

	@Test
	public void resolveTypeVariableFromMethodParameterTypeWithImplementsClass() throws Exception {
		Method method = Methods.class.getMethod("typedParameter", Object.class);
		MethodParameter methodParameter = MethodParameter.forMethodOrConstructor(method, 0);
		methodParameter.setContainingClass(TypedMethods.class);
		ResolvableType type = ResolvableType.forMethodParameter(methodParameter);
		assertThat(type.resolve(), equalTo((Class) String.class));
		assertThat(type.getType().toString(), equalTo("T"));
	}

	@Test
	public void resolveTypeVariableFromMethodReturn() throws Exception {
		Method method = Methods.class.getMethod("typedReturn");
		ResolvableType type = ResolvableType.forMethodReturnType(method);
		assertThat(type.resolve(), nullValue());
		assertThat(type.getType().toString(), equalTo("T"));
	}

	@Test
	public void resolveTypeVariableFromMethodReturnWithImplementsClass() throws Exception {
		Method method = Methods.class.getMethod("typedReturn");
		ResolvableType type = ResolvableType.forMethodReturnType(method, TypedMethods.class);
		assertThat(type.resolve(), equalTo((Class) String.class));
		assertThat(type.getType().toString(), equalTo("T"));
	}

	@Test
	public void resolveTypeVariableFromType() throws Exception {
		Type sourceType = Methods.class.getMethod("typedReturn").getGenericReturnType();
		ResolvableType type = ResolvableType.forType(sourceType);
		assertThat(type.resolve(), nullValue());
		assertThat(type.getType().toString(), equalTo("T"));
	}

	@Test
	public void resolveTypeVariableFromTypeWithVariableResolver() throws Exception {
		Type sourceType = Methods.class.getMethod("typedReturn").getGenericReturnType();
		ResolvableType type = ResolvableType.forType(
				sourceType, ResolvableType.forClass(TypedMethods.class).as(Methods.class));
		assertThat(type.resolve(), equalTo((Class) String.class));
		assertThat(type.getType().toString(), equalTo("T"));
	}

	@Test
	public void resolveTypeWithCustomVariableResolver() throws Exception {
		TypeVariableResolver variableResolver = mock(TypeVariableResolver.class);
		given(variableResolver.resolveVariable((TypeVariable<?>) anyObject())).willReturn(Long.class);

		ResolvableType variable = ResolvableType.forType(
				Fields.class.getField("typeVariableType").getGenericType(), variableResolver);
		ResolvableType parameterized = ResolvableType.forType(
				Fields.class.getField("parameterizedType").getGenericType(), variableResolver);

		assertThat(variable.resolve(), equalTo((Class) Long.class));
		assertThat(parameterized.resolve(), equalTo((Class) List.class));
		assertThat(parameterized.resolveGeneric(), equalTo((Class) Long.class));
		verify(variableResolver, atLeastOnce()).resolveVariable(this.typeVariableCaptor.capture());
		assertThat(this.typeVariableCaptor.getValue().getName(), equalTo("T"));
	}

	@Test
	public void toStrings() throws Exception {
		assertThat(ResolvableType.NONE.toString(), equalTo("?"));

		assertFieldToStringValue("classType", "java.util.List");
		assertFieldToStringValue("typeVariableType", "?");
		assertFieldToStringValue("parameterizedType", "java.util.List<?>");
		assertFieldToStringValue("arrayClassType", "java.util.List[]");
		assertFieldToStringValue("genericArrayType", "java.util.List<java.lang.String>[]");
		assertFieldToStringValue("genericMultiArrayType", "java.util.List<java.lang.String>[][][]");
		assertFieldToStringValue("wildcardType", "java.util.List<java.lang.Number>");
		assertFieldToStringValue("wildcardSuperType", "java.util.List<java.lang.Number>");
		assertFieldToStringValue("charSequenceList", "java.util.List<java.lang.CharSequence>");
		assertFieldToStringValue("stringList", "java.util.List<java.lang.String>");
		assertFieldToStringValue("stringListList", "java.util.List<java.util.List<java.lang.String>>");
		assertFieldToStringValue("stringArrayList", "java.util.List<java.lang.String[]>");
		assertFieldToStringValue("stringIntegerMultiValueMap", "org.springframework.util.MultiValueMap<java.lang.String, java.lang.Integer>");
		assertFieldToStringValue("stringIntegerMultiValueMapSwitched", VariableNameSwitch.class.getName() + "<java.lang.Integer, java.lang.String>");
		assertFieldToStringValue("listOfListOfUnknown", "java.util.List<java.util.List>");

		assertTypedFieldToStringValue("typeVariableType", "java.lang.String");
		assertTypedFieldToStringValue("parameterizedType", "java.util.List<java.lang.String>");

		assertThat(ResolvableType.forClass(ListOfGenericArray.class).toString(), equalTo(ListOfGenericArray.class.getName()));
		assertThat(ResolvableType.forClass(List.class, ListOfGenericArray.class).toString(), equalTo("java.util.List<java.util.List<java.lang.String>[]>"));
	}

	private void assertFieldToStringValue(String field, String expected) throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField(field));
		assertThat("field " + field + " toString", type.toString(), equalTo(expected));
	}

	private void assertTypedFieldToStringValue(String field, String expected)
			throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField(field), TypedFields.class);
		assertThat("field " + field + " toString", type.toString(), equalTo(expected));
	}

	@Test
	public void resolveFromOuterClass() throws Exception {
		Field field = EnclosedInParameterizedType.InnerTyped.class.getField("field");
		ResolvableType type = ResolvableType.forField(
				field, TypedEnclosedInParameterizedType.TypedInnerTyped.class);
		assertThat(type.resolve(), equalTo((Type) Integer.class));
	}

	@Test
	public void isAssignableFromMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Type must not be null");
		ResolvableType.forClass(Object.class).isAssignableFrom(null);
	}

	@Test
	public void isAssignableFromForNone() throws Exception {
		ResolvableType objectType = ResolvableType.forClass(Object.class);
		assertThat(objectType.isAssignableFrom(ResolvableType.NONE), equalTo(false));
		assertThat(ResolvableType.NONE.isAssignableFrom(objectType), equalTo(false));
	}

	@Test
	public void isAssignableFromForClassAndClass() throws Exception {
		ResolvableType objectType = ResolvableType.forClass(Object.class);
		ResolvableType charSequenceType = ResolvableType.forClass(CharSequence.class);
		ResolvableType stringType = ResolvableType.forClass(String.class);

		assertAssignable(objectType, objectType, charSequenceType, stringType).equalTo(true, true, true);
		assertAssignable(charSequenceType, objectType, charSequenceType, stringType).equalTo(false, true, true);
		assertAssignable(stringType, objectType, charSequenceType, stringType).equalTo(false, false, true);
	}

	@Test
	public void isAssignableFromCannotBeResolved() throws Exception {
		ResolvableType objectType = ResolvableType.forClass(Object.class);
		ResolvableType unresolvableVariable = ResolvableType.forField(AssignmentBase.class.getField("o"));
		assertThat(unresolvableVariable.resolve(), nullValue());
		assertAssignable(objectType, unresolvableVariable).equalTo(true);
		assertAssignable(unresolvableVariable, objectType).equalTo(true);
	}

	@Test
	public void isAssignableFromForClassAndSimpleVariable() throws Exception {
		ResolvableType objectType = ResolvableType.forClass(Object.class);
		ResolvableType charSequenceType = ResolvableType.forClass(CharSequence.class);
		ResolvableType stringType = ResolvableType.forClass(String.class);

		ResolvableType objectVariable = ResolvableType.forField(AssignmentBase.class.getField("o"), Assignment.class);
		ResolvableType charSequenceVariable = ResolvableType.forField(AssignmentBase.class.getField("c"), Assignment.class);
		ResolvableType stringVariable = ResolvableType.forField(AssignmentBase.class.getField("s"), Assignment.class);

		assertAssignable(objectType, objectVariable, charSequenceVariable, stringVariable).equalTo(true, true, true);
		assertAssignable(charSequenceType, objectVariable, charSequenceVariable, stringVariable).equalTo(false, true, true);
		assertAssignable(stringType, objectVariable, charSequenceVariable, stringVariable).equalTo(false, false, true);

		assertAssignable(objectVariable, objectType, charSequenceType, stringType).equalTo(true, true, true);
		assertAssignable(charSequenceVariable, objectType, charSequenceType, stringType).equalTo(false, true, true);
		assertAssignable(stringVariable, objectType, charSequenceType, stringType).equalTo(false, false, true);

		assertAssignable(objectVariable, objectVariable, charSequenceVariable, stringVariable).equalTo(true, true, true);
		assertAssignable(charSequenceVariable, objectVariable, charSequenceVariable, stringVariable).equalTo(false, true, true);
		assertAssignable(stringVariable, objectVariable, charSequenceVariable, stringVariable).equalTo(false, false, true);
	}

	@Test
	public void isAssignableFromForSameClassNonExtendsGenerics() throws Exception {
		ResolvableType objectList = ResolvableType.forField(AssignmentBase.class.getField("listo"), Assignment.class);
		ResolvableType stringList = ResolvableType.forField(AssignmentBase.class.getField("lists"), Assignment.class);

		assertAssignable(stringList, objectList).equalTo(false);
		assertAssignable(objectList, stringList).equalTo(false);
		assertAssignable(stringList, stringList).equalTo(true);
	}

	@Test
	public void isAssignableFromForSameClassExtendsGenerics() throws Exception {

		// Generic assignment can be a little confusing, given:
		//
		// List<CharSequence> c1, List<? extends CharSequence> c2, List<String> s;
		//
		// c2 = s; is allowed and is often used for argument input, for example
		// see List.addAll(). You can get items from c2 but you cannot add items without
		// getting a generic type 'is not applicable for the arguments' error. This makes
		// sense since if you added a StringBuffer to c2 it would break the rules on s.
		//
		// c1 = s; not allowed. Since there is no '? extends' to cause the generic
		// 'is not applicable for the arguments' error when adding (which would pollute
		// s).

		ResolvableType objectList = ResolvableType.forField(AssignmentBase.class.getField("listo"), Assignment.class);
		ResolvableType charSequenceList = ResolvableType.forField(AssignmentBase.class.getField("listc"), Assignment.class);
		ResolvableType stringList = ResolvableType.forField(AssignmentBase.class.getField("lists"), Assignment.class);
		ResolvableType extendsObjectList = ResolvableType.forField(AssignmentBase.class.getField("listxo"), Assignment.class);
		ResolvableType extendsCharSequenceList = ResolvableType.forField(AssignmentBase.class.getField("listxc"), Assignment.class);
		ResolvableType extendsStringList = ResolvableType.forField(AssignmentBase.class.getField("listxs"), Assignment.class);

		assertAssignable(objectList, extendsObjectList, extendsCharSequenceList, extendsStringList).equalTo(false, false, false);
		assertAssignable(charSequenceList, extendsObjectList, extendsCharSequenceList, extendsStringList).equalTo(false, false, false);
		assertAssignable(stringList, extendsObjectList, extendsCharSequenceList, extendsStringList).equalTo(false, false, false);
		assertAssignable(extendsObjectList, objectList, charSequenceList, stringList).equalTo(true, true, true);
		assertAssignable(extendsObjectList, extendsObjectList, extendsCharSequenceList, extendsStringList).equalTo(true, true, true);
		assertAssignable(extendsCharSequenceList, extendsObjectList, extendsCharSequenceList, extendsStringList).equalTo(false, true, true);
		assertAssignable(extendsCharSequenceList, objectList, charSequenceList, stringList).equalTo(false, true, true);
		assertAssignable(extendsStringList, extendsObjectList, extendsCharSequenceList, extendsStringList).equalTo(false, false, true);
		assertAssignable(extendsStringList, objectList, charSequenceList, stringList).equalTo(false, false, true);
	}

	@Test
	public void isAssignableFromForDifferentClassesWithGenerics() throws Exception {
		ResolvableType extendsCharSequenceCollection = ResolvableType.forField(AssignmentBase.class.getField("collectionxc"), Assignment.class);
		ResolvableType charSequenceCollection = ResolvableType.forField(AssignmentBase.class.getField("collectionc"), Assignment.class);
		ResolvableType charSequenceList = ResolvableType.forField(AssignmentBase.class.getField("listc"), Assignment.class);
		ResolvableType extendsCharSequenceList = ResolvableType.forField(AssignmentBase.class.getField("listxc"), Assignment.class);
		ResolvableType extendsStringList = ResolvableType.forField(AssignmentBase.class.getField("listxs"), Assignment.class);

		assertAssignable(extendsCharSequenceCollection, charSequenceCollection, charSequenceList, extendsCharSequenceList, extendsStringList)
				.equalTo(true, true, true, true);
		assertAssignable(charSequenceCollection, charSequenceList, extendsCharSequenceList, extendsStringList)
				.equalTo(true, false, false);
		assertAssignable(charSequenceList, extendsCharSequenceCollection, charSequenceCollection)
				.equalTo(false, false);
		assertAssignable(extendsCharSequenceList, extendsCharSequenceCollection, charSequenceCollection)
				.equalTo(false, false);
		assertAssignable(extendsStringList, charSequenceCollection, charSequenceList, extendsCharSequenceList)
				.equalTo(false, false, false);
	}

	@Test
	public void isAssignableFromForArrays() throws Exception {
		ResolvableType object = ResolvableType.forField(AssignmentBase.class.getField("o"), Assignment.class);
		ResolvableType objectArray = ResolvableType.forField(AssignmentBase.class.getField("oarray"), Assignment.class);
		ResolvableType charSequenceArray = ResolvableType.forField(AssignmentBase.class.getField("carray"), Assignment.class);
		ResolvableType stringArray = ResolvableType.forField(AssignmentBase.class.getField("sarray"), Assignment.class);

		assertAssignable(object, objectArray, charSequenceArray, stringArray).
				equalTo(true, true, true);
		assertAssignable(objectArray, object, objectArray, charSequenceArray, stringArray).
				equalTo(false, true, true, true);
		assertAssignable(charSequenceArray, object, objectArray, charSequenceArray, stringArray).
				equalTo(false, false, true, true);
		assertAssignable(stringArray, object, objectArray, charSequenceArray, stringArray).
				equalTo(false, false, false, true);
	}

	@Test
	public void isAssignableFromForWildcards() throws Exception {

		ResolvableType object = ResolvableType.forClass(Object.class);
		ResolvableType charSequence = ResolvableType.forClass(CharSequence.class);
		ResolvableType string = ResolvableType.forClass(String.class);
		ResolvableType extendsAnon = ResolvableType.forField(AssignmentBase.class.getField("listAnon"), Assignment.class).getGeneric();
		ResolvableType extendsObject = ResolvableType.forField(AssignmentBase.class.getField("listxo"), Assignment.class).getGeneric();
		ResolvableType extendsCharSequence = ResolvableType.forField(AssignmentBase.class.getField("listxc"), Assignment.class).getGeneric();
		ResolvableType extendsString = ResolvableType.forField(AssignmentBase.class.getField("listxs"), Assignment.class).getGeneric();
		ResolvableType superObject = ResolvableType.forField(AssignmentBase.class.getField("listso"), Assignment.class).getGeneric();
		ResolvableType superCharSequence = ResolvableType.forField(AssignmentBase.class.getField("listsc"), Assignment.class).getGeneric();
		ResolvableType superString = ResolvableType.forField(AssignmentBase.class.getField("listss"), Assignment.class).getGeneric();

		// Language Spec 4.5.1. Type Arguments and Wildcards

		// ? extends T <= ? extends S if T <: S
		assertAssignable(extendsCharSequence, extendsObject, extendsCharSequence, extendsString).
				equalTo(false, true, true);
		assertAssignable(extendsCharSequence, object, charSequence, string).
				equalTo(false, true, true);

		// ? super T <= ? super S if S <: T
		assertAssignable(superCharSequence, superObject, superCharSequence, superString).
				equalTo(true, true, false);
		assertAssignable(superCharSequence, object, charSequence, string).
				equalTo(true, true, false);

		// [Implied] super / extends cannot be mixed
		assertAssignable(superCharSequence, extendsObject, extendsCharSequence, extendsString).
				equalTo(false, false, false);
		assertAssignable(extendsCharSequence, superObject, superCharSequence, superString).
				equalTo(false, false, false);

		// T <= T
		assertAssignable(charSequence, object, charSequence, string).
				equalTo(false, true, true);

		// T <= ? extends T
		assertAssignable(extendsCharSequence, object, charSequence, string).
				equalTo(false, true, true);
		assertAssignable(charSequence, extendsObject, extendsCharSequence, extendsString).
				equalTo(false, false, false);
		assertAssignable(extendsAnon, object, charSequence, string).
				equalTo(true, true, true);

		// T <= ? super T
		assertAssignable(superCharSequence, object, charSequence, string).
				equalTo(true, true, false);
		assertAssignable(charSequence, superObject, superCharSequence, superString).
				equalTo(false, false, false);
	}

	@Test
	public void isAssignableFromForComplexWildcards() throws Exception {
		ResolvableType complex1 = ResolvableType.forField(AssignmentBase.class.getField("complexWildcard1"));
		ResolvableType complex2 = ResolvableType.forField(AssignmentBase.class.getField("complexWildcard2"));
		ResolvableType complex3 = ResolvableType.forField(AssignmentBase.class.getField("complexWildcard3"));
		ResolvableType complex4 = ResolvableType.forField(AssignmentBase.class.getField("complexWildcard4"));

		assertAssignable(complex1, complex2).equalTo(true);
		assertAssignable(complex2, complex1).equalTo(false);
		assertAssignable(complex3, complex4).equalTo(true);
		assertAssignable(complex4, complex3).equalTo(false);
	}

	@Test
	public void hashCodeAndEquals() throws Exception {
		ResolvableType forClass = ResolvableType.forClass(List.class);
		ResolvableType forFieldDirect = ResolvableType.forField(Fields.class.getDeclaredField("stringList"));
		ResolvableType forFieldViaType = ResolvableType.forType(Fields.class.getDeclaredField("stringList").getGenericType());
		ResolvableType forFieldWithImplementation = ResolvableType.forField(Fields.class.getDeclaredField("stringList"), TypedFields.class);

		assertThat(forClass, equalTo(forClass));
		assertThat(forClass.hashCode(), equalTo(forClass.hashCode()));
		assertThat(forClass, not(equalTo(forFieldDirect)));
		assertThat(forClass, not(equalTo(forFieldWithImplementation)));

		assertThat(forFieldDirect, equalTo(forFieldDirect));
		assertThat(forFieldDirect, equalTo(forFieldViaType));
		assertThat(forFieldDirect, not(equalTo(forFieldWithImplementation)));
	}

	@SuppressWarnings("unused")
	private HashMap<Integer, List<String>> myMap;

	@Test
	public void javaDocSample() throws Exception {
		ResolvableType t = ResolvableType.forField(getClass().getDeclaredField("myMap"));
		assertThat(t.getSuperType().toString(), equalTo("java.util.AbstractMap<java.lang.Integer, java.util.List<java.lang.String>>"));
		assertThat(t.asMap().toString(), equalTo("java.util.Map<java.lang.Integer, java.util.List<java.lang.String>>"));
		assertThat(t.getGeneric(0).resolve(), equalTo((Class)Integer.class));
		assertThat(t.getGeneric(1).resolve(), equalTo((Class)List.class));
		assertThat(t.getGeneric(1).toString(), equalTo("java.util.List<java.lang.String>"));
		assertThat(t.resolveGeneric(1, 0), equalTo((Class) String.class));
	}


	private static AssertAssignbleMatcher assertAssignable(final ResolvableType type,
			final ResolvableType... fromTypes) {
		return new AssertAssignbleMatcher() {
			@Override
			public void equalTo(boolean... values) {
				for (int i = 0; i < fromTypes.length; i++) {
					assertThat(stringDesc(type) + " isAssignableFrom "
							+ stringDesc(fromTypes[i]),
							type.isAssignableFrom(fromTypes[i]),
							Matchers.equalTo(values[i]));
				}
			}
		};
	}

	private static String stringDesc(ResolvableType type) {
		if (type == ResolvableType.NONE) {
			return "NONE";
		}
		if (type.getType().getClass().equals(Class.class)) {
			return type.toString();
		}
		return type.getType() + ":" + type;
	}


	private static interface AssertAssignbleMatcher {

		void equalTo(boolean... values);

	}


	static class ExtendsList extends ArrayList<CharSequence> {

	}


	static class ExtendsMap extends HashMap<String, Integer> {

	}


	static class Fields<T> {

		public List classType;

		public T typeVariableType;

		public List<T> parameterizedType;

		public List[] arrayClassType;

		public List<String>[] genericArrayType;

		public List<String>[][][] genericMultiArrayType;

		public List<? extends Number> wildcardType;

		public List<? super Number> wildcardSuperType = new ArrayList<Object>();

		public List<CharSequence> charSequenceList;

		public List<String> stringList;

		public List<List<String>> stringListList;

		public List<String[]> stringArrayList;

		public MultiValueMap<String, Integer> stringIntegerMultiValueMap;

		public VariableNameSwitch<Integer, String> stringIntegerMultiValueMapSwitched;

		public List<List> listOfListOfUnknown;

		@SuppressWarnings("unused")
		private List<String> privateField;

		public Map<Map<String, Integer>, Map<Byte, Long>> nested;

	}


	static class TypedFields extends Fields<String> {

	}


	static interface Methods<T> {

		List<CharSequence> charSequenceReturn();

		void charSequenceParameter(List<CharSequence> cs);

		<R extends CharSequence & Serializable> R boundedTypeVaraibleResult();

		void nested(Map<Map<String, Integer>, Map<Byte, Long>> p);

		void typedParameter(T p);

		T typedReturn();

	}


	static class AssignmentBase<O, C, S> {

		public O o;

		public C c;

		public S s;

		public List<O> listo;

		public List<C> listc;

		public List<S> lists;

		public List<?> listAnon;

		public List<? extends O> listxo;

		public List<? extends C> listxc;

		public List<? extends S> listxs;

		public List<? super O> listso;

		public List<? super C> listsc;

		public List<? super S> listss;

		public O[] oarray;

		public C[] carray;

		public S[] sarray;

		public Collection<C> collectionc;

		public Collection<? extends C> collectionxc;

		public Map<? super Integer, List<String>> complexWildcard1;

		public MultiValueMap<Number, String> complexWildcard2;

		public Collection<? extends Collection<? extends CharSequence>> complexWildcard3;

		public List<List<String>> complexWildcard4;

	}


	static class Assignment extends AssignmentBase<Object, CharSequence, String> {

	}


	static interface TypedMethods extends Methods<String> {

	}


	static class Constructors<T> {

		public Constructors(List<CharSequence> p) {
		}

		public Constructors(Map<T, Long> p) {
		}

	}


	static class TypedConstructors extends Constructors<String> {

		public TypedConstructors(List<CharSequence> p) {
			super(p);
		}

		public TypedConstructors(Map<String, Long> p) {
			super(p);
		}

	}


	public interface MyInterfaceType<T> {

	}


	public class MySimpleInterfaceType implements MyInterfaceType<String> {

	}


	public class MyCollectionInterfaceType implements MyInterfaceType<Collection<String>> {

	}


	public abstract class MySuperclassType<T> {

	}


	public class MySimpleSuperclassType extends MySuperclassType<String> {

	}


	public class MyCollectionSuperclassType extends MySuperclassType<Collection<String>> {

	}


	static interface Wildcard<T extends Number> extends List<T> {

	}


	static interface RawExtendsWildcard extends Wildcard {

	}


	static interface VariableNameSwitch<V, K> extends MultiValueMap<K, V> {

	}


	static interface ListOfGenericArray extends List<List<String>[]> {

	}


	static class EnclosedInParameterizedType<T> {

		static class InnerRaw {
		}

		class InnerTyped<Y> {

			public T field;
		}

	}


	static class TypedEnclosedInParameterizedType extends
			EnclosedInParameterizedType<Integer> {

		class TypedInnerTyped extends InnerTyped<Long> {
		}

	}

}
