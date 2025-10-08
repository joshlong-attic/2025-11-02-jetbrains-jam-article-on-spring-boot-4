package com.example.dogs_service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authorization.EnableGlobalMultiFactorAuthentication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthorities;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.registry.ImportHttpServices;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@EnableGlobalMultiFactorAuthentication(authorities = {
        GrantedAuthorities.FACTOR_PASSWORD_AUTHORITY,
        GrantedAuthorities.FACTOR_OTT_AUTHORITY
})
@EnableResilientMethods
@ImportHttpServices(CatFacts.class)
@Import(MyBeanRegistrar.class)
@SpringBootApplication
public class DogsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DogsServiceApplication.class, args);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    JdbcPostgresDialect jdbcPostgresDialect() {
        return JdbcPostgresDialect.INSTANCE;
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
                        ott
                                .tokenGenerationSuccessHandler((request, response, oneTimeToken) -> {

                                    response.getWriter().println("you've got console mail!");
                                    response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);

                                    IO.println("please go to http://localhost:8080/login/ott?token=" +
                                            oneTimeToken.getTokenValue());

                                })
                );

    }


//
//    @Bean
//    Runner runner(DogRepository repository) {
//        return new Runner(repository);
//    }
}


class MyBeanRegistrar implements BeanRegistrar {

    @Override
    public void register(BeanRegistry registry, Environment env) {

        registry.registerBean(RiskyClient.class);
        registry.registerBean(Runner.class);

        /*registry.registerBean(Runner.class, spec -> spec
                .description("description")
                .supplier(supplierContext -> {
                    var dogRepository = supplierContext.bean(DogRepository.class);
                    return new Runner(dogRepository);
                })
        ); */
    }
}


class BadException extends Exception {
}

class RiskyClient {

    private final AtomicInteger count = new AtomicInteger();

    @ConcurrencyLimit(10)
    @Retryable(maxAttempts = 4, includes = BadException.class)
    void doSomethingThatMightFail() throws Exception {
        if (this.count.incrementAndGet() < 3) {
            IO.println("trying...");
            throw new BadException();
        }
        IO.println("done");
    }

}


// xml, java config, functional config, component scanning
// BeanDefinitions
// beans


@Description("Prints all dogs")
class Runner implements ApplicationRunner {

    private final DogRepository repository;
    private final CatFacts facts;

    private final RiskyClient client;

    Runner(DogRepository repository, CatFacts facts, RiskyClient client) {
        this.repository = repository;
        this.facts = facts;
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        this.repository.findAll().forEach(IO::println);
        this.repository.findByName("Prancer").forEach(IO::println);

        this.facts.facts().facts().forEach(IO::println);

        this.client.doSomethingThatMightFail();
    }
}

interface DogRepository extends ListCrudRepository<@NonNull Dog, @NonNull Integer> {

    @Query("select * from dog where name = :name ")
    Collection<Dog> findByName(String name);

}

record CatFact(@JsonProperty("fact_number") int factNumber, String fact) {
}

record CatFactsResponse(Collection<CatFact> facts) {
}

interface CatFacts {

    @GetExchange("https://www.catfacts.net/api")
    CatFactsResponse facts();
}


// look mom, no Lombok!
record Dog(@Id int id, String name, String description) {
}

@Controller
@ResponseBody
class DogsController {

    private final DogRepository dogRepository;

    DogsController(DogRepository dogRepository) {
        this.dogRepository = dogRepository;
    }

    @GetMapping(value = "/dogs", version = "1.1")
    Collection<Dog> dogs() {
        return this.dogRepository.findAll();
    }

    @GetMapping(value = "/dogs", version = "1.0")
    Collection<Map<String, Object>> dogsClassic() {
        return this.dogRepository.findAll()
                .stream()
                .map(dog -> Map.of("id", (Object) dog.id(),
                        "dogName", dog.name()))
                .toList();
    }


}