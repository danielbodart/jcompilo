package com.googlecode.compilo.junit;

import com.googlecode.compilo.Processor;
import com.googlecode.totallylazy.Destination;
import com.googlecode.totallylazy.Function1;
import com.googlecode.totallylazy.Methods;
import com.googlecode.totallylazy.Predicate;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.Source;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import static com.googlecode.compilo.ClassesSource.classesSource;
import static com.googlecode.totallylazy.Files.file;
import static com.googlecode.totallylazy.Files.temporaryDirectory;
import static com.googlecode.totallylazy.Sequences.sequence;
import static com.googlecode.totallylazy.Strings.endsWith;
import static com.googlecode.totallylazy.ZipDestination.zipDestination;
import static java.lang.String.format;

public class Tests implements Processor {
    public static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors();
    private final List<String> tests = new ArrayList<String>();
    private final Predicate<? super String> predicate;

    private Tests(Predicate<? super String> predicate) {
        this.predicate = predicate;
    }

    public static Tests tests() {
        return testProcessor(endsWith("Test.java"));
    }

    public static Tests testProcessor(Predicate<? super String> predicate) {
        return new Tests(predicate);
    }

    @Override
    public String call(Source source, Destination destination) throws Exception {
        source.sources().size();
        return "";
    }

    @Override
    public boolean matches(String other) {
        boolean matched = predicate.matches(other);
        if (matched) tests.add(other);
        return matched;
    }

    public String execute(Sequence<File> dependencies) throws Exception {
        return execute(dependencies, DEFAULT_THREADS);
    }

    public String execute(Sequence<File> dependencies, int numberOfThreads) throws MalformedURLException, FileNotFoundException, ClassNotFoundException {
        final URLClassLoader classLoader = new URLClassLoader(asUrls(dependencies.cons(testExecutor())), null);

        Class<?> executor = classLoader.loadClass(TestExecutor.class.getName());
        Method execute = Methods.method(executor, "execute", List.class, int.class).get();
        Boolean result = Methods.<TestExecutor, Boolean>invoke(execute, null, tests, numberOfThreads);
        return format("    [junit] Running %s tests", tests.size());
    }

    private File testExecutor() throws FileNotFoundException {
        File testExecutor = file(temporaryDirectory(), "compilo.test.hook.jar");
        Destination destination = zipDestination(new FileOutputStream(testExecutor));

        Source source = classesSource(TestExecutor.class, TestExecutor.ResultCallable.class, TestExecutor.NullOutputStream.class, TestExecutor.NullPrintStream.class);
        Source.methods.copyAndClose(source, destination);
        return testExecutor;
    }

    private static URL[] asUrls(Sequence<File> jars) throws MalformedURLException {
        return jars.map(new Function1<File, URL>() {
            @Override
            public URL call(File file) throws Exception {
                return file.toURI().toURL();
            }
        }).toArray(URL.class);
    }
}