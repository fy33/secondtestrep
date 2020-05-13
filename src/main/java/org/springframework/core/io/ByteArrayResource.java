package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ByteArrayResource extends AbstractResource {
    private final byte[] byteArray;

    private final String description;

    public ByteArrayResource(byte[] byteArray) {
        this(byteArray, "resource loaded from byte array");
    }


    public ByteArrayResource(byte[] byteArray, @Nullable String description) {
        Assert.notNull(byteArray, "Byte array must not be null");
        this.byteArray = byteArray;
        this.description = (description != null ? description : "");
    }
    public final byte[] getByteArray() {
        return this.byteArray;
    }


    @Override
    public boolean exists() {
        return true;
    }



    @Override
    public long contentLength() {
        return this.byteArray.length;
    }




    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(this.byteArray);
    }

    @Override
    public String getDescription() {
        return "Byte array resource [" + this.description + "]";
    }


    @Override
    public boolean equals(@Nullable Object other) {
        return (this == other || (other instanceof ByteArrayResource &&
                Arrays.equals(((ByteArrayResource) other).byteArray, this.byteArray)));
    }


    @Override
    public int hashCode() {
        return (byte[].class.hashCode() * 29 * this.byteArray.length);
    }





















}
