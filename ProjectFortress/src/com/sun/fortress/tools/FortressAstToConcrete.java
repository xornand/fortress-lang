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

package com.sun.fortress.tools;

import java.util.List;
import com.sun.fortress.nodes.*;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionUnwrapException;
import edu.rice.cs.plt.iter.IterUtil;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

public class FortressAstToConcrete extends NodeDepthFirstVisitor<String> {

    /* Possible improvements **************************************************/
    /* 1. Add newlines and indentation
       2. Handle "parenthesized" field
       3. Handle syntax abstraction nodes.
     */

    /* utility methods ********************************************************/
    /* returns a string beginning with 'kind' followed by a sequence of elements
       in 'list' separated by commas and  enclosed by '{' and '}'
     */
    private String inCurlyBraces(String kind, List<String> list) {
        StringBuilder s = new StringBuilder();
        s.append( kind );
        s.append( "{ " );
        for ( String elem : IterUtil.skipLast(list) ){
            s.append( elem ).append( ", " );
        }
        if ( ! list.isEmpty() ){
            s.append( IterUtil.last(list) );
        }
        s.append( " }" );
        return s.toString();
    }

    /* returns a string of a sequence of elements
       in 'list' separated by commas and  enclosed by '[\' and '\]'
     */
    private String inOxfordBrackets(List<String> list) {
        StringBuilder s = new StringBuilder();
        s.append( "[\\ " );
        for ( String elem : IterUtil.skipLast(list) ){
            s.append( elem ).append( ", " );
        }
        if ( ! list.isEmpty() ){
                s.append( IterUtil.last(list) );
        }
        s.append( " \\]" );
        return s.toString();
    }

    /* returns a string of a sequence of elements
       in 'list' separated by commas and  enclosed by '(' and ')'
     */
    private String inParentheses(List<String> list) {
        if ( list.size() == 1) {
            return list.get(0);
        } else {
            StringBuilder s = new StringBuilder();
            s.append( "(" );
            for ( String elem : IterUtil.skipLast(list) ){
                s.append( elem ).append( ", " );
            }
            if ( ! list.isEmpty() ){
                s.append( IterUtil.last(list) );
            }
            s.append( ")" );
            return s.toString();
        }
    }

    /*  make sure it is parenthesized */
    private String inParentheses(String str) {
        if ( str.startsWith("(") && str.endsWith(")") )
            return str;
        else
            return "(" + str + ")";
    }

    private String join( List<String> all, String sep ){
        StringBuilder s = new StringBuilder();

        int index = 0;
        for ( String element : all ){
            s.append( element );
            if ( index < all.size() - 1 ){
                s.append( sep );
            }
            index += 1;
        }

        return s.toString();
    }

    /* visit nodes ************************************************************/
    @Override public String forComponentOnly(Component that, String name_result,
                                             List<String> imports_result,
                                             List<String> exports_result,
                                             List<String> decls_result) {
        StringBuilder s = new StringBuilder();
        if (that.is_native())
            s.append( "native " );
        s.append( "component " ).append( name_result ).append( "\n" );
        for ( String import_ : imports_result ){
            s.append( import_ ).append( "\n" );
        }
        for ( String export_ : exports_result ){
            s.append( export_ ).append( "\n" );
        }
        for ( String decl_ : decls_result ){
            s.append( decl_ ).append( "\n" );
        }
        s.append( "end" );
        return s.toString();
    }

    @Override public String forApiOnly(Api that, String name_result,
                                       List<String> imports_result,
                                       List<String> decls_result) {
        StringBuilder s = new StringBuilder();
        s.append( "api " ).append( name_result ).append( "\n" );
        for ( String import_ : imports_result ){
            s.append( import_ ).append( "\n" );
        }
        for ( String decl_ : decls_result ){
            s.append( decl_ ).append( "\n" );
        }
        s.append( "end" );
        return s.toString();
    }

    @Override public String forImportStarOnly(ImportStar that,
                                              String api_result,
                                              List<String> except_result) {
        StringBuilder s = new StringBuilder();
        s.append( "import " ).append( api_result ).append( ".{...}" );
        if ( ! except_result.isEmpty() ) {
            s.append(inCurlyBraces(" except ", except_result));
        }
        return s.toString();
    }

    @Override public String forImportNamesOnly(ImportNames that,
                                               String api_result,
                                               List<String> aliasedNames_result) {
        StringBuilder s = new StringBuilder();
        s.append( "import " ).append( api_result );
        if ( aliasedNames_result.isEmpty() ) {
            return bug(that, "Import statement should have at least one name.");
        } else {
            s.append(inCurlyBraces(".", aliasedNames_result));
        }
        return s.toString();
    }

    @Override public String forImportApiOnly(ImportApi that,
                                             List<String> apis_result) {
        StringBuilder s = new StringBuilder();
        if ( apis_result.isEmpty() ) {
            return bug(that, "Import statement should have at least one name.");
        } else {
            s.append(inCurlyBraces("import api ", apis_result));
        }
        return s.toString();
    }

    @Override public String forAliasedSimpleNameOnly(AliasedSimpleName that,
                                                     String name_result,
                                                     Option<String> alias_result) {
        StringBuilder s = new StringBuilder();
        s.append( name_result );
        if ( alias_result.isSome() ) {
            s.append( " as " ).append(alias_result.unwrap());
        }
        return s.toString();
    }

    @Override public String forAliasedAPINameOnly(AliasedAPIName that,
                                                  String api_result,
                                                  Option<String> alias_result) {
        StringBuilder s = new StringBuilder();
        s.append( api_result );
        if ( alias_result.isSome() ) {
            s.append( " as " ).append(alias_result.unwrap());
        }
        return s.toString();
    }

    @Override public String forExportOnly(Export that, List<String> apis_result) {
        StringBuilder s = new StringBuilder();
        if (apis_result.size() == 0)
            return bug(that, "An export statement should have at least one API name.");
        else
            return inCurlyBraces("export ", apis_result);
    }

    @Override public String forAbsTraitDeclOnly(AbsTraitDecl that,
                                                List<String> mods_result,
                                                String name_result,
                                                List<String> staticParams_result,
                                                List<String> extendsClause_result,
                                                String where_result,
                                                List<String> excludes_result,
                                                Option<List<String>> comprises_result,
                                                List<String> decls_result) {
        StringBuilder s = new StringBuilder();

        s.append( join(mods_result, " ") );
        s.append( "trait " ).append( name_result );
        if ( ! staticParams_result.isEmpty() ) {
            s.append( inOxfordBrackets(staticParams_result) );
        }
        if ( ! extendsClause_result.isEmpty() ) {
            s.append( inCurlyBraces("extends ", extendsClause_result) );
        }
        s.append( where_result );
        if ( ! excludes_result.isEmpty() ) {
            s.append( inCurlyBraces("excludes ", excludes_result) );
        }
        if ( comprises_result.isSome() ){
            List<String> throws_ = comprises_result.unwrap();
            if ( ! throws_.isEmpty() )
                s.append( inCurlyBraces("comprises ", throws_) );
        }
        s.append( "\n" );
        s.append( join(decls_result,"\n") );
        s.append( "\nend" ).append( "\n" );

        return s.toString();
    }

    @Override public String forTraitDeclOnly(TraitDecl that,
                                             List<String> mods_result,
                                             String name_result,
                                             List<String> staticParams_result,
                                             List<String> extendsClause_result,
                                             String where_result,
                                             List<String> excludes_result,
                                             Option<List<String>> comprises_result,
                                             List<String> decls_result) {
        StringBuilder s = new StringBuilder();

        s.append( join(mods_result, " ") );
        s.append( "trait " ).append( name_result );
        if ( ! staticParams_result.isEmpty() ) {
            s.append( inOxfordBrackets(staticParams_result) );
        }
        if ( ! extendsClause_result.isEmpty() ) {
            s.append( inCurlyBraces("extends ", extendsClause_result) );
        }
        s.append( where_result );
        if ( ! excludes_result.isEmpty() ) {
            s.append( inCurlyBraces("excludes ", excludes_result) );
        }
        if ( comprises_result.isSome() ){
            List<String> throws_ = comprises_result.unwrap();
            if ( ! throws_.isEmpty() )
                s.append( inCurlyBraces("comprises ", throws_) );
        }
        s.append( "\n" );
        s.append( join(decls_result,"\n") );
        s.append( "\nend" ).append( "\n" );

        return s.toString();
    }

    @Override public String forAbsObjectDeclOnly(AbsObjectDecl that,
                                                 List<String> mods_result,
                                                 String name_result,
                                                 List<String> staticParams_result,
                                                 List<String> extendsClause_result,
                                                 String where_result,
                                                 Option<List<String>> params_result,
                                                 Option<List<String>> throwsClause_result,
                                                 String contract_result,
                                                 List<String> decls_result) {
        StringBuilder s = new StringBuilder();

        s.append( join(mods_result, " ") );
        s.append( "object " ).append( name_result );
        if ( ! staticParams_result.isEmpty() ) {
            s.append( inOxfordBrackets(staticParams_result) );
        }
        if ( ! extendsClause_result.isEmpty() ) {
            s.append( inCurlyBraces("extends ", extendsClause_result) );
        }
        s.append( where_result );
        if ( params_result.isSome() ){
            s.append( inParentheses(params_result.unwrap()) );
        }
        if ( throwsClause_result.isSome() ){
            List<String> throws_ = throwsClause_result.unwrap();
            if ( ! throws_.isEmpty() )
                s.append( inCurlyBraces("throws ", throws_) );
        }
        s.append( contract_result );
        s.append( "\n" );
        s.append( join(decls_result,"\n") );
        s.append( "\nend" ).append( "\n" );

        return s.toString();
    }

    @Override public String forObjectDeclOnly(ObjectDecl that,
                                              List<String> mods_result,
                                              String name_result,
                                              List<String> staticParams_result,
                                              List<String> extendsClause_result,
                                              String where_result,
                                              Option<List<String>> params_result,
                                              Option<List<String>> throwsClause_result,
                                              String contract_result,
                                              List<String> decls_result) {
        StringBuilder s = new StringBuilder();

        s.append( join(mods_result, " ") );
        s.append( "object " ).append( name_result );
        if ( ! staticParams_result.isEmpty() ) {
            s.append( inOxfordBrackets(staticParams_result) );
        }
        if ( ! extendsClause_result.isEmpty() ) {
            s.append( inCurlyBraces("extends ", extendsClause_result) );
        }
        s.append( where_result );
        if ( params_result.isSome() ){
            s.append( inParentheses(params_result.unwrap()) );
        }
        if ( throwsClause_result.isSome() ){
            List<String> throws_ = throwsClause_result.unwrap();
            if ( ! throws_.isEmpty() )
                s.append( inCurlyBraces("throws ", throws_) );
        }
        s.append( contract_result );
        s.append( "\n" );
        s.append( join(decls_result,"\n") );
        s.append( "\nend" ).append( "\n" );

        return s.toString();
    }

    @Override public String forAbsVarDeclOnly(AbsVarDecl that,
                                              List<String> lhs_result) {
        return inParentheses(lhs_result);
    }

    @Override public String forVarDeclOnly(VarDecl that,
                                           List<String> lhs_result,
                                           String init_result) {
        StringBuilder s = new StringBuilder();

        s.append( inParentheses(lhs_result) );
        s.append( " = " );
        s.append( init_result );

        return s.toString();
    }

    @Override public String forLValueBindOnly(LValueBind that,
                                              String name_result,
                                              Option<String> type_result,
                                              List<String> mods_result) {
        StringBuilder s = new StringBuilder();

        s.append( join(mods_result, " ") );
        s.append( name_result );
        if ( type_result.isSome() ){
            s.append( ": " );
            s.append( type_result.unwrap() );
        }

        return s.toString();
    }

//    @Override public String forUnpastingBindOnly( that,
//    @Override public String forUnpastingSplitOnly( that,

    @Override public String forAbsFnDeclOnly(AbsFnDecl that,
                                             List<String> mods_result,
                                             String name_result,
                                             List<String> staticParams_result,
                                             List<String> params_result,
                                             Option<String> returnType_result,
                                             Option<List<String>> throwsClause_result,
                                             String where_result,
                                             String contract_result) {
        StringBuilder s = new StringBuilder();
        for ( String mod : mods_result ){
            s.append( mod ).append( " " );
        }
        s.append( name_result );
        if ( ! staticParams_result.isEmpty() )
            s.append( inOxfordBrackets(staticParams_result) );
        s.append( inParentheses(inParentheses(params_result)) );
        if (returnType_result.isSome())
            s.append( ": " ).append( returnType_result.unwrap() );
        if (throwsClause_result.isSome()) {
            List<String> throws_ = throwsClause_result.unwrap();
            if ( ! throws_.isEmpty() )
                s.append( inCurlyBraces("throws ", throws_) );
        }
        s.append( where_result );
        s.append( contract_result );
        return s.toString();
    }


    @Override public String forFnDefOnly(FnDef that,
                                         List<String> mods_result,
                                         String name_result,
                                         List<String> staticParams_result,
                                         List<String> params_result,
                                         Option<String> returnType_result,
                                         Option<List<String>> throwsClause_result,
                                         String where_result,
                                         String contract_result,
                                         String body_result) {
        StringBuilder s = new StringBuilder();
        for ( String mod : mods_result ){
            s.append( mod ).append( " " );
        }
        s.append( name_result );
        if ( ! staticParams_result.isEmpty() )
            s.append( inOxfordBrackets(staticParams_result) );
        s.append( inParentheses(inParentheses(params_result)) );
        if (returnType_result.isSome())
            s.append( ": " ).append( returnType_result.unwrap() );
        if (throwsClause_result.isSome()) {
            List<String> throws_ = throwsClause_result.unwrap();
            if ( ! throws_.isEmpty() )
                s.append( inCurlyBraces("throws ", throws_) );
        }
        s.append( where_result );
        s.append( contract_result );
        s.append( " = " );
        s.append( body_result );
        return s.toString();
    }

    @Override public String forNormalParamOnly(NormalParam that,
                                               List<String> mods_result,
                                               String name_result,
                                               Option<String> type_result,
                                               Option<String> defaultExpr_result) {
        StringBuilder s = new StringBuilder();
        for ( String mod : mods_result ){
            s.append( mod ).append( " " );
        }
        s.append( name_result );
        if (type_result.isSome())
            s.append( ": " ).append( type_result.unwrap() );
        if (defaultExpr_result.isSome())
            s.append( "=").append( defaultExpr_result );
        return s.toString();
    }

    @Override public String forVarargsParamOnly(VarargsParam that,
                                                List<String> mods_result,
                                                String name_result,
                                                String type_result) {
        StringBuilder s = new StringBuilder();
        for ( String mod : mods_result ){
            s.append( mod ).append( " " );
        }
        s.append( name_result );
        s.append( ": " );
        s.append( type_result );
        s.append( "..." );
        return s.toString();
    }

//    @Override public String forDimDeclOnly( that,
//    @Override public String forUnitDeclOnly( that,
//    @Override public String forTestDeclOnly( that,
//    @Override public String forPropertyDeclOnly( that,

    @Override public String forAsExprOnly(AsExpr that,
                                          String expr_result,
                                          String type_result) {
        return expr_result + " as " + type_result;
    }

    @Override public String forAsIfExprOnly(AsIfExpr that,
                                            String expr_result,
                                            String type_result) {
        return expr_result + " asif " + type_result;
    }

    @Override public String forAssignmentOnly(Assignment that,
                                              List<String> lhs_result,
                                              Option<String> opr_result,
                                              String rhs_result) {
        StringBuilder s = new StringBuilder();

        s.append( inParentheses(lhs_result) ).append( " " );
        if ( opr_result.isSome() ){
            s.append( opr_result.unwrap() ).append( "= " );
        } else {
            s.append( ":= " );
        }
        s.append( rhs_result );

        return s.toString();
    }

    @Override public String forBlockOnly(Block that,
                                         List<String> exprs_result) {
        StringBuilder s = new StringBuilder();
        s.append( join( exprs_result, "\n" ) );
        return s.toString();
    }

//    @Override public String forCaseExprOnly( that,

    @Override public String forDoOnly(Do that,
                                      List<String> fronts_result) {
        StringBuilder s = new StringBuilder();
        s.append( join(fronts_result, "\n") );
        s.append( "end" );
        return s.toString();
    }

    @Override public String forForOnly(For that,
                                       List<String> gens_result,
                                       String body_result) {
        StringBuilder s = new StringBuilder();

        s.append( "for " );
        s.append( join(gens_result, ", \n") );
        s.append( body_result );
        s.append( "\nend" );

        return s.toString();
    }

    @Override public String forIfOnly(If that,
                                      List<String> clauses_result,
                                      Option<String> elseClause_result) {
        StringBuilder s = new StringBuilder();

        s.append( "if " );
        if ( clauses_result.isEmpty() ) {
            return bug(that, "An if expression should have at least " +
                       "one then branch.");
        } else {
            s.append( IterUtil.first(clauses_result) );
            for (String ifclause : IterUtil.skipFirst(clauses_result) ) {
                s.append( "elif " );
                s.append( ifclause );
            }
        }
        if ( elseClause_result.isSome() ) {
            s.append("\nelse ");
            s.append( elseClause_result.unwrap() );
        }
        s.append( "\nend" );

        return s.toString();
    }

//    @Override public String forLabelOnly( that,
//    @Override public String forObjectExprOnly( that,
//    @Override public String for_RewriteObjectExprOnly( that,
//    @Override public String forTryOnly( that,

    @Override public String forTupleExprOnly(TupleExpr that,
                                             List<String> exprs_result) {
        if ( exprs_result.size() == 1 )
            return exprs_result.get(0);
        else {
            StringBuilder s = new StringBuilder();
            s.append( "(" );
            s.append( join(exprs_result, ", ") );
            s.append( ")" );
            return s.toString();
        }
    }

//    @Override public String forArgExprOnly( that,
//    @Override public String forTypecaseOnly( that,
//    @Override public String forWhileOnly( that,
//    @Override public String forAccumulatorOnly( that,
//    @Override public String forArrayComprehensionOnly( that,

    @Override public String forAtomicExprOnly(AtomicExpr that,
                                              String expr_result) {
        StringBuilder s = new StringBuilder();
        s.append( "atomic " ).append( expr_result );
        return s.toString();
    }

//    @Override public String forExitOnly( that,
//    @Override public String forSpawnOnly( that,
//    @Override public String forThrowOnly( that,
//    @Override public String forTryAtomicExprOnly( that,
//    @Override public String forFnExprOnly( that,
//    @Override public String forLetFnOnly( that,

    @Override public String forLocalVarDeclOnly(LocalVarDecl that,
                                                List<String> body_result,
                                                List<String> lhs_result,
                                                Option<String> rhs_result) {
        StringBuilder s = new StringBuilder();

        s.append( inParentheses(lhs_result) );
        if ( rhs_result.isSome() ){
            s.append( " = " );
            s.append( rhs_result.unwrap() );
        }
        s.append("\n");
        s.append( join( body_result, "\n" ) );

        return s.toString();
    }

//    @Override public String forGeneratedExprOnly( that,
//    @Override public String forSubscriptExprOnly( that,
//    @Override public String forFloatLiteralExprOnly( that,

    @Override public String forIntLiteralExprOnly(IntLiteralExpr that){
        return that.getVal().toString();
    }

//    @Override public String forCharLiteralExprOnly( that,

    @Override public String forStringLiteralExprOnly(StringLiteralExpr that) {
        return "\"" + that.getText() + "\"";
    }

    @Override public String forVoidLiteralExprOnly(VoidLiteralExpr that) {
        return "()";
    }

    @Override public String forVarRefOnly(VarRef that,
                                          String var_result) {
        return var_result;
    }

//    @Override public String for_RewriteObjectRefOnly( that,
//    @Override public String forFieldRefOnly( that,
//    @Override public String for_RewriteFieldRefOnly( that,

    @Override public String forFnRefOnly(FnRef that,
                                         String originalName_result,
                                         List<String> fns_result,
                                         List<String> staticArgs_result) {
        StringBuilder s = new StringBuilder();

        s.append( originalName_result );
        if ( ! staticArgs_result.isEmpty() ) {
            s.append( inOxfordBrackets(staticArgs_result) );
        }

        return s.toString();
    }

//    @Override public String for_RewriteFnRefOnly( that,

//    @Override public String forOpRefOnly( that,
    /* TODO: BIG Op StaticArgs
             LeftEncloser StaticArgs ExprList RightEncloser
     */
    @Override public String forOpRefOnly(OpRef that,
                                         String originalName_result,
                                         List<String> ops_result,
                                         List<String> staticArgs_result) {
        StringBuilder s = new StringBuilder();

        s.append( originalName_result );

        return s.toString();
    }

    @Override public String forLooseJuxtOnly(LooseJuxt that,
                                             String multiJuxt_result,
                                             String infixJuxt_result,
                                             List<String> exprs_result) {
        StringBuilder s = new StringBuilder();
        for ( String expr : exprs_result ){
            s.append( expr );
            s.append( " " );
        }
        return s.toString();
    }

    @Override public String forTightJuxtOnly(TightJuxt that,
                                             String multiJuxt_result,
                                             String infixJuxt_result,
                                             List<String> exprs_result) {
        StringBuilder s = new StringBuilder();
        if ( exprs_result.isEmpty() )
            return bug(that, "A tight juxtaposition expression should have " +
                       "at least two subexpressions.");
        s.append(IterUtil.first(exprs_result));
        for ( String expr : IterUtil.skipFirst(exprs_result) ){
            s.append( inParentheses(expr) );
        }
        return s.toString();
    }

    @Override public String for_RewriteFnAppOnly(_RewriteFnApp that,
                                                 String function_result,
                                                 String argument_result) {
        StringBuilder s = new StringBuilder();

        s.append( function_result );
        s.append( inParentheses(argument_result) );

        return s.toString();
    }

    //    @Override public String forOpExprOnly( that,
    /* TODO: operator fixity
     */
    @Override public String forOpExprOnly(OpExpr that,
                                          String op_result,
                                          List<String> args_result) {
        StringBuilder s = new StringBuilder();

        s.append( op_result );
        s.append( join(args_result, " ") );

        return s.toString();
    }

    @Override public String forAmbiguousMultifixOpExprOnly(AmbiguousMultifixOpExpr that,
                                                           String infix_op_result,
                                                           String multifix_op_result,
                                                           List<String> args_result) {
        StringBuilder s = new StringBuilder();
        s.append( join( args_result, " "+infix_op_result+" " ) );
        return s.toString();
    }

    @Override public String forChainExprOnly(ChainExpr that,
                                             String first_result,
                                             List<String> links_result) {
        StringBuilder s = new StringBuilder();

        s.append( first_result );
        s.append( join(links_result, " ") );

        return s.toString();
    }

    @Override public String forCoercionInvocationOnly(CoercionInvocation that,
                                                      String type_result,
                                                      List<String> staticArgs_result,
                                                      String arg_result) {
        /*
        StringBuilder s = new StringBuilder();

        s.append( type_result );
        if ( ! staticArgs_result.isEmpty() ) {
            s.append( inOxfordBrackets(staticArgs_result) );
        }
        s.append( ".coercion" );
        s.append( inParentheses(arg_result) );

        return s.toString();
        */
        return bug(that, "Explicit coercion invocation is not supported " +
                   "in Fortress concrete syntax.");
    }

    @Override public String forMethodInvocationOnly(MethodInvocation that,
                                                    String obj_result,
                                                    String method_result,
                                                    List<String> staticArgs_result,
                                                    String arg_result) {
        StringBuilder s = new StringBuilder();

        s.append( obj_result ).append( "." ).append( method_result );
        if ( ! staticArgs_result.isEmpty() ) {
            s.append( inOxfordBrackets(staticArgs_result ) );
        }
        s.append( inParentheses(arg_result) );

        return s.toString();
    }

//    @Override public String forMathPrimaryOnly( that,
//    @Override public String forArrayElementOnly( that,
//    @Override public String forArrayElementsOnly( that,
//    @Override public String forExponentTypeOnly( that,
//    @Override public String forBaseDimOnly( that,
//    @Override public String forDimRefOnly( that,
//    @Override public String forProductDimOnly( that,
//    @Override public String forQuotientDimOnly( that,
//    @Override public String forExponentDimOnly( that,
//    @Override public String forOpDimOnly( that,
//    @Override public String forAnyTypeOnly( that,
//    @Override public String forBottomTypeOnly( that,

    @Override public String forVarTypeOnly(VarType that, String name_result) {
        return name_result;
    }

//    @Override public String forTraitTypeOnly( that,
//    @Override public String for_RewriteGenericSingletonTypeOnly( that,
//    @Override public String forArrayTypeOnly( that,
//    @Override public String forMatrixTypeOnly( that,
//    @Override public String forTaggedDimTypeOnly( that,
//    @Override public String forTaggedUnitTypeOnly( that,
//    @Override public String forTupleTypeOnly( that,
//    @Override public String forVarargTupleTypeOnly( that,

    @Override public String forVoidTypeOnly(VoidType that) {
        return "()";
    }

    @Override public String forArrowTypeOnly(ArrowType that,
                                             String domain_result,
                                             String range_result,
                                             String effect_result) {
        StringBuilder s = new StringBuilder();
        s.append( domain_result );
        s.append( " -> " );
        s.append( range_result );
        s.append( effect_result );
        return s.toString();
    }

    @Override public String for_RewriteGenericArrowTypeOnly(_RewriteGenericArrowType that,
                                                            List<String> staticParams_result,
                                                            String domain_result,
                                                            String range_result,
                                                            String effect_result,
                                                            String where_result) {
        return bug(that, "Generic arrow types are not supported " +
                   "in Fortress concrete syntax.");
    }

    @Override public String for_InferenceVarTypeOnly(_InferenceVarType that) {
        return bug(that, "Inference variable types are not supported " +
                   "in Fortress concrete syntax.");
    }

    @Override public String forIntersectionTypeOnly(IntersectionType that,
                                                    List<String> elements_result) {
        return bug(that, "Intersection types are not supported " +
                   "in Fortress concrete syntax.");
    }

    @Override public String forUnionTypeOnly(UnionType that,
                                             List<String> elements_result) {
        return bug(that, "Union types are not supported " +
                   "in Fortress concrete syntax.");
    }

    @Override public String forFixedPointTypeOnly(FixedPointType that,
                                                  String name_result,
                                                  String body_result) {
        return bug(that, "Fixed point types are not supported " +
                   "in Fortress concrete syntax.");
    }

    @Override public String forLabelTypeOnly(LabelType that) {
        return bug(that, "Label types are not supported " +
                   "in Fortress concrete syntax.");
    }

    @Override public String forDomainOnly(Domain that,
                                          List<String> args_result,
                                          Option<String> varargs_result,
                                          List<String> keywords_result) {
        int args_size = args_result.size();
        int varargs_size = (varargs_result.isSome()) ? 1 : 0;
        int keywords_size = keywords_result.size();
        boolean inParen = ( args_size + varargs_size + keywords_size > 1 ||
                            varargs_size > 0 || keywords_size > 0);
        StringBuilder s = new StringBuilder();
        s.append( join(args_result, ", " ) );
        if ( varargs_size == 1 ) {
            if ( args_size > 0 )
                s.append( ", " );
            s.append( varargs_result.unwrap() );
        }
        if ( keywords_size > 0 ) {
            if ( args_size + varargs_size > 0)
                s.append( ", " );
            s.append( join(keywords_result, ", ") );
        }
        if ( inParen )
            return inParentheses( s.toString() );
        else return s.toString();
    }

    @Override public String forEffectOnly(Effect that,
                                          Option<List<String>> throwsClause_result) {
        if ( throwsClause_result.isSome() )
            return inCurlyBraces(" throws ", throwsClause_result.unwrap());
        else return "";
    }

    @Override public String forTypeArgOnly(TypeArg that,
                                           String type_result) {
        return type_result;
    }

//    @Override public String forIntArgOnly( that,
//    @Override public String forBoolArgOnly( that,
//    @Override public String forOpArgOnly( that,
//    @Override public String forDimArgOnly( that,
//    @Override public String forUnitArgOnly( that,
//    @Override public String forNumberConstraintOnly( that,
//    @Override public String forIntRefOnly( that,
//    @Override public String forSumConstraintOnly( that,
//    @Override public String forMinusConstraintOnly( that,
//    @Override public String forProductConstraintOnly( that,
//    @Override public String forExponentConstraintOnly( that,
//    @Override public String forBoolConstantOnly( that,
//    @Override public String forBoolRefOnly( that,
//    @Override public String forNotConstraintOnly( that,
//    @Override public String forOrConstraintOnly( that,
//    @Override public String forAndConstraintOnly( that,
//    @Override public String forImpliesConstraintOnly( that,
//    @Override public String forBEConstraintOnly( that,
//    @Override public String forUnitRefOnly( that,
//    @Override public String forProductUnitOnly( that,
//    @Override public String forQuotientUnitOnly( that,
//    @Override public String forExponentUnitOnly( that,

    @Override public String forWhereClauseOnly(WhereClause that,
                                               List<String> bindings_result,
                                               List<String> constraints_result) {
        if ( bindings_result.isEmpty() && constraints_result.isEmpty() )
            return "";
        StringBuilder s = new StringBuilder();
        s.append( "where " );
        if ( ! bindings_result.isEmpty() )
            s.append( inOxfordBrackets(bindings_result) );
        if ( ! constraints_result.isEmpty() )
            s.append( inCurlyBraces("", constraints_result) );
        return s.toString();
    }

    @Override public String forWhereTypeOnly(WhereType that,
                                             String name_result,
                                             List<String> supers_result) {
        StringBuilder s = new StringBuilder();
        s.append( name_result );
        if ( ! supers_result.isEmpty() ) {
            s.append( inCurlyBraces("extends ", supers_result) );
        }
        return s.toString();
    }

    @Override public String forWhereNatOnly(WhereNat that,
                                            String name_result) {
        return "nat " + name_result;
    }

    @Override public String forWhereIntOnly(WhereInt that,
                                            String name_result) {
        return "int " + name_result;
    }

    @Override public String forWhereBoolOnly(WhereBool that,
                                             String name_result) {
        return "bool " + name_result;
    }

    @Override public String forWhereUnitOnly(WhereUnit that,
                                             String name_result) {
        return "unit " + name_result;
    }

    @Override public String forWhereExtendsOnly(WhereExtends that,
                                                String name_result,
                                                List<String> supers_result) {
        StringBuilder s = new StringBuilder();
        s.append( name_result );
        if ( supers_result.isEmpty() ) {
            return bug(that, "A type variable constraint declared in " +
                       "a where clause should have its bound.");
        } else {
            s.append( inCurlyBraces("extends ", supers_result) );
        }
        return s.toString();
    }

    @Override public String forTypeAliasOnly(TypeAlias that,
                                             String name_result,
                                             List<String> staticParams_result,
                                             String type_result) {
        StringBuilder s = new StringBuilder();
        s.append( "type " ).append( name_result );
        if ( ! staticParams_result.isEmpty() ) {
            s.append ( inOxfordBrackets(staticParams_result) );
        }
        s.append( " = " ).append( type_result );
        return s.toString();
    }

    @Override public String forWhereCoercesOnly(WhereCoerces that,
                                                String left_result,
                                                String right_result) {
        StringBuilder s = new StringBuilder();
        s.append( left_result ).append( " coerces " ).append( right_result );
        return s.toString();
    }

    @Override public String forWhereWidensOnly(WhereWidens that,
                                               String left_result,
                                               String right_result) {
        StringBuilder s = new StringBuilder();
        s.append( left_result ).append( " widens " ).append( right_result );
        return s.toString();
    }

    @Override public String forWhereWidensCoercesOnly(WhereWidensCoerces that,
                                                      String left_result,
                                                      String right_result) {
        StringBuilder s = new StringBuilder();
        s.append( left_result ).append( " widens or coerces " ).append( right_result );
        return s.toString();
    }

    @Override public String forWhereEqualsOnly(WhereEquals that,
                                               String left_result,
                                               String right_result) {
        StringBuilder s = new StringBuilder();
        s.append( left_result ).append( " = " ).append( right_result );
        return s.toString();
    }

    @Override public String forUnitConstraintOnly(UnitConstraint that,
                                                  String name_result) {
        StringBuilder s = new StringBuilder();
        s.append( name_result ).append( " = dimensionless" );
        return s.toString();
    }

    @Override public String forLEConstraintOnly(LEConstraint that,
                                                String left_result,
                                                String right_result) {
        StringBuilder s = new StringBuilder();
        s.append( left_result ).append( " <= " ).append( right_result );
        return s.toString();
    }

    @Override public String forLTConstraintOnly(LTConstraint that,
                                                String left_result,
                                                String right_result) {
        StringBuilder s = new StringBuilder();
        s.append( left_result ).append( " < " ).append( right_result );
        return s.toString();
    }

    @Override public String forGEConstraintOnly(GEConstraint that,
                                                String left_result,
                                                String right_result) {
        StringBuilder s = new StringBuilder();
        s.append( left_result ).append( " >= " ).append( right_result );
        return s.toString();
    }

    @Override public String forGTConstraintOnly(GTConstraint that,
                                                String left_result,
                                                String right_result) {
        StringBuilder s = new StringBuilder();
        s.append( left_result ).append( " > " ).append( right_result );
        return s.toString();
    }

    @Override public String forIEConstraintOnly(IEConstraint that,
                                                String left_result,
                                                String right_result) {
        StringBuilder s = new StringBuilder();
        s.append( left_result ).append( " = " ).append( right_result );
        return s.toString();
    }

    @Override public String forBoolConstraintExprOnly(BoolConstraintExpr that,
                                                      String constraint_result) {
        return constraint_result;
    }

    @Override public String forContractOnly(Contract that,
                                            Option<List<String>> requires_result,
                                            Option<List<String>> ensures_result,
                                            Option<List<String>> invariants_result) {
        StringBuilder s = new StringBuilder();

        if (requires_result.isSome()) {
            List<String> requires = requires_result.unwrap();
            if ( ! requires.isEmpty() )
                s.append( inCurlyBraces("requires ", requires) ).append( "\n" );
        }

        if (ensures_result.isSome()) {
            List<String> ensures = ensures_result.unwrap();
            if ( ! ensures.isEmpty() )
                s.append( inCurlyBraces("ensures ", ensures) ).append( "\n" );
        }

        if (invariants_result.isSome()) {
            List<String> invariants = invariants_result.unwrap();
            if ( ! invariants.isEmpty() )
                s.append( inCurlyBraces("invariant ", invariants) ).append( "\n" );
        }

        return s.toString();
    }

    @Override public String forEnsuresClauseOnly(EnsuresClause that,
                                                 String post_result,
                                                 Option<String> pre_result) {
        StringBuilder s = new StringBuilder();
        s.append( post_result );
        if ( pre_result.isSome() ) {
            s.append( " provided " ).append( pre_result );
        }
        return s.toString();
    }

    @Override public String forModifierAbstractOnly(ModifierAbstract that) {
        return "abstract";
    }

    @Override public String forModifierAtomicOnly(ModifierAtomic that) {
        return "atomic";
    }

    @Override public String forModifierGetterOnly(ModifierGetter that) {
        return "getter";
    }

    @Override public String forModifierHiddenOnly(ModifierHidden that) {
        return "hidden";
    }

    @Override public String forModifierIOOnly(ModifierIO that) {
        return "io";
    }

    @Override public String forModifierOverrideOnly(ModifierOverride that) {
        return "override";
    }

    @Override public String forModifierPrivateOnly(ModifierPrivate that) {
        return "private";
    }

    @Override public String forModifierSettableOnly(ModifierSettable that) {
        return "settable";
    }

    @Override public String forModifierSetterOnly(ModifierSetter that) {
        return "setter";
    }

    @Override public String forModifierTestOnly(ModifierTest that) {
        return "test";
    }

    @Override public String forModifierTransientOnly(ModifierTransient that) {
        return "transient";
    }

    @Override public String forModifierValueOnly(ModifierValue that) {
        return "value";
    }

    @Override public String forModifierVarOnly(ModifierVar that) {
        return "var";
    }

    @Override public String forModifierWidensOnly(ModifierWidens that) {
        return "widens";
    }

    @Override public String forModifierWrappedOnly(ModifierWrapped that) {
        return "wrapped";
    }

    @Override public String forOpParamOnly(OpParam that,
                                           String name_result) {
        return "opr " + name_result;
    }

    @Override public String forBoolParamOnly(BoolParam that,
                                             String name_result) {
        return "bool " + name_result;
    }

    @Override public String forDimParamOnly(DimParam that,
                                            String name_result) {
        return "dim " + name_result;
    }

    @Override public String forIntParamOnly(IntParam that,
                                            String name_result) {
        return "int " + name_result;
    }

    @Override public String forNatParamOnly(NatParam that,
                                            String name_result) {
        return "nat " + name_result;
    }

    @Override public String forTypeParamOnly(TypeParam that,
                                             String name_result,
                                             List<String> extendsClause_result) {
        StringBuilder s = new StringBuilder();
        s.append( name_result );
        if ( ! extendsClause_result.isEmpty() ) {
            s.append( inCurlyBraces("extends ", extendsClause_result) );
        }
        if ( that.isAbsorbs() ) {
            s.append( " absorbs unit" );
        }
        return s.toString();
    }

    @Override public String forUnitParamOnly(UnitParam that,
                                             String name_result,
                                             Option<String> dim_result) {
        StringBuilder s = new StringBuilder();
        s.append( "unit " ).append( name_result );
        if ( dim_result.isSome() ) {
            s.append( ": " ).append( dim_result.unwrap() );
        }
        if ( that.isAbsorbs() ) {
            s.append( " absorbs unit" );
        }
        return s.toString();
    }

    @Override public String forAPINameOnly(APIName that, List<String> ids_result) {
        StringBuilder s = new StringBuilder();
        if (IterUtil.isEmpty(ids_result))
            return s.toString();
        else {
            for ( String id : IterUtil.skipLast(ids_result) ){
                s.append( id ).append( "." );
            }
            s.append( IterUtil.last(ids_result) );
            return s.toString();
        }
    }

    @Override public String forIdOnly(Id that, Option<String> api_result) {
        StringBuilder s = new StringBuilder();
        if ( api_result.isSome() )
            s.append( api_result.unwrap() ).append( "." );
        s.append( that.getText() );
        return s.toString();
    }

    @Override public String forOpOnly(Op that,
                                      Option<String> api_result,
                                      Option<String> fixity_result) {
        return that.getText();
    }

    @Override public String forEnclosingOnly(Enclosing that,
                                             Option<String> api_result,
                                             String open_result,
                                             String close_result) {
        StringBuilder s = new StringBuilder();

        s.append( open_result );
        s.append( " " );
        s.append( close_result );

        return s.toString();
    }

//    @Override public String forAnonymousFnNameOnly( that,
//    @Override public String forConstructorFnNameOnly( that,
//    @Override public String forArrayComprehensionClauseOnly( that,
//    @Override public String forKeywordExprOnly( that,
//    @Override public String forCaseClauseOnly( that,
//    @Override public String forCatchOnly( that,
//    @Override public String forCatchClauseOnly( that,

    @Override public String forDoFrontOnly(DoFront that,
                                           Option<String> loc_result,
                                           String expr_result) {
        StringBuilder s = new StringBuilder();

        if ( loc_result.isSome() ) {
            s.append( "at " ).append( loc_result.unwrap() ).append( " " );
        }
        if ( that.isAtomic() ) {
            s.append( "atomic " );
        }
        s.append( "do " ).append( expr_result );

        return s.toString();
    }

    @Override public String forIfClauseOnly(IfClause that,
                                            String test_result,
                                            String body_result) {
        StringBuilder s = new StringBuilder();

        s.append( test_result );
        s.append( "\nthen" );
        s.append( body_result ).append( "\n" );

        return s.toString();
    }

//    @Override public String forTypecaseClauseOnly( that,
//    @Override public String forExtentRangeOnly( that,

    @Override public String forGeneratorClauseOnly(GeneratorClause that,
                                                   List<String> bind_result,
                                                   String init_result) {
        StringBuilder s = new StringBuilder();

        if ( ! bind_result.isEmpty() ) {
            if ( bind_result.size() == 1 ) {
                s.append( bind_result.get(0) );
            } else {
                s.append( inParentheses(bind_result) );
            }
            s.append( " <- " );
        }
        s.append( init_result );

        return s.toString();
    }

    @Override public String forVarargsExprOnly(VarargsExpr that,
                                               String varargs_result) {
        return varargs_result + "...";
    }

    @Override public String forKeywordTypeOnly(KeywordType that,
                                               String name_result,
                                               String type_result) {
        return name_result + " = " + type_result;
    }

//    @Override public String forTraitTypeWhereOnly( that,
//    @Override public String forIndicesOnly( that,
//    @Override public String forParenthesisDelimitedMIOnly( that,
//    @Override public String forNonParenthesisDelimitedMIOnly( that,
//    @Override public String forExponentiationMIOnly( that,
//    @Override public String forSubscriptingMIOnly( that,

    @Override public String forInFixityOnly(InFixity that) {
        return "";
    }

    @Override public String forPreFixityOnly(PreFixity that) {
        return "";
    }

    @Override public String forPostFixityOnly(PostFixity that) {
        return "";
    }

    @Override public String forNoFixityOnly(NoFixity that) {
        return "";
    }

    @Override public String forMultiFixityOnly(MultiFixity that) {
        return "";
    }

    @Override public String forEnclosingFixityOnly(EnclosingFixity that) {
        return "";
    }

    @Override public String forBigFixityOnly(BigFixity that) {
        return "";
    }

    @Override public String forLinkOnly(Link that,
                                        String op_result,
                                        String expr_result) {
        StringBuilder s = new StringBuilder();
        s.append( " " ).append( op_result ).append( " " ).append( expr_result );
        return s.toString();
    }
}