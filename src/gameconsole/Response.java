/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gameconsole;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 *
 * @author Fernando
 */
public class Response {
    private final byte[] bytes;
    private final HttpResponseStatus status;
    private final String type;

    public Response(byte[] bytes, HttpResponseStatus status, String type) {
        this.bytes = bytes;
        this.status = status;
        this.type = type;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }
    
    
}
