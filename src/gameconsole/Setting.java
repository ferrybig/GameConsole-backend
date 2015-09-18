/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gameconsole;

import java.util.Objects;

/**
 *
 * @author Fernando
 */
public class Setting {
    private final String name;
    private final Type type;
    private final int length;
    private final boolean readonly;

    public Setting(String name, Type type, int length, boolean readonly) {
        this.name = name;
        this.type = type;
        this.length = length;
        this.readonly = readonly;
    }

    /**
     * @return the length
     */
    public int getLength() {
        return length;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    public boolean isReadonly() {
        return readonly;
    }

    
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.name);
        hash = 29 * hash + Objects.hashCode(this.type);
        hash = 29 * hash + this.length;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Setting other = (Setting) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        if (this.length != other.length) {
            return false;
        }
        return true;
    }
    
    
    
    public enum Type {
        PORT, IP, IPV4, IPV6, SAFE_STRING, NORMAL_STRING, TEXT
    }
}
