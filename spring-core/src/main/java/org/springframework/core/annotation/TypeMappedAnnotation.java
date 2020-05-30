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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link MergedAnnotation} that adapts attributes from a root annotation by
 * applying the mapping and mirroring rules of an {@link AnnotationTypeMapping}.
 *
 * <p>Root attribute values are extracted from a source object using a supplied
 * {@code BiFunction}. This allows various different annotation models to be
 * supported by the same class. For example, the attributes source might be an
 * actual {@link Annotation} instance where methods on the annotation instance
 * are {@linkplain ReflectionUtils#invokeMethod(Method, Object) invoked} to extract
 * values. Equally, the source could be a simple {@link Map} with values
 * extracted using {@link Map#get(Object)}.
 *
 * <p>Extracted root attribute values must be compatible with the attribute
 * return type, namely:
 *
 * <p><table border="1">
 * <tr><th>Return Type</th><th>Extracted Type</th></tr>
 * <tr><td>Class</td><td>Class or String</td></tr>
 * <tr><td>Class[]</td><td>Class[] or String[]</td></tr>
 * <tr><td>Annotation</td><td>Annotation, Map, or Object compatible with the value
 * extractor</td></tr>
 * <tr><td>Annotation[]</td><td>Annotation[], Map[], or Object[] where elements are
 * compatible with the value extractor</td></tr>
 * <tr><td>Other types</td><td>An exact match or the appropriate primitive wrapper</td></tr>
 * </table>
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.2
 * @param <A> the annotation type
 * @see TypeMappedAnnotations
 */
final class TypeMappedAnnotation<A extends Annotation> extends AbstractMergedAnnotation<A> {

	private static final Map<Class<?>, Object> EMPTY_ARRAYS;
	static {
		Map<Class<?>, Object> emptyArrays = new HashMap<>();
		emptyArrays.put(boolean.class, new boolean[0]);
		emptyArrays.put(byte.class, new byte[0]);
		emptyArrays.put(char.class, new char[0]);
		emptyArrays.put(double.class, new double[0]);
		emptyArrays.put(float.class, new float[0]);
		emptyArrays.put(int.class, new int[0]);
		emptyArrays.put(long.class, new long[0]);
		emptyArrays.put(short.class, new short[0]);
		emptyArrays.put(String.class, new String[0]);
		EMPTY_ARRAYS = Collections.unmodifiableMap(emptyArrays);
	}


	private final AnnotationTypeMapping mapping;

	@Nullable
	private final ClassLoader classLoader;

	@Nullable
	private final Object source;

	@Nullable
	private final Object rootAttributes;

	private final ValueExtractor valueExtractor;

	private final int aggregateIndex;

	private final boolean useMergedValues;

	@Nullable
	private final Predicate<String> attributeFilter;

	private final int[] resolvedRootMirrors;

	private final int[] resolvedMirrors;


	private TypeMappedAnnotation(AnnotationTypeMapping mapping, @Nullable ClassLoader classLoader,
			@Nullable Object source, @Nullable Object rootAttributes, ValueExtractor valueExtractor,
			int aggregateIndex) {

		this(mapping, classLoader, source, rootAttributes, valueExtractor, aggregateIndex, null);
	}

	private TypeMappedAnnotation(AnnotationTypeMapping mapping, @Nullable ClassLoader classLoader,
			@Nullable Object source, @Nullable Object rootAttributes, ValueExtractor valueExtractor,
			int aggregateIndex, @Nullable int[] resolvedRootMirrors) {

		this.mapping = mapping;
		this.classLoader = classLoader;
		this.source = source;
		this.rootAttributes = rootAttributes;
		this.valueExtractor = valueExtractor;
		this.aggregateIndex = aggregateIndex;
		this.useMergedValues = true;
		this.attributeFilter = null;
		this.resolvedRootMirrors = (resolvedRootMirrors != null ? resolvedRootMirrors :
				mapping.getRoot().getMirrorSets().resolve(source, rootAttributes, this.valueExtractor));
		this.resolvedMirrors = (getDistance() == 0 ? this.resolvedRootMirrors :
				mapping.getMirrorSets().resolve(source, this, this::getValueForMirrorResolution));
	}

	private TypeMappedAnnotation(AnnotationTypeMapping mapping, @Nullable ClassLoader classLoader,
			@Nullable Object source, @Nullable Object rootAnnotation, ValueExtractor valueExtractor,
			int aggregateIndex, boolean useMergedValues, @Nullable Predicate<String> attributeFilter,
			int[] resolvedRootMirrors, int[] resolvedMirrors) {

		this.classLoader = classLoader;
		this.source = source;
		this.rootAttributes = rootAnnotation;
		this.valueExtractor = valueExtractor;
		this.mapping = mapping;
		this.aggregateIndex = aggregateIndex;
		this.useMergedValues = useMergedValues;
		this.attributeFilter = attributeFilter;
		this.resolvedRootMirrors = resolvedRootMirrors;
		this.resolvedMirrors = resolvedMirrors;
	}


	@Override
	@SuppressWarnings("unchecked")
	public Class<A> getType() {
		return (Class<A>) this.mapping.getAnnotationType();
	}

	@Override
	public List<Class<? extends Annotation>> getMetaTypes() {
		return this.mapping.getMetaTypes();
	}

	@Override
	public boolean isPresent() {
		return true;
	}

	@Override
	public int getDistance() {
		return this.mapping.getDistance();
	}

	@Override
	public int getAggregateIndex() {
		return this.aggregateIndex;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}

	@Override
	@Nullable
	public MergedAnnotation<?> getMetaSource() {
		AnnotationTypeMapping metaSourceMapping = this.mapping.getSource();
		if (metaSourceMapping == null) {
			return null;
		}
		return new TypeMappedAnnotation<>(metaSourceMapping, this.classLoader, this.source,
				this.rootAttributes, this.valueExtractor, this.aggregateIndex, this.resolvedRootMirrors);
	}

	@Override
	public MergedAnnotation<?> getRoot() {
		if (getDistance() == 0) {
			return this;
		}
		AnnotationTypeMapping rootMapping = this.mapping.getRoot();
		return new TypeMappedAnnotation<>(rootMapping, this.classLoader, this.source,
				this.rootAttributes, this.valueExtractor, this.aggregateIndex, this.resolvedRootMirrors);
	}

	@Override
	public boolean hasDefaultValue(String attributeName) {
		int attributeIndex = getAttributeIndex(attributeName, true);
		Object value = getValue(attributeIndex, true, false);
		return (value == null || this.mapping.isEquivalentToDefaultValue(attributeIndex, value, this.valueExtractor));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName, Class<T> type)
			throws NoSuchElementException {

		int attributeIndex = getAttributeIndex(attributeName, true);
		Method attribute = this.mapping.getAttributes().get(attributeIndex);
		Assert.notNull(type, "Type must not be null");
		Assert.isAssignable(type, attribute.getReturnType(),
				() -> "Attribute " + attributeName + " type mismatch:");
		return (MergedAnnotation<T>) getRequiredValue(attributeIndex, attributeName);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(
			String attributeName, Class<T> type) throws NoSuchElementException {

		int attributeIndex = getAttributeIndex(attributeName, true);
		Method attribute = this.mapping.getAttributes().get(attributeIndex);
		Class<?> componentType = attribute.getReturnType().getComponentType();
		Assert.notNull(type, "Type must not be null");
		Assert.notNull(componentType, () -> "Attribute " + attributeName + " is not an array");
		Assert.isAssignable(type, componentType, () -> "Attribute " + attributeName + " component type mismatch:");
		return (MergedAnnotation<T>[]) getRequiredValue(attributeIndex, attributeName);
	}

	@Override
	public <T> Optional<T> getDefaultValue(String attributeName, Class<T> type) {
		int attributeIndex = getAttributeIndex(attributeName, false);
		if (attributeIndex == -1) {
			return Optional.empty();
		}
		Method attribute = this.mapping.getAttributes().get(attributeIndex);
		return Optional.ofNullable(adapt(attribute, attribute.getDefaultValue(), type));
	}

	@Override
	public MergedAnnotation<A> filterAttributes(Predicate<String> predicate) {
		if (this.attributeFilter != null) {
			predicate = this.attributeFilter.and(predicate);
		}
		return new TypeMappedAnnotation<>(this.mapping, this.classLoader, this.source, this.rootAttributes,
				this.valueExtractor, this.aggregateIndex, this.useMergedValues, predicate,
				this.resolvedRootMirrors, this.resolvedMirrors);
	}

	@Override
	public MergedAnnotation<A> withNonMergedAttributes() {
		return new TypeMappedAnnotation<>(this.mapping, this.classLoader, this.source, this.rootAttributes,
				this.valueExtractor, this.aggregateIndex, false, this.attributeFilter,
				this.resolvedRootMirrors, this.resolvedMirrors);
	}

	@Override
	public Map<String, Object> asMap(Adapt... adaptations) {
		return Collections.unmodifiableMap(asMap(mergedAnnotation -> new LinkedHashMap<>(), adaptations));
	}

	@Override
	public <T extends Map<String, Object>> T asMap(Function<MergedAnnotation<?>, T> factory, Adapt... adaptations) {
		T map = factory.apply(this);
		Assert.state(map != null, "Factory used to create MergedAnnotation Map must not return null");
		AttributeMethods attributes = this.mapping.getAttributes();
		for (int i = 0; i < attributes.size(); i++) {
			Method attribute = attributes.get(i);
			Object value = (isFiltered(attribute.getName()) ? null :
					getValue(i, getTypeForMapOptions(attribute, adaptations)));
			if (value != null) {
				map.put(attribute.getName(),
						adaptValueForMapOptions(attribute, value, map.getClass(), factory, adaptations));
			}
		}
		return map;
	}

	private Class<?> getTypeForMapOptions(Method attribute, Adapt[] adaptations) {
		Class<?> attributeType = attribute.getReturnType();
		Class<?> componentType = (attributeType.isArray() ? attributeType.getComponentType() : attributeType);
		if (Adapt.CLASS_TO_STRING.isIn(adaptations) && componentType == Class.class) {
			return (attributeType.isArray() ? String[].class : String.class);
		}
		return Object.class;
	}

	private <T extends Map<String, Object>> Object adaptValueForMapOptions(Method attribute, Object value,
			Class<?> mapType, Function<MergedAnnotation<?>, T> factory, Adapt[] adaptations) {

		if (value instanceof MergedAnnotation) {
			MergedAnnotation<?> annotation = (MergedAnnotation<?>) value;
			return (Adapt.ANNOTATION_TO_MAP.isIn(adaptations) ?
					annotation.asMap(factory, adaptations) : annotation.synthesize());
		}
		if (value instanceof MergedAnnotation[]) {
			MergedAnnotation<?>[] annotations = (MergedAnnotation<?>[]) value;
			if (Adapt.ANNOTATION_TO_MAP.isIn(adaptations)) {
				Object result = Array.newInstance(mapType, annotations.length);
				for (int i = 0; i < annotations.length; i++) {
					Array.set(result, i, annotations[i].asMap(factory, adaptations));
				}
				return result;
			}
			Object result = Array.newInstance(
					attribute.getReturnType().getComponentType(), annotations.length);
			for (int i = 0; i < annotations.length; i++) {
				Array.set(result, i, annotations[i].synthesize());
			}
			return result;
		}
		return value;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected A createSynthesized() {
		if (getType().isInstance(this.rootAttributes) && !isSynthesizable()) {
			return (A) this.rootAttributes;
		}
		return SynthesizedMergedAnnotationInvocationHandler.createProxy(this, getType());
	}

	private boolean isSynthesizable() {
		// Already synthesized?
		if (this.rootAttributes instanceof SynthesizedAnnotation) {
			return false;
		}
		return this.mapping.isSynthesizable();
	}

	@Override
	@Nullable
	protected <T> T getAttributeValue(String attributeName, Class<T> type) {
		int attributeIndex = getAttributeIndex(attributeName, false);
		return (attributeIndex != -1 ? getValue(attributeIndex, type) : null);
	}

	private Object getRequiredValue(int attributeIndex, String attributeName) {
		Object value = getValue(attributeIndex, Object.class);
		if (value == null) {
			throw new NoSuchElementException("No element at attribute index "
					+ attributeIndex + " for name " + attributeName);
		}
		return value;
	}

	@Nullable
	private <T> T getValue(int attributeIndex, Class<T> type) {
		Method attribute = this.mapping.getAttributes().get(attributeIndex);
		Object value = getValue(attributeIndex, true, false);
		if (value == null) {
			value = attribute.getDefaultValue();
		}
		return adapt(attribute, value, type);
	}

	@Nullable
	private Object getValue(int attributeIndex, boolean useConventionMapping, boolean forMirrorResolution) {
		AnnotationTypeMapping mapping = this.mapping;
		if (this.useMergedValues) {
			int mappedIndex = this.mapping.getAliasMapping(attributeIndex);
			if (mappedIndex == -1 && useConventionMapping) {
				mappedIndex = this.mapping.getConventionMapping(attributeIndex);
			}
			if (mappedIndex != -1) {
				mapping = mapping.getRoot();
				attributeIndex = mappedIndex;
			}
		}
		if (!forMirrorResolution) {
			attributeIndex =
					(mapping.getDistance() != 0 ? this.resolvedMirrors : this.resolvedRootMirrors)[attributeIndex];
		}
		if (attributeIndex == -1) {
			return null;
		}
		if (mapping.getDistance() == 0) {
			Method attribute = mapping.getAttributes().get(attributeIndex);
			Object result = this.valueExtractor.extract(attribute, this.rootAttributes);
			return (result != null ? result : attribute.getDefaultValue());
		}
		return getValueFromMetaAnnotation(attributeIndex, forMirrorResolution);
	}

	@Nullable
	private Object getValueFromMetaAnnotation(int attributeIndex, boolean forMirrorResolution) {
		Object value = null;
		if (this.useMergedValues || forMirrorResolution) {
			value = this.mapping.getMappedAnnotationValue(attributeIndex, forMirrorResolution);
		}
		if (value == null) {
			Method attribute = this.mapping.getAttributes().get(attributeIndex);
			value = ReflectionUtils.invokeMethod(attribute, this.mapping.getAnnotation());
		}
		return value;
	}

	@Nullable
	private Object getValueForMirrorResolution(Method attribute, Object annotation) {
		int attributeIndex = this.mapping.getAttributes().indexOf(attribute);
		boolean valueAttribute = VALUE.equals(attribute.getName());
		return getValue(attributeIndex, !valueAttribute, true);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private <T> T adapt(Method attribute, @Nullable Object value, Class<T> type) {
		if (value == null) {
			return null;
		}
		value = adaptForAttribute(attribute, value);
		type = getAdaptType(attribute, type);
		if (value instanceof Class && type == String.class) {
			value = ((Class<?>) value).getName();
		}
		else if (value instanceof String && type == Class.class) {
			value = ClassUtils.resolveClassName((String) value, getClassLoader());
		}
		else if (value instanceof Class[] && type == String[].class) {
			Class<?>[] classes = (Class<?>[]) value;
			String[] names = new String[classes.length];
			for (int i = 0; i < classes.length; i++) {
				names[i] = classes[i].getName();
			}
			value = names;
		}
		else if (value instanceof String[] && type == Class[].class) {
			String[] names = (String[]) value;
			Class<?>[] classes = new Class<?>[names.length];
			for (int i = 0; i < names.length; i++) {
				classes[i] = ClassUtils.resolveClassName(names[i], getClassLoader());
			}
			value = classes;
		}
		else if (value instanceof MergedAnnotation && type.isAnnotation()) {
			MergedAnnotation<?> annotation = (MergedAnnotation<?>) value;
			value = annotation.synthesize();
		}
		else if (value instanceof MergedAnnotation[] && type.isArray() && type.getComponentType().isAnnotation()) {
			MergedAnnotation<?>[] annotations = (MergedAnnotation<?>[]) value;
			Object array = Array.newInstance(type.getComponentType(), annotations.length);
			for (int i = 0; i < annotations.length; i++) {
				Array.set(array, i, annotations[i].synthesize());
			}
			value = array;
		}
		if (!type.isInstance(value)) {
			throw new IllegalArgumentException("Unable to adapt value of type " +
					value.getClass().getName() + " to " + type.getName());
		}
		return (T) value;
	}

	@SuppressWarnings("unchecked")
	private Object adaptForAttribute(Method attribute, Object value) {
		Class<?> attributeType = ClassUtils.resolvePrimitiveIfNecessary(attribute.getReturnType());
		if (attributeType.isArray() && !value.getClass().isArray()) {
			Object array = Array.newInstance(value.getClass(), 1);
			Array.set(array, 0, value);
			return adaptForAttribute(attribute, array);
		}
		if (attributeType.isAnnotation()) {
			return adaptToMergedAnnotation(value, (Class<? extends Annotation>) attributeType);
		}
		if (attributeType.isArray() && attributeType.getComponentType().isAnnotation()) {
			MergedAnnotation<?>[] result = new MergedAnnotation<?>[Array.getLength(value)];
			for (int i = 0; i < result.length; i++) {
				result[i] = adaptToMergedAnnotation(Array.get(value, i),
						(Class<? extends Annotation>) attributeType.getComponentType());
			}
			return result;
		}
		if ((attributeType == Class.class && value instanceof String) ||
				(attributeType == Class[].class && value instanceof String[]) ||
				(attributeType == String.class && value instanceof Class) ||
				(attributeType == String[].class && value instanceof Class[])) {
			return value;
		}
		if (attributeType.isArray() && isEmptyObjectArray(value)) {
			return emptyArray(attributeType.getComponentType());
		}
		if (!attributeType.isInstance(value)) {
			throw new IllegalStateException("Attribute '" + attribute.getName() +
					"' in annotation " + getType().getName() + " should be compatible with " +
					attributeType.getName() + " but a " + value.getClass().getName() +
					" value was returned");
		}
		return value;
	}

	private boolean isEmptyObjectArray(Object value) {
		return (value instanceof Object[] && ((Object[]) value).length == 0);
	}

	private Object emptyArray(Class<?> componentType) {
		Object result = EMPTY_ARRAYS.get(componentType);
		if (result == null) {
			result = Array.newInstance(componentType, 0);
		}
		return result;
	}

	private MergedAnnotation<?> adaptToMergedAnnotation(Object value, Class<? extends Annotation> annotationType) {
		if (value instanceof MergedAnnotation) {
			return (MergedAnnotation<?>) value;
		}
		AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(annotationType).get(0);
		return new TypeMappedAnnotation<>(
				mapping, null, this.source, value, getValueExtractor(value), this.aggregateIndex);
	}

	private ValueExtractor getValueExtractor(Object value) {
		if (value instanceof Annotation) {
			return ReflectionUtils::invokeMethod;
		}
		if (value instanceof Map) {
			return TypeMappedAnnotation::extractFromMap;
		}
		return this.valueExtractor;
	}

	@SuppressWarnings("unchecked")
	private <T> Class<T> getAdaptType(Method attribute, Class<T> type) {
		if (type != Object.class) {
			return type;
		}
		Class<?> attributeType = attribute.getReturnType();
		if (attributeType.isAnnotation()) {
			return (Class<T>) MergedAnnotation.class;
		}
		if (attributeType.isArray() && attributeType.getComponentType().isAnnotation()) {
			return (Class<T>) MergedAnnotation[].class;
		}
		return (Class<T>) ClassUtils.resolvePrimitiveIfNecessary(attributeType);
	}

	private int getAttributeIndex(String attributeName, boolean required) {
		Assert.hasText(attributeName, "Attribute name must not be null");
		int attributeIndex = (isFiltered(attributeName) ? -1 : this.mapping.getAttributes().indexOf(attributeName));
		if (attributeIndex == -1 && required) {
			throw new NoSuchElementException("No attribute named '" + attributeName +
					"' present in merged annotation " + getType().getName());
		}
		return attributeIndex;
	}

	private boolean isFiltered(String attributeName) {
		if (this.attributeFilter != null) {
			return !this.attributeFilter.test(attributeName);
		}
		return false;
	}

	@Nullable
	private ClassLoader getClassLoader() {
		if (this.classLoader != null) {
			return this.classLoader;
		}
		if (this.source != null) {
			if (this.source instanceof Class) {
				return ((Class<?>) source).getClassLoader();
			}
			if (this.source instanceof Member) {
				((Member) this.source).getDeclaringClass().getClassLoader();
			}
		}
		return null;
	}


	static <A extends Annotation> MergedAnnotation<A> from(@Nullable Object source, A annotation) {
		Assert.notNull(annotation, "Annotation must not be null");
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(annotation.annotationType());
		return new TypeMappedAnnotation<>(mappings.get(0), null, source, annotation, ReflectionUtils::invokeMethod, 0);
	}

	static <A extends Annotation> MergedAnnotation<A> of(
			@Nullable ClassLoader classLoader, @Nullable Object source,
			Class<A> annotationType, @Nullable Map<String, ?> attributes) {

		Assert.notNull(annotationType, "Annotation type must not be null");
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(annotationType);
		return new TypeMappedAnnotation<>(
				mappings.get(0), classLoader, source, attributes, TypeMappedAnnotation::extractFromMap, 0);
	}

	@Nullable
	static <A extends Annotation> TypeMappedAnnotation<A> createIfPossible(
			AnnotationTypeMapping mapping, MergedAnnotation<?> annotation, IntrospectionFailureLogger logger) {

		if (annotation instanceof TypeMappedAnnotation) {
			TypeMappedAnnotation<?> typeMappedAnnotation = (TypeMappedAnnotation<?>) annotation;
			return createIfPossible(mapping, typeMappedAnnotation.source,
					typeMappedAnnotation.rootAttributes,
					typeMappedAnnotation.valueExtractor,
					typeMappedAnnotation.aggregateIndex, logger);
		}
		return createIfPossible(mapping, annotation.getSource(), annotation.synthesize(),
				annotation.getAggregateIndex(), logger);
	}

	@Nullable
	static <A extends Annotation> TypeMappedAnnotation<A> createIfPossible(
			AnnotationTypeMapping mapping, @Nullable Object source, Annotation annotation,
			int aggregateIndex, IntrospectionFailureLogger logger) {

		return createIfPossible(mapping, source, annotation,
				ReflectionUtils::invokeMethod, aggregateIndex, logger);
	}

	@Nullable
	private static <A extends Annotation> TypeMappedAnnotation<A> createIfPossible(
			AnnotationTypeMapping mapping, @Nullable Object source, @Nullable Object rootAttribute,
			ValueExtractor valueExtractor, int aggregateIndex, IntrospectionFailureLogger logger) {

		try {
			return new TypeMappedAnnotation<>(mapping, null, source, rootAttribute,
					valueExtractor, aggregateIndex);
		}
		catch (Exception ex) {
			AnnotationUtils.rethrowAnnotationConfigurationException(ex);
			if (logger.isEnabled()) {
				String type = mapping.getAnnotationType().getName();
				String item = (mapping.getDistance() == 0 ? "annotation " + type :
						"meta-annotation " + type + " from " + mapping.getRoot().getAnnotationType().getName());
				logger.log("Failed to introspect " + item, source, ex);
			}
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Nullable
	static Object extractFromMap(Method attribute, @Nullable Object map) {
		return (map != null ? ((Map<String, ?>) map).get(attribute.getName()) : null);
	}

}
