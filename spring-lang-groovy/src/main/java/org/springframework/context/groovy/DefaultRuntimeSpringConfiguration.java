/*
 * Copyright 2010 the original author or authors.
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
package org.springframework.context.groovy;

import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;

import java.util.*;

/**
 * A programmable runtime Spring configuration that allows a spring ApplicationContext
 * to be constructed at runtime
 *
 * Credit must go to Solomon Duskis and the
 * article: http://jroller.com/page/Solomon?entry=programmatic_configuration_in_spring
 *
 */
public class DefaultRuntimeSpringConfiguration implements
        RuntimeSpringConfiguration {

    private static final Log LOG = LogFactory.getLog(DefaultRuntimeSpringConfiguration.class);
    protected GenericApplicationContext context;
    private Map<String, BeanConfiguration> beanConfigs = new HashMap<String, BeanConfiguration>();
    private Map<String, BeanDefinition> beanDefinitions = new HashMap<String, BeanDefinition>();
    private List<String> beanNames = new ArrayList<String>();
    protected ApplicationContext parent;
    protected ClassLoader classLoader;
    protected Map<String, List> aliases = new HashMap<String, List>();
    protected ListableBeanFactory beanFactory;

    public DefaultRuntimeSpringConfiguration() {
        super();
    }

    /**
     * Creates the ApplicationContext instance. Subclasses can override to customise the used ApplicationContext
     *
     * @param parent The parent ApplicationContext instance. Can be null.
     *
     * @return An instance of GenericApplicationContext         
     */
    protected GenericApplicationContext createApplicationContext(ApplicationContext parent) {
        if(parent != null && beanFactory!=null) {
            if(beanFactory instanceof DefaultListableBeanFactory) {
                return new GroovyPropertyApplicationContext((DefaultListableBeanFactory) beanFactory,parent);
            }
            else {
                throw new IllegalArgumentException("ListableBeanFactory set must be a subclass of DefaultListableBeanFactory");
            }
        }
        else if(beanFactory!=null) {
            if(beanFactory instanceof DefaultListableBeanFactory) {
                return new GroovyPropertyApplicationContext((DefaultListableBeanFactory) beanFactory);
            }
            else {
                throw new IllegalArgumentException("ListableBeanFactory set must be a subclass of DefaultListableBeanFactory");
            }
        }
        else if(parent !=null) {
            return new GroovyPropertyApplicationContext(parent);
        }
        else {
            return new GroovyPropertyApplicationContext();
        }
    }

    public DefaultRuntimeSpringConfiguration(ApplicationContext parent) {
        this(parent, null);
    }

    public DefaultRuntimeSpringConfiguration(ApplicationContext parent, ClassLoader cl) {
        super();
        this.parent = parent;
        this.classLoader = cl;
    }

    private void setClassLoaderOnContext(ClassLoader cl) {
        this.context.setClassLoader(cl);
        this.context.getBeanFactory().setBeanClassLoader(cl);
    }

    /**
     * Initialises the ApplicationContext instance
     */
    protected void initialiseApplicationContext() {
        if(this.context == null) {
            this.context = createApplicationContext(this.parent);
            if(classLoader != null)  {
                setClassLoaderOnContext(classLoader);
            }

            Assert.notNull(context);
        }
    }


    public BeanConfiguration addSingletonBean(String name, Class clazz) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name,clazz);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    public BeanConfiguration addPrototypeBean(String name, Class clazz) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name,clazz,true);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    public ApplicationContext getApplicationContext() {
        initialiseApplicationContext();
        registerBeansWithContext(context);
        context.refresh();
        return context;
    }

    public ApplicationContext getUnrefreshedApplicationContext() {
        initialiseApplicationContext();
        return context;
    }
    
    public BeanConfiguration addSingletonBean(String name) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    public BeanConfiguration createSingletonBean(Class clazz) {
        return new DefaultBeanConfiguration(clazz);
    }

    public BeanConfiguration addSingletonBean(String name, Class clazz, Collection args) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name,clazz,args);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    public BeanConfiguration addPrototypeBean(String name) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name,true);
        registerBeanConfiguration(name, bc);
        return bc;
    }

    private void registerBeanConfiguration(String name, BeanConfiguration bc) {
        beanConfigs.put(name,bc);
        beanNames.add(name);
    }

    public BeanConfiguration createSingletonBean(Class clazz, Collection constructorArguments) {
        return new DefaultBeanConfiguration(clazz, constructorArguments);
    }


    public BeanConfiguration createPrototypeBean(String name) {
        return new DefaultBeanConfiguration(name,true);
    }

    public BeanConfiguration createSingletonBean(String name) {
        return new DefaultBeanConfiguration(name);
    }

    public void addBeanConfiguration(String beanName, BeanConfiguration beanConfiguration) {
        beanConfiguration.setName(beanName);
        registerBeanConfiguration(beanName, beanConfiguration);
    }

    public void addBeanDefinition(String name, BeanDefinition bd) {
        beanDefinitions.put(name,bd);
        beanNames.add(name);
    }

    public boolean containsBean(String name) {
        return beanNames .contains(name);
    }

    public BeanConfiguration getBeanConfig(String name) {
        return beanConfigs.get(name);
    }

    public AbstractBeanDefinition createBeanDefinition(String name) {
        if(containsBean(name)) {
            if(beanDefinitions.containsKey(name))
                return (AbstractBeanDefinition)beanDefinitions.get(name);
            else if(beanConfigs.containsKey(name))
                return beanConfigs.get(name).getBeanDefinition();
        }
        return null;
    }

    public void registerPostProcessor(BeanFactoryPostProcessor processor) {
        initialiseApplicationContext();
        this.context.addBeanFactoryPostProcessor(processor);
    }



    public List getBeanNames() {
        return beanNames;
    }

    public void registerBeansWithContext(GenericApplicationContext applicationContext) {
            registerBeansWithRegistry(applicationContext);
    }

    public void registerBeansWithRegistry(BeanDefinitionRegistry registry) {

        registerUnrefreshedBeansWithRegistry(registry);
        registerBeanConfigsWithRegistry(registry);
        registerBeanDefinitionsWithRegistry(registry);

    }

    private void registerUnrefreshedBeansWithRegistry(BeanDefinitionRegistry registry) {
        if(context != null) {
            for(String beanName : context.getBeanDefinitionNames()) {
                registry.registerBeanDefinition(beanName, context.getBeanDefinition(beanName));
            }
        }
    }

    private void registerBeanConfigsWithRegistry(BeanDefinitionRegistry registry) {
        for (Object o : beanConfigs.values()) {
            BeanConfiguration bc = (BeanConfiguration) o;
            String beanName = bc.getName();
            if (LOG.isDebugEnabled()) {
                LOG.debug("[RuntimeConfiguration] Registering bean [" + beanName + "]");
                if (LOG.isTraceEnabled()) {
                    PropertyValue[] pvs = bc.getBeanDefinition()
                            .getPropertyValues()
                            .getPropertyValues();
                    for (PropertyValue pv : pvs) {
                        LOG.trace("[RuntimeConfiguration] With property [" + pv.getName() + "] set to [" + pv.getValue() + "]");
                    }
                }
            }


            if (registry.containsBeanDefinition(beanName)) {
                removeBeanDefinition(registry, beanName);
            }

            registry.registerBeanDefinition(beanName,
                    bc.getBeanDefinition());

            registerBeanAliases(registry, beanName);
        }
    }

    private void registerBeanDefinitionsWithRegistry(BeanDefinitionRegistry registry) {
        for (Object key : beanDefinitions.keySet()) {
            BeanDefinition bd = beanDefinitions.get(key);
            if (LOG.isDebugEnabled()) {
                LOG.debug("[RuntimeConfiguration] Registering bean [" + key + "]");
                if (LOG.isTraceEnabled()) {
                    PropertyValue[] pvs = bd.getPropertyValues().getPropertyValues();
                    for (PropertyValue pv : pvs) {
                        LOG.trace("[RuntimeConfiguration] With property [" + pv.getName() + "] set to [" + pv.getValue() + "]");
                    }
                }
            }
            final String beanName = key.toString();
            if (registry.containsBeanDefinition(beanName)) {
                removeBeanDefinition(registry, beanName);
            }

            registry.registerBeanDefinition(beanName, bd);

            registerBeanAliases(registry, beanName);
        }
    }

    public void registerBeansWithConfig(RuntimeSpringConfiguration targetSpringConfig) {
        if(targetSpringConfig!=null) {
            ApplicationContext ctx = targetSpringConfig.getUnrefreshedApplicationContext();
            if(ctx instanceof BeanDefinitionRegistry) {
                final BeanDefinitionRegistry registry = (BeanDefinitionRegistry) ctx;
                registerUnrefreshedBeansWithRegistry(registry);
                registerBeansWithRegistry(registry);
            }
            for (Map.Entry<String, BeanConfiguration> beanEntry : beanConfigs.entrySet()) {
                targetSpringConfig.addBeanConfiguration(beanEntry.getKey(), beanEntry.getValue());                
            }
        }
    }

    private void registerBeanAliases(BeanDefinitionRegistry beanDefinitionRegistry, String beanName) {
        List beanAliases = (List)aliases.get(beanName);
        if(beanAliases != null && !beanAliases.isEmpty()) {
            for (Object beanAliase : beanAliases) {
                String alias = (String) beanAliase;
                beanDefinitionRegistry.registerAlias(beanName, alias);
            }
        }
    }

    private void removeBeanDefinition(BeanDefinitionRegistry registry, String beanName) {
        MetaClass mc = GroovySystem.getMetaClassRegistry().getMetaClass(registry.getClass());
        if(mc.respondsTo(registry, "removeBeanDefinition").size()>0) {
            mc.invokeMethod(registry,"removeBeanDefinition",new Object[]{beanName});
        }
    }

    /**
     * Adds an abstract bean and returns the BeanConfiguration instance
     *
     * @param name The name of the bean
     * @return The BeanConfiguration object
     */
    public BeanConfiguration addAbstractBean(String name) {
        BeanConfiguration bc = new DefaultBeanConfiguration(name);
        bc.setAbstract(true);
        registerBeanConfiguration(name, bc);

        return bc;
    }

    public void addAlias(String alias, String beanName) {
        List beanAliases = this.aliases.get(beanName);
        if(beanAliases == null) {
            beanAliases = new ArrayList();
            this.aliases.put(beanName, beanAliases);
        }
        beanAliases.add(alias);
    }

    public BeanDefinition getBeanDefinition(String beanName) {
        return this.beanDefinitions.get(beanName);
    }

    public void setBeanFactory(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
}
