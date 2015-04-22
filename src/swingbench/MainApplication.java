/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swingbench;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.sql.*;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Level;
import static java.util.logging.Level.OFF;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JTree;
import javax.swing.JTable;
import static javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS;
import static javax.swing.JTable.AUTO_RESIZE_OFF;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.event.TableModelEvent.ALL_COLUMNS;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @author xt3
 */
public class MainApplication extends javax.swing.JFrame {
    
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver"; 
    private String db_url;
    private String db_username;
    private String db_password;
    private Connection conn;
    
    
    private DefaultTreeModel model;
    
    private Preferences prefs;

    /**
     * Creates new form MainApplication
     */
    public MainApplication() {
        initComponents();
        prefs = Preferences.userRoot().node(this.getClass().getName());
        
        checkForPref();
        
        connectionDialog.setPreferredSize(new Dimension(440, 200));//set your desired size
        connectionDialog.pack();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        int iWidth = (screenSize.width - connectionDialog.getWidth()) / 2;
        int iHeight = (screenSize.height - connectionDialog.getHeight()) / 2;
        connectionDialog.setLocation(iWidth, iHeight);
        queryDialog.setLocation(iWidth, iHeight);
        
        connectionDialog.setVisible(true);
    }
    
    private void checkForPref() {
        if (prefs.getBoolean("RememberMe", false)) {
            //set values in connectionDialog
            dialog_url.setText(prefs.get("Url", ""));
            dialog_username.setText(prefs.get("Username", ""));
            dialog_password.setText(prefs.get("Password", ""));
            rememberMeCheckBox.setSelected(true);
        } else {
            System.out.println("Preferences could not be read.");
        }
    }
    
    public boolean connectToServer() {
        conn = null;
        Statement stmt = null;
        try{
            //STEP 2: Register JDBC driver
            Class.forName("com.mysql.jdbc.Driver");

            //STEP 3: Open a connection
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(db_url,db_username,db_password);

            //STEP 4: Execute a query
            System.out.println("Creating statement...");
            stmt = conn.createStatement();
            stmt.close();
            return true;
            }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
            }catch(Exception e){
            //Handle errors for Class.forName
            e.printStackTrace();
            }finally{
            //finally block used to close resources
            try{
               if(stmt!=null)
                  stmt.close();
            }catch(SQLException se2){
            }
        }
        return false;
    }
    
    private void getDatabaseList() throws SQLException{
        System.out.println(conn);
        DefaultMutableTreeNode MySQL = new DefaultMutableTreeNode("MySQL");
        DatabaseMetaData md = conn.getMetaData();
        ResultSet db = md.getCatalogs();
        while(db.next()) {
            System.out.println("db  =  "+db.getString(1));
            DefaultMutableTreeNode database = new DefaultMutableTreeNode(db.getString(1));
            MySQL.add(database);
            String[] types = {"TABLE", "VIEW"};
            ResultSet tbl = md.getTables(db.getString(1), null, "%", types);
            boolean emptySet = true;
            while(tbl.next()) {
                emptySet = false;
                System.out.println("--tbl = "+tbl.getString("TABLE_NAME"));
                DefaultMutableTreeNode table = new DefaultMutableTreeNode(tbl.getString("TABLE_NAME"));
                database.add(table);
            }
            if (emptySet)
                database.add(new DefaultMutableTreeNode("(no tables visible)"));
            tbl.close();
        }
        db.close();
        
        JTree root = new JTree(MySQL);
        model = (DefaultTreeModel) root.getModel();
        schemaTree.setModel(model); 
    }
    
    private void updateTable(String db, String tbl) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select * from " + db + "." + tbl + ";");
        System.out.println(rs);
        selectedTable.setModel(buildTableModel(rs));
    }
    
    public void importSQL(Scanner s, boolean flag) throws SQLException, FileNotFoundException {
	s.useDelimiter(";");
	Statement stmt = null;
	try
	{
            stmt = conn.createStatement();
            while (s.hasNext())
            {
            	String line = s.next();
                boolean isWhitespace = line.matches("^\\s*$");
                if (line.startsWith("/*!") && line.endsWith("*/"))
                {
                    int i = line.indexOf(' ');
                    line = line.substring(i + 1, line.length() - " */".length());
                }

		if (line.trim().length() > 0 && !isWhitespace)
		{
                    line = line.trim() + ";";
                    PreparedStatement pst;
                    ResultSet rs;
                    long start_time = System.nanoTime();
                    if(line.toLowerCase().startsWith("select") || line.toLowerCase().startsWith("show")) {
                        pst = (PreparedStatement) conn.prepareStatement(line);
                        rs = pst.executeQuery();
                        if (flag){
                            customQueryTable.setModel(buildTableModel(rs));
                            
                            //Set Width Testing
                            for (int i = 0; i < customQueryTable.getColumnCount(); i++){
                                int stringLength = customQueryTable.getValueAt(0, i).toString().length();
                                customQueryTable.getColumnModel().getColumn(i).setPreferredWidth((stringLength + 50) * 2 );
                                customQueryTable.updateUI();
                            }
                            
                        }
                    }else{
                        stmt.execute(line);
                    }
                    long end_time = System.nanoTime();
                    double difference = (end_time - start_time)/1e9;
                    String time = String.format("%.2f", difference);
                    if (flag){
                        queryDebugPane.getStyledDocument().insertString(queryDebugPane.getStyledDocument().getLength(), "Statement: " + line + "  Execution time:" + time + "\n", null);
                    }else{
                        debugPane.getStyledDocument().insertString(debugPane.getStyledDocument().getLength(), "Statement: " + line + "  Execution time:" + time + "\n", null);
                    }
                }
            }
	}
        catch (BadLocationException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);           
        }	finally
	{
		if (stmt != null) stmt.close();
	}
    }
    
    public static DefaultTableModel buildTableModel(ResultSet rs) 
            throws SQLException {

        ResultSetMetaData metaData = rs.getMetaData();
        System.out.println(metaData);

        // names of columns
        Vector<String> columnNames = new Vector<>();
        int columnCount = metaData.getColumnCount();
        for (int column = 1; column <= columnCount; column++) {
            columnNames.add(metaData.getColumnName(column));
            System.out.println(metaData.getColumnName(column));
        }

        // data of the table
        Vector<Vector<Object>> data = new Vector<>();
        while (rs.next()) {
            Vector<Object> vector = new Vector<>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                vector.add(rs.getObject(columnIndex));
                System.out.println(rs.getObject(columnIndex));
            }
            data.add(vector);
        }

        return new DefaultTableModel(data, columnNames){
            @Override
            public boolean isCellEditable(int row, int column){
                return false;
            }
        };

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        connectionDialog = new javax.swing.JDialog();
        jLabel1 = new javax.swing.JLabel();
        dialog_url = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        dialog_username = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        dialog_password = new javax.swing.JPasswordField();
        connectButton = new javax.swing.JButton();
        dialog_error_label = new javax.swing.JLabel();
        rememberMeCheckBox = new javax.swing.JCheckBox();
        fileChooser = new javax.swing.JFileChooser();
        queryDialog = new javax.swing.JDialog();
        jScrollPane1 = new javax.swing.JScrollPane();
        customQueryTable = new javax.swing.JTable();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        queryPane1 = new javax.swing.JEditorPane();
        customQuerySubmitButton = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        queryDebugPane = new javax.swing.JTextPane();
        debugScrollPane = new javax.swing.JScrollPane();
        debugPane = new javax.swing.JTextPane();
        schemaScrollPane = new javax.swing.JScrollPane();
        schemaTree = new javax.swing.JTree();
        tableScrollPane = new javax.swing.JScrollPane();
        selectedTable = new javax.swing.JTable();
        debugConsoleLabel = new javax.swing.JLabel();
        mainMenuBar = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu_disconnectButton = new javax.swing.JMenuItem();
        jMenu_exitButton = new javax.swing.JMenuItem();
        jMenu_refreshButton = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        importSQL = new javax.swing.JMenuItem();
        importTXT = new javax.swing.JMenuItem();
        refreshTable = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        customQueryMenuItem = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();

        connectionDialog.setTitle("Connect to MySQL DB");
        connectionDialog.setAlwaysOnTop(true);
        connectionDialog.setMinimumSize(new java.awt.Dimension(418, 225));
        connectionDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowDeactivated(java.awt.event.WindowEvent evt) {
                connectionDialogWindowDeactivated(evt);
            }
        });
        connectionDialog.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                connectionDialogKeyPressed(evt);
            }
        });

        jLabel1.setText("IP:Port");

        dialog_url.setText("localhost:3306");
        dialog_url.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dialog_urlActionPerformed(evt);
            }
        });
        dialog_url.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                dialog_urlKeyPressed(evt);
            }
        });

        jLabel2.setText("Username:");

        dialog_username.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dialog_usernameActionPerformed(evt);
            }
        });
        dialog_username.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                dialog_usernameKeyPressed(evt);
            }
        });

        jLabel3.setText("Password:");

        dialog_password.setText("password");
        dialog_password.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                dialog_passwordKeyPressed(evt);
            }
        });

        connectButton.setText("Connect");
        connectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectButtonActionPerformed(evt);
            }
        });

        dialog_error_label.setForeground(java.awt.Color.red);

        rememberMeCheckBox.setText("Remember Login?");

        javax.swing.GroupLayout connectionDialogLayout = new javax.swing.GroupLayout(connectionDialog.getContentPane());
        connectionDialog.getContentPane().setLayout(connectionDialogLayout);
        connectionDialogLayout.setHorizontalGroup(
            connectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, connectionDialogLayout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(connectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, connectionDialogLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, connectionDialogLayout.createSequentialGroup()
                        .addGroup(connectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(dialog_error_label))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 22, Short.MAX_VALUE)
                        .addGroup(connectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, connectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(dialog_password)
                                .addComponent(dialog_username, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE))
                            .addComponent(dialog_url, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 238, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, connectionDialogLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(rememberMeCheckBox)
                        .addGap(56, 56, 56)
                        .addComponent(connectButton)))
                .addGap(41, 41, 41))
        );
        connectionDialogLayout.setVerticalGroup(
            connectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(connectionDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(connectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(connectionDialogLayout.createSequentialGroup()
                        .addGroup(connectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(dialog_url, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(connectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(dialog_username, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(connectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(dialog_password, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(31, 31, 31))
                    .addComponent(dialog_error_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 16, Short.MAX_VALUE)
                .addGroup(connectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(connectButton)
                    .addComponent(rememberMeCheckBox))
                .addContainerGap())
        );

        fileChooser.setCurrentDirectory(new File("."));

        queryDialog.setMinimumSize(new java.awt.Dimension(571, 500));
        queryDialog.setPreferredSize(new java.awt.Dimension(571, 500));

        jScrollPane1.setAutoscrolls(true);

        customQueryTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null}
            },
            new String [] {
                "Custom Query"
            })
            {public boolean isCellEditable(int row, int column){return false;}}
        );
        customQueryTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        customQueryTable.setDragEnabled(true);
        jScrollPane1.setViewportView(customQueryTable);

        jLabel4.setText("Query");

        jLabel5.setText("Debug console");

        jScrollPane3.setViewportView(queryPane1);

        customQuerySubmitButton.setText("Submit Query");
        customQuerySubmitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customQuerySubmitButtonActionPerformed(evt);
            }
        });

        queryDebugPane.setEditable(false);
        jScrollPane4.setViewportView(queryDebugPane);

        javax.swing.GroupLayout queryDialogLayout = new javax.swing.GroupLayout(queryDialog.getContentPane());
        queryDialog.getContentPane().setLayout(queryDialogLayout);
        queryDialogLayout.setHorizontalGroup(
            queryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 571, Short.MAX_VALUE)
            .addComponent(jScrollPane3)
            .addGroup(queryDialogLayout.createSequentialGroup()
                .addGroup(queryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(queryDialogLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(queryDialogLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(customQuerySubmitButton)))
                .addContainerGap())
            .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        queryDialogLayout.setVerticalGroup(
            queryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(queryDialogLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 212, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                .addGroup(queryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(customQuerySubmitButton))
                .addGap(2, 2, 2)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SwingBench");
        setMinimumSize(new java.awt.Dimension(700, 635));
        setPreferredSize(new java.awt.Dimension(700, 635));

        debugScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        debugScrollPane.setAutoscrolls(true);

        debugPane.setEditable(false);
        debugPane.setMinimumSize(new java.awt.Dimension(2147483647, 2147483647));
        debugScrollPane.setViewportView(debugPane);

        schemaTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                schemaTreeMouseClicked(evt);
            }
        });
        schemaScrollPane.setViewportView(schemaTree);

        tableScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        tableScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        selectedTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
            },
            new String [] {
                "SwingBench!"
            })
            {public boolean isCellEditable(int row, int column){return false;}}
        );
        selectedTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        selectedTable.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        selectedTable.setDragEnabled(true);
        tableScrollPane.setViewportView(selectedTable);

        debugConsoleLabel.setText("Debug console");

        jMenu1.setText("File");

        jMenu_disconnectButton.setText("Disconnect");
        jMenu_disconnectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenu_disconnectButtonActionPerformed(evt);
            }
        });
        jMenu1.add(jMenu_disconnectButton);

        jMenu_exitButton.setText("Exit");
        jMenu_exitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenu_exitButtonActionPerformed(evt);
            }
        });
        jMenu1.add(jMenu_exitButton);

        jMenu_refreshButton.setText("Refresh");
        jMenu_refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenu_refreshButtonActionPerformed(evt);
            }
        });
        jMenu1.add(jMenu_refreshButton);

        mainMenuBar.add(jMenu1);

        jMenu2.setText("Edit");

        importSQL.setText("Import .sql");
        importSQL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importSQLActionPerformed(evt);
            }
        });
        jMenu2.add(importSQL);

        importTXT.setText("Import .txt");
        importTXT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importTXTActionPerformed(evt);
            }
        });
        jMenu2.add(importTXT);

        refreshTable.setText("Export .sql");
        refreshTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshTableActionPerformed(evt);
            }
        });
        jMenu2.add(refreshTable);

        mainMenuBar.add(jMenu2);

        jMenu3.setText("Query");

        customQueryMenuItem.setText("Custom Query");
        customQueryMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customQueryMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(customQueryMenuItem);

        jMenuItem2.setText("Query-Builder");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem2);

        mainMenuBar.add(jMenu3);

        setJMenuBar(mainMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(schemaScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE))
            .addComponent(debugConsoleLabel)
            .addComponent(debugScrollPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(schemaScrollPane)
                    .addComponent(tableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(debugConsoleLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(debugScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void dialog_urlActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dialog_urlActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_dialog_urlActionPerformed

    private void dialog_usernameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dialog_usernameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_dialog_usernameActionPerformed

    private void connectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectButtonActionPerformed
        
        //If a user connects with remember me checked, store preferences
        if (rememberMeCheckBox.isSelected()) {
            prefs.putBoolean("RememberMe", true);
            prefs.put("Url", dialog_url.getText());
            prefs.put("Username", dialog_username.getText());
            prefs.put("Password", String.valueOf(dialog_password.getPassword()));
        } else {
            try {
                prefs.clear();
            } catch (BackingStoreException ex) {
                Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        dialog_error_label.setText("");
        db_url = "jdbc:mysql://" + dialog_url.getText();
        db_username = dialog_username.getText();
        db_password = String.valueOf(dialog_password.getPassword());
        if (!connectToServer()){
            dialog_error_label.setText("**Error Connecting**");
        } else {
            connectionDialog.setVisible(false);
            this.setVisible(true);
            debugPane.setText("Connected to: " + db_url + "\n");
            try {
                getDatabaseList();
            } catch (SQLException ex) {
                Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_connectButtonActionPerformed

    private void jMenu_exitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu_exitButtonActionPerformed
        System.exit(0);
    }//GEN-LAST:event_jMenu_exitButtonActionPerformed

    private void connectionDialogKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_connectionDialogKeyPressed
        // TODO add your handling code here:
        if (evt.getID() == KeyEvent.KEY_PRESSED) {
            if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                connectButton.doClick();
            }
        }
    }//GEN-LAST:event_connectionDialogKeyPressed

    private void jMenu_disconnectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu_disconnectButtonActionPerformed
        db_url = "";
        db_username = "";
        db_password = "";
        try {
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.setVisible(false);
        queryDialog.setVisible(false);
        connectionDialog.setVisible(true);
        conn = null;
    }//GEN-LAST:event_jMenu_disconnectButtonActionPerformed

    private void dialog_urlKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_dialog_urlKeyPressed
        if (evt.getID() == KeyEvent.KEY_PRESSED) {
            if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                connectButton.doClick();
            }
        }
    }//GEN-LAST:event_dialog_urlKeyPressed

    private void dialog_usernameKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_dialog_usernameKeyPressed
        if (evt.getID() == KeyEvent.KEY_PRESSED) {
            if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                connectButton.doClick();
            }
        }
    }//GEN-LAST:event_dialog_usernameKeyPressed

    private void dialog_passwordKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_dialog_passwordKeyPressed
        if (evt.getID() == KeyEvent.KEY_PRESSED) {
            if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                connectButton.doClick();
            }
        }
    }//GEN-LAST:event_dialog_passwordKeyPressed

    private void schemaTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_schemaTreeMouseClicked
        int selRow = schemaTree.getRowForLocation(evt.getX(), evt.getY());
        TreePath selPath = schemaTree.getPathForLocation(evt.getX(), evt.getY());
        if(selRow != -1 && selRow != 0) {
            if(evt.getClickCount() == 2) {
                try {
                    String db = selPath.getPath()[1].toString();
                    String tbl = selPath.getPath()[2].toString();
                                       
                    updateTable(db,tbl);
                    System.out.println(selRow);
                    System.out.println(selPath);
                    System.out.println(db);
                } catch (SQLException ex) {
                    Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }//GEN-LAST:event_schemaTreeMouseClicked

    private void importSQLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importSQLActionPerformed
        // TODO add your handling code here:
        int returnVal = fileChooser.showOpenDialog(null);
        
        if (returnVal == JFileChooser.APPROVE_OPTION){
            File file = fileChooser.getSelectedFile();
            try {
                debugPane.getStyledDocument().insertString(debugPane.getStyledDocument().getLength(), "Opening file: " + file.getName() + "\n", null);
                Scanner s = new Scanner(file);
                importSQL(s,false);
                getDatabaseList();
            } catch (BadLocationException | IOException | SQLException ex) {
                Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }//GEN-LAST:event_importSQLActionPerformed

    private void importTXTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importTXTActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_importTXTActionPerformed

    private void jMenu_refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu_refreshButtonActionPerformed
        try {
            getDatabaseList();
        } catch (SQLException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenu_refreshButtonActionPerformed

    private void connectionDialogWindowDeactivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_connectionDialogWindowDeactivated
        // TODO add your handling code here:
        if (conn == null){
          System.exit(0);  
        }      
    }//GEN-LAST:event_connectionDialogWindowDeactivated

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void customQueryMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customQueryMenuItemActionPerformed
        // TODO add your handling code here:
        queryDialog.setVisible(true);
    }//GEN-LAST:event_customQueryMenuItemActionPerformed

    private void customQuerySubmitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customQuerySubmitButtonActionPerformed
        try {
            // TODO add your handling code here:
            importSQL(new Scanner(queryPane1.getText()),true);
            queryPane1.setText(null);
        } catch (SQLException | FileNotFoundException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
            try {
                queryDebugPane.getStyledDocument().insertString(queryDebugPane.getStyledDocument().getLength(), ex + "\n", null);
            } catch (BadLocationException ex1) {
                Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }//GEN-LAST:event_customQuerySubmitButtonActionPerformed

    private void refreshTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshTableActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_refreshTableActionPerformed
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainApplication().setVisible(false);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton connectButton;
    private javax.swing.JDialog connectionDialog;
    private javax.swing.JMenuItem customQueryMenuItem;
    private javax.swing.JButton customQuerySubmitButton;
    private javax.swing.JTable customQueryTable;
    private javax.swing.JLabel debugConsoleLabel;
    private javax.swing.JTextPane debugPane;
    private javax.swing.JScrollPane debugScrollPane;
    private javax.swing.JLabel dialog_error_label;
    private javax.swing.JPasswordField dialog_password;
    private javax.swing.JTextField dialog_url;
    private javax.swing.JTextField dialog_username;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JMenuItem importSQL;
    private javax.swing.JMenuItem importTXT;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenu_disconnectButton;
    private javax.swing.JMenuItem jMenu_exitButton;
    private javax.swing.JMenuItem jMenu_refreshButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JMenuBar mainMenuBar;
    private javax.swing.JTextPane queryDebugPane;
    private javax.swing.JDialog queryDialog;
    private javax.swing.JEditorPane queryPane1;
    private javax.swing.JMenuItem refreshTable;
    private javax.swing.JCheckBox rememberMeCheckBox;
    private javax.swing.JScrollPane schemaScrollPane;
    private javax.swing.JTree schemaTree;
    private javax.swing.JTable selectedTable;
    private javax.swing.JScrollPane tableScrollPane;
    // End of variables declaration//GEN-END:variables
}
