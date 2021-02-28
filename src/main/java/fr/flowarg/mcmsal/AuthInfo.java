package fr.flowarg.mcmsal;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Adapted from MiniLauncher (a project of MiniDigger), see his amazing work on GitHub !
public class AuthInfo implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String token;
    private final UUID uuid;
    private final Map<String, String> properties;
    private final String userType;

    // FlowArg
    // TODO make a store object
    private final String storeJson;

    public AuthInfo(String username, String token, UUID uuid, Map<String, String> properties, String userType, String storeJson)
    {
        Objects.requireNonNull(username);
        Objects.requireNonNull(token);
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(properties);
        Objects.requireNonNull(userType);

        this.username = username;
        this.token = token;
        this.uuid = uuid;
        this.properties = properties;
        this.userType = userType;

        this.storeJson = storeJson;
    }

    public String getUsername()
    {
        return this.username;
    }

    public String getToken()
    {
        return this.token;
    }

    public UUID getUUID()
    {
        return this.uuid;
    }

    public Map<String, String> getProperties()
    {
        return this.properties;
    }

    public String getUserType()
    {
        return this.userType;
    }

    public String getStoreJson()
    {
        return this.storeJson;
    }

    @Override
    public String toString()
    {
        return String.format("AuthInfo [username=%s, token=%s, uuid=%s, properties=%s, userType=%s]",
                             this.username,
                             this.token,
                             this.uuid,
                             this.properties,
                             this.userType);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
            return true;
        if (obj instanceof AuthInfo)
        {
            final AuthInfo another = (AuthInfo)obj;
            return Objects.equals(this.username, another.username) &&
                    Objects.equals(this.token, another.token) &&
                    Objects.equals(this.uuid, another.uuid) &&
                    Objects.equals(this.properties, another.properties) &&
                    Objects.equals(this.userType, another.userType);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return this.token.hashCode();
    }
}
