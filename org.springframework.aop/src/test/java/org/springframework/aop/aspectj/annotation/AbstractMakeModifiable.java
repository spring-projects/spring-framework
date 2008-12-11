/*
 * Copyright 2002-2006 the original author or authors.
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
 
package org.springframework.aop.aspectj.annotation;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.util.ObjectUtils;

/**
 * Add a DeclareParents field in concrete subclasses, to identify
 * the type pattern to apply the introduction to.
 *
 * @author Rod Johnson
 * @since 2.0
 */
@Aspect
public abstract class AbstractMakeModifiable {
	
	public interface MutableModifable extends Modifiable {
		void markDirty();
	}
	
	public static class ModifiableImpl implements MutableModifable {
		private boolean modified;
		
		public void acceptChanges() {
			modified = false;
		}
		
		public boolean isModified() {
			return modified;
		}
		
		public void markDirty() {
			this.modified = true;
		}
	}
	
	@Before(value="execution(void set*(*)) && this(modifiable) && args(newValue)", 
			argNames="modifiable,newValue")
	public void recordModificationIfSetterArgumentDiffersFromOldValue(JoinPoint jp, 
		MutableModifable mixin, Object newValue) {
		
		/*
		 * We use the mixin to check and, if necessary, change,
		 * modification status. We need the JoinPoint to get the 
		 * setter method. We use newValue for comparison. 
		 * We try to invoke the getter if possible.
		 */
		
		if (mixin.isModified()) {
			// Already changed, don't need to change again
			//System.out.println("changed");
			return;
		}
		
		// Find the current raw value, by invoking the corresponding setter
		Method correspondingGetter =  getGetterFromSetter(((MethodSignature) jp.getSignature()).getMethod());
		boolean modified = true;
		if (correspondingGetter != null) {
			try {
				Object oldValue = correspondingGetter.invoke(jp.getTarget());
				//System.out.println("Old value=" + oldValue + "; new=" + newValue);
				modified = !ObjectUtils.nullSafeEquals(oldValue, newValue);
			}
			catch (Exception ex) {
				ex.printStackTrace();
				// Don't sweat on exceptions; assume value was modified
			}
		}
		else {
			//System.out.println("cannot get getter for " + jp);
		}
		if (modified) {
			mixin.markDirty();
		}
	}
	
	private Method getGetterFromSetter(Method setter) {
		String getterName = setter.getName().replaceFirst("set", "get");
		try {
			return setter.getDeclaringClass().getMethod(getterName, (Class[]) null);
		} 
		catch (NoSuchMethodException ex) {
			// must be write only
			return null;
		}
	}

}
