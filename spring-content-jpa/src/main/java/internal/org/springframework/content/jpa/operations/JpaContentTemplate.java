package internal.org.springframework.content.jpa.operations;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.operations.ContentOperations;
import org.springframework.content.commons.utils.BeanUtils;

import internal.org.springframework.content.jpa.utils.InputStreamEx;

public class JpaContentTemplate implements ContentOperations, InitializingBean {

	private static Log logger = LogFactory.getLog(JpaContentTemplate.class);
	
	@Autowired
	@PersistenceContext
	private EntityManager manager;
	
	@Autowired
	private DataSource datasource;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		ResultSet rs = datasource.getConnection().getMetaData().getTables(null, null, "BLOBS", new String[] {"TABLE"});
		if (!rs.next()) {
			logger.info("Creating JPA Content Repository");
			
			Statement stmt = datasource.getConnection().createStatement();
			String sql = "CREATE TABLE BLOBS " +
	                "(id INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 1), " +
	                " blob BLOB, " + 
	                " PRIMARY KEY ( id ))"; 

			stmt.executeUpdate(sql);
		}
	}

	@Override
	public <T> void setContent(T metadata, InputStream content) {
		if (BeanUtils.getFieldWithAnnotation(metadata, ContentId.class) == null) {
			
			String sql = "INSERT INTO BLOBS VALUES(NULL, ?);";
			try {
				PreparedStatement pS = datasource.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				InputStreamEx in = new InputStreamEx(content);
				pS.setBinaryStream(1, in);
				pS.executeUpdate();
		        ResultSet set = pS.getGeneratedKeys();
		        set.next();
		        int id = set.getInt("ID");
		        BeanUtils.setFieldWithAnnotation(metadata, ContentId.class, id);
		        BeanUtils.setFieldWithAnnotation(metadata, ContentLength.class, in.getLength());
			} catch (SQLException sqle) {
				logger.error("Error inserting content",sqle);
			}
		} else {
			String sql = "UPDATE BLOBS SET blob=? WHERE id=" + BeanUtils.getFieldWithAnnotation(metadata, ContentId.class);
			try {
				PreparedStatement pS = datasource.getConnection().prepareStatement(sql);
				InputStreamEx in = new InputStreamEx(content);
				pS.setBinaryStream(1, in);
				pS.executeUpdate();
		        BeanUtils.setFieldWithAnnotation(metadata, ContentLength.class, in.getLength());
			} catch (SQLException sqle) {
				logger.error(String.format("Error updating content %s", BeanUtils.getFieldWithAnnotation(metadata, ContentId.class)),sqle);
			}
		}
	}

	@Override
	public <T> void unsetContent(T metadata) {
		String sql = "DELETE FROM BLOBS WHERE id=" + BeanUtils.getFieldWithAnnotation(metadata, ContentId.class);
		try {
			PreparedStatement pS = datasource.getConnection().prepareStatement(sql);
			pS.executeUpdate();
	        BeanUtils.setFieldWithAnnotation(metadata, ContentId.class, null);
	        BeanUtils.setFieldWithAnnotation(metadata, ContentLength.class, 0);
		} catch (SQLException sqle) {
			logger.error(String.format("Error deleting content %s", BeanUtils.getFieldWithAnnotation(metadata, ContentId.class)), sqle);
		}
	}

	@Override
	public <T> InputStream getContent(T metadata) {
		String sql = "SELECT blob FROM BLOBS WHERE id='" + BeanUtils.getFieldWithAnnotation(metadata, ContentId.class) + "'";
		ResultSet set = null;
		try {
	        set = datasource.getConnection().prepareCall(sql).executeQuery();
	        if(!set.next()) return null;
	        Blob b = set.getBlob("blob");           
	        return b.getBinaryStream();
	        
	        // todo: wrap input stream so we can 'free' the blob
	        //b.free();
	    } catch (SQLException sqle) {
			logger.error(String.format("Error getting content %s", BeanUtils.getFieldWithAnnotation(metadata, ContentId.class)), sqle);
		} finally {
	    	if (set != null)
				try {
					set.close();
				} catch (SQLException sqle) {
					logger.error(String.format("Error closing resultset for content %s", BeanUtils.getFieldWithAnnotation(metadata, ContentId.class)), sqle);
				}
	    }
		return null;
	}
}