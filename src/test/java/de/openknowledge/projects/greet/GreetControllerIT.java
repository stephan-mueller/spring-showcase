/*
 * Copyright (C) open knowledge GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package de.openknowledge.projects.greet;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

/**
 * Integration test for the resource {@link GreetController}.
 */
class GreetControllerIT extends AbstractIntegrationTest {

  private static final Logger LOG = LoggerFactory.getLogger(GreetControllerIT.class);

  private static RequestSpecification requestSpecification;

  @BeforeAll
  static void setUpUri() {
    APPLICATION.withLogConsumer(new Slf4jLogConsumer(LOG));

    requestSpecification = new RequestSpecBuilder()
        .setPort(APPLICATION.getFirstMappedPort())
        .build();

    RestAssured.given(requestSpecification)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("{ \"greeting\" : \"Hello\" }")
        .when()
        .put("/api/greet/greeting")
        .then()
        .statusCode(HttpStatus.NO_CONTENT.value());
  }

  @Test
  void greet() {
    RestAssured.given(requestSpecification)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .pathParam("name", "Stephan")
        .when()
        .get("/api/greet/{name}")
        .then()
        .statusCode(HttpStatus.OK.value())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("message", Matchers.equalTo("Hello Stephan!"));
  }

  @Test
  void greetTheWorld() {
    RestAssured.given(requestSpecification)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .when()
        .get("/api/greet")
        .then()
        .statusCode(HttpStatus.OK.value())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("message", Matchers.equalTo("Hello World!"));
  }

  @Test
  void getGreeting() {
    RestAssured.given(requestSpecification)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .when()
        .get("/api/greet/greeting")
        .then()
        .statusCode(HttpStatus.OK.value())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("greeting", Matchers.equalTo("Hello"));
  }

  @Test
  void updateGreeting() {
    RestAssured.given(requestSpecification)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("{ \"greeting\" : \"Hello\" }")
        .when()
        .put("/api/greet/greeting")
        .then()
        .statusCode(HttpStatus.NO_CONTENT.value());
  }
}
