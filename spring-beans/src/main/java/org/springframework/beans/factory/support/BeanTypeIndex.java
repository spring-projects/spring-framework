/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.ClassUtils;

/**
 * Index from types to the bean definition names that may match them, used by
 * {@link DefaultListableBeanFactory} to narrow down the bean definitions that
 * need to be checked for a by-type lookup in a frozen configuration.
 *
 * <p>Without such an index, every lookup for a type that is not cached yet
 * checks all bean definitions, which makes the bootstrap of a context with
 * {@code N} bean definitions and {@code M} distinct injection point types an
 * {@code O(N * M)} operation.
 *
 * <p>The index is a superset: for a given type, it returns every bean
 * definition name that the regular type matching might accept, in registration
 * order. The factory still applies its regular type matching to each of the
 * returned names, so the outcome of a lookup is the same as with a check of all
 * bean definitions. A bean is registered under its known types and all their
 * superclasses and interfaces. A bean whose type cannot be safely determined
 * up front is a candidate for every type:
 * <ul>
 * <li>a bean without a predictable type, a {@link FactoryBean} (whose object
 * type is only known to the factory), an abstract bean definition, or a name
 * that is also an alias,</li>
 * <li>a non-singleton bean, since its predicted type may change once an
 * instance has been created,</li>
 * <li>a bean of an array type,</li>
 * <li>a singleton bean with an early reference in the course of its creation,
 * until the actual singleton instance has been registered.</li>
 * </ul>
 *
 * <p>The index can be built and updated concurrently: all state is guarded by
 * the index itself, and every operation only adds candidates, except for the
 * removal of an early reference marker once the singleton instance has been
 * registered. The factory discards the index whenever it discards its by-type
 * cache.
 *
 * @author Nazar Shuliak
 * @since 7.1.1
 * @see DefaultListableBeanFactory#getBeanNamesForType(Class, boolean, boolean)
 */
final class BeanTypeIndex {

	private final String[] beanNames;

	private final Map<String, Integer> positions;

	private final Map<Class<?>, BitSet> positionsByType = new HashMap<>(256);

	private final BitSet alwaysCandidates = new BitSet();

	private final BitSet earlyReferences = new BitSet();

	private volatile boolean complete;


	/**
	 * Create a new index for the given bean definition names.
	 * @param beanNames the bean definition names, in registration order
	 */
	BeanTypeIndex(String[] beanNames) {
		this.beanNames = beanNames;
		this.positions = new HashMap<>((int) (beanNames.length / 0.75f) + 1);
		for (int i = 0; i < beanNames.length; i++) {
			this.positions.put(beanNames[i], i);
		}
	}


	/**
	 * Return the bean definition names covered by this index, in registration order.
	 */
	String[] getBeanNames() {
		return this.beanNames;
	}

	/**
	 * Register the given type, its superclasses and its interfaces for the given bean.
	 * <p>A primitive type is registered as its wrapper type, and an array type turns
	 * the bean into a candidate for every type.
	 * @param beanName the name of the bean
	 * @param beanType the predicted or actual type of the bean
	 */
	synchronized void registerType(String beanName, Class<?> beanType) {
		Integer position = this.positions.get(beanName);
		if (position == null) {
			return;
		}
		if (beanType.isArray()) {
			this.alwaysCandidates.set(position);
			return;
		}
		Set<Class<?>> visited = new HashSet<>();
		Deque<Class<?>> queue = new ArrayDeque<>();
		queue.add(ClassUtils.resolvePrimitiveIfNecessary(beanType));
		while (!queue.isEmpty()) {
			Class<?> type = queue.poll();
			if (visited.add(type)) {
				this.positionsByType.computeIfAbsent(type, key -> new BitSet()).set(position);
				Class<?> superclass = type.getSuperclass();
				if (superclass != null) {
					queue.add(superclass);
				}
				Collections.addAll(queue, type.getInterfaces());
			}
		}
	}

	/**
	 * Register the given bean as a candidate for every type.
	 * @param beanName the name of the bean
	 */
	synchronized void registerUnknownType(String beanName) {
		Integer position = this.positions.get(beanName);
		if (position != null) {
			this.alwaysCandidates.set(position);
		}
	}

	/**
	 * Register the given bean as a candidate for every type until its singleton
	 * instance has been registered, since the type of early reference to the
	 * bean (for example, a proxy) is not known up front.
	 * @param beanName the name of the bean
	 * @see #registerSingleton
	 */
	synchronized void registerEarlyReference(String beanName) {
		Integer position = this.positions.get(beanName);
		if (position != null) {
			this.earlyReferences.set(position);
		}
	}

	/**
	 * Register the type of the given singleton instance for the given bean,
	 * replacing a previous early reference marker.
	 * @param beanName the name of the bean
	 * @param singletonObject the singleton instance
	 */
	synchronized void registerSingleton(String beanName, Object singletonObject) {
		Integer position = this.positions.get(beanName);
		if (position == null) {
			return;
		}
		if (singletonObject instanceof FactoryBean<?>) {
			this.alwaysCandidates.set(position);
		}
		else {
			registerType(beanName, singletonObject.getClass());
		}
		this.earlyReferences.clear(position);
	}

	/**
	 * Mark this index as completely built, which makes it available for lookups.
	 */
	void markComplete() {
		this.complete = true;
	}

	/**
	 * Return whether this index has been completely built.
	 */
	boolean isComplete() {
		return this.complete;
	}

	/**
	 * Return the names of all beans that may match the given type, in
	 * registration order.
	 * @param type the type to match
	 * @return a superset of the names of the matching bean definitions
	 */
	synchronized List<String> getCandidateNames(Class<?> type) {
		BitSet candidates = (BitSet) this.alwaysCandidates.clone();
		candidates.or(this.earlyReferences);
		BitSet positionsForType = this.positionsByType.get(ClassUtils.resolvePrimitiveIfNecessary(type));
		if (positionsForType != null) {
			candidates.or(positionsForType);
		}
		List<String> candidateNames = new ArrayList<>(candidates.cardinality());
		for (int i = candidates.nextSetBit(0); i >= 0; i = candidates.nextSetBit(i + 1)) {
			candidateNames.add(this.beanNames[i]);
		}
		return candidateNames;
	}

}
