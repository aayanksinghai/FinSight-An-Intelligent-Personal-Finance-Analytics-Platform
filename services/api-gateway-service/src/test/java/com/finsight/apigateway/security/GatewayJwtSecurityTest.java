package com.finsight.apigateway.security;

import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayJwtSecurityTest {

    private static final String PRIVATE_KEY_PEM = """
            -----BEGIN PRIVATE KEY-----
            MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCtbsVdAIl55Sb+
            VrziA9C9lX3V+xZuF79REaTOMoNNX+3QfWFth4P+0mWokydFyYeGLqBsmI9QuPiy
            J98fT661l3dc5L3E5rxSO7FzUM8tYC3fCCYj8hio+a6FqWKQ02wDBRHZP/0MecWa
            mX89eVP1oVUOxbIbSI3wpvcXzbmvTAjlQaJp344znSsJPsiMP1FOq4K0zktp5foj
            ZonMg346X3bL9SYwBkWaSfN4n7fust9C1RHP4IXUew6VAxyoAa68dZ02lpvAX8Hf
            1eUrn2VY4BeU/KjMYfXQAjNYe+GIoqj2qyxcN+22GmOpkoPYNA1BRQNxp5g4wPpp
            px90hyc9AgMBAAECggEAJcY/7f0BNH/FAbYeL0Mh+rz9+raU4NdCyBNF+FRXtWlc
            uKGqcAsJZJ3qXvBoKCLYJhFvRx/mbTBQmKsnBaCbHrm2GZFHxQR1hZqzUHjMGye8
            7fxOJFulY954FOw7B+zItJKs28r8YHSAZecjZ94Oe6xPRKeqHpertLwFCxX3qNaT
            tidRXKIqC4l4jAPyn1zYQHRquRAM0pvnjQH4W8Ebng2AHVmG0zIUD/H9zen3KRMR
            Pq1EU6LRq11T1nWtFsPZRoi6iim1Gz3tmBZDkNITMSHV7VPUUTmKUsWnlIkNOuVf
            6lHcYV14oyCqFMOC13r6EsJkckuUtru4tWLCSsbLUQKBgQD0gCItJN140jZNnhvs
            b92fuPrhYwcVPkHl7hGxIkCRUodpdiTBlCjIDhEfT0UMgDinIyd3tLsUOKCtI5pZ
            jhyyMxoDtf305ZIgMlmCLQbYmoBsXopxRam/dPvYWQS/NPzd/5cSiEFZstlsuJVy
            LtUwXx8huSPqBzTV8jYRmHES8QKBgQC1lvUh9MqncYrgK/LsVf8WngNSjCd5/IzU
            IDabXH54SxhiXWb4tQLNgyVuxo/t0cj3yMJGmcvcScow88UECJ7LhiJPtW+AP1IG
            s6vbNjLu6/it8wD2ja9mtKAdODCCF8WTchzKfuFwHqbwuCu1wKJ869L+hkZxfAaQ
            S5DSXJpBDQKBgQCwW9xLA9RcWgShZ/JsUSRjGWKdQHjTcdzGpTpNso8wQ385KubI
            Wr4vzzW6h8segT5cEJdfWRW4OEBtosiq4D8CqqmRE6zjANSuNKMrqp7NjmQjxu3F
            VqJiX1dpboxl/yqGVt35mB7LPJ5oNZxWiEFuaq5h/NFS9W+/Ar7NYk/tgQKBgHk9
            4kuaspgb0jqcfWRYgqmqNNOvqkfjXG/hVjRjDDnrAcvjSvYxfT6UGrEcuEp0MDdo
            fnY/B4L8bZhDmj54NvXyiAQqQtkg7fZ1jgAd7uHhAbe6sODtdrgfT2xS8OhbUNqC
            bUCyeHiwrou4m86NYLABkG3KX0w0H/nFylr54zAdAoGBAN1P3NSbiciEP+MbO+fm
            IvZOG22k5hY/6dBWPoFx5OP5qM3TkAQLjpSpE3b+9nWTqHw6PfGjfu/Ie5QiquH4
            KWnB+f9hTiwSfWts1ziZ+CqvdM26h7vSMC8KJQiBwmNiVXP2bkrYD//EUVc4Tlcv
            cqgFzJzbvaNHKSVPDEVQkFhP
            -----END PRIVATE KEY-----
            """;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void protectedApiShouldRejectRequestWithoutToken() {
        webTestClient.get()
                .uri("/api/secure/probe")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedApiShouldAcceptValidRs256Token() throws Exception {
        String token = Jwts.builder()
                .subject("test-user")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(parsePrivateKey(PRIVATE_KEY_PEM), Jwts.SIG.RS256)
                .compact();

        webTestClient.get()
                .uri("/api/secure/probe")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                // Security passes; 404 is expected because no route matches /api/secure/** yet.
                .expectStatus().isNotFound();
    }

    private PrivateKey parsePrivateKey(String privateKeyPem) throws Exception {
        String normalized = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }
}

