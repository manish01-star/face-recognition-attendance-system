package com.college.attendance.security;

import com.college.attendance.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;

    /*
     * Token expiration:
     * 24 Hours
     */
    private final long expiration =
            24 * 60 * 60 * 1000L;


    public JwtService(
            @Value("${jwt.secret}") String secret) {

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }


    /**
     * ============================================================
     * GENERATE JWT TOKEN
     * ============================================================
     */
    public String generateToken(User user) {

        Date now = new Date();

        Date expiry = new Date(
                now.getTime() + expiration
        );

        return Jwts.builder()

                /*
                 * Username
                 */
                .subject(
                        user.getUsername()
                )

                /*
                 * User ID
                 */
                .claim(
                        "userId",
                        user.getId()
                )

                /*
                 * User Role
                 *
                 * ADMIN
                 * TEACHER
                 * STUDENT
                 */
                .claim(
                        "role",
                        user.getRole().name()
                )

                .issuedAt(now)

                .expiration(expiry)

                .signWith(secretKey)

                .compact();
    }


    /**
     * ============================================================
     * EXTRACT USERNAME
     * ============================================================
     */
    public String extractUsername(String token) {

        return getClaimsFromToken(token)
                .getSubject();
    }


    /**
     * ============================================================
     * EXTRACT CLAIMS
     * ============================================================
     */
    public Claims getClaimsFromToken(String token) {

        return Jwts.parser()

                .verifyWith(secretKey)

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }


    /**
     * ============================================================
     * VALIDATE TOKEN
     * ============================================================
     */
    public boolean isTokenValid(String token) {

        try {

            Claims claims =
                    getClaimsFromToken(token);

            Date expirationDate =
                    claims.getExpiration();

            return expirationDate != null
                    && expirationDate.after(
                            new Date()
                    );

        } catch (Exception e) {

            return false;
        }
    }
}