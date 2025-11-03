# README

Hi, Spring fans! It's November 2025, we're rocking Java 25, and a new generation of Spring, starting with Spring Framework 7, begins arriving this very month. How exciting! There are a ton of new features across the portfolio from Spring Framework 7 on up. Spring Framework 7 is the beginning of the release train of a whole new generation of your favorite projects, including Spring Data, Spring Security, Spring Batch, Spring Integration, etc. These projects all get rolled up into the next generation Spring Boot 4 release, also due later this month. And Spring Boot 4, of course, is itself the foundational layer of a whole new generation of so called post-Boot projects like Spring Cloud, Spring Modulith, and Spring AI. 

## "Dogs and cats living together - MASS HYSTERIA!"
We couldn't possibly hope to look at _everything_, but let's at least look at some of my favorite features together. As always, we'll need a subject on which to focus for our demonstrations. So, with apologies to _Ghostbusters_, we'll build some applications that look at dogs and cats. 

## Portfolio-wide updates 

There are several things you'll notice, portfolio-wide. Let's look at some of them now. 

**Jakarta EE 11**: we've updated to Jakarta EE 11. Unlike the move from Java EE to Jakarta EE in Spring Boot 3, this move should be basically painless. 

**Java**: we've retained a Java 17 baseline but _strongly_ encourage a Java 25 posture whenever possible. And, for GraalVM native image users, we've moved the baseline to GraalVM 25, as well, which has a different configuration format. 

**Kotlin**: we've updated our Kotlin support across the board, now defaulting to Kotlin 2.2. This is an amazing release. We've also updated our support for Kotlin serialization, coroutines, etc. 

**Jackson 3**: the new generation of projects assumes version 3 of Jackson, the popular JSON marshalling library. Jackson 3 is a _very_ incompatible release.  Mercifully, it lives in a completely different package and Maven artifact, so you won't _accidentally_ start using it. It's the default in Spring Boot 4, but we'll have a compatibility starter to preserve Jackson 2 behavior, if required. It is possible to have both Jackson 2 and 3 on the classpath at the same time. 

**Nullability**: we've introduced JSpecify nullability annotations across the portfolio to finally eliminate  Tony Hoare's   $1 billion dollar mistake - `null` references! Most modern languages - like Kotlin, Typescript, Swift, C#, etc. - allow you to declare whether a reference can be `null` or not. But Java doesn't. (There's hope it'll land one day in Project Valhalla.) In the meantime, we've worked hand in hand with Jetbrains' IntelliJ IDEA team and Kotlin team, and with a consortium of other players, to develop  JSpecify, a set of standardized annotations. You'll enjoy feedback directly in your IDE, in your builds, and from Kotlin.

**Modularization**: Spring Boot 4's decomposed the  autoconfiguration artifact into smaller autoconfigurations. This will give us clearer signals about your intent and allow us more modular starters. My favorite example? In the past you'd specify `spring-boot-starter-web` if you wanted a web application _or_ if you wanted an HTTP client. Now, you can use `spring-boot-starter-webmvc` to get the webserver behavior and `spring-boot-starter-restclient` to get the HTTP client. Nice! 


## Our Domain Model 

Let's use the new versions of Spring Data, and in particular Spring Data JDBC, to talk to our application's SQL database. Configure the `application.properties` file to point a to a database. Wire up the Spring Data JDBC repository and entity:

```java

// look mom, no Lombok!
record Dog(@Id int id, String name, String description) {
}

interface DogRepository extends ListCrudRepository<@NonNull Dog, @NonNull Integer> {

    @Query("select * from dog where name = :name ")
    Collection<Dog> findByName(String name);
}
```

Note that we're using `@NonNull` from JSpecify in the generic parameters there. There are a ton of new features in Spring Data, but one of my favorites is that if you're using Spring Data in AOT mode (e.g., for GraalVM native images), Spring Data can _generate_ the repository implementation at compile time.  


## API versioning 
Let's suppose we want to develop a controller that returns the `dog` entities from our database. Here's the implementation that's in production:

```java

@Controller
@ResponseBody
class DogsController {

    private final DogRepository dogRepository;

    DogsController(DogRepository dogRepository) {
        this.dogRepository = dogRepository;
    }

    @GetMapping(value = "/dogs")
    Collection<Map<String, Object>> dogsClassic() {
        return this.dogRepository.findAll()
                .stream()
                .map(dog -> Map.of("id", (Object) dog.id(),
                        "dogName", dog.name()))
                .toList();
    }
}
```

Oops! We made a mistake, and we've got to break the API. In Spring Framework 7, there's new support for API versioning.

Change the mapping annotation above to look like this:

```java
    @GetMapping(value = "/dogs", version = "1.0")
```

Now, that API can co-exist with the new API, which clients will get by default. Here's that new API. 

```java
    @GetMapping(value = "/dogs", version = "1.1")
    Collection<Dog> dogs() {
        return this.dogRepository.findAll();
    }
```

It's trivial to declare a default version, and stipulate by what mechanism a client can trigger the selection of a particular version. 

```properties
spring.mvc.apiversion.default=1.1
spring.mvc.apiversion.use.query-parameter=api-version
```


## Facts and Cats 

I hesitate to even do this because it's not possible to assert things about cats; they're fluffy and inscrutable!  But somebody has tried! It's [here](https://www.catfacts.net/api). Let's build a client using the autoconfigured declarative HTTP clients in Spring Framework 7. 

```java
record CatFact(@JsonProperty("fact_number") int factNumber, String fact) {
}

record CatFactsResponse(Collection<CatFact> facts) {
}

interface CatFacts {

    @GetExchange("https://www.catfacts.net/api")
    CatFactsResponse facts();
}
```

It was possible, but painful, to create this declarative interface before. You had to create an `HttpServiceProxyFactory` bean. Now, you need only _import_ the HTTP client, like we have here:

```java
...
@ImportHttpServices(CatFacts.class)
@SpringBootApplication
public class DogsServiceApplication {
``` 

You could easily build a controller that uses this new declarative interface just by injecting the interface. 

```java
@Controller
@ResponseBody
class CatsController {

    private final CatFacts facts;

    CatsController(CatFacts facts) {
        this.facts = facts;
    }
    
    @GetMapping("/facts")
    Collection<CatFact> facts() {
        return this.facts.facts().facts();
    }
}
```

## Resilient Methods 

This _should_ work just fine! Except... as I write this, Amazon  Web Services `us-east-1`  is down. The lesson? Even the absolute best-run services sometimes fail.  While I hope the cat facts API always, em, _lands on its feet_, it might not. Let's   use the new resilient methods in Spring Framework 7 to add retries and concurrency throtlling. 


```java

    ...

    @ConcurrencyLimit(10)
    @Retryable(maxAttempts = 4, includes = BadException.class)
    @GetMapping("/facts")
    Collection<CatFact> facts()  throws Exception{
        if (this.count.incrementAndGet() < 3) {
            IO.println("trying...");
            throw new BadException();
        }
        IO.println("done");
        return this.facts.facts().facts();
    }
    ...


class BadException extends Exception {
}
```
I've injected some failure into this to demonstrate. Call the endpoint and it'll fail a few times before finally succeeding.  Spring will automatically _retry_ all requests because we've used the new `@Retryable` annotation on the method. We could've equally well-used the new `RetryTemplate`.  
 
We also want to make sure that we don't saturate the downstream service,  so we've  limited the number of concurrent threads that may call this method at the same time to 10 threads using the new `@ConcurrencyLimit` annotation. 

Enable resilient methods like this: 

```java
...
@EnableResilientMethods
@SpringBootApplication
public class DogsServiceApplication {
```

## Bean Registrars 

What if you want to dynamically register beans with Spring? What if you want to minimize reflection? Java configuration and component scanning are very powerful, but they're not dynamic, and they rely heavily on reflection when run on the JVM. In Spring Framework 7, we've introduced `BeanRegistrar`s. Here's an example where we register the same bean twice, once by convention and once with explicit configuration.

```java

class MyBeanRegistrar implements BeanRegistrar {

    @Override
    public void register(BeanRegistry registry, Environment env) {
        registry.registerBean(Runner.class);
        registry.registerBean(Runner.class, spec -> spec
                .description("description")
                .supplier(supplierContext -> {
                    var dogRepository = supplierContext.bean(DogRepository.class);
                    var catFacts = supplierContext.beanProvider(CatFacts.class);
                    return new Runner(dogRepository, catFacts.getIfUnique());
                })
        );
    }
}
```

Activate this configuration by importing it: 

```java
@Import(MyBeanRegistrar.class)
@SpringBootApplication
public class DogsServiceApplication {
```

## Multi Factor Auth in Spring Security 

We're almost ready to take this application production. But before we do, we'll need some security. I've got the latest version of Spring Security on the classpath along with the Spring Security WebAuthn starter. The WebAuthn support isn't new, but the Spring Boot starter for it is. I've configured a `UserDetailsService` and `PasswordEncoder`, which I won't reprint here. I want to customize the authorization. I can do this as before by defining a bean of type `SecurityFilterChain` , but this will undo all the defaults Spring Boot gave me. Now, I can alternatively define a `Customizer<HttpSecurity>`, where changes will be _additive_. Here I'm defining one time tokens, and Passkeys (WebAuthN) support.

```java

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

Both WebAuthN and one-time tokens are a good way to affirm that the person accessing our system is who they say they are. But what if we want a stronger signal? What if we want them to satisfy _both_ authentication mechanisms before we trust them? A huge new feature in Spring Security is support for multifactor authentication.  

```java
@EnableGlobalMultiFactorAuthentication(authorities = {
        FactorGrantedAuthority.PASSWORD_AUTHORITY,
        FactorGrantedAuthority.OTT_AUTHORITY
})
@Configuration
class SecurityConfiguration {
    ...
```
With this in place, the user will be asked to authenticate _twice_ before being allowed in. This new multifactor auth (MFA) support means we can require an arbitrary number of authentication factors. 

## Miles to go before we sleep 

This has been a quick and roving tour of some of my favorite features in the next generation of Spring Framework and Spring Boot. I hope that you got something out of this. The code is here for your [reference](https://github.com/joshlong-attic/2025-11-02-jetbrains-jam-article-on-spring-boot-4).

As always, we encourage users to kick the tires and try everything out on the Spring Initializer (just make sure to specify the latest version of Spring Boot that you see there). 

## About the author 
Josh Long works on the Spring team as its number one fan. You can find more of his content - blogs, Youtube content, podcasts, etc., on his [website](https://joshlong.com).