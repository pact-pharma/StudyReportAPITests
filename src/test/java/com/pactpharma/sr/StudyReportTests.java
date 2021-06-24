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
import static com.pactpharma.sr.TestUtilities.*;
public class StudyReportTests {
    final boolean isTestEnabled = false;

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
                        expectedErrorMessage, getFetchDocsWithTokeResponse.jsonPath().get(MESSAGE));
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

    //Study Id 51930 belongs to patient 0411 (BIOINFORMATICS)
    //Study Id 24682 belongs to patient 0403 (Protein Science S)
    //Study Id 3017400 belongs to patient 0030 (GE)
    //Study Id 3107271 belongs to patient 0315 (imPACT)
    //Study Id 42954 belongs to patient 0602 (Protein Science L)
    //Study Id 2541372 belongs to patient 0401 (TI)
    //Study Id 38465 belongs to patient 0020 (Status code is approved)

    //SELECT * FROM report_dev.study_report where data_file LIKE '%BINF%' ;
    //select * from report_dev.study_report where data_file LIKE '%GE%' and status='Pending';
    //select * from report_dev.study_report where report_name LIKE '%imPACT%' and status='Pending';
    //select * from report_dev.study_report where report_name LIKE '%Protein Science(L)%' and status='Pending';
    //select id, report_name from report_dev.study_report where report_name LIKE "%Tumor%" and status='Pending';
    @DataProvider(name = "putReportReportsDataProvider")
    public Object[][] putReportReportsDataProvider() {
        return new Object[][]{
               {CREATOR_USER_NAME, CREATOR_PASSWORD, "24682", 200,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, "This test conclusion.",
                "expectedPutReportReports.pdf", null},
               {CREATOR_USER_NAME, CREATOR_PASSWORD, "24682", 200,
                        null, "06/May/21", null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        "expectedPutReportReportsWithHandOffDate.pdf", null},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "24682", 200,
                        null, "06/May/21","This is tumor fusion Detected Comment",
                        "This is Low Expressed Nsm Comment", "Low Tc By Ngs Pct Comment",
                        "This is test recommendation", "This is test amendments",
                        "Melanoma", "Premalignant", "legs", "30748", "11905", null, null,
                        "expectedPutReportReportsWithHandOffDate.pdf", null},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "51930", 200,
                        null, null, "This is tumor fusion Detected Comment",
                        "This is Low Expressed Nsm Comment", "Low Tc By Ngs Pct Comment",
                        "This is test recommendation", "This is test amendments",
                        "Melanoma", "Premalignant", "legs", null, null, null, "This test conclusion.",
                        "expectedPutReportReportsBioinformaticsWithCommentsRecommendationsAmendmentsCancerAndTumorTypes.pdf", null},
               {CREATOR_USER_NAME, CREATOR_PASSWORD, "3017400", 200,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        "expectedReportReportsGE.pdf", null},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "3107271", 200,
                         null, null, null, null, null, null, null,
                         null, null, null, null, null, LSC_SELECTED_SAMPLES, null,
                        "expectedPutReportReportsPSL.pdf", null},
              /*  {CREATOR_USER_NAME, CREATOR_PASSWORD, "42981", 200,
                        null, "08/May/21", null, null, null, null, null,
                        null, null, null, null, null, LSC_SELECTED_SAMPLES, null,
                        "expectedPutReportReportsPSLWithCompactReportHandOffDate.pdf", null},*/
               {CREATOR_USER_NAME, CREATOR_PASSWORD, "42954", 400,
                        null, "08/May/21", null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        null, "Error: Named parameter \":lsc_selected_samples\" has no value in the given object."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "2541372", 200,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, "This test conclusion.",
                        "expectedPutReportReportsTI.pdf", null},
                //Study Report with Approved Status
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "38465", 200,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        null, "Modifications to approved report are disallowed!"},
                //Study Report with USER APPROVAL permissions
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "2541372", 400,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null,
                        "User svc-study-report-approval@pactpharma.com does not have permission to " +
                                "update report of type Tumor Immunology"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "24682", 400,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null,
                        "User svc-study-report-approval@pactpharma.com does " +
                                "not have permission to update report of type Protein Science(S)"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "51930", 400,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null,
                        "User svc-study-report-approval@pactpharma.com does not " +
                                "have permission to update report of type Bioinformatics"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "42954", 400,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null,
                        "User svc-study-report-approval@pactpharma.com " +
                                "does not have permission to update report of type Protein Science(L)"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "3017400", 400,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null,
                        "User svc-study-report-approval@pactpharma.com does not have " +
                                "permission to update report of type Gene Editing"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "2541372", 400,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null,
                        "User svc-study-report-approval@pactpharma.com does not have " +
                                "permission to update report of type Tumor Immunology"},
               /* {CREATOR_USER_NAME, CREATOR_PASSWORD, "24682", 200,
                        NOT_EXISTING_FIlE_ATTACHMENT_NAME, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        "expectedPutReportReports.pdf", null}*/
                };
    }

    @Test(dataProvider = "putReportReportsDataProvider", enabled = isTestEnabled )
    void putReportReports(String userName, String userPassword, String studyReportId,
                          int expectedReturnCode, String[] fileAttachmentName, String compactReportHandOffDate,
                          String tumorFusionDetectedComment, String lowExpressedNsmComment,
                          String lowTcByNgsPctComment, String recommendation, String amendments,
                          String cancerType, String tumorType, String tumorLocation, String expId,
                          String tCellNonConfidentCount, String[] lscSelectedSamples, String conclusion,
                          String expectedResponseFile, String expectedErrorMessage) throws Exception{
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);

        JSONObject requestObjectJSON = constructPutReportReportsBody(conclusion, fileAttachmentName, compactReportHandOffDate,
                tumorFusionDetectedComment, lowExpressedNsmComment, lowTcByNgsPctComment, recommendation, amendments,
                cancerType, tumorType, tumorLocation, expId, tCellNonConfidentCount, lscSelectedSamples);
        System.out.println("Body: " + requestObjectJSON.toJSONString());
        System.out.println("Request:" + String.format(PUT_REPORT_REPORTS , studyReportId));
        httpRequest.body(requestObjectJSON.toJSONString());

        Response response = httpRequest.request(Method.PUT, String.format(PUT_REPORT_REPORTS , studyReportId));
        Assert.assertEquals(String.format("Response code should be %s", expectedReturnCode),
                expectedReturnCode, response.getStatusCode());

        switch(expectedReturnCode) {
            case 200:
                if(expectedResponseFile != null) {
                    convertBase64ToPdfFile(String.format("src/test/tmp/%s", expectedResponseFile),
                            response.getBody().jsonPath().get(TestConstants.PDF).toString());

                    PDFUtil pdfUtil = new PDFUtil();

                    Assert.assertTrue(String.format("Put %s should retrieve file identical to %s file",
                            String.format(PUT_REPORT_REPORTS, studyReportId), expectedResponseFile),
                            pdfUtil.compare(String.format("src/test/resources/files/%s",expectedResponseFile), String.format("src/test/tmp/%s",expectedResponseFile)));
                } else {
                    Assert.assertEquals(String.format("Error message should be %s", expectedErrorMessage),
                            expectedErrorMessage, response.jsonPath().get(MESSAGE));
                }
                break;
            case 400:
                Assert.assertEquals(String.format("Error message should be %s", expectedErrorMessage),
                        expectedErrorMessage, response.jsonPath().get(MESSAGE));
                break;
        }

    }

    //Study id 27651 for patient 0027
    @DataProvider(name = "postReportReportsSaveDataProvider")
    public Object[][] postReportReportsSaveDataProvider() {
        return new Object[][]{
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "27651", 200,
                        "Protein Science(S)", "0027", "PACT407C",
                        "20-332_20001201_0027_PACT407C_Protein Science(S).pdf", null, "20-332",
                        "Pending", null, "04/Dec/2020", null, null, null, null, null,
                        null, null, null, "20001201", null, null, "This test conclusion.",
                        "  Report saved successfully."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "2677093", 200,
                        "Bioinformatics", "0412", "PACT493C",
                        "21-063_0412_PACT493C_Bioinformatics.pdf", null, "21-063",
                        "Pending", null, "04/Dec/2020",
                        "This is tumor fusion Detected Comment",
                        "This is Low Expressed Nsm Comment", "Low Tc By Ngs Pct Comment",
                        "This is test recommendation", "This is test amendments",
                        "Melanoma", "Premalignant", "legs",
                        null, null, null, "This test conclusion.", "  Report saved successfully."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "3297225", 200,
                        "imPACT", "0403", "PACT443C",
                        "20-457_0403_PACT443C_imPACT.pdf", null, "20-457",
                        "In Progress", null, null,
                        null, null, null, null, null, null, null, null,
                        null, "100.00", null, "This test conclusion.", "  Report saved successfully."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "2541133", 200,
                        "Tumor Immunology", "0403", "PACT443C",
                        "20-565_0403_PACT443C_Tumor Immunology.pdf", null, "20-565",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, null, "This TI test conclusion.", "  Report saved successfully."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "42993", 200,
                        "Protein Science(L)", "0403", "PACT443C",
                        "20-545_20002003_0403_PACT443C_Protein Science(L).pdf", null, "20-545",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, M02_LSC_SELECTED_SAMPLES, "This is test Protein Science(L) conclusion.",
                        "  Report saved successfully."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "2542290", 200,
                        "Gene Editing", "0504", "PACT326C",
                        "20-393_0504_PACT326C_Gene Editing.pdf", null, "20-393",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, null, "This is test GE conclusion.", "  Report saved successfully."},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "2542290", 400,
                        "Gene Editing", "0504", "PACT326C",
                        "20-393_0504_PACT326C_Gene Editing.pdf", null, "20-393",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, null, "This is test GE conclusion.",
                        "User svc-study-report-approval@pactpharma.com does " +
                                "not have permission to save report of type Gene Editing"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "42993", 400,
                        "Protein Science(L)", "0403", "PACT443C",
                        "20-545_20002003_0403_PACT443C_Protein Science(L).pdf", null, "20-545",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, M02_LSC_SELECTED_SAMPLES, "This is test Protein Science(L) conclusion.",
                        "User svc-study-report-approval@pactpharma.com does not have permission to save " +
                                "report of type Protein Science(L)"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "2541133", 400,
                        "Tumor Immunology", "0403", "PACT443C",
                        "20-565_0403_PACT443C_Tumor Immunology.pdf", null, "20-565",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, null, "This TI test conclusion.",
                        "User svc-study-report-approval@pactpharma.com does not have" +
                                " permission to save report of type Tumor Immunology"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "3297225", 400,
                        "imPACT", "0403", "PACT443C",
                        "20-457_0403_PACT443C_imPACT.pdf", null, "20-457",
                        "In Progress", null, null,
                        null, null, null, null, null, null, null, null,
                        null, "100.00", null, "This test conclusion.",
                        "User svc-study-report-approval@pactpharma.com does not " +
                                "have permission to save report of type imPACT"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "2677093", 400,
                        "Bioinformatics", "0412", "PACT493C",
                        "21-063_0412_PACT493C_Bioinformatics.pdf", null, "21-063",
                        "Pending", null, "04/Dec/2020",
                        "This is tumor fusion Detected Comment",
                        "This is Low Expressed Nsm Comment", "Low Tc By Ngs Pct Comment",
                        "This is test recommendation", "This is test amendments",
                        "Melanoma", "Premalignant", "legs",
                        null, null, null, "This test conclusion.",
                        "User svc-study-report-approval@pactpharma.com does not have " +
                                "permission to save report of type Bioinformatics"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "27651", 400,
                        "Protein Science(S)", "0027", "PACT407C",
                        "20-332_20001201_0027_PACT407C_Protein Science(S).pdf", null, "20-332",
                        "Pending", null, "04/Dec/2020", null, null, null, null, null,
                        null, null, null, "20001201", null, null, "This test conclusion.",
                        "User svc-study-report-approval@pactpharma.com does not have permission " +
                                "to save report of type Protein Science(S)"}
        };
    }

    //Study id 27651 for patient 0027
    @Test(dataProvider = "postReportReportsSaveDataProvider", enabled = isTestEnabled)
    void postReportReportsSave(String userName, String userPassword, String studyReportId,
                          int expectedReturnCode,
                          String reportType, String patientNum, String patient,
                          String reportName, String documentNames, String studyId, String status,
                          String[] fileAttachmentName, String compactReportHandOffDate,
                          String tumorFusionDetectedComment, String lowExpressedNsmComment,
                          String lowTcByNgsPctComment, String recommendation, String amendments,
                          String cancerType, String tumorType, String tumorLocation, String expId,
                          String tCellNonConfidentCount, String[] lscSelectedSamples, String conclusion, String expectedMessage) throws Exception {
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);

        JSONObject requestObjectJSON = constructPutReportReportsBody(conclusion, fileAttachmentName, compactReportHandOffDate,
                tumorFusionDetectedComment, lowExpressedNsmComment, lowTcByNgsPctComment, recommendation, amendments,
                cancerType, tumorType, tumorLocation, expId, tCellNonConfidentCount, lscSelectedSamples);
        System.out.println("Body: " + requestObjectJSON.toJSONString());
        System.out.println("Request:" + String.format(POST_REPORT_REPORTS_SAVE, studyReportId));
        httpRequest.body(requestObjectJSON.toJSONString());

        Response response = httpRequest.request(Method.POST, String.format(POST_REPORT_REPORTS_SAVE, studyReportId));
        Assert.assertEquals(String.format("Response code should be %s", expectedReturnCode),
                expectedReturnCode, response.getStatusCode());


        switch(expectedReturnCode) {
            case 200:
                Assert.assertTrue(String.format("Request PUT %s should print '%s'",
                        String.format(POST_REPORT_REPORTS_SAVE, studyReportId), expectedMessage),
                        expectedMessage.equalsIgnoreCase(removeNewLine(removeTags(response.body().asPrettyString()))));
                validateReport(httpRequest, userName, userPassword, studyReportId, reportType,
                        patientNum, patient, reportName, documentNames, studyId, status, expectedReturnCode,
                        fileAttachmentName, compactReportHandOffDate,
                    tumorFusionDetectedComment, lowExpressedNsmComment,
                    lowTcByNgsPctComment, recommendation, amendments,
                    cancerType, tumorType, tumorLocation, expId,
                    tCellNonConfidentCount, lscSelectedSamples, conclusion);
                break;
            case 400:
                Assert.assertEquals(String.format("Error message should be %s", expectedMessage),
                        expectedMessage, response.jsonPath().get(MESSAGE));
                break;
        }
    }


    @DataProvider(name = "putReportReportsSubmitDataProvider")
    public Object[][] putReportReportsSubmitDataProvider() {
        return new Object[][]{
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "27651", 200,
                        "Protein Science(S)", "0027", "PACT407C",
                        "20-332_20001201_0027_PACT407C_Protein Science(S).pdf", null, "20-332",
                        "Pending", null, "04/Dec/2020", null, null, null, null, null,
                        null, null, null, "20001201", null, null, "This test conclusion.",
                        "  Report submitted successfully."}
        };
    }

    @Test(dataProvider = "putReportReportsSubmitDataProvider", enabled = true)
    void putReportReportsSubmit(String userName, String userPassword, String studyReportId,
                                int expectedReturnCode,
                                String reportType, String patientNum, String patient,
                                String reportName, String documentNames, String studyId, String status,
                                String[] fileAttachmentName, String compactReportHandOffDate,
                                String tumorFusionDetectedComment, String lowExpressedNsmComment,
                                String lowTcByNgsPctComment, String recommendation, String amendments,
                                String cancerType, String tumorType, String tumorLocation, String expId,
                                String tCellNonConfidentCount, String[] lscSelectedSamples, String conclusion,
                                String expectedMessage) throws Exception {
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);

        JSONObject requestObjectJSON = constructPutReportReportsBody(conclusion, fileAttachmentName, compactReportHandOffDate,
                tumorFusionDetectedComment, lowExpressedNsmComment, lowTcByNgsPctComment, recommendation, amendments,
                cancerType, tumorType, tumorLocation, expId, tCellNonConfidentCount, lscSelectedSamples);
        System.out.println("Body: " + requestObjectJSON.toJSONString());
        System.out.println("Request:" + String.format(PUT_REPORT_REPORTS_SUBMIT, studyReportId));
        httpRequest.body(requestObjectJSON.toJSONString());

        Response response = httpRequest.request(Method.PUT, String.format(PUT_REPORT_REPORTS_SUBMIT , studyReportId));
        Assert.assertEquals(String.format("Response code should be %s", expectedReturnCode),
                expectedReturnCode, response.getStatusCode());

        switch(expectedReturnCode) {
            case 200:
                Assert.assertTrue(String.format("Request PUT %s should print '%s'",
                        String.format(POST_REPORT_REPORTS_SAVE, studyReportId), expectedMessage),
                        expectedMessage.equalsIgnoreCase(removeNewLine(removeTags(response.body().asPrettyString()))));
                validateReport(httpRequest, userName, userPassword, studyReportId, reportType,
                        patientNum, patient, reportName, documentNames, studyId, status, expectedReturnCode,
                        fileAttachmentName, compactReportHandOffDate,
                        tumorFusionDetectedComment, lowExpressedNsmComment,
                        lowTcByNgsPctComment, recommendation, amendments,
                        cancerType, tumorType, tumorLocation, expId,
                        tCellNonConfidentCount, lscSelectedSamples, conclusion);
                break;
            case 400:
                Assert.assertEquals(String.format("Error message should be %s", expectedMessage),
                        expectedMessage, response.jsonPath().get(MESSAGE));
                break;
        }
    }

    /**
     * This method reads study report and validates it.
     * @param httpRequest
     * @param userName
     * @param userPassword
     * @param studyReportId
     * @param reportType
     * @param patientNum
     * @param patient
     * @param reportName
     * @param documentNames
     * @param studyId
     * @param status
     * @param expectedReturnCode
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
     * @param conclusion
     */
    private void validateReport(RequestSpecification httpRequest, String userName, String userPassword, String studyReportId,
                                String reportType, String patientNum, String patient,
                                String reportName, String documentNames, String studyId, String status,
                                int expectedReturnCode, String[] fileAttachmentName, String compactReportHandOffDate,
                                String tumorFusionDetectedComment, String lowExpressedNsmComment,
                                String lowTcByNgsPctComment, String recommendation, String amendments,
                                String cancerType, String tumorType, String tumorLocation, String expId,
                                String tCellNonConfidentCount, String[] lscSelectedSamples, String conclusion) {

        Response response = httpRequest.request(Method.GET, String.format(GET_PDF_REPORT, studyReportId));
        System.out.println("response" + response.asPrettyString());
        validateValueFromResponse(response,"study.id", studyReportId);
        validateValueFromResponse(response,"study.studyId", studyId);
        validateValueFromResponse(response,"study.status", status);
        validateValueFromResponse(response,"study.patient", patient);
        validateValueFromResponse(response,"study.patient_num", patientNum);
        validateValueFromResponse(response,"study.report_type", reportType);
        validateValueFromResponse(response,"study.conclusion", conclusion);
        readJSonArrayFromResponse(response, "study.document_name", documentNames );
        switch(reportType) {
            case "Protein Science(S)":
                validateValueFromResponse(response, "reportDetails.compact.hand_off_date", compactReportHandOffDate);
                validateValueFromResponse(response, "reportDetails.experiment.exp_id", expId);
                break;
            case "Bioinformatics":
                validateValueFromResponse(response, "reportDetails.tumor_fusion_detected_comment", tumorFusionDetectedComment);
                validateValueFromResponse(response, "reportDetails.low_expressed_nsm_comment", lowExpressedNsmComment);
                validateValueFromResponse(response, "reportDetails.low_tc_by_ngs_pct_comment", lowTcByNgsPctComment);
                validateValueFromResponse(response, "reportDetails.recommendation", recommendation);
                break;
            case "imPACT":
                validateValueFromResponse(response, "reportDetails.t_cell_non_confident_count", tCellNonConfidentCount);
                break;
            case "Tumor Immunology":
                break;
            case "Protein Science(L)":
                validateArrayFromResponse(response, "reportDetails.samples", lscSelectedSamples);
                break;
            case "Gene Editing":
                //validateValueFromResponse(response, "reportDetails.study.study_id", studyId);
                //validateValueFromResponse(response, "reportDetails.study.pact_id", patient);
                break;
        }
    }

    /**
     * This method constructs request body for put /api/v1/report/reports/:id
     * @param conclusion
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
    private JSONObject constructPutReportReportsBody(String conclusion, String[] fileAttachmentName, String compactReportHandOffDate,
                                                     String tumorFusionDetectedComment, String lowExpressedNsmComment,
                                                     String lowTcByNgsPctComment, String recommendation, String amendments,
                                                     String cancerType, String tumorType, String tumorLocation, String expId,
                                                     String tCellNonConfidentCount, String[] lscSelectedSamples) {
        JSONObject requestParams = new JSONObject();
        requestParams = addBodyParameter(requestParams, CONCLUSION, conclusion);
        requestParams = addBodyArray(requestParams, FILE_ATTACHMENT_NAME, fileAttachmentName);
        requestParams = addBodyParameter(requestParams, COMPACT_REPORT_HAND_OFF_DATE, compactReportHandOffDate);
        requestParams = addBodyParameter(requestParams, TUMOR_FUSION_DETECTED_COMMENT, tumorFusionDetectedComment);
        requestParams = addBodyParameter(requestParams, LOW_EXPRESSED_NSM_COMMENT, lowExpressedNsmComment);
        requestParams = addBodyParameter(requestParams, LOW_TC_BY_NGS_PCT_COMMENT, lowTcByNgsPctComment);
        requestParams = addBodyParameter(requestParams, RECOMMENDATION, recommendation);
        requestParams = addBodyParameter(requestParams, AMENDMENTS, amendments);
        requestParams = addBodyParameter(requestParams, CANCER_TYPE, cancerType);
        requestParams = addBodyParameter(requestParams, TUMOR_TYPE, tumorType);
        requestParams = addBodyParameter(requestParams, TUMOR_LOCATION, tumorLocation);
        requestParams = addBodyParameter(requestParams, EXP_ID, expId);
        requestParams = addBodyParameter(requestParams, T_CELL_NON_CONFIDENT_COUNT, tCellNonConfidentCount);
        requestParams = addBodyArray(requestParams, SELECTED_LSC_SAMPLES, lscSelectedSamples);
        return requestParams;
    }
}
