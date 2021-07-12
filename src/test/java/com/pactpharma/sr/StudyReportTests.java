package com.pactpharma.sr;

import com.testautomationguru.utility.PDFUtil;
import com.testrail.listeners.TestNgTestRailListener;
import com.testrail.util.UseAsTestRailId;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.skyscreamer.jsonassert.JSONAssert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import java.io.File;
import java.util.*;

import static com.pactpharma.sr.TestConstants.*;
import static com.pactpharma.sr.TestConstants.APPROVAL_PASSWORD;
import static com.pactpharma.sr.TestUtilities.*;

@Listeners(TestNgTestRailListener.class)
public class StudyReportTests {
final boolean isTestEnabled = false;

    @DataProvider(name = "getFetchDocsDataProvider")
    public Object[][] getFetchDocsDataProvider(){
        return new Object[][] {{GET_FETCH_DOCS_URI, CREATOR_USER_NAME, CREATOR_PASSWORD,
                HttpStatus.SC_OK, "25046", "20-227_20000820_0014_PACT298C_Protein Science(S).tar.gz",
                "https://study-report.zest.pactpharma.com/api/v1/report/reports/25046/fetchdocs/", null},
                {GET_FETCH_DOCS_URI, CREATOR_USER_NAME, CREATOR_PASSWORD,
                        HttpStatus.SC_BAD_REQUEST, "125046", null, null, "Error: No such report found!"},
                {GET_FETCH_DOCS_URI, APPROVAL_USER_NAME, APPROVAL_PASSWORD,
                        HttpStatus.SC_BAD_REQUEST, "25046", null, null, "Error: User svc-study-report-approval@pactpharma.com does not have permission " +
                        "to download report of type Protein Science(S)"},
                {GET_FETCH_IN_WORD_FORMAT_URI, CREATOR_USER_NAME, CREATOR_PASSWORD,
                        HttpStatus.SC_OK, "25046", "20-227_20000820_0014_PACT298C_Protein Science(S).word.tar.gz",
                        "https://study-report.zest.pactpharma.com/api/v1/report/reports/25046/fetchinwordformat/", null},
                {GET_FETCH_IN_WORD_FORMAT_URI, CREATOR_USER_NAME, CREATOR_PASSWORD ,
                        HttpStatus.SC_BAD_REQUEST, "125046", null, null, "Error: No such report found!"},
                {GET_FETCH_IN_WORD_FORMAT_URI, APPROVAL_USER_NAME, APPROVAL_PASSWORD ,
                        HttpStatus.SC_BAD_REQUEST, "25046", null, null, "Error: User svc-study-report-approval@pactpharma.com does not have permission " +
                        "to download report of type Protein Science(S)"}
        };
    }
    @Test(dataProvider = "getFetchDocsDataProvider", enabled = isTestEnabled)
    void getFetchDocsTest(String fetchDocsUri, String userName, String userPassword, int expectedResponseCode, String studyReportId,
                    String expectedArchiveName, String expectedUriPrefix, String expectedErrorMessage) throws Exception {
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);
        Response response = httpRequest.request(Method.GET, String.format(fetchDocsUri, studyReportId));

        Assert.assertEquals(String.format("Response code should be %s", expectedResponseCode),
                expectedResponseCode, response.getStatusCode());
        switch(expectedResponseCode) {
            case HttpStatus.SC_OK:
                Assert.assertEquals(String.format("Archive name should be %s", expectedArchiveName),
                        expectedArchiveName, response.jsonPath().get("archiveName"));
                Assert.assertTrue(String.format("URI string should starts with %s", expectedUriPrefix),
                        response.jsonPath().get(URI).toString().startsWith(expectedUriPrefix));
                break;
            case HttpStatus.SC_BAD_REQUEST:
                System.out.println("Error Message:" + response.jsonPath().get("message"));
                Assert.assertEquals(String.format("Error message should be %s", expectedErrorMessage),
                        expectedErrorMessage, response.jsonPath().get(MESSAGE));
                break;
        }
    }

    @DataProvider(name = "getFetchDocsWithTokenDataProvider")
    public Object[][] getFetchDocsWithTokenDataProvider(){
        return new Object[][] {{CREATOR_USER_NAME, CREATOR_PASSWORD ,
                HttpStatus.SC_OK, "25046", "25046", false, "src/test/resources/files/test.pdf", null},
                {CREATOR_USER_NAME, CREATOR_PASSWORD ,
                 HttpStatus.SC_BAD_REQUEST, "25046", "25046", true, null, "Invalid report archive token!"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD ,
                        HttpStatus.SC_BAD_REQUEST, "25046", "125046", false, null, "No such report found!"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD ,
                        HttpStatus.SC_BAD_REQUEST, "25046", "125046", true, null, "Invalid report archive token!"}
        };
    }

    @Test(dataProvider = "getFetchDocsWithTokenDataProvider", enabled = isTestEnabled)
    void getFetchWithToken(String userName, String userPassword, int expectedResponseCode, String studyReportId,
                           String secondStudyReportId, boolean sleep,
                           String expectedPdfFile, String expectedErrorMessage) throws Exception {
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);
        Response response = httpRequest.request(Method.GET, String.format(GET_FETCH_DOCS_URI, studyReportId));
        Assert.assertEquals(String.format("Response code should be %s", expectedResponseCode),
                HttpStatus.SC_OK, response.getStatusCode());

        String uri = response.jsonPath().get(URI).toString();
        httpRequest.header(ACCEPT_ENCODING, GZIP);

        //Sleep for 10 seconds - waiting for archive token to expire
        if(sleep) {
            Thread.sleep(10000);
        }

        Response getFetchDocsWithTokeResponse = httpRequest.request(Method.GET,
                String.format(GET_FETCH_DOCS_URI + "/" + uri.substring(uri.lastIndexOf('/')+1),
                        secondStudyReportId));
        Assert.assertEquals(String.format("Response code should be %s", expectedResponseCode),
                expectedResponseCode, getFetchDocsWithTokeResponse.getStatusCode());

        switch(expectedResponseCode) {
            case HttpStatus.SC_OK:
                TestUtilities.readResponseInPdf(getFetchDocsWithTokeResponse, "src/test/tmp/fetchDocsWithTokenTest.pdf");
                PDFUtil pdfUtil = new PDFUtil();

                Assert.assertTrue(String.format("Get %s/%s should retrieve file identical to %s file",
                        GET_FETCH_DOCS_URI, uri.substring(uri.lastIndexOf('/')+1),
                        expectedPdfFile),pdfUtil.compare(expectedPdfFile, "src/test/tmp/fetchDocsWithTokenTest.pdf"));
                break;
            case HttpStatus.SC_BAD_REQUEST:
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
                HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportImPact.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", "20002337", null, null, null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportImPactWithExperimentId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", "20002337", "M01", null, null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportImPactWithExperimentIdAndImpactSampleName.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", null, "M01", null, null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportImPactWithImpactSampleName.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", null, null, null, "21-006", null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportImPact.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", null, null, "PP001146", null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportImPact.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", null, null, "PP001146", "21-006", null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportImPact.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", "20002337", "M01", "PP001146", "21-006", null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportImPactWithExperimentIdAndImpactSampleName.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "5500000", null, null, null, null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportImpactWithIncorrectPatientId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", "0.001", null, null, null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportImpactWithIncorrectExperimentId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", null, "XXXX", null, null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportImpactWithIncorrectImpactSampleName.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "imPACT", "55", "5500000", "XXXX", null, null, null,
                        400, "src/test/resources/files/expectedGetPdfSearchReportImpactWithIncorrectExperimentIdAndImpactName.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Bioinformatics", "55", null, null, null, null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportBioinformatics.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Bioinformatics", "55", null, null, "PP001585", "20-628", "12860102C",
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportBioinformaticsWithSampleNameStudyIdHgxIdentifier.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Bioinformatics", "55", null, null, "PP001584", "20-627", "12860102",
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportBioinformaticsWithIncorrectSampleNameStudyIdHgxIdentifier.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Bioinformatics", "55", "20002337", "XXXX", "PP001585", "20-628", "12860102C",
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportBioinformaticsWithAllParameters.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Bioinformatics", "55", null, null, "PP001585", null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportBioinformaticsWithSampleName.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Bioinformatics", "55", null, null, null, "20-628", null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportBioinformaticsWithStudyId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Bioinformatics", "55", null, null, null, null, "12860102C",
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportBioinformaticsWithHgxIdentifier.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Protein Science(S)", "55", null, null, null, null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportPSS.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Protein Science(S)", "55", "2035300", null, null, null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportPSSWithExpId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Protein Science(S)", "55", "203530011", null, null, null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportWithIncorrectPSSWithExpId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Protein Science(L)", "2383964", null, null, null, null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportPSL.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Protein Science(L)", "2383964", "3118938", null, null, null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportPSLWithExpId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Protein Science(L)", "2383964", "311893800", null, null, null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportPSLWithIncorrectExpId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Gene Editing", "2383964", null, null, null, null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportGE.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Gene Editing", "2383964", null, null, null, "21-088", null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportGEWithStudyReportId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Gene Editing", "2383964", null, null, null, "21-088XX", null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportGEWithIncorrectStudyReportId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Tumor Immunology", "2383964", null, null, null, null, null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportTI.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Tumor Immunology", "2383964", null, null, null, "21-247", null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportTIWithStudyReportId.json"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "Tumor Immunology", "2383964", null, null, null, "21-247XX", null,
                        HttpStatus.SC_OK, "src/test/resources/files/expectedGetPdfSearchReportTIWithIncorrectStudyReportId.json"}
        };
    }
    @Test(dataProvider = "getPdfSearchReportDataProvider", enabled = isTestEnabled)
    void getPdfSearchReport(String userName, String userPassword, String reportType, String patientId,
                            String experimentId, String impactSampleName, String sampleName, String studyId, String hgxIdentifier,
                            int expectedResponseCode,
                            String expectedResponseFile) throws Exception{
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);
        String requestUrl = constructPdfSearchReportUrl(reportType, patientId,
                experimentId, impactSampleName, sampleName, studyId, hgxIdentifier);

        System.out.println("URL: " + requestUrl);
        Response response = httpRequest.request(Method.GET, requestUrl);

        Assert.assertEquals(String.format("API:%s\nResponse code should be %s", requestUrl, expectedResponseCode),
                expectedResponseCode, response.getStatusCode());
        String expectedResponse = TestUtilities.readJsonFile(expectedResponseFile);

        JSONAssert.assertEquals(String.format("API:%s\nResponse should be %s",
                requestUrl, expectedResponse), expectedResponse, response.asPrettyString(), false);

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
    //UPDATE report_dev.study_report SET status="Approved" where id=38465;
    //
    @DataProvider(name = "putReportReportsDataProvider")
    public Object[][] putReportReportsDataProvider() {
        return new Object[][]{
               {CREATOR_USER_NAME, CREATOR_PASSWORD, "24682", HttpStatus.SC_OK,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, "This test conclusion.",
                "expectedPutReportReports.pdf", null},
               {CREATOR_USER_NAME, CREATOR_PASSWORD, "24682", HttpStatus.SC_OK,
                        null, "06/May/21", null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        "expectedPutReportReportsWithHandOffDate.pdf", null},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "24682", HttpStatus.SC_OK,
                        null, "06/May/21","This is tumor fusion Detected Comment",
                        "This is Low Expressed Nsm Comment", "Low Tc By Ngs Pct Comment",
                        "This is test recommendation", "This is test amendments",
                        "Melanoma", "Premalignant", "legs", "30748", "11905", null, null,
                        "expectedPutReportReportsWithHandOffDate.pdf", null},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "51930", HttpStatus.SC_OK,
                        null, null, "This is tumor fusion Detected Comment",
                        "This is Low Expressed Nsm Comment", "Low Tc By Ngs Pct Comment",
                        "This is test recommendation", "This is test amendments",
                        "Melanoma", "Premalignant", "legs", null, null, null, "This test conclusion.",
                        "expectedPutReportReportsBioinformaticsWithCommentsRecommendationsAmendmentsCancerAndTumorTypes.pdf", null},
               {CREATOR_USER_NAME, CREATOR_PASSWORD, "3017400", HttpStatus.SC_OK,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        "expectedReportReportsGE.pdf", null},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "3107271", HttpStatus.SC_OK,
                         null, null, null, null, null, null, null,
                         null, null, null, null, null, LSC_SELECTED_SAMPLES, null,
                        "expectedPutReportReportsPSL.pdf", null},
              /*  {CREATOR_USER_NAME, CREATOR_PASSWORD, "42981", 200,
                        null, "08/May/21", null, null, null, null, null,
                        null, null, null, null, null, LSC_SELECTED_SAMPLES, null,
                        "expectedPutReportReportsPSLWithCompactReportHandOffDate.pdf", null},*/
               {CREATOR_USER_NAME, CREATOR_PASSWORD, "42954", HttpStatus.SC_BAD_REQUEST,
                        null, "08/May/21", null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        null, "Named parameter \":lsc_selected_samples\" has no value in the given object."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "2541372", HttpStatus.SC_OK,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, "This test conclusion.",
                        "expectedPutReportReportsTI.pdf", null},
                //Study Report with Approved Status
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "38465", HttpStatus.SC_BAD_REQUEST,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        null, "Modification to approved report is disallowed!"},
                //Study Report with USER APPROVAL permissions
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "2541372", HttpStatus.SC_BAD_REQUEST,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null,
                        "User svc-study-report-approval@pactpharma.com does not have permission to " +
                                "update report of type Tumor Immunology"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "24682", HttpStatus.SC_BAD_REQUEST,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null,
                        "User svc-study-report-approval@pactpharma.com does " +
                                "not have permission to update report of type Protein Science(S)"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "51930", HttpStatus.SC_BAD_REQUEST,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null,
                        "User svc-study-report-approval@pactpharma.com does not " +
                                "have permission to update report of type Bioinformatics"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "42954", HttpStatus.SC_BAD_REQUEST,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null,
                        "User svc-study-report-approval@pactpharma.com " +
                                "does not have permission to update report of type Protein Science(L)"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "3017400", HttpStatus.SC_BAD_REQUEST,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null,
                        "User svc-study-report-approval@pactpharma.com does not have " +
                                "permission to update report of type Gene Editing"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "2541372", HttpStatus.SC_BAD_REQUEST,
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
                          int expectedResponseCode, String[] fileAttachmentName, String compactReportHandOffDate,
                          String tumorFusionDetectedComment, String lowExpressedNsmComment,
                          String lowTcByNgsPctComment, String recommendation, String amendments,
                          String cancerType, String tumorType, String tumorLocation, String expId,
                          String tCellNonConfidentCount, String[] lscSelectedSamples, String conclusion,
                          String expectedResponseFile, String expectedErrorMessage) throws Exception{
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);

        JSONObject requestObjectJSON = constructReportReportsBody(conclusion, fileAttachmentName, compactReportHandOffDate,
                tumorFusionDetectedComment, lowExpressedNsmComment, lowTcByNgsPctComment, recommendation, amendments,
                cancerType, tumorType, tumorLocation, expId, tCellNonConfidentCount, lscSelectedSamples);
        System.out.println("Body: " + requestObjectJSON.toJSONString());
        System.out.println("Request:" + String.format(PUT_REPORT_REPORTS , studyReportId));
        httpRequest.body(requestObjectJSON.toJSONString());

        Response response = httpRequest.request(Method.PUT, String.format(PUT_REPORT_REPORTS , studyReportId));
        Assert.assertEquals(String.format("Response code should be %s", expectedResponseCode),
                expectedResponseCode, response.getStatusCode());

        switch(expectedResponseCode) {
            case HttpStatus.SC_OK:
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
            case HttpStatus.SC_BAD_REQUEST:
                Assert.assertEquals(String.format("Error message should be %s", expectedErrorMessage),
                        expectedErrorMessage, response.jsonPath().get(MESSAGE));
                break;
        }

    }

    //Study id 27651 for patient 0027
    @DataProvider(name = "postReportReportsSaveDataProvider")
    public Object[][] postReportReportsSaveDataProvider() {
        return new Object[][]{
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "27651", HttpStatus.SC_OK,
                        "Protein Science(S)", "0027", "PACT407C",
                        "20-332_20001201_0027_PACT407C_Protein Science(S).pdf", null, "20-332",
                        "In Progress", null, "04/Dec/2020", null, null, null, null, null,
                        null, null, null, "20001201", null, null, "This test conclusion.",
                        "  Report saved successfully."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "2677093", HttpStatus.SC_OK,
                        "Bioinformatics", "0412", "PACT493C",
                        "21-063_0412_PACT493C_Bioinformatics.pdf", null, "21-063",
                        "In Progress", null, "04/Dec/2020",
                        "This is tumor fusion Detected Comment",
                        "This is Low Expressed Nsm Comment", "Low Tc By Ngs Pct Comment",
                        "This is test recommendation", "This is test amendments",
                        "Melanoma", "Premalignant", "legs",
                        null, null, null, "This test conclusion.", "  Report saved successfully."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "3297225", HttpStatus.SC_OK,
                        "imPACT", "0403", "PACT443C",
                        "20-457_0403_PACT443C_imPACT.pdf", null, "20-457",
                        "In Progress", null, null,
                        null, null, null, null, null, null, null, null,
                        null, "100.00", null, "This test conclusion.", "  Report saved successfully."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "2541133", HttpStatus.SC_OK,
                        "Tumor Immunology", "0403", "PACT443C",
                        "20-565_0403_PACT443C_Tumor Immunology.pdf", null, "20-565",
                        "In Progress", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, null, "This TI test conclusion.", "  Report saved successfully."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "42993", HttpStatus.SC_OK,
                        "Protein Science(L)", "0403", "PACT443C",
                        "20-545_20002003_0403_PACT443C_Protein Science(L).pdf", "42993_ge-0030.pdf", "20-545",
                        "In Progress", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, M02_LSC_SELECTED_SAMPLES, "This is test Protein Science(L) conclusion.",
                        "  Report saved successfully."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "2542290", HttpStatus.SC_OK,
                        "Gene Editing", "0504", "PACT326C",
                        "20-393_0504_PACT326C_Gene Editing.pdf", null, "20-393",
                        "In Progress", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, null, "This is test GE conclusion.", "  Report saved successfully."},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "2542290", HttpStatus.SC_BAD_REQUEST,
                        "Gene Editing", "0504", "PACT326C",
                        "20-393_0504_PACT326C_Gene Editing.pdf", null, "20-393",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, null, "This is test GE conclusion.",
                        "User svc-study-report-approval@pactpharma.com does " +
                                "not have permission to save report of type Gene Editing"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "42993", HttpStatus.SC_BAD_REQUEST,
                        "Protein Science(L)", "0403", "PACT443C",
                        "20-545_20002003_0403_PACT443C_Protein Science(L).pdf", null, "20-545",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, M02_LSC_SELECTED_SAMPLES, "This is test Protein Science(L) conclusion.",
                        "User svc-study-report-approval@pactpharma.com does not have permission to save " +
                                "report of type Protein Science(L)"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "2541133", HttpStatus.SC_BAD_REQUEST,
                        "Tumor Immunology", "0403", "PACT443C",
                        "20-565_0403_PACT443C_Tumor Immunology.pdf", null, "20-565",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, null, "This TI test conclusion.",
                        "User svc-study-report-approval@pactpharma.com does not have" +
                                " permission to save report of type Tumor Immunology"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "3297225", HttpStatus.SC_BAD_REQUEST,
                        "imPACT", "0403", "PACT443C",
                        "20-457_0403_PACT443C_imPACT.pdf", null, "20-457",
                        "In Progress", null, null,
                        null, null, null, null, null, null, null, null,
                        null, "100.00", null, "This test conclusion.",
                        "User svc-study-report-approval@pactpharma.com does not " +
                                "have permission to save report of type imPACT"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "2677093", HttpStatus.SC_BAD_REQUEST,
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
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "27651", HttpStatus.SC_BAD_REQUEST,
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
                          int expectedResponseCode,
                          String reportType, String patientNum, String patient,
                          String reportName, String documentNames, String studyId, String status,
                          String[] fileAttachmentName, String compactReportHandOffDate,
                          String tumorFusionDetectedComment, String lowExpressedNsmComment,
                          String lowTcByNgsPctComment, String recommendation, String amendments,
                          String cancerType, String tumorType, String tumorLocation, String expId,
                          String tCellNonConfidentCount, String[] lscSelectedSamples, String conclusion, String expectedMessage) throws Exception {
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);

        JSONObject requestObjectJSON = constructReportReportsBody(conclusion, fileAttachmentName, compactReportHandOffDate,
                tumorFusionDetectedComment, lowExpressedNsmComment, lowTcByNgsPctComment, recommendation, amendments,
                cancerType, tumorType, tumorLocation, expId, tCellNonConfidentCount, lscSelectedSamples);
        System.out.println("Body: " + requestObjectJSON.toJSONString());
        System.out.println("Request:" + String.format(POST_REPORT_REPORTS_SAVE, studyReportId));
        httpRequest.body(requestObjectJSON.toJSONString());

        Response response = httpRequest.request(Method.POST, String.format(POST_REPORT_REPORTS_SAVE, studyReportId));
        Assert.assertEquals(String.format("Response code should be %s", expectedResponseCode),
                expectedResponseCode, response.getStatusCode());


        switch(expectedResponseCode) {
            case HttpStatus.SC_OK:
                Assert.assertTrue(String.format("Request PUT %s should print '%s'",
                        String.format(POST_REPORT_REPORTS_SAVE, studyReportId), expectedMessage),
                        expectedMessage.equalsIgnoreCase(removeNewLine(removeTags(response.body().asPrettyString()))));
                validateReport(httpRequest, userName, userPassword, studyReportId, reportType,
                        patientNum, patient, reportName, documentNames, studyId, status,
                        fileAttachmentName, compactReportHandOffDate,
                    tumorFusionDetectedComment, lowExpressedNsmComment,
                    lowTcByNgsPctComment, recommendation, amendments,
                    cancerType, tumorType, tumorLocation, expId,
                    tCellNonConfidentCount, lscSelectedSamples, conclusion);
                break;
            case HttpStatus.SC_BAD_REQUEST:
                Assert.assertEquals(String.format("Error message should be %s", expectedMessage),
                        expectedMessage, response.jsonPath().get(MESSAGE));
                break;
        }
    }

        /* UPDATE report_dev.study_report SET status="In Progress" where id=27651;
           UPDATE report_dev.study_report SET status="In Progress" where id=2677093;
           UPDATE report_dev.study_report SET status="In Progress" where id=2541133;
           UPDATE report_dev.study_report SET status="In Progress" where id=42993;
           UPDATE report_dev.study_report SET status="In Progress" where id=2542290;
           UPDATE report_dev.study_report SET status="In Progress" where id=42993;
           UPDATE report_dev.study_report SET status="In Progress" where id=2541133;
           UPDATE report_dev.study_report SET status="In Progress" where id=3297225;
           UPDATE report_dev.study_report SET status="In Progress" where id=267709;
           UPDATE report_dev.study_report SET status="In Progress" where id=2517281;
           UPDATE report_dev.study_report SET status="In Progress" where id=2641522;
           UPDATE report_dev.study_report SET status="In Progress" where id=2739606;
           UPDATE report_dev.study_report SET status="In Progress" where id=34183;
           UPDATE report_dev.study_report SET status="In Progress" where id=42963;
           "*/
    @DataProvider(name = "putReportReportsSubmitDataProvider")
    public Object[][] putReportReportsSubmitDataProvider() {
        return new Object[][]{
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "27651", HttpStatus.SC_OK,
                        "Protein Science(S)", "0027", "PACT407C",
                        "20-332_20001201_0027_PACT407C_Protein Science(S).pdf", null, "20-332",
                        "Pending", null, "04/Dec/2020", null, null, null, null, null,
                        null, null, null, "20001201", null, null, "This test conclusion.",
                        "  Report submitted successfully."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "2677093", HttpStatus.SC_OK,
                        "Bioinformatics", "0412", "PACT493C",
                        "21-063_0412_PACT493C_Bioinformatics.pdf", null, "21-063",
                        "Pending", null, "04/Dec/2020",
                        "This is tumor submit fusion Detected Comment",
                        "This is submit Low Expressed Nsm Comment",
                        "This is submit Low Tc By Ngs Pct Comment",
                        "This is submit test recommendation", "This is submit test amendments",
                        "Melanoma", "Premalignant", "legs",
                        null, null, null, "This test Bioinformatics conclusion.",
                        "  Report submitted successfully."},
               /* {CREATOR_USER_NAME, CREATOR_PASSWORD, "3297225", 200,
                        "imPACT", "0403", "PACT443C",
                        "20-457_0403_PACT443C_imPACT.pdf", null, "20-457",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, "100.00", null, "This submit imPact test conclusion.", "  Report submitted successfully."},*/
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "2541133", HttpStatus.SC_OK,
                        "Tumor Immunology", "0403", "PACT443C",
                        "20-565_0403_PACT443C_Tumor Immunology.pdf", null, "20-565",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, null, "This submit TI test conclusion.",  "  Report submitted successfully."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "42993", HttpStatus.SC_OK,
                        "Protein Science(L)", "0403", "PACT443C",
                        "20-545_20002003_0403_PACT443C_Protein Science(L).pdf", "42993_ge-0030.pdf", "20-545",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, M02_LSC_SELECTED_SAMPLES, "This is test Protein Science(L) conclusion.",
                        "  Report submitted successfully."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "2542290", HttpStatus.SC_OK,
                        "Gene Editing", "0504", "PACT326C",
                        "20-393_0504_PACT326C_Gene Editing.pdf", null, "20-393",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, null, "This is test GE conclusion.", "  Report submitted successfully."},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "2517281", HttpStatus.SC_BAD_REQUEST,
                        "Gene Editing", "0504", "PACT326C",
                        "20-393_0504_PACT326C_Gene Editing.pdf", null, "20-393",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, null, "This is test GE conclusion.",
                        "User svc-study-report-approval@pactpharma.com does " +
                                "not have permission to submit report of type Gene Editing"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "42963", HttpStatus.SC_BAD_REQUEST,
                        "Protein Science(L)", "0403", "PACT443C",
                        "20-545_20002003_0403_PACT443C_Protein Science(L).pdf", null, "20-545",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, M02_LSC_SELECTED_SAMPLES, "This is test Protein Science(L) conclusion.",
                        "User svc-study-report-approval@pactpharma.com does not have permission to submit " +
                                "report of type Protein Science(L)"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "2739606", HttpStatus.SC_BAD_REQUEST,
                        "Tumor Immunology", "0403", "PACT443C",
                        "20-565_0403_PACT443C_Tumor Immunology.pdf", null, "20-565",
                        "Pending", null, null,
                        null, null, null, null, null, null, null, null,
                        null, null, null, "This TI test conclusion.",
                        "User svc-study-report-approval@pactpharma.com does not have" +
                                " permission to submit report of type Tumor Immunology"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "3297225", HttpStatus.SC_BAD_REQUEST,
                        "imPACT", "0403", "PACT443C",
                        "20-457_0403_PACT443C_imPACT.pdf", null, "20-457",
                        "In Progress", null, null,
                        null, null, null, null, null, null, null, null,
                        null, "100.00", null, "This test conclusion.",
                        "User svc-study-report-approval@pactpharma.com does not " +
                                "have permission to submit report of type imPACT"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "2641522", HttpStatus.SC_BAD_REQUEST,
                        "Bioinformatics", "0412", "PACT493C",
                        "21-063_0412_PACT493C_Bioinformatics.pdf", null, "21-063",
                        "Pending", null, "04/Dec/2020",
                        "This is tumor fusion Detected Comment",
                        "This is Low Expressed Nsm Comment", "Low Tc By Ngs Pct Comment",
                        "This is test recommendation", "This is test amendments",
                        "Melanoma", "Premalignant", "legs",
                        null, null, null, "This test conclusion.",
                        "User svc-study-report-approval@pactpharma.com does not have " +
                                "permission to submit report of type Bioinformatics"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "34183", HttpStatus.SC_BAD_REQUEST,
                        "Protein Science(S)", "0027", "PACT407C",
                        "20-332_20001201_0027_PACT407C_Protein Science(S).pdf", null, "20-332",
                        "Pending", null, "04/Dec/2020", null, null, null, null, null,
                        null, null, null, "20001201", null, null, "This test conclusion.",
                        "User svc-study-report-approval@pactpharma.com does not have permission " +
                                "to submit report of type Protein Science(S)"}
        };
    }

    @Test(dataProvider = "putReportReportsSubmitDataProvider", enabled = isTestEnabled)
    void putReportReportsSubmit(String userName, String userPassword, String studyReportId,
                                int expectedResponseCode,
                                String reportType, String patientNum, String patient,
                                String reportName, String documentNames, String studyId, String status,
                                String[] fileAttachmentName, String compactReportHandOffDate,
                                String tumorFusionDetectedComment, String lowExpressedNsmComment,
                                String lowTcByNgsPctComment, String recommendation, String amendments,
                                String cancerType, String tumorType, String tumorLocation, String expId,
                                String tCellNonConfidentCount, String[] lscSelectedSamples, String conclusion,
                                String expectedMessage) throws Exception {
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);

        JSONObject requestObjectJSON = constructReportReportsBody(conclusion, fileAttachmentName, compactReportHandOffDate,
                tumorFusionDetectedComment, lowExpressedNsmComment, lowTcByNgsPctComment, recommendation, amendments,
                cancerType, tumorType, tumorLocation, expId, tCellNonConfidentCount, lscSelectedSamples);
        System.out.println("Body: " + requestObjectJSON.toJSONString());
        System.out.println("Request:" + String.format(PUT_REPORT_REPORTS_SUBMIT, studyReportId));
        httpRequest.body(requestObjectJSON.toJSONString());

        Response response = httpRequest.request(Method.PUT, String.format(PUT_REPORT_REPORTS_SUBMIT , studyReportId));
        Assert.assertEquals(String.format("Response code should be %s", expectedResponseCode),
                expectedResponseCode, response.getStatusCode());

        switch(expectedResponseCode) {
            case HttpStatus.SC_OK:
                Assert.assertTrue(String.format("Request PUT %s should print '%s'",
                        String.format(PUT_REPORT_REPORTS_SUBMIT, studyReportId), expectedMessage),
                        expectedMessage.equalsIgnoreCase(removeNewLine(removeTags(response.body().asPrettyString()))));
                validateReport(httpRequest, userName, userPassword, studyReportId, reportType,
                        patientNum, patient, reportName, documentNames, studyId, status,
                        fileAttachmentName, compactReportHandOffDate,
                        tumorFusionDetectedComment, lowExpressedNsmComment,
                        lowTcByNgsPctComment, recommendation, amendments,
                        cancerType, tumorType, tumorLocation, expId,
                        tCellNonConfidentCount, lscSelectedSamples, conclusion);
                break;
            case HttpStatus.SC_BAD_REQUEST:
                Assert.assertEquals(String.format("Error message should be %s", expectedMessage),
                        expectedMessage, response.jsonPath().get(MESSAGE));
                break;
        }
    }

    @DataProvider(name = "postReportReportsStatusDataProvider")
    public Object[][] postReportReportsStatusDataProvider() {
        return new Object[][]{
               {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "3119233", false, HttpStatus.SC_OK,
                        "Approved", null, "imPACT", "0037", "PACT506C",
                        "21-117_0037_PACT506C_imPACT.pdf", null, "21-117",
                        null, null, null, null, null, null, null, null, null, null,
                        null, "1.00", null, null, "  Report has been successfully approved."},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "3118773", false, HttpStatus.SC_OK,
                        "Reject", null, "Tumor Immunology", "0015", "PACT299C",
                        "20-085_0015_PACT299C_Tumor Immunology.pdf", null, "20-085",
                        null, null, null, null, null, null, null, null, null, null,
                        null, null, null, "This test conclusion.", "  Report has been successfully rejected."},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "2517325", false, HttpStatus.SC_OK,
                        "Reject", "0", "Gene Editing", "0512", "PACT463C",
                        "20-637_0512_PACT463C_Gene Editing.pdf", "2517325_sample.pdf", "20-637",
                        null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, "  Report has been successfully rejected."},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "2792633", false, HttpStatus.SC_OK,
                        "Reject", "1", "Bioinformatics", "0611", "PACT507C",
                        "21-107_0611_PACT507C_Bioinformatics.pdf", null, "21-107",
                        null, null, null, null, null, null, null,
                        "sdsda", "dsadadasasa", "adadasad",
                        null, null, null, null, "  Report has been successfully rejected."},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "29641", false, HttpStatus.SC_BAD_REQUEST,
                        "Reject", null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        "User svc-study-report-creator@pactpharma.com does not have permission to " +
                                "approve report of type Protein Science(S)"},
                {CREATOR_USER_NAME, CREATOR_PASSWORD, "29641", false, HttpStatus.SC_BAD_REQUEST,
                        "Approved", null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        "User svc-study-report-creator@pactpharma.com does not have permission to " +
                                "approve report of type Protein Science(S)"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "30738", true, HttpStatus.SC_BAD_REQUEST,
                        "Approved", null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        "Modification to approved report is disallowed!"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "30738", true, HttpStatus.SC_BAD_REQUEST,
                        "Approved", null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        "Modification to approved report is disallowed!"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "28374", true, HttpStatus.SC_BAD_REQUEST,
                        "Approved", null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        "Report has not been submitted yet for approval"},
                {APPROVAL_USER_NAME, APPROVAL_PASSWORD, "28374", true, HttpStatus.SC_BAD_REQUEST,
                        "Reject", null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        "Report has not been submitted yet for approval"}
        };
    }

    /*
    Run before test execution:
         UPDATE report_dev.study_report SET status="Pending" where id=3119233;
         UPDATE report_dev.study_report SET status="Pending" where id=3118773;
         UPDATE report_dev.study_report SET status="Pending" where id=2517325;
         UPDATE report_dev.study_report SET status="Pending" where id=2792633;
         UPDATE report_dev.study_report SET status="Pending" where id=29641;
         UPDATE report_dev.study_report SET status="Pending" where id=30738;
         UPDATE report_dev.study_report SET status="In Progress" where id=28374;

     */
    @Test(dataProvider = "postReportReportsStatusDataProvider", enabled = isTestEnabled)
    void postReportReportsStatus(String userName, String userPassword, String studyReportId, boolean repeat,
                                 int expectedResponseCode, String status, String failureReason,
                                 String reportType, String patientNum, String patient,
                                 String reportName, String documentNames, String studyId,
                                 String[] fileAttachmentName, String compactReportHandOffDate,
                                 String tumorFusionDetectedComment, String lowExpressedNsmComment,
                                 String lowTcByNgsPctComment, String recommendation, String amendments,
                                 String cancerType, String tumorType, String tumorLocation, String expId,
                                 String tCellNonConfidentCount, String[] lscSelectedSamples, String conclusion,
                                 String expectedMessage) throws Exception {
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);
        System.out.println("Request:" + String.format(POST_REPORT_REPORTS_STATUS, studyReportId, status));

        if(failureReason != null) {
            JSONObject requestObjectJSON = constructStatusRejectBody(failureReason);
            httpRequest.body(requestObjectJSON.toJSONString());
            System.out.println("Body: " + requestObjectJSON.toJSONString());
        }
        Response response = httpRequest.request(Method.POST,
                    String.format(POST_REPORT_REPORTS_STATUS, studyReportId, status));
        if(repeat) {
            response = httpRequest.request(Method.POST,
                    String.format(POST_REPORT_REPORTS_STATUS, studyReportId, status));
        }

        System.out.println("Message: " + (removeNewLine(removeTags(response.body().asPrettyString()))));

        Assert.assertEquals(String.format("Response code should be %s", expectedResponseCode),
                expectedResponseCode, response.getStatusCode());
        switch(expectedResponseCode) {
            case HttpStatus.SC_OK:
                Assert.assertTrue(String.format("Request POST %s should print '%s'",
                        String.format(POST_REPORT_REPORTS_STATUS, studyReportId, status), expectedMessage),
                        expectedMessage.equalsIgnoreCase(removeNewLine(removeTags(response.body().asPrettyString()))));
                validateReport(httpRequest, userName, userPassword, studyReportId, reportType,
                        patientNum, patient, reportName, documentNames, studyId, status,
                        fileAttachmentName, compactReportHandOffDate,
                        tumorFusionDetectedComment, lowExpressedNsmComment,
                        lowTcByNgsPctComment, recommendation, amendments,
                        cancerType, tumorType, tumorLocation, expId,
                        tCellNonConfidentCount, lscSelectedSamples, conclusion);
                break;
            case HttpStatus.SC_BAD_REQUEST:
                Assert.assertEquals(String.format("Error message should be %s", expectedMessage),
                        expectedMessage, response.jsonPath().get(MESSAGE));
                break;
        }
    }

    @DataProvider(name = "getPdfAllDataProvider")
    public Object[][] getPdfAllDataProvider() {
        return new Object[][]{
                {GET_PDF_ALL, null, HttpStatus.SC_OK, CREATOR_USER_NAME, CREATOR_PASSWORD,
                        "Pending,In Progress,Approved,Reject", "pending,progress,approved,reject", 1},
                {GET_PDF_ALL, "status[]=pending&page=1", HttpStatus.SC_OK, CREATOR_USER_NAME, CREATOR_PASSWORD, "Pending",
                        "pending", 1},
                {GET_PDF_ALL, "status[]=progress&page=2", HttpStatus.SC_OK, CREATOR_USER_NAME, CREATOR_PASSWORD,
                        "In Progress", "progress", 2},
                {GET_PDF_ALL, "status[]=approved&page=1", HttpStatus.SC_OK, CREATOR_USER_NAME, CREATOR_PASSWORD,
                        "Approved", "approved", 1},
                {GET_PDF_ALL, "status[]=reject&page=1", HttpStatus.SC_OK, CREATOR_USER_NAME, CREATOR_PASSWORD, "Reject",
                        "reject", 1},
                {GET_PDF_ALL, "status[]=approved&status[]=reject&page=1", HttpStatus.SC_OK, CREATOR_USER_NAME,
                        CREATOR_PASSWORD, "Approved,Reject", "approved,reject", 1},
                {GET_PDF_ALL, "status[]=approved&status[]=reject&status[]=progress&page=3", HttpStatus.SC_OK,
                        CREATOR_USER_NAME, CREATOR_PASSWORD, "Approved,Reject,In Progress", "approved,reject,progress", 3},
                {GET_PDF_ALL, "status[]=approved&status[]=reject&status[]=pending", HttpStatus.SC_OK,
                        CREATOR_USER_NAME, CREATOR_PASSWORD, "Approved,Reject,Pending", "approved,reject,pending", 1},
                {GET_PDF_ALL, "page=4", HttpStatus.SC_OK, CREATOR_USER_NAME, CREATOR_PASSWORD,
                        "Pending,In Progress,Approved,Reject", "pending,progress,approved,reject", 4},
                {GET_PDF_ALL, null, HttpStatus.SC_OK, APPROVAL_USER_NAME, APPROVAL_PASSWORD,
                        "Pending,In Progress,Approved,Reject", "pending,progress,approved,reject", 1},
                {GET_PDF_ALL, "status[]=INCORRECT&page=2", HttpStatus.SC_OK, CREATOR_USER_NAME, CREATOR_PASSWORD,
                        "Pending,In Progress,Approved,Reject", "pending,progress,approved,reject", 2}
        };
    }

    @Test(dataProvider = "getPdfAllDataProvider",enabled = false)
    public void getPdfAll(String baseUrl, String parameters, int expectedResponseCode, String userName, String userPassword,
                          String expectedStatus, String expectedStatusQuery, int currentPage ) {
        RequestSpecification httpRequest =
                TestUtilities.generateRequestSpecification(userName, userPassword);
        String url = baseUrl;
        if(parameters != null) {
            url = String.format("%s?%s", baseUrl, parameters);
        }
        Response response = httpRequest.request(Method.GET, url);
        Assert.assertEquals(String.format("Response code should be %s", expectedResponseCode),
                expectedResponseCode, response.getStatusCode());
        Assert.assertTrue(String.format("GET %s response should contains records with statuses %s, " +
                        "current page %s and expected status query %s", url, expectedStatus,
                currentPage,expectedStatusQuery),
                validateGetPdfAllResponse(response,expectedStatus, expectedStatusQuery,currentPage));
    }

    @DataProvider(name = "getPdfSearchPatientDataProvider")
    public Object[][] getPdfSearchPatientDataProvider() {
        return new Object[][]{
                {GET_PDF_SEARCH_PATIENT, HttpStatus.SC_OK, "332C", CREATOR_USER_NAME, CREATOR_PASSWORD,
                        "src/test/resources/files/getPdfSearchPatientWithOneReport.json"},
                {GET_PDF_SEARCH_PATIENT, HttpStatus.SC_OK, "255C", CREATOR_USER_NAME, CREATOR_PASSWORD,
                        "src/test/resources/files/getPdfSearchPatientWithSeveralReport.json"},
                {GET_PDF_SEARCH_PATIENT, HttpStatus.SC_OK, "332C", APPROVAL_USER_NAME, APPROVAL_PASSWORD,
                        "src/test/resources/files/getPdfSearchPatientWithOneReport.json"},
                {GET_PDF_SEARCH_PATIENT, HttpStatus.SC_OK, "255C", APPROVAL_USER_NAME, APPROVAL_PASSWORD,
                        "src/test/resources/files/getPdfSearchPatientWithApprovalRole.json"}
        };
    }

    /*
    UPDATE report_dev.study_report SET status="Pending" where id=29279;
    UPDATE report_dev.study_report SET status="Pending" where id=28560;
     */
    @Test(dataProvider = "getPdfSearchPatientDataProvider", enabled = isTestEnabled)
    public void getPdfSearchPatient(String url, int expectedResponseCode, String patientId, String userName,
                                    String userPassword, String expectedResponseFile) throws Exception {
        executeUrlAndValidateJsonResponse(Method.GET, String.format(url, patientId), expectedResponseCode, userName,
                userPassword, expectedResponseFile);
    }

    @DataProvider(name = "getPdfPatientDataProvider")
    public Object[][] getPdfPatientPatientDataProvider() {
        return new Object[][]{
                {GET_PDF_PATIENT, HttpStatus.SC_OK, CREATOR_USER_NAME, CREATOR_PASSWORD,
                        "src/test/resources/files/getPdfPatient.json"},
                {GET_PDF_PATIENT, HttpStatus.SC_OK, APPROVAL_USER_NAME, APPROVAL_PASSWORD,
                        "src/test/resources/files/getPdfPatient.json"}
        };
     }
    @Test(dataProvider = "getPdfPatientDataProvider", enabled = isTestEnabled)
    public void getPdfPatient(String url, int expectedResponseCode, String userName,
                              String userPassword, String expectedResponseFile) throws Exception{
        executeUrlAndValidateJsonResponse(Method.GET, url, expectedResponseCode, userName,
                userPassword, expectedResponseFile);
    }

    @DataProvider(name = "postUploadReportsDocumentsDataProvider")
    public Object[][] postUploadReportsDocumentsDataProvider() {
        return new Object[][]{
                {POST_UPLOAD_REPORTS_DOCUMENTS, "32272", CREATOR_USER_NAME, CREATOR_PASSWORD,
                        HttpStatus.SC_OK, "dummy.txt", "dummy.txt", null, "32272_dummy.txt"},
                {POST_UPLOAD_REPORTS_DOCUMENTS, "32272", CREATOR_USER_NAME, CREATOR_PASSWORD,
                        HttpStatus.SC_BAD_REQUEST, null, null, "No files received", null},
                {POST_UPLOAD_REPORTS_DOCUMENTS, "32272", CREATOR_USER_NAME, CREATOR_PASSWORD,
                        HttpStatus.SC_OK, "dummy.txt,dummy1.txt,dummy2.txt", "dummy.txt,dummy1.txt,dummy2.txt", null,
                        "32272_dummy.txt,32272_dummy1.txt,32272_dummy2.txt"},
                {POST_UPLOAD_REPORTS_DOCUMENTS, "32272", APPROVAL_USER_NAME, APPROVAL_PASSWORD,
                        HttpStatus.SC_BAD_REQUEST, "dummy.txt", "dummy.txt", "User svc-study-report-approval@pactpharma.com " +
                        "does not have permission to upload supporting document of type Protein Science(S)",
                        null}
        };
    }

    @UseAsTestRailId(testRailId = 2217)
    @Test(dataProvider = "postUploadReportsDocumentsDataProvider", enabled = isTestEnabled)
    public void postUploadReportsDocuments(String url, String studyReportId, String userName, String userPassword,
                                           int expectedResponseCode, String filesToUpload, String expectedFiles,
                                           String expectedMessage, String expectedFilesFromGetRequest) {
        RequestSpecification httpRequest =
                TestUtilities.generateRequestSpecification(userName, userPassword);
        String[] fileNamesArray = null;
        if(filesToUpload != null) {
            httpRequest.header(CONTENT_TYPE, "multipart/form-data");
            fileNamesArray = filesToUpload.split(",");
            for (String fileName : fileNamesArray) {
                File fileToUpload = new File("src/test/resources/files/" + fileName);
                httpRequest.multiPart(FILE, fileToUpload);
            }
        }
        Response response = httpRequest.request(Method.POST, String.format(url, studyReportId));
        Assert.assertEquals(String.format("Response code should be %s", expectedResponseCode),
                expectedResponseCode, response.getStatusCode());
        switch(expectedResponseCode) {
            case HttpStatus.SC_OK:
                List<Object> files = response.getBody().jsonPath().getList(".");
                Set<String> actualFileSet = new HashSet<>();
                Set<String> expectedFileSet = new HashSet<>();
                files.stream().forEach(obj -> actualFileSet.add((String) obj));
                expectedFileSet.addAll(Arrays.asList(expectedFiles.split(",")));
                Assert.assertTrue(String.format("POST %s response should contain %s files",
                            String.format(url, studyReportId), expectedFiles), expectedFileSet.equals(actualFileSet));
                putSaveAndSubmitReport(userName, userPassword, studyReportId, fileNamesArray);
                response = httpRequest.request(Method.GET, String.format(GET_PDF_REPORT, studyReportId));
                validateKeyValueArrayFromResponse(response, "study.document_name", expectedFilesFromGetRequest);
                executePutUploadReportsDocument(userName, userPassword,
                        studyReportId, expectedFilesFromGetRequest.split(","), HttpStatus.SC_OK,
                        "  File(s) has been deleted");
                validateKeyValueArrayFromResponse(response, "study.document_name", expectedFilesFromGetRequest);
                break;
            case HttpStatus.SC_BAD_REQUEST:
                Assert.assertEquals(String.format("Error message should be %s", expectedMessage),
                        expectedMessage, response.jsonPath().get(MESSAGE));
                break;
        }
    }

    @DataProvider(name = "putUploadReportsDocumentsDataProvider")
    public Object[][] putUploadReportsDocumentsDataProvider() {
        return new Object[][]{
                {"32272", CREATOR_USER_NAME, CREATOR_PASSWORD,
                        HttpStatus.SC_OK, "dymmy.txt", "  File(s) has been deleted"},
                {"32272", APPROVAL_USER_NAME, APPROVAL_PASSWORD,
                        HttpStatus.SC_UNPROCESSABLE_ENTITY, "32272_dymmy.txt", "User svc-study-report-approval@pactpharma.com does not have permission " +
                        "to upload supporting document of type Protein Science(S)"},
                {"32272", CREATOR_USER_NAME, CREATOR_PASSWORD,
                        HttpStatus.SC_UNPROCESSABLE_ENTITY, null, "\"body\" must be an array"}
        };
    }

    @UseAsTestRailId(testRailId = 2216)
    @Test(dataProvider = "putUploadReportsDocumentsDataProvider", enabled = isTestEnabled)
    public void putUploadReportsDocuments(String studyReportId, String userName, String userPassword,
                                           int expectedResponseCode, String filesToUpload, String expectedMessage) {
        switch(expectedResponseCode) {
            case HttpStatus.SC_OK:
                executePutUploadReportsDocument(userName, userPassword,
                        studyReportId, filesToUpload.split(","), HttpStatus.SC_OK,
                        expectedMessage);
                break;
            case HttpStatus.SC_BAD_REQUEST:
                executePutUploadReportsDocument(userName, userPassword,
                        studyReportId, null, expectedResponseCode,
                        expectedMessage);
                break;
        }
    }
    /**
     * This method builds Request Body and executes PUT /api/v1/upload/reports/{id}/documents.
     * Body should contains array of files to delete.
     * @param userName - user name to generate token
     * @param userPassword - password to generate token
     * @param studyReportId - study report id
     * @param fileNamesArray - array of files to delete
     * @param expectedResponseStatusCode - expected response status code
     * @param expectedMessage - expected messages
     */
    private void executePutUploadReportsDocument(String userName, String userPassword,
                                                 String studyReportId, String[] fileNamesArray, int expectedResponseStatusCode,
                                                 String expectedMessage) {
        RequestSpecification httpRequest =
                TestUtilities.generateRequestSpecification(userName, userPassword);
        JSONArray fileArray = new JSONArray();
        if(fileNamesArray != null) {
            Arrays.stream(fileNamesArray).forEach(s -> fileArray.add(s));
            httpRequest.body(fileArray.toJSONString());
        }
        Response response = httpRequest.request(Method.PUT, String.format(PUT_UPLOAD_REPORTS_DOCUMENTS, studyReportId));
        Assert.assertEquals(String.format("PUT %s status code should be %s",
                String.format(PUT_UPLOAD_REPORTS_DOCUMENTS, studyReportId), expectedResponseStatusCode),
                expectedResponseStatusCode, response.statusCode());
        if(expectedResponseStatusCode == HttpStatus.SC_OK) {
            Assert.assertTrue(String.format("Request PUT %s should print '%s'",
                    String.format(PUT_UPLOAD_REPORTS_DOCUMENTS, studyReportId), expectedMessage),
                    expectedMessage.equalsIgnoreCase(removeNewLine(removeTags(response.body().asPrettyString()))));
        } else {
            Assert.assertTrue(String.format("Request PUT %s should print '%s'",
                    String.format(PUT_UPLOAD_REPORTS_DOCUMENTS, studyReportId), expectedMessage),
                    expectedMessage.equalsIgnoreCase(response.jsonPath().get(MESSAGE)));
        }
    }

    /**
     * This method constructs request body and executes PUT /api/v1/report/reports/{id}, POST /api/v1/report/reports/{id}/save and
     * Body should contains array of File Attachment Names.
     * @param userName - user name
     * @param userPassword - user password
     * @param studyReportId - study report Id
     * @param fileNamesArray - file name
     */
    private void putSaveAndSubmitReport(String userName, String userPassword, String studyReportId, String[] fileNamesArray) {
        JSONObject requestObjectJSON = constructReportReportsBody(null, fileNamesArray, null,
                null, null, null, null, null,
                null, null, null, null, null, null);
        RequestSpecification httpRequest =
                TestUtilities.generateRequestSpecification(userName, userPassword);
        Assert.assertEquals(String.format("API PUT %s should return status code %s",
                String.format(PUT_REPORT_REPORTS, studyReportId), HttpStatus.SC_OK), HttpStatus.SC_OK,
                httpRequest.body(requestObjectJSON.toJSONString())
                .request(Method.PUT,String.format(PUT_REPORT_REPORTS, studyReportId)).getStatusCode());
        Assert.assertEquals(String.format("API POST %s should return status code %s",
                String.format(POST_REPORT_REPORTS_SAVE, studyReportId), HttpStatus.SC_OK),
                HttpStatus.SC_OK, httpRequest.body(requestObjectJSON.toJSONString())
                .request(Method.POST,String.format(POST_REPORT_REPORTS_SAVE, studyReportId)).getStatusCode());
        Assert.assertEquals(String.format("API PUT %s should return status code %s",
                String.format(PUT_REPORT_REPORTS_SUBMIT, studyReportId), HttpStatus.SC_OK),
                HttpStatus.SC_OK, httpRequest.body(requestObjectJSON.toJSONString())
                .request(Method.PUT,String.format(PUT_REPORT_REPORTS_SUBMIT, studyReportId)).getStatusCode());
    }


    /**
     * This method executes HTTP method and compares results with expected JSON file.
     * @param method
     * @param url
     * @param expectedResponseCode
     * @param userName
     * @param userPassword
     * @param expectedResponseFile
     * @throws Exception
     */
    private void executeUrlAndValidateJsonResponse(Method method, String url, int expectedResponseCode, String userName,
                                                   String userPassword, String expectedResponseFile) throws Exception{
        RequestSpecification httpRequest =
                TestUtilities.generateRequestSpecification(userName, userPassword);
        Response response = httpRequest.request(method, url);
        Assert.assertEquals(String.format("Response code should be %s", expectedResponseCode),
                expectedResponseCode, response.getStatusCode());
        String expectedResponse = TestUtilities.readJsonFile(expectedResponseFile);
        JSONAssert.assertEquals(String.format("API:%s %s\nResponse should be %s", method,
                url, expectedResponse), expectedResponse, response.asPrettyString(), false);
    }

    /**
     * This method validates GET PDF ALL Response
     * @param response - response
     * @param expectedStatus - expected status
     * @param expectedStatusQuery - expected status query
     * @param expectedCurrentPage - expected current page
     * @return
     */
    private boolean validateGetPdfAllResponse(Response response, String expectedStatus, String expectedStatusQuery,
                                              int expectedCurrentPage ) {
        Set<String> expectedStatusSet = new HashSet<String>(Arrays.asList(expectedStatus.split(",")));
        Set<String> expectedStatusQuerySet = new HashSet<String>(Arrays.asList(expectedStatusQuery.split(",")));
        ArrayList<JSONObject> resultArray = (ArrayList<JSONObject>)response.jsonPath().get(RESULT);
        int size = resultArray.size();

        for(int i=0; i<size; i++) {
            Map<String, String> result = resultArray.get(i);
            if(!expectedStatus.contains(result.get(STATUS))) {
                System.out.println
                        (String.format("Respond should contain % statuses. However, it contains %s status.", expectedStatus,
                                result.get(STATUS).toString()));
                return false;
            }
        }
        if(response.body().jsonPath().getInt(CURRENT_PAGE)!= expectedCurrentPage) {
            System.out.println(String.format("Current page should be %s", expectedCurrentPage));
            return false;
        }
        List<String> statusQueryList = response.body().jsonPath().getList(STATUS_QUERY);
        Set<String> statusQuerySet = new HashSet<String>(statusQueryList);
        if(!expectedStatusQuerySet.equals(statusQuerySet)) {
            System.out.print(String.format("Status query should contain %s", expectedStatusQuery));
            return false;
        }
        return true;
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
                                String[] fileAttachmentName, String compactReportHandOffDate,
                                String tumorFusionDetectedComment, String lowExpressedNsmComment,
                                String lowTcByNgsPctComment, String recommendation, String amendments,
                                String cancerType, String tumorType, String tumorLocation, String expId,
                                String tCellNonConfidentCount, String[] lscSelectedSamples, String conclusion) {

        Response response = httpRequest.request(Method.GET, String.format(GET_PDF_REPORT, studyReportId));
        validateValueFromResponse(response,"study.id", studyReportId);
        validateValueFromResponse(response,"study.studyId", studyId);
        validateValueFromResponse(response,"study.status", status);
        validateValueFromResponse(response,"study.patient", patient);
        validateValueFromResponse(response,"study.patient_num", patientNum);
        validateValueFromResponse(response,"study.report_type", reportType);
        validateValueFromResponse(response,"study.conclusion", conclusion);
        validateKeyValueArrayFromResponse(response, "study.document_name", documentNames );
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
                validateValueFromResponse(response, "reportDetails.tumor_type", tumorType);
                validateValueFromResponse(response, "reportDetails.cancer_type", cancerType);
                validateValueFromResponse(response, "reportDetails.tumor_location", tumorLocation);

                break;
            case "imPACT":
                if(tCellNonConfidentCount == null) {
                    tCellNonConfidentCount = "0.00";
                }
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
     * @param fileAttachmentNames
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
    private JSONObject constructReportReportsBody(String conclusion, String[] fileAttachmentNames, String compactReportHandOffDate,
                                                     String tumorFusionDetectedComment, String lowExpressedNsmComment,
                                                     String lowTcByNgsPctComment, String recommendation, String amendments,
                                                     String cancerType, String tumorType, String tumorLocation, String expId,
                                                     String tCellNonConfidentCount, String[] lscSelectedSamples) {
        JSONObject requestParams = new JSONObject();
        requestParams = addBodyParameter(requestParams, CONCLUSION, conclusion);
        requestParams = addBodyArray(requestParams, FILE_ATTACHMENT_NAME, fileAttachmentNames);
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

    /**
     * This method constructs request body for POST /report/reports/{id}/status/{status}
     * @param rejectReason - reject reason
     * @return  - request body JSON File
     */
    private JSONObject constructStatusRejectBody(String rejectReason) {
        JSONObject requestParams = new JSONObject();
        requestParams = addBodyParameter(requestParams, "reason", rejectReason);
        return requestParams;
    }

    /**
     * This method constructs /api/v1/pdf/search/reports?report_type=%s&patient_id=%s
     * @param reportType
     * @param patientId
     * @param experimentId
     * @param impactSampleName
     * @param sampleName
     * @param studyId
     * @param hgxIdentifier
     * @return
     */
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
