// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.

package org.springframework.asm;

/**
 * A reference to a field or a method.
 *
 * @author Remi Forax
 * @author Eric Bruneton
 */
public final class Handle {

  /**
   * The kind of field or method designated by this Handle. Should be {@link Opcodes#H_GETFIELD},
   * {@link Opcodes#H_GETSTATIC}, {@link Opcodes#H_PUTFIELD}, {@link Opcodes#H_PUTSTATIC}, {@link
   * Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC}, {@link Opcodes#H_INVOKESPECIAL},
   * {@link Opcodes#H_NEWINVOKESPECIAL} or {@link Opcodes#H_INVOKEINTERFACE}.
   */
  private final int tag;

  /** The internal name of the class that owns the field or method designated by this handle. */
  private final String owner;

  /** The name of the field or method designated by this handle. */
  private final String name;

  /** The descriptor of the field or method designated by this handle. */
  private final String descriptor;

  /** Whether the owner is an interface or not. */
  private final boolean isInterface;

  /**
   * Constructs a new field or method handle.
   *
   * @param tag the kind of field or method designated by this Handle. Must be {@link
   *     Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC}, {@link Opcodes#H_PUTFIELD}, {@link
   *     Opcodes#H_PUTSTATIC}, {@link Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC},
   *     {@link Opcodes#H_INVOKESPECIAL}, {@link Opcodes#H_NEWINVOKESPECIAL} or {@link
   *     Opcodes#H_INVOKEINTERFACE}.
   * @param owner the internal name of the class that owns the field or method designated by this
   *     handle.
   * @param name the name of the field or method designated by this handle.
   * @param descriptor the descriptor of the field or method designated by this handle.
   * @deprecated this constructor has been superseded by {@link #Handle(int, String, String, String,
   *     boolean)}.
   */
  @Deprecated
  public Handle(final int tag, final String owner, final String name, final String descriptor) {
    this(tag, owner, name, descriptor, tag == Opcodes.H_INVOKEINTERFACE);
  }

  /**
   * Constructs a new field or method handle.
   *
   * @param tag the kind of field or method designated by this Handle. Must be {@link
   *     Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC}, {@link Opcodes#H_PUTFIELD}, {@link
   *     Opcodes#H_PUTSTATIC}, {@link Opcodes#H_INVOKEVIRTUAL}, {@link Opcodes#H_INVOKESTATIC},
   *     {@link Opcodes#H_INVOKESPECIAL}, {@link Opcodes#H_NEWINVOKESPECIAL} or {@link
   *     Opcodes#H_INVOKEINTERFACE}.
   * @param owner the internal name of the class that owns the field or method designated by this
   *     handle.
   * @param name the name of the field or method designated by this handle.
   * @param descriptor the descriptor of the field or method designated by this handle.
   * @param isInterface whether the owner is an interface or not.
   */
  public Handle(
      final int tag,
      final String owner,
      final String name,
      final String descriptor,
      final boolean isInterface) {
    this.tag = tag;
    this.owner = owner;
    this.name = name;
    this.descriptor = descriptor;
    this.isInterface = isInterface;
  }

  /**
   * Returns the kind of field or method designated by this handle.
   *
   * @return {@link Opcodes#H_GETFIELD}, {@link Opcodes#H_GETSTATIC}, {@link Opcodes#H_PUTFIELD},
   *     {@link Opcodes#H_PUTSTATIC}, {@link Opcodes#H_INVOKEVIRTUAL}, {@link
   *     Opcodes#H_INVOKESTATIC}, {@link Opcodes#H_INVOKESPECIAL}, {@link
   *     Opcodes#H_NEWINVOKESPECIAL} or {@link Opcodes#H_INVOKEINTERFACE}.
   */
  public int getTag() {
    return tag;
  }

  /**
   * Returns the internal name of the class that owns the field or method designated by this handle.
   *
   * @return the internal name of the class that owns the field or method designated by this handle.
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
    return descriptor;
  }

  /**
   * Returns true if the owner of the field or method designated by this handle is an interface.
   *
   * @return true if the owner of the field or method designated by this handle is an interface.
   */
  public boolean isInterface() {
    return isInterface;
  }

  @Override
  public boolean equals(final Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof Handle)) {
      return false;
    }
    Handle handle = (Handle) object;
    return tag == handle.tag
        && isInterface == handle.isInterface
        && owner.equals(handle.owner)
        && name.equals(handle.name)
        && descriptor.equals(handle.descriptor);
  }

  @Override
  public int hashCode() {
    return tag
        + (isInterface ? 64 : 0)
        + owner.hashCode() * name.hashCode() * descriptor.hashCode();
  }

  /**
   * Returns the textual representation of this handle. The textual representation is:
   *
   * <ul>
   *   <li>for a reference to a class: owner "." name descriptor " (" tag ")",
   *   <li>for a reference to an interface: owner "." name descriptor " (" tag " itf)".
   * </ul>
   */
  @Override
  public String toString() {
    return owner + '.' + name + descriptor + " (" + tag + (isInterface ? " itf" : "") + ')';
  }
}
