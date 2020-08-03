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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * A resource that provides access to the world.
 */
@RestController
@RequestMapping("api/greet")
@Timed(value = "Greet", description = "Metrics of the GreetingController")
public class GreetController {

  private static final Logger LOG = LoggerFactory.getLogger(GreetController.class);

  @Autowired
  private GreetingApplicationService service;

  @GetMapping("{name}")
  @Operation(operationId = "greetSomeone", description = "Greet someone")
  @ApiResponse(responseCode = "200", description = "Ok", content = @Content(schema = @Schema(implementation = GreetDTO.class)))
  public ResponseEntity<GreetDTO> greet(@Parameter(description = "name") @PathVariable(value = "name") final String name) {
    LOG.info("Greet {}", name);

    GreetDTO message = new GreetDTO(service.getMessage(name));

    LOG.info("{}", message);

    return ResponseEntity.status(HttpStatus.OK)
        .body(message);
  }
  @GetMapping
  @Operation(operationId = "greetSomeone", description = "Greet the world")
  @ApiResponse(responseCode = "200", description = "Ok", content = @Content(schema = @Schema(implementation = GreetDTO.class)))
  public ResponseEntity<GreetDTO> greetTheWorld() {
    return greet("World");
  }

  @GetMapping("greeting")
  @Operation(operationId = "getGreeting", description = "Get greeting")
  @ApiResponse(responseCode = "200", description = "Ok", content = @Content(schema = @Schema(implementation = GreetingDTO.class)))
  public ResponseEntity<GreetingDTO> getGreeting() {
    LOG.info("Get greeting");

    GreetingDTO greeting = new GreetingDTO(service.getGreeting());

    LOG.info("{}", greeting);

    return ResponseEntity.status(HttpStatus.OK)
        .body(greeting);
  }

  @PutMapping("greeting")
  @Operation(operationId = "updateGreeting", description = "Update greeting")
  @RequestBody(description = "greeting", required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
      schema = @Schema(implementation = GreetingDTO.class, type = "OBJECT", example = "{\"greeting\" : \"Hola\"}")))
  @ApiResponse(responseCode = "204", description = "Greeting updated")
  @ApiResponse(responseCode = "400", description = "Invalid 'greeting' request")
  public ResponseEntity<?> updateGreeting(@Valid @org.springframework.web.bind.annotation.RequestBody final GreetingDTO greeting) {
    LOG.info("Set greeting to {}", greeting.getGreeting());

    service.updateGreeting(greeting.getGreeting());

    LOG.info("Greeting updated");

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
