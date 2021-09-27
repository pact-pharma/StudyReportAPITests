package com.testrail.listeners;

import com.testrail.client.APIClient;
import com.testrail.client.TestRailStatusID;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.testng.ITestResult;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import static com.testrail.listeners.TestRailVariables.*;
import static com.testrail.util.TestRailConstants.*;

public class PostResults {
    private static String testRun = null;

    /**
     * This method posts Test Rail Results.
     *
     * @param testRailId - Test Rail test ID
     * @param result - test result
     */
    public void postTestRailResult(int testRailId, ITestResult result) {
        System.out.println(String.format("Test case Id %s with Status::%s", testRailId, TestRailStatusID.getResult(result.getStatus())));
        post(testRailId, getTestRailMetaData(result));
    }

    /**
     * This method posts Test Rail Results with the Parameters
     *
     * @param testRailId - - Test Rail test ID
     * @param result - test result
     * @param parameters - test parameter
     */
    public void postTestRailResult(int testRailId, ITestResult result, String parameters) {
        System.out.println(String.format("TestId %s with parameter %s with Status::%s", testRailId, parameters,
                TestRailStatusID.getResult(result.getStatus())));
        post(testRailId, getTestRailMetaData(result, parameters));
    }

    /**
     * This methd creates a Map object which contains the MetaData for the Test which needs to be updated in Test Rail.
     * This is with the Parameters.
     *
     * @param result
     * @return
     */
    private Map getTestRailMetaData(ITestResult result, String parameters) {
        //Set the status_id for the test Rail for Pass and Fail.
        Map dataTestRail = new HashMap();
        if (result.getStatus() == ITestResult.SUCCESS) {
            dataTestRail.put(STATUS_ID, TestRailStatusID.PASS);
            dataTestRail.put(COMMENT, String.format("SUCCESS with parameters :%s",parameters));
        } else if (result.getStatus() == ITestResult.FAILURE) {
            dataTestRail.put(STATUS_ID, TestRailStatusID.FAIL);
            dataTestRail.put(COMMENT, String.format("FAILURE with parameters :%s %s", parameters, result.getThrowable()));
        }
        return dataTestRail;
    }

    /**
     * This method creates a map object which contains the MetaData for the Test which needs to be updated in Test Rail.
     *
     * @param result
     * @return
     */
    private Map getTestRailMetaData(ITestResult result) {
        //Set the status_id for the test Rail for Pass, Fail and Skip status.
        Map dataTestRail = new HashMap();
        if (result.getStatus() == ITestResult.SUCCESS) {
            dataTestRail.put(STATUS_ID, TestRailStatusID.PASS);
            dataTestRail.put(COMMENT, "SUCCESS");
        } else if (result.getStatus() == ITestResult.FAILURE) {
            dataTestRail.put(STATUS_ID, TestRailStatusID.FAIL);
            dataTestRail.put(COMMENT, String.format("FAILURE %s", result.getThrowable()));
        }
        return dataTestRail;
    }

    /**
     * This method calls TestRail Client and Posts Request.
     *
     * @param testRailId
     * @param data
     */
    private void post(int testRailId, Map data) {
        try{
           if(testRun == null) {
               readRunId(getTestRailAPIClient());
            }
            getTestRailAPIClient().sendPost(String.format("add_result_for_case/%s/%s", testRun, testRailId), data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method reads run ID
     * @throws Exception
     */
    private void readRunId(APIClient client) throws Exception {
        // JSONArray projects = (JSONArray) getTestRailAPIClient().sendGet("get_projects");
        JSONObject res = (JSONObject) getTestRailAPIClient().sendGet("get_projects");
        JSONArray projects = (JSONArray) res.get("projects");
        for(Object object: projects) {
            String projectName = (String) ((JSONObject) object).get(NAME);
            if(projectName.trim().equalsIgnoreCase(STUDY_REPORT_API)) {
                String projectId = String.valueOf(((JSONObject)object).get(ID));
                JSONArray suites = (JSONArray) client.sendGet(String.format("get_suites/%s", projectId));
                if(suites.size() > 0) {
                    JSONObject suite = (JSONObject) suites.get(0);
                    String suiteId = String.valueOf(suite.get(ID));
                    Map<String, Serializable> data = new HashMap<>();
                    data.put(SUITE_ID, suiteId);
                    data.put(INCLUDE_ALL, true);
                    JSONObject testRun = (JSONObject) client.sendPost(String.format("add_run/%s", projectId), data);
                    String testRunId = String.valueOf(testRun.get(ID));
                    setTestRun(testRunId);
                }
                break;
            }
        }
    }

    /**
     * This method sets test run.
     * @param testRunToSet
     */
    public static void setTestRun(String testRunToSet) {
        testRun = testRunToSet;
    }

    /**
     * Get the Test Rail Api Client.
     *
     * @return
     */
    private APIClient getTestRailAPIClient() {
        APIClient client = new APIClient(BASE_URL);
        client.setUser(USER);
        client.setPassword(TEST_RAIL_PASSWORD);
        return client;
    }
}
