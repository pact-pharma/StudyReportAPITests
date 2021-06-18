package com.pactpharma.sr;

public class TestConstants {
    public static String TOKEN_BASE_URI = "https://login.microsoftonline.com/7e9e134e-8b39-4bc7-93f1-ac46a728032f/oauth2/v2.0/token";
    public static String BASE_URI = "https://study-report.zest.pactpharma.com";
    public static String ID_TOKEN = "id_token";
    public static String CONTENT_TYPE = "Content-Type";
    public static String ACCEPT_ENCODING = "Accept-Encoding";
    public static String CLIENT_ID = "client_id";
    public static String CLIENT_SECRET = "client_secret";
    public static String RESOURCE = "resource";
    public static String GRANT_TYPE = "grant_type";
    public static String AUTHORIZATION = "Authorization";
    public static String USER_NAME = "username";
    public static String PASSWORD = "password";
    public static String SCOPE = "scope";
    public static String CREATOR_USER_NAME = "svc-study-report-creator@pactpharma.com";
    public static String CREATOR_PASSWORD = "GRv0n0t!7hdL";
    public static String APPROVAL_USER_NAME = "svc-study-report-approval@pactpharma.com";
    public static String APPROVAL_PASSWORD = "k#2REA3pp42R";
    public static String GET_FETCH_DOCS_URI = "/api/v1/report/reports/%s/fetchdocs";
    public static String GET_FETCH_IN_WORD_FORMAT_URI = "/api/v1/report/reports/%s/fetchinwordformat";
    public static String GET_PDF_SEARCH_REPORT = "/api/v1/pdf/search/reports?report_type=%s&patient_id=%s";
    public static String PUT_REPORT_REPORTS = "/api/v1/report/reports/%s";
    public static String GZIP ="gzip";
    public static String URI = "uri";
    public static String MESSAGE = "message";
    public static String PDF = "pdf";
    public static String[] LSC_SELECTED_SAMPLES = {"MO1", "MO2"};
    public static String[] NOT_EXISTING_FIlE_ATTACHMENT_NAME= {"XXXX.pdf"};

}
