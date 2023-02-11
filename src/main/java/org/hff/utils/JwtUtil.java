package org.hff.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.hff.permission.RoleEnum;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hff.MyPlugin.config;


public class JwtUtil {

    static JwtBuilder jwtBuilder = Jwts.builder()
            .setIssuer("grasscutter-plugin")
            .setSubject("grasscutter-tools");
    static JwtParserBuilder jwtParserBuilder = Jwts.parserBuilder();

    public static String generateToken(@NotNull RoleEnum role, @NotNull String accountId) {
        long jwtExpire = config.getJwtExpire();

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expireDate = new Date(nowMillis + jwtExpire);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.getDesc());
        claims.put("accountId", accountId);

        return jwtBuilder
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .setClaims(claims)
                .signWith(getKey())
                .compact();
    }

    public static Claims parseToken(@NotNull String token) {
        return jwtParserBuilder
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static String generateSecret() {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        return Encoders.BASE64.encode(key.getEncoded());
    }

    private static SecretKey getKey() {
        String secret = config.getSecret();
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
