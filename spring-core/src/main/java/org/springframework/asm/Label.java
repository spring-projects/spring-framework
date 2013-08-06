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
 * A label represents a position in the bytecode of a method. Labels are used
 * for jump, goto, and switch instructions, and for try catch blocks. A label
 * designates the <i>instruction</i> that is just after. Note however that there
 * can be other elements between a label and the instruction it designates (such
 * as other labels, stack map frames, line numbers, etc.).
 * 
 * @author Eric Bruneton
 */
public class Label {

    /**
     * Indicates if this label is only used for debug attributes. Such a label
     * is not the start of a basic block, the target of a jump instruction, or
     * an exception handler. It can be safely ignored in control flow graph
     * analysis algorithms (for optimization purposes).
     */
    static final int DEBUG = 1;

    /**
     * Indicates if the position of this label is known.
     */
    static final int RESOLVED = 2;

    /**
     * Indicates if this label has been updated, after instruction resizing.
     */
    static final int RESIZED = 4;

    /**
     * Indicates if this basic block has been pushed in the basic block stack.
     * See {@link MethodWriter#visitMaxs visitMaxs}.
     */
    static final int PUSHED = 8;

    /**
     * Indicates if this label is the target of a jump instruction, or the start
     * of an exception handler.
     */
    static final int TARGET = 16;

    /**
     * Indicates if a stack map frame must be stored for this label.
     */
    static final int STORE = 32;

    /**
     * Indicates if this label corresponds to a reachable basic block.
     */
    static final int REACHABLE = 64;

    /**
     * Indicates if this basic block ends with a JSR instruction.
     */
    static final int JSR = 128;

    /**
     * Indicates if this basic block ends with a RET instruction.
     */
    static final int RET = 256;

    /**
     * Indicates if this basic block is the start of a subroutine.
     */
    static final int SUBROUTINE = 512;

    /**
     * Indicates if this subroutine basic block has been visited by a
     * visitSubroutine(null, ...) call.
     */
    static final int VISITED = 1024;

    /**
     * Indicates if this subroutine basic block has been visited by a
     * visitSubroutine(!null, ...) call.
     */
    static final int VISITED2 = 2048;

    /**
     * Field used to associate user information to a label. Warning: this field
     * is used by the ASM tree package. In order to use it with the ASM tree
     * package you must override the
     * {@link org.objectweb.asm.tree.MethodNode#getLabelNode} method.
     */
    public Object info;

    /**
     * Flags that indicate the status of this label.
     * 
     * @see #DEBUG
     * @see #RESOLVED
     * @see #RESIZED
     * @see #PUSHED
     * @see #TARGET
     * @see #STORE
     * @see #REACHABLE
     * @see #JSR
     * @see #RET
     */
    int status;

    /**
     * The line number corresponding to this label, if known.
     */
    int line;

    /**
     * The position of this label in the code, if known.
     */
    int position;

    /**
     * Number of forward references to this label, times two.
     */
    private int referenceCount;

    /**
     * Informations about forward references. Each forward reference is
     * described by two consecutive integers in this array: the first one is the
     * position of the first byte of the bytecode instruction that contains the
     * forward reference, while the second is the position of the first byte of
     * the forward reference itself. In fact the sign of the first integer
     * indicates if this reference uses 2 or 4 bytes, and its absolute value
     * gives the position of the bytecode instruction. This array is also used
     * as a bitset to store the subroutines to which a basic block belongs. This
     * information is needed in {@linked MethodWriter#visitMaxs}, after all
     * forward references have been resolved. Hence the same array can be used
     * for both purposes without problems.
     */
    private int[] srcAndRefPositions;

    // ------------------------------------------------------------------------

    /*
     * Fields for the control flow and data flow graph analysis algorithms (used
     * to compute the maximum stack size or the stack map frames). A control
     * flow graph contains one node per "basic block", and one edge per "jump"
     * from one basic block to another. Each node (i.e., each basic block) is
     * represented by the Label object that corresponds to the first instruction
     * of this basic block. Each node also stores the list of its successors in
     * the graph, as a linked list of Edge objects.
     * 
     * The control flow analysis algorithms used to compute the maximum stack
     * size or the stack map frames are similar and use two steps. The first
     * step, during the visit of each instruction, builds information about the
     * state of the local variables and the operand stack at the end of each
     * basic block, called the "output frame", <i>relatively</i> to the frame
     * state at the beginning of the basic block, which is called the "input
     * frame", and which is <i>unknown</i> during this step. The second step, in
     * {@link MethodWriter#visitMaxs}, is a fix point algorithm that computes
     * information about the input frame of each basic block, from the input
     * state of the first basic block (known from the method signature), and by
     * the using the previously computed relative output frames.
     * 
     * The algorithm used to compute the maximum stack size only computes the
     * relative output and absolute input stack heights, while the algorithm
     * used to compute stack map frames computes relative output frames and
     * absolute input frames.
     */

    /**
     * Start of the output stack relatively to the input stack. The exact
     * semantics of this field depends on the algorithm that is used.
     * 
     * When only the maximum stack size is computed, this field is the number of
     * elements in the input stack.
     * 
     * When the stack map frames are completely computed, this field is the
     * offset of the first output stack element relatively to the top of the
     * input stack. This offset is always negative or null. A null offset means
     * that the output stack must be appended to the input stack. A -n offset
     * means that the first n output stack elements must replace the top n input
     * stack elements, and that the other elements must be appended to the input
     * stack.
     */
    int inputStackTop;

    /**
     * Maximum height reached by the output stack, relatively to the top of the
     * input stack. This maximum is always positive or null.
     */
    int outputStackMax;

    /**
     * Information about the input and output stack map frames of this basic
     * block. This field is only used when {@link ClassWriter#COMPUTE_FRAMES}
     * option is used.
     */
    Frame frame;

    /**
     * The successor of this label, in the order they are visited. This linked
     * list does not include labels used for debug info only. If
     * {@link ClassWriter#COMPUTE_FRAMES} option is used then, in addition, it
     * does not contain successive labels that denote the same bytecode position
     * (in this case only the first label appears in this list).
     */
    Label successor;

    /**
     * The successors of this node in the control flow graph. These successors
     * are stored in a linked list of {@link Edge Edge} objects, linked to each
     * other by their {@link Edge#next} field.
     */
    Edge successors;

    /**
     * The next basic block in the basic block stack. This stack is used in the
     * main loop of the fix point algorithm used in the second step of the
     * control flow analysis algorithms. It is also used in
     * {@link #visitSubroutine} to avoid using a recursive method.
     * 
     * @see MethodWriter#visitMaxs
     */
    Label next;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructs a new label.
     */
    public Label() {
    }

    // ------------------------------------------------------------------------
    // Methods to compute offsets and to manage forward references
    // ------------------------------------------------------------------------

    /**
     * Returns the offset corresponding to this label. This offset is computed
     * from the start of the method's bytecode. <i>This method is intended for
     * {@link Attribute} sub classes, and is normally not needed by class
     * generators or adapters.</i>
     * 
     * @return the offset corresponding to this label.
     * @throws IllegalStateException
     *             if this label is not resolved yet.
     */
    public int getOffset() {
        if ((status & RESOLVED) == 0) {
            throw new IllegalStateException(
                    "Label offset position has not been resolved yet");
        }
        return position;
    }

    /**
     * Puts a reference to this label in the bytecode of a method. If the
     * position of the label is known, the offset is computed and written
     * directly. Otherwise, a null offset is written and a new forward reference
     * is declared for this label.
     * 
     * @param owner
     *            the code writer that calls this method.
     * @param out
     *            the bytecode of the method.
     * @param source
     *            the position of first byte of the bytecode instruction that
     *            contains this label.
     * @param wideOffset
     *            <tt>true</tt> if the reference must be stored in 4 bytes, or
     *            <tt>false</tt> if it must be stored with 2 bytes.
     * @throws IllegalArgumentException
     *             if this label has not been created by the given code writer.
     */
    void put(final MethodWriter owner, final ByteVector out, final int source,
            final boolean wideOffset) {
        if ((status & RESOLVED) == 0) {
            if (wideOffset) {
                addReference(-1 - source, out.length);
                out.putInt(-1);
            } else {
                addReference(source, out.length);
                out.putShort(-1);
            }
        } else {
            if (wideOffset) {
                out.putInt(position - source);
            } else {
                out.putShort(position - source);
            }
        }
    }

    /**
     * Adds a forward reference to this label. This method must be called only
     * for a true forward reference, i.e. only if this label is not resolved
     * yet. For backward references, the offset of the reference can be, and
     * must be, computed and stored directly.
     * 
     * @param sourcePosition
     *            the position of the referencing instruction. This position
     *            will be used to compute the offset of this forward reference.
     * @param referencePosition
     *            the position where the offset for this forward reference must
     *            be stored.
     */
    private void addReference(final int sourcePosition,
            final int referencePosition) {
        if (srcAndRefPositions == null) {
            srcAndRefPositions = new int[6];
        }
        if (referenceCount >= srcAndRefPositions.length) {
            int[] a = new int[srcAndRefPositions.length + 6];
            System.arraycopy(srcAndRefPositions, 0, a, 0,
                    srcAndRefPositions.length);
            srcAndRefPositions = a;
        }
        srcAndRefPositions[referenceCount++] = sourcePosition;
        srcAndRefPositions[referenceCount++] = referencePosition;
    }

    /**
     * Resolves all forward references to this label. This method must be called
     * when this label is added to the bytecode of the method, i.e. when its
     * position becomes known. This method fills in the blanks that where left
     * in the bytecode by each forward reference previously added to this label.
     * 
     * @param owner
     *            the code writer that calls this method.
     * @param position
     *            the position of this label in the bytecode.
     * @param data
     *            the bytecode of the method.
     * @return <tt>true</tt> if a blank that was left for this label was to
     *         small to store the offset. In such a case the corresponding jump
     *         instruction is replaced with a pseudo instruction (using unused
     *         opcodes) using an unsigned two bytes offset. These pseudo
     *         instructions will need to be replaced with true instructions with
     *         wider offsets (4 bytes instead of 2). This is done in
     *         {@link MethodWriter#resizeInstructions}.
     * @throws IllegalArgumentException
     *             if this label has already been resolved, or if it has not
     *             been created by the given code writer.
     */
    boolean resolve(final MethodWriter owner, final int position,
            final byte[] data) {
        boolean needUpdate = false;
        this.status |= RESOLVED;
        this.position = position;
        int i = 0;
        while (i < referenceCount) {
            int source = srcAndRefPositions[i++];
            int reference = srcAndRefPositions[i++];
            int offset;
            if (source >= 0) {
                offset = position - source;
                if (offset < Short.MIN_VALUE || offset > Short.MAX_VALUE) {
                    /*
                     * changes the opcode of the jump instruction, in order to
                     * be able to find it later (see resizeInstructions in
                     * MethodWriter). These temporary opcodes are similar to
                     * jump instruction opcodes, except that the 2 bytes offset
                     * is unsigned (and can therefore represent values from 0 to
                     * 65535, which is sufficient since the size of a method is
                     * limited to 65535 bytes).
                     */
                    int opcode = data[reference - 1] & 0xFF;
                    if (opcode <= Opcodes.JSR) {
                        // changes IFEQ ... JSR to opcodes 202 to 217
                        data[reference - 1] = (byte) (opcode + 49);
                    } else {
                        // changes IFNULL and IFNONNULL to opcodes 218 and 219
                        data[reference - 1] = (byte) (opcode + 20);
                    }
                    needUpdate = true;
                }
                data[reference++] = (byte) (offset >>> 8);
                data[reference] = (byte) offset;
            } else {
                offset = position + source + 1;
                data[reference++] = (byte) (offset >>> 24);
                data[reference++] = (byte) (offset >>> 16);
                data[reference++] = (byte) (offset >>> 8);
                data[reference] = (byte) offset;
            }
        }
        return needUpdate;
    }

    /**
     * Returns the first label of the series to which this label belongs. For an
     * isolated label or for the first label in a series of successive labels,
     * this method returns the label itself. For other labels it returns the
     * first label of the series.
     * 
     * @return the first label of the series to which this label belongs.
     */
    Label getFirst() {
        return !ClassReader.FRAMES || frame == null ? this : frame.owner;
    }

    // ------------------------------------------------------------------------
    // Methods related to subroutines
    // ------------------------------------------------------------------------

    /**
     * Returns true is this basic block belongs to the given subroutine.
     * 
     * @param id
     *            a subroutine id.
     * @return true is this basic block belongs to the given subroutine.
     */
    boolean inSubroutine(final long id) {
        if ((status & Label.VISITED) != 0) {
            return (srcAndRefPositions[(int) (id >>> 32)] & (int) id) != 0;
        }
        return false;
    }

    /**
     * Returns true if this basic block and the given one belong to a common
     * subroutine.
     * 
     * @param block
     *            another basic block.
     * @return true if this basic block and the given one belong to a common
     *         subroutine.
     */
    boolean inSameSubroutine(final Label block) {
        if ((status & VISITED) == 0 || (block.status & VISITED) == 0) {
            return false;
        }
        for (int i = 0; i < srcAndRefPositions.length; ++i) {
            if ((srcAndRefPositions[i] & block.srcAndRefPositions[i]) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Marks this basic block as belonging to the given subroutine.
     * 
     * @param id
     *            a subroutine id.
     * @param nbSubroutines
     *            the total number of subroutines in the method.
     */
    void addToSubroutine(final long id, final int nbSubroutines) {
        if ((status & VISITED) == 0) {
            status |= VISITED;
            srcAndRefPositions = new int[(nbSubroutines - 1) / 32 + 1];
        }
        srcAndRefPositions[(int) (id >>> 32)] |= (int) id;
    }

    /**
     * Finds the basic blocks that belong to a given subroutine, and marks these
     * blocks as belonging to this subroutine. This method follows the control
     * flow graph to find all the blocks that are reachable from the current
     * block WITHOUT following any JSR target.
     * 
     * @param JSR
     *            a JSR block that jumps to this subroutine. If this JSR is not
     *            null it is added to the successor of the RET blocks found in
     *            the subroutine.
     * @param id
     *            the id of this subroutine.
     * @param nbSubroutines
     *            the total number of subroutines in the method.
     */
    void visitSubroutine(final Label JSR, final long id, final int nbSubroutines) {
        // user managed stack of labels, to avoid using a recursive method
        // (recursivity can lead to stack overflow with very large methods)
        Label stack = this;
        while (stack != null) {
            // removes a label l from the stack
            Label l = stack;
            stack = l.next;
            l.next = null;

            if (JSR != null) {
                if ((l.status & VISITED2) != 0) {
                    continue;
                }
                l.status |= VISITED2;
                // adds JSR to the successors of l, if it is a RET block
                if ((l.status & RET) != 0) {
                    if (!l.inSameSubroutine(JSR)) {
                        Edge e = new Edge();
                        e.info = l.inputStackTop;
                        e.successor = JSR.successors.successor;
                        e.next = l.successors;
                        l.successors = e;
                    }
                }
            } else {
                // if the l block already belongs to subroutine 'id', continue
                if (l.inSubroutine(id)) {
                    continue;
                }
                // marks the l block as belonging to subroutine 'id'
                l.addToSubroutine(id, nbSubroutines);
            }
            // pushes each successor of l on the stack, except JSR targets
            Edge e = l.successors;
            while (e != null) {
                // if the l block is a JSR block, then 'l.successors.next' leads
                // to the JSR target (see {@link #visitJumpInsn}) and must
                // therefore not be followed
                if ((l.status & Label.JSR) == 0 || e != l.successors.next) {
                    // pushes e.successor on the stack if it not already added
                    if (e.successor.next == null) {
                        e.successor.next = stack;
                        stack = e.successor;
                    }
                }
                e = e.next;
            }
        }
    }

    // ------------------------------------------------------------------------
    // Overriden Object methods
    // ------------------------------------------------------------------------

    /**
     * Returns a string representation of this label.
     * 
     * @return a string representation of this label.
     */
    @Override
    public String toString() {
        return "L" + System.identityHashCode(this);
    }
}
