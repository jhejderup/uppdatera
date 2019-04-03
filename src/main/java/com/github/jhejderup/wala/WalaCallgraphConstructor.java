package com.github.jhejderup.wala;

import com.github.jhejderup.data.*;
import com.github.jhejderup.data.ufi.UFI;
import com.github.jhejderup.data.ufi.UniversalType;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class WalaCallgraphConstructor {


    //A filter that accepts WALA objects that "belong" to the application loader.
    private static Predicate<CGNode> applicationLoaderFilter =
            node -> isApplication(node.getMethod().getDeclaringClass());

    public static void build(String classpath) throws IOException, WalaException, CallGraphBuilderCancelException {
        //1. Fetch exclusion file
        ClassLoader classLoader = WalaCallgraphConstructor.class.getClassLoader();
        File exclusionFile = new File(classLoader.getResource("Java60RegressionExclusions.txt").getFile());

        //2. Set the analysis scope
        com.ibm.wala.ipa.callgraph.AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(classpath, exclusionFile);

        //3. Class Hierarchy for name resolution -> missing superclasses are replaced by the ClassHierarchy root,
        //   i.e. java.lang.Object
        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);

        //4. Specify Entrypoints -> all non-primordial public entrypoints (also with declared parameters, not sub-types)
        ArrayList<Entrypoint> entryPoints = getEntrypoints(cha);


        //5. Encapsulates various analysis options
        AnalysisOptions options = new AnalysisOptions(scope, entryPoints);


        AnalysisCache cache = new AnalysisCacheImpl();

        //6 Build the call graph
        //0-CFA points-to analysis
        // CallGraphBuilder builder = Util.makeZeroCFABuilder(Language.JAVA, options, cache, cha, scope);
        CallGraphBuilder builder = Util.makeRTABuilder(options, cache, cha, scope);
        CallGraph cg = builder.makeCallGraph(options, null);

        ArrayList<MethodHierarchy> methods = new ArrayList<>(getAllMethods(cha));
        ArrayList<ResolvedCall> calls = new ArrayList<>(getResolvedCalls(cg));


        calls.stream()
                .flatMap(call -> Stream.of(convertToUFI(call.source), convertToUFI(call.target)))
                .forEach(System.out::println);
    }

    //Resolve reference to actual method
    //TODO: Understand why some are not resolved (like abstract class, interfaces)
    //Finding the class is not an issue, but comes when we look up the method
    //best to be careful here: we should only call implementations!
    private static List<ResolvedCall> getResolvedCalls(CallGraph cg) {
        Iterable<CGNode> cgNodes = () -> cg.iterator();
        List<ResolvedCall> calls = StreamSupport
                .stream(cgNodes.spliterator(), false)
                .filter(applicationLoaderFilter)
                .flatMap(node -> {
                    Iterable<CallSiteReference> callSites = () -> node.iterateCallSites();
                    return StreamSupport
                            .stream(callSites.spliterator(), false)
                            .map(callsite -> {
                                MethodReference ref = callsite.getDeclaredTarget();
                                IMethod target = cg.getClassHierarchy().resolveMethod(ref);
                                if (target == null)
                                    return null;
                                else
                                    return new ResolvedCall(node.getMethod(), callsite.getInvocationCode(), target);
                            });
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return calls;
    }

    private static List<MethodHierarchy> getAllMethods(ClassHierarchy cha) {
        Iterable<IClass> classes = () -> cha.getLoader(ClassLoaderReference.Application).iterateAllClasses();


        Stream<IMethod> methods = StreamSupport.stream(classes.spliterator(), false)
                .flatMap(klass -> klass.getDeclaredMethods().parallelStream());

        List<MethodHierarchy> info = methods.map(m -> {
            //Check inheritance
            Optional<IMethod> inheritM = getOverriden(m);

            if (inheritM.isPresent()) {
                return new MethodHierarchy(m, MethodHierarchy.Relation.OVERRIDES, inheritM);
            } else {
                //Check implemented interfaces
                Optional<IMethod> ifaceM = getImplemented(m);
                if (ifaceM.isPresent()) {
                    return new MethodHierarchy(m, MethodHierarchy.Relation.IMPLEMENTS, ifaceM);
                } else {
                    return new MethodHierarchy(m, MethodHierarchy.Relation.CONCRETE, Optional.empty());

                }
            }
        }).collect(Collectors.toList());
        return info;
    }

    ///
    /// Get overriden or implemented method
    ///
    private static Optional<IMethod> getOverriden(IMethod method) {
        IClass c = method.getDeclaringClass();
        IClass parent = c.getSuperclass();
        if (parent == null) {
            return Optional.empty();
        } else {
            MethodReference ref = MethodReference.findOrCreate(parent.getReference(), method.getSelector());
            IMethod m2 = method.getClassHierarchy().resolveMethod(ref);
            if (m2 != null && !m2.equals(method)) {
                return Optional.of(m2);
            }
            return Optional.empty();
        }
    }

    private static Optional<IMethod> getImplemented(IMethod method) {
        return method.getDeclaringClass()
                .getAllImplementedInterfaces() //As interfaces can extend other interfaces, we get all ancestors
                .stream()
                .map(intrface -> {
                    MethodReference ref = MethodReference.findOrCreate(intrface.getReference(), method.getSelector());
                    IMethod m2 = method.getClassHierarchy().resolveMethod(ref);
                    if (m2 != null && !m2.equals(method)) {
                        return m2;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst();
    }

    ///
    /// Creating list diff Entrypoints (stuff taken from  woutrrr/lapp)
    ///
    private static ArrayList<Entrypoint> getEntrypoints(ClassHierarchy cha) {
        Iterable<IClass> classes = () -> cha.iterator();
        List<Entrypoint> entryPoints = StreamSupport.stream(classes.spliterator(), false)
                .filter(WalaCallgraphConstructor::isPublicClass)
                .flatMap(klass -> klass.getAllMethods().parallelStream())
                .filter(WalaCallgraphConstructor::isPublicMethod)
                .map(m -> new DefaultEntrypoint(m, cha))
                .collect(Collectors.toList());
        return new ArrayList<>(entryPoints);
    }

    ///
    /// Helper functions
    ///
    private static boolean isPublicClass(IClass klass) {
        return isApplication(klass)
                && !klass.isInterface()
                && klass.isPublic();
    }

    private static boolean isPublicMethod(IMethod method) {
        return isApplication(method.getDeclaringClass())
                && method.isPublic()
                && !method.isAbstract();
    }

    private static Boolean isJavaStandardLibrary(IClass klass) {
        return klass.getClassLoader().getReference().equals(ClassLoaderReference.Primordial);
    }

    private static Boolean isApplication(IClass klass) {
        return klass.getClassLoader().getReference().equals(ClassLoaderReference.Application);
    }

    ///
    /// Fetching MavenCoordinate
    ///
    private static String fetchJarFile(IClass klass) throws IOException {
        ShrikeClass shrikeKlass = (ShrikeClass) klass;
        JarFileEntry moduleEntry = (JarFileEntry) shrikeKlass.getModuleEntry();
        JarFile jarFile = moduleEntry.getJarFile();
        String jarPath = jarFile.getName();
        return jarPath;
    }

    private static Optional<Namespace> getGlobalPath(IClass klass) {

        if (klass instanceof ArrayClass) {
            return Optional.of(new JDKClassPath());
        }

        try {
            String jarFile = fetchJarFile(klass);
            if (!jarFile.endsWith("rt.jar")) {
                return Optional.of(MavenCoordinate.of(jarFile));
            } else {
                return Optional.of(new JDKClassPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }


    }

    private static UniversalType resolveTypeRef(IClassHierarchy cha, TypeReference tyref) {
        String s = tyref.getName().toString();
        assert (s.length() > 0);
        switch (s.charAt(0)) {
            case TypeReference.BooleanTypeCode:
                return new UniversalType(
                        Optional.of(new Namespace("PrimitiveType")),
                        new Namespace("boolean"));
            case TypeReference.ByteTypeCode:
                return new UniversalType(
                        Optional.of(new Namespace("PrimitiveType")),
                        new Namespace("byte"));
            case TypeReference.CharTypeCode:
                return new UniversalType(
                        Optional.of(new Namespace("PrimitiveType")),
                        new Namespace("char"));
            case TypeReference.DoubleTypeCode:
                return new UniversalType(
                        Optional.of(new Namespace("PrimitiveType")),
                        new Namespace("double"));
            case TypeReference.FloatTypeCode:
                return new UniversalType(
                        Optional.of(new Namespace("PrimitiveType")),
                        new Namespace("float"));
            case TypeReference.IntTypeCode:
                return new UniversalType(
                        Optional.of(new Namespace("PrimitiveType")),
                        new Namespace("int"));
            case TypeReference.LongTypeCode:
                return new UniversalType(
                        Optional.of(new Namespace("PrimitiveType")),
                        new Namespace("long"));
            case TypeReference.ShortTypeCode:
                return new UniversalType(Optional.of(
                        new Namespace("PrimitiveType")),
                        new Namespace("short"));
            case TypeReference.VoidTypeCode:
                return new UniversalType(Optional.of(
                        new Namespace("PrimitiveType")),
                        new Namespace("void"));
            case TypeReference.ClassTypeCode:
                IClass klass = cha.lookupClass(tyref);
                Namespace inner = klass != null ?
                        new Namespace((klass.getName().toString()).substring(1).split("/")) :
                        new Namespace("_unknownType", tyref.getName().toString());
                Optional<Namespace> outer = klass != null ? getGlobalPath(klass) : Optional.empty();
                return new UniversalType(outer, inner);
            case TypeReference.ArrayTypeCode:
                return new UniversalType(Optional.empty(), new Namespace("TODO[]"));


            case TypeReference.PointerTypeCode:
            case TypeReference.ReferenceTypeCode:
            default:
                throw new IllegalArgumentException("malformed TypeSignature string:" + s);
        }
    }
    ///
    /// Should be made as interface methods
    ///
    public static UFI convertToUFI(IMethod item) {
        Optional<Namespace> outer = getGlobalPath(item.getDeclaringClass());
        Namespace inner = new Namespace((item.getDeclaringClass().getName().toString()).substring(1).split("/"));
        UniversalType path = new UniversalType(outer, inner);
        String methodName = item.getName().toString();
        UniversalType returnType = resolveTypeRef(item.getClassHierarchy(), item.getReturnType());
        Optional<List<UniversalType>> args = item.getNumberOfParameters() > 0 ?
                Optional.of(IntStream.range(0, item.getNumberOfParameters() - 1)
                        .mapToObj(i -> resolveTypeRef(item.getClassHierarchy(), item.getParameterType(i)))
                        .collect(Collectors.toList())) : Optional.empty();
        return new UFI(path, methodName, args, returnType);
    }


    public Map<UFI, IMethod> mapping() {
        return null;
    }
}
