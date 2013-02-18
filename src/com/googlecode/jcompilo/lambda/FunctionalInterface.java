package com.googlecode.jcompilo.lambda;

import com.googlecode.jcompilo.asm.Asm;
import com.googlecode.totallylazy.Eq;
import com.googlecode.totallylazy.Mapper;
import com.googlecode.totallylazy.Sequence;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;

import java.lang.reflect.Field;

import static com.googlecode.jcompilo.asm.Asm.instructions;
import static com.googlecode.totallylazy.Predicates.is;
import static com.googlecode.totallylazy.Sequences.sequence;
import static com.googlecode.totallylazy.Unchecked.cast;

public class FunctionalInterface extends Eq {
    public final Type classType;
    public final Sequence<Type> argumentTypes;
    public final Type returnType;
    public final InsnList body;

    private FunctionalInterface(final Type classType, final Sequence<Type> argumentTypes, final Type returnType, final InsnList body) {
        this.classType = classType;
        this.argumentTypes = argumentTypes;
        this.returnType = returnType;
        this.body = body;
    }

    public static FunctionalInterface functionalInterface(final Type classType, final Sequence<Type> argumentTypes, final Type returnType, final InsnList body) {
        return new FunctionalInterface(classType, argumentTypes, returnType, body);
    }

    public static <T> Mapper<Field, T> value(final Object instance) {
        return new Mapper<Field, T>() {
            @Override
            public T call(final Field field) throws Exception {
                return cast(field.get(instance));
            }
        };
    }

    @Override
    public String toString() {
        return classType.getClassName() + "<" + argumentTypes.map(new Mapper<Type, String>() {
            @Override
            public String call(final Type type) throws Exception {
                return type.getClassName();
            }
        }).toString(",") + "," + returnType.getClassName() + ">\n" + Asm.toString(body);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(final FunctionalInterface functionalInterface) {
        return this.toString().equals(functionalInterface.toString());
    }
}