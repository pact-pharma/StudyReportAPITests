package com.testrail.listeners;

import java.util.Arrays;
import com.testrail.util.UseAsTestRailId;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;


/**
 * TestNgTestRailListener for Tracking the Test Results and Updating the Test Rail.
 * Implementation supports both normal tests and Data Provider test.
 *
 */
public class TestNgTestRailListener implements IInvokedMethodListener {

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult){
            UseAsTestRailId useAsTestRailId =
                    method.getTestMethod().getConstructorOrMethod().getMethod().getAnnotation(UseAsTestRailId.class);
            //Data driven tests need to be handled differently
            if (method.getTestMethod().isDataDriven()) {
                // Get the Parameters from the Result
                Object[] parameters = testResult.getParameters();
                // Post the result to Test Rail
                new PostResults().postTestRailResult(
                        useAsTestRailId.testRailId(), testResult, Arrays.toString(parameters));
            } else {
                new PostResults().postTestRailResult(useAsTestRailId.testRailId(), testResult);
            }
    }

}