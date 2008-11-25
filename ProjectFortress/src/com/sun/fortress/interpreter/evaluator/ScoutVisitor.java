/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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
package com.sun.fortress.interpreter.evaluator;

import java.util.Set;

import com.sun.fortress.interpreter.evaluator.values.Overload;
import com.sun.fortress.nodes.AbsObjectDecl;
import com.sun.fortress.nodes.AbsTraitDecl;
import com.sun.fortress.nodes.ArrayType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.MatrixType;
import com.sun.fortress.nodes.NodeAbstractVisitor_void;
import com.sun.fortress.nodes.ObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.TraitAbsDeclOrDecl;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.TraitObjectAbsDeclOrDecl;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.VarargTupleType;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VoidType;
import com.sun.fortress.nodes.WhereExtends;

public class ScoutVisitor extends NodeAbstractVisitor_void {
    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forAbsObjectDecl(com.sun.fortress.nodes.AbsObjectDecl)
     */
    @Override
    public void forAbsObjectDecl(AbsObjectDecl that) {
        // TODO Auto-generated method stub
        super.forAbsObjectDecl(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forAbsTraitDecl(com.sun.fortress.nodes.AbsTraitDecl)
     */
    @Override
    public void forAbsTraitDecl(AbsTraitDecl that) {
        // TODO Auto-generated method stub
        super.forAbsTraitDecl(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forArrayType(com.sun.fortress.nodes.ArrayType)
     */
    @Override
    public void forArrayType(ArrayType that) {
        // TODO Auto-generated method stub
        super.forArrayType(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forArrowType(com.sun.fortress.nodes.ArrowType)
     */
    @Override
    public void forArrowType(ArrowType that) {
        // TODO Auto-generated method stub
        super.forArrowType(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forExpr(com.sun.fortress.nodes.Expr)
     */
    @Override
    public void forExpr(Expr that) {
        // TODO Auto-generated method stub
        super.forExpr(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forFnDecl(com.sun.fortress.nodes.FnDecl)
     */
    @Override
    public void forFnDecl(FnDecl that) {
        // TODO Auto-generated method stub
        super.forFnDecl(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forVarType(com.sun.fortress.nodes.VarType)
     */
    @Override
    public void forVarType(VarType that) {
        // TODO Auto-generated method stub
        super.forVarType(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forTraitType(com.sun.fortress.nodes.TraitType)
     */
    @Override
    public void forTraitType(TraitType that) {
        // TODO Auto-generated method stub
        super.forTraitType(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forMatrixType(com.sun.fortress.nodes.MatrixType)
     */
    @Override
    public void forMatrixType(MatrixType that) {
        // TODO Auto-generated method stub
        super.forMatrixType(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forObjectAbsDeclOrDecl(com.sun.fortress.nodes.ObjectAbsDeclOrDecl)
     */
    @Override
    public void forObjectAbsDeclOrDecl(ObjectAbsDeclOrDecl that) {
        // TODO Auto-generated method stub
        super.forObjectAbsDeclOrDecl(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forObjectDecl(com.sun.fortress.nodes.ObjectDecl)
     */
    @Override
    public void forObjectDecl(ObjectDecl that) {
        // TODO Auto-generated method stub
        super.forObjectDecl(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forTraitAbsDeclOrDecl(com.sun.fortress.nodes.TraitAbsDeclOrDecl)
     */
    @Override
    public void forTraitAbsDeclOrDecl(TraitAbsDeclOrDecl that) {
        // TODO Auto-generated method stub
        super.forTraitAbsDeclOrDecl(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forTraitDecl(com.sun.fortress.nodes.TraitDecl)
     */
    @Override
    public void forTraitDecl(TraitDecl that) {
        // TODO Auto-generated method stub
        super.forTraitDecl(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forTraitObjectAbsDeclOrDecl(com.sun.fortress.nodes.TraitObjectAbsDeclOrDecl)
     */
    @Override
    public void forTraitObjectAbsDeclOrDecl(TraitObjectAbsDeclOrDecl that) {
        // TODO Auto-generated method stub
        super.forTraitObjectAbsDeclOrDecl(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forBaseType(com.sun.fortress.nodes.BaseType)
     */
    @Override
    public void forBaseType(BaseType that) {
        // TODO Auto-generated method stub
        super.forBaseType(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forTraitTypeWhere(com.sun.fortress.nodes.TraitTypeWhere)
     */
    @Override
    public void forTraitTypeWhere(TraitTypeWhere that) {
        // TODO Auto-generated method stub
        super.forTraitTypeWhere(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forVarargTupleType(com.sun.fortress.nodes.VarargTupleType)
     */
    @Override
    public void forVarargTupleType(VarargTupleType that) {
        // TODO Auto-generated method stub
        super.forVarargTupleType(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forTupleType(com.sun.fortress.nodes.TupleType)
     */
    @Override
    public void forTupleType(TupleType that) {
        // TODO Auto-generated method stub
        super.forTupleType(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forType(com.sun.fortress.nodes.Type)
     */
    @Override
    public void forType(Type that) {
        // TODO Auto-generated method stub
        super.forType(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forVoidType(com.sun.fortress.nodes.VoidType)
     */
    @Override
    public void forVoidType(VoidType that) {
        // TODO Auto-generated method stub
        super.forVoidType(that);
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.nodes.NodeAbstractVisitor_void#forWhereExtends(com.sun.fortress.nodes.WhereExtends)
     */
    @Override
    public void forWhereExtends(WhereExtends that) {
        // TODO Auto-generated method stub
        super.forWhereExtends(that);
    }

    Set<Overload> pendingOverloads;
}
