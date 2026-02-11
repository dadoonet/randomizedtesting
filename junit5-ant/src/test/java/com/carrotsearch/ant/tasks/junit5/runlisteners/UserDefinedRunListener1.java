package com.carrotsearch.ant.tasks.junit5.runlisteners;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

public class UserDefinedRunListener1 implements TestExecutionListener {

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        System.out.println("UserDefinedRunListener1.testPlanExecutionStarted()");
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        System.out.println("UserDefinedRunListener1.testPlanExecutionFinished()");
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            System.out.println("UserDefinedRunListener1.executionStarted()");
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
        if (testIdentifier.isTest()) {
            System.out.println("UserDefinedRunListener1.executionFinished()");
            if (result.getStatus() == TestExecutionResult.Status.FAILED) {
                System.out.println("UserDefinedRunListener1.testFailure()");
            } else if (result.getStatus() == TestExecutionResult.Status.ABORTED) {
                System.out.println("UserDefinedRunListener1.testAssumptionFailure()");
            }
        }
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        System.out.println("UserDefinedRunListener1.executionSkipped()");
    }

}
