package com.googlecode.compilo.intellij;

import com.googlecode.compilo.CompileOption;
import com.googlecode.compilo.Inputs;
import com.googlecode.compilo.MemoryStore;
import com.googlecode.compilo.Outputs;
import com.googlecode.compilo.Resource;
import com.googlecode.totallylazy.FileDestination;
import com.googlecode.totallylazy.Function1;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.Sequences;
import com.googlecode.totallylazy.Sets;
import com.intellij.compiler.OutputParser;
import com.intellij.compiler.impl.CompilerUtil;
import com.intellij.compiler.impl.javaCompiler.BackendCompiler;
import com.intellij.compiler.impl.javaCompiler.ModuleChunk;
import com.intellij.compiler.impl.javaCompiler.api.CompilerAPIConfiguration;
import com.intellij.compiler.impl.javaCompiler.javac.JavacConfigurable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.googlecode.totallylazy.Sequences.sequence;

public class CompiloBackendCompiler implements BackendCompiler {
    private static final Set<FileType> JAVA = Sets.<FileType>set(StdFileTypes.JAVA);

    private final Project myProject;

    public CompiloBackendCompiler(Project project) {
      myProject = project;
    }

    @NotNull
    public String getId() {
        return "compilo";
    }

    @NotNull
    public String getPresentableName() {
        return "Compilo";
    }

    @NotNull
    public Configurable createConfigurable() {
        return new JavacConfigurable(CompilerAPIConfiguration.getSettings(myProject, CompilerAPIConfiguration.class));
    }

    @NotNull
    public Set<FileType> getCompilableFileTypes() {
        return JAVA;
    }

    public OutputParser createErrorParser(@NotNull String s, Process process) {
        return null;
    }

    public OutputParser createOutputParser(@NotNull String s) {
        return null;
    }

    public boolean checkCompiler(CompileScope compileScope) {
        return true;
    }

    @NotNull
    public Process launchProcess(@NotNull ModuleChunk moduleChunk, @NotNull String outputPath, @NotNull CompileContext compileContext) throws IOException {
        return new CompiloProcess(inputsFor(moduleChunk), outputs(outputPath), dependencies(moduleChunk), compileOptions(moduleChunk));
    }

    public void compileFinished() {
    }

    public static Outputs outputs(String outputPath) {
        return Outputs.constructors.output(FileDestination.fileDestination(new File(outputPath)));
    }

    public static Sequence<CompileOption> compileOptions(final ModuleChunk moduleChunk) {
        return ApplicationManager.getApplication().runReadAction(new Computable<Sequence<CompileOption>>() {
            public Sequence<CompileOption> compute() {
                List<String> commandLine = new ArrayList<String>();
                CompilerUtil.addSourceCommandLineSwitch(moduleChunk.getJdk(), moduleChunk.getLanguageLevel(), commandLine);
                return Sequences.sequence(commandLine).map(compileOption);
            }
        });
    }

    public static final Function1<String,CompileOption> compileOption = new Function1<String, CompileOption>() {
        @Override
        public CompileOption call(String s) throws Exception {
            return CompileOption.compileOption(s);
        }
    };

    public static Iterable<File> dependencies(ModuleChunk moduleChunk) {
        return Sequences.sequence(moduleChunk.getCompilationClasspathFiles()).map(new Function1<VirtualFile, File>() {
            @Override
            public File call(VirtualFile virtualFile) throws Exception {
                return new File(virtualFile.getPresentableUrl());
            }
        });
    }

    public static Inputs inputsFor(ModuleChunk moduleChunk) throws IOException {
        Map<String, Resource> files = new ConcurrentHashMap<String, Resource>();
        for (VirtualFile virtualFile : moduleChunk.getFilesToCompile()) {
            files.put(virtualFile.getPresentableUrl(), Resource.constructors.resource(virtualFile.getPresentableUrl(), new Date(), virtualFile.contentsToByteArray()));
        }
        return new MemoryStore(files);
    }
}
