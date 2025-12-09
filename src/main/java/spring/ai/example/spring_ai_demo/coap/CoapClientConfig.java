package spring.ai.example.spring_ai_demo.coap;

import org.eclipse.californium.core.CoapClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoapClientConfig {

    @Bean
   public CoapClient coapClient() {
                return new CoapClient("coap://localhost/example");
    }
}

