package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.*;

public class PathResource extends AbstractResource implements WritableResource {


    private final Path path;



    public PathResource(Path path) {
        Assert.notNull(path, "Path must not be null");
        this.path = path.normalize();
    }

    public PathResource(String path) {
        Assert.notNull(path, "Path must not be null");
        this.path = Paths.get(path).normalize();
    }

    public PathResource(URI uri) {
        Assert.notNull(uri, "URI must not be null");
        this.path = Paths.get(uri).normalize();
    }

    public final String getPath() {
        return this.path.toString();
    }



    @Override
    public boolean exists() {
        return Files.exists(this.path);
    }

    @Override
    public boolean isReadable() {
        return (Files.isReadable(this.path) && !Files.isDirectory(this.path));
    }


    @Override
    public InputStream getInputStream() throws IOException {
        if (!exists()) {
            throw new FileNotFoundException(getPath() + " (no such file or directory)");
        }
        if (Files.isDirectory(this.path)) {
            throw new FileNotFoundException(getPath() + " (is a directory)");
        }
        return Files.newInputStream(this.path);
    }


    @Override
    public boolean isWritable() {
        return (Files.isWritable(this.path) && !Files.isDirectory(this.path));
    }



    @Override
    public OutputStream getOutputStream() throws IOException {
        if (Files.isDirectory(this.path)) {
            throw new FileNotFoundException(getPath() + " (is a directory)");
        }
        return Files.newOutputStream(this.path);
    }

    @Override
    public URL getURL() throws IOException {
        return this.path.toUri().toURL();
    }


    @Override
    public URI getURI() throws IOException {
        return this.path.toUri();
    }


    @Override
    public boolean isFile() {
        return true;
    }


    @Override
    public File getFile() throws IOException {
        try {
            return this.path.toFile();
        }
        catch (UnsupportedOperationException ex) {
            // Only paths on the default file system can be converted to a File:
            // Do exception translation for cases where conversion is not possible.
            throw new FileNotFoundException(this.path + " cannot be resolved to absolute file path");
        }
    }

    @Override
    public ReadableByteChannel readableChannel() throws IOException {
        try {
            return Files.newByteChannel(this.path, StandardOpenOption.READ);
        }
        catch (NoSuchFileException ex) {
            throw new FileNotFoundException(ex.getMessage());
        }
    }


    @Override
    public WritableByteChannel writableChannel() throws IOException {
        return Files.newByteChannel(this.path, StandardOpenOption.WRITE);
    }

    @Override
    public long contentLength() throws IOException {
        return Files.size(this.path);
    }
    @Override
    public long lastModified() throws IOException {
        // We can not use the superclass method since it uses conversion to a File and
        // only a Path on the default file system can be converted to a File...
        return Files.getLastModifiedTime(this.path).toMillis();
    }
    @Override
    public Resource createRelative(String relativePath) {
        return new PathResource(this.path.resolve(relativePath));
    }


    @Override
    public String getFilename() {
        return this.path.getFileName().toString();
    }


    @Override
    public String getDescription() {
        return "path [" + this.path.toAbsolutePath() + "]";
    }


    @Override
    public boolean equals(@Nullable Object other) {
        return (this == other || (other instanceof PathResource &&
                this.path.equals(((PathResource) other).path)));
    }

    @Override
    public int hashCode() {
        return this.path.hashCode();
    }


}
