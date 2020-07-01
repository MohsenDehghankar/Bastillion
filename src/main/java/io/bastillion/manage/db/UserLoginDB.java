package io.bastillion.manage.db;

import io.bastillion.manage.util.DBUtils;

import javax.swing.plaf.ScrollBarUI;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserLoginDB {
    public static void createTable(Connection con) {
        boolean tableExists = true;
        try {
            Statement statement = con.createStatement();
            statement.executeUpdate("create table if not exists users_login (username varchar(100) not null unique, local_login boolean default false)");
            DBUtils.closeStmt(statement);
            String sql = "select count(*) from users_login";
            PreparedStatement preparedStatement = con.prepareStatement(sql);
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            if (rs.getInt("count(*)") == 0) {
                System.out.println("users_login not existed");
                tableExists = false;
            }
            DBUtils.closeRs(rs);
        } catch (SQLException e) {
            System.out.println("can't create users_login table!");
            e.printStackTrace();
        }
        try {
            if (!tableExists) {
                List<String> noRadiusUsers = AuthDB.getNoRadiusUsers();
                String sql = "select username from users";
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                ArrayList<String> users = new ArrayList<>();
                while (rs.next())
                    users.add(rs.getString(1));
                DBUtils.closeRs(rs);
                DBUtils.closeStmt(ps);
                for (String user : users) {
                    PreparedStatement ps2 = con.prepareStatement("insert into users_login (username, local_login) values (?,?)");
                    ps2.setString(1, user);
                    ps2.setBoolean(2, false);
                    ps2.execute();
                    DBUtils.closeStmt(ps2);
                }
                for (String noRadiusUser : noRadiusUsers) {
                    PreparedStatement ps3 = con.prepareStatement("update users_login set local_login=true where username=?");
                    ps3.setString(1, noRadiusUser);
                    ps3.execute();
                    DBUtils.closeStmt(ps3);
                }
            }
        } catch (SQLException e) {
            System.out.println("error in initializing users_login table");
            e.printStackTrace();
        }
    }

    public static void addUser(Connection con, String username) {
        try {
            PreparedStatement ps = con.prepareStatement("insert into users_login (username, local_login) value (?,?)");
            ps.setString(1, username);
            ps.setBoolean(2, false);
            ps.execute();
            DBUtils.closeStmt(ps);
        } catch (SQLException e) {
            System.out.println("error in adding user to users_login [white error]");
        }
    }

    public static void deleteUser(Connection con, String username) {
        try {
            PreparedStatement ps = con.prepareStatement("delete from users_login where username=?");
            ps.setString(1, username);
            ps.execute();
            DBUtils.closeStmt(ps);
        } catch (SQLException e) {
            System.out.println("error in deleting user from users_login.");
        }
    }

    public static boolean getType(String username, Connection conn) {
        try {
            PreparedStatement ps = conn.prepareStatement("select local_login from users_login where username=?");
            ps.setString(1, username);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next())
                return resultSet.getBoolean(1);
            DBUtils.closeStmt(ps);
        } catch (SQLException e) {
            System.out.println("error in retrieving users login method");
        }
        return false;
    }

    public static void changeType(String username) {
        Connection con = DBUtils.getConn();
        boolean type = getType(username, con);
        try {
            PreparedStatement ps = con.prepareStatement("update users_login set local_login=? where username=?");
            ps.setBoolean(1, !type);
            ps.setString(2, username);
            ps.execute();
            DBUtils.closeStmt(ps);
        } catch (SQLException e) {
            System.out.println("can't change user login type.");
            e.printStackTrace();
        }
    }
}
