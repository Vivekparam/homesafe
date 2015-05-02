package cse403.homesafe.Messaging;

import cse403.homesafe.Data.Contact;
import cse403.homesafe.Data.Location;

/**
 * Interface for the various communication channels available from the device. Provides
 * a consistent method of invocation.
 */
public interface Message {

    /**
     * Sends alert message to the specified recipient through the appropriate subclass
     * communication channel.
     * @param recipient Recipient of the intended message
     * @param location Last location of user
     * @param customMessage Message to be send
     */
    public void sendMessage(Contact recipient, Location location, String customMessage);

}
