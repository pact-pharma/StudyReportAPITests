package com.testrail.listeners;

import java.util.Arrays;
import com.testrail.util.UseAsTestRailId;
import com.testrail.util.UseAsTestRailEnabled;
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
        UseAsTestRailEnabled useAsTestRailEnabled =
                method.getTestMethod().getConstructorOrMethod().getMethod().getAnnotation(UseAsTestRailEnabled.class);

               if (method.getTestMethod().isDataDriven()) {
                   if (useAsTestRailEnabled != null && useAsTestRailEnabled.isTestRailEnabled()) {
                       Object[] parameters = testResult.getParameters();
                       new PostResults().postTestRailResult(
                               (int) parameters[0], testResult, Arrays.toString(parameters));
                   }
               } else {
                   if(useAsTestRailId != null) {
                       new PostResults().postTestRailResult(useAsTestRailId.testRailId(), testResult);
                   }

           }
    }

}