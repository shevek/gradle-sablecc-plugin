package org.anarres.gradle.plugin.sablecc;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
@RunWith(Parameterized.class)
public class SableCCPluginApplyTest {

    private static final Logger LOG = LoggerFactory.getLogger(SableCCPluginApplyTest.class);

    @Nonnull
    private static Object[] A(Object... in) {
        return in;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters() throws Exception {
        return Arrays.asList(
                A("2.12"),
                A("2.14"),
                A("3.0"),
                A("3.2.1"),
                A("3.4.1"),
                A("4.10.3"),
                A("5.6")
        );
    }

    private final String gradleVersion;
    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();
    public File testProjectBuildFile;

    @Before
    public void setUp() throws Exception {
        testProjectBuildFile = testProjectDir.newFile("build.gradle");
    }

    public SableCCPluginApplyTest(String gradleVersion) {
        this.gradleVersion = gradleVersion;
    }

    @Test
    public void testApply() throws Exception {
        String text = "plugins {\nid 'java';\nid 'org.anarres.sablecc';\n }\n";
        Files.write(testProjectBuildFile.toPath(), Collections.singletonList(text));

        GradleRunner runner = GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withPluginClasspath()
                .withDebug(true)
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("--stacktrace", "tasks");
        LOG.info("Building...\n\n");
        // System.out.println("ClassPath is " + runner.getPluginClasspath());
        BuildResult result = runner.build();
        LOG.info("Output:\n\n" + result.getOutput() + "\n\n");
    }
}
