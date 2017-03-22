package org.anarres.gradle.plugin.sablecc;

import groovy.lang.Closure;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.anarres.gradle.plugin.velocity.VelocityTask;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.CopySpec;
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
        final SableCCPluginExtension extension = project.getExtensions().create("sablecc", SableCCPluginExtension.class, project);
        final Configuration configuration = project.getConfigurations().create("sablecc");

        project.getDependencies().add(configuration.getName(), "sablecc:sablecc:3.2-1");

        final Task sableccGrammarTask = project.getTasks().create("sableccGrammar", VelocityTask.class, new Action<VelocityTask>() {

            @Override
            public void execute(VelocityTask task) {

                task.conventionMapping("inputDir", new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        return project.file(extension.inputDir);
                    }
                });

                task.conventionMapping("includeDirs", new Callable<List<File>>() {
                    @Override
                    public List<File> call() {
                        List<Object> includeDirs = extension.includeDirs;
                        if (includeDirs == null)
                            return null;
                        List<File> out = new ArrayList<File>();
                        for (Object includeDir : includeDirs)
                            out.add(project.file(includeDir));
                        return out;
                    }
                });

                task.conventionMapping("outputDir", new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        return project.file(extension.intermediateDir);
                    }
                });

                task.setFilter("**/*.sablecc");
            }
        });

        final Task sableccParserTask = project.getTasks().create("sableccParser", SableCC.class, new Action<SableCC>() {
            @Override
            public void execute(SableCC task) {
                task.dependsOn(sableccGrammarTask);
                task.setDescription("Preprocesses SableCC grammar files.");

                task.conventionMapping("inputDir", new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        return project.file(extension.intermediateDir);
                    }
                });

                task.conventionMapping("outputDir", new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        return project.file(extension.outputDir);
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
