/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core.convert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link TypeDescriptor}.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Nathan Piper
 */
@SuppressWarnings("rawtypes")
class TypeDescriptorTests {

	@Test
	void parameterPrimitive() throws Exception {
		TypeDescriptor desc = new TypeDescriptor(new MethodParameter(getClass().getMethod("testParameterPrimitive", int.class), 0));
		assertThat(desc.getType()).isEqualTo(int.class);
		assertThat(desc.getObjectType()).isEqualTo(Integer.class);
		assertThat(desc.getName()).isEqualTo("int");
		assertThat(desc.toString()).isEqualTo("int");
		assertThat(desc.isPrimitive()).isTrue();
		assertThat(desc.getAnnotations()).isEmpty();
		assertThat(desc.isCollection()).isFalse();
		assertThat(desc.isMap()).isFalse();
	}

	@Test
	void parameterScalar() throws Exception {
		TypeDescriptor desc = new TypeDescriptor(new MethodParameter(getClass().getMethod("testParameterScalar", String.class), 0));
		assertThat(desc.getType()).isEqualTo(String.class);
		assertThat(desc.getObjectType()).isEqualTo(String.class);
		assertThat(desc.getName()).isEqualTo("java.lang.String");
		assertThat(desc.toString()).isEqualTo("java.lang.String");
		assertThat(desc.isPrimitive()).isFalse();
		assertThat(desc.getAnnotations()).isEmpty();
		assertThat(desc.isCollection()).isFalse();
		assertThat(desc.isArray()).isFalse();
		assertThat(desc.isMap()).isFalse();
	}

	@Test
	void parameterList() throws Exception {
		MethodParameter methodParameter = new MethodParameter(getClass().getMethod("testParameterList", List.class), 0);
		TypeDescriptor desc = new TypeDescriptor(methodParameter);
		assertThat(desc.getType()).isEqualTo(List.class);
		assertThat(desc.getObjectType()).isEqualTo(List.class);
		assertThat(desc.getName()).isEqualTo("java.util.List");
		assertThat(desc.toString()).isEqualTo("java.util.List<java.util.List<java.util.Map<java.lang.Integer, java.lang.Enum<?>>>>");
		assertThat(desc.isPrimitive()).isFalse();
		assertThat(desc.getAnnotations()).isEmpty();
		assertThat(desc.isCollection()).isTrue();
		assertThat(desc.isArray()).isFalse();
		assertThat(desc.getElementTypeDescriptor().getType()).isEqualTo(List.class);
		assertThat(desc.getElementTypeDescriptor()).isEqualTo(TypeDescriptor.nested(methodParameter, 1));
		assertThat(desc.getElementTypeDescriptor().getElementTypeDescriptor()).isEqualTo(TypeDescriptor.nested(methodParameter, 2));
		assertThat(desc.getElementTypeDescriptor().getElementTypeDescriptor().getMapValueTypeDescriptor()).isEqualTo(TypeDescriptor.nested(methodParameter, 3));
		assertThat(desc.getElementTypeDescriptor().getElementTypeDescriptor().getMapKeyTypeDescriptor().getType()).isEqualTo(Integer.class);
		assertThat(desc.getElementTypeDescriptor().getElementTypeDescriptor().getMapValueTypeDescriptor().getType()).isEqualTo(Enum.class);
		assertThat(desc.isMap()).isFalse();
	}

	@Test
	void parameterListNoParamTypes() throws Exception {
		MethodParameter methodParameter = new MethodParameter(getClass().getMethod("testParameterListNoParamTypes", List.class), 0);
		TypeDescriptor desc = new TypeDescriptor(methodParameter);
		assertThat(desc.getType()).isEqualTo(List.class);
		assertThat(desc.getObjectType()).isEqualTo(List.class);
		assertThat(desc.getName()).isEqualTo("java.util.List");
		assertThat(desc.toString()).isEqualTo("java.util.List<?>");
		assertThat(desc.isPrimitive()).isFalse();
		assertThat(desc.getAnnotations()).isEmpty();
		assertThat(desc.isCollection()).isTrue();
		assertThat(desc.isArray()).isFalse();
		assertThat((Object) desc.getElementTypeDescriptor()).isNull();
		assertThat(desc.isMap()).isFalse();
	}

	@Test
	void parameterArray() throws Exception {
		MethodParameter methodParameter = new MethodParameter(getClass().getMethod("testParameterArray", Integer[].class), 0);
		TypeDescriptor desc = new TypeDescriptor(methodParameter);
		assertThat(desc.getType()).isEqualTo(Integer[].class);
		assertThat(desc.getObjectType()).isEqualTo(Integer[].class);
		assertThat(desc.getName()).isEqualTo("java.lang.Integer[]");
		assertThat(desc.toString()).isEqualTo("java.lang.Integer[]");
		assertThat(desc.isPrimitive()).isFalse();
		assertThat(desc.getAnnotations()).isEmpty();
		assertThat(desc.isCollection()).isFalse();
		assertThat(desc.isArray()).isTrue();
		assertThat(desc.getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		assertThat(desc.getElementTypeDescriptor()).isEqualTo(TypeDescriptor.valueOf(Integer.class));
		assertThat(desc.isMap()).isFalse();
	}

	@Test
	void parameterMap() throws Exception {
		MethodParameter methodParameter = new MethodParameter(getClass().getMethod("testParameterMap", Map.class), 0);
		TypeDescriptor desc = new TypeDescriptor(methodParameter);
		assertThat(desc.getType()).isEqualTo(Map.class);
		assertThat(desc.getObjectType()).isEqualTo(Map.class);
		assertThat(desc.getName()).isEqualTo("java.util.Map");
		assertThat(desc.toString()).isEqualTo("java.util.Map<java.lang.Integer, java.util.List<java.lang.String>>");
		assertThat(desc.isPrimitive()).isFalse();
		assertThat(desc.getAnnotations()).isEmpty();
		assertThat(desc.isCollection()).isFalse();
		assertThat(desc.isArray()).isFalse();
		assertThat(desc.isMap()).isTrue();
		assertThat(desc.getMapValueTypeDescriptor()).isEqualTo(TypeDescriptor.nested(methodParameter, 1));
		assertThat(desc.getMapValueTypeDescriptor().getElementTypeDescriptor()).isEqualTo(TypeDescriptor.nested(methodParameter, 2));
		assertThat(desc.getMapKeyTypeDescriptor().getType()).isEqualTo(Integer.class);
		assertThat(desc.getMapValueTypeDescriptor().getType()).isEqualTo(List.class);
		assertThat(desc.getMapValueTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(String.class);
	}

	@Test
	void parameterAnnotated() throws Exception {
		TypeDescriptor t1 = new TypeDescriptor(new MethodParameter(getClass().getMethod("testAnnotatedMethod", String.class), 0));
		assertThat(t1.getType()).isEqualTo(String.class);
		assertThat(t1.getAnnotations()).hasSize(1);
		assertThat(t1.getAnnotation(ParameterAnnotation.class)).isNotNull();
		assertThat(t1.hasAnnotation(ParameterAnnotation.class)).isTrue();
		assertThat(t1.getAnnotation(ParameterAnnotation.class).value()).isEqualTo(123);
	}

	@Test
	void getAnnotationsReturnsClonedArray() throws Exception {
		TypeDescriptor t = new TypeDescriptor(new MethodParameter(getClass().getMethod("testAnnotatedMethod", String.class), 0));
		t.getAnnotations()[0] = null;
		assertThat(t.getAnnotations()[0]).isNotNull();
	}

	@Test
	void propertyComplex() throws Exception {
		Property property = new Property(getClass(), getClass().getMethod("getComplexProperty"),
				getClass().getMethod("setComplexProperty", Map.class));
		TypeDescriptor desc = new TypeDescriptor(property);
		assertThat(desc.getMapKeyTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(desc.getMapValueTypeDescriptor().getElementTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
	}

	@Test
	void propertyGenericType() throws Exception {
		GenericType<Integer> genericBean = new IntegerType();
		Property property = new Property(getClass(), genericBean.getClass().getMethod("getProperty"),
				genericBean.getClass().getMethod("setProperty", Integer.class));
		TypeDescriptor desc = new TypeDescriptor(property);
		assertThat(desc.getType()).isEqualTo(Integer.class);
	}

	@Test
	void propertyTypeCovariance() throws Exception {
		GenericType<Number> genericBean = new NumberType();
		Property property = new Property(getClass(), genericBean.getClass().getMethod("getProperty"),
				genericBean.getClass().getMethod("setProperty", Number.class));
		TypeDescriptor desc = new TypeDescriptor(property);
		assertThat(desc.getType()).isEqualTo(Integer.class);
	}

	@Test
	void propertyGenericTypeList() throws Exception {
		GenericType<Integer> genericBean = new IntegerType();
		Property property = new Property(getClass(), genericBean.getClass().getMethod("getListProperty"),
				genericBean.getClass().getMethod("setListProperty", List.class));
		TypeDescriptor desc = new TypeDescriptor(property);
		assertThat(desc.getType()).isEqualTo(List.class);
		assertThat(desc.getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
	}

	@Test
	void propertyGenericClassList() throws Exception {
		IntegerClass genericBean = new IntegerClass();
		Property property = new Property(genericBean.getClass(), genericBean.getClass().getMethod("getListProperty"),
				genericBean.getClass().getMethod("setListProperty", List.class));
		TypeDescriptor desc = new TypeDescriptor(property);
		assertThat(desc.getType()).isEqualTo(List.class);
		assertThat(desc.getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		assertThat(desc.getAnnotation(MethodAnnotation1.class)).isNotNull();
		assertThat(desc.hasAnnotation(MethodAnnotation1.class)).isTrue();
	}

	@Test
	void property() throws Exception {
		Property property = new Property(
				getClass(), getClass().getMethod("getProperty"), getClass().getMethod("setProperty", Map.class));
		TypeDescriptor desc = new TypeDescriptor(property);
		assertThat(desc.getType()).isEqualTo(Map.class);
		assertThat(desc.getMapKeyTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		assertThat(desc.getMapValueTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Long.class);
		assertThat(desc.getAnnotation(MethodAnnotation1.class)).isNotNull();
		assertThat(desc.getAnnotation(MethodAnnotation2.class)).isNotNull();
		assertThat(desc.getAnnotation(MethodAnnotation3.class)).isNotNull();
	}

	@Test
	void getAnnotationOnMethodThatIsLocallyAnnotated() throws Exception {
		assertAnnotationFoundOnMethod(MethodAnnotation1.class, "methodWithLocalAnnotation");
	}

	@Test
	void getAnnotationOnMethodThatIsMetaAnnotated() throws Exception {
		assertAnnotationFoundOnMethod(MethodAnnotation1.class, "methodWithComposedAnnotation");
	}

	@Test
	void getAnnotationOnMethodThatIsMetaMetaAnnotated() throws Exception {
		assertAnnotationFoundOnMethod(MethodAnnotation1.class, "methodWithComposedComposedAnnotation");
	}

	private void assertAnnotationFoundOnMethod(Class<? extends Annotation> annotationType, String methodName) throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(new MethodParameter(getClass().getMethod(methodName), -1));
		assertThat(typeDescriptor.getAnnotation(annotationType)).as("Should have found @" + annotationType.getSimpleName() + " on " + methodName + ".").isNotNull();
	}

	@Test
	void fieldScalar() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(getClass().getField("fieldScalar"));
		assertThat(typeDescriptor.isPrimitive()).isFalse();
		assertThat(typeDescriptor.isArray()).isFalse();
		assertThat(typeDescriptor.isCollection()).isFalse();
		assertThat(typeDescriptor.isMap()).isFalse();
		assertThat(typeDescriptor.getType()).isEqualTo(Integer.class);
		assertThat(typeDescriptor.getObjectType()).isEqualTo(Integer.class);
	}

	@Test
	void fieldList() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfString"));
		assertThat(typeDescriptor.isArray()).isFalse();
		assertThat(typeDescriptor.getType()).isEqualTo(List.class);
		assertThat(typeDescriptor.getElementTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(typeDescriptor.toString()).isEqualTo("java.util.List<java.lang.String>");
	}

	@Test
	void fieldListOfListOfString() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfListOfString"));
		assertThat(typeDescriptor.isArray()).isFalse();
		assertThat(typeDescriptor.getType()).isEqualTo(List.class);
		assertThat(typeDescriptor.getElementTypeDescriptor().getType()).isEqualTo(List.class);
		assertThat(typeDescriptor.getElementTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(typeDescriptor.toString()).isEqualTo("java.util.List<java.util.List<java.lang.String>>");
	}

	@Test
	void fieldListOfListUnknown() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("listOfListOfUnknown"));
		assertThat(typeDescriptor.isArray()).isFalse();
		assertThat(typeDescriptor.getType()).isEqualTo(List.class);
		assertThat(typeDescriptor.getElementTypeDescriptor().getType()).isEqualTo(List.class);
		assertThat(typeDescriptor.getElementTypeDescriptor().getElementTypeDescriptor()).isNull();
		assertThat(typeDescriptor.toString()).isEqualTo("java.util.List<java.util.List<?>>");
	}

	@Test
	void fieldArray() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("intArray"));
		assertThat(typeDescriptor.isArray()).isTrue();
		assertThat(typeDescriptor.getElementTypeDescriptor().getType()).isEqualTo(int.class);
		assertThat(typeDescriptor.toString()).isEqualTo("int[]");
	}

	@Test
	void fieldComplexTypeDescriptor() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("arrayOfListOfString"));
		assertThat(typeDescriptor.isArray()).isTrue();
		assertThat(typeDescriptor.getElementTypeDescriptor().getType()).isEqualTo(List.class);
		assertThat(typeDescriptor.getElementTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(typeDescriptor.toString()).isEqualTo("java.util.List<java.lang.String>[]");
	}

	@Test
	void fieldComplexTypeDescriptor2() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(TypeDescriptorTests.class.getDeclaredField("nestedMapField"));
		assertThat(typeDescriptor.isMap()).isTrue();
		assertThat(typeDescriptor.getMapKeyTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(typeDescriptor.getMapValueTypeDescriptor().getType()).isEqualTo(List.class);
		assertThat(typeDescriptor.getMapValueTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		assertThat(typeDescriptor.toString()).isEqualTo("java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>");
	}

	@Test
	void fieldMap() throws Exception {
		TypeDescriptor desc = new TypeDescriptor(TypeDescriptorTests.class.getField("fieldMap"));
		assertThat(desc.isMap()).isTrue();
		assertThat(desc.getMapKeyTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		assertThat(desc.getMapValueTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Long.class);
	}

	@Test
	void fieldAnnotated() throws Exception {
		TypeDescriptor typeDescriptor = new TypeDescriptor(getClass().getField("fieldAnnotated"));
		assertThat(typeDescriptor.getAnnotations()).hasSize(1);
		assertThat(typeDescriptor.getAnnotation(FieldAnnotation.class)).isNotNull();
	}

	@Test
	void valueOfScalar() {
		TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(Integer.class);
		assertThat(typeDescriptor.isPrimitive()).isFalse();
		assertThat(typeDescriptor.isArray()).isFalse();
		assertThat(typeDescriptor.isCollection()).isFalse();
		assertThat(typeDescriptor.isMap()).isFalse();
		assertThat(typeDescriptor.getType()).isEqualTo(Integer.class);
		assertThat(typeDescriptor.getObjectType()).isEqualTo(Integer.class);
	}

	@Test
	void valueOfPrimitive() {
		TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(int.class);
		assertThat(typeDescriptor.isPrimitive()).isTrue();
		assertThat(typeDescriptor.isArray()).isFalse();
		assertThat(typeDescriptor.isCollection()).isFalse();
		assertThat(typeDescriptor.isMap()).isFalse();
		assertThat(typeDescriptor.getType()).isEqualTo(int.class);
		assertThat(typeDescriptor.getObjectType()).isEqualTo(Integer.class);
	}

	@Test
	void valueOfArray() {
		TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(int[].class);
		assertThat(typeDescriptor.isArray()).isTrue();
		assertThat(typeDescriptor.isCollection()).isFalse();
		assertThat(typeDescriptor.isMap()).isFalse();
		assertThat(typeDescriptor.getElementTypeDescriptor().getType()).isEqualTo(int.class);
	}

	@Test
	void valueOfCollection() {
		TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(Collection.class);
		assertThat(typeDescriptor.isCollection()).isTrue();
		assertThat(typeDescriptor.isArray()).isFalse();
		assertThat(typeDescriptor.isMap()).isFalse();
		assertThat((Object) typeDescriptor.getElementTypeDescriptor()).isNull();
	}

	@Test
	void forObject() {
		TypeDescriptor desc = TypeDescriptor.forObject("3");
		assertThat(desc.getType()).isEqualTo(String.class);
	}

	@Test
	void forObjectNullTypeDescriptor() {
		TypeDescriptor desc = TypeDescriptor.forObject(null);
		assertThat((Object) desc).isNull();
	}

	@Test
	void nestedMethodParameterType2Levels() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test2", List.class), 0), 2);
		assertThat(t1.getType()).isEqualTo(String.class);
	}

	@Test
	void nestedMethodParameterTypeMap() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test3", Map.class), 0), 1);
		assertThat(t1.getType()).isEqualTo(String.class);
	}

	@Test
	void nestedMethodParameterTypeMapTwoLevels() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test4", List.class), 0), 2);
		assertThat(t1.getType()).isEqualTo(String.class);
	}

	@Test
	void nestedMethodParameterNot1NestedLevel() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test4", List.class), 0, 2), 2));
	}

	@Test
	void nestedTooManyLevels() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test4", List.class), 0), 3);
		assertThat((Object) t1).isNull();
	}

	@Test
	void nestedMethodParameterTypeNotNestable() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test5", String.class), 0), 2);
		assertThat((Object) t1).isNull();
	}

	@Test
	void nestedMethodParameterTypeInvalidNestingLevel() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test5", String.class), 0, 2), 2));
	}

	@Test
	void nestedNotParameterized() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test6", List.class), 0), 1);
		assertThat(t1.getType()).isEqualTo(List.class);
		assertThat(t1.toString()).isEqualTo("java.util.List<?>");
		TypeDescriptor t2 = TypeDescriptor.nested(new MethodParameter(getClass().getMethod("test6", List.class), 0), 2);
		assertThat((Object) t2).isNull();
	}

	@Test
	void nestedFieldTypeMapTwoLevels() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.nested(getClass().getField("test4"), 2);
		assertThat(t1.getType()).isEqualTo(String.class);
	}

	@Test
	void nestedPropertyTypeMapTwoLevels() throws Exception {
		Property property = new Property(getClass(), getClass().getMethod("getTest4"), getClass().getMethod("setTest4", List.class));
		TypeDescriptor t1 = TypeDescriptor.nested(property, 2);
		assertThat(t1.getType()).isEqualTo(String.class);
	}

	@Test
	void collection() {
		TypeDescriptor desc = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(Integer.class));
		assertThat(desc.getType()).isEqualTo(List.class);
		assertThat(desc.getObjectType()).isEqualTo(List.class);
		assertThat(desc.getName()).isEqualTo("java.util.List");
		assertThat(desc.toString()).isEqualTo("java.util.List<java.lang.Integer>");
		assertThat(desc.isPrimitive()).isFalse();
		assertThat(desc.getAnnotations()).isEmpty();
		assertThat(desc.isCollection()).isTrue();
		assertThat(desc.isArray()).isFalse();
		assertThat(desc.getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		assertThat(desc.getElementTypeDescriptor()).isEqualTo(TypeDescriptor.valueOf(Integer.class));
		assertThat(desc.isMap()).isFalse();
	}

	@Test
	void collectionNested() {
		TypeDescriptor desc = TypeDescriptor.collection(List.class, TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(Integer.class)));
		assertThat(desc.getType()).isEqualTo(List.class);
		assertThat(desc.getObjectType()).isEqualTo(List.class);
		assertThat(desc.getName()).isEqualTo("java.util.List");
		assertThat(desc.toString()).isEqualTo("java.util.List<java.util.List<java.lang.Integer>>");
		assertThat(desc.isPrimitive()).isFalse();
		assertThat(desc.getAnnotations()).isEmpty();
		assertThat(desc.isCollection()).isTrue();
		assertThat(desc.isArray()).isFalse();
		assertThat(desc.getElementTypeDescriptor().getType()).isEqualTo(List.class);
		assertThat(desc.getElementTypeDescriptor().getElementTypeDescriptor()).isEqualTo(TypeDescriptor.valueOf(Integer.class));
		assertThat(desc.isMap()).isFalse();
	}

	@Test
	void map() {
		TypeDescriptor desc = TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class));
		assertThat(desc.getType()).isEqualTo(Map.class);
		assertThat(desc.getObjectType()).isEqualTo(Map.class);
		assertThat(desc.getName()).isEqualTo("java.util.Map");
		assertThat(desc.toString()).isEqualTo("java.util.Map<java.lang.String, java.lang.Integer>");
		assertThat(desc.isPrimitive()).isFalse();
		assertThat(desc.getAnnotations()).isEmpty();
		assertThat(desc.isCollection()).isFalse();
		assertThat(desc.isArray()).isFalse();
		assertThat(desc.isMap()).isTrue();
		assertThat(desc.getMapKeyTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(desc.getMapValueTypeDescriptor().getType()).isEqualTo(Integer.class);
	}

	@Test
	void mapNested() {
		TypeDescriptor desc = TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(String.class),
				TypeDescriptor.map(Map.class, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
		assertThat(desc.getType()).isEqualTo(Map.class);
		assertThat(desc.getObjectType()).isEqualTo(Map.class);
		assertThat(desc.getName()).isEqualTo("java.util.Map");
		assertThat(desc.toString()).isEqualTo("java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Integer>>");
		assertThat(desc.isPrimitive()).isFalse();
		assertThat(desc.getAnnotations()).isEmpty();
		assertThat(desc.isCollection()).isFalse();
		assertThat(desc.isArray()).isFalse();
		assertThat(desc.isMap()).isTrue();
		assertThat(desc.getMapKeyTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(desc.getMapValueTypeDescriptor().getMapKeyTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(desc.getMapValueTypeDescriptor().getMapValueTypeDescriptor().getType()).isEqualTo(Integer.class);
	}

	@Test
	void narrow() {
		TypeDescriptor desc = TypeDescriptor.valueOf(Number.class);
		Integer value = 3;
		desc = desc.narrow(value);
		assertThat(desc.getType()).isEqualTo(Integer.class);
	}

	@Test
	void elementType() {
		TypeDescriptor desc = TypeDescriptor.valueOf(List.class);
		Integer value = 3;
		desc = desc.elementTypeDescriptor(value);
		assertThat(desc.getType()).isEqualTo(Integer.class);
	}

	@Test
	void elementTypePreserveContext() throws Exception {
		TypeDescriptor desc = new TypeDescriptor(getClass().getField("listPreserveContext"));
		assertThat(desc.getElementTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		List<Integer> value = new ArrayList<>(3);
		desc = desc.elementTypeDescriptor(value);
		assertThat(desc.getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		assertThat(desc.getAnnotation(FieldAnnotation.class)).isNotNull();
	}

	@Test
	void mapKeyType() {
		TypeDescriptor desc = TypeDescriptor.valueOf(Map.class);
		Integer value = 3;
		desc = desc.getMapKeyTypeDescriptor(value);
		assertThat(desc.getType()).isEqualTo(Integer.class);
	}

	@Test
	void mapKeyTypePreserveContext() throws Exception {
		TypeDescriptor desc = new TypeDescriptor(getClass().getField("mapPreserveContext"));
		assertThat(desc.getMapKeyTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		List<Integer> value = new ArrayList<>(3);
		desc = desc.getMapKeyTypeDescriptor(value);
		assertThat(desc.getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		assertThat(desc.getAnnotation(FieldAnnotation.class)).isNotNull();
	}

	@Test
	void mapValueType() {
		TypeDescriptor desc = TypeDescriptor.valueOf(Map.class);
		Integer value = 3;
		desc = desc.getMapValueTypeDescriptor(value);
		assertThat(desc.getType()).isEqualTo(Integer.class);
	}

	@Test
	void mapValueTypePreserveContext() throws Exception {
		TypeDescriptor desc = new TypeDescriptor(getClass().getField("mapPreserveContext"));
		assertThat(desc.getMapValueTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		List<Integer> value = new ArrayList<>(3);
		desc = desc.getMapValueTypeDescriptor(value);
		assertThat(desc.getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
		assertThat(desc.getAnnotation(FieldAnnotation.class)).isNotNull();
	}

	@Test
	void equality() throws Exception {
		TypeDescriptor t1 = TypeDescriptor.valueOf(String.class);
		TypeDescriptor t2 = TypeDescriptor.valueOf(String.class);
		TypeDescriptor t3 = TypeDescriptor.valueOf(Date.class);
		TypeDescriptor t4 = TypeDescriptor.valueOf(Date.class);
		TypeDescriptor t5 = TypeDescriptor.valueOf(List.class);
		TypeDescriptor t6 = TypeDescriptor.valueOf(List.class);
		TypeDescriptor t7 = TypeDescriptor.valueOf(Map.class);
		TypeDescriptor t8 = TypeDescriptor.valueOf(Map.class);
		assertThat(t2).isEqualTo(t1);
		assertThat(t4).isEqualTo(t3);
		assertThat(t6).isEqualTo(t5);
		assertThat(t8).isEqualTo(t7);

		TypeDescriptor t9 = new TypeDescriptor(getClass().getField("listField"));
		TypeDescriptor t10 = new TypeDescriptor(getClass().getField("listField"));
		assertThat(t10).isEqualTo(t9);

		TypeDescriptor t11 = new TypeDescriptor(getClass().getField("mapField"));
		TypeDescriptor t12 = new TypeDescriptor(getClass().getField("mapField"));
		assertThat(t12).isEqualTo(t11);

		MethodParameter testAnnotatedMethod = new MethodParameter(getClass().getMethod("testAnnotatedMethod", String.class), 0);
		TypeDescriptor t13 = new TypeDescriptor(testAnnotatedMethod);
		TypeDescriptor t14 = new TypeDescriptor(testAnnotatedMethod);
		assertThat(t14).isEqualTo(t13);

		TypeDescriptor t15 = new TypeDescriptor(testAnnotatedMethod);
		TypeDescriptor t16 = new TypeDescriptor(new MethodParameter(getClass().getMethod("testAnnotatedMethodDifferentAnnotationValue", String.class), 0));
		assertThat(t16).isNotEqualTo(t15);

		TypeDescriptor t17 = new TypeDescriptor(testAnnotatedMethod);
		TypeDescriptor t18 = new TypeDescriptor(new MethodParameter(getClass().getMethod("test5", String.class), 0));
		assertThat(t18).isNotEqualTo(t17);
	}

	@Test
	void isAssignableTypes() {
		assertThat(TypeDescriptor.valueOf(Integer.class).isAssignableTo(TypeDescriptor.valueOf(Number.class))).isTrue();
		assertThat(TypeDescriptor.valueOf(Number.class).isAssignableTo(TypeDescriptor.valueOf(Integer.class))).isFalse();
		assertThat(TypeDescriptor.valueOf(String.class).isAssignableTo(TypeDescriptor.valueOf(String[].class))).isFalse();
	}

	@Test
	void isAssignableElementTypes() throws Exception {
		assertThat(new TypeDescriptor(getClass().getField("listField")).isAssignableTo(new TypeDescriptor(getClass().getField("listField")))).isTrue();
		assertThat(new TypeDescriptor(getClass().getField("notGenericList")).isAssignableTo(new TypeDescriptor(getClass().getField("listField")))).isTrue();
		assertThat(new TypeDescriptor(getClass().getField("listField")).isAssignableTo(new TypeDescriptor(getClass().getField("notGenericList")))).isTrue();
		assertThat(new TypeDescriptor(getClass().getField("isAssignableElementTypes")).isAssignableTo(new TypeDescriptor(getClass().getField("listField")))).isFalse();
		assertThat(TypeDescriptor.valueOf(List.class).isAssignableTo(new TypeDescriptor(getClass().getField("listField")))).isTrue();
	}

	@Test
	void isAssignableMapKeyValueTypes() throws Exception {
		assertThat(new TypeDescriptor(getClass().getField("mapField")).isAssignableTo(new TypeDescriptor(getClass().getField("mapField")))).isTrue();
		assertThat(new TypeDescriptor(getClass().getField("notGenericMap")).isAssignableTo(new TypeDescriptor(getClass().getField("mapField")))).isTrue();
		assertThat(new TypeDescriptor(getClass().getField("mapField")).isAssignableTo(new TypeDescriptor(getClass().getField("notGenericMap")))).isTrue();
		assertThat(new TypeDescriptor(getClass().getField("isAssignableMapKeyValueTypes")).isAssignableTo(new TypeDescriptor(getClass().getField("mapField")))).isFalse();
		assertThat(TypeDescriptor.valueOf(Map.class).isAssignableTo(new TypeDescriptor(getClass().getField("mapField")))).isTrue();
	}

	@Test
	void multiValueMap() throws Exception {
		TypeDescriptor td = new TypeDescriptor(getClass().getField("multiValueMap"));
		assertThat(td.isMap()).isTrue();
		assertThat(td.getMapKeyTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(td.getMapValueTypeDescriptor().getType()).isEqualTo(List.class);
		assertThat(td.getMapValueTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
	}

	@Test
	void passDownGeneric() throws Exception {
		TypeDescriptor td = new TypeDescriptor(getClass().getField("passDownGeneric"));
		assertThat(td.getElementTypeDescriptor().getType()).isEqualTo(List.class);
		assertThat(td.getElementTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Set.class);
		assertThat(td.getElementTypeDescriptor().getElementTypeDescriptor().getElementTypeDescriptor().getType()).isEqualTo(Integer.class);
	}

	@Test
	void upcast() throws Exception {
		Property property = new Property(getClass(), getClass().getMethod("getProperty"),
				getClass().getMethod("setProperty", Map.class));
		TypeDescriptor typeDescriptor = new TypeDescriptor(property);
		TypeDescriptor upcast = typeDescriptor.upcast(Object.class);
		assertThat(upcast.getAnnotation(MethodAnnotation1.class)).isNotNull();
	}

	@Test
	void upCastNotSuper() throws Exception {
		Property property = new Property(getClass(), getClass().getMethod("getProperty"),
				getClass().getMethod("setProperty", Map.class));
		TypeDescriptor typeDescriptor = new TypeDescriptor(property);
		assertThatIllegalArgumentException().isThrownBy(() ->
				typeDescriptor.upcast(Collection.class))
			.withMessage("interface java.util.Map is not assignable to interface java.util.Collection");
	}

	@Test
	void elementTypeForCollectionSubclass() {
		@SuppressWarnings("serial")
		class CustomSet extends HashSet<String> {
		}

		assertThat(TypeDescriptor.valueOf(String.class)).isEqualTo(TypeDescriptor.valueOf(CustomSet.class).getElementTypeDescriptor());
		assertThat(TypeDescriptor.valueOf(String.class)).isEqualTo(TypeDescriptor.forObject(new CustomSet()).getElementTypeDescriptor());
	}

	@Test
	void elementTypeForMapSubclass() {
		@SuppressWarnings("serial")
		class CustomMap extends HashMap<String, Integer> {
		}

		assertThat(TypeDescriptor.valueOf(String.class)).isEqualTo(TypeDescriptor.valueOf(CustomMap.class).getMapKeyTypeDescriptor());
		assertThat(TypeDescriptor.valueOf(Integer.class)).isEqualTo(TypeDescriptor.valueOf(CustomMap.class).getMapValueTypeDescriptor());
		assertThat(TypeDescriptor.valueOf(String.class)).isEqualTo(TypeDescriptor.forObject(new CustomMap()).getMapKeyTypeDescriptor());
		assertThat(TypeDescriptor.valueOf(Integer.class)).isEqualTo(TypeDescriptor.forObject(new CustomMap()).getMapValueTypeDescriptor());
	}

	@Test
	void createMapArray() {
		TypeDescriptor mapType = TypeDescriptor.map(
				LinkedHashMap.class, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class));
		TypeDescriptor arrayType = TypeDescriptor.array(mapType);
		assertThat(LinkedHashMap[].class).isEqualTo(arrayType.getType());
		assertThat(mapType).isEqualTo(arrayType.getElementTypeDescriptor());
	}

	@Test
	void createStringArray() {
		TypeDescriptor arrayType = TypeDescriptor.array(TypeDescriptor.valueOf(String.class));
		assertThat(TypeDescriptor.valueOf(String[].class)).isEqualTo(arrayType);
	}

	@Test
	void createNullArray() {
		assertThat((Object) TypeDescriptor.array(null)).isNull();
	}

	@Test
	void serializable() throws Exception {
		TypeDescriptor typeDescriptor = TypeDescriptor.forObject("");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream outputStream = new ObjectOutputStream(out);
		outputStream.writeObject(typeDescriptor);
		ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(
				out.toByteArray()));
		TypeDescriptor readObject = (TypeDescriptor) inputStream.readObject();
		assertThat(readObject).isEqualTo(typeDescriptor);
	}

	@Test
	void createCollectionWithNullElement() {
		TypeDescriptor typeDescriptor = TypeDescriptor.collection(List.class, null);
		assertThat(typeDescriptor.getElementTypeDescriptor()).isNull();
	}

	@Test
	void createMapWithNullElements() {
		TypeDescriptor typeDescriptor = TypeDescriptor.map(LinkedHashMap.class, null, null);
		assertThat(typeDescriptor.getMapKeyTypeDescriptor()).isNull();
		assertThat(typeDescriptor.getMapValueTypeDescriptor()).isNull();
	}

	@Test
	void getSource() throws Exception {
		Field field = getClass().getField("fieldScalar");
		MethodParameter methodParameter = new MethodParameter(getClass().getMethod("testParameterPrimitive", int.class), 0);
		assertThat(new TypeDescriptor(field).getSource()).isEqualTo(field);
		assertThat(new TypeDescriptor(methodParameter).getSource()).isEqualTo(methodParameter);
		assertThat(TypeDescriptor.valueOf(Integer.class).getSource()).isEqualTo(Integer.class);
	}

	@Test  // gh-31672
	void equalityWithGenerics() {
		ResolvableType rt1 = ResolvableType.forClassWithGenerics(Optional.class, Integer.class);
		ResolvableType rt2 = ResolvableType.forClassWithGenerics(Optional.class, String.class);

		TypeDescriptor td1 = new TypeDescriptor(rt1, null, null);
		TypeDescriptor td2 = new TypeDescriptor(rt2, null, null);

		assertThat(td1).isNotEqualTo(td2);
	}


	// Methods designed for test introspection

	public void testParameterPrimitive(int primitive) {
	}

	public void testParameterScalar(String value) {
	}

	public void testParameterList(List<List<Map<Integer, Enum<?>>>> list) {
	}

	public void testParameterListNoParamTypes(List list) {
	}

	public void testParameterArray(Integer[] array) {
	}

	public void testParameterMap(Map<Integer, List<String>> map) {
	}

	public void test1(List<String> param1) {
	}

	public void test2(List<List<String>> param1) {
	}

	public void test3(Map<Integer, String> param1) {
	}

	public void test4(List<Map<Integer, String>> param1) {
	}

	public void test5(String param1) {
	}

	public void test6(List<List> param1) {
	}

	public List<Map<Integer, String>> getTest4() {
		return null;
	}

	public void setTest4(List<Map<Integer, String>> test4) {
	}

	public Map<String, List<List<Integer>>> getComplexProperty() {
		return null;
	}

	@MethodAnnotation1
	public Map<List<Integer>, List<Long>> getProperty() {
		return property;
	}

	@MethodAnnotation2
	public void setProperty(Map<List<Integer>, List<Long>> property) {
		this.property = property;
	}

	@MethodAnnotation1
	public void methodWithLocalAnnotation() {
	}

	@ComposedMethodAnnotation1
	public void methodWithComposedAnnotation() {
	}

	@ComposedComposedMethodAnnotation1
	public void methodWithComposedComposedAnnotation() {
	}

	public void setComplexProperty(Map<String, List<List<Integer>>> complexProperty) {
	}

	public void testAnnotatedMethod(@ParameterAnnotation(123) String parameter) {
	}

	public void testAnnotatedMethodDifferentAnnotationValue(@ParameterAnnotation(567) String parameter) {
	}


	// Fields designed for test introspection

	public Integer fieldScalar;

	public List<String> listOfString;

	public List<List<String>> listOfListOfString = new ArrayList<>();

	public List<List> listOfListOfUnknown = new ArrayList<>();

	public int[] intArray;

	public List<String>[] arrayOfListOfString;

	public List<Integer> listField = new ArrayList<>();

	public Map<String, Integer> mapField = new HashMap<>();

	public Map<String, List<Integer>> nestedMapField = new HashMap<>();

	public Map<List<Integer>, List<Long>> fieldMap;

	public List<Map<Integer, String>> test4;

	@FieldAnnotation
	public List<String> fieldAnnotated;

	@FieldAnnotation
	public List<List<Integer>> listPreserveContext;

	@FieldAnnotation
	public Map<List<Integer>, List<Integer>> mapPreserveContext;

	@MethodAnnotation3
	private Map<List<Integer>, List<Long>> property;

	public List notGenericList;

	public List<Number> isAssignableElementTypes;

	public Map notGenericMap;

	public Map<CharSequence, Number> isAssignableMapKeyValueTypes;

	public MultiValueMap<String, Integer> multiValueMap = new LinkedMultiValueMap<>();

	public PassDownGeneric<Integer> passDownGeneric = new PassDownGeneric<>();


	// Classes designed for test introspection

	@SuppressWarnings("serial")
	public static class PassDownGeneric<T> extends ArrayList<List<Set<T>>> {
	}


	public static class GenericClass<T> {

		public T getProperty() {
			return null;
		}

		public void setProperty(T t) {
		}

		@MethodAnnotation1
		public List<T> getListProperty() {
			return null;
		}

		public void setListProperty(List<T> t) {
		}
	}


	public static class IntegerClass extends GenericClass<Integer> {
	}


	public interface GenericType<T> {

		T getProperty();

		void setProperty(T t);

		List<T> getListProperty();

		void setListProperty(List<T> t);
	}


	public class IntegerType implements GenericType<Integer> {

		@Override
		public Integer getProperty() {
			return null;
		}

		@Override
		public void setProperty(Integer t) {
		}

		@Override
		public List<Integer> getListProperty() {
			return null;
		}

		@Override
		public void setListProperty(List<Integer> t) {
		}
	}


	public class NumberType implements GenericType<Number> {

		@Override
		public Integer getProperty() {
			return null;
		}

		@Override
		public void setProperty(Number t) {
		}

		@Override
		public List<Number> getListProperty() {
			return null;
		}

		@Override
		public void setListProperty(List<Number> t) {
		}
	}


	// Annotations used on tested elements

	@Target({ElementType.PARAMETER})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ParameterAnnotation {

		int value();
	}


	@Target({ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface FieldAnnotation {
	}


	@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MethodAnnotation1 {
	}


	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MethodAnnotation2 {
	}


	@Target({ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MethodAnnotation3 {
	}


	@MethodAnnotation1
	@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ComposedMethodAnnotation1 {
	}


	@ComposedMethodAnnotation1
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface ComposedComposedMethodAnnotation1 {
	}

}
