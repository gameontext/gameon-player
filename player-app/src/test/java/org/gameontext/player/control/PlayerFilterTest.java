/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
package org.gameontext.player.control;

import static mockit.Deencapsulation.setField;

import java.io.IOException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gameontext.player.control.PlayerFilter;
import org.junit.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;

public class PlayerFilterTest {
    @Tested
    PlayerFilter tested;

    @Injectable String keyStore;
    @Injectable String keyStorePW;
    @Injectable String keyStoreAlias;

    @Test
    public void testValidJwtViaQueryParam(@Mocked HttpServletRequest request,
            @Mocked HttpServletResponse response,
            @Mocked FilterChain chain) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, ServletException{

        Certificate cert = CertificateUtils.getCertificate();
        Key key = CertificateUtils.getKey();

        //plug the cert into the test object
        setField(tested,"signingCert",cert);

        String playerId = "myPlayerId";

        //build the jwt we will test with...
        Claims testClaims = Jwts.claims();
        testClaims.put("aud", "test");
        testClaims.setSubject(playerId);
        String newJwt = Jwts.builder().setHeaderParam("kid", "test").setClaims(testClaims)
                .signWith(SignatureAlgorithm.RS256, key).compact();

        //expected calls..(and mock responses)
        new Expectations() {{
            request.getParameterValues("jwt"); returns(new String[]{newJwt});
        }};

        //invoke the filter
        tested.doFilter(request, response, chain);

        //check that stuff happened that we wanted..
        new Verifications() {{
            request.setAttribute("player.id", playerId); times = 1;
            request.setAttribute("player.claims", any); times = 1;
            chain.doFilter(request, response);
        }};
    }

    @Test
    public void testExpiredJwtViaQueryParam(@Mocked HttpServletRequest request,
            @Mocked HttpServletResponse response,
            @Mocked FilterChain chain) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, ServletException{

        Certificate cert = CertificateUtils.getCertificate();
        Key key = CertificateUtils.getKey();

        //plug the cert into the test object
        setField(tested,"signingCert",cert);

        String playerId = "myPlayerId";

        //build the jwt we will test with...
        Claims testClaims = Jwts.claims();
        testClaims.put("aud", "test");
        testClaims.setSubject(playerId);
        //firmly mark this jwt as expired...
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, -64);
        testClaims.setExpiration(calendar.getTime());
        String newJwt = Jwts.builder().setHeaderParam("kid", "test").setClaims(testClaims)
                .signWith(SignatureAlgorithm.RS256, key).compact();

        //expected calls..(and mock responses)
        new Expectations() {{
            request.getParameterValues("jwt"); returns(new String[]{newJwt});
            request.getMethod(); returns("POST");
            request.getContextPath(); returns ("/accounts");
        }};

        //invoke the filter
        tested.doFilter(request, response, chain);

        //check that stuff happened that we wanted..
        new Verifications() {{
            response.sendError(HttpServletResponse.SC_FORBIDDEN); times = 1;
            chain.doFilter((ServletRequest)any, (ServletResponse)any); times = 0;
        }};
    }

    @Test
    public void testMissingJwtViaMissingQueryParamPOST(@Mocked HttpServletRequest request,
            @Mocked HttpServletResponse response,
            @Mocked FilterChain chain) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, ServletException{

        Certificate cert = CertificateUtils.getCertificate();

        //plug the cert into the test object
        setField(tested,"signingCert",cert);

        //expected calls..(and mock responses)
        new Expectations() {{
            request.getParameterValues("jwt"); returns(new String[]{});
            request.getMethod(); returns("POST");
            request.getContextPath(); returns ("/accounts");
        }};

        //invoke the filter
        tested.doFilter(request, response, chain);

        //check that stuff happened that we wanted..
        new Verifications() {{
            response.sendError(HttpServletResponse.SC_FORBIDDEN); times = 1;
            chain.doFilter((ServletRequest)any, (ServletResponse)any); times = 0;
        }};
    }

    @Test
    public void testMissingJwtViaMissingQueryParamGET(@Mocked HttpServletRequest request,
            @Mocked HttpServletResponse response,
            @Mocked FilterChain chain) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, ServletException{

        Certificate cert = CertificateUtils.getCertificate();

        //plug the cert into the test object
        setField(tested,"signingCert",cert);

        //expected calls..(and mock responses)
        new Expectations() {{
            request.getParameterValues("jwt"); returns(new String[]{});
            request.getMethod(); returns("GET");
            request.getContextPath(); returns ("/accounts");
        }};

        //invoke the filter
        tested.doFilter(request, response, chain);

        //check that stuff happened that we wanted..
        new Verifications() {{
            request.setAttribute("player.id", null); times = 1;
            request.setAttribute("player.claims", any); times = 1;
            chain.doFilter(request, response);
        }};
    }

    public void testValidJwtViaHeaderParam(@Mocked HttpServletRequest request,
            @Mocked HttpServletResponse response,
            @Mocked FilterChain chain) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, ServletException{

        Certificate cert = CertificateUtils.getCertificate();
        Key key = CertificateUtils.getKey();

        //plug the cert into the test object
        setField(tested,"signingCert",cert);

        String playerId = "myPlayerId";

        //build the jwt we will test with...
        Claims testClaims = Jwts.claims();
        testClaims.put("aud", "test");
        testClaims.setSubject(playerId);
        String newJwt = Jwts.builder().setHeaderParam("kid", "test").setClaims(testClaims)
                .signWith(SignatureAlgorithm.RS256, key).compact();

        //expected calls..(and mock responses)
        new Expectations() {{
            request.getHeaders("gameon-jwt"); returns(Collections.enumeration(Arrays.asList(new String[]{newJwt})));
        }};

        //invoke the filter
        tested.doFilter(request, response, chain);

        //check that stuff happened that we wanted..
        new Verifications() {{
            request.setAttribute("player.id", playerId); times = 1;
            request.setAttribute("player.claims", any); times = 1;
            chain.doFilter(request, response);
        }};
    }

    @Test
    public void testExpiredJwtViaHeaderParam(@Mocked HttpServletRequest request,
            @Mocked HttpServletResponse response,
            @Mocked FilterChain chain) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, ServletException{

        Certificate cert = CertificateUtils.getCertificate();
        Key key = CertificateUtils.getKey();

        //plug the cert into the test object
        setField(tested,"signingCert",cert);

        String playerId = "myPlayerId";

        //build the jwt we will test with...
        Claims testClaims = Jwts.claims();
        testClaims.put("aud", "test");
        testClaims.setSubject(playerId);
        //firmly mark this jwt as expired...
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, -64);
        testClaims.setExpiration(calendar.getTime());
        String newJwt = Jwts.builder().setHeaderParam("kid", "test").setClaims(testClaims)
                .signWith(SignatureAlgorithm.RS256, key).compact();

        //expected calls..(and mock responses)
        new Expectations() {{
            request.getHeaders("gameon-jwt"); returns(Collections.enumeration(Arrays.asList(new String[]{newJwt})));
            request.getMethod(); returns("POST");
            request.getContextPath(); returns ("/accounts");
        }};

        //invoke the filter
        tested.doFilter(request, response, chain);

        //check that stuff happened that we wanted..
        new Verifications() {{
            response.sendError(HttpServletResponse.SC_FORBIDDEN); times = 1;
            chain.doFilter((ServletRequest)any, (ServletResponse)any); times = 0;
        }};
    }

    @Test
    public void testMissingJwtViaMissingHeaderParamPOST(@Mocked HttpServletRequest request,
            @Mocked HttpServletResponse response,
            @Mocked FilterChain chain) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, ServletException{

        Certificate cert = CertificateUtils.getCertificate();

        //plug the cert into the test object
        setField(tested,"signingCert",cert);

        //expected calls..(and mock responses)
        new Expectations() {{
            request.getHeaders("gameon-jwt"); returns(Collections.enumeration(Arrays.asList(new String[]{})));
            request.getMethod(); returns("POST");
            request.getContextPath(); returns ("/accounts");
        }};

        //invoke the filter
        tested.doFilter(request, response, chain);

        //check that stuff happened that we wanted..
        new Verifications() {{
            response.sendError(HttpServletResponse.SC_FORBIDDEN); times = 1;
            chain.doFilter((ServletRequest)any, (ServletResponse)any); times = 0;
        }};
    }

    @Test
    public void testMissingJwtViaMissingHeaderParamGET(@Mocked HttpServletRequest request,
            @Mocked HttpServletResponse response,
            @Mocked FilterChain chain) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, ServletException{

        Certificate cert = CertificateUtils.getCertificate();

        //plug the cert into the test object
        setField(tested,"signingCert",cert);

        //expected calls..(and mock responses)
        new Expectations() {{
            request.getHeaders("gameon-jwt"); returns(Collections.enumeration(Arrays.asList(new String[]{})));
            request.getMethod(); returns("GET");
            request.getContextPath(); returns ("/accounts");
        }};

        //invoke the filter
        tested.doFilter(request, response, chain);

        //check that stuff happened that we wanted..
        new Verifications() {{
            request.setAttribute("player.id", null); times = 1;
            request.setAttribute("player.claims", any); times = 1;
            chain.doFilter(request, response);
        }};
    }

}
