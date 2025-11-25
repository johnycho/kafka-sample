package com.sample.kafka.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:8080");
        server.setDescription("로컬 개발 서버");

        Contact contact = new Contact();
        contact.setName("카프카 테스트 팀");
        contact.setEmail("test@example.com");

        Info info = new Info()
                .title("카프카 테스트 API")
                .version("1.0.0")
                .description("임베디드 카프카를 사용한 스프링부트 테스트 프로젝트 API 문서입니다.")
                .contact(contact);

        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}


