/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

/**
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class SimpleMapScope implements Scope, Serializable {

	private final Map<String, Object> map = new HashMap<>();

	private final List<Runnable> callbacks = new ArrayList<>();


	public SimpleMapScope() {
	}

	public final Map<String, Object> getMap() {
		return this.map;
	}


	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		synchronized (this.map) {
			Object scopedObject = this.map.get(name);
			if (scopedObject == null) {
				scopedObject = objectFactory.getObject();
				this.map.put(name, scopedObject);
			}
			return scopedObject;
		}
	}

	@Override
	public Object remove(String name) {
		synchronized (this.map) {
			return this.map.remove(name);
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		this.callbacks.add(callback);
	}

	public void close() {
		for (Runnable runnable : this.callbacks) {
			runnable.run();
		}
	}

}
