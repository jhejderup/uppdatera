package com.github.jhejderup.wala;


import com.github.jhejderup.data.MavenCoordinate;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileEntry;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class WalaCallgraphConstructor {

    private final static Pattern pattern = Pattern.compile("m2\\/repository\\/?(?<group>.*)\\/(?<artifact>[^\\/]*)\\/(?<version>[^\\/]*)\\/([^\\/]*).jar");

    public static void build(String classpath) throws IOException, ClassHierarchyException, CallGraphBuilderCancelException {
        //1. Fetch exclusion file
        ClassLoader classLoader = WalaCallgraphConstructor.class.getClassLoader();
        File exclusionFile = new File(classLoader.getResource("Java60RegressionExclusions.txt").getFile());

        //2. Set the analysis scope
        com.ibm.wala.ipa.callgraph.AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(classpath, exclusionFile);

        //3. Class Hierarchy for name resolution -> missing superclasses are replaced by the ClassHierarchy root,
        //   i.e. java.lang.Object
        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);

        getMethods(cha);

//        //4. Specify Entrypoints -> all non-primordial public entrypoints (also with declared parameters, not sub-types)
//        ArrayList<Entrypoint> entryPoints = getEntrypoints(cha);
//
//        //5. Encapsulates various analysis options
//        AnalysisOptions options = new AnalysisOptions(scope, entryPoints);
//        AnalysisCache cache = new AnalysisCacheImpl();
//
//
//        //6 Build the call graph
//                //0-CFA points-to analysis
//                // CallGraphBuilder builder = Util.makeZeroCFABuilder(Language.JAVA, options, cache, cha, scope);
//        CallGraphBuilder builder = Util.makeRTABuilder(options, cache, cha, scope);
//        CallGraph cg = builder.makeCallGraph(options, null);
//




    }


    private static void getMethods(ClassHierarchy cha) {
        Iterable<IClass> classes = () -> cha.getLoader(ClassLoaderReference.Application).iterateAllClasses();


        Stream<IMethod> methods = StreamSupport.stream(classes.spliterator(), false)
                .flatMap(klass -> klass.getDeclaredMethods().parallelStream());

        methods.forEach(m -> {
            System.out.println(m);
            Optional<IMethod> inheritM = getOverriden(m);

            //Check inheritance
            if(inheritM.isPresent()){
                System.out.println(">>> inherits");
                System.out.println(inheritM.get());
            } else {
                //Check interfaces
                Optional<IMethod> ifaceM = getImplemented(m);
                if (ifaceM.isPresent()) {
                    System.out.println(">>> implements");
                    System.out.println(ifaceM.get());
                } else {
                    System.out.println("IMPLEMENTATION");
                }

            }
            System.out.println("-----");
        });



    }
    ///
    /// Get overriden or implemented method
    ///
    private static Optional<IMethod>  getOverriden(IMethod method) {
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
                .getAllImplementedInterfaces()
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
    /// Creating list of Entrypoints (stuff taken from  woutrrr/lapp)
    ///
    private static ArrayList<Entrypoint> getEntrypoints(ClassHierarchy cha) {
        Iterable<IClass> iterable = () -> cha.iterator();
        List<Entrypoint> entryPoints = StreamSupport.stream(iterable.spliterator(), false)
                .filter(WalaCallgraphConstructor::acceptClassForEntryPoints)
                .flatMap(klass -> klass.getAllMethods().parallelStream())
                .filter(WalaCallgraphConstructor::acceptMethodAsEntryPoint)
                .map(m -> new DefaultEntrypoint(m, cha))
                .collect(Collectors.toList());
        return new ArrayList<>(entryPoints);
    }

    private static boolean acceptClassForEntryPoints(IClass klass) {
        return isApplication(klass)
                && !klass.isInterface()
                && klass.isPublic();
    }

    private static boolean acceptMethodAsEntryPoint(IMethod method) {
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
    private static MavenCoordinate getMavenCoordinate(String path) {
            Matcher matcher = pattern.matcher(path);
            matcher.find();
            return new MavenCoordinate(
                    matcher.group("group").replace('/', '.'),
                    matcher.group("artifact"),
                    matcher.group("version")
            );
    }

    private static String fetchJarFile(IClass klass) throws IOException {
        ShrikeClass shrikeKlass = (ShrikeClass) klass;
        JarFileEntry moduleEntry = (JarFileEntry) shrikeKlass.getModuleEntry();
        JarFile jarFile = moduleEntry.getJarFile();
        String jarPath = jarFile.getName();
        jarFile.close();
        return jarPath;
    }





}
