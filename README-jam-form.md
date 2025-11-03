# README

Hi, Spring fans! Later this month we get Spring Framework 7 and Spring Boot 4 and all the portfolio projects that Spring Boot 4 depends on. Let's at least look at some of my favorite features together. **Jakarta EE 11** 11 is the new baseline. We've retained a **Java** 17 baseline but _strongly_ encourage a Java 25 posture whenever possible. We've moved the baseline to GraalVM 25 for native images. we've updated our **Kotlin** support  to Kotlin 2.2.  The new generation of projects assumes the new  **Jackson 3**, the popular JSON marshalling library. We've introduced **JSpecify nullability** annotations across the portfolio to finally eliminate  Tony Hoare's   $1 billion dollar mistake - `null` references. Spring Boot 4 has decomposed the  autoconfiguration artifact into smaller more **modular autoconfigurations**. 

One of my favorite new features is **API versioning**. Here's a Spring MVC controller with two endpoints, each designated a particular _version_, the default of which we can specify with `spring.mvc.apiversion.default=1.1`.  

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
What if you want to dynamically register beans with Spring? What if you want to minimize reflection? Java configuration and component scanning are very powerful, but they're not dynamic, and they rely heavily on reflection when run on the JVM. In Spring Framework 7, we've introduced **BeanRegistrar** to help with both. Here's an example of implicitly and explicitly defining bean of class `Runner` twice. You can activate it by `@Import`-ing it just like you would other Java configuration classes. 

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
@EnableGlobalMultiFactorAuthentication(authorities = {
        FactorGrantedAuthority.PASSWORD_AUTHORITY,
        FactorGrantedAuthority.OTT_AUTHORITY
})
@Configuration
class SecurityConfiguration {
    
    ...

    @Bean
    Customizer<HttpSecurity> httpSecurityCustomizer() {
        return httpSecurity -> httpSecurity
                .webAuthn(w -> w
                    ...
                )
                .oneTimeTokenLogin(ott ->
                    ... 
                );
    }
}
```
 The code is here for your [reference](https://github.com/joshlong-attic/2025-11-02-jetbrains-jam-article-on-spring-boot-4). We encourage users to kick the tires and try everything out on the [Spring Initializer](https://start.spring.io).