package com.pactpharma.sr;

import com.testautomationguru.utility.PDFUtil;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.Assert;
import org.skyscreamer.jsonassert.JSONAssert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static com.pactpharma.sr.TestConstants.*;

public class StudyReportTests {

    @DataProvider(name = "getFetchDocsDataProvider")
    public Object[][] getFetchDocsDataProvider(){
        return new Object[][] {{GET_FETCH_DOCS_URI, CREATOR_USER_NAME, CREATOR_PASSWORD,
                200, "25046", "20-227_20000820_0014_PACT298C_Protein Science(S).tar.gz",
                "https://study-report.zest.pactpharma.com/api/v1/report/reports/25046/fetchdocs/", null},
                {GET_FETCH_DOCS_URI, CREATOR_USER_NAME, CREATOR_PASSWORD,
                        400, "125046", null, null, "Error: No such report found!"},
                {GET_FETCH_DOCS_URI, APPROVAL_USER_NAME, APPROVAL_PASSWORD,
                        400, "25046", null, null, "User svc-study-report-approval@pactpharma.com does not have permission " +
                        "to download report of type Protein Science(S)"},
                {GET_FETCH_IN_WORD_FORMAT_URI, CREATOR_USER_NAME, CREATOR_PASSWORD,
                        200, "25046", "20-227_20000820_0014_PACT298C_Protein Science(S).word.tar.gz",
                        "https://study-report.zest.pactpharma.com/api/v1/report/reports/25046/fetchinwordformat/", null},
                {GET_FETCH_IN_WORD_FORMAT_URI, CREATOR_USER_NAME, CREATOR_PASSWORD ,
                        400, "125046", null, null, "Error: No such report found!"},
                {GET_FETCH_IN_WORD_FORMAT_URI, APPROVAL_USER_NAME, APPROVAL_PASSWORD ,
                        400, "25046", null, null, "User svc-study-report-approval@pactpharma.com does not have permission " +
                        "to download report of type Protein Science(S)"}
        };

    }
    @Test(dataProvider = "getFetchDocsDataProvider", enabled = false)
    void getFetchDocsTest(String fetchDocsUri, String userName, String userPassword, int expectedReturnCode, String studyReportId,
                    String expectedArchiveName, String expectedUriPrefix, String expectedErrorMessage) throws Exception {
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);
        Response response = httpRequest.request(Method.GET, String.format(fetchDocsUri, studyReportId));

        Assert.assertEquals(String.format("Response code should be %s", expectedReturnCode),
                                                         expectedReturnCode, response.getStatusCode());
        System.out.println("response" + response.asPrettyString());
        switch(expectedReturnCode) {
            case 200:
                Assert.assertEquals(String.format("Archive name should be %s", expectedArchiveName),
                        expectedArchiveName, response.jsonPath().get("archiveName"));
                Assert.assertTrue(String.format("URI string should starts with %s", expectedUriPrefix),
                        response.jsonPath().get(URI).toString().startsWith(expectedUriPrefix));
                break;
            case 400:
                System.out.println("Error Message:" + response.jsonPath().get("message"));
                Assert.assertEquals(String.format("Error message should be %s", expectedErrorMessage),
                        expectedErrorMessage, response.jsonPath().get(MESSAGE));
                break;
        }

        //String expectedResult = TestUtilities.readJsonFile("src/test/resources/reportForPatient0021");
        //JSONAssert.assertEquals(expectedResult, response.asPrettyString(), false);
    }

    @DataProvider(name = "getFetchDocsWithTokenDataProvider")
    public Object[][] getFetchDocsWithTokenDataProvider(){
        return new Object[][] {{CREATOR_USER_NAME, CREATOR_PASSWORD ,
                200, "25046", "25046", false, "src/test/resources/files/test.pdf", null},
                {CREATOR_USER_NAME, CREATOR_PASSWORD ,
                 400, "25046", "25046", true, null, "Error: Invalid report archive token!"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD ,
                400, "25046", "125046", false, null, "Error: No such report found!"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD ,
                 400, "25046", "125046", true, null, "Error: Invalid report archive token!"}
        };
    }

    @Test(dataProvider = "getFetchDocsWithTokenDataProvider", enabled = false)
    void getFetchWithToken(String userName, String userPassword, int expectedReturnCode, String studyReportId,
                           String secondStudyReportId, boolean sleep,
                           String expectedPdfFile, String expectedErrorMessage) throws Exception {
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);
        Response response = httpRequest.request(Method.GET, String.format(GET_FETCH_DOCS_URI, studyReportId));
        Assert.assertEquals(String.format("Response code should be %s", 200),
                200, response.getStatusCode());

        String uri = response.jsonPath().get(URI).toString();
        httpRequest.header(ACCEPT_ENCODING, GZIP);

        //Sleep for 10 seconds - waiting for archive token to expire
        if(sleep) {
            Thread.sleep(10000);
        }

        Response getFetchDocsWithTokeResponse = httpRequest.request(Method.GET,
                String.format(GET_FETCH_DOCS_URI + "/" + uri.substring(uri.lastIndexOf('/')+1),
                        secondStudyReportId));
        Assert.assertEquals(String.format("Response code should be %s", expectedReturnCode),
                expectedReturnCode, getFetchDocsWithTokeResponse.getStatusCode());

        switch(expectedReturnCode) {
            case 200:
                TestUtilities.readResponseInPdf(getFetchDocsWithTokeResponse, "src/test/tmp/fetchDocsWithTokenTest.pdf");
                PDFUtil pdfUtil = new PDFUtil();

                Assert.assertTrue(String.format("Get %s/%s should retrieve file identical to %s file",
                        GET_FETCH_DOCS_URI, uri.substring(uri.lastIndexOf('/')+1),
                        expectedPdfFile),pdfUtil.compare(expectedPdfFile, "src/test/tmp/fetchDocsWithTokenTest.pdf"));
                break;
            case 400:
                Assert.assertEquals(String.format("Error message should be %s", expectedErrorMessage),
                        expectedErrorMessage, getFetchDocsWithTokeResponse.jsonPath().get("message"));
                break;
        }
    }

    @DataProvider(name = "getPdfSearchReportDataProvider")
    public Object[][] getPdfSearchReportDataProvider(){
        return new Object[][]{{CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55",
                200, "src/test/resources/files/expectedGetPdfSearchReportPatientId55imPact.json"}
        };
    }
    @Test(dataProvider = "getPdfSearchReportDataProvider", enabled = true)
    void getPdfSearchReport(String userName, String userPassword, String reportType, String patientId,
                            int expectedReturnCode,
                            String expectedResponseFile) throws Exception{
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);
        Response response = httpRequest.request(Method.GET, String.format(GET_PDF_SEARCH_REPORT, reportType, patientId));
        Assert.assertEquals(String.format("Response code should be %s", expectedReturnCode),
                expectedReturnCode, response.getStatusCode());
        String expectedResponse = TestUtilities.readJsonFile(expectedResponseFile);
        JSONAssert.assertEquals(String.format("API:%s\nResponse should be %s",
                String.format(GET_PDF_SEARCH_REPORT, reportType, patientId), expectedResponse), expectedResponse, response.asPrettyString(), false);
    }

}
