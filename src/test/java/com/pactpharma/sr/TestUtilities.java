package com.pactpharma.sr;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.simple.JSONObject;
import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import static com.pactpharma.sr.TestConstants.*;


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
     * @param jsonObject - json object
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
}
