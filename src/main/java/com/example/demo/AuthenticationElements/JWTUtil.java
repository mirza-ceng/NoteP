/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.demo.AuthenticationElements;

import org.springframework.stereotype.Component;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author 2005m
 */
@Component
public class JWTUtil {

    private final SecretKey secretKey;

    public JWTUtil(@Value("${jwt.secretSTR}") String secretSTR) {

        secretKey = Keys.hmacShaKeyFor(secretSTR.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String eMail) {
        return Jwts.builder()
                .subject(eMail)
                .issuedAt(new Date())
                .expiration(
                        new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 5)//5 saat loggedIn süresi
                )
                .signWith(secretKey).compact();

    }

    public String extractEMail(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();

    }

    public boolean validateToken(String token, String eMail) {
        String extractedEMail = extractEMail(token);
        boolean isExpired = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration().before(new Date());

        return !isExpired && extractedEMail.equals(eMail);

    }

}
