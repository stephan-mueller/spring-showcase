# Spring Showcase

[![GitHub last commit](https://img.shields.io/github/last-commit/stephan-mueller/spring-showcase)](https://github.com/stephan-mueller/spring-showcase/commits) 
[![GitHub](https://img.shields.io/github/license/stephan-mueller/spring-showcase)](https://github.com/stephan-mueller/spring-showcase/blob/master/LICENSE)
[![CircleCI](https://circleci.com/gh/stephan-mueller/spring-showcase.svg?style=shield)](https://app.circleci.com/pipelines/github/stephan-mueller/spring-showcase)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=stephan-mueller_spring-showcase&metric=alert_status)](https://sonarcloud.io/dashboard?id=stephan-mueller_spring-showcase)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=stephan-mueller_spring-showcase&metric=coverage)](https://sonarcloud.io/dashboard?id=stephan-mueller_spring-showcase)

This is a showcase for the microservice framework [Spring Boot](https://spring.io). It contains a hello world application, 
which demonstrates several features of Spring Boot, 
[Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-features.html#production-ready), 
[Spring Boot Admin](https://github.com/codecentric/spring-boot-admin) and [Springdoc OpenAPI](https://springdoc.org).

Software requirements to run the samples are `maven`, `openjdk-8` (or any other JDK 8) and `docker`.
When running the Maven lifecycle it will use the `spring-boot-maven-plugin` to create a runnable JAR (fat JAR) which contains the 
application and the Tomcat server. The fat JAR will be copied into a Docker image using Spotify's `dockerfile-maven-plugin` during the 
package phase.

**Notable Features:**
* Dockerfile for runnable JAR 
* Integration of Spring Boot Actuator for Health and Metrics 
* Integration of Spring Boot Admin for Monitoring
* Integration of Springdoc OpenAPI
* Testcontainer tests with REST-assured, Cucumber and Postman/Newman
* Code-Coverage for Testcontainer tests
* [CircleCI](https://circleci.com) Integration
* [Sonarcloud](https://sonarcloud.io) Integration


## How to run

Before running the application it needs to be compiled and packaged using `Maven`. It creates the runnable JAR and Docker image and can be 
run via `docker`:

```shell script
$ mvn clean package
$ docker run --rm -p 8080:8080 spring-showcase
```

If everything worked you can access the OpenAPI UI via [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) 
and the Spring Boot Admin UI via [http://localhost:8080/](http://localhost:8080/)

### Resolving issues

Sometimes it may happen that the containers did not stop as expected when trying to stop the pipeline early. This may
result in running containers although they should have been stopped and removed. To detect them you need to check
Docker:

```shell script
$ docker ps -a | grep spring-showcase
```

If there are containers remaining although the application has been stopped you can remove them:

```shell script
$ docker rm <ids of the containers>
```


## Features

### Application 

The application is a very simple "Hello World" greeting service. It supports GET requests for generating a greeting message, and a PUT 
request for changing the greeting itself. The response is encoded using JSON.

Try the application
```shell script
curl -X GET http://localhost:8080/api/greet
{"message":"Hello World!"}

curl -X GET http://localhost:8080/api/greet/Stephan
{"message":"Hello Stephan!"}

curl -X PUT -H "Content-Type: application/json" -d '{"greeting" : "Hola"}' http://localhost:8080/api/greet/greeting

curl -X GET http://localhost:8080/api/greet/greeting
{"greeting":"Hola"}

curl -X GET http://localhost:8080/api/greet/Max
{"message":"Hola Max!"}
```

### Health, Metrics and OpenAPI

The application provides support for health, metrics and openapi endpoints assisted by 
[Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-features.html#production-ready) and 
[Springdoc OpenAPI](https://springdoc.org).

Health
```shell script
curl -s -X GET http://localhost:8080/actuator/health
```

Metrics in Prometheus / JSON Format
```shell script
curl -s -X GET http://localhost:8080/actuator/prometheus

curl -s -X GET http://localhost:8080/actuator/metrics
```

OpenAPI in YAML / JSON Format
```shell script
curl -s -X GET http://localhost:8080/v3/api-docs.yaml

curl -s -X GET http://localhost:8080/v3/api-docs
```

### Spring Boot Admin

codecentric’s Spring Boot Admin is a community project to manage and monitor Spring Boot applications. The application provides the 
Spring Boot Admin Client, Server and UI. The Admin UI is reachable via [http://localhost:8080/](http://localhost:8080/).


### Testcontainer tests with REST-assured, Cucumber and Postman/Newman

For the application a set of integration tests is provided. The tests bases on Testcontainers combined with other testing frameworks like 
REST-assured, Cucumber and Postman/Newman. The docker container for the application is build by the `dockerfile-maven-plugin` during the 
`package` phase.

To improve the runtime of the testcontainer tests by avoid starting and stopping the container for every test class, the 
[singleton container](https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/) pattern is used.

The container is started only once when the base class is loaded. The container can then be used by all inheriting test classes. At the end 
of the test suite the Ryuk container that is started by Testcontainers core will take care of stopping the singleton container.

AbstractIntegrationTest - Superclass for all Testcontainers tests providing the containerized application
```java
public abstract class AbstractIntegrationTest {

  protected static final String NETWORK_ALIAS_APPLICATION = "application";

  protected static final Network NETWORK = Network.newNetwork();

  protected static final GenericContainer<?> APPLICATION = new GenericContainer<>("spring-showcase")
      .withExposedPorts(8080)
      .withNetwork(NETWORK)
      .withNetworkAliases(NETWORK_ALIAS_APPLICATION)
      .waitingFor(Wait.forHealthcheck());

  static {
    APPLICATION.start();
  }
}
```  

#### Integration tests with Testcontainer and REST-assured

[REST-assured](http://rest-assured.io) is a popular test framework for testing and validating REST services that brings the the simplicity 
of dynamic languages into the Java domain. 

To ease making HTTP requests to the containerized application, REST-assured provides specifications to reuse response expectations and/or 
request parameters for different tests. The `RequestSpecBuilder` is used to define the dynamic port of the application for all requests only 
once. 

GreetingResourceIT - Integration tests for the GreetingResource
```java
class GreetResourceIT extends AbstractIntegrationTest {

  private static final Logger LOG = LoggerFactory.getLogger(GreetResourceIT.class);

  private static RequestSpecification requestSpecification;

  @BeforeAll
  static void setUpUri() {
    APPLICATION.withLogConsumer(new Slf4jLogConsumer(LOG));

    requestSpecification = new RequestSpecBuilder()
        .setPort(APPLICATION.getFirstMappedPort())
        .build();

    RestAssured.given(requestSpecification)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{ \"greeting\" : \"Hello\" }")
        .when()
        .put("/api/greet/greeting")
        .then()
        .statusCode(Response.Status.NO_CONTENT.getStatusCode());
  }

  @Test
  void greetTheWorld() {
    RestAssured.given(requestSpecification)
        .accept(MediaType.APPLICATION_JSON)
        .when()
        .get("/api/greet")
        .then()
        .statusCode(Response.Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body("message", Matchers.equalTo("Hello World!"));
  }
}
```

#### Acceptance tests with Testcontainer, REST-assured and Cucumber

[Cucumber](https://github.com/cucumber/cucumber-jvm) is one of the most popular tools that supports Behaviour-Driven Development(BDD) for 
the Java language. Cucumber reads executable specifications written in natural language and validates that the software does what those 
specifications say. The specifications consist of several examples or scenarios - which is why this approach is known as 
[Specification by Example](https://en.wikipedia.org/wiki/Specification_by_example).

Greeting.feature - Acceptance tests in natural language (Gherkin syntax)
```gherkin
Feature: Greeting

  Scenario: Greet the world
    Given a greeting "Hello"
    When a user wants to greet
    Then the message is "Hello World!"

  Scenario Outline: Greet someone
    Given a greeting "<greeting>"
    When a user wants to greet "<name>"
    Then the message is "<greeting> <name>!"

    Examples:
      | greeting | name      |
      | Hola     | Christian |
      | Hey      | Max       |
      | Moin     | Stephan   |
```

To run cucumber tests, you still have to to use the `Cucumber` JUnit4 runner, due to missing support for JUnit5.

GreetingCucumberIT - JUnit4 based test class that runs all acceptance tests of the project
```java
@RunWith(Cucumber.class)
@CucumberOptions(plugin = {"pretty"}, features = "src/test/resources/it/feature")
public class GreetingCucumberIT {
}
```

Due to its BDD-oriented nature, REST-assured seamlessly integrates with Cucumber to implement acceptance tests for RESTful APIs. To 

GreetingCucumberSteps - Step definitions matching the steps in the feature file
```java
public class GreetingCucumberSteps extends AbstractIntegrationTest {

  private RequestSpecification requestSpecification;

  private io.restassured.response.Response response;

  @Before
  public void beforeScenario() {
    requestSpecification = new RequestSpecBuilder()
        .setPort(APPLICATION.getFirstMappedPort())
        .build();
  }

  @Given("a greeting {string}")
  public void given_a_greeting(final String greeting) {
    RestAssured.given(requestSpecification)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new GreetingDTO(greeting))
        .when()
        .put("/api/greet/greeting")
        .then()
        .statusCode(Response.Status.NO_CONTENT.getStatusCode());
  }

  @When("a user wants to greet")
  public void when_a_user_wants_to_greet() {
    response = RestAssured.given(requestSpecification)
        .accept(MediaType.APPLICATION_JSON)
        .when()
        .get("/api/greet");
  }

  @When("a user wants to greet {string}")
  public void when_a_user_wants_to_greet(final String name) {
    response = RestAssured.given(requestSpecification)
        .accept(MediaType.APPLICATION_JSON)
        .pathParam("name", name)
        .when()
        .get("/api/greet/{name}");
  }

  @Then("the message is {string}")
  public void then_the_message_is(final String message) {
    response.then()
        .statusCode(Response.Status.OK.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body("message", Matchers.equalTo(message));
  }
}
```

As expected the execution of the specification examples can also be easily followed in the log output

Cucucmber log output
```text
[INFO] Running de.openknowledge.projects.greet.GreetingCucumberIT

Scenario: Greet the world                # src/test/resources/it/feature/Greeting.feature:3
  Given a greeting "Hello"               # de.openknowledge.projects.greet.GreetingCucumberSteps.given_a_greeting(java.lang.String)
  When a user wants to greet             # de.openknowledge.projects.greet.GreetingCucumberSteps.when_a_user_wants_to_greet()
  Then the message is "Hello World!"     # de.openknowledge.projects.greet.GreetingCucumberSteps.then_the_message_is(java.lang.String)

Scenario Outline: Greet someone          # src/test/resources/it/feature/Greeting.feature:15
  Given a greeting "Hola"                # de.openknowledge.projects.greet.GreetingCucumberSteps.given_a_greeting(java.lang.String)
  When a user wants to greet "Christian" # de.openknowledge.projects.greet.GreetingCucumberSteps.when_a_user_wants_to_greet(java.lang.String)
  Then the message is "Hola Christian!"  # de.openknowledge.projects.greet.GreetingCucumberSteps.then_the_message_is(java.lang.String)

Scenario Outline: Greet someone          # src/test/resources/it/feature/Greeting.feature:16
  Given a greeting "Hey"                 # de.openknowledge.projects.greet.GreetingCucumberSteps.given_a_greeting(java.lang.String)
  When a user wants to greet "Max"       # de.openknowledge.projects.greet.GreetingCucumberSteps.when_a_user_wants_to_greet(java.lang.String)
  Then the message is "Hey Max!"         # de.openknowledge.projects.greet.GreetingCucumberSteps.then_the_message_is(java.lang.String)

Scenario Outline: Greet someone          # src/test/resources/it/feature/Greeting.feature:17
  Given a greeting "Moin"                # de.openknowledge.projects.greet.GreetingCucumberSteps.given_a_greeting(java.lang.String)
  When a user wants to greet "Stephan"   # de.openknowledge.projects.greet.GreetingCucumberSteps.when_a_user_wants_to_greet(java.lang.String)
  Then the message is "Moin Stephan!"    # de.openknowledge.projects.greet.GreetingCucumberSteps.then_the_message_is(java.lang.String)

[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.916 s - in de.openknowledge.projects.greet.GreetingCucumberIT
```

#### API Tests with Testcontainer and Postman/Newman

Postman is an popular API client that supports automated API testing. Test collections developed in Postman can be exported and integrated 
with your CI/CD pipeline by using [Newman](https://github.com/postmanlabs/newman), Postman's command line Collection Runner. 

Newman allows you to run and test a Postman Collection directly from the command line. It is built with extensibility in mind so that it 
can easily integrate it with continuous integration servers, build systems and even Testcontainers.

To automate Postman test collections with Testcontainers the newman docker image is required. The collection and the environment file has 
to be copied to the docker image, and a file system bind has to be configured, to be able to access the test reports.

**IMPORTANT**: The newman container is started and stopped for the execution of a single command - running the collection. To prevent that 
the containers is stopped before the test collection is executed, a `OneShotStartupCheckStrategy` with a timeout of 5 to 10 seconds has to 
be configured for the newman container.

GreetingPostmanIT - Newman container that runs a Postman collection against the containerized application.
```java
class GreetingPostmanIT extends AbstractIntegrationTest {

  private static final Logger LOG = LoggerFactory.getLogger(GreetResourceIT.class);

  private static final GenericContainer<?> NEWMAN = new GenericContainer<>("postman/newman:5.1.0-alpine")
      .withNetwork(NETWORK)
      .dependsOn(APPLICATION)
      .withCopyFileToContainer(MountableFile.forClasspathResource("postman/hello-world.postman_collection.json"),
                               "/etc/newman/hello-world.postman_collection.json")
      .withCopyFileToContainer(MountableFile.forClasspathResource("postman/hello-world.postman_environment.json"),
                               "/etc/newman/hello-world.postman_environment.json")
      .withFileSystemBind("target/postman/reports", "/etc/newman/reports", BindMode.READ_WRITE)
      .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(5)));

  @Test
  void run() {
    NEWMAN.withCommand("run", "hello-world.postman_collection.json",
                       "--environment=hello-world.postman_environment.json",
                       "--reporters=cli,junit",
                       "--reporter-junit-export=reports/hello-world.newman-report.xml");
    NEWMAN.start();

    LOG.info(NEWMAN.getLogs());

    assertThat(NEWMAN.getCurrentContainerInfo().getState().getExitCode()).isZero();
  }
}
```

## Deployment in SAP Business Technology Platform (BTP) Cloud Foundry
BTP is a platform-as-a-service by SAP, offering services for developing and integrating custom applications with SAP
systems and deploying them in different cloud-native environments. This application includes configuration for deploying it
in the [Cloud Foundry](https://help.sap.com/docs/btp/sap-business-technology-platform/cloud-foundry-environment) environment.
It makes deploying a single fat JAR simple without worrying about infrastructure details.

### Optional: BTP Trial
If you don't have access to an existing BTP account, you can get a trial account for 90 days: https://www.sap.com/products/technology-platform/trial.html

When the trial account is ready, log in to the BTP Cockpit and make sure that the Cloud Foundry environment is enabled.

### How to deploy
First install the Cloud Foundry CLI, see the [installation guide](https://github.com/cloudfoundry/cli/wiki/V8-CLI-Installation-Guide).

Lookup the CF API endpoint in BTP Cockpit and configure the CLI to use it, with a trial account it's probably the US endpoint:
```shell script
$ cf api https://api.cf.us10-001.hana.ondemand.com
```

Login with CF CLI:
```shell script
$ cf login --sso
```

Package the application into a fat JAR. Use the `btp` maven-profile to skip the docker build:
```shell script
$ mvn clean package -P btp
```

Push the application:
```shell script
$ cf push
```
This command uses the `manifest.yml` to create the service accordingly in Cloud Foundry and pushes the JAR.
If it's successful, the command will output the URL of the running application.

### Details on the configuration

The [manifest.yml](manifest.yml) describes how the application should be deployed in Cloud Foundry. 
It points to the packaged JAR and enables the `sap_java_buildpack`, which prepares the application for the environment and injects some agents for monitoring.
You can also configure static routes and environment variables like the active spring profile.

The [application-btp.properties](src/main/resources/application-btp.properties) is used for Spring configuration in the BTP environment.
It configures the service URL of the Spring Boot Admin client instance using the `${vcap.application.uris[0]}` property. 
This property is set automatically by the Spring Boot class `CloudFoundryVcapEnvironmentPostProcessor` by reading the `VCAP_APPLICATION` environment variable,
which is set by Cloud Foundry on startup and contains details on the application like the public URL.