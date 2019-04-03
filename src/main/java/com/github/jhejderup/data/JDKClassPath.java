package com.github.jhejderup.data;

import java.io.Serializable;
import java.util.Objects;

public class JDKClassPath extends Namespace implements Serializable {

    public final String version;
    public final String vendor;

    public JDKClassPath(){
        super(Runtime.class.getPackage().getImplementationVersion(),Runtime.class.getPackage().getImplementationVendor());
        this.version = Runtime.class.getPackage().getImplementationVersion();
        this.vendor = Runtime.class.getPackage().getImplementationVendor();
    }

    @Override
    public boolean equals(Object o) {
        // self check
        if (this == o)
            return true;
        // null check
        if (o == null)
            return false;
        // type check and cast
        if (getClass() != o.getClass())
            return false;
        JDKClassPath jdk = (JDKClassPath) o;
        return super.equals(o) &&
                Objects.equals(this.vendor, jdk.vendor) &&
                Objects.equals(this.version, jdk.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.segments,this.vendor,this.version);
    }
}
