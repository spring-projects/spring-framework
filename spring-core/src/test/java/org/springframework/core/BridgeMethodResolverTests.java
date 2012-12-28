/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.util.ReflectionUtils;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class BridgeMethodResolverTests {

	private static TypeVariable<?> findTypeVariable(Class<?> clazz, String name) {
		TypeVariable<?>[] variables = clazz.getTypeParameters();
		for (TypeVariable<?> variable : variables) {
			if (variable.getName().equals(name)) {
				return variable;
			}
		}
		return null;
	}

	private static Method findMethodWithReturnType(String name, Class<?> returnType, Class<SettingsDaoImpl> targetType) {
		Method[] methods = targetType.getMethods();
		for (Method m : methods) {
			if (m.getName().equals(name) && m.getReturnType().equals(returnType)) {
				return m;
			}
		}
		return null;
	}


	@Test
	public void testFindBridgedMethod() throws Exception {
		Method unbridged = MyFoo.class.getDeclaredMethod("someMethod", String.class, Object.class);
		Method bridged = MyFoo.class.getDeclaredMethod("someMethod", Serializable.class, Object.class);
		assertFalse(unbridged.isBridge());
		assertTrue(bridged.isBridge());

		assertEquals("Unbridged method not returned directly", unbridged, BridgeMethodResolver.findBridgedMethod(unbridged));
		assertEquals("Incorrect bridged method returned", unbridged, BridgeMethodResolver.findBridgedMethod(bridged));
	}

	@Test
	public void testFindBridgedVarargMethod() throws Exception {
		Method unbridged = MyFoo.class.getDeclaredMethod("someVarargMethod", String.class, Object[].class);
		Method bridged = MyFoo.class.getDeclaredMethod("someVarargMethod", Serializable.class, Object[].class);
		assertFalse(unbridged.isBridge());
		assertTrue(bridged.isBridge());

		assertEquals("Unbridged method not returned directly", unbridged, BridgeMethodResolver.findBridgedMethod(unbridged));
		assertEquals("Incorrect bridged method returned", unbridged, BridgeMethodResolver.findBridgedMethod(bridged));
	}

	@Test
	public void testFindBridgedMethodInHierarchy() throws Exception {
		Method bridgeMethod = DateAdder.class.getMethod("add", Object.class);
		assertTrue(bridgeMethod.isBridge());
		Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(bridgeMethod);
		assertFalse(bridgedMethod.isBridge());
		assertEquals("add", bridgedMethod.getName());
		assertEquals(1, bridgedMethod.getParameterTypes().length);
		assertEquals(Date.class, bridgedMethod.getParameterTypes()[0]);
	}

	@Test
	public void testIsBridgeMethodFor() throws Exception {
		Map<TypeVariable, Type> typeParameterMap = GenericTypeResolver.getTypeVariableMap(MyBar.class);
		Method bridged = MyBar.class.getDeclaredMethod("someMethod", String.class, Object.class);
		Method other = MyBar.class.getDeclaredMethod("someMethod", Integer.class, Object.class);
		Method bridge = MyBar.class.getDeclaredMethod("someMethod", Object.class, Object.class);

		assertTrue("Should be bridge method", BridgeMethodResolver.isBridgeMethodFor(bridge, bridged, typeParameterMap));
		assertFalse("Should not be bridge method", BridgeMethodResolver.isBridgeMethodFor(bridge, other, typeParameterMap));
	}

	@Test
	public void testCreateTypeVariableMap() throws Exception {
		Map<TypeVariable, Type> typeVariableMap = GenericTypeResolver.getTypeVariableMap(MyBar.class);
		TypeVariable<?> barT = findTypeVariable(InterBar.class, "T");
		assertEquals(String.class, typeVariableMap.get(barT));

		typeVariableMap = GenericTypeResolver.getTypeVariableMap(MyFoo.class);
		TypeVariable<?> fooT = findTypeVariable(Foo.class, "T");
		assertEquals(String.class, typeVariableMap.get(fooT));

		typeVariableMap = GenericTypeResolver.getTypeVariableMap(ExtendsEnclosing.ExtendsEnclosed.ExtendsReallyDeepNow.class);
		TypeVariable<?> r = findTypeVariable(Enclosing.Enclosed.ReallyDeepNow.class, "R");
		TypeVariable<?> s = findTypeVariable(Enclosing.Enclosed.class, "S");
		TypeVariable<?> t = findTypeVariable(Enclosing.class, "T");
		assertEquals(Long.class, typeVariableMap.get(r));
		assertEquals(Integer.class, typeVariableMap.get(s));
		assertEquals(String.class, typeVariableMap.get(t));
	}

	@Test
	public void testDoubleParameterization() throws Exception {
		Method objectBridge = MyBoo.class.getDeclaredMethod("foo", Object.class);
		Method serializableBridge = MyBoo.class.getDeclaredMethod("foo", Serializable.class);

		Method stringFoo = MyBoo.class.getDeclaredMethod("foo", String.class);
		Method integerFoo = MyBoo.class.getDeclaredMethod("foo", Integer.class);

		assertEquals("foo(String) not resolved.", stringFoo, BridgeMethodResolver.findBridgedMethod(objectBridge));
		assertEquals("foo(Integer) not resolved.", integerFoo, BridgeMethodResolver.findBridgedMethod(serializableBridge));
	}

	@Test
	public void testFindBridgedMethodFromMultipleBridges() throws Exception {
		Method loadWithObjectReturn = findMethodWithReturnType("load", Object.class, SettingsDaoImpl.class);
		assertNotNull(loadWithObjectReturn);

		Method loadWithSettingsReturn = findMethodWithReturnType("load", Settings.class, SettingsDaoImpl.class);
		assertNotNull(loadWithSettingsReturn);
		assertNotSame(loadWithObjectReturn, loadWithSettingsReturn);

		Method method = SettingsDaoImpl.class.getMethod("load");
		assertEquals(method, BridgeMethodResolver.findBridgedMethod(loadWithObjectReturn));
		assertEquals(method, BridgeMethodResolver.findBridgedMethod(loadWithSettingsReturn));
	}

	@Test
	public void testFindBridgedMethodFromParent() throws Exception {
		Method loadFromParentBridge = SettingsDaoImpl.class.getMethod("loadFromParent");
		assertTrue(loadFromParentBridge.isBridge());

		Method loadFromParent = AbstractDaoImpl.class.getMethod("loadFromParent");
		assertFalse(loadFromParent.isBridge());

		assertEquals(loadFromParent, BridgeMethodResolver.findBridgedMethod(loadFromParentBridge));
	}

	@Test
	public void testWithSingleBoundParameterizedOnInstantiate() throws Exception {
		Method bridgeMethod = DelayQueue.class.getMethod("add", Object.class);
		assertTrue(bridgeMethod.isBridge());
		Method actualMethod = DelayQueue.class.getMethod("add", Delayed.class);
		assertFalse(actualMethod.isBridge());
		assertEquals(actualMethod, BridgeMethodResolver.findBridgedMethod(bridgeMethod));
	}

	@Test
	public void testWithDoubleBoundParameterizedOnInstantiate() throws Exception {
		Method bridgeMethod = SerializableBounded.class.getMethod("boundedOperation", Object.class);
		assertTrue(bridgeMethod.isBridge());
		Method actualMethod = SerializableBounded.class.getMethod("boundedOperation", HashMap.class);
		assertFalse(actualMethod.isBridge());
		assertEquals(actualMethod, BridgeMethodResolver.findBridgedMethod(bridgeMethod));
	}

	@Test
	public void testWithGenericParameter() throws Exception {
		Method[] methods = StringGenericParameter.class.getMethods();
		Method bridgeMethod = null;
		Method bridgedMethod = null;
		for (Method method : methods) {
			if ("getFor".equals(method.getName()) && !method.getParameterTypes()[0].equals(Integer.class)) {
				if (method.getReturnType().equals(Object.class)) {
					bridgeMethod = method;
				}
				else {
					bridgedMethod = method;
				}
			}
		}
		assertTrue(bridgeMethod != null && bridgeMethod.isBridge());
		assertTrue(bridgedMethod != null && !bridgedMethod.isBridge());
		assertEquals(bridgedMethod, BridgeMethodResolver.findBridgedMethod(bridgeMethod));
	}

	@Test
	public void testOnAllMethods() throws Exception {
		Method[] methods = StringList.class.getMethods();
		for (Method method : methods) {
			assertNotNull(BridgeMethodResolver.findBridgedMethod(method));
		}
	}

	@Test
	public void testSPR2583() throws Exception {
		Method bridgedMethod = MessageBroadcasterImpl.class.getMethod("receive", MessageEvent.class);
		assertFalse(bridgedMethod.isBridge());
		Method bridgeMethod = MessageBroadcasterImpl.class.getMethod("receive", Event.class);
		assertTrue(bridgeMethod.isBridge());

		Method otherMethod = MessageBroadcasterImpl.class.getMethod("receive", NewMessageEvent.class);
		assertFalse(otherMethod.isBridge());

		Map<TypeVariable, Type> typeVariableMap = GenericTypeResolver.getTypeVariableMap(MessageBroadcasterImpl.class);
		assertFalse("Match identified incorrectly", BridgeMethodResolver.isBridgeMethodFor(bridgeMethod, otherMethod, typeVariableMap));
		assertTrue("Match not found correctly", BridgeMethodResolver.isBridgeMethodFor(bridgeMethod, bridgedMethod, typeVariableMap));

		assertEquals(bridgedMethod, BridgeMethodResolver.findBridgedMethod(bridgeMethod));
	}

	@Test
	public void testSPR2454() throws Exception {
		Map<TypeVariable, Type> typeVariableMap = GenericTypeResolver.getTypeVariableMap(YourHomer.class);
		TypeVariable<?> variable = findTypeVariable(MyHomer.class, "L");
		assertEquals(AbstractBounded.class, ((ParameterizedType) typeVariableMap.get(variable)).getRawType());
	}

	@Test
	public void testSPR2603() throws Exception {
		Method objectBridge = YourHomer.class.getDeclaredMethod("foo", Bounded.class);
		Method abstractBoundedFoo = YourHomer.class.getDeclaredMethod("foo", AbstractBounded.class);

		Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(objectBridge);
		assertEquals("foo(AbstractBounded) not resolved.", abstractBoundedFoo, bridgedMethod);
	}

	@Test
	public void testSPR2648() throws Exception {
		Method bridgeMethod = ReflectionUtils.findMethod(GenericSqlMapIntegerDao.class, "saveOrUpdate", Object.class);
		assertTrue(bridgeMethod != null && bridgeMethod.isBridge());
		Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(bridgeMethod);
		assertFalse(bridgedMethod.isBridge());
		assertEquals("saveOrUpdate", bridgedMethod.getName());
	}

	@Test
	public void testSPR2763() throws Exception {
		Method bridgedMethod = AbstractDao.class.getDeclaredMethod("save", Object.class);
		assertFalse(bridgedMethod.isBridge());

		Method bridgeMethod = UserDaoImpl.class.getDeclaredMethod("save", User.class);
		assertTrue(bridgeMethod.isBridge());

		assertEquals(bridgedMethod, BridgeMethodResolver.findBridgedMethod(bridgeMethod));
	}

	@Test
	public void testSPR3041() throws Exception {
		Method bridgedMethod = BusinessDao.class.getDeclaredMethod("save", Business.class);
		assertFalse(bridgedMethod.isBridge());

		Method bridgeMethod = BusinessDao.class.getDeclaredMethod("save", Object.class);
		assertTrue(bridgeMethod.isBridge());

		assertEquals(bridgedMethod, BridgeMethodResolver.findBridgedMethod(bridgeMethod));
	}

	@Test
	public void testSPR3173() throws Exception {
		Method bridgedMethod = UserDaoImpl.class.getDeclaredMethod("saveVararg", User.class, Object[].class);
		assertFalse(bridgedMethod.isBridge());

		Method bridgeMethod = UserDaoImpl.class.getDeclaredMethod("saveVararg", Object.class, Object[].class);
		assertTrue(bridgeMethod.isBridge());

		assertEquals(bridgedMethod, BridgeMethodResolver.findBridgedMethod(bridgeMethod));
	}

	@Test
	public void testSPR3304() throws Exception {
		Method bridgedMethod = MegaMessageProducerImpl.class.getDeclaredMethod("receive", MegaMessageEvent.class);
		assertFalse(bridgedMethod.isBridge());

		Method bridgeMethod  = MegaMessageProducerImpl.class.getDeclaredMethod("receive", MegaEvent.class);
		assertTrue(bridgeMethod.isBridge());

		assertEquals(bridgedMethod, BridgeMethodResolver.findBridgedMethod(bridgeMethod));
	}

	@Test
	public void testSPR3324() throws Exception {
		Method bridgedMethod = BusinessDao.class.getDeclaredMethod("get", Long.class);
		assertFalse(bridgedMethod.isBridge());

		Method bridgeMethod = BusinessDao.class.getDeclaredMethod("get", Object.class);
		assertTrue(bridgeMethod.isBridge());

		assertEquals(bridgedMethod, BridgeMethodResolver.findBridgedMethod(bridgeMethod));
	}

	@Test
	public void testSPR3357() throws Exception {
		Method bridgedMethod = ExtendsAbstractImplementsInterface.class.getDeclaredMethod(
				"doSomething", DomainObjectExtendsSuper.class, Object.class);
		assertFalse(bridgedMethod.isBridge());

		Method bridgeMethod = ExtendsAbstractImplementsInterface.class.getDeclaredMethod(
				"doSomething", DomainObjectSuper.class, Object.class);
		assertTrue(bridgeMethod.isBridge());

		assertEquals(bridgedMethod, BridgeMethodResolver.findBridgedMethod(bridgeMethod));
	}

	@Test
	public void testSPR3485() throws Exception {
		Method bridgedMethod = DomainObject.class.getDeclaredMethod(
				"method2", ParameterType.class, byte[].class);
		assertFalse(bridgedMethod.isBridge());

		Method bridgeMethod = DomainObject.class.getDeclaredMethod(
				"method2", Serializable.class, Object.class);
		assertTrue(bridgeMethod.isBridge());

		assertEquals(bridgedMethod, BridgeMethodResolver.findBridgedMethod(bridgeMethod));
	}

	@Test
	public void testSPR3534() throws Exception {
		Method bridgeMethod = ReflectionUtils.findMethod(TestEmailProvider.class, "findBy", Object.class);
		assertTrue(bridgeMethod != null && bridgeMethod.isBridge());
		Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(bridgeMethod);
		assertFalse(bridgedMethod.isBridge());
		assertEquals("findBy", bridgedMethod.getName());
	}


	public static interface Foo<T extends Serializable> {

		void someMethod(T theArg, Object otherArg);

		void someVarargMethod(T theArg, Object... otherArg);
	}


	public static class MyFoo implements Foo<String> {

		public void someMethod(Integer theArg, Object otherArg) {
		}

		@Override
		public void someMethod(String theArg, Object otherArg) {
		}

		@Override
		public void someVarargMethod(String theArg, Object... otherArgs) {
		}
	}


	public static abstract class Bar<T> {

		void someMethod(Map<?, ?> m, Object otherArg) {
		}

		void someMethod(T theArg, Map<?, ?> m) {
		}

		abstract void someMethod(T theArg, Object otherArg);
	}


	public static abstract class InterBar<T> extends Bar<T> {

	}


	public static class MyBar extends InterBar<String> {

		@Override
		public void someMethod(String theArg, Object otherArg) {
		}

		public void someMethod(Integer theArg, Object otherArg) {
		}
	}


	public interface Adder<T> {

		void add(T item);
	}


	public abstract class AbstractDateAdder implements Adder<Date> {

		@Override
		public abstract void add(Date date);
	}


	public class DateAdder extends AbstractDateAdder {

		@Override
		public void add(Date date) {
		}
	}


	public class Enclosing<T> {

		public class Enclosed<S> {

			public class ReallyDeepNow<R> {

				void someMethod(S s, T t, R r) {
				}
			}
		}
	}


	public class ExtendsEnclosing extends Enclosing<String> {

		public class ExtendsEnclosed extends Enclosed<Integer> {

			public class ExtendsReallyDeepNow extends ReallyDeepNow<Long> {

				@Override
				void someMethod(Integer s, String t, Long r) {
					throw new UnsupportedOperationException();
				}
			}
		}
	}


	public interface Boo<E, T extends Serializable> {

		void foo(E e);

		void foo(T t);
	}


	public class MyBoo implements Boo<String, Integer> {

		@Override
		public void foo(String e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void foo(Integer t) {
			throw new UnsupportedOperationException();
		}
	}


	public interface Settings {

	}


	public interface ConcreteSettings extends Settings {

	}


	public interface Dao<T, S> {

		T load();

		S loadFromParent();
	}


	public interface SettingsDao<T extends Settings, S> extends Dao<T, S> {

		@Override
		T load();
	}


	public interface ConcreteSettingsDao extends SettingsDao<ConcreteSettings, String> {

		@Override
		String loadFromParent();
	}


	abstract class AbstractDaoImpl<T, S> implements Dao<T, S> {

		protected T object;

		protected S otherObject;

		protected AbstractDaoImpl(T object, S otherObject) {
			this.object = object;
			this.otherObject = otherObject;
		}

		//@Transactional(readOnly = true)
		@Override
		public S loadFromParent() {
			return otherObject;
		}
	}


	class SettingsDaoImpl extends AbstractDaoImpl<ConcreteSettings, String> implements ConcreteSettingsDao {

		protected SettingsDaoImpl(ConcreteSettings object) {
			super(object, "From Parent");
		}

		//@Transactional(readOnly = true)
		@Override
		public ConcreteSettings load() {
			return super.object;
		}
	}


	public static interface Bounded<E> {

		boolean boundedOperation(E e);
	}


	private static class AbstractBounded<E> implements Bounded<E> {

		@Override
		public boolean boundedOperation(E myE) {
			return true;
		}
	}


	private static class SerializableBounded<E extends HashMap & Delayed> extends AbstractBounded<E> {

		@Override
		public boolean boundedOperation(E myE) {
			return false;
		}
	}


	public static interface GenericParameter<T> {

		T getFor(Class<T> cls);
	}


	private static class StringGenericParameter implements GenericParameter<String> {

		@Override
		public String getFor(Class<String> cls) {
			return "foo";
		}

		public String getFor(Integer integer) {
			return "foo";
		}
	}


	private static class StringList implements List<String> {

		@Override
		public int size() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isEmpty() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean contains(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<String> iterator() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object[] toArray() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean add(String o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends String> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(int index, Collection<? extends String> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String get(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String set(int index, String element) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void add(int index, String element) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String remove(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int indexOf(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int lastIndexOf(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ListIterator<String> listIterator() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ListIterator<String> listIterator(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<String> subList(int fromIndex, int toIndex) {
			throw new UnsupportedOperationException();
		}
	}


	public interface Event {

		int getPriority();
	}


	public class GenericEvent implements Event {

		private int priority;

		@Override
		public int getPriority() {
			return priority;
		}

		/**
		 * Constructor that takes an event priority
		 */
		public GenericEvent(int priority) {
			this.priority = priority;
		}

		/**
		 * Default Constructor
		 */
		public GenericEvent() {
		}
	}


	public interface UserInitiatedEvent {

		//public Session getInitiatorSession();
	}


	public abstract class BaseUserInitiatedEvent extends GenericEvent implements UserInitiatedEvent {

	}


	public class MessageEvent extends BaseUserInitiatedEvent {

	}


	public interface Channel<E extends Event> {

		void send(E event);

		void subscribe(final Receiver<E> receiver, Class<E> event);

		void unsubscribe(final Receiver<E> receiver, Class<E> event);
	}


	public interface Broadcaster {
	}


	public interface EventBroadcaster extends Broadcaster {

		public void subscribe();

		public void unsubscribe();

		public void setChannel(Channel<?> channel);
	}


	public class GenericBroadcasterImpl implements Broadcaster {

	}


	public abstract class GenericEventBroadcasterImpl<T extends Event> extends GenericBroadcasterImpl
					implements EventBroadcaster {

		private Class<T>[] subscribingEvents;

		private Channel<T> channel;

		/**
		 * Abstract method to retrieve instance of subclass
		 *
		 * @return receiver instance
		 */
		public abstract Receiver<T> getInstance();

		@Override
		public void setChannel(Channel channel) {
			this.channel = channel;
		}

		private String beanName;

		public void setBeanName(String name) {
			this.beanName = name;
		}

		@Override
		public void subscribe() {

		}

		@Override
		public void unsubscribe() {

		}

		public GenericEventBroadcasterImpl(Class<? extends T>... events) {

		}
	}


	public interface Receiver<E extends Event> {

		void receive(E event);
	}


	public interface MessageBroadcaster extends Receiver<MessageEvent> {

	}


	public class RemovedMessageEvent extends MessageEvent {

	}


	public class NewMessageEvent extends MessageEvent {

	}


	public class ModifiedMessageEvent extends MessageEvent {

	}


	public class MessageBroadcasterImpl extends GenericEventBroadcasterImpl<MessageEvent>
					implements MessageBroadcaster {

		public MessageBroadcasterImpl() {
			super(NewMessageEvent.class);
		}

		@Override
		public void receive(MessageEvent event) {
			throw new UnsupportedOperationException("should not be called, use subclassed events");
		}

		public void receive(NewMessageEvent event) {
		}

		@Override
		public Receiver<MessageEvent> getInstance() {
			return null;
		}

		public void receive(RemovedMessageEvent event) {
		}

		public void receive(ModifiedMessageEvent event) {
		}
	}


	//-----------------------------
	// SPR-2454 Test Classes
	//-----------------------------

	public interface SimpleGenericRepository<T> {

		public Class<T> getPersistentClass();

		List<T> findByQuery();

		List<T> findAll();

		T refresh(T entity);

		T saveOrUpdate(T entity);

		void delete(Collection<T> entities);
	}


	public interface RepositoryRegistry {

		<T> SimpleGenericRepository<T> getFor(Class<T> entityType);
	}


	public class SettableRepositoryRegistry<R extends SimpleGenericRepository<?>>
					implements RepositoryRegistry {

		protected void injectInto(R rep) {
		}

		public void register(R rep) {
		}

		public void register(R... reps) {
		}

		public void setRepos(R... reps) {
		}

		@Override
		public <T> SimpleGenericRepository<T> getFor(Class<T> entityType) {
			return null;
		}

		public void afterPropertiesSet() throws Exception {
		}
	}


	public interface ConvenientGenericRepository<T, ID extends Serializable> extends SimpleGenericRepository<T> {

		T findById(ID id, boolean lock);

		List<T> findByExample(T exampleInstance);

		void delete(ID id);

		void delete(T entity);
	}


	public class GenericHibernateRepository<T, ID extends Serializable>
					implements ConvenientGenericRepository<T, ID> {

		/**
		 * @param c Mandatory. The domain class this repository is responsible for.
		 */
		// Since it is impossible to determine the actual type of a type
		// parameter (!), we resort to requiring the caller to provide the
		// actual type as parameter, too.
		// Not set in a constructor to enable easy CGLIB-proxying (passing
		// constructor arguments to Spring AOP proxies is quite cumbersome).
		public void setPersistentClass(Class<T> c) {
		}

		@Override
		public Class<T> getPersistentClass() {
			return null;
		}

		@Override
		public T findById(ID id, boolean lock) {
			return null;
		}

		@Override
		public List<T> findAll() {
			return null;
		}

		@Override
		public List<T> findByExample(T exampleInstance) {
			return null;
		}

		@Override
		public List<T> findByQuery() {
			return null;
		}

		@Override
		public T saveOrUpdate(T entity) {
			return null;
		}

		@Override
		public void delete(T entity) {
		}

		@Override
		public T refresh(T entity) {
			return null;
		}

		@Override
		public void delete(ID id) {
		}

		@Override
		public void delete(Collection<T> entities) {
		}
	}


	public class HibernateRepositoryRegistry extends SettableRepositoryRegistry<GenericHibernateRepository<?, ?>> {

		@Override
		public void injectInto(GenericHibernateRepository<?, ?> rep) {
		}

		@Override
		public <T> GenericHibernateRepository<T, ?> getFor(Class<T> entityType) {
			return null;
		}
	}


	//-------------------
	// SPR-2603 classes
	//-------------------

	public interface Homer<E> {

		void foo(E e);
	}


	public class MyHomer<T extends Bounded<T>, L extends T> implements Homer<L> {

		@Override
		public void foo(L t) {
			throw new UnsupportedOperationException();
		}
	}


	public class YourHomer<T extends AbstractBounded<T>, L extends T> extends MyHomer<T, L> {

		@Override
		public void foo(L t) {
			throw new UnsupportedOperationException();
		}
	}


	public interface GenericDao<T> {

		public void saveOrUpdate(T t);
	}


	public interface ConvenienceGenericDao<T> extends GenericDao<T> {
	}


	public class GenericSqlMapDao<T extends Serializable> implements ConvenienceGenericDao<T> {

		@Override
		public void saveOrUpdate(T t) {
			throw new UnsupportedOperationException();
		}
	}


	public class GenericSqlMapIntegerDao<T extends Integer> extends GenericSqlMapDao<T> {

		@Override
		public void saveOrUpdate(T t) {
		}
	}


	public class Permission {
	}


	public class User {
	}


	public interface UserDao {

		//@Transactional
		void save(User user);

		//@Transactional
		void save(Permission perm);
	}


	public abstract class AbstractDao<T> {

		public void save(T t) {
		}

		public void saveVararg(T t, Object... args) {
		}
	}


	public class UserDaoImpl extends AbstractDao<User> implements UserDao {

		@Override
		public void save(Permission perm) {
		}

		@Override
		public void saveVararg(User user, Object... args) {
		}
	}


	public interface DaoInterface<T,P> {
			T get(P id);
	}


	public abstract class BusinessGenericDao<T, PK extends Serializable> implements DaoInterface<T, PK> {

		public void save(T object) {
		}
	}


	public class Business<T> {
	}


	public class BusinessDao extends BusinessGenericDao<Business<?>, Long> {

	@Override
	public void save(Business<?> business) {
	}

		@Override
		public Business<?> get(Long id) {
			return null;
		}

		public Business<?> get(String code) {
			return null;
		}
	}


	//-------------------
	// SPR-3304 classes
	//-------------------

	private static class MegaEvent {
	}


	private static class MegaMessageEvent extends MegaEvent {
	}


	private static class NewMegaMessageEvent extends MegaEvent {
	}


	private static class ModifiedMegaMessageEvent extends MegaEvent {
	}


	public static interface MegaReceiver<E extends MegaEvent> {

		void receive(E event);
	}


	public static interface MegaMessageProducer extends MegaReceiver<MegaMessageEvent> {
	}


	private static class Other<S,E> {
	}


	private static class MegaMessageProducerImpl extends Other<Long, String> implements MegaMessageProducer {

		public void receive(NewMegaMessageEvent event) {
			throw new UnsupportedOperationException();
		}

		public void receive(ModifiedMegaMessageEvent event) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void receive(MegaMessageEvent event) {
			throw new UnsupportedOperationException();
		}
	}


	//-------------------
	// SPR-3357 classes
	//-------------------

	private static class DomainObjectSuper {
	}


	private static class DomainObjectExtendsSuper extends DomainObjectSuper {
	}


	public interface IGenericInterface<D extends DomainObjectSuper> {

		<T> void doSomething(final D domainObject, final T value);
	}


	private static abstract class AbstractImplementsInterface<D extends DomainObjectSuper> implements IGenericInterface<D> {

		@Override
		public <T> void doSomething(D domainObject, T value) {
		}

		public void anotherBaseMethod() {
		}
	}


	private static class ExtendsAbstractImplementsInterface extends AbstractImplementsInterface<DomainObjectExtendsSuper> {

		@Override
		public <T> void doSomething(DomainObjectExtendsSuper domainObject, T value) {
			super.doSomething(domainObject, value);
		}
	}


	//-------------------
	// SPR-3485 classes
	//-------------------

	@SuppressWarnings("serial")
	private static class ParameterType implements Serializable {
	}


	private static class AbstractDomainObject<P extends Serializable, R> {

		public R method1(P p) {
			return null;
		}

		public void method2(P p, R r) {
		}
	}


	private static class DomainObject extends AbstractDomainObject<ParameterType, byte[]> {

		@Override
		public byte[] method1(ParameterType p) {
			return super.method1(p);
		}

		@Override
		public void method2(ParameterType p, byte[] r) {
			super.method2(p, r);
		}
	}


	//-------------------
	// SPR-3534 classes
	//-------------------

	public interface SearchProvider<RETURN_TYPE, CONDITIONS_TYPE> {

		Collection<RETURN_TYPE> findBy(CONDITIONS_TYPE conditions);
	}


	public static class SearchConditions {
	}


	public interface IExternalMessageProvider<S extends ExternalMessage, T extends ExternalMessageSearchConditions<?>>
			extends SearchProvider<S, T> {
	}


	public static class ExternalMessage {
	}


	public static class ExternalMessageSearchConditions<T extends ExternalMessage> extends SearchConditions {
	}


	public static class ExternalMessageProvider<S extends ExternalMessage, T extends ExternalMessageSearchConditions<S>>
			implements IExternalMessageProvider<S, T> {

		@Override
		public Collection<S> findBy(T conditions) {
			return null;
		}
	}


	public static class EmailMessage extends ExternalMessage {
	}


	public static class EmailSearchConditions extends ExternalMessageSearchConditions<EmailMessage> {
	}


	public static class EmailMessageProvider extends ExternalMessageProvider<EmailMessage, EmailSearchConditions> {
	}


	public static class TestEmailProvider extends EmailMessageProvider {

		@Override
		public Collection<EmailMessage> findBy(EmailSearchConditions conditions) {
			return null;
		}
	}

}
