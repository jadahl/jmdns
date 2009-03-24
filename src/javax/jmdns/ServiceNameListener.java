//Copyright 2009 Jonas Ådahl
//Licensed under Apache License version 2.0

package javax.jmdns;

/**
 * Listener class used for notifications about service names of service
 * being changed.
 */
public interface ServiceNameListener {
    
    /**
     * Called when the name of a service info has been changed.
     */
    public void serviceNameChanged(String newName, String oldName);
}
