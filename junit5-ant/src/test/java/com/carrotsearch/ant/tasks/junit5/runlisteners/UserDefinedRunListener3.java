package com.carrotsearch.ant.tasks.junit5.runlisteners;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

public class UserDefinedRunListener3 implements TestExecutionListener {

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        System.out.println("UserDefinedRunListener3.testPlanExecutionStarted()");
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        System.out.println("UserDefinedRunListener3.testPlanExecutionFinished()");
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            System.out.println("UserDefinedRunListener3.executionStarted()");
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
        if (testIdentifier.isTest()) {
            System.out.println("UserDefinedRunListener3.executionFinished()");
            if (result.getStatus() == TestExecutionResult.Status.FAILED) {
                System.out.println("UserDefinedRunListener3.testFailure()");
            } else if (result.getStatus() == TestExecutionResult.Status.ABORTED) {
                System.out.println("UserDefinedRunListener3.testAssumptionFailure()");
            }
        }
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        System.out.println("UserDefinedRunListener3.executionSkipped()");
    }

}
