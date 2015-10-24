/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gameconsole;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;

/**
 *
 * @author Fernando
 */
public class ServerLogRoute implements Route {

	@Override
	public boolean acceptsMethod(HttpMethod method) {
		return HttpMethod.POST == method;
	}

	@Override
	public String getRouteName() {
		return "service/server/log";
	}

	@Override
	public int maxDataSize() {
		return 1024;
	}

	@Override
	public HttpResponse proccessRequest(String route, HttpHeaders headers, byte[] data) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}
