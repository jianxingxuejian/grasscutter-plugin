package org.hff.utils;

import express.http.Response;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.hff.Config;
import org.hff.MyPlugin;
import org.hff.api.ApiCode;
import org.hff.api.ApiResult;
import org.hff.i18n.Locale;
import org.hff.permission.RoleEnum;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class JwtUtil {

    private static final Config config = MyPlugin.config;

    public static String generateToken(@NotNull String username, @NotNull RoleEnum role) {
        long jwtExpire = config.getJwtExpire();

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expireDate = new Date(nowMillis + jwtExpire);

        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .setIssuer("grasscutter-plugin")
                .setSubject("grasscutter-tools")
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .setClaims(claims)
                .signWith(getKey())
                .compact();
    }

    public static Claims parseToken(String token, @NotNull Locale locale, @NotNull Response response) {
        if (token == null) {
            response.json(ApiResult.result(ApiCode.AUTH_FAIL, locale));
            return null;
        }
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            response.json(ApiResult.result(ApiCode.TOKEN_EXPIRED, locale));
        } catch (Exception e) {
            response.json(ApiResult.result(ApiCode.TOKEN_PARSE_EXCEPTION, locale));
        }
        return null;
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
