
import java.io.PrintWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set CORS headers
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter pw = response.getWriter();
        Gson gson = new Gson();

        // Parse user details from request
        User user = gson.fromJson(request.getReader(), User.class);
        String username = user.getUsername();
        String password = user.getPassword();

        // Validate input
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            pw.write(gson.toJson("User info missing"));
            pw.flush();
            return;
        }

        // Register the user in the database
        int userID = registerUser(username, password);

        if (userID == -1) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            pw.write(gson.toJson("Username is taken"));
        } else if (userID > 0) {
            response.setStatus(HttpServletResponse.SC_OK);
            pw.write(gson.toJson("User registered successfully with ID: " + userID));
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            pw.write(gson.toJson("An unexpected error occurred."));
        }

        pw.flush();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.getWriter().write("RegisterServlet is running!");
    }

    // Method to handle database interaction for user registration
    private static int registerUser(String username, String password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return -3; // Driver not found
        }

        Connection conn = null;
        PreparedStatement checkUserStmt = null;
        PreparedStatement insertUserStmt = null;
        ResultSet rs = null;

        int userID = -1;

        try {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1/WeatherConditions?user=root&password=PASSWORD");

            // Check if username exists
            String checkUserQuery = "SELECT id FROM users WHERE username = ?";
            checkUserStmt = conn.prepareStatement(checkUserQuery);
            checkUserStmt.setString(1, username.trim());
            rs = checkUserStmt.executeQuery();

            if (rs.next()) {
                System.out.println("Username exists: " + username);
                userID = -1; // Username taken
            } else {
                // Insert new user
                System.out.println("Username not found, proceeding to insert.");
                String insertUserQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
                insertUserStmt = conn.prepareStatement(insertUserQuery, PreparedStatement.RETURN_GENERATED_KEYS);
                insertUserStmt.setString(1, username.trim());
                insertUserStmt.setString(2, password.trim());
                insertUserStmt.executeUpdate();

                // Get the auto-generated user ID
                rs = insertUserStmt.getGeneratedKeys();
                if (rs.next()) {
                    userID = rs.getInt(1);
                    System.out.println("New user registered with ID: " + userID);
                }
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (checkUserStmt != null) checkUserStmt.close();
                if (insertUserStmt != null) insertUserStmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return userID;
    }
}
