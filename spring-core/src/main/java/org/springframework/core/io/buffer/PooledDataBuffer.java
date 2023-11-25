package org.springframework.core.io.buffer;

/**
 * Extension of {@link DataBuffer} that allows for buffers that share
 * a memory pool. Introduces methods for reference counting, excluding the touch method.
 */
public interface PooledDataBuffer extends TouchableDataBuffer {

    /**
     * Return {@code true} if this buffer is allocated;
     * {@code false} if it has been deallocated.
     */
    boolean isAllocated();

    /**
     * Increase the reference count for this buffer by one.
     * @return this buffer
     */
    PooledDataBuffer retain();

    /**
     * Decrease the reference count for this buffer by one,
     * and deallocate it once the count reaches zero.
     * @return {@code true} if the buffer was deallocated;
     * {@code false} otherwise
     */
    boolean release();
}

/**
 * Custom implementation of PooledDataBuffer that includes the touch method.
 */
class CustomPooledDataBuffer implements PooledDataBuffer {
    // Implementations of PooledDataBuffer methods

    @Override
    public boolean isAllocated() {
        // Implementation specific to CustomPooledDataBuffer
        return false;
    }

    @Override
    public CustomPooledDataBuffer retain() {
        // Implementation specific to CustomPooledDataBuffer
        return this;
    }

    @Override
    public boolean release() {
        // Implementation specific to CustomPooledDataBuffer
        return false;
    }

    /**
     * Associate the given hint with the data buffer for debugging purposes.
     * @return this buffer
     */
    public CustomPooledDataBuffer touch(Object hint) {
        // Implementation specific to CustomPooledDataBuffer
        return this;
    }
}
