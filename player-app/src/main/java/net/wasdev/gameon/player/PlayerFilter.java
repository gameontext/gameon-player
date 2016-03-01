/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package net.wasdev.gameon.player;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.wasdev.gameon.auth.JWT;
import net.wasdev.gameon.auth.JWT.AuthenticationState;

@WebFilter(filterName = "playerAuthFilter", urlPatterns = { "/players/*" })
public class PlayerFilter implements Filter {

    @Resource(lookup = "jwtKeyStore")
    String keyStore;

    @Resource(lookup = "jwtKeyStorePassword")
    String keyStorePW;

    @Resource(lookup = "jwtKeyStoreAlias")
    String keyStoreAlias;
    
    public final static String GAMEON_ID = "game-on.org";

    private static Certificate signingCert = null;

    private synchronized void getKeyStoreInfo() throws IOException {
        try {
            // load up the keystore..
            FileInputStream is = new FileInputStream(keyStore);
            KeyStore signingKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            signingKeystore.load(is, keyStorePW.toCharArray());

            // grab the cert we'll use to test signatures
            signingCert = signingKeystore.getCertificate(keyStoreAlias);
        } catch (KeyStoreException e) {
            throw new IOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } catch (CertificateException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            getKeyStoreInfo();
        } catch (IOException io) {
            throw new ServletException(io);
        }
    }

    private final static String jwtParamName = "jwt";
    private final static String jwtHeaderName = "gameon-jwt";


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String playerId = null;
        Map<String, Object> claims = null;
        
        HttpServletRequest req = ((HttpServletRequest) request);
        String jwtHeader = null;
        String jwtParam = null;
        
        //reject the request if multiple jwt headers or parameters were supplied
        for(Enumeration<String> headers = req.getHeaders(jwtHeaderName); headers.hasMoreElements(); ) {
            if(jwtHeader == null) {
                jwtHeader = headers.nextElement();
            } else {
                //multiple header values are an error, so get a bad request
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }
        for(String param : req.getParameterValues(jwtParamName)) {
            if(jwtParam == null) {
                jwtParam = param;
            } else {
                //multiple header values are an error, so get a bad request
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }
        
        JWT jwt = new JWT(signingCert, jwtHeader, jwtParam);
        if(jwt.getState().equals(AuthenticationState.ACCESS_DENIED)) {
            //JWT is not valid, however we let GET requests with no parameters through
            if(!("GET".equals(req.getMethod()) && (req.getQueryString()==null || req.getQueryString().isEmpty()))){
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        } else {
            claims = jwt.getClaims();
            playerId = jwt.getClaims().getSubject();
        }
        request.setAttribute("player.id", playerId);
        request.setAttribute("player.claims", claims);
        chain.doFilter(request, response);        
    }

    @Override
    public void destroy() {

    }

}
