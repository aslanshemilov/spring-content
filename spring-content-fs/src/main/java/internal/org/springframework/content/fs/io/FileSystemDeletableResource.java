package internal.org.springframework.content.fs.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.utils.FileService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import static java.lang.String.format;

public class FileSystemDeletableResource implements WritableResource, DeletableResource {

	private static Log logger = LogFactory.getLog(FileSystemDeletableResource.class);

	private final FileSystemResource resource;
	private final FileService fileService;

	public FileSystemDeletableResource(FileSystemResource resource, FileService fileService) {
		this.resource = resource;
		this.fileService = fileService;
	}

	@Override
	public void delete() {

		File parent = null;
		try {
			parent = resource.getFile().getParentFile();
			FileUtils.forceDelete(this.getFile());
		} catch (IOException e) {
			logger.warn(format("Unable to get file for resource %s", resource));
		}

		if (parent != null) {
			try {
				fileService.rmdirs(parent);
			} catch (IOException e) {
				logger.warn(format("Removing orphaned directories starting at %s, left by removal of resource %s", parent.getAbsolutePath(), resource));
			}
		}
	}

	public boolean isOpen() {
		return resource.isOpen();
	}

	public final String getPath() {
		return resource.getPath();
	}

	public boolean exists() {
		return resource.exists();
	}

	public boolean isReadable() {
		return resource.isReadable();
	}

	public InputStream getInputStream() throws IOException {
		return resource.getInputStream();
	}

	public boolean isWritable() {
		return resource.isWritable();
	}

	public long lastModified() throws IOException {
		return resource.lastModified();
	}

	public OutputStream getOutputStream() throws IOException {
		if (!exists()) {
			Files.createDirectories(Paths.get(this.getFile().getParent()));
			Files.createFile(this.getFile().toPath());
		}
		return resource.getOutputStream();
	}

	public URL getURL() throws IOException {
		return resource.getURL();
	}

	public URI getURI() throws IOException {
		return resource.getURI();
	}

	public File getFile() {
		return resource.getFile();
	}

	public long contentLength() throws IOException {
		return resource.contentLength();
	}

	public Resource createRelative(String relativePath) {
		return resource.createRelative(relativePath);
	}

	public String toString() {
		return resource.toString();
	}

	public String getFilename() {
		return resource.getFilename();
	}

	public String getDescription() {
		return resource.getDescription();
	}

	public boolean equals(Object obj) {
		return resource.equals(obj);
	}

	public int hashCode() {
		return resource.hashCode();
	}
}