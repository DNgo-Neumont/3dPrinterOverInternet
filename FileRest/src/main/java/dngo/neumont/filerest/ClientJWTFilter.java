package dngo.neumont.filerest;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class ClientJWTFilter extends OncePerRequestFilter {
    private final String auth_secret = System.getenv("auth_secret");
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        //Prevents running filter on verification and refresh endpoints
        if( request.getServletPath().equals("/user/refreshAuth") || request.getServletPath().equals("/user/auth/")){
            filterChain.doFilter(request, response);
        } else{
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if(authHeader != null && authHeader.startsWith("Bearer ")){
                try{
                    //Will have to decrypt incoming token later - RSA is likely due to public and private keys
                    String token = authHeader.substring("Bearer ".length());
                    //encryption algo
                    Algorithm algorithm = Algorithm.HMAC256(auth_secret.getBytes());
                    //JWT verifier to decrypt and use the JWT
                    JWTVerifier jwtVerifier = JWT.require(algorithm).build();
                    DecodedJWT decodedJWT = jwtVerifier.verify(token);
                    String userName = decodedJWT.getSubject();
                    String[] roles = decodedJWT.getClaim("roles").asArray(String.class);
                    Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    for(String role : roles){
                        authorities.add(new SimpleGrantedAuthority(role));
                    }
                    //All goes well, we verify the JWT and send the request off to whatever it needs to go to
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userName, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    filterChain.doFilter(request, response);
                }catch (Exception e){
                    //Exception handler that sends back whatever caused an exception to the client
                    System.err.println("Error with authentication: " + e.getMessage());
                    e.printStackTrace();
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error",e.getMessage());
                    response.setContentType(APPLICATION_JSON_VALUE);
                    new ObjectMapper().writeValue(response.getOutputStream(), errorResponse);
                }
            }else{
                //continue chain if no auth - meant for things like the AUTH endpoint
                filterChain.doFilter(request, response);
            }
        }

    }
}
