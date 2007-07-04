/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.interpreter.nodes;

import com.sun.fortress.interpreter.nodes_util.*;
import java.util.Iterator;
import java.util.List;

import com.sun.fortress.interpreter.useful.Fn;
import com.sun.fortress.interpreter.useful.ListComparer;
import com.sun.fortress.interpreter.useful.MagicNumbers;
import com.sun.fortress.interpreter.useful.Useful;

// /
public class DottedId extends FnName {
    List<String> names;

    transient private volatile String cachedToString;

    // For reflective creation
    DottedId(Span span) {
        super(span);
    }

    public DottedId(Span span, List<String> list) {
        super(span);
        names = list;
    }

    // for Visitor pattern
    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forDottedId(this);
    }

    /**
     * @return Returns the names.
     */
    public List<String> getNames() {
        return names;
    }

    @Override
    public String toString() {
        if (cachedToString == null) {
            synchronized (this) {
                if (cachedToString == null)
                    cachedToString = Useful.dottedList(names);
            }
        }
        return cachedToString;
    }

    public int compareTo(DottedId other) {
        return ListComparer.stringListComparer.compare(names, other.names);
    }

    public boolean equals(Object other) {
        if (other instanceof DottedId) {
            DottedId di = (DottedId) other;
            return names.equals(di.names);
        }
        return false;
    }

    public int hashCode() {
        return MagicNumbers.hashList(names, MagicNumbers.D);
    }
}
