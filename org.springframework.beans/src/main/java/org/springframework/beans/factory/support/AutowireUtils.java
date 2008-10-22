/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

import org.springframework.core.JdkVersion;
import org.springframework.util.ClassUtils;

/**
 * Utility class that contains various methods useful for
 * the implementation of autowire-capable bean factories.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 1.1.2
 * @see AbstractAutowireCapableBeanFactory
 */
abstract class AutowireUtils {

	private static final String QUALIFIED_ANNOTATION_AUTOWIRE_CANDIDATE_RESOLVER_CLASS_NAME =
			"org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver";


	/**
	 * Sort the given constructors, preferring public constructors and "greedy" ones
	 * with a maximum of arguments. The result will contain public constructors first,
	 * with decreasing number of arguments, then non-public constructors, again with
	 * decreasing number of arguments.
	 * @param constructors the constructor array to sort
	 */
	public static void sortConstructors(Constructor[] constructors) {
		Arrays.sort(constructors, new Comparator() {
			public int compare(Object o1, Object o2) {
				Constructor c1 = (Constructor) o1;
				Constructor c2 = (Constructor) o2;
				boolean p1 = Modifier.isPublic(c1.getModifiers());
				boolean p2 = Modifier.isPublic(c2.getModifiers());
				if (p1 != p2) {
					return (p1 ? -1 : 1);
				}
				int c1pl = c1.getParameterTypes().length;
				int c2pl = c2.getParameterTypes().length;
				return (new Integer(c1pl)).compareTo(new Integer(c2pl)) * -1;
			}
		});
	}

	/**
	 * Determine whether the given bean property is excluded from dependency checks.
	 * <p>This implementation excludes properties defined by CGLIB.
	 * @param pd the PropertyDescriptor of the bean property
	 * @return whether the bean property is excluded
	 */
	public static boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
		Method wm = pd.getWriteMethod();
		if (wm == null) {
			return false;
		}
		if (wm.getDeclaringClass().getName().indexOf("$$") == -1) {
			// Not a CGLIB method so it's OK.
			return false;
		}
		// It was declared by CGLIB, but we might still want to autowire it
		// if it was actually declared by the superclass.
		Class superclass = wm.getDeclaringClass().getSuperclass();
		return !ClassUtils.hasMethod(superclass, wm.getName(), wm.getParameterTypes());
	}

	/**
	 * Return whether the setter method of the given bean property is defined
	 * in any of the given interfaces.
	 * @param pd the PropertyDescriptor of the bean property
	 * @param interfaces the Set of interfaces (Class objects)
	 * @return whether the setter method is defined by an interface
	 */
	public static boolean isSetterDefinedInInterface(PropertyDescriptor pd, Set interfaces) {
		Method setter = pd.getWriteMethod();
		if (setter != null) {
			Class targetClass = setter.getDeclaringClass();
			for (Iterator it = interfaces.iterator(); it.hasNext();) {
				Class ifc = (Class) it.next();
				if (ifc.isAssignableFrom(targetClass) &&
						ClassUtils.hasMethod(ifc, setter.getName(), setter.getParameterTypes())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * If at least Java 1.5, this will return an annotation-aware resolver.
	 * Otherwise it returns a resolver that checks the bean definition only.
	 */
	public static AutowireCandidateResolver createAutowireCandidateResolver() {
		if (JdkVersion.isAtLeastJava15()) {
			try {
				Class resolverClass = ClassUtils.forName(
						QUALIFIED_ANNOTATION_AUTOWIRE_CANDIDATE_RESOLVER_CLASS_NAME, AutowireUtils.class.getClassLoader());
				return (AutowireCandidateResolver) resolverClass.newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Unable to load Java 1.5 dependent class [" +
						QUALIFIED_ANNOTATION_AUTOWIRE_CANDIDATE_RESOLVER_CLASS_NAME + "]", ex);
			}
		}
		else {
			return new SimpleAutowireCandidateResolver();
		}
	}

}
