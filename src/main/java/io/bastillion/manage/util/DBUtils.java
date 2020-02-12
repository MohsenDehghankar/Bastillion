/**
 * Copyright (C) 2013 Loophole, LLC
 * <p>
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * As a special exception, the copyright holders give permission to link the
 * code of portions of this program with the OpenSSL library under certain
 * conditions as described in each individual source file and distribute
 * linked combinations including the program with the OpenSSL library. You
 * must comply with the GNU Affero General Public License in all respects for
 * all of the code used other than as permitted herein. If you modify file(s)
 * with this exception, you may extend this exception to your version of the
 * file(s), but you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version. If you delete this
 * exception statement from all source files in the program, then also delete
 * it in the license file.
 */
package io.bastillion.manage.util;

import java.io.File;
import java.io.FileWriter;
import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class to create and close database resources
 */
public class DBUtils {

    private static Logger log = LoggerFactory.getLogger(DBUtils.class);

    private DBUtils() {
    }

    /**
     * returns DB connection
     *
     * @return DB connection
     */
    public static Connection getConn() {
        Connection con = null;
        try {
            con = DSPool.getDataSource().getConnection();


            //testing database
            /*PreparedStatement st = con.prepareStatement("select * from session_log");
            ResultSet rs = st.executeQuery();
            FileWriter file = new FileWriter("database-test-result2.txt");
            ResultSetMetaData metaData = rs.getMetaData();
            int col_num = metaData.getColumnCount();
            for (int i = 1; i <= col_num; i++) {
                file.write(metaData.getColumnName(i) + " - ");
            }
            file.write("\n");
            while (rs.next()) {
                for (int i = 1; i <= col_num; i++) {
                    file.write(metaData.getColumnName(i) + " : " + rs.getString(metaData.getColumnName(i)) + " ");
                }
                file.write("\n\n");
            }
            file.flush();
            file.close();*/


        } catch (Exception ex) {
            log.error(ex.toString(), ex);
        }
        return con;

    }

    /**
     * close DB connection
     *
     * @param con DB connection
     */
    public static void closeConn(Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        } catch (Exception ex) {
            log.error(ex.toString(), ex);
        }


    }

    /**
     * Close DB statement
     *
     * @param stmt DB statement
     */
    public static void closeStmt(Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (Exception ex) {
            log.error(ex.toString(), ex);
        }


    }

    /**
     * close DB result set
     *
     * @param rs DB result set
     */
    public static void closeRs(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (Exception ex) {
            log.error(ex.toString(), ex);
        }


    }

}
