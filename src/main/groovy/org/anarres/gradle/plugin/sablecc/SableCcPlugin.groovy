package org.anarres.gradle.plugin.sablecc

import java.util.regex.Matcher
import java.util.regex.Pattern
import org.gradle.api.Plugin
import org.gradle.api.Project

class SableCcPluginExtension {
	String inputDir = "src/main/sablecc"
	String intermediateDir = "build/generated-sources/grammar"
	String outputDir = "build/generated-sources/sablecc"
}

class SableCcPlugin implements Plugin<Project> {
	void apply(Project project) {

		project.extensions.create("sablecc", SableCcPluginExtension)

		project.configurations.create('sablecc')
		project.dependencies.sablecc "sablecc:sablecc:3.2-1"

		project.task('sableccGrammar', type: VelocityTask) {
			inputDir = project.file(project.sablecc.inputDir)
			outputDir = project.file(project.sablecc.intermediateDir)
			includeDir = project.file(project.sablecc.inputDir)
			filter = "**/*.sablecc"
		}

		project.task('sableccParser', dependsOn: project.sableccGrammar) {
			description "Preprocesses SableCC grammar files."
			inputs.dir project.sablecc.intermediateDir
			outputs.dir project.sablecc.outputDir
			doLast {
				File inputDir = project.file(project.sablecc.intermediateDir)
				// println "Reading from $inputDir"
				File outputDir = project.file(project.sablecc.outputDir)
				// println "Writing to $outputDir"
				outputDir.deleteDir()
				outputDir.mkdirs()

				ant.taskdef(
					name: 'sablecc',
					classname: 'org.sablecc.ant.taskdef.Sablecc',
					classpath: project.configurations.sablecc.asPath
				)
				ant.sablecc(
					src: inputDir,
					outputdirectory: outputDir,
					includes: '**/*.sablecc'
				)

				def outputFiles = project.fileTree(dir: outputDir)
				outputFiles.visit { e ->
					if (e.file.isFile()) {
						// println "Processing " + e
						SableCCPostProcessor.processFile(e.file)
					}

				}
			}
		}
		project.compileJava.dependsOn(project.sableccParser)
		project.sourceSets.main.java.srcDir project.sablecc.outputDir

		project.task('sableccResources', type: org.gradle.api.tasks.Copy) {
			description "Copies SableCC resource files."
			// inputs.files project.fileTree(dir: project.sablecc.outputDir, include: '**/*.dat')
			// outputs.dir project.sourceSets.main.output.resourcesDir
			into project.sourceSets.main.output.resourcesDir
			from(project.sablecc.outputDir) {
				include '**/*.dat'
			}
		}
		project.classes.dependsOn(project.sableccResources)

	}
}
