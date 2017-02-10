package gitlogparser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MySqlConn {
	private static final Logger LOGGER = Logger.getLogger( MySqlConn.class.getName() );
	private Connection con;

	private boolean connToDb() {
		boolean status = false;
		//System.out.println(MySqlConn.class.getClassLoader().getResource("logging.properties"));
		try {			
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/codequalitymeasurements?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance",
					"root", "root");
			// here sonoo is database name, root is username and password
			status = true;
		} catch (Exception e) {
			LOGGER.log( Level.SEVERE, e.toString(), e );
		}
		return status;
	}

	public boolean executeDmlStmt(String query) {
		boolean status = false;
		if (connToDb()) {
			try {
				Statement stmt = con.createStatement();
				int rs = stmt.executeUpdate(query);
				stmt.close();
				status = true;
			} catch (Exception e) {
				LOGGER.log( Level.SEVERE, e.toString(), e );
			}finally{
				if(con!=null)
					try {
						con.close();
					} catch (SQLException e) {
						LOGGER.log( Level.SEVERE, e.toString(), e );
					}
			}
		}
		return status;
	}

}
