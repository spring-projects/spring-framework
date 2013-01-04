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

package org.springframework.orm.hibernate3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.transaction.TransactionManager;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.cache.CacheProvider;
import org.hibernate.cache.NoCacheProvider;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.impl.NoCachingRegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.cfg.Mappings;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.connection.UserSuppliedConnectionProvider;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.event.MergeEvent;
import org.hibernate.event.MergeEventListener;
import org.hibernate.mapping.TypeDef;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * @author Juergen Hoeller
 * @since 05.03.2005
 */
public class LocalSessionFactoryBeanTests extends TestCase {

	@SuppressWarnings("serial")
	public void testLocalSessionFactoryBeanWithDataSource() throws Exception {
		final DriverManagerDataSource ds = new DriverManagerDataSource();
		final List invocations = new ArrayList();
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean() {
			@Override
			protected Configuration newConfiguration() {
				return new Configuration() {
					@Override
					public Configuration addInputStream(InputStream is) {
						try {
							is.close();
						}
						catch (IOException ex) {
						}
						invocations.add("addResource");
						return this;
					}
				};
			}
			@Override
			protected SessionFactory newSessionFactory(Configuration config) {
				assertEquals(LocalDataSourceConnectionProvider.class.getName(),
						config.getProperty(Environment.CONNECTION_PROVIDER));
				assertEquals(ds, LocalSessionFactoryBean.getConfigTimeDataSource());
				invocations.add("newSessionFactory");
				return null;
			}
		};
		sfb.setDataSource(ds);
		sfb.afterPropertiesSet();
		assertTrue(sfb.getConfiguration() != null);
		assertEquals("newSessionFactory", invocations.get(0));
	}

	@SuppressWarnings("serial")
	public void testLocalSessionFactoryBeanWithCacheRegionFactory() throws Exception {
		final RegionFactory regionFactory = new NoCachingRegionFactory(null);
		final List invocations = new ArrayList();
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean() {
			@Override
			protected Configuration newConfiguration() {
				return new Configuration() {
					@Override
					public Configuration addInputStream(InputStream is) {
						try {
							is.close();
						}
						catch (IOException ex) {
						}
						invocations.add("addResource");
						return this;
					}
				};
			}
			@Override
			protected SessionFactory newSessionFactory(Configuration config) {
				assertEquals(LocalRegionFactoryProxy.class.getName(),
						config.getProperty(Environment.CACHE_REGION_FACTORY));
				assertSame(regionFactory, LocalSessionFactoryBean.getConfigTimeRegionFactory());
				invocations.add("newSessionFactory");
				return null;
			}
		};
		sfb.setCacheRegionFactory(regionFactory);
		sfb.afterPropertiesSet();
		assertTrue(sfb.getConfiguration() != null);
		assertEquals("newSessionFactory", invocations.get(0));
	}

	@SuppressWarnings("serial")
	public void testLocalSessionFactoryBeanWithCacheProvider() throws Exception {
		final CacheProvider cacheProvider = new NoCacheProvider();
		final List invocations = new ArrayList();
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean() {
			@Override
			protected Configuration newConfiguration() {
				return new Configuration() {
					@Override
					public Configuration addInputStream(InputStream is) {
						try {
							is.close();
						}
						catch (IOException ex) {
						}
						invocations.add("addResource");
						return this;
					}
				};
			}

			@Override
			protected SessionFactory newSessionFactory(Configuration config) {
				assertEquals(LocalCacheProviderProxy.class.getName(),
						config.getProperty(Environment.CACHE_PROVIDER));
				assertSame(cacheProvider, LocalSessionFactoryBean.getConfigTimeCacheProvider());
				invocations.add("newSessionFactory");
				return null;
			}
		};
		sfb.setCacheProvider(cacheProvider);
		sfb.afterPropertiesSet();
		assertTrue(sfb.getConfiguration() != null);
		assertEquals("newSessionFactory", invocations.get(0));
	}

	@SuppressWarnings("serial")
	public void testLocalSessionFactoryBeanWithTransactionAwareDataSource() throws Exception {
		final DriverManagerDataSource ds = new DriverManagerDataSource();
		final List invocations = new ArrayList();
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean() {
			@Override
			protected Configuration newConfiguration() {
				return new Configuration() {
					@Override
					public Configuration addInputStream(InputStream is) {
						try {
							is.close();
						}
						catch (IOException ex) {
						}
						invocations.add("addResource");
						return this;
					}
				};
			}

			@Override
			protected SessionFactory newSessionFactory(Configuration config) {
				assertEquals(TransactionAwareDataSourceConnectionProvider.class.getName(),
						config.getProperty(Environment.CONNECTION_PROVIDER));
				assertEquals(ds, LocalSessionFactoryBean.getConfigTimeDataSource());
				invocations.add("newSessionFactory");
				return null;
			}
		};
		sfb.setDataSource(ds);
		sfb.setUseTransactionAwareDataSource(true);
		sfb.afterPropertiesSet();
		assertTrue(sfb.getConfiguration() != null);
		assertEquals("newSessionFactory", invocations.get(0));
	}

	@SuppressWarnings("serial")
	public void testLocalSessionFactoryBeanWithDataSourceAndMappingResources() throws Exception {
		final DriverManagerDataSource ds = new DriverManagerDataSource();
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		final TransactionManager tm = (TransactionManager) tmControl.getMock();
		final List invocations = new ArrayList();
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean() {
			@Override
			protected Configuration newConfiguration() {
				return new Configuration() {
					@Override
					public Configuration addInputStream(InputStream is) {
						try {
							is.close();
						}
						catch (IOException ex) {
						}
						invocations.add("addResource");
						return this;
					}
				};
			}

			@Override
			protected SessionFactory newSessionFactory(Configuration config) {
				assertEquals(LocalJtaDataSourceConnectionProvider.class.getName(),
						config.getProperty(Environment.CONNECTION_PROVIDER));
				assertEquals(ds, LocalSessionFactoryBean.getConfigTimeDataSource());
				assertEquals(LocalTransactionManagerLookup.class.getName(),
						config.getProperty(Environment.TRANSACTION_MANAGER_STRATEGY));
				assertEquals(tm, LocalSessionFactoryBean.getConfigTimeTransactionManager());
				invocations.add("newSessionFactory");
				return null;
			}
		};
		sfb.setMappingResources(new String[]{
				"/org/springframework/beans/factory/xml/test.xml",
				"/org/springframework/beans/factory/xml/child.xml"});
		sfb.setDataSource(ds);
		sfb.setJtaTransactionManager(tm);
		sfb.afterPropertiesSet();
		assertTrue(sfb.getConfiguration() != null);
		assertEquals("addResource", invocations.get(0));
		assertEquals("addResource", invocations.get(1));
		assertEquals("newSessionFactory", invocations.get(2));
	}

	@SuppressWarnings("serial")
	public void testLocalSessionFactoryBeanWithDataSourceAndMappingJarLocations() throws Exception {
		final DriverManagerDataSource ds = new DriverManagerDataSource();
		final Set invocations = new HashSet();
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean() {
			@Override
			protected Configuration newConfiguration() {
				return new Configuration() {
					@Override
					public Configuration addJar(File file) {
						invocations.add("addResource " + file.getPath());
						return this;
					}
				};
			}

			@Override
			protected SessionFactory newSessionFactory(Configuration config) {
				assertEquals(LocalDataSourceConnectionProvider.class.getName(),
						config.getProperty(Environment.CONNECTION_PROVIDER));
				assertEquals(ds, LocalSessionFactoryBean.getConfigTimeDataSource());
				invocations.add("newSessionFactory");
				return null;
			}
		};
		sfb.setMappingJarLocations(new Resource[]{
				new FileSystemResource("mapping.hbm.jar"), new FileSystemResource("mapping2.hbm.jar")});
		sfb.setDataSource(ds);
		sfb.afterPropertiesSet();
		assertTrue(sfb.getConfiguration() != null);
		assertTrue(invocations.contains("addResource mapping.hbm.jar"));
		assertTrue(invocations.contains("addResource mapping2.hbm.jar"));
		assertTrue(invocations.contains("newSessionFactory"));
	}

	@SuppressWarnings("serial")
	public void testLocalSessionFactoryBeanWithDataSourceAndProperties() throws Exception {
		final DriverManagerDataSource ds = new DriverManagerDataSource();
		final Set invocations = new HashSet();
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean() {
			@Override
			protected Configuration newConfiguration() {
				return new Configuration() {
					@Override
					public Configuration addInputStream(InputStream is) {
						try {
							is.close();
						}
						catch (IOException ex) {
						}
						invocations.add("addResource");
						return this;
					}
				};
			}

			@Override
			protected SessionFactory newSessionFactory(Configuration config) {
				assertEquals(LocalDataSourceConnectionProvider.class.getName(),
						config.getProperty(Environment.CONNECTION_PROVIDER));
				assertEquals(ds, LocalSessionFactoryBean.getConfigTimeDataSource());
				assertEquals("myValue", config.getProperty("myProperty"));
				invocations.add("newSessionFactory");
				return null;
			}
		};
		sfb.setMappingLocations(new Resource[]{
				new ClassPathResource("/org/springframework/beans/factory/xml/test.xml")});
		sfb.setDataSource(ds);
		Properties prop = new Properties();
		prop.setProperty(Environment.CONNECTION_PROVIDER, "myClass");
		prop.setProperty("myProperty", "myValue");
		sfb.setHibernateProperties(prop);
		sfb.afterPropertiesSet();
		assertTrue(sfb.getConfiguration() != null);
		assertTrue(invocations.contains("addResource"));
		assertTrue(invocations.contains("newSessionFactory"));
	}

	public void testLocalSessionFactoryBeanWithValidProperties() throws Exception {
		final Set invocations = new HashSet();
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean() {
			@Override
			protected SessionFactory newSessionFactory(Configuration config) {
				assertEquals(UserSuppliedConnectionProvider.class.getName(),
						config.getProperty(Environment.CONNECTION_PROVIDER));
				assertEquals("myValue", config.getProperty("myProperty"));
				invocations.add("newSessionFactory");
				return null;
			}
		};
		Properties prop = new Properties();
		prop.setProperty(Environment.CONNECTION_PROVIDER, UserSuppliedConnectionProvider.class.getName());
		prop.setProperty("myProperty", "myValue");
		sfb.setHibernateProperties(prop);
		sfb.afterPropertiesSet();
		assertTrue(sfb.getConfiguration() != null);
		assertTrue(invocations.contains("newSessionFactory"));
	}

	public void testLocalSessionFactoryBeanWithInvalidProperties() throws Exception {
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean();
		sfb.setMappingResources(new String[0]);
		Properties prop = new Properties();
		prop.setProperty(Environment.CONNECTION_PROVIDER, "myClass");
		sfb.setHibernateProperties(prop);
		try {
			sfb.afterPropertiesSet();
		}
		catch (HibernateException ex) {
			// expected, provider class not found
		}
	}

	public void testLocalSessionFactoryBeanWithInvalidMappings() throws Exception {
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean();
		sfb.setMappingResources(new String[]{"mapping.hbm.xml"});
		try {
			sfb.afterPropertiesSet();
		}
		catch (IOException ex) {
			// expected, mapping resource not found
		}
	}

	public void testLocalSessionFactoryBeanWithCustomSessionFactory() throws Exception {
		MockControl factoryControl = MockControl.createControl(SessionFactory.class);
		final SessionFactory sessionFactory = (SessionFactory) factoryControl.getMock();
		sessionFactory.close();
		factoryControl.setVoidCallable(1);
		factoryControl.replay();
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean() {
			@Override
			protected SessionFactory newSessionFactory(Configuration config) {
				return sessionFactory;
			}
		};
		sfb.setMappingResources(new String[0]);
		sfb.setDataSource(new DriverManagerDataSource());
		sfb.setExposeTransactionAwareSessionFactory(false);
		sfb.afterPropertiesSet();
		assertTrue(sessionFactory == sfb.getObject());
		sfb.destroy();
		factoryControl.verify();
	}

	@SuppressWarnings("serial")
	public void testLocalSessionFactoryBeanWithEntityInterceptor() throws Exception {
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean() {
			@Override
			protected Configuration newConfiguration() {
				return new Configuration() {
					@Override
					public Configuration setInterceptor(Interceptor interceptor) {
						throw new IllegalArgumentException(interceptor.toString());
					}
				};
			}
		};
		sfb.setMappingResources(new String[0]);
		sfb.setDataSource(new DriverManagerDataSource());
		MockControl interceptorControl = MockControl.createControl(Interceptor.class);
		Interceptor entityInterceptor = (Interceptor) interceptorControl.getMock();
		interceptorControl.replay();
		sfb.setEntityInterceptor(entityInterceptor);
		try {
			sfb.afterPropertiesSet();
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
			assertTrue("Correct exception", ex.getMessage().equals(entityInterceptor.toString()));
		}
	}

	@SuppressWarnings("serial")
	public void testLocalSessionFactoryBeanWithNamingStrategy() throws Exception {
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean() {
			@Override
			protected Configuration newConfiguration() {
				return new Configuration() {
					@Override
					public Configuration setNamingStrategy(NamingStrategy namingStrategy) {
						throw new IllegalArgumentException(namingStrategy.toString());
					}
				};
			}
		};
		sfb.setMappingResources(new String[0]);
		sfb.setDataSource(new DriverManagerDataSource());
		sfb.setNamingStrategy(ImprovedNamingStrategy.INSTANCE);
		try {
			sfb.afterPropertiesSet();
			fail("Should have thrown IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
			assertTrue("Correct exception", ex.getMessage().equals(ImprovedNamingStrategy.INSTANCE.toString()));
		}
	}

	@SuppressWarnings("serial")
	public void testLocalSessionFactoryBeanWithCacheStrategies() throws Exception {
		final Properties registeredClassCache = new Properties();
		final Properties registeredCollectionCache = new Properties();
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean() {
			@Override
			protected Configuration newConfiguration() {
				return new Configuration() {
					@Override
					public Configuration setCacheConcurrencyStrategy(String clazz, String concurrencyStrategy) {
						registeredClassCache.setProperty(clazz, concurrencyStrategy);
						return this;
					}
					@Override
					public Configuration setCollectionCacheConcurrencyStrategy(String collectionRole, String concurrencyStrategy) {
						registeredCollectionCache.setProperty(collectionRole, concurrencyStrategy);
						return this;
					}
				};
			}
			@Override
			protected SessionFactory newSessionFactory(Configuration config) {
				return null;
			}
		};

		sfb.setMappingResources(new String[0]);
		sfb.setDataSource(new DriverManagerDataSource());
		Properties classCache = new Properties();
		classCache.setProperty("org.springframework.tests.sample.beans.TestBean", "read-write");
		sfb.setEntityCacheStrategies(classCache);
		Properties collectionCache = new Properties();
		collectionCache.setProperty("org.springframework.tests.sample.beans.TestBean.friends", "read-only");
		sfb.setCollectionCacheStrategies(collectionCache);
		sfb.afterPropertiesSet();

		assertEquals(classCache, registeredClassCache);
		assertEquals(collectionCache, registeredCollectionCache);
	}

	@SuppressWarnings("serial")
	public void testLocalSessionFactoryBeanWithCacheStrategiesAndRegions() throws Exception {
		final Properties registeredClassCache = new Properties();
		final Properties registeredCollectionCache = new Properties();
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean() {
			@Override
			protected Configuration newConfiguration() {
				return new Configuration() {
					// changed from return type 'void' to 'Configuration' in Hibernate 3.6
					@Override
					public void setCacheConcurrencyStrategy(String clazz, String concurrencyStrategy, String regionName) {
						registeredClassCache.setProperty(clazz, concurrencyStrategy + "," + regionName);
					}
					@Override
					public void setCollectionCacheConcurrencyStrategy(String collectionRole, String concurrencyStrategy, String regionName) {
						registeredCollectionCache.setProperty(collectionRole, concurrencyStrategy + "," + regionName);
					}
				};
			}
			@Override
			protected SessionFactory newSessionFactory(Configuration config) {
				return null;
			}
		};

		sfb.setMappingResources(new String[0]);
		sfb.setDataSource(new DriverManagerDataSource());
		Properties classCache = new Properties();
		classCache.setProperty("org.springframework.tests.sample.beans.TestBean", "read-write,myRegion");
		sfb.setEntityCacheStrategies(classCache);
		Properties collectionCache = new Properties();
		collectionCache.setProperty("org.springframework.tests.sample.beans.TestBean.friends", "read-only,myRegion");
		sfb.setCollectionCacheStrategies(collectionCache);
		sfb.afterPropertiesSet();

		assertEquals(classCache, registeredClassCache);
		assertEquals(collectionCache, registeredCollectionCache);
	}

	@SuppressWarnings("serial")
	public void testLocalSessionFactoryBeanWithEventListeners() throws Exception {
		final Map registeredListeners = new HashMap();
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean() {
			@Override
			protected Configuration newConfiguration() {
				return new Configuration() {
					@Override
					public void setListener(String type, Object listener) {
						registeredListeners.put(type, listener);
					}
				};
			}
			@Override
			protected SessionFactory newSessionFactory(Configuration config) {
				return null;
			}
		};
		sfb.setMappingResources(new String[0]);
		sfb.setDataSource(new DriverManagerDataSource());
		Map listeners = new HashMap();
		listeners.put("flush", "myListener");
		listeners.put("create", "yourListener");
		sfb.setEventListeners(listeners);
		sfb.afterPropertiesSet();
		assertEquals(listeners, registeredListeners);
	}

	@SuppressWarnings("serial")
	public void testLocalSessionFactoryBeanWithEventListenerSet() throws Exception {
		final Map registeredListeners = new HashMap();
		LocalSessionFactoryBean sfb = new LocalSessionFactoryBean() {
			@Override
			protected Configuration newConfiguration() {
				return new Configuration() {
					@Override
					public void setListeners(String type, Object[] listeners) {
						assertTrue(listeners instanceof MergeEventListener[]);
						registeredListeners.put(type, new HashSet(Arrays.asList(listeners)));
					}
				};
			}
			@Override
			protected SessionFactory newSessionFactory(Configuration config) {
				return null;
			}
		};
		sfb.setMappingResources(new String[0]);
		sfb.setDataSource(new DriverManagerDataSource());
		Map listeners = new HashMap();
		Set mergeSet = new HashSet();
		mergeSet.add(new DummyMergeEventListener());
		mergeSet.add(new DummyMergeEventListener());
		listeners.put("merge", mergeSet);
		sfb.setEventListeners(listeners);
		sfb.afterPropertiesSet();
		assertEquals(listeners, registeredListeners);
	}

	/*
	public void testLocalSessionFactoryBeanWithFilterDefinitions() throws Exception {
		XmlBeanFactory xbf = new XmlBeanFactory(new ClassPathResource("filterDefinitions.xml", getClass()));
		FilterTestLocalSessionFactoryBean sf = (FilterTestLocalSessionFactoryBean) xbf.getBean("&sessionFactory");
		assertEquals(2, sf.registeredFilterDefinitions.size());
		FilterDefinition filter1 = (FilterDefinition) sf.registeredFilterDefinitions.get(0);
		FilterDefinition filter2 = (FilterDefinition) sf.registeredFilterDefinitions.get(1);

		assertEquals("filter1", filter1.getFilterName());
		assertEquals(2, filter1.getParameterNames().size());
		assertEquals(Hibernate.STRING, filter1.getParameterType("param1"));
		assertEquals(Hibernate.LONG, filter1.getParameterType("otherParam"));
		assertEquals("someCondition", filter1.getDefaultFilterCondition());

		assertEquals("filter2", filter2.getFilterName());
		assertEquals(1, filter2.getParameterNames().size());
		assertEquals(Hibernate.INTEGER, filter2.getParameterType("myParam"));
	}
	*/

	public void testLocalSessionFactoryBeanWithTypeDefinitions() throws Exception {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(new ClassPathResource("typeDefinitions.xml", getClass()));
		TypeTestLocalSessionFactoryBean sf = (TypeTestLocalSessionFactoryBean) xbf.getBean("&sessionFactory");
		// Requires re-compilation when switching to Hibernate 3.5/3.6
		// since Mappings changed from a class to an interface
		TypeDef type1 = sf.mappings.getTypeDef("type1");
		TypeDef type2 = sf.mappings.getTypeDef("type2");

		assertEquals("mypackage.MyTypeClass", type1.getTypeClass());
		assertEquals(2, type1.getParameters().size());
		assertEquals("value1", type1.getParameters().getProperty("param1"));
		assertEquals("othervalue", type1.getParameters().getProperty("otherParam"));

		assertEquals("mypackage.MyOtherTypeClass", type2.getTypeClass());
		assertEquals(1, type2.getParameters().size());
		assertEquals("myvalue", type2.getParameters().getProperty("myParam"));
	}


	@SuppressWarnings("serial")
	public static class FilterTestLocalSessionFactoryBean extends LocalSessionFactoryBean {

		public List registeredFilterDefinitions = new LinkedList();

		@Override
		protected Configuration newConfiguration() throws HibernateException {
			return new Configuration() {
				@Override
				public void addFilterDefinition(FilterDefinition definition) {
					registeredFilterDefinitions.add(definition);
				}
			};
		}

		@Override
		protected SessionFactory newSessionFactory(Configuration config) {
			return null;
		}
	}


	public static class TypeTestLocalSessionFactoryBean extends LocalSessionFactoryBean {

		public Mappings mappings;

		@Override
		protected SessionFactory newSessionFactory(Configuration config) {
			this.mappings = config.createMappings();
			return null;
		}
	}


	@SuppressWarnings("serial")
	public static class DummyMergeEventListener implements MergeEventListener {

		@Override
		public void onMerge(MergeEvent event) throws HibernateException {
		}

		@Override
		public void onMerge(MergeEvent event, Map copiedAlready) throws HibernateException {
		}
	}

}
