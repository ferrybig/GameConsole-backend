/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gameconsole;

/**
 *
 * @author Fernando
 */
public interface AuthManager {
	public AuthToken getToken(String key);
	
	public AuthToken getNewToken();
	
	public interface AuthToken {
		
		public String getKey();
		
		public boolean mayStopServer(Server server);
		
		public boolean mayStartServer(Server server);
		
		public boolean mayViewServer(Server server);
		
		public boolean mayEditServer(Server server);
		
		public boolean globalEdit();
		
		public boolean allowPasswordChange();
	}
}
