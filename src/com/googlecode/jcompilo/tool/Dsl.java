package com.googlecode.jcompilo.tool;

import com.googlecode.jcompilo.ResourceHandler;
import com.googlecode.totallylazy.UnaryFunction;

import static com.googlecode.jcompilo.Compiler.typeFor;
import static com.googlecode.jcompilo.asm.AsmResourceHandler.asmResourceHandler;
import static com.googlecode.jcompilo.tco.TailRecHandler.tailRecHandler;
import static com.googlecode.totallylazy.Sequences.sequence;
import static com.googlecode.totallylazy.callables.Compose.compose;
import static com.googlecode.totallylazy.collections.PersistentList.constructors.empty;

public class Dsl {
    @SafeVarargs
    public static JCompiler compiler(UnaryFunction<JCompiler>... builders) {
        return sequence(builders).
                reduce(compose(JCompiler.class)).
                apply(new JCompiler(JCompiler.DEFAULT_COMPILER, empty(ResourceHandler.class)));
    }

    public static UnaryFunction<JCompiler> tailrec() {
        return tailrec("com.googlecode.totallylazy.annotations.tailrec");
    }

    public static UnaryFunction<JCompiler> tailrec(final String className) {
        return new UnaryFunction<JCompiler>() {
            @Override
            public JCompiler call(JCompiler compiler) throws Exception {
                return new JCompiler(compiler.compiler(),
                        compiler.resourceHandlers().append(asmResourceHandler().add(typeFor(className), tailRecHandler())));
            }
        };
    }

}
