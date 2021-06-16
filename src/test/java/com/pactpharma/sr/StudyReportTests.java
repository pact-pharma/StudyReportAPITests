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
    @Test(dataProvider = "getFetchDocsDataProvider", enabled = true)
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

    @Test(dataProvider = "getFetchDocsWithTokenDataProvider", enabled = true)
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

    //Patient 0603 has id 55
    //Patient 0612 has is 2383964
    @DataProvider(name = "getPdfSearchReportDataProvider")
    public Object[][] getPdfSearchReportDataProvider(){
        return new Object[][]{{CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", null, null, null, null, null,
                200, "src/test/resources/files/expectedGetPdfSearchReportImPact.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", "20002337", null, null, null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportImPactWithExperimentId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", "20002337", "M01", null, null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportImPactWithExperimentIdAndImpactSampleName.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", null, "M01", null, null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportImPactWithImpactSampleName.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", null, null, null, "21-006", null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportImPact.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", null, null, "PP001146", null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportImPact.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", null, null, "PP001146", "21-006", null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportImPact.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", "20002337", "M01", "PP001146", "21-006", null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportImPactWithExperimentIdAndImpactSampleName.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "5500000", null, null, null, null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportImpactWithIncorrectPatientId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", "0.001", null, null, null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportImpactWithIncorrectExperimentId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", null, "XXXX", null, null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportImpactWithIncorrectImpactSampleName.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", "5500000", "XXXX", null, null, null,
                        400, "src/test/resources/files/expectedGetPdfSearchReportImpactWithIncorrectExperimentIdAndImpactName.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Bioinformatics", "55", null, null, null, null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportBioinformatics.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Bioinformatics", "55", null, null, "PP001585", "20-628", "12860102C",
                        200, "src/test/resources/files/expectedGetPdfSearchReportBioinformaticsWithSampleNameStudyIdHgxIdentifier.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Bioinformatics", "55", null, null, "PP001584", "20-627", "12860102",
                        200, "src/test/resources/files/expectedGetPdfSearchReportBioinformaticsWithIncorrectSampleNameStudyIdHgxIdentifier.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Bioinformatics", "55", "20002337", "XXXX", "PP001585", "20-628", "12860102C",
                        200, "src/test/resources/files/expectedGetPdfSearchReportBioinformaticsWithAllParameters.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Bioinformatics", "55", null, null, "PP001585", null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportBioinformaticsWithSampleName.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Bioinformatics", "55", null, null, null, "20-628", null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportBioinformaticsWithStudyId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Bioinformatics", "55", null, null, null, null, "12860102C",
                        200, "src/test/resources/files/expectedGetPdfSearchReportBioinformaticsWithHgxIdentifier.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Protein Science(S)", "55", null, null, null, null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportPSS.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Protein Science(S)", "55", "2035300", null, null, null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportPSSWithExpId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Protein Science(S)", "55", "203530011", null, null, null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportWithIncorrectPSSWithExpId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Protein Science(L)", "2383964", null, null, null, null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportPSL.json"}
        };
    }
    @Test(dataProvider = "getPdfSearchReportDataProvider", enabled = true)
    void getPdfSearchReport(String userName, String userPassword, String reportType, String patientId,
                            String experimentId, String impactSampleName, String sampleName, String studyId, String hgxIdentifier,
                            int expectedReturnCode,
                            String expectedResponseFile) throws Exception{
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);
        String requestUrl = constructPdfSearchReportUrl(reportType, patientId,
                experimentId, impactSampleName, sampleName, studyId, hgxIdentifier);

        System.out.println("URL: " + requestUrl);
        Response response = httpRequest.request(Method.GET, requestUrl);

        Assert.assertEquals(String.format("API:%s\nResponse code should be %s", requestUrl, expectedReturnCode),
                expectedReturnCode, response.getStatusCode());
        String expectedResponse = TestUtilities.readJsonFile(expectedResponseFile);

        JSONAssert.assertEquals(String.format("API:%s\nResponse should be %s",
                requestUrl, expectedResponse), expectedResponse, response.asPrettyString(), false);

    }



    private String constructPdfSearchReportUrl(String reportType, String patientId,
       String experimentId, String impactSampleName, String sampleName, String studyId, String hgxIdentifier) {
        StringBuilder strBuilderUrl
                = new StringBuilder();
        strBuilderUrl.append(String.format(GET_PDF_SEARCH_REPORT, reportType, patientId));
        strBuilderUrl = TestUtilities.constructPartOfUrl(strBuilderUrl, experimentId, "&exp_id=%s");
        strBuilderUrl = TestUtilities.constructPartOfUrl(strBuilderUrl, impactSampleName, "&impact_sample_name=%s");
        strBuilderUrl = TestUtilities.constructPartOfUrl(strBuilderUrl, sampleName, "&sample_name=%s");
        strBuilderUrl = TestUtilities.constructPartOfUrl(strBuilderUrl,  studyId, "&study_id=%s");
        strBuilderUrl = TestUtilities.constructPartOfUrl(strBuilderUrl,  hgxIdentifier, "&hgx_identifier=%s");
        return strBuilderUrl.toString();

    }
}
