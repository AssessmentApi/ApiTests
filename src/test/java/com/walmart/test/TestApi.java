package com.walmart.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.walmart.mockDB.MockDB;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.*;

public class TestApi {

    String bearerToken = "Bearer a73a75c54b75d9eed785a0546edf621dd893fe64643aa61b86d73512ab5eb505";

    @BeforeTest
    public void setUp() throws JsonProcessingException {
        MockDB.initialize();
        // Please do not change anything from Mock DB
    }

    @Test
    public void getUserList() {
        String urlGET = "https://gorest.co.in/public/v2/users";

        Response response = RestAssured.given()
                .when()
                .get(urlGET)
                .then()
                .statusCode(200)
                .extract().response();

        JsonPath jsonPath = response.jsonPath();

        List<Integer> apiIds = jsonPath.getList("id");
        List<String> apiNames = jsonPath.getList("name");
        List<String> apiEmails = jsonPath.getList("email");
        List<String> apiGenders = jsonPath.getList("gender");
        List<String> apiStatus = jsonPath.getList("status");

        for (int i = 0; i < apiIds.size(); i++) {
            Assert.assertEquals(apiIds.get(i), MockDB.ids.get(i));
            Assert.assertEquals(apiNames.get(i), MockDB.names.get(i));
            Assert.assertEquals(apiEmails.get(i), MockDB.emails.get(i));
            Assert.assertEquals(apiGenders.get(i), MockDB.genders.get(i));
            Assert.assertEquals(apiStatus.get(i), MockDB.statuses.get(i));
        }
    }

    @Test(priority = 1)
    public void createUserList() {
        String urlPOST = "https://gorest.co.in/public/v2/users";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "Test1");
        requestBody.put("email", "testuser" + new Random().nextInt(10000) + "@example.com");
        requestBody.put("gender", "male");
        requestBody.put("status", "active");

        Response response = RestAssured.given()
                .header("Authorization", bearerToken)
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post(urlPOST)
                .then()
                .statusCode(201)
                .extract().response();

        System.out.println("Create Response: " + response.asString());
    }

    @Test(priority = 2)
    public void uptadeUser() {
        String urlPATCH = "https://gorest.co.in/public/v2/users/{id}";

        int idToUpdate = MockDB.ids.get(new Random().nextInt(MockDB.ids.size()));

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "Updated Name");
        requestBody.put("email", "updated" + new Random().nextInt(10000) + "@mail.com");
        requestBody.put("gender", "female");
        requestBody.put("status", "inactive");

        RestAssured.given()
                .header("Authorization", bearerToken)
                .contentType("application/json")
                .pathParam("id", idToUpdate)
                .body(requestBody)
                .when()
                .patch(urlPATCH)
                .then()
                .statusCode(200);
    }

    @Test(priority = 3)
    public void deleteUser() {
        String urlDELETE = "https://gorest.co.in/public/v2/users/{id}";

        int idToDelete = MockDB.ids.get(new Random().nextInt(MockDB.ids.size()));

        // DELETE Request
        RestAssured.given()
                .header("Authorization", bearerToken)
                .pathParam("id", idToDelete)
                .when()
                .delete(urlDELETE)
                .then()
                .statusCode(204);

        // Confirm Deletion with GET
        RestAssured.given()
                .pathParam("id", idToDelete)
                .when()
                .get("https://gorest.co.in/public/v2/users/{id}")
                .then()
                .statusCode(404);
    }
}
