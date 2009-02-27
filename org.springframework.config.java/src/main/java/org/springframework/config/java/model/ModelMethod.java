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
package org.springframework.config.java.model;

import static java.lang.String.*;
import static org.springframework.config.java.Util.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.NoOp;

import org.springframework.util.Assert;


/** TODO: JAVADOC */
public final class ModelMethod implements Validatable {

    private final String name;
    private final int modifiers;
    private final ModelClass returnType;
    private final List<Annotation> annotations = new ArrayList<Annotation>();
    private transient ConfigurationClass declaringClass;
    private transient int lineNumber;
    private transient Factory factoryAnno;
    private transient final List<Validator> validators = new ArrayList<Validator>();

    public ModelMethod(String name, int modifiers, ModelClass returnType, Annotation... annotations) {
        Assert.hasText(name);
        this.name = name;

        Assert.notNull(annotations);
        for(Annotation annotation : annotations) {
            this.annotations.add(annotation);
            if(factoryAnno == null)
                factoryAnno = annotation.annotationType().getAnnotation(Factory.class);
        }
        
        Assert.isTrue(modifiers >= 0, "modifiers must be non-negative: " + modifiers);
        this.modifiers = modifiers;
        
        Assert.notNull(returnType);
        this.returnType = returnType;
    }

    public String getName() {
        return name;
    }
    
    public ModelClass getReturnType() {
        return returnType;
    }

    /**
     * @see  java.lang.reflect.Modifier
     */
    public int getModifiers() {
        return modifiers;
    }

    /**
     * Returns the annotation on this method matching <var>annoType</var> or null
     * IllegalStateException} if not present.
     * 
     * @see #getRequiredAnnotation(Class)
     */
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Class<T> annoType) {
        for(Annotation anno : annotations)
            if(anno.annotationType().equals(annoType))
                return (T) anno;
        
        return null;
    }

    /**
     * Returns the annotation on this method matching <var>annoType</var> or throws
     * {@link IllegalStateException} if not present.
     * 
     * @see #getAnnotation(Class)
     */
    public <T extends Annotation> T getRequiredAnnotation(Class<T> annoType) {
        T anno = getAnnotation(annoType);
        
        if(anno == null)
            throw new IllegalStateException(
                    format("annotation %s not found on %s", annoType.getSimpleName(), this));
        
        return anno;
    }

    /**
     * Sets up bi-directional relationship between this method and its declaring class.
     *
     * @see ConfigurationClass#addMethod(ModelMethod)
     */
    public void setDeclaringClass(ConfigurationClass declaringClass) {
        this.declaringClass = declaringClass;
    }

    public ConfigurationClass getDeclaringClass() {
        return declaringClass;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
    
    public void registerValidator(Validator validator) {
        validators.add(validator); 
    }
    
    public void validate(List<UsageError> errors) {
        for(Validator validator : validators)
            validator.validate(this, errors);
        
        if (Modifier.isPrivate(getModifiers()))
            errors.add(new PrivateMethodError());

        if (Modifier.isFinal(getModifiers()))
            errors.add(new FinalMethodError());
    }

    public BeanDefinitionRegistrar getRegistrar() {
        return getInstance(factoryAnno.registrarType());
    }

    public Set<Validator> getValidators() {
        HashSet<Validator> validator = new HashSet<Validator>();
                
        for(Class<? extends Validator> validatorType : factoryAnno.validatorTypes())
            validator.add(getInstance(validatorType));
        
        return validator;
    }

    public Callback getCallback() {
        Class<? extends Callback> callbackType = factoryAnno.callbackType();
        
        if(callbackType.equals(NoOp.class))
            return NoOp.INSTANCE;
        
        return getInstance(callbackType);
    }

    @Override
    public String toString() {
        String returnTypeName = returnType == null ? "<unknown>" : returnType.getSimpleName();
        return String.format("%s: name=%s; returnType=%s; modifiers=%d",
                             getClass().getSimpleName(), name, returnTypeName, modifiers);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
        result = prime * result + modifiers;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ModelMethod other = (ModelMethod) obj;
        if (annotations == null) {
            if (other.annotations != null)
                return false;
        } else if (!annotations.equals(other.annotations))
            return false;
        if (modifiers != other.modifiers)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (returnType == null) {
            if (other.returnType != null)
                return false;
        } else if (!returnType.equals(other.returnType))
            return false;
        return true;
    }

    /** JavaConfigMethods must be visible (non-private) in order to accommodate CGLIB. */
    public class PrivateMethodError extends UsageError {
        public PrivateMethodError() {
            super(getDeclaringClass(), getLineNumber());
        }

        @Override
        public String getDescription() {
            return format("method '%s' may not be private", getName());
        }
    }

    /** JavaConfigMethods must be extensible (non-final) in order to accommodate CGLIB. */
    public class FinalMethodError extends UsageError {
        public FinalMethodError() {
            super(getDeclaringClass(), getLineNumber());
        }

        @Override
        public String getDescription() {
            return format("method '%s' may not be final - remove the final modifier to continue",
                          getName());
        }
    }
    
}
