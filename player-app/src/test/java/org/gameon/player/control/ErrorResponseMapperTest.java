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
package org.gameon.player.control;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import mockit.Mocked;
import mockit.Verifications;

public class ErrorResponseMapperTest {

    ObjectMapper om;
    ErrorResponseMapper exMapper;

    @Before
    public void before() throws Exception {
        om = new ObjectMapper();
        exMapper = new ErrorResponseMapper();
        exMapper.mapper = om;
    }
    
    @Test
    public void testException(@Mocked Response response, @Mocked ResponseBuilder builder) {
        exMapper.toResponse(new Exception("TestException"));
        
        new Verifications() {{
            ResponseBuilder rb;
            Status s;
            String json;
            
            rb = Response.status(s = withCapture()); times = 1;
            rb.entity(json = withCapture()); times = 1;
            
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR, s);
            Assert.assertTrue("Stringified json should include status 500: [" + json + "]", 
                    json.contains("\"status\":500"));
            Assert.assertTrue("Stringified json should include message containing exception's message: [" + json + "]", 
                    json.contains("\"message\":\"TestException\""));
            Assert.assertFalse("Stringified json should not include info", 
                    json.contains("more_info"));
        }};
    }

    @Test
    public void testPlayerAccountModificationExceptionMessage(@Mocked Response response, @Mocked ResponseBuilder builder) {
        exMapper.toResponse(new PlayerAccountModificationException("TestException"));
        
        new Verifications() {{
            ResponseBuilder rb;
            Status s;
            String json;
            
            rb = Response.status(s = withCapture()); times = 1;
            rb.entity(json = withCapture()); times = 1;
            
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR, s);
            Assert.assertTrue("Stringified json should include status 500: [" + json + "]", 
                    json.contains("\"status\":500"));
            Assert.assertTrue("Stringified json should include message containing exception's message: [" + json + "]", 
                    json.contains("\"message\":\"TestException\""));
            Assert.assertFalse("Stringified json should not include info: [" + json + "]", 
                    json.contains("more_info"));
        }};
    }
    
    @Test
    public void testPlayerAccountModificationExceptionMessageInfo(@Mocked Response response, @Mocked ResponseBuilder builder) {
        exMapper.toResponse(new PlayerAccountModificationException("TestException", "more_info"));
        
        new Verifications() {{
            ResponseBuilder rb;
            Status s;
            String json;
            
            rb = Response.status(s = withCapture()); times = 1;
            rb.entity(json = withCapture()); times = 1;
            
            Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR, s);
            Assert.assertTrue("Stringified json should include status 500: [" + json + "]", 
                    json.contains("\"status\":500"));
            Assert.assertTrue("Stringified json should include message containing exception's message: [" + json + "]", 
                    json.contains("\"message\":\"TestException\""));
            Assert.assertTrue("Stringified json should include info: [" + json + "]", 
                    json.contains("\"more_info\":\"more_info\""));
        }};
    }

    @Test
    public void testPlayerAccountModificationExceptionMessageInfoStatus(@Mocked Response response, @Mocked ResponseBuilder builder) {
        exMapper.toResponse(new PlayerAccountModificationException(Response.Status.FORBIDDEN, "TestException", "more_info"));
        
        new Verifications() {{
            ResponseBuilder rb;
            Status s;
            String json;
            
            rb = Response.status(s = withCapture()); times = 1;
            rb.entity(json = withCapture()); times = 1;
            
            Assert.assertEquals(Response.Status.FORBIDDEN, s);
            Assert.assertTrue("Stringified json should include status 403: [" + json + "]", 
                    json.contains("\"status\":403"));
            Assert.assertTrue("Stringified json should include message containing exception's message: [" + json + "]", 
                    json.contains("\"message\":\"TestException\""));
            Assert.assertTrue("Stringified json should include info: [" + json + "]", 
                    json.contains("\"more_info\":\"more_info\""));
        }};
    }


    @Test
    public void testDocumentNotFoundException(@Mocked Response response, @Mocked ResponseBuilder builder) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("body", "ex_body");
        
        exMapper.toResponse(new DocumentNotFoundException("TestException", node));
        
        new Verifications() {{
            ResponseBuilder rb;
            Status s;
            String json;
            
            rb = Response.status(s = withCapture()); times = 1;
            rb.entity(json = withCapture()); times = 1;
            
            Assert.assertEquals(Response.Status.NOT_FOUND, s);
            Assert.assertTrue("Stringified json should include status 404: [" + json + "]", 
                    json.contains("\"status\":404"));
            Assert.assertTrue("Stringified json should include message containing exception's message: [" + json + "]", 
                    json.contains("\"message\":\"TestException\""));
            Assert.assertTrue("Stringified json should include info: [" + json + "]", 
                    json.contains("\"more_info\":{\"body\":\"ex_body\"}"));
        }};
    }

    @Test
    public void testUpdateConflictException(@Mocked Response response, @Mocked ResponseBuilder builder) {
        exMapper.toResponse(new UpdateConflictException("TestDocument", "Revision"));
        
        new Verifications() {{
            ResponseBuilder rb;
            Status s;
            String json;
            
            rb = Response.status(s = withCapture()); times = 1;
            rb.entity(json = withCapture()); times = 1;
            
            Assert.assertEquals(Response.Status.CONFLICT, s);
            Assert.assertTrue("Stringified json should include status 409: [" + json + "]", 
                    json.contains("\"status\":409"));
            Assert.assertTrue("Stringified json should include message containing exception's message: [" + json + "]", 
                    json.contains("\"message\":\"document update conflict: id: TestDocument rev: Revision\""));
            Assert.assertFalse("Stringified json should not include info: [" + json + "]", 
                    json.contains("more_info"));
        }};
    }


    @Test
    public void testJsonParseException(@Mocked Response response, @Mocked ResponseBuilder builder) {
        exMapper.toResponse(new JsonParseException("TestException", JsonLocation.NA));
        
        new Verifications() {{
            ResponseBuilder rb;
            Status s;
            String json;
            
            rb = Response.status(s = withCapture()); times = 1;
            rb.entity(json = withCapture()); times = 1;
            
            Assert.assertEquals(Response.Status.BAD_REQUEST, s);
            Assert.assertTrue("Stringified json should include status 400: [" + json + "]", 
                    json.contains("\"status\":400"));
            Assert.assertTrue("Stringified json should include message containing exception's message: [" + json + "]", 
                    json.contains("\"message\":\"TestException\\n at [Source: N/A; line: -1, column: -1]\""));
            Assert.assertFalse("Stringified json should not include info: [" + json + "]", 
                    json.contains("more_info"));
        }};
    }


    @Test
    public void testJsonMappingException(@Mocked Response response, @Mocked ResponseBuilder builder) {
        exMapper.toResponse(new JsonMappingException("TestException"));
        
        new Verifications() {{
            ResponseBuilder rb;
            Status s;
            String json;
            
            rb = Response.status(s = withCapture()); times = 1;
            rb.entity(json = withCapture()); times = 1;
            
            System.out.println(s);
            System.out.println(json);
            
            Assert.assertEquals(Response.Status.BAD_REQUEST, s);
            Assert.assertTrue("Stringified json should include status 400: [" + json + "]", 
                    json.contains("\"status\":400"));
            Assert.assertTrue("Stringified json should include message containing exception's message: [" + json + "]", 
                    json.contains("\"message\":\"TestException\""));
            Assert.assertFalse("Stringified json should not include info: [" + json + "]", 
                    json.contains("more_info"));
        }};
    }
}
