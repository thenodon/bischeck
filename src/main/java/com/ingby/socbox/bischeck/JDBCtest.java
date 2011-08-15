package com.ingby.socbox.bischeck;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JDBCtest {
	static public void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		Class.forName("com.ibm.as400.access.AS400JDBCDriver").newInstance();
		//Connection conn = DriverManager.getConnection("jdbc:as400://dax.dhl.com/PP01DAT01/prompt=false&user=andhaal&password=dsmprag10&error=full");
		Connection conn = DriverManager.getConnection("jdbc:as400://dax.dhl.com;user=andhaal;password=dsmprag10");
		Statement stat = conn.createStatement();
			stat.setQueryTimeout(10);
		ResultSet res = stat.executeQuery("select count(*) from PP01DAT01.EDISTAT where datercv='20110112'");
		if (res.next())
			System.out.println(res.getString(1));
		conn.close();
		
		Class.forName("com.ibm.as400.access.AS400JDBCDriver").newInstance();
		conn = DriverManager.getConnection("jdbc:as400://dax.dhl.com;user=andhaal;password=dsmprag10");
		stat = conn.createStatement();
			stat.setQueryTimeout(10);
		res = stat.executeQuery("select count(*) from PP01DAT01.EDISTAT where datercv='20110111'");
		if (res.next())
			System.out.println(res.getString(1));
		conn.close();
		
		System.exit(0);
	}
}
