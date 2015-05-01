/*
 * Copyright 2002-2015 the original author or authors.
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

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * {@link org.springframework.aop.TargetSource} implementation that holds
 * objects in a configurable Apache Commons2 Pool.
 *
 * <p>By default, an instance of {@code GenericObjectPool} is created.
 * Subclasses may change the type of {@code ObjectPool} used by
 * overriding the {@code createObjectPool()} method.
 *
 * <p>Provides many configuration properties mirroring those of the Commons Pool
 * {@code GenericObjectPool} class; these properties are passed to the
 * {@code GenericObjectPool} during construction. If creating a subclass of this
 * class to change the {@code ObjectPool} implementation type, pass in the values
 * of configuration properties that are relevant to your chosen implementation.
 *
 * <p>The {@code testOnBorrow}, {@code testOnReturn} and {@code testWhileIdle}
 * properties are explicitly not mirrored because the implementation of
 * {@code PoolableObjectFactory} used by this class does not implement
 * meaningful validation. All exposed Commons Pool properties use the
 * corresponding Commons Pool defaults.
 *
 * <p>Commons Pool 2.x uses object equality while Commons Pool 1.x used identity
 * equality. This clearly means that Commons Pool 2 behaves differently if several
 * instances having the same identity according to their {@link Object#equals(Object)}
 * method are managed in the same pool. To provide a smooth upgrade, a
 * backward-compatible pool is created by default; use {@link #setUseObjectEquality(boolean)}
 * if you need the standard Commons Pool 2.x behavior.
 *
 * <p>Compatible with Apache Commons Pool 2.2
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 4.2
 * @see GenericObjectPool
 * @see #createObjectPool()
 * @see #setMaxSize
 * @see #setMaxIdle
 * @see #setMinIdle
 * @see #setMaxWait
 * @see #setTimeBetweenEvictionRunsMillis
 * @see #setMinEvictableIdleTimeMillis
 */
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
public class CommonsPool2TargetSource extends AbstractPoolingTargetSource implements PooledObjectFactory<Object> {

	private int maxIdle = GenericObjectPoolConfig.DEFAULT_MAX_IDLE;

	private int minIdle = GenericObjectPoolConfig.DEFAULT_MIN_IDLE;

	private long maxWait = GenericObjectPoolConfig.DEFAULT_MAX_TOTAL;

	private long timeBetweenEvictionRunsMillis = GenericObjectPoolConfig.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

	private long minEvictableIdleTimeMillis = GenericObjectPoolConfig.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

	private boolean blockWhenExhausted = GenericObjectPoolConfig.DEFAULT_BLOCK_WHEN_EXHAUSTED;

	private boolean useObjectEquality;

	/**
	 * The Apache Commons {@code ObjectPool} used to pool target objects
	 */
	private ObjectPool pool;


	/**
	 * Create a CommonsPoolTargetSource with default settings.
	 * Default maximum size of the pool is 8.
	 * @see #setMaxSize
	 * @see GenericObjectPoolConfig#setMaxTotal
	 */
	public CommonsPool2TargetSource() {
		setMaxSize(GenericObjectPoolConfig.DEFAULT_MAX_TOTAL);
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
	 * @see GenericObjectPool#setMaxTotal
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
	 * Set whether the call should bock when the pool is exhausted.
	 */
	public void setBlockWhenExhausted(boolean blockWhenExhausted) {
		this.blockWhenExhausted = blockWhenExhausted;
	}

	/**
	 * Specify if the call should block when the pool is exhausted.
	 */
	public boolean isBlockWhenExhausted() {
		return blockWhenExhausted;
	}

	/**
	 * Set if the pool should use object equality. Commons Pool 1.x has no specific requirement in
	 * that regard and allows two distinct instances being equal to be put in the same pool. However,
	 * this behavior has changed with commons pool 2. To preserve backward compatibility, the pool
	 * is configured to use reference equality ({@code false}.
	 */
	public void setUseObjectEquality(boolean useObjectEquality) {
		this.useObjectEquality = useObjectEquality;
	}

	/**
	 * Specify if the pool should use object equality. Return {@code false} if it should use
	 * reference equality (as it was the case for Commons Pool 1.x)
	 */
	public boolean isUseObjectEquality() {
		return useObjectEquality;
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
	 * @return an empty Commons {@code ObjectPool}.
	 * @see GenericObjectPool
	 * @see #setMaxSize
	 */
	protected ObjectPool createObjectPool() {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(getMaxSize());
		config.setMaxIdle(getMaxIdle());
		config.setMinIdle(getMinIdle());
		config.setMaxWaitMillis(getMaxWait());
		config.setTimeBetweenEvictionRunsMillis(getTimeBetweenEvictionRunsMillis());
		config.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis());
		config.setBlockWhenExhausted(isBlockWhenExhausted());
		return new GenericObjectPool(this, config);
	}


	/**
	 * Borrow an object from the {@code ObjectPool}.
	 */
	@Override
	public Object getTarget() throws Exception {
		Object o = this.pool.borrowObject();
		return (isUseObjectEquality() ? o : ((IdentityWrapper)o).target);
	}

	/**
	 * Returns the specified object to the underlying {@code ObjectPool}.
	 */
	@Override
	public void releaseTarget(Object target) throws Exception {
		Object value = (isUseObjectEquality() ? target : new IdentityWrapper(target));
		this.pool.returnObject(value);
	}

	@Override
	public int getActiveCount() throws UnsupportedOperationException {
		return this.pool.getNumActive();
	}

	@Override
	public int getIdleCount() throws UnsupportedOperationException {
		return this.pool.getNumIdle();
	}


	/**
	 * Closes the underlying {@code ObjectPool} when destroying this object.
	 */
	@Override
	public void destroy() throws Exception {
		logger.debug("Closing Commons ObjectPool");
		this.pool.close();
	}


	//----------------------------------------------------------------------------
	// Implementation of org.apache.commons.pool2.PooledObjectFactory interface
	//----------------------------------------------------------------------------

	@Override
	public PooledObject<Object> makeObject() throws Exception {
		Object target = newPrototypeInstance();
		Object poolValue = (isUseObjectEquality() ? target : new IdentityWrapper(target));
		return new DefaultPooledObject<Object>(poolValue);
	}

	@Override
	public void destroyObject(PooledObject<Object> p) throws Exception {
		destroyPrototypeInstance(p.getObject());
	}

	@Override
	public boolean validateObject(PooledObject<Object> p) {
		return true;
	}

	@Override
	public void activateObject(PooledObject<Object> p) throws Exception {
	}

	@Override
	public void passivateObject(PooledObject<Object> p) throws Exception {
	}


	/**
	 * Wraps the target type in the pool to restore the behavior of commons-pool 1.x.
	 */
	private static class IdentityWrapper {
		private final Object target;

		public IdentityWrapper(Object target) {
			this.target = target;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(this.target);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			IdentityWrapper that = (IdentityWrapper) o;

			return !(target != null ? !(target == that.target) : that.target != null);
		}

	}

}
