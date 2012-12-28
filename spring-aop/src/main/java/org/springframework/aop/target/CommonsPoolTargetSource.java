/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.aop.target;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import org.springframework.beans.BeansException;
import org.springframework.core.Constants;

/**
 * TargetSource implementation that holds objects in a configurable
 * Jakarta Commons Pool.
 *
 * <p>By default, an instance of <code>GenericObjectPool</code> is created.
 * Subclasses may change the type of <code>ObjectPool</code> used by
 * overriding the <code>createObjectPool()</code> method.
 *
 * <p>Provides many configuration properties mirroring those of the Commons Pool
 * <code>GenericObjectPool</code> class; these properties are passed to the
 * <code>GenericObjectPool</code> during construction. If creating a subclass of this
 * class to change the <code>ObjectPool</code> implementation type, pass in the values
 * of configuration properties that are relevant to your chosen implementation.
 *
 * <p>The <code>testOnBorrow</code>, <code>testOnReturn</code> and <code>testWhileIdle</code>
 * properties are explictly not mirrored because the implementation of
 * <code>PoolableObjectFactory</code> used by this class does not implement
 * meaningful validation. All exposed Commons Pool properties use the corresponding
 * Commons Pool defaults: for example,
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see GenericObjectPool
 * @see #createObjectPool()
 * @see #setMaxSize
 * @see #setMaxIdle
 * @see #setMinIdle
 * @see #setMaxWait
 * @see #setTimeBetweenEvictionRunsMillis
 * @see #setMinEvictableIdleTimeMillis
 */
public class CommonsPoolTargetSource extends AbstractPoolingTargetSource
		implements PoolableObjectFactory {

	private static final Constants constants = new Constants(GenericObjectPool.class);


	private int maxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;

	private int minIdle = GenericObjectPool.DEFAULT_MIN_IDLE;

	private long maxWait = GenericObjectPool.DEFAULT_MAX_WAIT;

	private long timeBetweenEvictionRunsMillis = GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

	private long minEvictableIdleTimeMillis = GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

	private byte whenExhaustedAction = GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION;

	/**
	 * The Jakarta Commons <code>ObjectPool</code> used to pool target objects
	 */
	private ObjectPool pool;


	/**
	 * Create a CommonsPoolTargetSource with default settings.
	 * Default maximum size of the pool is 8.
	 * @see #setMaxSize
	 * @see GenericObjectPool#setMaxActive
	 */
	public CommonsPoolTargetSource() {
		setMaxSize(GenericObjectPool.DEFAULT_MAX_ACTIVE);
	}

	/**
	 * Set the maximum number of idle objects in the pool.
	 * Default is 8.
	 * @see GenericObjectPool#setMaxIdle
	 */
	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}

	/**
	 * Return the maximum number of idle objects in the pool.
	 */
	public int getMaxIdle() {
		return this.maxIdle;
	}

	/**
	 * Set the minimum number of idle objects in the pool.
	 * Default is 0.
	 * @see GenericObjectPool#setMinIdle
	 */
	public void setMinIdle(int minIdle) {
		this.minIdle = minIdle;
	}

	/**
	 * Return the minimum number of idle objects in the pool.
	 */
	public int getMinIdle() {
		return this.minIdle;
	}

	/**
	 * Set the maximum waiting time for fetching an object from the pool.
	 * Default is -1, waiting forever.
	 * @see GenericObjectPool#setMaxWait
	 */
	public void setMaxWait(long maxWait) {
		this.maxWait = maxWait;
	}

	/**
	 * Return the maximum waiting time for fetching an object from the pool.
	 */
	public long getMaxWait() {
		return this.maxWait;
	}

	/**
	 * Set the time between eviction runs that check idle objects whether
	 * they have been idle for too long or have become invalid.
	 * Default is -1, not performing any eviction.
	 * @see GenericObjectPool#setTimeBetweenEvictionRunsMillis
	 */
	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
		this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}

	/**
	 * Return the time between eviction runs that check idle objects.
	 */
	public long getTimeBetweenEvictionRunsMillis() {
		return this.timeBetweenEvictionRunsMillis;
	}

	/**
	 * Set the minimum time that an idle object can sit in the pool before
	 * it becomes subject to eviction. Default is 1800000 (30 minutes).
	 * <p>Note that eviction runs need to be performed to take this
	 * setting into effect.
	 * @see #setTimeBetweenEvictionRunsMillis
	 * @see GenericObjectPool#setMinEvictableIdleTimeMillis
	 */
	public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
		this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}

	/**
	 * Return the minimum time that an idle object can sit in the pool.
	 */
	public long getMinEvictableIdleTimeMillis() {
		return this.minEvictableIdleTimeMillis;
	}

	/**
	 * Set the action to take when the pool is exhausted. Uses the
	 * constant names defined in Commons Pool's GenericObjectPool class:
	 * "WHEN_EXHAUSTED_BLOCK", "WHEN_EXHAUSTED_FAIL", "WHEN_EXHAUSTED_GROW".
	 * @see #setWhenExhaustedAction(byte)
	 */
	public void setWhenExhaustedActionName(String whenExhaustedActionName) {
		setWhenExhaustedAction(constants.asNumber(whenExhaustedActionName).byteValue());
	}

	/**
	 * Set the action to take when the pool is exhausted. Uses the
	 * constant values defined in Commons Pool's GenericObjectPool class.
	 * @see GenericObjectPool#setWhenExhaustedAction(byte)
	 * @see GenericObjectPool#WHEN_EXHAUSTED_BLOCK
	 * @see GenericObjectPool#WHEN_EXHAUSTED_FAIL
	 * @see GenericObjectPool#WHEN_EXHAUSTED_GROW
	 */
	public void setWhenExhaustedAction(byte whenExhaustedAction) {
		this.whenExhaustedAction = whenExhaustedAction;
	}

	/**
	 * Return the action to take when the pool is exhausted.
	 */
	public byte getWhenExhaustedAction() {
		return whenExhaustedAction;
	}


	/**
	 * Creates and holds an ObjectPool instance.
	 * @see #createObjectPool()
	 */
	@Override
	protected final void createPool() {
		logger.debug("Creating Commons object pool");
		this.pool = createObjectPool();
	}

	/**
	 * Subclasses can override this if they want to return a specific Commons pool.
	 * They should apply any configuration properties to the pool here.
	 * <p>Default is a GenericObjectPool instance with the given pool size.
	 * @return an empty Commons <code>ObjectPool</code>.
	 * @see org.apache.commons.pool.impl.GenericObjectPool
	 * @see #setMaxSize
	 */
	protected ObjectPool createObjectPool() {
		GenericObjectPool gop = new GenericObjectPool(this);
		gop.setMaxActive(getMaxSize());
		gop.setMaxIdle(getMaxIdle());
		gop.setMinIdle(getMinIdle());
		gop.setMaxWait(getMaxWait());
		gop.setTimeBetweenEvictionRunsMillis(getTimeBetweenEvictionRunsMillis());
		gop.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis());
		gop.setWhenExhaustedAction(getWhenExhaustedAction());
		return gop;
	}


	/**
	 * Borrow an object from the <code>ObjectPool</code>.
	 */
	@Override
	public Object getTarget() throws Exception {
		return this.pool.borrowObject();
	}

	/**
	 * Returns the specified object to the underlying <code>ObjectPool</code>.
	 */
	@Override
	public void releaseTarget(Object target) throws Exception {
		this.pool.returnObject(target);
	}

	public int getActiveCount() throws UnsupportedOperationException {
		return this.pool.getNumActive();
	}

	public int getIdleCount() throws UnsupportedOperationException {
		return this.pool.getNumIdle();
	}


	/**
	 * Closes the underlying <code>ObjectPool</code> when destroying this object.
	 */
	public void destroy() throws Exception {
		logger.debug("Closing Commons ObjectPool");
		this.pool.close();
	}


	//----------------------------------------------------------------------------
	// Implementation of org.apache.commons.pool.PoolableObjectFactory interface
	//----------------------------------------------------------------------------

	public Object makeObject() throws BeansException {
		return newPrototypeInstance();
	}

	public void destroyObject(Object obj) throws Exception {
		destroyPrototypeInstance(obj);
	}

	public boolean validateObject(Object obj) {
		return true;
	}

	public void activateObject(Object obj) {
	}

	public void passivateObject(Object obj) {
	}

}
