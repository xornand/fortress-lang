/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.compiler.disambiguator.ExprDisambiguator;
import com.sun.fortress.compiler.disambiguator.NameEnv;
import com.sun.fortress.compiler.disambiguator.NonterminalDisambiguator;
import com.sun.fortress.compiler.disambiguator.SelfParamDisambiguator;
import com.sun.fortress.compiler.disambiguator.TopLevelEnv;
import com.sun.fortress.compiler.disambiguator.TypeDisambiguator;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.GrammarIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.GrammarDecl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.HasAt;

/**
 * Eliminates ambiguities in an AST that can be resolved solely by knowing what
 * kind of entity a name refers to.  This class specifically handles
 * the following:
 * <ul>
 * <li>All names referring to APIs are made fully qualified (FnRefs
 *     and OpExprs may then contain lists of qualified names referring to
 *     multiple APIs).</li>
 * <li>VarRefs referring to functions become FnRefs with placeholders
 *     for implicit static arguments filled in (to be replaced later
 *     during type inference).</li>
 * <li>VarRefs referring to getters, setters, or methods become FieldRefs.</li>
 * <li>VarRefs referring to methods, and that are juxtaposed with Exprs, become
 *     MethodInvocations.</li>
 * <li>FieldRefs referring to methods, and that are juxtaposed with
 *     Exprs, become MethodInvocations.</li>
 * <li>FnRefs referring to methods, and that are juxtaposed with Exprs, become
 *     MethodInvocations.</li>
 * <li>VarTypes referring to traits become TraitTypes (with 0
 *     arguments)</li>
 * </ul>
 *
 * Additionally, all name references that are undefined or used incorrectly are
 * treated as static errors.
 */
public class Disambiguator {
    /**
     * Disambiguate the names of nonterminals.
     */
    private static List<Api> disambiguateGrammarMembers(Collection<ApiIndex> apis,
                                                        List<StaticError> errors,
                                                        GlobalEnvironment globalEnv)
    {
        List<Api> results = new ArrayList<Api>();
        for (ApiIndex index : apis) {
            // ApiIndex index = globalEnv.api(api.getName());
            NameEnv env = new TopLevelEnv(globalEnv, index, errors);
            List<StaticError> newErrs = new ArrayList<StaticError>();
            NonterminalDisambiguator pd = new NonterminalDisambiguator(env, globalEnv, newErrs);
            Debug.debug( Debug.Type.SYNTAX, 3, "Disambiguate grammar members for api " + index );
            Api pdResult = (Api) index.ast().accept(pd);
            results.add(pdResult);
            if (!newErrs.isEmpty()) {
                errors.addAll(newErrs);
            }
        }
        return results;
    }

    /** Result of {@link #disambiguateApis}. */
    public static class ApiResult extends StaticPhaseResult {
        private final Iterable<Api> _apis;

        public ApiResult(Iterable<Api> apis,
                Iterable<? extends StaticError> errors) {
            super(errors);
            _apis = apis;
        }

        public Iterable<Api> apis() { return _apis; }
    }



    /**
     * Disambiguate the given apis. To support circular references,
     * the apis should appear in the given environment.
     * @param apis_to_disambiguate Apis currently being disambiguated.
     * @param globalEnv The current global environment.
     * @param repository_apis Apis that already exist in the repository.
     */
    public static ApiResult disambiguateApis(Iterable<Api> apisToDisambiguate,
                                             GlobalEnvironment globalEnv,
                                             Map<APIName, ApiIndex> repositoryApis)
    {

    	repositoryApis = Collections.unmodifiableMap(repositoryApis);

        List<StaticError> errors = new ArrayList<StaticError>();

        // First, loop through apis, disambiguating types.
        List<Api> newApis = new ArrayList<Api>();
        for (Api api : apisToDisambiguate) {
            ApiIndex index = globalEnv.api(api.getName());

            NameEnv env = new TopLevelEnv(globalEnv, index, errors);

            Set<IdOrOpOrAnonymousName> onDemandImports = new HashSet<IdOrOpOrAnonymousName>();

            SelfParamDisambiguator selfDisambig = new SelfParamDisambiguator();
            Api spdResult = (Api) api.accept(selfDisambig);

            List<StaticError> newErrs = new ArrayList<StaticError>();
            TypeDisambiguator td =
                new TypeDisambiguator(env, onDemandImports, newErrs);
            Api tdResult = (Api) spdResult.accept(td);

            if (newErrs.isEmpty()) {
                newApis.add(tdResult);
            } else {
             	errors.addAll(newErrs);
            }
        }

        // Go no further if we couldn't disambiguate the types.
        if( errors.size() > 0 ) {
        	return new ApiResult(newApis, errors);
        }

        // then, rebuild the indices
        IndexBuilder.ApiResult rebuiltIndx = IndexBuilder.buildApis(newApis, System.currentTimeMillis());
        GlobalEnvironment newGlobalEnv = new GlobalEnvironment.FromMap(CollectUtil.union(repositoryApis,
                                                                                         rebuiltIndx.apis()));

        // Finally, disambiguate the expressions using the rebuilt indices.
        List<Api> results = new ArrayList<Api>();
        for (Api api : newApis) {
            ApiIndex index = newGlobalEnv.api(api.getName());

            NameEnv env = new TopLevelEnv(newGlobalEnv, index, errors);

            List<StaticError> newErrs = new ArrayList<StaticError>();
            ExprDisambiguator ed = new ExprDisambiguator(env, newErrs);
            Api edResult = (Api) api.accept(ed);
            if (newErrs.isEmpty()) {
            	results.add(edResult);
            } else {
            	errors.addAll(newErrs);
            }
        }

        IndexBuilder.ApiResult rebuiltIndx2 = IndexBuilder.buildApis(results, System.currentTimeMillis());
        GlobalEnvironment newGlobalEnv2 = new GlobalEnvironment.FromMap(CollectUtil.union(repositoryApis,
                                                                                          rebuiltIndx2.apis()));

        initializeGrammarIndexExtensions(rebuiltIndx2.apis().values(), globalEnv.apis().values() );
        results = disambiguateGrammarMembers(rebuiltIndx2.apis().values(), errors, newGlobalEnv2);
        return new ApiResult(results, errors);
    }

    private static Collection<? extends StaticError> initializeGrammarIndexExtensions(Collection<ApiIndex> apis,
                                                                                      Collection<ApiIndex> moreApis )
    {
        List<StaticError> errors = new LinkedList<StaticError>();
        Map<String, GrammarIndex> grammars = new HashMap<String, GrammarIndex>();

        /* It seems that this for loop has to come first, but I'm not sure why.
         * If it comes after the next for loop then the grammar index's wont
         * be fully qualified ant the NonterminalEnv.constructNonterminalApi
         * will complain. That is because the grammars from the moreApis set
         * have some overlap with the grammars from the apis set, so they will
         * conflict with each other in the grammars map. The grammars from the
         * moreApis set should be fully qualified, but they aren't.
         */
        for (ApiIndex a2: moreApis) {
            for (Map.Entry<String, GrammarIndex> e: a2.grammars().entrySet()) {
                Debug.debug( Debug.Type.COMPILER, 4, "Add Grammar " + e.getKey() + " from other apis" );
                grammars.put(e.getKey(), e.getValue());
            }
        }
        for (ApiIndex a2: apis) {
            for (Map.Entry<String, GrammarIndex> e: a2.grammars().entrySet()) {
                Debug.debug( Debug.Type.COMPILER, 4, "Add Grammar " + e.getKey() + " from normal apis" );
                grammars.put(e.getKey(), e.getValue());
            }
        }

        for (ApiIndex a1: apis) {
            for (Map.Entry<String,GrammarIndex> e: a1.grammars().entrySet()) {
                GrammarDecl og = e.getValue().ast();
                List<GrammarIndex> ls = new LinkedList<GrammarIndex>();
                for (Id n: og.getExtendsClause()) {
                    GrammarIndex g = grammars.get(n.getText());
                    if ( g == null ){
                        throw new RuntimeException( "Could not find grammar for " + n.getText() );
                    }
                    ls.add(g);
                }
                Debug.debug( Debug.Type.SYNTAX, 3, "Grammar " + e.getKey() + " extends " + ls );
                e.getValue().setExtended(ls);
            }
        }
        return errors;
    }

    /** Result of {@link #disambiguateComponents}. */
    public static class ComponentResult extends StaticPhaseResult {
        private final Iterable<Component> _components;
        public ComponentResult(Iterable<Component> components,
                Iterable<? extends StaticError> errors) {
            super(errors);
            _components = components;
        }
        public Iterable<Component> components() { return _components; }
    }

    /** Disambiguate the given components. */
    public static ComponentResult disambiguateComponents(Iterable<Component> components,
                                                         GlobalEnvironment globalEnv,
                                                         Map<APIName, ComponentIndex> indices) {


        List<Component> results = new ArrayList<Component>();
        List<StaticError> errors = new ArrayList<StaticError>();

        // First, disambiguate the types
        List<Component> new_comps = new ArrayList<Component>();
        for (Component comp : components) {
            ComponentIndex index = indices.get(comp.getName());
            if (index == null) {
                throw new IllegalArgumentException("Missing component index");
            }


            NameEnv env = new TopLevelEnv(globalEnv, index, errors);
            Set<IdOrOpOrAnonymousName> onDemandImports = new HashSet<IdOrOpOrAnonymousName>();

            SelfParamDisambiguator self_disambig = new SelfParamDisambiguator();
            Component spdResult = (Component) comp.accept(self_disambig);

            List<StaticError> newErrs = new ArrayList<StaticError>();
            TypeDisambiguator td =
                new TypeDisambiguator(env, onDemandImports, newErrs);
            Component tdResult = (Component) spdResult.accept(td);
            if (newErrs.isEmpty())
            	new_comps.add(tdResult);
            else
            	errors.addAll(newErrs);
        }

        // Then, rebuild the component indices based on disambiguated types
        IndexBuilder.ComponentResult new_comp_ir =
        	IndexBuilder.buildComponents(new_comps, System.currentTimeMillis());

        for( Component comp : new_comps ) {
            ComponentIndex index = new_comp_ir.components().get(comp.getName());
            if (index == null) {
                throw new IllegalArgumentException("Missing component index");
            }

            // Filter env based on what this component imports
//       	 	Map<APIName,ApiIndex> filtered = filterApis(globalEnv.apis(), comp);
//       	 	GlobalEnvironment filtered_global_env = new GlobalEnvironment.FromMap(filtered);
            NameEnv env = new TopLevelEnv(globalEnv, index, errors);

            // Check the set of exported APIs in the components.
            checkExports(index, errors);
            if ( !errors.isEmpty() )
                return new ComponentResult(results, errors);

            // Finally, disambiguate the expressions
            List<StaticError> newErrs = new ArrayList<StaticError>();
            ExprDisambiguator ed =
                new ExprDisambiguator(env, //onDemandImports,
                		newErrs);
            Component edResult = (Component) comp.accept(ed);
            if (newErrs.isEmpty())
            	results.add(edResult);
            else
            	errors.addAll(newErrs);
        }

        return new ComponentResult(results, errors);
    }

    /* Check the set of exported APIs in this component:
     *   An API must not be imported and exported
     *   by the same component.
     */
    private static void checkExports(ComponentIndex component,
                                     List<StaticError> errors) {
        // No API may be both imported and exported by the same component.
        Set<APIName> imports = component.imports();
        for (APIName exp : component.exports()) {
            if ( imports.contains(exp) )
                error(errors, exp,
                      "Component " + component.ast().getName() +
                      " imports and exports API " + exp + ".\n" +
                      "    An API must not be imported and exported" +
                      " by the same component.");
        }
    }

    private static void error(List<StaticError> errors, HasAt loc, String msg) {
        errors.add(StaticError.make(msg, loc));
    }
}
