package com.github.jhejderup.callgraph.opal;

import com.github.jhejderup.callgraph.JVMIdentifier;
import com.github.jhejderup.callgraph.MethodScope;
import com.github.jhejderup.callgraph.ResolvedMethod;
import org.opalj.br.Method;
import org.opalj.br.analyses.Project;

public final class OpalResolvedMethod extends ResolvedMethod {

    private final JVMIdentifier identifier;
    private final MethodScope scope;

    public OpalResolvedMethod(Method method, Project<?> project) {
        this.identifier = JVMIdentifier.fromOpalMethod(method);

        if (project.isProjectType(method.declaringClassFile().thisType())) {
            scope = MethodScope.APPLICATION;
        } else if (project.isLibraryType(method.declaringClassFile().thisType())) {
            scope = MethodScope.DEPENDENCY;
        } else {
            scope = MethodScope.PRIMORDIAL;
        }
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
