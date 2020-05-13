package org.springframework.web.servlet.resource;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import java.io.IOException;

public class TransformedResource extends ByteArrayResource {
    @Nullable
    private final String filename;

    private final long lastModified;


    public TransformedResource(Resource original, byte[] transformedContent) {
        super(transformedContent);
        this.filename = original.getFilename();
        try {
            this.lastModified = original.lastModified();
        }
        catch (IOException ex) {
            // should never happen
            throw new IllegalArgumentException(ex);
        }
    }



    @Override
    @Nullable
    public String getFilename() {
        return this.filename;
    }

    @Override
    public long lastModified() throws IOException {
        return this.lastModified;
    }
































}
