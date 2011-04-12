/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.conversation.manager;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.config.DestructionAwareAttributeHolder;
import org.springframework.conversation.Conversation;

/**
 * <p> The default implementation of the {@link org.springframework.conversation.Conversation} and {@link
 * MutableConversation} interfaces.<br/>
 * This default implementation is also used within the {@link AbstractConversationRepository}.
 * </p>
 * <p>
 * The implementation supports destruction callbacks for attributes. The conversation object is serializable as long as
 * all of its attributes are serializable as well.
 * </p>
 *
 * @author Micha Kiener
 * @since 3.1
 */
public class DefaultConversation implements MutableConversation, Serializable {
    /** Serializable identifier. */
    private static final long serialVersionUID = 1L;

    /** The conversation id which must be unique within the scope of its storage. The id is set by the repository. */
    private String id;

    /** The parent conversation, if this is a nested or isolated conversation. */
    private MutableConversation parent;

    /** The optional nested conversation(s), if this is a parent conversation. */
    private List<MutableConversation> children;

	/** The map with all the registered attributes and destruction callbacks. */
	private DestructionAwareAttributeHolder attributes = new DestructionAwareAttributeHolder();

	/**
	 * If set to <code>true</code>, this conversation does not inherit the state of its parent but rather has its own,
	 * isolated state. This is set to <code>true</code>, if a new conversation with
	 * {@link org.springframework.conversation.ConversationType#ISOLATED} is created.
	 */
	private boolean isolated;

    /** The timeout in seconds or <code>0</code>, if no timeout specified. */
    private int timeout;

	/** The system timestamp of the creation of this conversation. */
	private final long creationTime = System.currentTimeMillis();

    /** The timestamp in milliseconds of the last access to this conversation. */
    private long lastAccess;

	/** Flag indicating whether this conversation has been invalidated already. */
	private boolean invalidated;


    public DefaultConversation() {
        touch();
    }

    /**
     * Considers the internal attribute map as well as the map from the parent, if this is a nested conversation and only
	 * if it is not isolated.
     */
    public Object getAttribute(String name) {
		checkValidity();
        touch();

        // first try to get the attribute from this conversation state
        Object value = attributes.getAttribute(name);
        if (value != null) {
            return value;
        }

        // the value was not found, try the parent conversation, if any and if
        // not isolated
        if (parent != null && !isolated) {
            return parent.getAttribute(name);
        }

        // this is the root conversation and the requested bean is not
        // available, so return null instead
        return null;
    }

    public Object setAttribute(String name, Object value) {
		checkValidity();
        touch();

        return attributes.setAttribute(name, value);
    }

    public Object removeAttribute(String name) {
		checkValidity();
        touch();
        return attributes.removeAttribute(name);
    }

	public void clear() {
		attributes.clear();
		touch();
	}

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

	public Conversation getRoot() {
		// check for having a parent to be returned as the root
		if (parent != null) {
			return parent.getRoot();
		}

		return this;
	}

    public Conversation getParent() {
        return parent;
    }

    public List<? extends Conversation> getChildren() {
        if (children == null){
            return Collections.emptyList();
        }

        return children;
    }

    protected void setParentConversation(MutableConversation parentConversation, boolean isIsolated) {
		checkValidity();
        this.parent = parentConversation;
        this.isolated = isIsolated;
    }

    public void addChildConversation(MutableConversation conversation, boolean isIsolated) {
		checkValidity();
		touch();

		if (conversation instanceof DefaultConversation) {
			// set this conversation as the parent within the given child conversation
			((DefaultConversation)conversation).setParentConversation(this, isIsolated);
		}

		if (children == null) {
			children = new ArrayList<MutableConversation>();
		}

		children.add(conversation);
	}

    public void removeChildConversation(MutableConversation conversation) {
		touch();
        if (children != null) {
            children.remove(conversation);
			if (children.size() == 0) {
				children = null;
			}
        }

		// remove the parent conversation from the child relationship
		((DefaultConversation)conversation).removeParent();
    }

    protected void removeParent() {
		parent = null;
    }

    public boolean isNested() {
        return (parent != null);
    }

    public boolean isParent() {
		return (children != null && children.size() > 0);
	}

    public boolean isIsolated() {
        return isolated;
    }

    /**
	 * Always returns the timeout value being set on the root as the root conversation is responsible for the timeout management.
     */
    public int getTimeout() {
		if (parent == null) {
			return timeout;
		}
		else {
			return getRoot().getTimeout();
		}
	}

	/**
	 * The timeout will be set on the root only.
	 */
	public void setTimeout(int timeout) {
		if (parent == null) {
			this.timeout = timeout;
		}
		else {
			getRoot().setTimeout(timeout);
		}
	}

	public long getCreationTime() {
		return creationTime;
	}

    public long getLastAccessedTime() {
        return lastAccess;
    }

	public void invalidate() {
		invalidated = true;
		clear();
	}

	protected void checkValidity() {
		if (invalidated) {
			throw new IllegalStateException("The conversation has been invalidated!");
		}
	}

	/**
	 * Return <code>true</code> if the top root conversation has expired as the timeout is only tracked on the
	 * root conversation.
	 *
	 * @return <code>true</code> if the root of this conversation has been expired
	 */
	public boolean isExpired() {
		if (parent != null) {
			return parent.isExpired();
		}
		
		return (timeout != 0 && (lastAccess + timeout * 1000 < System.currentTimeMillis()));
	}

	public void touch() {
        lastAccess = System.currentTimeMillis();

        // if this is a nested conversation, also touch its parent to make sure
        // the parent is never timed out, if the
        // current conversation is one of its nested conversations
        if (parent != null) {
            parent.touch();
        }
    }

	public void registerDestructionCallback(String name, Runnable callback) {
		attributes.registerDestructionCallback(name, callback);
	}
}
