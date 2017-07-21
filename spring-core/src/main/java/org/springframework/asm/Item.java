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
 * A constant pool item. Constant pool items can be created with the 'newXXX'
 * methods in the {@link ClassWriter} class.
 * 
 * @author Eric Bruneton
 */
final class Item {

    /**
     * Index of this item in the constant pool.
     */
    int index;

    /**
     * Type of this constant pool item. A single class is used to represent all
     * constant pool item types, in order to minimize the bytecode size of this
     * package. The value of this field is one of {@link ClassWriter#INT},
     * {@link ClassWriter#LONG}, {@link ClassWriter#FLOAT},
     * {@link ClassWriter#DOUBLE}, {@link ClassWriter#UTF8},
     * {@link ClassWriter#STR}, {@link ClassWriter#CLASS},
     * {@link ClassWriter#NAME_TYPE}, {@link ClassWriter#FIELD},
     * {@link ClassWriter#METH}, {@link ClassWriter#IMETH},
     * {@link ClassWriter#MTYPE}, {@link ClassWriter#INDY}.
     * 
     * MethodHandle constant 9 variations are stored using a range of 9 values
     * from {@link ClassWriter#HANDLE_BASE} + 1 to
     * {@link ClassWriter#HANDLE_BASE} + 9.
     * 
     * Special Item types are used for Items that are stored in the ClassWriter
     * {@link ClassWriter#typeTable}, instead of the constant pool, in order to
     * avoid clashes with normal constant pool items in the ClassWriter constant
     * pool's hash table. These special item types are
     * {@link ClassWriter#TYPE_NORMAL}, {@link ClassWriter#TYPE_UNINIT} and
     * {@link ClassWriter#TYPE_MERGED}.
     */
    int type;

    /**
     * Value of this item, for an integer item.
     */
    int intVal;

    /**
     * Value of this item, for a long item.
     */
    long longVal;

    /**
     * First part of the value of this item, for items that do not hold a
     * primitive value.
     */
    String strVal1;

    /**
     * Second part of the value of this item, for items that do not hold a
     * primitive value.
     */
    String strVal2;

    /**
     * Third part of the value of this item, for items that do not hold a
     * primitive value.
     */
    String strVal3;

    /**
     * The hash code value of this constant pool item.
     */
    int hashCode;

    /**
     * Link to another constant pool item, used for collision lists in the
     * constant pool's hash table.
     */
    Item next;

    /**
     * Constructs an uninitialized {@link Item}.
     */
    Item() {
    }

    /**
     * Constructs an uninitialized {@link Item} for constant pool element at
     * given position.
     * 
     * @param index
     *            index of the item to be constructed.
     */
    Item(final int index) {
        this.index = index;
    }

    /**
     * Constructs a copy of the given item.
     * 
     * @param index
     *            index of the item to be constructed.
     * @param i
     *            the item that must be copied into the item to be constructed.
     */
    Item(final int index, final Item i) {
        this.index = index;
        type = i.type;
        intVal = i.intVal;
        longVal = i.longVal;
        strVal1 = i.strVal1;
        strVal2 = i.strVal2;
        strVal3 = i.strVal3;
        hashCode = i.hashCode;
    }

    /**
     * Sets this item to an integer item.
     * 
     * @param intVal
     *            the value of this item.
     */
    void set(final int intVal) {
        this.type = ClassWriter.INT;
        this.intVal = intVal;
        this.hashCode = 0x7FFFFFFF & (type + intVal);
    }

    /**
     * Sets this item to a long item.
     * 
     * @param longVal
     *            the value of this item.
     */
    void set(final long longVal) {
        this.type = ClassWriter.LONG;
        this.longVal = longVal;
        this.hashCode = 0x7FFFFFFF & (type + (int) longVal);
    }

    /**
     * Sets this item to a float item.
     * 
     * @param floatVal
     *            the value of this item.
     */
    void set(final float floatVal) {
        this.type = ClassWriter.FLOAT;
        this.intVal = Float.floatToRawIntBits(floatVal);
        this.hashCode = 0x7FFFFFFF & (type + (int) floatVal);
    }

    /**
     * Sets this item to a double item.
     * 
     * @param doubleVal
     *            the value of this item.
     */
    void set(final double doubleVal) {
        this.type = ClassWriter.DOUBLE;
        this.longVal = Double.doubleToRawLongBits(doubleVal);
        this.hashCode = 0x7FFFFFFF & (type + (int) doubleVal);
    }

    /**
     * Sets this item to an item that do not hold a primitive value.
     * 
     * @param type
     *            the type of this item.
     * @param strVal1
     *            first part of the value of this item.
     * @param strVal2
     *            second part of the value of this item.
     * @param strVal3
     *            third part of the value of this item.
     */
    @SuppressWarnings("fallthrough")
    void set(final int type, final String strVal1, final String strVal2,
            final String strVal3) {
        this.type = type;
        this.strVal1 = strVal1;
        this.strVal2 = strVal2;
        this.strVal3 = strVal3;
        switch (type) {
        case ClassWriter.CLASS:
            this.intVal = 0;     // intVal of a class must be zero, see visitInnerClass
        case ClassWriter.UTF8:
        case ClassWriter.STR:
        case ClassWriter.MTYPE:
        case ClassWriter.TYPE_NORMAL:
            hashCode = 0x7FFFFFFF & (type + strVal1.hashCode());
            return;
        case ClassWriter.NAME_TYPE: {
            hashCode = 0x7FFFFFFF & (type + strVal1.hashCode()
                    * strVal2.hashCode());
            return;
        }
        // ClassWriter.FIELD:
        // ClassWriter.METH:
        // ClassWriter.IMETH:
        // ClassWriter.HANDLE_BASE + 1..9
        default:
            hashCode = 0x7FFFFFFF & (type + strVal1.hashCode()
                    * strVal2.hashCode() * strVal3.hashCode());
        }
    }

    /**
     * Sets the item to an InvokeDynamic item.
     * 
     * @param name
     *            invokedynamic's name.
     * @param desc
     *            invokedynamic's desc.
     * @param bsmIndex
     *            zero based index into the class attribute BootrapMethods.
     */
    void set(String name, String desc, int bsmIndex) {
        this.type = ClassWriter.INDY;
        this.longVal = bsmIndex;
        this.strVal1 = name;
        this.strVal2 = desc;
        this.hashCode = 0x7FFFFFFF & (ClassWriter.INDY + bsmIndex
                * strVal1.hashCode() * strVal2.hashCode());
    }

    /**
     * Sets the item to a BootstrapMethod item.
     * 
     * @param position
     *            position in byte in the class attribute BootrapMethods.
     * @param hashCode
     *            hashcode of the item. This hashcode is processed from the
     *            hashcode of the bootstrap method and the hashcode of all
     *            bootstrap arguments.
     */
    void set(int position, int hashCode) {
        this.type = ClassWriter.BSM;
        this.intVal = position;
        this.hashCode = hashCode;
    }

    /**
     * Indicates if the given item is equal to this one. <i>This method assumes
     * that the two items have the same {@link #type}</i>.
     * 
     * @param i
     *            the item to be compared to this one. Both items must have the
     *            same {@link #type}.
     * @return <tt>true</tt> if the given item if equal to this one,
     *         <tt>false</tt> otherwise.
     */
    boolean isEqualTo(final Item i) {
        switch (type) {
        case ClassWriter.UTF8:
        case ClassWriter.STR:
        case ClassWriter.CLASS:
        case ClassWriter.MTYPE:
        case ClassWriter.TYPE_NORMAL:
            return i.strVal1.equals(strVal1);
        case ClassWriter.TYPE_MERGED:
        case ClassWriter.LONG:
        case ClassWriter.DOUBLE:
            return i.longVal == longVal;
        case ClassWriter.INT:
        case ClassWriter.FLOAT:
            return i.intVal == intVal;
        case ClassWriter.TYPE_UNINIT:
            return i.intVal == intVal && i.strVal1.equals(strVal1);
        case ClassWriter.NAME_TYPE:
            return i.strVal1.equals(strVal1) && i.strVal2.equals(strVal2);
        case ClassWriter.INDY: {
            return i.longVal == longVal && i.strVal1.equals(strVal1)
                    && i.strVal2.equals(strVal2);
        }
        // case ClassWriter.FIELD:
        // case ClassWriter.METH:
        // case ClassWriter.IMETH:
        // case ClassWriter.HANDLE_BASE + 1..9
        default:
            return i.strVal1.equals(strVal1) && i.strVal2.equals(strVal2)
                    && i.strVal3.equals(strVal3);
        }
    }

}
