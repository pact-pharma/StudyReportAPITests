package com.pactpharma.sr;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;
import static com.pactpharma.sr.TestConstants.*;


public class TestUtilities {

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

    public static String readJsonFile(String jsonFile) throws Exception{
        String json = readFileAsString(jsonFile);

        System.out.println(json);
        return json;
    }

    public static String readFileAsString(String file)throws Exception
    {
        return new String(Files.readAllBytes(Paths.get(file)));
    }

    public static RequestSpecification generateRequestSpecification(String userName, String userPassword) {
        RestAssured.baseURI = BASE_URI;
        RequestSpecification httpRequest = RestAssured.given();
        String token = TestUtilities.getToken(userName, userPassword);
       // System.out.println("token " + token);
        httpRequest.header(AUTHORIZATION, String.format("Bearer %s", token));
        return httpRequest;
    }

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


    public static StringBuilder constructPartOfUrl(StringBuilder strBuilderUrl, String stringToSubstitute, String partOfUrl) {
        if(!(stringToSubstitute == null || stringToSubstitute.isEmpty() || stringToSubstitute.isBlank())){
            strBuilderUrl.append(String.format(partOfUrl, stringToSubstitute));
        }
        return strBuilderUrl;
    }




}
