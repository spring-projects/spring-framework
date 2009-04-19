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

package org.springframework.context.annotation;

import java.io.IOException;
import java.io.InputStream;
import static java.lang.String.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.asm.ClassReader;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import static org.springframework.core.annotation.AnnotationUtils.*;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Various utility methods commonly used when interacting with ASM, classloading
 * and creating {@link ConfigurationClassAnnotation} instances.
 * 
 * @author Chris Beams
 * @since 3.0
 */
class ConfigurationClassReaderUtils {

	private static final Log logger = LogFactory.getLog(ConfigurationClassReaderUtils.class);

	/**
	 * Convert a type descriptor to a classname suitable for classloading with
	 * Class.forName().
	 * 
	 * @param typeDescriptor see ASM guide section 2.1.3
	 */
	public static String convertAsmTypeDescriptorToClassName(String typeDescriptor) {
		final String internalName; // See ASM guide section 2.1.2

		if ("V".equals(typeDescriptor))
			return Void.class.getName();
		if ("I".equals(typeDescriptor))
			return Integer.class.getName();
		if ("Z".equals(typeDescriptor))
			return Boolean.class.getName();

		// strip the leading array/object/primitive identifier
		if (typeDescriptor.startsWith("[["))
			internalName = typeDescriptor.substring(3);
		else if (typeDescriptor.startsWith("["))
			internalName = typeDescriptor.substring(2);
		else
			internalName = typeDescriptor.substring(1);

		// convert slashes to dots
		String className = internalName.replace('/', '.');

		// and strip trailing semicolon (if present)
		if (className.endsWith(";"))
			className = className.substring(0, internalName.length() - 1);

		return className;
	}

	/**
	 * @param methodDescriptor see ASM guide section 2.1.4
	 */
	public static String getReturnTypeFromAsmMethodDescriptor(String methodDescriptor) {
		String returnTypeDescriptor = methodDescriptor.substring(methodDescriptor.indexOf(')') + 1);
		return convertAsmTypeDescriptorToClassName(returnTypeDescriptor);
	}

	/**
	 * Create a new ASM {@link ClassReader} for <var>pathToClass</var>. Appends '.class' to
	 * pathToClass before attempting to load.
	 */
	public static ClassReader newAsmClassReader(String pathToClass, ClassLoader classLoader) {
		InputStream is = getClassAsStream(pathToClass, classLoader);
		return newAsmClassReader(is);
	}

	/**
	 * Convenience method that creates and returns a new ASM {@link ClassReader} for the
	 * given InputStream <var>is</var>, closing the InputStream after creating the
	 * ClassReader and rethrowing any IOException thrown during ClassReader instantiation as
	 * an unchecked exception. Logs and ignores any IOException thrown when closing the
	 * InputStream.
	 * 
	 * @param is InputStream that will be provided to the new ClassReader instance.
	 */
	public static ClassReader newAsmClassReader(InputStream is) {
		try {
			return new ClassReader(is);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("An unexpected exception occurred while creating ASM ClassReader: " + ex);
		}
		finally {
			try {
				is.close();
			}
			catch (IOException ex) {
				// ignore
			}
		}
	}

	/**
	 * Uses the default ClassLoader to load <var>pathToClass</var>. Appends '.class' to
	 * pathToClass before attempting to load.
	 * @param pathToClass resource path to class, not including .class suffix. e.g.: com/acme/MyClass
	 * @return inputStream for <var>pathToClass</var>
	 * @throws RuntimeException if <var>pathToClass</var> does not exist
	 */
	public static InputStream getClassAsStream(String pathToClass, ClassLoader classLoader) {
		String classFileName = pathToClass + ClassUtils.CLASS_FILE_SUFFIX;
		InputStream is = classLoader.getResourceAsStream(classFileName);
		if (is == null) {
			throw new IllegalStateException("Class file [" + classFileName + "] not found");
		}
		return is;
	}

	/**
	 * Loads the specified class using the default class loader, gracefully handling any
	 * {@link ClassNotFoundException} that may be thrown by issuing a WARN level logging
	 * statement and return null. This functionality is specifically implemented to
	 * accomodate tooling (Spring IDE) concerns, where user-defined types will not be
	 * available to the tooling.
	 * <p>
	 * Because {@link ClassNotFoundException} is compensated for by returning null, callers
	 * must take care to handle the null case appropriately.
	 * <p>
	 * In cases where the WARN logging statement is not desired, use the
	 * {@link #loadClass(String)} method, which returns null but issues no logging
	 * statements.
	 * <p>
	 * This method should only ever return null in the case of a user-defined type be
	 * processed at tooling time. Therefore, tooling may not be able to represent any custom
	 * annotation semantics, but JavaConfig itself will not have any problem loading and
	 * respecting them at actual runtime.
	 * 
	 * @param <T> type of class to be returned
	 * @param fqClassName fully-qualified class name
	 * 
	 * @return newly loaded class, null if class could not be found.
	 * 
	 * @see #loadClass(String)
	 * @see #loadRequiredClass(String)
	 * @see ClassUtils#getDefaultClassLoader()
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<? extends T> loadToolingSafeClass(String fqClassName, ClassLoader classLoader) {
		try {
			return (Class<? extends T>) classLoader.loadClass(fqClassName);
		}
		catch (ClassNotFoundException ex) {
			logger.warn(String.format("Unable to load class [%s], likely due to tooling-specific restrictions."
			        + "Attempting to continue, but unexpected errors may occur", fqClassName), ex);
			return null;
		}
	}

	/**
	 * Creates a {@link ConfigurationClassAnnotation} for {@code annoType}. JDK dynamic proxies are used,
	 * and the returned proxy implements both {@link ConfigurationClassAnnotation} and the annotation type.
	 * @param annoType annotation type that must be supplied and returned
	 * @param annoType type of annotation to create
	 */
	@SuppressWarnings("unchecked")
	public static ConfigurationClassAnnotation createMutableAnnotation(Class<? extends Annotation> annoType, ClassLoader classLoader) {
		MutableAnnotationInvocationHandler handler = new MutableAnnotationInvocationHandler(annoType);
		Class<?>[] interfaces = new Class<?>[] {annoType, ConfigurationClassAnnotation.class};
		return (ConfigurationClassAnnotation) Proxy.newProxyInstance(classLoader, interfaces, handler);
	}


	/**
	 * Handles calls to {@link ConfigurationClassAnnotation} attribute methods at runtime. Essentially
	 * emulates what JDK annotation dynamic proxies do.
	 */
	private static final class MutableAnnotationInvocationHandler implements InvocationHandler {

		private final Class<? extends Annotation> annoType;
		private final Map<String, Object> attributes = new HashMap<String, Object>();
		private final Map<String, Class<?>> attributeTypes = new HashMap<String, Class<?>>();

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
					throw new IllegalStateException(ex);
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

}
