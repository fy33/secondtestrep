package org.springframework.core.io;

import org.springframework.lang.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class DescriptiveResource extends AbstractResource {
    private final String description;


    public DescriptiveResource(@Nullable String description) {
        this.description = (description != null ? description : "");
    }


    @Override
    public boolean exists() {
        return false;
    }



    @Override
    public boolean isReadable() {
        return false;
    }



    @Override
    public InputStream getInputStream() throws IOException {
        throw new FileNotFoundException(
                getDescription() + " cannot be opened because it does not point to a readable resource");
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return (this == other || (other instanceof DescriptiveResource &&
                ((DescriptiveResource) other).description.equals(this.description)));
    }


    @Override
    public int hashCode() {
        return this.description.hashCode();
    }



















}
