package org.springframework.http.converter;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ResourceHttpMessageConverter extends AbstractHttpMessageConverter<Resource> {
    private final boolean supportsReadStreaming;

    public ResourceHttpMessageConverter() {
        super(MediaType.ALL);
        this.supportsReadStreaming = true;
    }


    public ResourceHttpMessageConverter(boolean supportsReadStreaming) {
        super(MediaType.ALL);
        this.supportsReadStreaming = supportsReadStreaming;
    }


    @Override
    protected boolean supports(Class<?> clazz) {
        return Resource.class.isAssignableFrom(clazz);
    }


    @Override
    protected Resource readInternal(Class<? extends Resource> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        if (this.supportsReadStreaming && InputStreamResource.class == clazz) {
            return new InputStreamResource(inputMessage.getBody()) {
                @Override
                public String getFilename() {
                    return inputMessage.getHeaders().getContentDisposition().getFilename();
                }
                @Override
                public long contentLength() throws IOException {
                    long length = inputMessage.getHeaders().getContentLength();
                    return (length != -1 ? length : super.contentLength());
                }
            };
        }
        else if (Resource.class == clazz || ByteArrayResource.class.isAssignableFrom(clazz)) {
            byte[] body = StreamUtils.copyToByteArray(inputMessage.getBody());
            return new ByteArrayResource(body) {
                @Override
                @Nullable
                public String getFilename() {
                    return inputMessage.getHeaders().getContentDisposition().getFilename();
                }
            };
        }
        else {
            throw new HttpMessageNotReadableException("Unsupported resource class: " + clazz, inputMessage);
        }
    }
    @Override
    protected MediaType getDefaultContentType(Resource resource) {
        return MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }



    @Override
    protected Long getContentLength(Resource resource, @Nullable MediaType contentType) throws IOException {
        // Don't try to determine contentLength on InputStreamResource - cannot be read afterwards...
        // Note: custom InputStreamResource subclasses could provide a pre-calculated content length!
        if (InputStreamResource.class == resource.getClass()) {
            return null;
        }
        long contentLength = resource.contentLength();
        return (contentLength < 0 ? null : contentLength);
    }


    @Override
    protected void writeInternal(Resource resource, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        writeContent(resource, outputMessage);
    }


    protected void writeContent(Resource resource, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        try {
            InputStream in = resource.getInputStream();
            try {
                StreamUtils.copy(in, outputMessage.getBody());
            }
            catch (NullPointerException ex) {
                // ignore, see SPR-13620
            }
            finally {
                try {
                    in.close();
                }
                catch (Throwable ex) {
                    // ignore, see SPR-12999
                }
            }
        }
        catch (FileNotFoundException ex) {
            // ignore, see SPR-12999
        }
    }





















}
