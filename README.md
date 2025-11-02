# README

Hi, Spring fans! It's November 2025, we're rocking Java 25, and a new generation of Spring, starting with Spring Framework 7, begins arriving this very month. How exciting! There are a ton of new features across the portfolio from Spring Framework 7 on up. Spring Framework 7 is the beginning of the release train of a whole new generation of your favorite projects, including Spring Data, Spring Security, Spring Batch, Spring Integration, etc. These projects all get rolled up into the next generation Spring Boot 4 release, also due later this month. And Spring Boot 4, of course, is itself the foundational layer of a whole new generation of so called post-Boot projects like Spring Cloud, Spring Modulith, and Spring AI. 

## "Dogs and cats living together - MASS HYSTERIA!"
We couldn't possibly hope to look at _everything_, but let's at least look at some of my favorite features together. As always, we'll need a subject on which to focus for our demonstrations. So, with apologies to _Ghostbusters_, we'll build some applications that look at dogs and cats. 

## Portfolio-wide fundamentals 

There are several things you'll notice, portfolio-wide. Let's look at some of them now. 

*Jakarta EE 11*: we've updated to Jakarta EE 11. Unlike the move from Java EE to Jakarta EE in Spring Boot 3, this move should be basically painless. 

*Java*: we've retained a Java 17 baseline but _strongly_ encourage a Java 25 posture whenever possible. And, for GraalVM native image users, we've moved the baseline to GraalVM 25, as well, which has a different configuration format. 

*Kotlin*: we've updated our Kotlin support across the board, now defaulting to Kotlin 2.2. This is an amazing release. We've also updated our support for Kotlin serialization, coroutines, etc. 

*Jackson 3*: the new generation of projects assumes version 3 of Jackson, the popular JSON marshalling library. Jackson 3 is a _very_ incompatible release.  Mercifully, it lives in a completely different package and Maven artifact, so you won't _accidentally_ start using it. It's the default in Spring Boot 4, but we'll have a compatibility starter to preserve Jackson 2 behavior, if required. It is possible to have both Jackson 2 and 3 on the classpath at the same time. 

*Nullability*: Tony Hoare famously called `null` the $1 billion dollar mistake. Null references are a scourge in modern software and its no wonder most languages, like Kotlin, Typescript, Swift, C#, etc., feature explicit nullability semantics - the ability to stipulate whether a reference can be `null` or not. But Java doesn't. At least not yet. There's hope it'll land one day in Project Valhalla. But that day is not today. In the meantime, we've worked hand in hand with Jetbrains' IntelliJ IDEA team and Kotlin team, and with a consortium of other players, to develop  JSpecify, a de-facto set of standardized annotations to indicate whether parts of the Spring portfolio support `null` references. You'll enjoy feedback directly in your IDE, and if you're using Spring from Kotlin, the compiler itself will tell you whether you're able to pass in `null` references. If you use a project like NullAway, you'll get feedback in your build for Java (Maven and Gradle), too! 

*Modularization*: Spring Boot 4's decomposed the modular autoconfiguration artifact of every Spring Boot iteration since the very beginning into smaller autoconfiguration jars that correspond more or less to the starters to which they're attached. The result is that you'll get faster startup and processing time, and we've got new starters that are only possible because of this decomposition. My favorite example? In the past you'd specify `spring-boot-starter-web` if you wanted a web application _or_ if you wanted an HTTP client. Now, you can use `spring-boot-starter-webmvc` to get the webserver behavior and `spring-boot-starter-restclient` to get the HTTP client. Nice! 


## Our Domain Model 

Let's use the new versions of Spring Data, and in particular Spring Data JDBC, to talk to our application's SQL database. Configure the `application.properties` file.

```properties
spring.application.name=pets

spring.datasource.url=jdbc:postgresql://localhost/mydatabase
spring.datasource.username=myuser
spring.datasource.password=secret
```

Now, wire up the Spring Data JDBC repository and entity:

```java

// look mom, no Lombok!
record Dog(@Id int id, String name, String description) {
}

interface DogRepository extends ListCrudRepository<@NonNull Dog, @NonNull Integer> {

    @Query("select * from dog where name = :name ")
    Collection<Dog> findByName(String name);
}
```

Note that we're using `@NonNull` from JSpecify in the generic parameters there. There are a ton of new features in Spring Data, but one of my favorites is that if you're using Spring Data in AOT mode (e.g., for GraalVM native images), Spring Data can _generate_ the repository implementation at compile time. This means that you'll get better and more debuggable implementations _and_ better performance. Win-win! If you want this behavior, be sure to register a dialect so that Spring can determine which dialect to use in generating the SQL. Normally it deduces this at runtime by interacting with the SQL database, but that's not an option here. 

```java
    @Bean
    JdbcPostgresDialect jdbcPostgresDialect() {
        return JdbcPostgresDialect.INSTANCE;
    }
```

## API versioning 
Let's suppose we want to develop a controller that returns the `dog` entities from our database. Here's today's implementation: 

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

This is today's API. Tomorrow will come soon enough, however, and when it does, we'll need to change the API. Oops! We made a mistake, and we've got to break the API. In Spring Framework 7, there's new support for API versioning.

Change the mapping annotation above to look like this:

```java
    @GetMapping(value = "/dogs", version = "1.0")
    ...
```

Now, that API can co-exist with the new API, which clients will get by default. Here's that new API. 

```java
    @GetMapping(value = "/dogs", version = "1.1")
    Collection<Dog> dogs() {
        return this.dogRepository.findAll();
    }
```

It's trivial to stipulate by what mechanism a client can trigger the selection of a particular version. 

```properties
spring.mvc.apiversion.default=1.1
spring.mvc.apiversion.use.query-parameter=api-version
```

This tells the server to serve up the 1.1 version of the API and to select based on a query parameter called `api-version`. There is support for path variables and headers, too. 

## Facts and Cats 

I hesitate to even do this because it's not possible to assert things about cats; they're fluffy and inscrutable! But, if we can assert facts about them, then we can build an API to serve up those facts.  Right? And somebody did! It's [here](https://www.catfacts.net/api), and I  want to integrate it into our new service. Let's build a client we can use to talk to that service using the autoconfigured declarative HTTP clients in Spring Framework 7. 

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

It was possible, but painful, to create this declarative interface before. You had to create an `HttpServiceProxyFactory`. Now, you need only _import_ the HTTP client, like we have here:

```java
...
@ImportHttpServices(CatFacts.class)
@SpringBootApplication
public class DogsServiceApplication {
    ...
``` 

You could easily build a client that uses this new declarative interface just by injecting the interface. 

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

This _should_ work just fine! Except... as I write this, Amazon  Web Services `us-east-1`  is down. That datacenter is one of the most bedrock parts of the internet; it _never_ goes down! But it did, and it was painful. The lesson? Even the absolute best run services sometimes fail. It's just a fact of life. So, while I hope the cat facts API upon which we rely never falters, we have to assume it might. Let's wrap our controller method using the new resilient methods in Spring Framework 7. 


```java

@Controller
@ResponseBody
class CatsController {

    private final CatFacts facts;
    
    private final AtomicInteger count = new AtomicInteger();
    
    CatsController(CatFacts facts) {
        this.facts = facts;
    }

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
}


class BadException extends Exception {
}

```

We've injected some failure into this controller method in the example above. It'll fail three times, and then finally let the call succeed. We can relax, knowing that Spring will automatically _retry_ all requests because we've used the new `@Retryable` annotation on the method. We could've equally well-used the new `RetryTemplate`. Both `RetryTemplate` and `@Retryable` are inspired by the original Spring Retry project.  
 
We also want to make sure that we don't saturate the downstream service,  so we've  limited the number of concurrent threads that may call this method at the same time to 10 threads using the new `@ConcurrencyLimit` annotation. 

Enable resilient methods like this: 

```java
...
@EnableResilientMethods
@SpringBootApplication
public class DogsServiceApplication {
    ...

```

## Bean Registrars 

So far, we've been designating Spring beans with the Spring _stereotype_ annotations (`@Component`, `@Repository`, `@Service`, `@Controller`). You'll probably also recognize Spring's Java configuration component model, where you define `@Bean`-annotated provider methods in a class annotated with `@Configuration`. These two approaches handle most usecases just fine. If you want Spring to implicitly figure out how to construct and wire-up a given type, use `@Component`. If you want to control the creation of the object, or you can't access the source code, use a `@Configuration` class and define the bean in a `@Bean`-provider method. 

But what if you want to define multiple beans in a loop, or perhaps as a result of some external cues, like a file, or a database? Do you want to type less code? Do you want to minimize reflection? Spring Framework 7's new `BeanRegistrar` might be just the ticket for you. 

I've got a class named `Runnner` whose purpose is to call some of the methods we've built up in these examples. It's implementation isn't that important; what matters is that we need to register it as a bean. Let's use `BeanRegistrar`. 

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

In the example above, we've registered the same bean, `Runner`, twice. The first time is akin to just annotating a Spring bean with `@Component`. Spring will deduce which constructor to call and attempt to satisfy the dependencies automatically. In this case, it's a trivial one-liner to register the `Runner` instance! Far less typing than registering a bean using Java configuration. 

In the second instance, we're providing a `Supplier<T>` instance to manually create the `Runner`. We're using the provided `SupplierContext` to look up other beans in the Spring `BeanFactory` or to look up an `ObjectProvider` for a bean in the Spring `BeanFactory`. 

Either way, when the program starts up, we'll have two `Runner` instances. 

## Multi Factor Auth in Spring Security 

We're almost ready to take this application production. But before we do, we'll need some security. I've got the latest version of Spring Security on the classpath along with the Spring Security WebAuthn starter. The WebAuthn support isn't new, but the Spring Boot starter for it is. 

```java

@Configuration
class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    InMemoryUserDetailsManager inMemoryUserDetailsManager(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(User.withUsername("josh")
                .roles("USER")
                .password(passwordEncoder.encode("pw"))
                .build());
    }

    @Bean
    Customizer<HttpSecurity> httpSecurityCustomizer() {
        return httpSecurity -> httpSecurity
                .webAuthn(w -> w
                        .rpId("localhost")
                        .rpName("bootiful")
                        .allowedOrigins("http://localhost:8080")
                )
                .oneTimeTokenLogin(ott ->
                        ott.tokenGenerationSuccessHandler((request, response, oneTimeToken) -> {
                            response.getWriter().println("you've got console mail!");
                            response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);

                            IO.println("please go to http://localhost:8080/login/ott?token=" + oneTimeToken.getTokenValue());
                        })
                );
    }
}
```

The first two beans, `PasswordEncoder` and the `InMemoryUserDetailsManager` are stock standard Spring Security stuff. The `PasswordEncoder` encodes passwords and the `InMemoryUserDetailsManager` answers questions about usernames and passwords. This isn't new. The last one - the `Customizer<HttpSecurity>` - however, is. Normally, in a Spring Security application you configure a `SecurityFilterChain` object to override how the framework is meant to do authorization. Suppose I want to turn on OAuth client support, or customize which HTTP paths get which permissions, or turn on one-time tokens. All of this has historically required created a `SecurityFilterChain`. As soon as you did that, Spring Boot's default autoconfiguration disappeared  and you were left with _no_ defaults. Eeek! In this new generation, you can define beans of type `Customizer<HttpSecurity>`, interact with the `HttpSecurity` object jsut as you would wehn constructing the `SecurityFilterChain`, and the changes are _additive_. You don't have to restipulate everything that Spring Boot gives you by default. 

In this example, we enable two additional forms of authentication: WebAuthn (you've probably have heard of the productized name, _Passkeys_) and one-time tokens. Each of them is a good way to affirm that the person accessing our system is who they say they are. But what if we want a stronger signal? What if we want them to satisfy _both_ authentication mechanisms before we trust them? A huge new feature in Spring Security is support for multifactor authentication. There's a full DSL, if you want it, but for our purposes, the following is enough: 

```java
@EnableGlobalMultiFactorAuthentication(authorities = {
        FactorGrantedAuthority.PASSWORD_AUTHORITY,
        FactorGrantedAuthority.OTT_AUTHORITY
})
@Configuration
class SecurityConfiguration {
    ...
```
With this in place, the user will be asked to authenticate _twice_ before being allowed in. This new multifactor auth (MFA) support means we can require an arbitrary number of authentication factors. It also means we can ask for other, perhaps lesser confidence signals that nonetheless provide interesting information. One of them might be for example that a user has recently logged in. You might have a CAPTCHA that confirms a user is not a bot. These kinds of signals don't tell us that the user is who they say they are, but they _do_ tell us information about the user _in addition_ to the more important signals like that they've presented a valid Passkey or a one-time token or password. 

## Miles to go before we sleep 

This has been a quick and roving tour of some of my favorite features in the next generation of Spring Framework and Spring Boot. I hope that you got something out of this. The code is here for your refernce.

We're in the final weeks in the run up to the new release and while there's still a bit of work to do, things are in a very good shape already. As always, we encourage users to kick the tires and try everything out on the Spring Initializer (just make sure to specify the latest version of Spring Boot that you see there). 