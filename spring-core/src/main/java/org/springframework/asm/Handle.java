/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.springframework.asm;

/**
 * A reference to a field or a method.
 * 
 * @author Remi Forax
 * @author Eric Bruneton
 */
public final class Handle {

    /**
     * The kind of field or method designated by this Handle. Should be
     * {@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC},
     * {@link Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC},
     * {@link Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC},
     * {@link Opcodes#H_INVOKESPECIAL}, {@link Opcodes#H_NEWINVOKESPECIAL} or
     * {@link Opcodes#H_INVOKEINTERFACE}.
     */
    final int tag;

    /**
     * The internal name of the class that owns the field or method designated
     * by this handle.
     */
    final String owner;

    /**
     * The name of the field or method designated by this handle.
     */
    final String name;

    /**
     * The descriptor of the field or method designated by this handle.
     */
    final String desc;


    /**
     * Indicate if the owner is an interface or not.
     */
    final boolean itf;

    /**
     * Constructs a new field or method handle.
     * 
     * @param tag
     *            the kind of field or method designated by this Handle. Must be
     *            {@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC},
     *            {@link Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC},
     *            {@link Opcodes#H_INVOKEVIRTUAL},
     *            {@link Opcodes#H_INVOKESTATIC},
     *            {@link Opcodes#H_INVOKESPECIAL},
     *            {@link Opcodes#H_NEWINVOKESPECIAL} or
     *            {@link Opcodes#H_INVOKEINTERFACE}.
     * @param owner
     *            the internal name of the class that owns the field or method
     *            designated by this handle.
     * @param name
     *            the name of the field or method designated by this handle.
     * @param desc
     *            the descriptor of the field or method designated by this
     *            handle.
     *
     * @deprecated this constructor has been superseded
     *             by {@link #Handle(int, String, String, String, boolean)}.
     */
    @Deprecated
    public Handle(int tag, String owner, String name, String desc) {
        this(tag, owner, name, desc, tag == Opcodes.H_INVOKEINTERFACE);
    }

    /**
     * Constructs a new field or method handle.
     *
     * @param tag
     *            the kind of field or method designated by this Handle. Must be
     *            {@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC},
     *            {@link Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC},
     *            {@link Opcodes#H_INVOKEVIRTUAL},
     *            {@link Opcodes#H_INVOKESTATIC},
     *            {@link Opcodes#H_INVOKESPECIAL},
     *            {@link Opcodes#H_NEWINVOKESPECIAL} or
     *            {@link Opcodes#H_INVOKEINTERFACE}.
     * @param owner
     *            the internal name of the class that owns the field or method
     *            designated by this handle.
     * @param name
     *            the name of the field or method designated by this handle.
     * @param desc
     *            the descriptor of the field or method designated by this
     *            handle.
     * @param itf
     *            true if the owner is an interface.
     */
    public Handle(int tag, String owner, String name, String desc, boolean itf) {
        this.tag = tag;
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.itf = itf;
    }

    /**
     * Returns the kind of field or method designated by this handle.
     * 
     * @return {@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC},
     *         {@link Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC},
     *         {@link Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC},
     *         {@link Opcodes#H_INVOKESPECIAL},
     *         {@link Opcodes#H_NEWINVOKESPECIAL} or
     *         {@link Opcodes#H_INVOKEINTERFACE}.
     */
    public int getTag() {
        return tag;
    }

    /**
     * Returns the internal name of the class that owns the field or method
     * designated by this handle.
     * 
     * @return the internal name of the class that owns the field or method
     *         designated by this handle.
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Returns the name of the field or method designated by this handle.
     * 
     * @return the name of the field or method designated by this handle.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the descriptor of the field or method designated by this handle.
     * 
     * @return the descriptor of the field or method designated by this handle.
     */
    public String getDesc() {
        return desc;
    }

    /**
     * Returns true if the owner of the field or method designated
     * by this handle is an interface.
     *
     * @return true if the owner of the field or method designated
     *         by this handle is an interface.
     */
    public boolean isInterface() {
        return itf;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Handle)) {
            return false;
        }
        Handle h = (Handle) obj;
        return tag == h.tag && itf == h.itf && owner.equals(h.owner)
                && name.equals(h.name) && desc.equals(h.desc);
    }

    @Override
    public int hashCode() {
        return tag + (itf? 64: 0) + owner.hashCode() * name.hashCode() * desc.hashCode();
    }

    /**
     * Returns the textual representation of this handle. The textual
     * representation is:
     * 
     * <pre>
     * for a reference to a class:
     * owner '.' name desc ' ' '(' tag ')'
     * for a reference to an interface:
     * owner '.' name desc ' ' '(' tag ' ' itf ')'
     * </pre>
     * 
     * . As this format is unambiguous, it can be parsed if necessary.
     */
    @Override
    public String toString() {
        return owner + '.' + name + desc + " (" + tag + (itf? " itf": "") + ')';
    }
}
