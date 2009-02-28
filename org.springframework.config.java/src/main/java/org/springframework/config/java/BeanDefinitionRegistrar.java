package org.springframework.config.java;

import java.lang.reflect.Method;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Registers bean definition(s) for a particular method, usually based on its annotation metadata.
 * 
 * <h3>Constraints</h3>
 * Implementations must have only a default constructor, or explicitly declare
 * a no-arg constructor.
 * 
 * @see Factory
 * @see ModelMethod
 * 
 * @author Chris Beams
 */
// TODO: SJC-242 document FactoryMethodHandler
// TODO: SJC-242 odd that the api here uses both ModelMethod and java.lang.reflect.Member
// TODO: SJC-242 document that there must be a no-arg ctor
public interface BeanDefinitionRegistrar {

    /**
     * Determines whether this registrar is capable of handling <var>method</var>.
     */
    // TODO: rename to supports() in alignment with Validator nomenclature
    boolean accepts(Method method);

    /**
     * Registers any bean definitions for <var>method</var> with <var>registry</var>.
     */
    void register(ModelMethod method, BeanDefinitionRegistry registry);

}
