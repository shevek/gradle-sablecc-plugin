package org.anarres.gradle.plugin.sablecc;

import groovy.lang.Closure;
import java.io.File;
import java.io.IOException;
import org.anarres.gradle.plugin.velocity.VelocityTask;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.EmptyFileVisitor;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

/**
 *
 * @author shevek
 */
public class PureJavaSableCCPlugin implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        final SableCCPluginExtension extension = project.getExtensions().create("sablecc", SableCCPluginExtension.class);
        final Configuration configuration = project.getConfigurations().create("sablecc");

        project.getDependencies().add(configuration.getName(), "sablecc:sablecc:3.2-1");

        final Task sableccGrammarTask = project.getTasks().create("sableccGrammar", VelocityTask.class, new Action<VelocityTask>() {

            @Override
            public void execute(VelocityTask task) {
                task.inputDir = project.file(extension.inputDir);
                task.outputDir = project.file(extension.intermediateDir);
                task.includeDir = project.file(extension.inputDir);
                task.filter = "**/*.sablecc";
            }
        });

        final Task sableccParserTask = project.getTasks().create("sableccParser", new Action<Task>() {
            @Override
            public void execute(Task task) {
                task.dependsOn(sableccGrammarTask);
                task.setDescription("Preprocesses SableCC grammar files.");
                task.getInputs().dir(extension.intermediateDir);
                task.getOutputs().dir(extension.outputDir);
                task.doLast(new Action<Task>() {

                    @Override
                    public void execute(Task task) {
                        // println "Reading from $inputDir"
                        final File outputDir = project.file(extension.outputDir);
                        // println "Writing to $outputDir"
                        DefaultGroovyMethods.deleteDir(outputDir);
                        outputDir.mkdirs();

                        ConfigurableFileTree inputFiles = project.fileTree(extension.intermediateDir);
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

                        ConfigurableFileTree outputFiles = project.fileTree(outputDir);
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
                });
            }
        });

        project.getTasks().getByName("compileJava").dependsOn(sableccParserTask);
        SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
        final SourceSet mainSourceSet = sourceSets.getByName("main");
        mainSourceSet.getJava().srcDir(extension.outputDir);

        Task sableccResourcesTask = project.getTasks().create("sableccResources", Copy.class, new Action<Copy>() {

            @Override
            public void execute(Copy task) {
                task.setDescription("Copies SableCC resource files.");
                task.into(mainSourceSet.getOutput().getResourcesDir());
                task.from(extension.outputDir, new Closure<Void>(PureJavaSableCCPlugin.this) {

                    public Void doCall(CopySpec spec) {
                        spec.include("**/*.dat");
                        return null;
                    }
                });
            }
        });
        project.getTasks().getByName("classes").dependsOn(sableccResourcesTask);

    }

}
