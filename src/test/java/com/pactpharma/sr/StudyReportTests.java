package com.pactpharma.sr;

import com.testautomationguru.utility.PDFUtil;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.skyscreamer.jsonassert.JSONAssert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static com.pactpharma.sr.TestConstants.*;
import static com.pactpharma.sr.TestUtilities.addBodyParameter;
import static com.pactpharma.sr.TestUtilities.convertBase64ToPdfFile;


public class StudyReportTests {
    final boolean isTestEnabled = true;

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
    @Test(dataProvider = "getFetchDocsDataProvider", enabled = isTestEnabled)
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

    @Test(dataProvider = "getFetchDocsWithTokenDataProvider", enabled = isTestEnabled)
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
                        200, "src/test/resources/files/expectedGetPdfSearchReportPSL.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Protein Science(L)", "2383964", "3118938", null, null, null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportPSLWithExpId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Protein Science(L)", "2383964", "311893800", null, null, null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportPSLWithIncorrectExpId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Gene Editing", "2383964", null, null, null, null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportGE.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Gene Editing", "2383964", null, null, null, "21-088", null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportGEWithStudyReportId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Gene Editing", "2383964", null, null, null, "21-088XX", null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportGEWithIncorrectStudyReportId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Tumor Immunology", "2383964", null, null, null, null, null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportTI.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Tumor Immunology", "2383964", null, null, null, "21-247", null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportTIWithStudyReportId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Tumor Immunology", "2383964", null, null, null, "21-247XX", null,
                        200, "src/test/resources/files/expectedGetPdfSearchReportTIWithIncorrectStudyReportId.json"}
        };
    }
    @Test(dataProvider = "getPdfSearchReportDataProvider", enabled = isTestEnabled)
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

    @DataProvider(name = "putReportReportsDataProvider")
    public Object[][] putReportReportsDataProvider() {
        return new Object[][]{
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "24682", 200,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                "src/test/resources/files/putReportReports.pdf"},
               {CREATOR_USER_NAME, CREATOR_PASSWORD, "24682", 200,
                        null, "06/May/21", null, null, null, null, null,
                        null, null, null, null, null, null,
                        "src/test/resources/files/putReportReportsWithHandOffDate.pdf"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "24682", 200,
                        null, "06/May/21","This is tumor fusion Detected Comment",
                        "This is Low Expressed Nsm Comment", "Low Tc By Ngs Pct Comment",
                        "This is test recommendation", "This is test amendments",
                        "Melanoma", "Premalignant", "legs", "30748", "11905", null,
                        "src/test/resources/files/putReportReportsWithHandOffDate.pdf"}
              /*  {CREATOR_USER_NAME, CREATOR_PASSWORD, "51894", 200,
                        null, "06/Apr/21","This is tumor fusion Detected Comment",
                        "This is Low Expressed Nsm Comment", "Low Tc By Ngs Pct Comment",
                        "This is test recommendation", "This is test amendments",
                        "Melanoma", "Premalignant", "legs", null, null, null,
                        "src/test/resources/files/putReportReportsWithHandOffDate.pdf"},*/
                };
    }

    //Study Id belong to Patint 0612
    @Test(dataProvider = "putReportReportsDataProvider", enabled = true)
    void putReportReports(String userName, String userPassword, String studyReportId,
                          int expectedReturnCode, String fileAttachmentName, String compactReportHandOffDate,
                          String tumorFusionDetectedComment, String lowExpressedNsmComment,
                          String lowTcByNgsPctComment, String recommendation, String amendments,
                          String cancerType, String tumorType, String tumorLocation, String expId,
                          String tCellNonConfidentCount, String lscSelectedSamples,
                          String expectedResponseFile) throws Exception{
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);

        JSONObject requestObjectJSON = constructPutReportReportsBody(fileAttachmentName, compactReportHandOffDate,
                tumorFusionDetectedComment, lowExpressedNsmComment, lowTcByNgsPctComment, recommendation, amendments,
                cancerType, tumorType, tumorLocation, expId, tCellNonConfidentCount, lscSelectedSamples);
        System.out.println("Body: " + requestObjectJSON);
        System.out.println("Request:" + String.format(PUT_REPORT_REPORTS , studyReportId));
        httpRequest.body(requestObjectJSON.toJSONString());

        Response response = httpRequest.request(Method.PUT, String.format(PUT_REPORT_REPORTS , studyReportId));
        Assert.assertEquals(String.format("Response code should be %s", expectedReturnCode),
                expectedReturnCode, response.getStatusCode());

        convertBase64ToPdfFile("src/test/tmp/convertPdf.pdf",
                response.getBody().jsonPath().get(TestConstants.PDF).toString());

        PDFUtil pdfUtil = new PDFUtil();

         Assert.assertTrue(String.format("Put %s should retrieve file identical to %s file",
                String.format(PUT_REPORT_REPORTS , studyReportId), expectedResponseFile),
                pdfUtil.compare(expectedResponseFile, "src/test/tmp/convertPdf.pdf"));
    }

    /**
     * This method constructs request body for put /api/v1/report/reports/:id
     * @param fileAttachmentName
     * @param compactReportHandOffDate
     * @param tumorFusionDetectedComment
     * @param lowExpressedNsmComment
     * @param lowTcByNgsPctComment
     * @param recommendation
     * @param amendments
     * @param cancerType
     * @param tumorType
     * @param tumorLocation
     * @param expId
     * @param tCellNonConfidentCount
     * @param lscSelectedSamples
     * @return - Request body JSON File
     */
    private JSONObject constructPutReportReportsBody(String fileAttachmentName, String compactReportHandOffDate,
                                                     String tumorFusionDetectedComment, String lowExpressedNsmComment,
                                                     String lowTcByNgsPctComment, String recommendation, String amendments,
                                                     String cancerType, String tumorType, String tumorLocation, String expId,
                                                     String tCellNonConfidentCount, String lscSelectedSamples) {
        JSONObject requestParams = new JSONObject();
        requestParams = addBodyParameter(requestParams, "fileAttachmentName", fileAttachmentName);
        requestParams = addBodyParameter(requestParams, "compact_report_hand_off_date", compactReportHandOffDate);
        requestParams = addBodyParameter(requestParams, "tumor_fusion_detected_comment", tumorFusionDetectedComment);
        requestParams = addBodyParameter(requestParams, "low_expressed_nsm_comment", lowExpressedNsmComment);
        requestParams = addBodyParameter(requestParams, "low_tc_by_ngs_pct_comment", lowTcByNgsPctComment);
        requestParams = addBodyParameter(requestParams, "recommendation", recommendation);
        requestParams = addBodyParameter(requestParams, "amendments", amendments);
        requestParams = addBodyParameter(requestParams, "cancer_type", cancerType);
        requestParams = addBodyParameter(requestParams, "tumor_type", tumorType);
        requestParams = addBodyParameter(requestParams, "tumor_location", tumorLocation);
        requestParams = addBodyParameter(requestParams, "exp_id", expId);
        requestParams = addBodyParameter(requestParams, "t_cell_non_confident_count", tCellNonConfidentCount);
        requestParams = addBodyParameter(requestParams, "lsc_selected_samples", lscSelectedSamples);
        return requestParams;
    }
}
