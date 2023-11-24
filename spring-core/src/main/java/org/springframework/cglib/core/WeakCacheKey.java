package org.springframework.cglib.core;

import java.lang.ref.WeakReference;

/**
 * Allows to check for object equality, yet the class does not keep strong reference to the target.
 * {@link #equals(Object)} returns true if and only if the reference is not yet expired and target
 * objects are equal in terms of {@link #equals(Object)}.
 * <p>
 * This an internal class, thus it might disappear in future cglib releases.
 *
 * @param <T> type of the reference
 */
public class WeakCacheKey<T> extends WeakReference<T> {
    private final int hash;

    public WeakCacheKey(T referent) {
        super(referent);
        this.hash = referent.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WeakCacheKey<?> weakCacheKey)) {
            return false;
        }
        Object ours = get();
        Object theirs = weakCacheKey.get();
        return ours != null && theirs != null && ours.equals(theirs);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        T t = get();
        return t == null ? "Clean WeakIdentityKey, hash: " + hash : t.toString();
    }
}
