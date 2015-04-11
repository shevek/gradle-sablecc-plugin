package org.anarres.gradle.plugin.sablecc;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author shevek
 */
public class SableCCPluginExtension {

    public Object inputDir = "src/main/sablecc";
    public List<Object> includeDirs = new ArrayList<Object>();
    public Object intermediateDir = "build/generated-sources/grammar";
    public Object outputDir = "build/generated-sources/sablecc";

}
