package com.googlecode.jcompilo;

import com.googlecode.totallylazy.Characters;
import com.googlecode.totallylazy.Mapper;
import com.googlecode.totallylazy.Option;
import com.googlecode.totallylazy.Sequence;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import static com.googlecode.jcompilo.Resource.constructors.resource;
import static com.googlecode.totallylazy.Bytes.bytes;
import static com.googlecode.totallylazy.Files.name;
import static com.googlecode.totallylazy.LazyException.lazyException;
import static com.googlecode.totallylazy.Predicates.not;
import static com.googlecode.totallylazy.Predicates.where;
import static com.googlecode.totallylazy.Sequences.sequence;
import static com.googlecode.totallylazy.Strings.endsWith;
import static javax.tools.StandardLocation.CLASS_PATH;

public class CompilerResources implements Resources {
    private final StandardJavaFileManager fileManager;

    public CompilerResources(final JavaCompiler compiler, final Iterable<File> dependancies) {
        fileManager = compiler.getStandardFileManager(null, null, Characters.UTF8);
        setDependencies(sequence(dependancies).filter(where(name(), not(endsWith("-sources.jar")))));
    }

    public JavaFileManager output(final Outputs outputs) throws FileNotFoundException {
        return new OutputsManager(fileManager, outputs);
    }

    private void setDependencies(Sequence<File> dependancies) {
        try {
            if (dependancies.isEmpty()) return;
            fileManager.setLocation(CLASS_PATH, dependancies);
        } catch (IOException e) {
            throw lazyException(e);
        }
    }

    @Override
    public Option<Resource> get(final String name) {
        return sequence(fileManager.getJavaFileObjects(name)).headOption().map(new Mapper<JavaFileObject, Resource>() {
            @Override
            public Resource call(final JavaFileObject fileObject) throws Exception {
                return resource(fileObject.getName(), new Date(fileObject.getLastModified()), bytes(fileObject.openInputStream()));
            }
        });
    }
}