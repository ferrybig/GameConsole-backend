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
public interface User {
	public long getId();
	
	public boolean isAdmin();
	
	public String getFullName();
	
}
