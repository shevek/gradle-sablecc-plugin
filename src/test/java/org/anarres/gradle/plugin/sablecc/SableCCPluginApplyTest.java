package org.anarres.gradle.plugin.sablecc;

import java.util.Collections;
import org.anarres.gradle.plugin.velocity.VelocityTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.AbstractTask;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author shevek
 */
public class SableCCPluginApplyTest {

    Project project;

    @Before
    public void setUp() {
        project = ProjectBuilder.builder().build();
    }

    @Test
    public void testApply() {
        project.apply(Collections.singletonMap("plugin", "java"));
        project.apply(Collections.singletonMap("plugin", "org.anarres.sablecc"));
        assertTrue("Project is missing plugin", project.getPlugins().hasPlugin(PureJavaSableCCPlugin.class));
        {
            Task task = project.getTasks().findByName("sableccGrammar");
            assertNotNull("Project is missing sableccGrammar task", task);
            assertTrue("SableCC grammar task is the wrong type", task instanceof VelocityTask);
            assertTrue("SableCC grammar task should be enabled", ((AbstractTask) task).isEnabled());
        }
        {
            Task task = project.getTasks().findByName("sableccParser");
            assertNotNull("Project is missing sableccParser task", task);
            assertTrue("SableCC parser task is the wrong type", task instanceof SableCC);
            assertTrue("SableCC parser task should be enabled", ((AbstractTask) task).isEnabled());
        }
    }
}
