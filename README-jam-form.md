# README

Hi, Spring fans! Spring Boot 4 comes out in November 2025!  Let's at least look at some of my favorite features together. **Jakarta EE 11** 11 is the new baseline. We've retained a **Java** 17 baseline but _strongly_ encourage a Java 25 posture whenever possible. We've moved the baseline to **GraalVM 25** for native images. we've updated our **Kotlin** support to Kotlin 2.2. The new generation of projects assumes the new  **Jackson 3**. We've introduced **JSpecify nullability** annotations across the portfolio to finally eliminate Tony Hoare's $1 billion dollar mistake - `null` references. Spring Boot 4 has decomposed the autoconfiguration artifact into smaller, more **modular autoconfigurations**. 

One of my favorite new features is **API versioning**. Here's a Spring MVC controller with two endpoints, each designated a particular _version_, the default version of which we can specify with `spring.mvc.apiversion.default=1.1`.  

```java

@RestController
class DogsController {
    ...
    
    @GetMapping(value = "/dogs",version="1.0")
    Collection<Map<String, Object>> dogsClassic() {
        ... 
    }

    @GetMapping(value = "/dogs", version = "1.1")
    Collection<Dog> dogs() {
        ... 
    }
}
```

I also love the new easy-mode declarative HTTP clients. Let's build a client to talk to the Cat Facts API and then activate it using `@ImportHttpServices(CatFacts.class)`: 

```java
record CatFact(@JsonProperty("fact_number") int factNumber, String fact) { }

record CatFactsResponse(Collection<CatFact> facts) { }

interface CatFacts {

    @GetExchange("https://www.catfacts.net/api")
    CatFactsResponse facts();
}
```

We built a simple client, and it should work, but what if the service is down? We can enable **resilient methods** with Spring's new `@EnableResilientMethods` annotation. This gives us two new features: `@ConcurrencyLimit` and `@Retryable` (and the related `RetryTemplate`)


```java
    ...

    @ConcurrencyLimit(10)
    @Retryable(maxAttempts = 4, includes = BadException.class)
    @GetMapping("/facts")
    Collection<CatFact> facts()  throws Exception {
        ...
    }
    ...

```

Spring Framework 7 brings **BeanRegistrar** configuration, which are a more dynamic alternative to Java configuration that can be used in conjunction with Java configuration and component scanning with the usual `@Import` annotation.

```java
class MyBeanRegistrar implements BeanRegistrar {

    @Override
    public void register(BeanRegistry registry, Environment env) {
        registry.registerBean(Runner.class);
        registry.registerBean(Runner.class, spec -> spec
                .description("description")
                .supplier(supplierContext -> new Runner(
                        supplierContext.bean(DogRepository.class), supplierContext.beanProvider(CatFacts.class)
                        .getIfUnique())
                ));
    }
}
```

Spring Security brings with it a lot of nice new features, two of my favorites are multi-factor auth, allowing you to require that multiple authentication factors be present, and `Customizer<HttpSecurity>` beans for additive changes to the `HttpSecurity` configuration. 

```java
@EnableGlobalMultiFactorAuthentication(
    authorities = { FactorGrantedAuthority.PASSWORD_AUTHORITY, FactorGrantedAuthority.OTT_AUTHORITY })
@Configuration
class SecurityConfiguration {

    @Bean
    Customizer<HttpSecurity> httpSecurityCustomizer() {
        return httpSecurity -> httpSecurity
                .webAuthn(w -> ... )
                .oneTimeTokenLogin(ott -> ... );
    }
}
```
 The code is here for your [reference](https://github.com/joshlong-attic/2025-11-02-jetbrains-jam-article-on-spring-boot-4). I encourage users to kick the tires and try everything out on the [Spring Initializer](https://start.spring.io) and enjoy your journey to production!