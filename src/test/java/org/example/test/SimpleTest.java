package org.example.test; 

import org.apache.commons.io.FileUtils;
import java.io.File;

import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;
import hudson.model.Result;
import org.junit.runners.model.Statement;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.jenkinsci.plugins.workflow.actions.LogAction;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.cps.global.WorkflowLibRepository;
import org.junit.Test;
import org.junit.Rule;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import org.junit.Before;
import java.net.URL;

public class SimpleTest {
    @Rule public RestartableJenkinsRule story = new RestartableJenkinsRule();
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Inject
    Jenkins jenkins;

    @Inject
    WorkflowLibRepository repo;

    @Test public void smokes() throws Exception {
        story.addStep(new Statement() {
           @Override public void evaluate() throws Throwable {
               String path = SimpleTest.class.getResource("/") + "../../src";
               path = path.replace("file:", "");
               File repodir = new File(repo.workspace+"/src");
               repodir.mkdirs();
               FileUtils.copyDirectory(new File(path), repodir);
               WorkflowJob job = jenkins.createProject(WorkflowJob.class, "job");
               job.setDefinition(new CpsFlowDefinition("import org.example.test.Foo; new Foo().hello()"));
               // Start the job
               WorkflowRun build = job.scheduleBuild2(0).getStartCondition().get();
               CpsFlowExecution execution = (CpsFlowExecution) build.getExecutionPromise().get();
               execution.waitForSuspension();

               System.out.println(JenkinsRule.getLog(build));

               List<LogAction> logActions = new ArrayList<LogAction>();
               for (FlowNode n : new FlowGraphWalker(build.getExecution())) {
                   LogAction la = n.getAction(LogAction.class);
                   if (la != null) {
                       logActions.add(la);
                   }
               }
               assertEquals(1, logActions.size());
               StringWriter w = new StringWriter();
               logActions.get(0).getLogText().writeLogTo(0, w);
               assertEquals("answer is 42", w.toString().trim());
               Matcher m = Pattern.compile("answer is 42").matcher(JenkinsRule.getLog(build));
               assertTrue("message printed once", m.find());
               assertFalse("message not printed twice", m.find());
           }
        }
      );
    }
}
