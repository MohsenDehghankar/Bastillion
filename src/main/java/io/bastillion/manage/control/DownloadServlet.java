package io.bastillion.manage.control;

import io.bastillion.manage.db.UserDB;
import io.bastillion.manage.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@WebServlet("/downloadthis")
public class DownloadServlet extends HttpServlet {
    private final int ARBITARY_SIZE = 1048;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        super.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        String fileName = req.getParameter("fileName");
        String username = req.getParameter("user");
        String token = req.getParameter("token");

       /* System.out.println("user: " + username);
        System.out.println("token: " + token);
        System.out.println("filename: " + fileName);*/

        if (token.equals(UserDB.getUserToken(username))) {
            String attachment = "attachment; filename=" + fileName;
            resp.setHeader("Content-disposition", attachment);
            String addr = "/download/" + fileName;
            try (InputStream in = req.getServletContext().getResourceAsStream(addr);
                 OutputStream out = resp.getOutputStream()) {

                byte[] buffer = new byte[ARBITARY_SIZE];

                int numBytesRead;
                while ((numBytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, numBytesRead);
                }
            }
        }
    }
}
