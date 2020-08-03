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

/**
 * Health check for the resource {@link GreetingController}.
 */
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Health check for the controller {@link GreetController}.
 */
@Component
public class GreetControllerHealthIndicator implements HealthIndicator {

  private static final String SERVER_PORT = "local.server.port";

  @Autowired
  private Environment environment;

  @Override
  public Health health() {
    Health.Builder builder = new Health.Builder();

    HttpHeaders headers = new HttpHeaders();
    headers.set("ORIGIN", String.format("%s:%s", "localhost", environment.getProperty(SERVER_PORT)));

    HttpEntity<String> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response = new RestTemplate().exchange(getResourceUri(), HttpMethod.OPTIONS, entity, String.class);

    boolean up = HttpStatus.OK.equals(response.getStatusCode());

    if (up) {
      builder.withDetail("resource", "available").up();
    } else {
      builder.withDetail("resource", "not available").down();
    }

    return builder.build();
  }

  private URI getResourceUri() {
    return UriComponentsBuilder.newInstance()
        .scheme("http")
        .host("localhost")
        .port(environment.getProperty(SERVER_PORT))
        .path(GreetController.class.getAnnotation(RequestMapping.class).value()[0])
        .build()
        .toUri();
  }
}
