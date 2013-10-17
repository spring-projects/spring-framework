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

package org.springframework.beans.factory.groovy;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.GroovyShell;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.ResourcePatternUtils;

/**
 * A Groovy-based reader for Spring bean definitions: Like a Groovy builder,
 * but more of a DSL for Spring configuration. Allows syntax like:
 *
 * <pre>import org.hibernate.SessionFactory
 * import org.apache.commons.dbcp.BasicDataSource
 *
 * def reader = new GroovyBeanDefinitionReader(myApplicationContext)
 * reader.beans {
 *     dataSource(BasicDataSource) {                  // <--- invokeMethod
 *         driverClassName = "org.hsqldb.jdbcDriver"
 *         url = "jdbc:hsqldb:mem:grailsDB"
 *         username = "sa"                            // <-- setProperty
 *         password = ""
 *         settings = [mynew:"setting"]
 *     }
 *     sessionFactory(SessionFactory) {
 *         dataSource = dataSource                    // <-- getProperty for retrieving references
 *     }
 *     myService(MyService) {
 *         nestedBean = { AnotherBean bean ->         // <-- setProperty with closure for nested bean
 *             dataSource = dataSource
 *         }
 *     }
 * }</pre>
 *
 * <p>You can also load resources containing beans defined as a Groovy script using
 * either the {@link #loadBeanDefinitions(org.springframework.core.io.Resource...)}
 * or {@link #loadBeanDefinitions(String...)} method, with a script looking as follows:
 *
 * <pre>import org.hibernate.SessionFactory
 * import org.apache.commons.dbcp.BasicDataSource
 *
 * beans {
 *     dataSource(BasicDataSource) {
 *         driverClassName = "org.hsqldb.jdbcDriver"
 *         url = "jdbc:hsqldb:mem:grailsDB"
 *         username = "sa"
 *         password = ""
 *         settings = [mynew:"setting"]
 *     }
 *     sessionFactory(SessionFactory) {
 *         dataSource = dataSource
 *     }
 *     myService(MyService) {
 *         nestedBean = { AnotherBean bean ->
 *             dataSource = dataSource
 *         }
 *     }
 * }</pre>
 *
 * <p>Typically applied to a
 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory}
 * or a {@link org.springframework.context.support.GenericApplicationContext},
 * but can be used against any {@link BeanDefinitionRegistry} implementation.
 *
 * @author Jeff Brown
 * @author Graeme Rocher
 * @author Juergen Hoeller
 * @since 4.0
 * @see BeanDefinitionRegistry
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 * @see org.springframework.context.support.GenericGroovyApplicationContext
 */
public class GroovyBeanDefinitionReader extends AbstractBeanDefinitionReader implements GroovyObject {

	private final XmlBeanDefinitionReader xmlBeanDefinitionReader;

	private MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(getClass());

	private Binding binding;

	private GroovyBeanDefinitionWrapper currentBeanDefinition;

	private final Map <String, String> namespaces = new HashMap<String, String>();

    private final Map<String, DeferredProperty> deferredProperties = new HashMap<String, DeferredProperty>();


	/**
	 * Create new GroovyBeanDefinitionReader for the given bean factory.
	 * @param registry the BeanFactory to load bean definitions into,
	 * in the form of a BeanDefinitionRegistry
	 */
    public GroovyBeanDefinitionReader(BeanDefinitionRegistry registry) {
        super(registry);
        this.xmlBeanDefinitionReader = new XmlBeanDefinitionReader(registry);
		this.xmlBeanDefinitionReader.setValidating(false);
    }

	/**
	 * Create new GroovyBeanDefinitionReader based on the given XmlBeanDefinitionReader,
	 * using the same registry and delegating XML loading to it.
	 * @param xmlBeanDefinitionReader the XmlBeanDefinitionReader to derive the registry
	 * from and to delegate XML loading to
	 */
	public GroovyBeanDefinitionReader(XmlBeanDefinitionReader xmlBeanDefinitionReader) {
		super(xmlBeanDefinitionReader.getRegistry());
		this.xmlBeanDefinitionReader = xmlBeanDefinitionReader;
	}


	public void setMetaClass(MetaClass metaClass) {
		this.metaClass = metaClass;
	}

	public MetaClass getMetaClass() {
		return this.metaClass;
	}

	/**
	 * Set the binding, i.e. the Groovy variables available in the scope
	 * of a GroovyBeanDefinitionReader closure.
	 */
	public void setBinding(Binding binding) {
		this.binding = binding;
	}

	/**
	 * Return a specified binding for Groovy variables, if any.
	 */
	public Binding getBinding() {
		return this.binding;
	}


	// TRADITIONAL BEAN DEFINITION READER METHODS 

	/**
	 * Load bean definitions from the specified Groovy script.
	 * @param resource the resource descriptor for the Groovy script
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource));
	}

	/**
	 * Load bean definitions from the specified Groovy script.
	 * @param encodedResource the resource descriptor for the Groovy script,
	 * allowing to specify an encoding to use for parsing the file
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
		Closure beans = new Closure(this){
			public Object call(Object[] args) {
				invokeBeanDefiningClosure((Closure) args[0]);
				return null;
			}
		};
		Binding binding = new Binding() {
			@Override
			public void setVariable(String name, Object value) {
				if (currentBeanDefinition !=null) {
					applyPropertyToBeanDefinition(name, value);
				}
				else {
					super.setVariable(name, value);
				}
			}
		};
		binding.setVariable("beans", beans);


		int countBefore = getRegistry().getBeanDefinitionCount();
		try {
			GroovyShell shell = new GroovyShell(getResourceLoader().getClassLoader(), binding);
			shell.evaluate(encodedResource.getReader(), encodedResource.getResource().getFilename());
		}
		catch (Throwable ex) {
			throw new BeanDefinitionParsingException(new Problem("Error evaluating Groovy script: " + ex.getMessage(),
					new Location(encodedResource.getResource()), null, ex));
		}
		return getRegistry().getBeanDefinitionCount() - countBefore;
	}


	// METHODS FOR CONSUMPTION IN A GROOVY CLOSURE

	/**
	 * Defines a set of beans for the given block or closure.
	 * @param c the block or closure
	 * @return this GroovyBeanDefinitionReader instance
	 */
	public GroovyBeanDefinitionReader beans(Closure c) {
		return invokeBeanDefiningClosure(c);
	}

	/**
	 * Defines an inner bean definition.
	 * @param type the bean type
	 * @return the bean definition
	 */
	public GenericBeanDefinition bean(Class<?> type) {
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(type);
		return beanDefinition;
	}

	/**
	 * Defines an inner bean definition
	 * @param type The bean type
	 * @param args The constructors arguments and closure configurer
	 * @return The bean definition
	 */
	public AbstractBeanDefinition bean(Class<?> type, Object...args) {
		GroovyBeanDefinitionWrapper current = this.currentBeanDefinition;
		try {
			Closure callable = null;
			Collection constructorArgs = null;
			if (args != null && args.length > 0) {
				int index = args.length;
				Object lastArg = args[index-1];

				if (lastArg instanceof Closure) {
					callable = (Closure) lastArg;
					index--;
				}
				if (index > -1) {
					constructorArgs = resolveConstructorArguments(args, 0, index);
				}
			}
			this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(null, type, constructorArgs);
			if (callable != null) {
				callable.call(this.currentBeanDefinition);
			}
			return this.currentBeanDefinition.getBeanDefinition();

		}
		finally {
			this.currentBeanDefinition = current;
		}
	}

	/**
     * Defines an Spring namespace definition to use.
     * @param definition the namespace definition
     */
    public void xmlns(Map<String, String> definition) {
        if (!definition.isEmpty()) {
            for (Map.Entry<String,String> entry : definition.entrySet()) {
                String namespace = entry.getKey();
                String uri = entry.getValue();
                if (uri == null) {
                    throw new IllegalArgumentException("Namespace definition must supply a non-null URI");
                }
                NamespaceHandler namespaceHandler = this.xmlBeanDefinitionReader.getNamespaceHandlerResolver().resolve(uri);
                if (namespaceHandler == null) {
                    throw new BeanDefinitionParsingException(new Problem("No namespace handler found for URI: " + uri,
							new Location(new DescriptiveResource(("Groovy")))));
                }
                this.namespaces.put(namespace, uri);
            }
        }
    }

	/**
	 * Imports Spring bean definitions from either XML or Groovy sources into the current bean builder instance.
	 * @param resourcePattern the resource pattern
	 */
	public void importBeans(String resourcePattern) throws IOException {
		Resource[] resources =
				ResourcePatternUtils.getResourcePatternResolver(getResourceLoader()).getResources(resourcePattern);
		for (Resource resource : resources) {
			String filename = resource.getFilename();
			if (filename.endsWith(".groovy")) {
				loadBeanDefinitions(resource);
			}
			else if (filename.endsWith(".xml")) {
				this.xmlBeanDefinitionReader.loadBeanDefinitions(resource);
			}
		}
	}


	// INTERNAL HANDLING OF GROOVY CLOSURES AND PROPERTIES

    /**
	 * This method overrides method invocation to create beans for each method name that
	 * takes a class argument
	 */
	public Object invokeMethod(String name, Object arg) {
        Object[] args = (Object[])arg;
        if ("beans".equals(name) && args.length == 1 && args[0] instanceof Closure) {
            return beans((Closure) args[0]);
        }
		else if ("ref".equals(name)) {
			String refName;
			if (args[0] == null)
				throw new IllegalArgumentException("Argument to ref() is not a valid bean or was not found");
			
			if (args[0] instanceof RuntimeBeanReference) {
				refName = ((RuntimeBeanReference)args[0]).getBeanName();
			}
			else {
				refName = args[0].toString();	
			}
			boolean parentRef = false;
			if (args.length > 1) {
				if (args[1] instanceof Boolean) {
					parentRef = (Boolean) args[1];
				}
			}
			return new RuntimeBeanReference(refName, parentRef);
		}
        else if (this.namespaces.containsKey(name) && args.length > 0 && (args[0] instanceof Closure)) {
            GroovyDynamicElementReader reader = createDynamicElementReader(name);
            reader.invokeMethod("doCall", args);
        }
		else if (args.length > 0 && args[0] instanceof Closure) {
            // abstract bean definition
            return invokeBeanDefiningMethod(name, args);
		}
		else if (args.length > 0 && (args[0] instanceof Class || args[0] instanceof RuntimeBeanReference || args[0] instanceof Map)) {
			return invokeBeanDefiningMethod(name, args);			
		}
		else if (args.length > 1 && args[args.length -1] instanceof Closure) {
			return invokeBeanDefiningMethod(name, args);
		}
        MetaClass mc = DefaultGroovyMethods.getMetaClass(getRegistry());
        if (!mc.respondsTo(getRegistry(), name, args).isEmpty()){
            return mc.invokeMethod(getRegistry(), name, args);
        }
        return this;
	}
	
	private boolean addDeferredProperty(String property, Object newValue) {
		if (newValue instanceof List) {
			this.deferredProperties.put(this.currentBeanDefinition.getBeanName() + '.' + property,
					new DeferredProperty(this.currentBeanDefinition, property, newValue));
			return true;
		}
		else if (newValue instanceof Map) {
			this.deferredProperties.put(this.currentBeanDefinition.getBeanName() + '.' + property,
					new DeferredProperty(this.currentBeanDefinition, property, newValue));
			return true;
		}
		return false;
	}

	private void finalizeDeferredProperties() {
		for (DeferredProperty dp : this.deferredProperties.values()) {
			if (dp.value instanceof List) {
				dp.value = manageListIfNecessary((List) dp.value);
			}
			else if (dp.value instanceof Map) {
				dp.value = manageMapIfNecessary((Map) dp.value);
			}
			dp.apply();
		}
		deferredProperties.clear();
	}

	/**
	 * When an methods argument is only a closure it is a set of bean definitions.
	 * @param callable the closure argument
	 * @return this GroovyBeanDefinitionReader instance
	 */
	protected GroovyBeanDefinitionReader invokeBeanDefiningClosure(Closure callable) {
		callable.setDelegate(this);
		callable.call();
		finalizeDeferredProperties();
		return this;
	}

	/**
	 * This method is called when a bean definition node is called.
	 * @param beanName the name of the bean to define
	 * @param args the arguments to the bean. The first argument is the class name, the last
	 * argument is sometimes a closure. All the arguments in between are constructor arguments.
	 * @return the bean definition wrapper
	 */
	private GroovyBeanDefinitionWrapper invokeBeanDefiningMethod(String beanName, Object[] args) {
        boolean hasClosureArgument = args[args.length - 1] instanceof Closure;
        if (args[0] instanceof Class) {
            Class beanClass = (args[0] instanceof Class ? (Class) args[0] : args[0].getClass());
            if (args.length >= 1) {
                if (hasClosureArgument) {
					if (args.length-1 != 1) {
                        this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(
								beanName, beanClass, resolveConstructorArguments(args,1,args.length-1));
					}
					else {
						this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, beanClass);
					}				
				}
				else  {
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(
							beanName, beanClass, resolveConstructorArguments(args,1,args.length));
				}

			}			
		}
		else if (args[0] instanceof RuntimeBeanReference) {
			this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
			this.currentBeanDefinition.getBeanDefinition().setFactoryBeanName(((RuntimeBeanReference) args[0]).getBeanName());
		}
		else if (args[0] instanceof Map) {
            // named constructor arguments
            if (args.length > 1 && args[1] instanceof Class) {
                List constructorArgs = resolveConstructorArguments(args, 2, hasClosureArgument ? args.length-1 :  args.length);
                this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, (Class)args[1], constructorArgs);
                Map namedArgs = (Map)args[0];
                for (Object o : namedArgs.keySet()) {
                    String propName = (String) o;
                    setProperty(propName, namedArgs.get(propName));
                }
            }
            // factory method syntax
            else {
				this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
                //First arg is the map containing factoryBean : factoryMethod
                Map.Entry factoryBeanEntry = (Map.Entry) ((Map) args[0]).entrySet().iterator().next();
                // If we have a closure body, that will be the last argument.
                // In between are the constructor args
                int constructorArgsTest = hasClosureArgument?2:1;
                // If we have more than this number of args, we have constructor args
                if (args.length > constructorArgsTest){
                    // factory-method requires args
                    int endOfConstructArgs = (hasClosureArgument? args.length - 1 : args.length);
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, null,
							resolveConstructorArguments(args, 1, endOfConstructArgs));
                }
				else {
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
                }
				this.currentBeanDefinition.getBeanDefinition().setFactoryBeanName(factoryBeanEntry.getKey().toString());
				this.currentBeanDefinition.getBeanDefinition().setFactoryMethodName(factoryBeanEntry.getValue().toString());
            }

        }
        else if (args[0] instanceof Closure) {
			this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName);
			this.currentBeanDefinition.getBeanDefinition().setAbstract(true);
        }
        else {
            List constructorArgs = resolveConstructorArguments(args, 0, hasClosureArgument ? args.length-1 : args.length);
            currentBeanDefinition = new GroovyBeanDefinitionWrapper(beanName, null, constructorArgs);
        }

        if (hasClosureArgument) {
			Closure callable = (Closure)args[args.length-1];
			callable.setDelegate(this);
            callable.setResolveStrategy(Closure.DELEGATE_FIRST);
            callable.call(new Object[]{currentBeanDefinition});
		}

        GroovyBeanDefinitionWrapper beanDefinition = currentBeanDefinition;
		this.currentBeanDefinition = null;
		beanDefinition.getBeanDefinition().setAttribute(GroovyBeanDefinitionWrapper.class.getName(), beanDefinition);
		getRegistry().registerBeanDefinition(beanName, beanDefinition.getBeanDefinition());
		return beanDefinition;
	}

    protected List<Object> resolveConstructorArguments(Object[] args, int start, int end) {
        Object[] constructorArgs = Arrays.copyOfRange(args, start, end);
        for (int i = 0; i < constructorArgs.length; i++) {
			if (constructorArgs[i] instanceof GString) {
				constructorArgs[i] = constructorArgs[i].toString();
			}
        	else if (constructorArgs[i] instanceof List) {
        		constructorArgs[i] = manageListIfNecessary((List) constructorArgs[i]);
        	}
			else if (constructorArgs[i] instanceof Map){
        		constructorArgs[i] = manageMapIfNecessary((Map) constructorArgs[i]);
        	}
        }
        return Arrays.asList(constructorArgs);
    }

	/**
	 * Checks whether there are any {@link RuntimeBeanReference} inside the Map
	 * and converts it to a ManagedMap if necessary.
	 * @param map the original Map
	 * @return either the original map or a managed copy of it
	 */
	private Object manageMapIfNecessary(Map<?, ?> map) {
		boolean containsRuntimeRefs = false;
		for (Object element : map.values()) {
			if (element instanceof RuntimeBeanReference) {
				containsRuntimeRefs = true;
				break;
			}
		}
		if (containsRuntimeRefs) {
			Map<Object, Object> managedMap = new ManagedMap<Object, Object>();
			managedMap.putAll(map);
			return managedMap;
		}
		return map;
	}

	/**
	 * Checks whether there are any {@link RuntimeBeanReference} inside the Lis
	 * and  converts it to a ManagedList if necessary.
	 * @param list the original List
	 * @return either the original list or a managed copy of it
	 */
	private Object manageListIfNecessary(List<?> list) {
		boolean containsRuntimeRefs = false;
		for (Object element : list) {
			if (element instanceof RuntimeBeanReference) {
				containsRuntimeRefs = true;
				break;
			}
		}
		if (containsRuntimeRefs) {
			List<Object> managedList = new ManagedList<Object>();
			managedList.addAll(list);
			return managedList;
		}
		return list;
	}

	/**
	 * This method overrides property setting in the scope of the GroovyBeanDefinitionReader
	 * to set properties on the current bean definition.
	 */
	public void setProperty(String name, Object value) {
		if (this.currentBeanDefinition != null) {
            applyPropertyToBeanDefinition(name, value);
		}		
	}

    protected void applyPropertyToBeanDefinition(String name, Object value) {
        if (value instanceof GString) {
			value = value.toString();
		}
        if (addDeferredProperty(name, value)) {
            return;
        }
        else if (value instanceof Closure) {
            GroovyBeanDefinitionWrapper current = this.currentBeanDefinition;
            try {
                Closure callable = (Closure) value;
                Class parameterType = callable.getParameterTypes()[0];
                if (parameterType.equals(Object.class)) {
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper("");
                    callable.call(this.currentBeanDefinition);
                }
                else {
					this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(null, parameterType);
                    callable.call((Object) null);
                }

                value = this.currentBeanDefinition.getBeanDefinition();
            }
            finally {
				this.currentBeanDefinition = current;
            }
        }
		this.currentBeanDefinition.addProperty(name, value);
    }

	/**
	 * This method overrides property retrieval in the scope of the GroovyBeanDefinitionReader to either:
	 * a) Retrieve a variable from the bean builder's binding if it exists
	 * b) Retrieve a RuntimeBeanReference for a specific bean if it exists
	 * c) Otherwise just delegate to MetaClass.getProperty which will resolve properties from the GroovyBeanDefinitionReader itself
	 */
	public Object getProperty(String name) {
		Binding binding = getBinding();
		if (binding != null && binding.hasVariable(name)) {
			return binding.getVariable(name);
		}
		else {
            if (this.namespaces.containsKey(name)) {
                return createDynamicElementReader(name);
            }
			if (getRegistry().containsBeanDefinition(name)) {
				GroovyBeanDefinitionWrapper beanDefinition = (GroovyBeanDefinitionWrapper)
						getRegistry().getBeanDefinition(name).getAttribute(GroovyBeanDefinitionWrapper.class.getName());
				if (beanDefinition != null) {
					return new GroovyRuntimeBeanReference(name, beanDefinition, false);
				}					
				else {
					return new RuntimeBeanReference(name, false);
				}
			}
			// This is to deal with the case where the property setter is the last
			// statement in a closure (hence the return value)
			else if (this.currentBeanDefinition != null) {
				MutablePropertyValues pvs = this.currentBeanDefinition.getBeanDefinition().getPropertyValues();
				if (pvs.contains(name)) {
					return pvs.get(name);
				}
				else {
					DeferredProperty dp = this.deferredProperties.get(this.currentBeanDefinition.getBeanName() + name);
					if (dp != null) {
						return dp.value;
					}
					else {
						return getMetaClass().getProperty(this, name);
					}
				}
			}
			else {
				return getMetaClass().getProperty(this, name);
			}			
		}			
	}

    private GroovyDynamicElementReader createDynamicElementReader(String namespace) {
		XmlReaderContext readerContext = this.xmlBeanDefinitionReader.createReaderContext(new DescriptiveResource("Groovy"));
		BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext, getEnvironment());
		boolean decorating = (this.currentBeanDefinition != null);
		if (!decorating) {
			this.currentBeanDefinition = new GroovyBeanDefinitionWrapper(namespace);
		}
        return new GroovyDynamicElementReader(namespace, this.namespaces, delegate, this.currentBeanDefinition, decorating) {
            @Override
            protected void afterInvocation() {
                if (!this.decorating) {
                    currentBeanDefinition = null;
                }
            }
        };
    }


	/**
	 * This class is used to defer the adding of a property to a bean definition until later
	 * This is for a case where you assign a property to a list that may not contain bean references at
	 * that point of assignment, but may later hence it would need to be managed
	 */
	private static class DeferredProperty {

		private final GroovyBeanDefinitionWrapper beanDefinition;

		private final String name;

		private Object value;

		public DeferredProperty(GroovyBeanDefinitionWrapper beanDefinition, String name, Object value) {
			this.beanDefinition = beanDefinition;
			this.name = name;
			this.value = value;
		}

		public void apply() {
			this.beanDefinition.addProperty(this.name, this.value);
		}
	}


	/**
	 * A RuntimeBeanReference that takes care of adding new properties to runtime references
	 */
	private class GroovyRuntimeBeanReference extends RuntimeBeanReference implements GroovyObject {

		private final GroovyBeanDefinitionWrapper beanDefinition;

		private MetaClass metaClass;

		public GroovyRuntimeBeanReference(String beanName, GroovyBeanDefinitionWrapper beanDefinition, boolean toParent) {
			super(beanName, toParent);
			this.beanDefinition = beanDefinition;
			this.metaClass = InvokerHelper.getMetaClass(this);
		}

		public MetaClass getMetaClass() {
			return this.metaClass;
		}

		public Object getProperty(String property) {
			if (property.equals("beanName")) {
				return getBeanName();
			}
			else if (property.equals("source")) {
				return getSource();
			}
			else if (this.beanDefinition != null) {
				return new GroovyPropertyValue(
						property, this.beanDefinition.getBeanDefinition().getPropertyValues().get(property));
			}
			else {
				return this.metaClass.getProperty(this, property);
			}
		}

		public Object invokeMethod(String name, Object args) {
			return this.metaClass.invokeMethod(this, name, args);
		}

		public void setMetaClass(MetaClass metaClass) {
			this.metaClass = metaClass;
		}

		public void setProperty(String property, Object newValue) {
			if (!addDeferredProperty(property, newValue)) {
				this.beanDefinition.getBeanDefinition().getPropertyValues().add(property, newValue);
			}
		}


		/**
		 * Wraps a bean definition property an ensures that any RuntimeBeanReference
		 * additions to it are deferred for resolution later.
		 */
		private class GroovyPropertyValue extends GroovyObjectSupport {

			private final String propertyName;

			private final Object propertyValue;

			public GroovyPropertyValue(String propertyName, Object propertyValue) {
				this.propertyName = propertyName;
				this.propertyValue = propertyValue;
			}

			public void leftShift(Object value) {
				InvokerHelper.invokeMethod(this.propertyValue, "leftShift", value);
				updateDeferredProperties(value);
			}

			public boolean add(Object value) {
				boolean retVal = (Boolean) InvokerHelper.invokeMethod(this.propertyValue, "add", value);
				updateDeferredProperties(value);
				return retVal;
			}

			public boolean addAll(Collection values) {
				boolean retVal = (Boolean) InvokerHelper.invokeMethod(this.propertyValue, "addAll", values);
				for (Object value : values) {
					updateDeferredProperties(value);
				}
				return retVal;
			}

			public Object invokeMethod(String name, Object args) {
				return InvokerHelper.invokeMethod(this.propertyValue, name, args);
			}

			public Object getProperty(String name) {
				return InvokerHelper.getProperty(this.propertyValue, name);
			}

			public void setProperty(String name, Object value) {
				InvokerHelper.setProperty(this.propertyValue, name, value);
			}

			private void updateDeferredProperties(Object value) {
				if (value instanceof RuntimeBeanReference) {
					deferredProperties.put(beanDefinition.getBeanName(),
							new DeferredProperty(beanDefinition, this.propertyName, this.propertyValue));
				}
			}
		}
	}

}
