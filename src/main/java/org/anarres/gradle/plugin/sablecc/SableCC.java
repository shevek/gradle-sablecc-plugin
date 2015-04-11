/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.gradle.plugin.sablecc;

import java.io.File;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.EmptyFileVisitor;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/**
 *
 * @author shevek
 */
public class SableCC extends ConventionTask {

    private File inputDir;
    private File outputDir;

    @Nonnull
    @InputDirectory
    public File getInputDir() {
        return inputDir;
    }

    public void setInputDir(File inputDir) {
        this.inputDir = inputDir;
    }

    @Nonnull
    @OutputDirectory
    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    @TaskAction
    public void runSableCC() {
        // println "Reading from $inputDir"
        final File outputDir = getOutputDir();
        // println "Writing to $outputDir"
        DefaultGroovyMethods.deleteDir(outputDir);
        outputDir.mkdirs();

        ConfigurableFileTree inputFiles = getProject().fileTree(getInputDir());
        inputFiles.include("**/*.sablecc");
        inputFiles.visit(new EmptyFileVisitor() {
            @Override
            public void visitFile(FileVisitDetails fvd) {
                try {
                    org.sablecc.sablecc.SableCC.processGrammar(fvd.getFile().getAbsolutePath(), outputDir.getAbsolutePath());
                } catch (Exception e) {
                    throw new GradleException("Failed to process " + fvd, e);
                }
            }
        });

        ConfigurableFileTree outputFiles = getProject().fileTree(outputDir);
        outputFiles.visit(new EmptyFileVisitor() {
            @Override
            public void visitFile(FileVisitDetails fvd) {
                try {
                    SableCCPostProcessor.processFile(fvd.getFile());
                } catch (IOException e) {
                    throw new GradleException("Failed to post-process " + fvd, e);
                }
            }
        });

    }

}
