package internal.org.springframework.content.jpa.io;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DelegatingBlobResourceLoader implements ResourceLoader {

	private static Log logger = LogFactory.getLog(DelegatingBlobResourceLoader.class);

	private DataSource ds;
	private Map<String, BlobResourceLoader> loaders;

	private String database = null;

	@Autowired
	public DelegatingBlobResourceLoader(DataSource ds, List<BlobResourceLoader> loaders) {
		this.ds = ds;
		this.loaders = new HashMap<>();
		for (BlobResourceLoader loader : loaders) {
			String database = loader.getDatabaseName();
			if (database != null) {
				this.loaders.put(database, loader);
			}
		}
	}

	@Override
	public Resource getResource(String location) {
		if (database == null) {
			Connection conn = DataSourceUtils.getConnection(ds);
			try {
				database = conn.getMetaData().getDatabaseProductName();
			}
			catch (SQLException e) {
				logger.error("Error fetching database name", e);
			} finally {
				DataSourceUtils.releaseConnection(conn, ds);
			}
		}
		BlobResourceLoader loader = loaders.get(database);
		if (loader == null) {
			loader = loaders.get("GENERIC");
		}
		return loader.getResource(location);
	}

	@Override
	public ClassLoader getClassLoader() {
		return ClassUtils.getDefaultClassLoader();
	}
}
