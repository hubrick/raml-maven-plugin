# RAML Maven Plugin

Provides generation of Spring Web resource interfaces based on RAML files.

## Usage

Add the plugin to your pom.xml in `<build>` section:

    <plugins>
        <plugin>
            <groupId>com.hubrick</groupId>
            <artifactId>raml-maven-plugin</artifactId>
            <version>1.0.0</version>
            <configuration>
                <basePackage>tld.example.resources</basePackage>
                <modelPackage>model</modelPackage>
                <fileset>
                    <directory>${basedir}/src/main/raml</directory>
                    <includes>
                        <include>**/*.raml</include>
                    </includes>
                </fileset>
                <schemaGenerator>
                    <!-- Optionally, change settings -->
                    <generateBuilders>true</generateBuilders>
                </schemaGenerator>
            </configuration>
        </plugin>
    </plugins>

## Configuration

 Parameter      | Description
----------------|-------------------------------------------------------
basePackage     |a package, in which the resource classes will be put in
modelPackage    |a subpackage, in which the model classes will be put in
fileset         |where to source `.raml` files from
schemaGenerator |schema generator settings (see below)

## Goals

### spring-web

Generates Spring Web resources. Example:

    $ mvn raml:spring-web

users.raml
```yaml
#%RAML 0.8
title:

/users:
  post:
    body:
      application/json:
        schema: user
  /{userId}:
    uriParameters:
      userId:
        type: string
        description: |
          @x-javaType java.util.UUID
    get:
      responses:
        200:
          body:
            application/json:
              schema: user
              example: |
                  { "id": "99648e19-fcf4-4227-b9e9-99b0fc5e3f2c",
                    "name": "John Doe",
                    "birthdate": "1970-01-01" }

schemas:
  - user: |
      {
        "type": "object",
        "properties": {
          "id": { "$ref": "types.json#/definitions/uuid" },
          "name": { "type": "string" },
          "birthdate": { "type": "object", "javaType": "java.util.Date" }
        }
      }
```

User.java
```java
    package tld.example.resources.model;

    import java.util.Date;
    import java.util.HashMap;
    import java.util.Map;
    import java.util.UUID;
    import javax.annotation.Generated;
    import com.fasterxml.jackson.annotation.JsonAnyGetter;
    import com.fasterxml.jackson.annotation.JsonAnySetter;
    import com.fasterxml.jackson.annotation.JsonIgnore;
    import com.fasterxml.jackson.annotation.JsonInclude;
    import com.fasterxml.jackson.annotation.JsonProperty;
    import com.fasterxml.jackson.annotation.JsonPropertyOrder;
    import org.apache.commons.lang.builder.EqualsBuilder;
    import org.apache.commons.lang.builder.HashCodeBuilder;
    import org.apache.commons.lang.builder.ToStringBuilder;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Generated("org.jsonschema2pojo")
    @JsonPropertyOrder({
        "id",
        "name",
        "birthdate"
    })
    public class User {

        @JsonProperty("id")
        private UUID id;
        @JsonProperty("name")
        private String name;
        @JsonProperty("birthdate")
        private Date birthdate;
        @JsonIgnore
        private Map<String, Object> additionalProperties = new HashMap<String, Object>();

        // ...

    }
```

UsersResource.java
```java
    package tld.example.resources;

    import java.util.UUID;
    import org.springframework.web.bind.annotation.PathVariable;
    import org.springframework.web.bind.annotation.RequestBody;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RequestMethod;
    import org.springframework.web.bind.annotation.RestController;
    import tld.example.resources.model.User;

    @RestController
    @RequestMapping("/")
    public interface UsersResource {

        @RequestMapping(value = "/users", method = RequestMethod.POST)
        void postUsers(@RequestBody User user);

        @RequestMapping(value = "/users/{userId}", method = RequestMethod.GET)
        User getUsers(@PathVariable("userId") UUID userId);

    }
```

### spring-web-validate

Validates existing code for compliance with resource definitions in RAML files.

    $ mvn raml:spring-web-validate

 Parameter      | Description
----------------|----------------------------------------------------
includes        |generated source files inclusion filter
excludes        |generated source files exclusion filter

#### Example

    <plugins>
        <plugin>
            <groupId>com.hubrick</groupId>
            <artifactId>raml-maven-plugin</artifactId>
            <version>1.0.0</version>
            <configuration>
                <basePackage>tld.example.resources</basePackage>
                <modelPackage>model</modelPackage>
                <fileset>
                    <directory>${basedir}/src/main/raml</directory>
                    <includes>
                        <include>**/*.raml</include>
                    </includes>
                </fileset>
                <includes>
                    <param>**/*Resource.java</param>
                </includes>
                <schemaGenerator>
                    <!-- Optionally, change settings -->
                    <generateBuilders>true</generateBuilders>
                </schemaGenerator>
            </configuration>
        </plugin>
    </plugins>

### Schema Generator

The plugin uses [jsonschema2pojo](https://github.com/joelittlejohn/jsonschema2pojo) to generate schemas.
Below is the list of settings that can go in `schemaConfigurator` section of plugin's configuration:

 Property                               | Type
----------------------------------------|---------
generateBuilders                        | boolean
usePrimitives                           | boolean
propertyWordDelimiters                  | char[]
useLongIntegers                         | boolean
useDoubleNumbers                        | boolean
includeHashcodeAndEquals                | boolean
includeToString                         | boolean
annotationStyle                         | JACKSON, JACKSON1, JACKSON2, GSON or NONE
includeJsr303Annotations                | boolean
useJodaDates                            | boolean
useJodaLocalDates                       | boolean
useJodaLocalTimes                       | boolean
useCommonsLang3                         | boolean
parcelable                              | boolean
initializeCollections                   | boolean
classNamePrefix                         | boolean
classNameSuffix                         | boolean
includeConstructors                     | boolean
constructorsRequiredPropertiesOnly      | boolean
