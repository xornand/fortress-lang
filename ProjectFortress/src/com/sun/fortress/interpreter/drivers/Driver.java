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

package com.sun.fortress.interpreter.drivers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import xtc.parser.ParseError;
import xtc.parser.Result;
import xtc.parser.SemanticValue;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.env.FortressTests;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.Init;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.tasks.EvaluatorTask;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunnerGroup;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.interpreter.nodes.AliasedDottedId;
import com.sun.fortress.interpreter.nodes.AliasedName;
import com.sun.fortress.interpreter.nodes.Api;
import com.sun.fortress.interpreter.nodes.CompilationUnit;
import com.sun.fortress.interpreter.nodes.Component;
import com.sun.fortress.interpreter.nodes.DottedId;
import com.sun.fortress.interpreter.nodes.FnName;
import com.sun.fortress.interpreter.nodes.Import;
import com.sun.fortress.interpreter.nodes.ImportApi;
import com.sun.fortress.interpreter.nodes.ImportFrom;
import com.sun.fortress.interpreter.nodes.ImportNames;
import com.sun.fortress.interpreter.nodes.ImportStar;
import com.sun.fortress.interpreter.nodes.Name;
import com.sun.fortress.interpreter.nodes.Option;
import com.sun.fortress.interpreter.nodes.Printer;
import com.sun.fortress.interpreter.nodes.Unprinter;
import com.sun.fortress.interpreter.parser.Fortress;
import com.sun.fortress.interpreter.reader.Lex;
import com.sun.fortress.interpreter.rewrite.Disambiguate;
import com.sun.fortress.interpreter.useful.CheckedNullPointerException;
import com.sun.fortress.interpreter.useful.Fn;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.PureList;
import com.sun.fortress.interpreter.useful.NI;
import com.sun.fortress.interpreter.useful.Useful;
import com.sun.fortress.interpreter.useful.Visitor2;
import com.sun.fortress.interpreter.typechecker.TypeChecker;
import com.sun.fortress.interpreter.typechecker.TypeError;
import com.sun.fortress.interpreter.typechecker.TypeCheckerResult;

public class Driver {

    private static int numThreads = 8;

    private static boolean _libraryTest = false;
    
    private static String LIB_DIR = ProjectProperties.TEST_LIB_DIR;

    private Driver() {
    };

    public static CompilationUnit readJavaAst(String fileName)
            throws IOException {
        BufferedReader br = Useful.utf8BufferedFileReader(fileName);
        return readJavaAst(fileName, br);

    }

    /**
     * @param reportedFileName
     * @param br
     * @return
     * @throws IOException
     */
    public static CompilationUnit readJavaAst(String reportedFileName,
            BufferedReader br) throws IOException {
        Lex lex = new Lex(br);
        try {
            Unprinter up = new Unprinter(lex);
            lex.name();
            CompilationUnit p = (CompilationUnit) up.readNode(lex.name());
            return p;
        } finally {
            if (!lex.atEOF())
                System.out.println("Parse of " + reportedFileName
                        + " ended EARLY at line = " + lex.line()
                        + ",  column = " + lex.column());
        }
    }

    public static CompilationUnit parseToJavaAst(String reportedFileName,
            BufferedReader in) throws IOException {
        Fortress p = new Fortress(in, reportedFileName, (int) new File(
                reportedFileName).length());
        Result r = p.pFile(0);

        if (r.hasValue()) {
            SemanticValue v = (SemanticValue) r;
            CompilationUnit n = (CompilationUnit) v.value;
            return n;
        } else {
            ParseError err = (ParseError) r;
            if (-1 == err.index) {
                System.err.println("  Parse error");
            } else {
                System.err.println("  " + p.location(err.index) + ": "
                        + err.msg);
            }
            return null;
        }
    }

    /**
     * Perform static analysis on the given program.
     * @return  {@code true} iff the program is considered well-formed by the type checker.
     */
    public static TypeCheckerResult check(CompilationUnit p) {
        TypeCheckerResult result = TypeChecker.check(p); 
        PureList<TypeError> errors = result.getErrors();
        
        for (TypeError error : errors) {
            System.err.println("Static error" + error.getLocation() + ": " + error.getMessage());
        }
        if (result.hasErrors()) {
            System.err.println("THERE WERE " + errors.size() + " STATIC ERRORS");
        }
        return result;
    }
    
    /**
     * Runs a command and captures its output and errors streams.
     * 
     * @param command
     *            The command to run
     * @param output
     *            Output from the command is written here.
     * @param errors
     *            Errors from the command are written here.
     * @param exceptions
     *            If the execution of the command throws an exception, it is
     *            stored here.
     * @return true iff any errors were written.
     * @throws IOException
     */
    public static boolean runCommand(String command, final PrintStream output,
            final IOException[] exceptions, PrintStream errors)
            throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process p = runtime.exec(command);
        final BufferedReader input_from_process = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
        final BufferedReader errors_from_process = new BufferedReader(
                new InputStreamReader(p.getErrorStream()));

        boolean errors_encountered = false;
        Thread th = new Thread() {
            public void run() {
                try {
                    try {
                        String line = input_from_process.readLine();
                        while (line != null) {
                            output.println(line);
                            line = input_from_process.readLine();
                        }
                    } finally {
                        output.close();
                        input_from_process.close();
                    }
                } catch (IOException ex) {
                    exceptions[0] = ex;
                }
            }
        };

        th.start();

        // Print errors, discarding any leading blank lines.
        String line = errors_from_process.readLine();
        boolean first = true;

        while (line != null) {
            if (!first || line.trim().length() > 0) {
                errors.println(line);
                first = false;
                errors_encountered = true;
            }
            line = errors_from_process.readLine();
        }

        try {
            th.join();
        } catch (InterruptedException ex) {

        }
        return errors_encountered;
    }

    public static void writeJavaAst(CompilationUnit p, String s)
            throws IOException {
        BufferedWriter fout = Useful.utf8BufferedFileWriter(s);
        writeJavaAst(p, fout);
        fout.close();
    }

    /**
     * @param p
     * @param fout
     * @throws IOException
     */
    public static void writeJavaAst(CompilationUnit p, BufferedWriter fout)
            throws IOException {
        (new Printer()).dump(p, fout, 0);
    }

    static public void runTests() {
    }

    public static BetterEnv evalComponent(CompilationUnit p) {

        Init.initializeEverything();

        /*
         * Begin "linker" -- to be replaced with the real thing, when more
         * infrastructure is present.
         */

        HashMap<String, ComponentWrapper> linker = new HashMap<String, ComponentWrapper>();

        Stack<ComponentWrapper> pile = new Stack<ComponentWrapper>();
        ArrayList<ComponentWrapper> components = new ArrayList<ComponentWrapper>();

        ComponentWrapper comp = new ComponentWrapper((Component) p);

        /*
         * This "linker" implements a one-to-one, same-name correspondence
         * between APIs and components.
         * 
         * phase 0.5: starting with the main component, identify other
         * components that will be needed (the fortress will contain the map
         * from component C's imported APIs to their implementing components).
         * Each component will be paired with its top level environment (TLE).
         * 
         * phase 1: for each component, prepare the TLE by creating all name
         * slots (nothing in the slots will is initialized AT ALL -- it's all
         * thunks, empty mutables, and uninitialized types).
         * 
         * phase 1.5: for each component C, add to C's TLE all the names that C
         * imports (using the API->component map from the fortress to figure out
         * the values/types associated with each of those names).
         * 
         */
        comp.populateEnvironment();
        linker.put("main", comp);
        pile.push(comp);

        
        /*
         * This is a patch; eventually, it will all be done explicitly.
         */
        ComponentWrapper lib = new ComponentWrapper(Libraries.theLibrary());
        lib.getEnvironment().installPrimitives();
        lib.populateEnvironment();
        pile.push(lib);

        /*
         * This performs closure over APIs and components, ensuring that all are
         * initialized through phase one of building environments.
         */
        while (!pile.isEmpty()) {

            ComponentWrapper cw = pile.pop();
            components.add(cw);

            CompilationUnit c = cw.getComponent();
            List<Import> imports = c.getImports();

            ensureImportsImplemented(linker, pile, imports);
        }

        /*
         * Transitional stuff.
         * 
         * This will stick native hooks into the "library", and these will in
         * turn be copied into all the other environments.
         */
        Glue.installHooks(lib.getEnvironment());

        /*
         * Inject imported names into environments.
         * 
         * TODO traits and objects must have their members filtered in these
         * injections. This is not at all simple in the case of a component
         * exporting multiple APIs that may also have different views of the
         * same trait, when some subset of those APIs is imported in another
         * component. Because this is not simple, because the many-to-many case
         * is not yet handled, it is not yet implemented. Getting this right
         * will probably affect the code that builds object types and deals with
         * overloading there.
         */
        for (ComponentWrapper cw : components) {
            CompilationUnit c = cw.getComponent();
            List<Import> imports = c.getImports();
            final BetterEnv e = cw.getEnvironment();

            /*
             * Transitional stuff. Import everything from "library" into a
             * Component.
             */

            if (cw != lib)
                importAllExcept(e, lib.getEnvironment(), lib.getEnvironment(),
                        Collections.<String> emptyList(), "FortressLibrary",
                        "FortressLibrary");

            for (Import i : imports) {
                if (i instanceof ImportApi) {
                    ImportApi ix = (ImportApi) i;
                    List<AliasedDottedId> apis = ix.getApis();
                    for (AliasedDottedId adi : apis) {
                        DottedId id = adi.getId();
                        String from_apiname = id.toString();

                        Option<DottedId> alias = adi.getAlias();
                        String known_as = alias.getVal(id).toString();

                        ComponentWrapper from_cw = linker.get(from_apiname);

                        /*
                         * Every name N in api A with optional alias B is added
                         * to e, using the value from the component C
                         * implementing A. If B is present, C.N is referenced as
                         * B.N, else it is referenced as A.N.
                         */

                        /*
                         * Not-yet-implemented because of issues with selectors.
                         */
                        NI.nyi("Import of dotted name");
                    }

                } else if (i instanceof ImportFrom) {
                    ImportFrom ix = (ImportFrom) i;
                    DottedId source = ix.getSource();
                    String from_apiname = source.toString();

                    ComponentWrapper from_cw = linker.get(from_apiname);
                    BetterEnv from_e = from_cw.getEnvironment();
                    BetterEnv api_e = from_cw.getExportedCW(from_apiname)
                            .getEnvironment();

                    /* Pull in names, UNqualified */

                    if (ix instanceof ImportNames) {
                        /* A set of names */
                        List<AliasedName> names = ((ImportNames) ix).getNames();
                        for (AliasedName an : names) {
                            FnName name = an.getName();
                            Option<FnName> alias = an.getAlias();
                            /*
                             * If alias exists, associate the binding from
                             * component_wrapper with alias, otherwise associate
                             * it with plain old name.
                             */

                            inject(e, api_e, from_e, name, alias, from_apiname,
                                    from_cw.getComponent().getName().toString());

                        }

                    } else if (ix instanceof ImportStar) {
                        /* All names BUT excepts, as they are listed. */
                        final List<Name> excepts = ((ImportStar) ix)
                                .getExcept();
                        final List<String> except_names = Useful.applyToAll(
                                excepts, new Fn<Name, String>() {
                                    public String apply(Name n) {
                                        return n.name();
                                    }
                                });

                        importAllExcept(e, api_e, from_e, except_names,
                                from_apiname, from_cw.getComponent().getName()
                                        .toString());

                    }
                } else {

                }
            }
        }

        for (ComponentWrapper cw : components) {
            cw.initTypes();
        }
        for (ComponentWrapper cw : components) {
            cw.initFuncs();
        }
        for (ComponentWrapper cw : components) {
            cw.initVars();
        }

        // Libraries.link(be, dis);

        return comp.getEnvironment();
    }

    /**
     * @param into_e
     * @param from_e
     * @param except_names
     */
    private static void importAllExcept(final BetterEnv into_e,
            BetterEnv api_e, final BetterEnv from_e,
            final List<String> except_names, final String a, final String c) {

        Visitor2<String, FType> vt = new Visitor2<String, FType>() {
            public void visit(String s, FType o) {
                try {
                    if (!except_names.contains(s)) {
                        into_e.putType(s, NI.cnnf(from_e.getTypeNull(s)));
                    }
                } catch (CheckedNullPointerException ex) {
                    throw new ProgramError("Import of " + s + " from api " + a
                            + " not found in implementing component " + c);
                }
            }
        };
        /*
         * Potential problem here, we have to make overloading work when we put
         * a function.
         */
        Visitor2<String, FValue> vv = new Visitor2<String, FValue>() {
            public void visit(String s, FValue o) {
                try {
                    if (!except_names.contains(s)) {
                        into_e.putValue(s, NI.cnnf(from_e.getValueRaw(s)));
                    }
                } catch (CheckedNullPointerException ex) {
                    throw new ProgramError("Import of " + s + " from api " + a
                            + " not found in implementing component " + c);
                }
            }
        };
        Visitor2<String, Number> vi = new Visitor2<String, Number>() {
            public void visit(String s, Number o) {
                try {
                    if (!except_names.contains(s)) {
                        into_e.putInt(s, NI.cnnf(from_e.getIntNull(s)));
                    }
                } catch (CheckedNullPointerException ex) {
                    throw new ProgramError("Import of " + s + " from api " + a
                            + " not found in implementing component " + c);
                }
            }
        };
        Visitor2<String, Number> vn = new Visitor2<String, Number>() {
            public void visit(String s, Number o) {
                try {
                    if (!except_names.contains(s)) {
                        into_e.putNat(s, NI.cnnf(from_e.getNat(s)));
                    }
                } catch (CheckedNullPointerException ex) {
                    throw new ProgramError("Import of " + s + " from api " + a
                            + " not found in implementing component " + c);
                }
            }
        };
        Visitor2<String, Boolean> vb = new Visitor2<String, Boolean>() {
            public void visit(String s, Boolean o) {
                try {
                    if (!except_names.contains(s)) {
                        into_e.putBool(s, NI.cnnf(from_e.getBool(s)));
                    }
                } catch (CheckedNullPointerException ex) {
                    throw new ProgramError("Import of " + s + " from api " + a
                            + " not found in implementing component " + c);
                }
            }
        };

        api_e.visit(vt, vn, vi, vv, vb);
    }

    private static void inject(BetterEnv e, BetterEnv api_e, BetterEnv from_e,
            FnName name, Option<FnName> alias, String a, String c) {
        String s = name.name();
        String add_as = s;
        if (alias.isPresent()) {
            add_as = alias.getVal().name();
        }
        try {
            if (api_e.getNatNull(s) != null) {
                e.putNat(add_as, NI.cnnf(from_e.getNatNull(s)));
            }
            if (api_e.getIntNull(s) != null) {
                e.putInt(add_as, NI.cnnf(from_e.getIntNull(s)));
            }
            if (api_e.getBoolNull(s) != null) {
                e.putBool(add_as, NI.cnnf(from_e.getBoolNull(s)));
            }
            if (api_e.getTypeNull(s) != null) {
                e.putType(add_as, NI.cnnf(from_e.getTypeNull(s)));
            }
            if (api_e.getValueRaw(s) != null) {
                e.putValue(add_as, NI.cnnf(from_e.getValueRaw(s)));
            }
        } catch (CheckedNullPointerException ex) {
            throw new ProgramError("Import of " + name + " from api " + a
                    + " not found in implementing component " + c);
        }

    }

    /**
     * For each imported API, checks to see if it is already "linked". If not,
     * "finds" it (looks for apiname.fss/apiname.tfs) and reads it in and sticks
     * it in the "pile".
     * 
     * For each component, also reads in the corresponding APIs so that we know
     * what to export.
     * 
     * @param linker
     * @param pile
     * @param imports
     */
    private static void ensureImportsImplemented(
            HashMap<String, ComponentWrapper> linker,
            Stack<ComponentWrapper> pile, List<Import> imports) {

        for (Import i : imports) {
            if (i instanceof ImportApi) {
                ImportApi ix = (ImportApi) i;
                List<AliasedDottedId> apis = ix.getApis();
                for (AliasedDottedId adi : apis) {
                    DottedId id = adi.getId();
                    ensureApiImplemented(linker, pile, id);

                }

            } else if (i instanceof ImportFrom) {
                ImportFrom ix = (ImportFrom) i;
                DottedId source = ix.getSource();
                ensureApiImplemented(linker, pile, source);
            } else {
                throw new InterpreterError(i,"Unrecognized import");
            }
        }
    }

    /**
     * @param linker
     * @param pile
     * @param id
     */
    private static void ensureApiImplemented(
            HashMap<String, ComponentWrapper> linker,
            Stack<ComponentWrapper> pile, DottedId id) {
        String apiname = id.toString();
        if (linker.get(apiname) == null) {
            /*
             * Here, the linker prototype takes the extreme shortcut of assuming
             * that Api Foo is implemented by Component Foo, dots and all.
             * 
             * These two lines are what needs to be replaced by a real linker.
             */
            Api newapi = readTreeOrSourceApi(LIB_DIR + apiname);
            Component newcomp = readTreeOrSourceComponent(LIB_DIR + apiname);
            
            ComponentWrapper apicw = new ComponentWrapper(newapi);
            ComponentWrapper newwrapper = new ComponentWrapper(newcomp, apicw);
            newwrapper.populateEnvironment();
            linker.put(apiname, newwrapper);
            pile.push(newwrapper);

            List<Import> imports = newcomp.getImports();
            ensureImportsImplemented(linker, pile, imports);

            imports = newapi.getImports();
            ensureImportsImplemented(linker, pile, imports);
        }
    }

    // This runs the program from inside a task.
    public static void runProgramTask(CompilationUnit p, boolean runTests,
            List<String> args) {

        FortressTests.reset();
        BetterEnv e = evalComponent(p);

        Closure run_fn = e.getRunMethod();
        Toplevel toplevel = new Toplevel();
        if (runTests) {
            List<Closure> testClosures = FortressTests.get();
            for (Iterator<Closure> i = testClosures.iterator(); i.hasNext();) {
                Closure testCl = i.next();
                List<FValue> fvalue_args = new ArrayList<FValue>();

                testCl.apply(fvalue_args, toplevel, e);
            }
        }
        ArrayList<FValue> fvalueArgs = new ArrayList<FValue>();
        for (String s : args) {
            fvalueArgs.add(FString.make(s));
        }
        FValue ret = run_fn.apply(fvalueArgs, toplevel, e);
        // try {
        // e.dump(System.out);
        // } catch (IOException ioe) {
        // System.out.println("io exception" + ioe);
        // }
        if (!(ret instanceof FVoid))
            throw new ProgramError("run method returned non-void value");
        System.out.println("finish runProgram");
    }

    static FortressTaskRunnerGroup group;

    // This creates the parallel context
    public static void runProgram(CompilationUnit p, boolean runTests,
            boolean libraryTest, List<String> args) throws Throwable {
        _libraryTest = libraryTest;
        String numThreadsString = System.getenv("NumFortressThreads");
        if (numThreadsString != null)
            numThreads = Integer.parseInt(numThreadsString);

        if (group == null)

           group = new FortressTaskRunnerGroup(numThreads);

        EvaluatorTask evTask = new EvaluatorTask(p, runTests, args, null);
        try {
            group.invoke(evTask);
        } catch (InterruptedException ex) {
        } finally {
            // group.interruptAll();
        }
        if (evTask.causedException()) {
            Throwable th = evTask.getException();
            throw th;
        }
    }

    public static void runProgram(CompilationUnit p, boolean runTests,
            List<String> args) throws Throwable {
        runProgram(p, runTests, false, args);
    }

    private static class Toplevel implements HasAt {
        public String at() {
            return "toplevel";
        }

        public String stringName() {
            return "driver";
        }
    }

    public static Component readTreeOrSourceComponent(String basename) {
        return (Component) readTreeOrSource(basename + ".fss", basename
                + ".tfs");
    }

    public static Api readTreeOrSourceApi(String basename) {
        return (Api) readTreeOrSource(basename + ".fsi", basename + ".tfi");
    }

    /**
     * Attempts to read in preparsed program. Reparses if parsed form is missing
     * or newer than preparsed form.
     * 
     * @param librarySource
     * @param libraryTree
     * @return
     */
    public static CompilationUnit readTreeOrSource(String librarySource,
            String libraryTree) {
        CompilationUnit c = null;
        if (Useful.olderThanOrMissing(libraryTree, librarySource)) {
            try {
                System.err
                        .println("Missing or stale preparsed AST " + libraryTree + ", rebuilding from source " + librarySource );
                long begin = System.currentTimeMillis();

                c = parseToJavaAst(librarySource, Useful
                        .utf8BufferedFileReader(librarySource));

                System.err.println("Parsed " + librarySource + ": "
                        + (System.currentTimeMillis() - begin)
                        + " milliseconds");
                writeJavaAst(c, libraryTree);
            } catch (Throwable ex) {
                System.err.println("Trouble preparsing library AST.");
                ex.printStackTrace();
            }

        } else
            try {
                long begin = System.currentTimeMillis();
                c = readJavaAst(libraryTree);
                System.err.println("Read " + libraryTree + ": "
                        + (System.currentTimeMillis() - begin)
                        + " milliseconds");
            } catch (Throwable ex) {
                System.err.println("Trouble reading preparsed library AST.");
                ex.printStackTrace();
            }
        return c;
    }

}
