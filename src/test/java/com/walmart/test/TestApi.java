package com.walmart.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walmart.mockDB.MockDB;
import com.walmart.models.User;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Random;

@Slf4j
public class TestApi {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String LIST_USERS_API = "https://gorest.co.in/public/v2/users";
    private final String CREATE_USERS_API = "https://gorest.co.in/public/v2/users";
    private final String GET_USER_API = "https://gorest.co.in/public/v2/users/{id}";
    private final String DELETE_USER_API = "https://gorest.co.in/public/v2/users/{id}";
    private final String UPDATE_USER_API = "https://gorest.co.in/public/v2/users/{id}";

    private final String AUTH_TOKEN = "Bearer a73a75c54b75d9eed785a0546edf621dd893fe64643aa61b86d73512ab5eb505";

    @BeforeTest
    public void setUp() throws JsonProcessingException {
        MockDB.initialize();
        //Please do not change anything from Mock DB
    }


    @Test
    public void getUserList() throws JsonProcessingException {
        /*Create GET Method for below URL using RestAssured.
         Does not requried header 
         Validate the Status code
         Validate the API response body with MockDB Class Attributes using 'For Loop' within TestNG Assertions
         Hint: You can fetch Api response as jsonPath and store them in to List */
        MockDB.initialize(); // need to re-run this as when the whole test suite runs, data might have been modified by other tests
        int expectedStatusCode = 200;
        Response response = RestAssured.given()
                .when()
                .get(LIST_USERS_API)
                .then()
                .extract()
                .response();
        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, expectedStatusCode);

        List<User> userResult;
        JsonPath jsonPath = response.jsonPath();
        try {
            userResult = this.objectMapper.convertValue(jsonPath.getList(""), new TypeReference<>() {
            });
        } catch (IllegalArgumentException ex) {
            log.error("Response object is different than expected", ex);
            throw ex;
        }
        for (User user : userResult) {
            int userFoundIndex = -1;
            for (int j = 0; j < MockDB.ids.size(); j++) {
                if (MockDB.ids.get(j).equals(user.getId())) {
                    Assert.assertEquals(user.getEmail(), MockDB.emails.get(j));
                    Assert.assertEquals(user.getName(), MockDB.names.get(j));
                    Assert.assertEquals(user.getGender(), MockDB.genders.get(j));
                    Assert.assertEquals(user.getStatus(), MockDB.statuses.get(j));
                    userFoundIndex = j;
                    break;
                }
            }
            Assert.assertNotEquals(userFoundIndex, -1, "No valid user found in MockDB " + user.getId());
        }
    }


    @Test(priority = 1)
    public void createUserList() {
     /*
     Create POST Method using below URL
     Create requestBody using MAP(key-value) for name,email,gender,status(Active-Inactive)
     pass Header as : .header("Authorization","Bearer a73a75c54b75d9eed785a0546edf621dd893fe64643aa61b86d73512ab5eb505")
     validate the HTTP status Code using Assertions
     Print the Response body
      */
        String name = "test user";
        String email = "dummy" + System.currentTimeMillis() + "@example.com";
        String status = "active";
        String gender = "male";
        // ideally test for null values too in multiple create apis
        User activeDummyUser = User
                .builder()
                .name(name)
                .email(email)
                .gender(gender)
                .status(status)
                .build();

        // Send POST request
        // This can raise exception when there is a network call failure. need to handle that too
        // can be flaky if downstream is broken
        Response response = RestAssured.given()
                .header("Authorization", AUTH_TOKEN)
                .contentType("application/json")
                .body(activeDummyUser)
                .when()
                .post(CREATE_USERS_API)
                .then()
                .extract()
                .response();

        int expectedStatusCode = 201;
        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, expectedStatusCode);
        log.info(response.body().prettyPrint());
        User createdUser = response.body().as(User.class);
        Assert.assertEquals(createdUser.getName(), name);
        Assert.assertEquals(createdUser.getStatus(), status);
        Assert.assertEquals(createdUser.getEmail(), email);
        Assert.assertEquals(createdUser.getGender(), gender);

    }

    @Test
    public void updateUser() {
     /*
     Create PATCH Method using below URL to update user details
     Create requestBody using MAP(key-value) for name,email,gender,status
     pass Header as : .header("Authorization","Bearer a73a75c54b75d9eed785a0546edf621dd893fe64643aa61b86d73512ab5eb505")
     validate the only HTTP status Code using Assertion
     For id Parameter you can fetch a random id from Mock DB(List<Integer> ids)
      */
        Random random = new Random();
        int randomIndex = random.nextInt(MockDB.ids.size());
        Integer userId = MockDB.ids.get(randomIndex);

        // Create request body
        User userToBeUpdated = User
                .builder()
                .id(userId)
                .gender(MockDB.genders.get(randomIndex))
                .email(MockDB.emails.get(randomIndex))
                .status(MockDB.statuses.get(randomIndex))
                .name(MockDB.names.get(randomIndex))
                .build();

        String newName = "Updated Name";
        userToBeUpdated.setName(newName);

        // Send PATCH request
        Response response = RestAssured.given()
                .header("Authorization", AUTH_TOKEN)
                .contentType("application/json")
                .pathParam("id", userId)
                .body(userToBeUpdated)
                .when()
                .patch(UPDATE_USER_API)
                .then()
                .extract()
                .response();

        int expectedStatusCode = 200;
        int statusCode = response.getStatusCode();
        Assert.assertEquals(statusCode, expectedStatusCode);
        log.info(response.body().prettyPrint());
        Assert.assertEquals(response.body().as(User.class).getName(), newName); // verify if the name actually changed

    }

    @Test
    public void deleteUser() {
     /*
     Create DELETE Method using below URL to delete user and verify with GET method if user is deleted
     pass Header as : .header("Authorization","Bearer a73a75c54b75d9eed785a0546edf621dd893fe64643aa61b86d73512ab5eb505")
     validate the only HTTP status Code using Assertion
     For id Parameter you can fetch a random id from Mock DB(List<Integer> ids)
      */
        Random random = new Random();
        int randomIndex = random.nextInt(MockDB.ids.size());
        int userId = MockDB.ids.get(randomIndex);

        // Send DELETE request
        Response deleteResponse = RestAssured.given()
                .header("Authorization", AUTH_TOKEN)
                .pathParam("id", userId)
                .when()
                .delete(DELETE_USER_API)
                .then()
                .extract()
                .response();

        int expectedStatusCode = 204;
        int deleteStatusCode = deleteResponse.getStatusCode();
        Assert.assertEquals(deleteStatusCode, expectedStatusCode);

        Response getResponse = RestAssured.given()
                .pathParam("id", userId)
                .when()
                .get(GET_USER_API)
                .then()
                .extract()
                .response();

        expectedStatusCode = 404;
        int getStatusCode = getResponse.getStatusCode();
        Assert.assertEquals(getStatusCode, expectedStatusCode);
    }
}