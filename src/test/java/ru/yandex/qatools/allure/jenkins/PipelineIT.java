package ru.yandex.qatools.allure.jenkins;

import hudson.FilePath;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.FlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import ru.yandex.qatools.allure.jenkins.testdata.TestUtils;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
public class PipelineIT {

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @ClassRule
    public static JenkinsRule jRule = new JenkinsRule();

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    public static String commandline;

    public static String jdk;

    @BeforeClass
    public static void setUp() throws Exception {
        jdk = TestUtils.getJdk(jRule).getName();
        commandline = TestUtils.getAllureCommandline(jRule, folder).getName();
    }

    @Test
    public void shouldSupportPipeline() throws Exception {
        WorkflowJob project = jRule.createProject(WorkflowJob.class);
        prepareWorkspace(project);

        FlowDefinition definition = new CpsFlowDefinition("node { allure(results: [[path: paths]]) }", true);
        project.setDefinition(definition);
        project.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition("paths", "allure-results")
        ));
        WorkflowRun build = jRule.buildAndAssertSuccess(project);

        assertThat(build.getActions(AllureReportBuildAction.class)).hasSize(1);
    }

    private void prepareWorkspace(WorkflowJob project) throws IOException, InterruptedException {
        FilePath workspace = jRule.jenkins.getWorkspaceFor(project);
        String testSuiteFileName = "sample-testsuite.xml";
        FilePath allureReportsDir = workspace.child("allure-results").child(testSuiteFileName);
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(testSuiteFileName)) {
            allureReportsDir.copyFrom(is);
        }
    }
}
