package com.github.jhejderup.callgraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test suite for testing simple use cases of the CallgraphConstructor.
 */
// TODO: write test for polymorphism (abstract classes + interfaces)
// TODO: write test for calling static methods
// TODO: write test with lambda expression
public abstract class CallgraphConstructorTest {

    /**
     * Fake class for testing purposes.
     */
    private static class FakeResolvedMethod extends ResolvedMethod {

        private JVMIdentifier identifier;
        private MethodScope scope;

        FakeResolvedMethod(JVMIdentifier identifier, MethodScope scope) {
            this.identifier = identifier;
            this.scope = scope;
        }

        @Override
        public JVMIdentifier getIdentifier() {
            return identifier;
        }

        @Override
        public MethodScope getScope() {
            return scope;
        }
    }

    @Test
    public void testLinearCallgraphSingleDependency() throws CallgraphException {
        String projectClasspath = "data/test1/classpath";
        String dependenciesClasspath = "data/test1/dependency/test1dep.jar";

        // Setup calls
        JVMIdentifier appMainID = new JVMIdentifier("Lcom/company/test/Main", "main", "([Ljava/lang/String;)V");
        JVMIdentifier depConstrID = new JVMIdentifier("Lcom/company/dep/DependencyAPI", "<init>", "()V");
        JVMIdentifier depFuncID = new JVMIdentifier("Lcom/company/dep/DependencyAPI", "dependencyFunc", "()V");
        JVMIdentifier depFuncInternalID = new JVMIdentifier("Lcom/company/dep/DependencyAPI", "internalFunc", "()V");

        ResolvedMethod appMain = new FakeResolvedMethod(appMainID, MethodScope.APPLICATION);
        ResolvedMethod depConstr = new FakeResolvedMethod(depConstrID, MethodScope.DEPENDENCY);
        ResolvedMethod depFunc = new FakeResolvedMethod(depFuncID, MethodScope.DEPENDENCY);
        ResolvedMethod depFuncInternal = new FakeResolvedMethod(depFuncInternalID, MethodScope.DEPENDENCY);

        ResolvedCall call1 = new ResolvedCall(appMain, depConstr);
        ResolvedCall call2 = new ResolvedCall(appMain, depFunc);
        ResolvedCall call3 = new ResolvedCall(depFunc, depFuncInternal);

        // Run callgraph constructor
        var resolvedCalls = getConstructor().build(projectClasspath, dependenciesClasspath);

        // Make assertions
        assertEquals(3, resolvedCalls.size());
        assertTrue(resolvedCalls.contains(call1));
        assertTrue(resolvedCalls.contains(call2));
        assertTrue(resolvedCalls.contains(call3));
    }

    @Test
    public void testLinearCallgraphNestedDependencies() throws CallgraphException {
        String projectClasspath = "data/test2/classpath";
        String dependenciesClasspath = "data/test2/dependency/test2dep1.jar:data/test2/dependency/test2dep2.jar";

        // Setup calls
        JVMIdentifier appMainID = new JVMIdentifier("Lcom/company/test/Main", "main", "([Ljava/lang/String;)V");
        JVMIdentifier dep1ConstrID = new JVMIdentifier("Lcom/company/dep1/Dependency1API", "<init>", "()V");
        JVMIdentifier dep1FuncID = new JVMIdentifier("Lcom/company/dep1/Dependency1API", "dependency1Func", "()V");
        JVMIdentifier dep2ConstrID = new JVMIdentifier("Lcom/company/dep2/Dependency2API", "<init>", "()V");
        JVMIdentifier dep2FuncID = new JVMIdentifier("Lcom/company/dep2/Dependency2API", "dependency2Func", "()V");
        JVMIdentifier dep2FuncInternalID = new JVMIdentifier("Lcom/company/dep2/Dependency2API", "internalFunc", "()V");

        ResolvedMethod appMain = new FakeResolvedMethod(appMainID, MethodScope.APPLICATION);
        ResolvedMethod dep1Constr = new FakeResolvedMethod(dep1ConstrID, MethodScope.DEPENDENCY);
        ResolvedMethod dep1Func = new FakeResolvedMethod(dep1FuncID, MethodScope.DEPENDENCY);
        ResolvedMethod dep2Constr = new FakeResolvedMethod(dep2ConstrID, MethodScope.DEPENDENCY);
        ResolvedMethod dep2Func = new FakeResolvedMethod(dep2FuncID, MethodScope.DEPENDENCY);
        ResolvedMethod dep2FuncInternal = new FakeResolvedMethod(dep2FuncInternalID, MethodScope.DEPENDENCY);

        ResolvedCall call1 = new ResolvedCall(appMain, dep1Constr);
        ResolvedCall call2 = new ResolvedCall(appMain, dep1Func);
        ResolvedCall call3 = new ResolvedCall(dep1Func, dep2Constr);
        ResolvedCall call4 = new ResolvedCall(dep1Func, dep2Func);
        ResolvedCall call5 = new ResolvedCall(dep2Func, dep2FuncInternal);

        // Run callgraph constructor
        var resolvedCalls = getConstructor().build(projectClasspath, dependenciesClasspath);

        // Make assertions
        assertEquals(5, resolvedCalls.size());
        assertTrue(resolvedCalls.contains(call1));
        assertTrue(resolvedCalls.contains(call2));
        assertTrue(resolvedCalls.contains(call3));
        assertTrue(resolvedCalls.contains(call4));
        assertTrue(resolvedCalls.contains(call5));
    }

    protected abstract CallgraphConstructor getConstructor();

}
