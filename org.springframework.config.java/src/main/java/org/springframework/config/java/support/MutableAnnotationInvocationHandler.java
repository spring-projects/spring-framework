/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.config.java.support;

import static java.lang.String.*;
import static org.springframework.core.annotation.AnnotationUtils.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;


/**
 * Handles calls to {@link MutableAnnotation} attribute methods at runtime. Essentially
 * emulates what JDK annotation dynamic proxies do.
 *
 * @author Chris Beams
 * @see MutableAnnotation
 * @see MutableAnnotationUtils
 */
final class MutableAnnotationInvocationHandler implements InvocationHandler {

	private final Class<? extends Annotation> annoType;
	private final HashMap<String, Object> attributes = new HashMap<String, Object>();
	private final HashMap<String, Class<?>> attributeTypes = new HashMap<String, Class<?>>();

	public MutableAnnotationInvocationHandler(Class<? extends Annotation> annoType) {
		// pre-populate the attributes hash will all the names
		// and default values of the attributes defined in 'annoType'
		Method[] attribs = annoType.getDeclaredMethods();
		for (Method attrib : attribs) {
			this.attributes.put(attrib.getName(), getDefaultValue(annoType, attrib.getName()));
			this.attributeTypes.put(attrib.getName(), getAttributeType(annoType, attrib.getName()));
		}

		this.annoType = annoType;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Assert.isInstanceOf(Annotation.class, proxy);

		String methodName = method.getName();

		// first -> check to see if this method is an attribute on our annotation
		if (attributes.containsKey(methodName))
			return attributes.get(methodName);


		// second -> is it a method from java.lang.annotation.Annotation?
		if (methodName.equals("annotationType"))
			return annoType;


		// third -> is it a method from java.lang.Object?
		if (methodName.equals("toString"))
			return format("@%s(%s)", annoType.getName(), getAttribs());

		if (methodName.equals("equals"))
			return isEqualTo(proxy, args[0]);

		if (methodName.equals("hashCode"))
			return calculateHashCode(proxy);


		// finally -> is it a method specified by MutableAnno?
		if (methodName.equals("setAttributeValue")) {
			attributes.put((String) args[0], args[1]);
			return null; // setAttributeValue has a 'void' return type
		}

		if (methodName.equals("getAttributeType"))
			return attributeTypes.get(args[0]);

		throw new UnsupportedOperationException("this proxy does not support method: " + methodName);
	}

	/**
	 * Conforms to the hashCode() specification for Annotation.
	 * 
	 * @see Annotation#hashCode()
	 */
	private Object calculateHashCode(Object proxy) {
		int sum = 0;

		for (String attribName : attributes.keySet()) {
			Object attribValue = attributes.get(attribName);

			final int attribNameHashCode = attribName.hashCode();
			final int attribValueHashCode;

			if (attribValue == null)
				// memberValue may be null when a mutable annotation is being added to a
				// collection
				// and before it has actually been visited (and populated) by
				// MutableAnnotationVisitor
				attribValueHashCode = 0;
			else if (attribValue.getClass().isArray())
				attribValueHashCode = Arrays.hashCode((Object[]) attribValue);
			else
				attribValueHashCode = attribValue.hashCode();

			sum += (127 * attribNameHashCode) ^ attribValueHashCode;
		}

		return sum;
	}

	/**
	 * Compares <var>proxy</var> object and <var>other</var> object by comparing the return
	 * values of the methods specified by their common {@link Annotation} ancestry.
	 * <p/>
	 * <var>other</var> must be the same type as or a subtype of <var>proxy</var>. Will
	 * return false otherwise.
	 * <p/>
	 * Eagerly returns true if {@code proxy} == <var>other</var>
	 * </p>
	 * <p/>
	 * Conforms strictly to the equals() specification for Annotation
	 * </p>
	 * 
	 * @see Annotation#equals(Object)
	 */
	private Object isEqualTo(Object proxy, Object other) {
		if (proxy == other)
			return true;

		if (other == null)
			return false;

		if (!annoType.isAssignableFrom(other.getClass()))
			return false;

		for (String attribName : attributes.keySet()) {
			Object thisVal;
			Object thatVal;

			try {
				thisVal = attributes.get(attribName);
				thatVal = other.getClass().getDeclaredMethod(attribName).invoke(other);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}

			if ((thisVal == null) && (thatVal != null))
				return false;

			if ((thatVal == null) && (thisVal != null))
				return false;

			if (thatVal.getClass().isArray()) {
				if (!Arrays.equals((Object[]) thatVal, (Object[]) thisVal)) {
					return false;
				}
			} else if (thisVal instanceof Double) {
				if (!Double.valueOf((Double) thisVal).equals(Double.valueOf((Double) thatVal)))
					return false;
			} else if (thisVal instanceof Float) {
				if (!Float.valueOf((Float) thisVal).equals(Float.valueOf((Float) thatVal)))
					return false;
			} else if (!thisVal.equals(thatVal)) {
				return false;
			}
		}

		return true;
	}

	private String getAttribs() {
		ArrayList<String> attribs = new ArrayList<String>();

		for (String attribName : attributes.keySet())
			attribs.add(format("%s=%s", attribName, attributes.get(attribName)));

		return StringUtils.collectionToDelimitedString(attribs, ", ");
	}

	/**
	 * Retrieve the type of the given annotation attribute.
	 */
	private static Class<?> getAttributeType(Class<? extends Annotation> annotationType, String attributeName) {
		Method method = null;

		try {
			method = annotationType.getDeclaredMethod(attributeName);
		} catch (Exception ex) {
			ReflectionUtils.handleReflectionException(ex);
		}

		return method.getReturnType();
	}

}
