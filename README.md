# Introduction

This is a gradle plugin which creates parsers using
[SableCC](http://sablecc.org/), clearly the best parser-generator known
to man. Why is it the best? Because it supports automatic CST-to-AST
transformation, emits all the visitor patterns and analysis helpers
you will ever need, and is LR, not LL(k).

Why is it version 1.0.0? Because I've been using it in my own projects
for a long time now, it's basically debugged, and you should use it too.
However, no software survives contact with a customer, so by the time
you read this, it's probably version 1.7.15 or something.

# Usage

To apply a default configuration which transforms src/main/sablecc
into build/generated-sources/sablecc:

	buildscript {
		dependencies {
			classpath 'org.anarres.gradle:gradle-sablecc-plugin:[1.0.0,)'
		}
	}

	apply plugin: 'sablecc'

	sablecc {
		// ...
	}

# API Documentation

The [JavaDoc API](http://shevek.github.io/gradle-sablecc-plugin/docs/javadoc/)
is also available, but not very interesting.

