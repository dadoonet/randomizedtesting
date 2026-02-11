package com.carrotsearch.ant.tasks.junit5.it;

import org.junit.jupiter.api.Test;

public class TestRunListeners extends JUnit5XmlTestBase {

    @Test
    public void singleUserDefinedRunListener() {
        super.executeTarget("singleUserDefinedRunListener");

        assertLogContains("UserDefinedRunListener1.executionStarted()");
        assertLogContains("UserDefinedRunListener1.executionFinished()");
    }

    @Test
    public void multipleUserDefinedRunListeners() {
        super.executeTarget("multipleUserDefinedRunListeners");

        assertLogContains("UserDefinedRunListener2.executionStarted()");
        assertLogContains("UserDefinedRunListener2.executionFinished()");

        assertLogContains("UserDefinedRunListener3.executionStarted()");
        assertLogContains("UserDefinedRunListener3.executionFinished()");
    }

}
