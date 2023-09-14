package hudson.plugins.mstest;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.TestResultProjectAction;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;

/**
 * Unit tests for MSTestPublisher class
 *
 * @author Antonio Marques
 */
public class MSTestPublisherTest extends TestHelper {

    private Mockery classContext;
    private AbstractProject project;
    private TaskListener buildListener;
    private Run<?, ?> run;

    @Before
    public void setUp() throws Exception {
        createWorkspace();
        classContext = getClassMock();
        project = classContext.mock(AbstractProject.class);
        buildListener = classContext.mock(TaskListener.class);
        run = classContext.mock(Run.class);
    }

    @After
    public void tearDown() throws Exception {
        deleteWorkspace();
    }

    private MSTestPublisher getTestedPublisher(String pattern) {
        MSTestPublisher publisher = new MSTestPublisher();
        publisher.setTestResultsFile(pattern);
        publisher.setFailOnError(false);
        publisher.setKeepLongStdio(false);
        return publisher;
    }

    @Test
    public void testGetProjectActionProjectReusing() {
        classContext.checking(new Expectations() {
            {
                oneOf(project).getAction(with(equal(TestResultProjectAction.class)));
                will(returnValue(new TestResultProjectAction(project)));
            }
        });
        MSTestPublisher publisher = getTestedPublisher("build.trx");
        Action projectAction = publisher.getProjectAction(project);
        Assert.assertNull("The action was not null", projectAction);
    }

    @Test
    public void testGetProjectActionProject() {
        classContext.checking(new Expectations() {
            {
                oneOf(project).getAction(with(equal(TestResultProjectAction.class)));
                will(returnValue(null));
            }
        });
        MSTestPublisher publisher = getTestedPublisher("build.trx");
        Action projectAction = publisher.getProjectAction(project);
        Assert.assertNotNull("The action was null", projectAction);
        Assert.assertEquals("The action type is incorrect", TestResultProjectAction.class,
            projectAction.getClass());
    }

    @Test
    public void testNoFileMatchingPattern() throws Exception {
        classContext.checking(new Expectations() {
            {
                oneOf(run).getEnvironment(with(equal(buildListener)));
                will(returnValue(new EnvVars()));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(buildListener).getLogger();
                will(returnValue(new PrintStream(new ByteArrayOutputStream())));
                oneOf(buildListener).fatalError(with(any(String.class)));
            }
        });
        File subfolder = new File(parentFile, "subfolder");
        assert subfolder.exists() || subfolder.mkdirs();
        File testFile = new File(subfolder, "xmlentities-forged.trx");
        assert !testFile.exists() || testFile.delete();
        InputStream testStream = this.getClass()
            .getResourceAsStream("JENKINS-23531-xmlentities-forged.trx");
        FileCopyUtils.copy(testStream, new FileOutputStream(testFile));
        String[] results = MSTestPublisher
            .resolveTestReports("*.trx", run, workspace, buildListener, Level.INFO);
        Assert.assertEquals(0, results.length);
    }

    @Ignore
    @Test
    public void testComplete() throws Exception {
        classContext.checking(new Expectations() {
            {
                ignoring(run).getEnvironment(with(equal(buildListener)));
                will(returnValue(new EnvVars()));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(run).getTimestamp();
                GregorianCalendar c = new GregorianCalendar();
                c.setTimeInMillis(0L);
                will(returnValue(c));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(buildListener).getLogger();
                will(returnValue(new PrintStream(new ByteArrayOutputStream())));
                oneOf(buildListener).fatalError(with(any(String.class)));
            }
        });
        classContext.checking(new Expectations() {
            {
                oneOf(run).getAction(with(equal(TestResultAction.class)));
                will(returnValue(null));
            }
        });
        classContext.checking(new Expectations() {
            {
                oneOf(run).getPreviousBuild();
                will(returnValue(null));
            }
        });
        classContext.checking(new Expectations() {
            {
                oneOf(run).getNumber();
                will(returnValue(1));
            }
        });
        classContext.checking(new Expectations() {
            {
                new TestResultAction(with(run), with(new TestResult()), with(buildListener));
                will(returnValue(new TestResultAction(run, null, buildListener)));
            }
        });

        File subfolder = new File(parentFile, "subfolder");
        assert subfolder.exists() || subfolder.mkdirs();
        File testFile = new File(subfolder, "xmlentities-forged.trx");
        assert !testFile.exists() || testFile.delete();
        InputStream testStream = this.getClass()
            .getResourceAsStream("JENKINS-23531-xmlentities-forged.trx");
        FileCopyUtils.copy(testStream, new FileOutputStream(testFile));
        MSTestPublisher publisher = getTestedPublisher("**/*.trx");
        Launcher launcher = classContext.mock(Launcher.class);
        publisher.perform(run, workspace, launcher, buildListener);
    }
}
