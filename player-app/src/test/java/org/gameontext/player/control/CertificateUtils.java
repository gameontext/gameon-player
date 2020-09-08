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

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.junit.Test;

import com.google.api.client.util.PemReader;
import com.google.api.client.util.PemReader.Section;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;

public class CertificateUtils {

    //An RSA PrivateKey/Certificate for use during testing.
    //the key isn't encrypted, as that would be a little pointless
    //since the private key, and all credentials for it are in this
    //one file.
    //We ONLY use this key pair during unit test.

    //expiry date 2030. 
    // openssl req -nodes -new -x509  -keyout /tmp/key.pem -out /tmp/cert.pem -days 3650
    // cat /tmp/cert.pem /tmp/key.pem /tmp/cert.txt
    //then import /tmp/cert.txt into certString as below.

    final static String certString =
    "-----BEGIN CERTIFICATE-----\n"+
    "MIIDzTCCArWgAwIBAgIUShhlFeADqKApkwv7f40HvqJ3w0kwDQYJKoZIhvcNAQEL\n"+
    "BQAwdjELMAkGA1UEBhMCVFQxDTALBgNVBAgMBFRlc3QxDTALBgNVBAcMBFRlc3Qx\n"+
    "DTALBgNVBAoMBFRlc3QxDTALBgNVBAsMBFRlc3QxDTALBgNVBAMMBFRlc3QxHDAa\n"+
    "BgkqhkiG9w0BCQEWDXRlc3RAdGVzdC5jb20wHhcNMjAwOTA4MTg1MDM2WhcNMzAw\n"+
    "OTA2MTg1MDM2WjB2MQswCQYDVQQGEwJUVDENMAsGA1UECAwEVGVzdDENMAsGA1UE\n"+
    "BwwEVGVzdDENMAsGA1UECgwEVGVzdDENMAsGA1UECwwEVGVzdDENMAsGA1UEAwwE\n"+
    "VGVzdDEcMBoGCSqGSIb3DQEJARYNdGVzdEB0ZXN0LmNvbTCCASIwDQYJKoZIhvcN\n"+
    "AQEBBQADggEPADCCAQoCggEBAMbMxnWtFPm8LTX6BAQoXn8IOW4tvJIK/yRUQ8MJ\n"+
    "rQUIpeyU0F4Xb0QWU6g/hA0d1v/ccfCNaimvl8xFblwzJif+u36dv+qDQhmJewjy\n"+
    "91m+NvMU7u3SjUnyyxabALtAXNSaxWX9j4HNTvLaeVwNVB8L5otWeVJ27PMlVwkG\n"+
    "6Ji9DK9iadzOROBQ1tYQb5KLKbzc2GzitCi+MPNWRxwxM4H6FRbxLmGZA/QOvT9n\n"+
    "x6HwTtOOCUGEy/zNXmnV9RmNog/ZQgQFOcDF8OWoP1U+JcciSMm5cS4ql5jyunpD\n"+
    "uW82/kdfyRTa2POcujAqy/fx4T9lLbz18a8S3qZtC+Q56wECAwEAAaNTMFEwHQYD\n"+
    "VR0OBBYEFNYSxWdJGqPtxyMQjdFt+fB1szlLMB8GA1UdIwQYMBaAFNYSxWdJGqPt\n"+
    "xyMQjdFt+fB1szlLMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEB\n"+
    "AIJ9KYDoQYa1LjuD1GPQg8YYVO8sg/4GNe0BEThYzmwenbQVaeAZP+cToHshfncD\n"+
    "hBDYu9NmoES0SA9tcBOLzkAyDuLrMT8C4rtfJGtvHRsGzNoRJuS99xdquY9omkdT\n"+
    "F38Fj24vmrFOMMCWY84STEcbyJR5hLI/j2gxeVWjNza69VOyE4wNE2OuT5qMFR7n\n"+
    "AERU/jfO6x6Kr7La9uWZk9m63UJGKL3f78LVS5YOHRyKlbXD3AI8fKGj6a4Mm3dm\n"+
    "9raECpjvMPoyDt6kIW2R8m/Moz/xqHxpMRStpEm1HXabyhfxAsAxASnuncG0+O+m\n"+
    "zbl7nc0HclgHyMY2Ou7j05c=\n"+
    "-----END CERTIFICATE-----\n"+
    "-----BEGIN PRIVATE KEY-----\n"+
    "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDGzMZ1rRT5vC01\n"+
    "+gQEKF5/CDluLbySCv8kVEPDCa0FCKXslNBeF29EFlOoP4QNHdb/3HHwjWopr5fM\n"+
    "RW5cMyYn/rt+nb/qg0IZiXsI8vdZvjbzFO7t0o1J8ssWmwC7QFzUmsVl/Y+BzU7y\n"+
    "2nlcDVQfC+aLVnlSduzzJVcJBuiYvQyvYmnczkTgUNbWEG+Siym83Nhs4rQovjDz\n"+
    "VkccMTOB+hUW8S5hmQP0Dr0/Z8eh8E7TjglBhMv8zV5p1fUZjaIP2UIEBTnAxfDl\n"+
    "qD9VPiXHIkjJuXEuKpeY8rp6Q7lvNv5HX8kU2tjznLowKsv38eE/ZS289fGvEt6m\n"+
    "bQvkOesBAgMBAAECggEAPyDJqJaUwZTy2mARJGzZTQeEjSsy5UFesd+cQPPyoFWV\n"+
    "suGypR5V884PNK8utKeUHV2YROXzH1emIXSuzdJkPHEUgul/Bu41cDyK+FWHHFVd\n"+
    "x6UPFjA1M5VIzl3cRpnyoIShSHjTOEnE1zNvND77RnyV8gs8rWYcaj2iPLiX5eAb\n"+
    "Mff/mfcZYeVNmIB87XZ0CwoxvkgtbMzxkB8KVU5wHLH19gitg8/AmnQyzTmi5+nj\n"+
    "uRwfi0cr3ecP5D/C4bnSTBdgLJ1QTtAglYBeNJx/zdeJ/HW6Bp8gYddQpzYU5ZzT\n"+
    "vIXwx0WlCUZk3UMfOrPgTZqtoVqp2qfm3Y+YTw5tAQKBgQDn1wZNrac7pE2qhnBB\n"+
    "EfqVAaETH9tYxVXti4T2z6v/nFczvLoWvUyhDOg9hOTf0NMlUlz585CaFqtOzMCL\n"+
    "xOJYXOaPMavtTka4pswJEqhmp4RujYa24p5gLcDqdfYz/G1bUFAEKRse2gwCD3ZB\n"+
    "IQsA+t8xZGQzM2HDJ7fdsG3wUQKBgQDbhFDSqRK/RHeZCAT8Dj7CcuZK3H+azRta\n"+
    "EEM93hIetnXWaq27BSeWoHuFh43wJnlwE5otfJdYAdLq7Xnl8rTCb61Hl6Q9CEVW\n"+
    "djp6xAJl2X7hVTwO728/RGzKac2cudrg6NhsJyVqxT9oHN4Qqo4GRb3OPaBIEWme\n"+
    "aSmmU+TTsQKBgCTwY7a4tm6QTTegV/5mKPDY45sydjZ8qqZAlpzkldkSReqeZV/+\n"+
    "JVl7vv0eUYE/uoS1zM6eeimy9vSFNyCN7Cp8Etg559TVpfsByHyhlmdUxYr/zbkR\n"+
    "/n4AjD5PMT0zORFViIpBKmsN/t/NKuBRrXkof6tU/YoS476+c1NFKx8hAoGBAK7r\n"+
    "5fen8J9nMKJKKKatt0b9hhM7V5eEP3pqIRronanJnWbJxTyVI/G8WhGSbgFitzwe\n"+
    "8qmycWsYsPixWYRp/a4+jWbSKHbV42K9fWYcUQjV4mwunlgMZaqVnNdCrixoUUkN\n"+
    "Yn/0RbWqDhepgS7oqZnH8zKoGtOyxGYNyLmYemxRAoGAWnMjrT8ed61eENbbvuvF\n"+
    "Npmmn12sLHiQMuJEq55NFl9pq1cGaSMcv4qLp7sBCCxZNEVimNalO7S+yHerO6Sy\n"+
    "9UF/7D0sue2XjVNohWx1iYXh8zzM5Cr6pYCz7ruwgWYf2hqt+r89IWbVC0Oo5/1c\n"+
    "/WPOpptTLN9aIjDKues3V2I=\n"+
    "-----END PRIVATE KEY-----\n"+
    "\n";

    /**
     * Since the cert data above is final, we're safe to cache the Cert once we build it
     */
    private static Certificate cert = null;
    /**
     * Since the key data above is final, we're safe to cache the Key once we build it
     */
    private static Key key = null;

    /**
     * Utility method to obtain the test Certficate
     * @return the test Certificate
     * @throws Exception if something fails (it shouldn't)
     */
    public static synchronized Certificate getCertificate() throws CertificateException, IOException {
        if(cert==null){
            Section certSection = PemReader.readFirstSectionAndClose(new StringReader(certString), "CERTIFICATE");
            byte[] certBytes = certSection.getBase64DecodedBytes();
            cert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certBytes));
        }
        return cert;
    }

    /**
     * Utility method to obtain the test private Key.
     * @return the test Key
     * @throws Exception if something fails (it shouldn't)
     */
    public static synchronized Key getKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        if(key==null){
            Section keySection = PemReader.readFirstSectionAndClose(new StringReader(certString), "PRIVATE KEY");
            byte[] keyBytes = keySection.getBase64DecodedBytes();
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            key = kf.generatePrivate(keySpec);
        }
        return key;
    }

    @Test
    public void testCertRead() throws CertificateException, IOException{
        Certificate c = getCertificate();
        assertNotNull("Unable to build certficate for testing",c);
    }

    @Test
    public void testKeyRead() throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException{
        Key k = getKey();
        assertNotNull("Unable to build key for testing",k);
    }

    @Test
    public void testJwt() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, ExpiredJwtException, UnsupportedJwtException, MalformedJwtException, SecurityException, IllegalArgumentException, CertificateException {
        Claims testClaims = Jwts.claims();
        testClaims.put("aud", "test");
        String newJwt = Jwts.builder().setHeaderParam("kid", "test").setClaims(testClaims)
                .signWith(getKey(), SignatureAlgorithm.RS256).compact();
        assertNotNull("could not build jwt using test certificate",newJwt);
        Jws<Claims> jwt = Jwts.parser().setSigningKey(getCertificate().getPublicKey()).parseClaimsJws(newJwt);
        assertNotNull("could not decode jwt using test certificate",jwt);
    }
}
