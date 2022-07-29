package org.hff.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.hff.Config;
import org.hff.MyPlugin;
import org.hff.permission.RoleEnum;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static emu.grasscutter.Grasscutter.getLogger;


public class JwtUtil {

    private static final Config config = MyPlugin.getInstance().getConfig();

    public static String generateToken(@NotNull RoleEnum role, @NotNull String uid) {
        long jwtExpire = config.getJwtExpire();

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expireDate = new Date(nowMillis + jwtExpire);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.getDesc());
        claims.put("uid", uid);

       try{
           return Jwts.builder()
                   .setIssuer("grasscutter-plugin")
                   .setSubject("grasscutter-tools")
                   .setIssuedAt(now)
                   .setExpiration(expireDate)
                   .setClaims(claims)
                   .signWith(getKey())
                   .compact();
       }catch (Exception e){
           getLogger().error("JwtUtil generateToken error", e);
       }
       return null;
    }

    public static Claims parseToken(@NotNull String token) {
        return Jwts.parserBuilder()
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
