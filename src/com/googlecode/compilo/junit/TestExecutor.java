package com.googlecode.compilo.junit;

import com.googlecode.totallylazy.Sequence;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.googlecode.totallylazy.Lists.list;
import static com.googlecode.totallylazy.Sequences.sequence;

public class TestExecutor {
    public static void main(String[] arguments) {
        try {
            Sequence<String> values = sequence(arguments);
            boolean success = execute(values.tail().toList(), Integer.valueOf(values.head()), System.out);
            System.exit(success ? 0 : -1);
        } catch (Exception e) {
            System.exit(-1);
        }
    }

    public static boolean execute(final List<String> testNames, final int numberOfThreads, PrintStream out) throws Exception {
        System.setOut(nullPrintStream());
        System.setErr(nullPrintStream());

        Result result = execute(asClasses(testNames), numberOfThreads);

        boolean success = result.wasSuccessful();
        if (!success) {
            out.printf("%s tests failed:%n%n", result.getFailureCount());
            for (Failure failure : result.getFailures()) {
                Description description = failure.getDescription();
                Throwable throwable = failure.getException();
                out.printf("%s.%s %s%n", description.getTestClass().getSimpleName(), description.getMethodName(), throwable.getClass().getName());
            }
        }

        return success;
    }

    public static Result execute(Class<?>[] classes, int numberOfThreads) throws InterruptedException, ClassNotFoundException {
        Result result = new Result();

        if(numberOfThreads == 1 ) {
            junit(result).run(classes);
        } else {
            execute(numberOfThreads, tests(result, classes));
        }

        return result;
    }

    private static JUnitCore junit(Result result) {
        JUnitCore junit = new JUnitCore();
        junit.addListener(result.createListener());
        return junit;
    }

    private static List<Callable<Result>> tests(Result result, final Class<?>[] classes) throws ClassNotFoundException {
        List<Callable<Result>> tests = new ArrayList<Callable<Result>>();
        for (Class<?> testClass : classes) {
            tests.add(new ResultCallable(result, testClass));
        }
        return tests;
    }

    @SuppressWarnings("unchecked")
    private static void execute(int numberOfThreads, final List<? extends Callable<?>> tests) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        executorService.invokeAll((Collection<? extends Callable<Object>>) tests);
        executorService.shutdownNow();
    }

    private static PrintStream nullPrintStream() {
        return new NullPrintStream();
    }

    public static Class<?>[] asClasses(List<String> fileNames) throws ClassNotFoundException {
        Class<?>[] result = new Class<?>[fileNames.size()];
        for (int i = 0; i < fileNames.size(); i++) {
            result[i] = Class.forName(className(fileNames.get(i)));
        }
        return result;
    }

    public static String className(String filename) {
        return filename.replace('/', '.').replace(".java", "");
    }

    static class ResultCallable implements Callable<Result> {
        private final Result result;
        private final Class<?> testClass;

        public ResultCallable(Result result, Class<?> testClass) {
            this.result = result;
            this.testClass = testClass;
        }

        @Override
        public Result call() {
            return junit(result).run(testClass);
        }
    }

    static class NullPrintStream extends PrintStream {
        public NullPrintStream() {
            super(new NullOutputStream());
        }
    }

    static class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
        }
    }
}
