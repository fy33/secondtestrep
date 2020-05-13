package org.springframework.core.codec;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.springframework.core.codec.ResourceDecoder.FILENAME_HINT;

public class ResourceDecoder extends AbstractDataBufferDecoder<Resource> {
    public static String FILENAME_HINT = ResourceDecoder.class.getName() + ".filename";

    @Override
    public Resource decode(DataBuffer dataBuffer, ResolvableType elementType,
                           @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);

        if (logger.isDebugEnabled()) {
            logger.debug(Hints.getLogPrefix(hints) + "Read " + bytes.length + " bytes");
        }

        Class<?> clazz = elementType.toClass();
        String filename = hints != null ? (String) hints.get(FILENAME_HINT) : null;
        if (clazz == InputStreamResource.class) {
            return new InputStreamResource(new ByteArrayInputStream(bytes)) {
                @Override
                public String getFilename() {
                    return filename;
                }
                @Override
                public long contentLength() {
                    return bytes.length;
                }
            };
        }
        else if (Resource.class.isAssignableFrom(clazz)) {
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
        }
        else {
            throw new IllegalStateException("Unsupported resource class: " + clazz);
        }
    }
}
