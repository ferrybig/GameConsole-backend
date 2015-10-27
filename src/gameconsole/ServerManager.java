/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gameconsole;

import gameconsole.AuthManager.AuthToken;

/**
 *
 * @author Fernando
 */
public interface ServerManager {
	public Server getServer(AuthToken token, String name);
}
