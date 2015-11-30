/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gameconsole;

import gameconsole.AuthManager.AuthToken;
import java.util.Collection;

/**
 *
 * @author Fernando
 */
public interface ServerManager {
	public Server getServer(AuthToken token, String name);
	
	public Collection<Server> serverListing(AuthToken token);

	public Collection<Server> getAllServers();

	public boolean deleteServer(AuthToken token, String name);

}
