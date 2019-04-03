package com.github.jhejderup.data;

import java.io.Serializable;

public class JDKClassPath extends Namespace implements Serializable {

    public final String version;
    public final String vendor;

    public JDKClassPath(){
        super(Runtime.class.getPackage().getImplementationVersion(),Runtime.class.getPackage().getImplementationVendor());
        this.version = Runtime.class.getPackage().getImplementationVersion();
        this.vendor = Runtime.class.getPackage().getImplementationVendor();
    }
}
