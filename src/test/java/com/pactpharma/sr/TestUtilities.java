package com.pactpharma.sr;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import static com.pactpharma.sr.TestConstants.*;
import io.restassured.http.ContentType;


public class TestUtilities {

    /**
     * This methods generates token for given user Name and password.
     * @param userName
     * @param password
     * @return
     */
    public static String getToken(String userName, String password) {
        RestAssured.baseURI = TOKEN_BASE_URI;
        RequestSpecification httpRequest = RestAssured.given();
        httpRequest.header(CONTENT_TYPE, "multipart/form-data");
        httpRequest.multiPart(CLIENT_SECRET, "lBw1Bl-az10uFSis6gn2-j.UHy-mXY__ew");
        httpRequest.multiPart(GRANT_TYPE, "password");
        httpRequest.multiPart(CLIENT_ID, "08c5a6fe-ad37-4a30-9bc7-344908a00f88");
        httpRequest.multiPart(USER_NAME, userName);
        httpRequest.multiPart(PASSWORD, password);
        httpRequest.multiPart(SCOPE, "openid profile");

        Response response = httpRequest.request(Method.POST);
        int statusCode = response.getStatusCode();
        Assert.assertEquals(200, statusCode);

        return response.jsonPath().get(ID_TOKEN);
    }

    /**
     * This method reads JSON File as String
     * @param jsonFile
     * @return
     * @throws Exception
     */
    public static String readJsonFile(String jsonFile) throws Exception{
        String json = readFileAsString(jsonFile);
        return json;
    }

    /**
     * This method reads file as string.
     * @param file - file name
     * @return
     * @throws Exception
     */
    public static String readFileAsString(String file)throws Exception
    {
        return new String(Files.readAllBytes(Paths.get(file)));
    }

    /**
     * This methods generates request specification.
     * @param userName - user name to generate token
     * @param userPassword - password to generate token
     * @return
     */
    public static RequestSpecification generateRequestSpecification(String userName, String userPassword) {
        RestAssured.baseURI = BASE_URI;
        RequestSpecification httpRequest = RestAssured.given();
        String token = TestUtilities.getToken(userName, userPassword);
        httpRequest.header(AUTHORIZATION, String.format("Bearer %s", token));
        httpRequest.header("Content-Type", "application/json");
        httpRequest.header("Accept", "application/json");
        httpRequest.contentType(ContentType.JSON);

        return httpRequest;
    }

    /**
     * This method un-compresses response and write it to PDF file
     * @param response
     * @param generatedPath
     * @throws Exception
     */
    public static void readResponseInPdf(Response response, String generatedPath) throws Exception {
        String directoryName = generatedPath.substring(0, generatedPath.lastIndexOf('/')+1);
        File directory = new File(directoryName);
        if(!directory.exists()) {
            directory.mkdir();
        }
        File testFile = new File(generatedPath);
        FileOutputStream out = new FileOutputStream(testFile);
        GZIPInputStream gis = new GZIPInputStream(response.getBody().asInputStream());
        byte[] buffer = new byte[1024];
        int len;
        while((len = gis.read(buffer)) != -1){
            out.write(buffer, 0, len);
        }
        out.close();
    }


    /**
     * This method constructs partial URL string and appends it to String Builder
     * @param strBuilderUrl - String builder
     * @param stringToSubstitute - string to substitute
     * @param partOfUrl - partial URL (could contains %s)
     * @return
     */
    public static StringBuilder constructPartOfUrl(StringBuilder strBuilderUrl, String stringToSubstitute, String partOfUrl) {
        if(!(stringToSubstitute == null || stringToSubstitute.isEmpty() || stringToSubstitute.isBlank())){
            strBuilderUrl.append(String.format(partOfUrl, stringToSubstitute));
        }
        return strBuilderUrl;
    }

    /**
     * This method converts base64 String to PDF file
     * @param newPdfFile - fileName
     * @param base64String - base64 String
     * @throws Exception
     */
    public static void convertBase64ToPdfFile(String newPdfFile, String base64String) throws Exception {
        FileOutputStream out = new FileOutputStream(new File(newPdfFile));
        byte[] decoder = Base64.getDecoder().decode(base64String);
        out.write(decoder);
        out.close();
    }

    /**
     * This method adds new parameter for JSON Object
     * @param jsonObject - JSON object
     * @param parameterName - parameter name
     * @param parameterValue - parameter value
     * @return
     */
    public static JSONObject addBodyParameter(JSONObject jsonObject, String parameterName, String parameterValue) {
        if(parameterValue!=null) {
            jsonObject.put(parameterName, parameterValue);
        }
        return jsonObject;
    }

    /**
     * This method adds JSONArray to json object
     * @param jsonObject - JSON Object
     * @param arrayName - array Name
     * @param parameterValues - JSON array need to be constructed from this String array
     * @return
     */
    public static JSONObject addBodyArray(JSONObject jsonObject, String arrayName, String[] parameterValues) {

        if(parameterValues != null) {
            JSONArray array = new JSONArray();
            for (String parameterValue : parameterValues) {
                array.add(parameterValue);
            }
            jsonObject.put(arrayName, array);
        }
        return jsonObject;
    }

    public static String removeTags(String string) {
        Pattern REMOVE_TAGS = Pattern.compile("<.+?>");
        if (string == null || string.length() == 0) {
            return string;
        }

        Matcher m = REMOVE_TAGS.matcher(string);
        return m.replaceAll("");
    }

    /**
     * This method reads reports and writes it in PDF file
     * @param userName - user name to generate token
     * @param userPassword - user password to generate token
     * @param studyReportId - study Report ID
     * @param file - pdf file
     * @throws Exception
     */
    public static void readReport(String userName, String userPassword, String studyReportId, String file) throws Exception{
        RequestSpecification httpRequest = TestUtilities.generateRequestSpecification(userName, userPassword);
        Response response = httpRequest.request(Method.GET, String.format(GET_FETCH_DOCS_URI, studyReportId));
        Assert.assertEquals(String.format("Response code should be %s", 200),
                200, response.getStatusCode());
        String uri = response.jsonPath().get(URI).toString();
        httpRequest.header(ACCEPT_ENCODING, GZIP);
        Response getFetchDocsWithTokeResponse = httpRequest.request(Method.GET,
                String.format(GET_FETCH_DOCS_URI + "/" + uri.substring(uri.lastIndexOf('/')+1),
                        studyReportId));
        TestUtilities.readResponseInPdf(getFetchDocsWithTokeResponse, file);
    }

    /**
     * This method reads and validates JSON Array from response
     * @param response - response
     * @param valueJSONPath - json Path to array
     * @param expectedData - expected data separated by comma
     */
    public static void readJSonArrayFromResponse(Response response, String valueJSONPath, String expectedData) {
        if(expectedData != null) {
            validateArrayFromResponse(response, valueJSONPath, expectedData.split(","));
        } else {
            validateArrayFromResponse(response, valueJSONPath, null);
        }
       /* ArrayList<String> array = (ArrayList)response.jsonPath().getJsonObject(valueJSONPath);
        if(expectedData == null) {
            Assert.assertTrue(String.format("%s array should be empty", valueJSONPath), array == null || array.size() == 0 );

        } else {
            Iterator<String> iterator = array.iterator();
            String[] expectedArray = expectedData.split(",");
            Assert.assertEquals(String.format("% Array should have %s elements",
                    valueJSONPath,expectedArray.length ), expectedArray.length, array.size());
            Set expectedSet = new HashSet<String>();
            expectedSet.addAll(Arrays.asList(expectedArray));
            Set actualSet = new HashSet<String>();
            actualSet.addAll(array);

            Assert.assertTrue(String.format("% array should contain values %s", expectedData), actualSet.equals(expectedSet));
        }*/

    }

    /**
     * This method reads and validates parameter from response
     * @param response - response
     * @param valueJSONPath - JSON path to parameter
     * @param expectedValue - expected value
     */
    public static void validateValueFromResponse(Response response, String valueJSONPath, String expectedValue) {
        if(expectedValue!=null) {
            Assert.assertEquals(String.format("%s Response value should be %s", valueJSONPath, expectedValue),
                    response.jsonPath().get(valueJSONPath).toString(), expectedValue);
        } else {
            Assert.assertEquals(String.format("%s Response value should be null", valueJSONPath),
                    response.jsonPath().get(valueJSONPath), null);
        }
    }

    public static void validateArrayFromResponse(Response response, String valueJSONPath, String[] expectedValue) {
        ArrayList<String> list = (ArrayList)response.jsonPath().getJsonObject(valueJSONPath);
        if(expectedValue!=null) {
            Set<String> responseSet = new HashSet<String>();
            Set<String> expectedSet = new HashSet<String>();
            responseSet.addAll(list);
            expectedSet.addAll(Arrays.asList(expectedValue));
            Assert.assertTrue(String.format("%s array should contain values %s", valueJSONPath,
                    Arrays.asList(expectedValue).toString()), responseSet.equals(expectedSet));
        } else {
            Assert.assertTrue(String.format("%s array should be empty", valueJSONPath), list == null || list.size() == 0 );

        }
    }
}
