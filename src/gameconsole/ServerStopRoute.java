/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gameconsole;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.json.JSONObject;

/**
 *
 * @author Fernando
 */
/**
 *
 * @author Fernando
 */
public class ServerStopRoute implements Route {

	@Override
	public boolean acceptsMethod(HttpMethod method) {
		return HttpMethod.POST == method;
	}

	@Override
	public String getRouteName() {
		return "server/stop";
	}

	@Override
	public int maxDataSize() {
		return 1024;
	}

	@Override
	public HttpResponse proccessRequest(String uti, HttpRequest reg, WebSocketFrame frame) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}

