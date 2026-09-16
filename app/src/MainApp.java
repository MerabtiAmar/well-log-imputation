import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class MainApp {
    private static JTextField[][] trainEntries = new JTextField[0][0];
    private static JTextField[][] predictEntries = new JTextField[0][0];
    private final String user_type;

    public MainApp(String user_type) {
        this.user_type = user_type;
    }

    public void app(){
            SwingUtilities.invokeLater(() -> {
                JFrame window = new JFrame("ENAGEO Prédiction");
                window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int windowWidth = (int) (screenSize.getWidth() * 0.8);
                int windowHeight = (int) (screenSize.getHeight() * 0.8);
                window.setSize(windowWidth, windowHeight);

                int xPosition = (int) ((screenSize.getWidth() - windowWidth) / 2);
                int yPosition = (int) ((screenSize.getHeight() - windowHeight) / 2);
                window.setLocation(xPosition, yPosition);

                ImageIcon icon = new ImageIcon("assets/icon.png"); // Replace with the path to your icon image
                window.setIconImage(icon.getImage());

                // Load the image
                ImageIcon imageIcon = new ImageIcon("assets/banner.png");
                Image image = imageIcon.getImage();
                // Resize the image
                Image resizedImage = image.getScaledInstance((int) (image.getWidth(null) * 0.6),
                        (int) (image.getHeight(null) * 0.6), Image.SCALE_SMOOTH);
                ImageIcon resizedImageIcon = new ImageIcon(resizedImage);

                // Create a label for the image
                JLabel imageLabel = new JLabel(resizedImageIcon);
                imageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                imageLabel.setToolTipText("Click to open Enageo's website");
                imageLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        openWebsite();
                    }
                });
                window.add(imageLabel, BorderLayout.NORTH);

                JTabbedPane tabbedPane = new JTabbedPane();
                window.add(tabbedPane);

                if(Objects.equals(user_type, "admin")) {
                    //admin tab
                    JPanel adminPanel = new JPanel(new BorderLayout());
                    tabbedPane.addTab("Administration", adminPanel);
                    JPanel trainGridPanel = new JPanel();
                    JScrollPane trainScrollPane = new JScrollPane(trainGridPanel);
                    adminPanel.add(trainScrollPane, BorderLayout.CENTER);

                    JPanel adminButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    adminPanel.add(adminButtonsPanel, BorderLayout.NORTH);
                    // Create the buttons
                    JButton createUserButton = new JButton("Create user");
                    JButton deleteUserButton = new JButton("Delete user");
                    JButton modifyUserButton = new JButton("Modify user");
                    JButton logoutButton = new JButton("Log out");

// Create the list of usernames
                    DefaultListModel<String> userModel = new DefaultListModel<>();
                    JList<String> userList = new JList<>(userModel);
                    userList.setCellRenderer(new CustomListCellRenderer());

// Retrieve usernames from the database and populate the user list
                    try {
                        Connection connection = AppConfig.connect();
                        Statement statement = connection.createStatement();
                        ResultSet resultSet = statement.executeQuery("SELECT username FROM users WHERE user_type != 'admin'");

                        while (resultSet.next()) {
                            String username = resultSet.getString("username");
                            userModel.addElement(username);
                        }

                        resultSet.close();
                        statement.close();
                        connection.close();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }

// Action listener for create user button
                    createUserButton.addActionListener(e -> {
                        // Show dialog for creating a user
                        JTextField usernameField = new JTextField();
                        JPasswordField passwordField = new JPasswordField();
                        Object[] fields = {
                                "Username:", usernameField,
                                "Password:", passwordField
                        };
                        int option = JOptionPane.showConfirmDialog(window, fields, "Create User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);

                        // Check if the user clicked "OK"
                        if (option == JOptionPane.OK_OPTION) {
                            String username = usernameField.getText().trim();
                            String password = new String(passwordField.getPassword());

                            // Check if username or password is empty
                            if (!username.isEmpty() && !password.isEmpty()) {
                                try {
                                    Connection connection = AppConfig.connect();

                                    // Check if the username already exists
                                    PreparedStatement checkStatement = connection.prepareStatement("SELECT username FROM users WHERE username = ?");
                                    checkStatement.setString(1, username);
                                    ResultSet checkResult = checkStatement.executeQuery();
                                    if (checkResult.next()) {
                                        JOptionPane.showMessageDialog(window, "Username already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                                        checkResult.close();
                                        checkStatement.close();
                                        connection.close();
                                        return;
                                    }
                                    checkResult.close();
                                    checkStatement.close();

                                    // Insert the user into the database
                                    String query = "INSERT INTO users (username, password, user_type) VALUES (?, ?, ?)";
                                    PreparedStatement pstmt = connection.prepareStatement(query);
                                    pstmt.setString(1, username);
                                    pstmt.setString(2, password);
                                    pstmt.setString(3, "user");
                                    pstmt.executeUpdate();

                                    pstmt.close();
                                    connection.close();

                                    // Refresh the user list
                                    userModel.addElement(username);
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                    // Handle database insert error
                                }
                            } else {
                                // Show error message for empty username or password
                                JOptionPane.showMessageDialog(window, "Username or password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });

// Action listener for delete user button
                    deleteUserButton.addActionListener(e -> {
                        String selectedUsername = userList.getSelectedValue();

                        if (selectedUsername != null) {
                            String message = "Are you sure you want to delete " + selectedUsername + "?";
                            int confirmOption = JOptionPane.showOptionDialog(window, message, "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                            if (confirmOption == JOptionPane.YES_OPTION) {
                                // Delete the user
                                try {
                                    Connection connection = AppConfig.connect();
                                    String query = "DELETE FROM users WHERE username = ?";
                                    PreparedStatement pstmt = connection.prepareStatement(query);
                                    pstmt.setString(1, selectedUsername);
                                    pstmt.executeUpdate();
                                    pstmt.close();
                                    connection.close();

                                    // Remove the user from the user list
                                    userModel.removeElement(selectedUsername);
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                    // Handle database delete error
                                }
                            }
                        } else {
                            JOptionPane.showMessageDialog(window, "No user selected.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });

// Action listener for modify user button
                    modifyUserButton.addActionListener(e -> {
                        String selectedUsername = userList.getSelectedValue();

                        if (selectedUsername != null) {
                            try {
                                Connection connection = AppConfig.connect();
                                PreparedStatement checkStatement = connection.prepareStatement("SELECT username FROM users WHERE username != ?");
                                checkStatement.setString(1, selectedUsername);
                                ResultSet checkResult = checkStatement.executeQuery();
                                List<String> existingUsernames = new ArrayList<>();

                                while (checkResult.next()) {
                                    existingUsernames.add(checkResult.getString("username"));
                                }

                                JTextField usernameField = new JTextField(selectedUsername);
                                JPasswordField passwordField = new JPasswordField();

                                Object[] fields = {
                                        "Username:", usernameField,
                                        "Password:", passwordField
                                };

                                int option = JOptionPane.showConfirmDialog(window, fields, "Modify User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);

                                if (option == JOptionPane.OK_OPTION) {
                                    String newUsername = usernameField.getText().trim();
                                    String newPassword = new String(passwordField.getPassword());

                                    if (!newUsername.isEmpty() && !newPassword.isEmpty()) {
                                        if (existingUsernames.contains(newUsername)) {
                                            JOptionPane.showMessageDialog(window, "Username already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                                            checkResult.close();
                                            checkStatement.close();
                                            connection.close();
                                            return;
                                        }

                                        String updateQuery = "UPDATE users SET username = ?, password = ? WHERE username = ?";
                                        PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
                                        updateStatement.setString(1, newUsername);
                                        updateStatement.setString(2, newPassword);
                                        updateStatement.setString(3, selectedUsername);
                                        updateStatement.executeUpdate();
                                        updateStatement.close();
                                        connection.close();

                                        // Update the user in the user list
                                        userModel.setElementAt(newUsername, userList.getSelectedIndex());
                                    } else {
                                        JOptionPane.showMessageDialog(window, "Username or password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                                    }
                                }

                                checkResult.close();
                                checkStatement.close();
                                connection.close();
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                                // Handle database update error
                            }
                        } else {
                            JOptionPane.showMessageDialog(window, "No user selected.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });

                    logoutButton.addActionListener(e -> {
                        Login loginInterface = new Login();
                        loginInterface.setVisible(true);
                        window.setVisible(false);
                    });

// Add the buttons and the user list to the admin panel
                    adminButtonsPanel.add(createUserButton);
                    adminButtonsPanel.add(deleteUserButton);
                    adminButtonsPanel.add(modifyUserButton);
                    adminButtonsPanel.add(logoutButton);
                    trainGridPanel.setLayout(new BorderLayout());
                    trainGridPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

                }

                // Train tab
                JPanel trainPanel = new JPanel(new BorderLayout());
                tabbedPane.addTab("Train", trainPanel);

                JPanel trainGridPanel = new JPanel();
                JScrollPane trainScrollPane = new JScrollPane(trainGridPanel);
                trainPanel.add(trainScrollPane, BorderLayout.CENTER);

                JPanel trainButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                trainPanel.add(trainButtonsPanel, BorderLayout.NORTH);

                JButton importButton1 = new JButton("Import");
                importButton1.addActionListener(e -> {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
                    int result = fileChooser.showOpenDialog(window);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        // File selected, measure dimensions and adjust grid size
                        String filePath = fileChooser.getSelectedFile().getPath();

                        // Show importing dialog
                        JOptionPane importingDialog = new JOptionPane("Importing...", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
                        JDialog dialog = importingDialog.createDialog(window, "Importing");
                        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                        dialog.setModal(true);

                        SwingWorker<Void, Void> importWorker = new SwingWorker<>() {
                            @Override
                            protected Void doInBackground() {
                                try {
                                    if(!new File(filePath).exists()){
                                        JOptionPane.showMessageDialog(window, "File does not exists");
                                    }else {
                                        int[] dimensions = measureCSVDimensions(filePath);
                                        if (dimensions != null) {
                                            int newTrainRows = dimensions[0];
                                            int newTrainColumns = dimensions[1];
                                            trainEntries = createGridPanel(trainGridPanel, trainScrollPane, newTrainRows - 1, newTrainColumns - 1);
                                            importCSVData(trainEntries, filePath);
                                        } else {
                                            JOptionPane.showMessageDialog(window, "Invalid CSV file format or empty file.");
                                        }
                                    }
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                                return null;
                            }

                            @Override
                            protected void done() {
                                dialog.dispose(); // Close the importing dialog when the task is done
                            }
                        };

                        importWorker.execute(); // Execute the SwingWorker to perform the import task

                        dialog.setVisible(true); // Show the importing dialog
                    }
                });



                trainButtonsPanel.add(importButton1);

                JButton exportButtonTrain = new JButton("Export");
                exportButtonTrain.addActionListener(e -> {
                    if (isGridEmpty(trainEntries)) {
                        JOptionPane.showMessageDialog(window, "Cannot export an empty grid.");
                    } else {
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
                        int result = fileChooser.showSaveDialog(window);
                        if (result == JFileChooser.APPROVE_OPTION) {
                            String filePath = fileChooser.getSelectedFile().getPath();
                            try {
                                exportCSVData(trainEntries, filePath);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });
                trainButtonsPanel.add(exportButtonTrain);

                String[] optionsTrain = {"Regression", "Support Vector Machine", "Neural Networks"};
                JComboBox<String> comboBoxTrain = new JComboBox<>(optionsTrain);
                comboBoxTrain.setRenderer(new TrainStatusCellRenderer());
                trainButtonsPanel.add(comboBoxTrain);

                // Add "Train" button to the train tab
                JButton trainButton = new JButton("Train");
                trainButton.addActionListener(e -> {
                    if (isGridEmpty(trainEntries)) {
                        JOptionPane.showMessageDialog(window, "Cannot train on an empty grid.");
                    } else {
                        String filePath = "temporary_csv_train.csv";
                        if (isGridFull(trainEntries)){
                            try {
                                exportCSVData(trainEntries, filePath);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            String[] command = {
                                    AppConfig.pythonExecutable(),
                                    "train.py",
                                    (String) comboBoxTrain.getSelectedItem()
                            };
                            // Show initial "Please wait" dialog
                            JOptionPane dialog = new JOptionPane("Please wait...", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
                            JDialog dialogBox = dialog.createDialog("Message");
                            dialogBox.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                            // Start a separate thread using SwingWorker for running the Python script
                            SwingWorker<Integer, String> scriptWorker = new SwingWorker<>() {
                                @Override
                                protected Integer doInBackground() throws Exception {
                                    Process process = Runtime.getRuntime().exec(command);
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        publish(line); // Publish each line of output for processing in process() method
                                    }
                                    int exitCode = process.waitFor();
                                    System.out.println("Python program exited with code: " + exitCode);
                                    return exitCode;
                                }
                                @Override
                                protected void process(List<String> chunks) {
                                    for (String line : chunks) {
                                        System.out.println(line); // Print each line of output to the console
                                    }
                                }
                                @Override
                                protected void done() {
                                    try {
                                        int exitCode = get();
                                        boolean isTrained = (exitCode == 0); // Assuming exit code 0 means the script was successful
                                        // Update dialog text based on exit code
                                        if (isTrained) {
                                            dialog.setMessage("Model trained successfully.");
                                            dialog.setMessageType(JOptionPane.INFORMATION_MESSAGE);
                                            // Create OK button
                                            JButton okButton = new JButton("OK");
                                            okButton.addActionListener(e -> dialogBox.dispose()); // Close the dialog when OK button is clicked
                                            dialog.setOptions(new Object[]{okButton}); // Set the OK button as an option in the dialog
                                            dialog.getRootPane().setDefaultButton(okButton);
                                        } else {
                                            dialog.setMessage("Unexpected error has occurred. Exit code: " + exitCode);
                                            dialog.setMessageType(JOptionPane.ERROR_MESSAGE);
                                            // Create OK button
                                            JButton okButton = new JButton("OK");
                                            okButton.addActionListener(e -> dialogBox.dispose()); // Close the dialog when OK button is clicked
                                            dialog.setOptions(new Object[]{okButton}); // Set the OK button as an option in the dialog
                                            dialog.getRootPane().setDefaultButton(okButton);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            };
                            scriptWorker.execute(); // Start the SwingWorker
                            dialogBox.setVisible(true); // Display the dialog
                            // Wait for the scriptWorker to complete
                            try {
                                scriptWorker.get();
                            } catch (InterruptedException | ExecutionException e1) {
                                e1.printStackTrace();
                            }
                        }else{
                            JOptionPane.showMessageDialog(window, "Cannot train on a grid with empty spaces");
                        }
                    }
                });
                trainButtonsPanel.add(trainButton);

                // Add "Clear" button to the train tab
                JButton clearButtonTrain = new JButton("Clear");
                clearButtonTrain.addActionListener(e -> clearGridEntries(trainEntries));
                trainButtonsPanel.add(clearButtonTrain);



                int trainRows = 20;
                int trainColumns = 5;
                trainEntries = createGrid(trainGridPanel, trainRows, trainColumns);

                // Add "Set Dimensions" button to the train tab
                JButton setDimensionsButtonTrain = new JButton("Set Dimensions");
                setDimensionsButtonTrain.addActionListener(e -> {
                    JPanel dimensionsPanel = new JPanel();
                    dimensionsPanel.setLayout(new GridLayout(2, 2));

                    JTextField rowsInputField = new JTextField(5);
                    JTextField columnsInputField = new JTextField(5);

                    dimensionsPanel.add(new JLabel("Rows:"));
                    dimensionsPanel.add(rowsInputField);
                    dimensionsPanel.add(new JLabel("Columns:"));
                    dimensionsPanel.add(columnsInputField);

                    int result = JOptionPane.showConfirmDialog(
                            window,
                            dimensionsPanel,
                            "Set Dimensions",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE
                    );

                    if (result == JOptionPane.OK_OPTION) {
                        String rowsInput = rowsInputField.getText();
                        String columnsInput = columnsInputField.getText();

                        try {
                            int newTrainRows = Integer.parseInt(rowsInput);
                            int newTrainColumns = Integer.parseInt(columnsInput);

                            if (newTrainRows >= 1 && newTrainColumns >= 1) {
                                trainEntries = createGridPanel(trainGridPanel, trainScrollPane, newTrainRows, newTrainColumns);
                            } else {
                                JOptionPane.showMessageDialog(window, "Invalid dimensions entered. Dimensions must be greater than or equal to 1.");
                            }
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(window, "Invalid input format. Please enter valid integers for dimensions.");
                        }
                    }
                });

                trainButtonsPanel.add(setDimensionsButtonTrain);

                JButton logoutButton = new JButton("Log Out");
                trainButtonsPanel.add(logoutButton);
                logoutButton.addActionListener(e -> {
                    int choice = JOptionPane.showConfirmDialog(null, "Are you sure you want to log out?", "Log Out Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
                    if (choice == JOptionPane.YES_OPTION) {
                        Login loginInterface = new Login();
                        loginInterface.setVisible(true);
                        window.setVisible(false);
                    }
                });



                // Predict tab
                JPanel predictPanel = new JPanel(new BorderLayout());
                tabbedPane.addTab("Predict", predictPanel);
                JPanel predictButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                predictPanel.add(predictButtonsPanel, BorderLayout.NORTH);

                JPanel predictGridPanel = new JPanel();
                JScrollPane predictScrollPane = new JScrollPane(predictGridPanel);
                predictPanel.add(predictScrollPane, BorderLayout.CENTER);

                JButton importButton2 = new JButton("Import");
                importButton2.addActionListener(e -> {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
                    int result = fileChooser.showOpenDialog(window);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        // File selected, measure dimensions and adjust grid size
                        String filePath = fileChooser.getSelectedFile().getPath();

                        // Show importing dialog
                        JOptionPane importingDialog = new JOptionPane("Importing...", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
                        JDialog dialog = importingDialog.createDialog(window, "Importing");
                        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                        dialog.setModal(true);

                        SwingWorker<Void, Void> importWorker = new SwingWorker<>() {
                            @Override
                            protected Void doInBackground() {
                                try {
                                    if(!new File(filePath).exists()){
                                        JOptionPane.showMessageDialog(window, "File does not exists");
                                    }else {
                                        int[] dimensions = measureCSVDimensions(filePath);
                                        if (dimensions != null) {
                                            int newPredictRows = dimensions[0];
                                            int newPredictColumns = dimensions[1];
                                            predictEntries = createGridPanel(predictGridPanel, predictScrollPane, newPredictRows - 1, newPredictColumns - 1);
                                            importCSVData(predictEntries, filePath);
                                        } else {
                                            JOptionPane.showMessageDialog(window, "Invalid CSV file format or empty file.");
                                        }
                                    }
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                                return null;
                            }

                            @Override
                            protected void done() {
                                dialog.dispose(); // Close the importing dialog when the task is done
                            }
                        };

                        importWorker.execute(); // Execute the SwingWorker to perform the import task

                        dialog.setVisible(true); // Show the importing dialog
                    }
                });


                predictButtonsPanel.add(importButton2);

                JButton exportButtonPredict = new JButton("Export");
                exportButtonPredict.addActionListener(e -> {
                    if (isGridEmpty(predictEntries)) {
                        JOptionPane.showMessageDialog(window, "Cannot export an empty grid.");
                    } else {
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
                        int result = fileChooser.showSaveDialog(window);
                        if (result == JFileChooser.APPROVE_OPTION) {
                            String filePath = fileChooser.getSelectedFile().getPath();
                            try {
                                exportCSVData(predictEntries, filePath);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });
                predictButtonsPanel.add(exportButtonPredict);

                String[] optionsPredict = {"Regression", "Support Vector Machine", "Neural Networks"};
                Map<String,String> pkl = new HashMap<>();
                pkl.put(optionsPredict[0],"trained_models_regression.pkl");
                pkl.put(optionsPredict[1],"trained_models_svm.pkl");
                pkl.put(optionsPredict[2],"trained_models_nn.pkl");
                JComboBox<String> comboBoxPredict = new JComboBox<>(optionsPredict);
                comboBoxPredict.setRenderer(new TrainStatusCellRenderer());
                predictButtonsPanel.add(comboBoxPredict);

                // Add "Predict" button to the predict tab
                JButton predictButton = new JButton("Predict");
                predictButton.addActionListener(e -> {
                    if (isGridEmpty(predictEntries)) {
                        JOptionPane.showMessageDialog(window, "Cannot predict on an empty grid.");
                    } else if (!isGridPredictable(predictEntries)){
                        JOptionPane.showMessageDialog(window, "Cannot predict, grid contains rows with more than 2 missing characteristics.");
                    } else if (isGridFull(predictEntries)){
                        JOptionPane.showMessageDialog(window, "Cannot predict, the grid is already full.");
                    } else if (!new File(pkl.get(comboBoxPredict.getSelectedItem())).exists()) {
                        JOptionPane.showMessageDialog(window, "Cannot predict with non trained model.");
                    } else {
                        String filePath = "temporary_csv_predict.csv";
                        try {
                            exportCSVData(predictEntries, filePath);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        String[] command = {
                                AppConfig.pythonExecutable(),
                                "predict.py",
                                (String) comboBoxPredict.getSelectedItem()
                        };
                        // Show initial "Please wait" dialog
                        JOptionPane dialog = new JOptionPane("Please wait...", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
                        JDialog dialogBox = dialog.createDialog("Message");
                        dialogBox.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                        // Start a separate thread using SwingWorker for running the Python script
                        SwingWorker<Integer, String> scriptWorker = new SwingWorker<>() {
                            @Override
                            protected Integer doInBackground() throws Exception {
                                Process process = Runtime.getRuntime().exec(command);
                                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    publish(line); // Publish each line of output for processing in process() method
                                }
                                int exitCode = process.waitFor();
                                System.out.println("Python program exited with code: " + exitCode);
                                return exitCode;
                            }
                            @Override
                            protected void process(List<String> chunks) {
                                for (String line : chunks) {
                                    System.out.println(line); // Print each line of output to the console
                                }
                            }
                            @Override
                            protected void done() {
                                try {
                                    int exitCode = get();
                                    boolean isPrediction = (exitCode == 0); // Assuming exit code 0 means the script was successful
                                    // Update dialog text based on exit code
                                    if (isPrediction) {
                                        try {
                                            String resultFilepath = "updated_dataset.csv";
                                            int[] dimensions = measureCSVDimensions(resultFilepath);
                                            if (dimensions != null) {
                                                int newPredictRows = dimensions[0];
                                                int newPredictColumns = dimensions[1];
                                                predictEntries = createGridPanel(predictGridPanel, predictScrollPane, newPredictRows-1, newPredictColumns-1);
                                                importCSVData(predictEntries, resultFilepath);
                                            } else {
                                                JOptionPane.showMessageDialog(window, "Invalid CSV file format or empty file.");
                                            }
                                        } catch (IOException ex) {
                                            ex.printStackTrace();
                                        }
                                        dialog.setMessage("Prediction terminated with success");
                                        dialog.setMessageType(JOptionPane.PLAIN_MESSAGE);
                                        JButton okButton = new JButton("OK");
                                        okButton.addActionListener(e -> dialogBox.dispose()); // Close the dialog when OK button is clicked
                                        dialog.setOptions(new Object[]{okButton}); // Set the OK button as an option in the dialog
                                        dialog.getRootPane().setDefaultButton(okButton);
                                    } else {
                                        if (exitCode == -1) {
                                            dialog.setMessage("File does not correspond to model trained");
                                            dialog.setMessageType(JOptionPane.PLAIN_MESSAGE);
                                            JButton okButton = new JButton("OK");
                                            okButton.addActionListener(e -> dialogBox.dispose()); // Close the dialog when OK button is clicked
                                            dialog.setOptions(new Object[]{okButton}); // Set the OK button as an option in the dialog
                                            dialog.getRootPane().setDefaultButton(okButton);
                                        }else {
                                            dialog.setMessage("Unexpected error.Exit code: " + exitCode);
                                            dialog.setMessageType(JOptionPane.ERROR_MESSAGE);
                                            JButton okButton = new JButton("OK");
                                            okButton.addActionListener(e -> dialogBox.dispose()); // Close the dialog when OK button is clicked
                                            dialog.setOptions(new Object[]{okButton}); // Set the OK button as an option in the dialog
                                            dialog.getRootPane().setDefaultButton(okButton);
                                        }

                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        scriptWorker.execute(); // Start the SwingWorker
                        dialogBox.setVisible(true); // Display the dialog
                        // Wait for the scriptWorker to complete
                        try {
                            scriptWorker.get();
                        } catch (InterruptedException | ExecutionException e1) {
                            e1.printStackTrace();
                        }
                    }

                });
                predictButtonsPanel.add(predictButton);

                // Add "Clear" button to the predict tab
                JButton clearButtonPredict = new JButton("Clear");
                clearButtonPredict.addActionListener(e ->{
                    clearGridEntries(predictEntries);
                });
                predictButtonsPanel.add(clearButtonPredict);

                int predictRows = 20;
                int predictColumns = 5;
                predictEntries = createGrid(predictGridPanel, predictRows, predictColumns);

                // Add "Set Dimensions" button to the predict tab
                JButton setDimensionsButtonPredict = new JButton("Set Dimensions");
                setDimensionsButtonPredict.addActionListener(e -> {
                    JPanel dimensionsPanel = new JPanel();
                    dimensionsPanel.setLayout(new GridLayout(2, 2));

                    JTextField rowsInputField = new JTextField(5);
                    JTextField columnsInputField = new JTextField(5);

                    dimensionsPanel.add(new JLabel("Rows:"));
                    dimensionsPanel.add(rowsInputField);
                    dimensionsPanel.add(new JLabel("Columns:"));
                    dimensionsPanel.add(columnsInputField);

                    int result = JOptionPane.showConfirmDialog(
                            window,
                            dimensionsPanel,
                            "Set Dimensions",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE
                    );

                    if (result == JOptionPane.OK_OPTION) {
                        String rowsInput = rowsInputField.getText();
                        String columnsInput = columnsInputField.getText();

                        try {
                            int newPredictRows = Integer.parseInt(rowsInput);
                            int newPredictColumns = Integer.parseInt(columnsInput);

                            if (newPredictRows >= 1 && newPredictColumns >= 1) {
                                predictEntries = createGridPanel(predictGridPanel, predictScrollPane, newPredictRows, newPredictColumns);
                            } else {
                                JOptionPane.showMessageDialog(window, "Invalid dimensions entered. Dimensions must be greater than or equal to 1.");
                            }
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(window, "Invalid input format. Please enter valid integers for dimensions.");
                        }
                    }
                });

                predictButtonsPanel.add(setDimensionsButtonPredict);

                JButton logoutButtonPredict = new JButton("Log Out");
                predictButtonsPanel.add(logoutButtonPredict);
                logoutButtonPredict.addActionListener(e -> {
                    int choice = JOptionPane.showConfirmDialog(null, "Are you sure you want to log out?", "Log Out Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
                    if (choice == JOptionPane.YES_OPTION) {
                        Login loginInterface = new Login();
                        loginInterface.setVisible(true);
                        window.setVisible(false);
                    }
                });



                window.setVisible(true);
            });
        }

    private static boolean validateNumber(String value) {
        try {
            if (value.isEmpty() || value.equals("-")) {
                return true;
            }
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void openWebsite() {
        try {
            Desktop.getDesktop().browse(new URI("https://www.enageo.com/"));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static int[] measureCSVDimensions(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        int rows = 0;
        int columns = -1;
        String line;
        while ((line = reader.readLine()) != null) {
            String[] values = line.split(",", -1); // Split with -1 to include empty values
            if (values.length == 1 && values[0].isEmpty()) {
                reader.close();
                return null; // Invalid CSV file format, empty row
            }
            if (columns == -1) {
                columns = values.length;
            } else if (columns != values.length) {
                reader.close();
                return null; // Invalid CSV file format, columns are not consistent
            }
            rows++;
        }
        reader.close();
        return new int[]{rows, columns};
    }


    private static void clearGridEntries(JTextField[][] entries) {
        for (JTextField[] row : entries) {
            for (JTextField entry : row) {
                entry.setText("");
            }
        }
    }


    private static JTextField[][] createGrid(Container container, int rows, int columns) {
        JTextField[][] newEntries = new JTextField[rows + 1][columns + 1];
        container.setLayout(new GridLayout(rows + 1, columns + 1));
        // Add an empty row at the top
        for (int j = 0; j < columns + 1; j++) {
            JTextField emptyEntry = new JTextField(10);
            emptyEntry.setEditable(true);
            emptyEntry.setBackground(new Color(220 , 220, 220)); // Set light gray background color
            emptyEntry.setFont(emptyEntry.getFont().deriveFont(Font.BOLD)); // Make the text bold
            emptyEntry.setHorizontalAlignment(JTextField.CENTER);
            container.add(emptyEntry);
            newEntries[0][j] = emptyEntry;
        }

        // Add the rest of the rows
        for (int i = 1; i < rows + 1; i++) {
            // Add an empty column on the left
            JTextField emptyEntry = new JTextField(10);
            emptyEntry.setEditable(true);
            emptyEntry.setFont(emptyEntry.getFont().deriveFont(Font.BOLD)); // Make the text bold
            emptyEntry.setHorizontalAlignment(JTextField.CENTER);
            container.add(emptyEntry);
            newEntries[i][0] = emptyEntry;

            for (int j = 1; j < columns + 1; j++) {
                JTextField entry = new JTextField(10);
                entry.setHorizontalAlignment(JTextField.CENTER);
                entry.setInputVerifier(new InputVerifier() {
                    @Override
                    public boolean verify(JComponent input) {
                        JTextField textField = (JTextField) input;
                        return validateNumber(textField.getText());
                    }
                });
                container.add(entry);
                newEntries[i][j] = entry;
            }
        }
        return newEntries;
    }


    private void exportCSVData(JTextField[][] entries, String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(filePath)) {
            StringBuilder sb = new StringBuilder();
            for (JTextField[] entry : entries) {
                for (int j = 0; j < entry.length; j++) {
                    sb.append(entry[j].getText());
                    if (j < entry.length - 1) {
                        sb.append(",");
                    }
                }
                sb.append(System.lineSeparator());
            }
            writer.write(sb.toString());
        }
    }


    private static JTextField[][] createGridPanel(JPanel gridPanel, JScrollPane scrollPane,
                                                  int rows, int columns) {
        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(rows + 1, columns + 1));
        JTextField[][] newEntries = new JTextField[rows + 1][columns + 1];
        // Add an empty row at the top
        for (int j = 0; j < columns + 1; j++) {
            JTextField emptyEntry = new JTextField(10);
            emptyEntry.setEditable(true);
            emptyEntry.setBackground(new Color(220, 220, 220)); // Set light gray background color
            emptyEntry.setFont(emptyEntry.getFont().deriveFont(Font.BOLD)); // Make the text bold
            emptyEntry.setHorizontalAlignment(JTextField.CENTER);
            gridPanel.add(emptyEntry);
            newEntries[0][j] = emptyEntry;
        }

        // Add the rest of the rows
        for (int i = 1; i < rows + 1; i++) {
            // Add an empty column on the left
            JTextField emptyEntry = new JTextField(10);
            emptyEntry.setEditable(true);
            emptyEntry.setFont(emptyEntry.getFont().deriveFont(Font.BOLD)); // Make the text bold
            emptyEntry.setHorizontalAlignment(JTextField.CENTER);
            gridPanel.add(emptyEntry);
            newEntries[i][0] = emptyEntry;

            for (int j = 1; j < columns + 1; j++) {
                JTextField entry = new JTextField(10);
                entry.setHorizontalAlignment(JTextField.CENTER);
                entry.setInputVerifier(new InputVerifier() {
                    @Override
                    public boolean verify(JComponent input) {
                        JTextField textField = (JTextField) input;
                        return validateNumber(textField.getText());
                    }
                });
                gridPanel.add(entry);
                newEntries[i][j] = entry;
            }
        }

        gridPanel.revalidate();
        gridPanel.repaint();
        scrollPane.revalidate();
        scrollPane.repaint();
        return newEntries;
    }


    private static void importCSVData(JTextField[][] entries, String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        int row = 0;
        while ((line = reader.readLine()) != null && row < entries.length) {
            String[] values = line.split(",");
            for (int col = 0; col < entries[row].length && col < values.length; col++) {
                entries[row][col].setText(values[col]);
            }
            row++;
        }
        reader.close();
    }

    private boolean isGridEmpty(JTextField[][] entries) {
        for (JTextField[] entry : entries) {
            for (JTextField jTextField : entry) {
                if (!jTextField.getText().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isGridFull(JTextField[][] entries) {
        for (JTextField[] entry : entries) {
            for (JTextField jTextField : entry) {
                if (jTextField.getText().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isGridPredictable(JTextField[][] entries) {
        for (JTextField[] entry : entries) {
            int emptyCount = 0;
            for (JTextField jTextField : entry) {
                if (jTextField.getText().isEmpty()) {
                    emptyCount++;
                    if (emptyCount > 1) {
                        return false;
                    }
                }
            }
        }
        return true;
    }


    private static class CustomListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            // Customize the appearance of the list cell
            if (renderer instanceof JLabel label) {
                label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Add padding
                label.setFont(label.getFont().deriveFont(Font.BOLD)); // Set font style
            }
            return renderer;
        }
    }

    public static class TrainStatusCellRenderer extends DefaultListCellRenderer {
        private final Color GREEN_COLOR = new Color(0, 128, 0);
        private final Color RED_COLOR = Color.RED;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            // Get the selected item
            String selectedItem = value.toString();

            // Set the default foreground and background colors
            label.setForeground(list.getForeground());
            label.setBackground(list.getBackground());

            // Check if the corresponding file exists
            switch (selectedItem) {
                case "Regression" -> {
                    if (checkFileExistence("trained_models_regression.pkl")) {
                        label.setForeground(GREEN_COLOR);
                    } else {
                        label.setForeground(RED_COLOR);
                    }
                }
                case "Support Vector Machine" -> {
                    if (checkFileExistence("trained_models_svm.pkl")) {
                        label.setForeground(GREEN_COLOR);
                    } else {
                        label.setForeground(RED_COLOR);
                    }
                }
                case "Neural Networks" -> {
                    if (checkFileExistence("trained_models_nn.pkl")) {
                        label.setForeground(GREEN_COLOR);
                    } else {
                        label.setForeground(RED_COLOR);
                    }
                }
                case "Random Forest" -> {
                    if (checkFileExistence("trained_models_rf.pkl")) {
                        label.setForeground(GREEN_COLOR);
                    } else {
                        label.setForeground(RED_COLOR);
                    }
                }
                case "Gradient Boosting" -> {
                    if (checkFileExistence("trained_models_gb.pkl")) {
                        label.setForeground(GREEN_COLOR);
                    } else {
                        label.setForeground(RED_COLOR);
                    }
                }
            }

            return label;
        }

        private boolean checkFileExistence(String fileName) {
            File file = new File(fileName);
            return file.exists();
        }
    }
}
