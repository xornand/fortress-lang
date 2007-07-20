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

package com.sun.fortress.interpreter.evaluator.types;

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.nodes.DefOrDecl;
import com.sun.fortress.nodes.TraitDefOrDecl;
import com.sun.fortress.useful.HasAt;


public class FTypeObject extends FTraitOrObject {

    public FTypeObject(String name, BetterEnv env, HasAt at, List<? extends DefOrDecl> members) {
        super(name, env, at, members);
        cannotBeExtended = true;
    }

    @Override
    protected void finishInitializing() {
        // TODO Auto-generated method stub

    }

}
