/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gameconsole;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import java.util.Set;

/**
 *
 * @author Fernando
 */
public interface HttpServerRouter {
	
	public void addRoute(Route route);
	
	public Set<Route> getRoutes();
	
	public void removeRoute(Route route);
	
	public interface Route {
		public String getRouteName();
		
		public boolean acceptsMethod(HttpMethod method);
		
		public int maxDataSize();
		
		public HttpResponse proccessRequest(String route, HttpHeaders headers, byte[] data);
	}
}
