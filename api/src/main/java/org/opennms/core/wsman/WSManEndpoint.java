package org.opennms.core.wsman;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class WSManEndpoint {

    public static enum WSManVersion {
        WSMAN_1_0,
        WSMAN_1_1,
        WSMAN_1_2
    }

    private final URL url;
    private final String username;
    private final String password;
    private final boolean strictSSL;
    private final WSManVersion serverVersion;

    private WSManEndpoint(Builder builder) {
        url = builder.url;
        username = builder.username;
        password = builder.password;
        strictSSL = builder.strictSSL;
        serverVersion = builder.serverVersion;
    }

    public static class Builder {
       private final URL url;
       private boolean strictSSL = true;
       private String username;
       private String password;
       private WSManVersion serverVersion = WSManVersion.WSMAN_1_2;

       public Builder(String url) throws MalformedURLException {
           this(new URL(Objects.requireNonNull(url, "url cannot be null")));
       }

       public Builder(URL url) {
           this.url = Objects.requireNonNull(url, "url cannot be null");
       }

       public Builder withBasicAuth(String username, String password) {
           this.username = Objects.requireNonNull(username, "username cannot be null");
           this.password = Objects.requireNonNull(password, "password cannot be null");
           return this;
       }

       public Builder withStrictSSL(boolean strictSSL) {
           this.strictSSL = strictSSL;
           return this;
       }

       public Builder withServerVersion(WSManVersion version) {
           this.serverVersion = Objects.requireNonNull(version, "version cannot be null");
           return this;
       }

       public WSManEndpoint build() {
           return new WSManEndpoint(this);
       }
    }

    public URL getUrl() {
        return url;
    }

    public boolean isBasicAuth() {
        return username != null;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isStrictSSL() {
        return strictSSL;
    }

    public WSManVersion getServerVersion() {
        return serverVersion;
    }

    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
            .omitNullValues()
            .add("url", url)
            .add("isBasicAuth", isBasicAuth())
            .add("isStrictSSL", isStrictSSL())
            .add("serverVersion", serverVersion)
            .toString();
    }
}
