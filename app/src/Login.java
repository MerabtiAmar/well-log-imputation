import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;

public class Login extends JFrame {
    private final JTextField usernameField;
    private final JPasswordField passwordField;

    public Login() {
        // Set up the frame
        setTitle("Login");
        setSize(420, 300);
        setResizable(false);
        setLayout(new BorderLayout());

        // Create a panel for the form elements
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 2, 2));
        formPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // Create the username label and text field
        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField(20);

        // Create the password label and text field
        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField(20);
        passwordField.setEchoChar('*');

        // Create the login button
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            // Check user credentials using MySQL
            UserCredentials credentials = checkCredentials(username, password);
            if (credentials.authenticated()) {
                String userType = credentials.userType();
                MainApp mainApp = new MainApp(userType);
                mainApp.app();
                setVisible(false);
            } else {
                JOptionPane.showMessageDialog(Login.this, "Invalid username or password", "Login Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Create the exit button
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> System.exit(0)); // Exit the application when clicked

        // Add components to the form panel
        formPanel.add(usernameLabel);
        formPanel.add(usernameField);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);

        // Create a panel for the login and exit buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton); // Add the exit button

        // Add panels to the frame
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        ImageIcon icon = new ImageIcon("assets/icon.png");
        setIconImage(icon.getImage());

        // Add image
        ImageIcon imageIcon = new ImageIcon("assets/banner.png");
        Image image = imageIcon.getImage();
        Image resizedImage = image.getScaledInstance((int) (image.getWidth(null) * 0.6),
                (int) (image.getHeight(null) * 0.6), Image.SCALE_SMOOTH);
        ImageIcon resizedImageIcon = new ImageIcon(resizedImage);
        JLabel imageLabel = new JLabel(resizedImageIcon);
        imageLabel.setToolTipText("Click to open Enageo's website");
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://www.enageo.com/"));
                } catch (IOException | URISyntaxException ex) {
                    ex.printStackTrace();
                }
            }
        });
        add(imageLabel, BorderLayout.NORTH);

        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(loginButton);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private UserCredentials checkCredentials(String username, String password) {
        try (Connection conn = AppConfig.connect()) {
            String query = "SELECT * FROM users WHERE username=? AND password=?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String userType = rs.getString("user_type");
                        return new UserCredentials(true, userType);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new UserCredentials(false, null);
    }

    private record UserCredentials(boolean authenticated, String userType) {
    }
}
