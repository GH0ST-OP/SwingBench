/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swingbench;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import com.opencsv.*;
import java.awt.Image;
import java.io.BufferedWriter;
import java.io.StringWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @authors Matthew Monteleone, Justin Doonan
 */
public class MainApplication extends javax.swing.JFrame {

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private String db_url;
    private String db_username;
    private String db_password;
    private Connection conn;
    private File importFile;
    private String dir;
    private boolean exportBoolean = true;

    private CreateTableWorker worker1;

    private DefaultTreeModel model;

    private String[] types = {"INT", "TINYINT", "SMALLINT",
        "MEDIUMINT", "BIGINT", "FLOAT", "DOUBLE", "DECIMAL(16,4)",
        "DATE", "DATETIME", "TIMESTAMP", "TIME", "YEAR",
        "CHAR(1)", "CHAR(25)", "CHAR(160)", "VARCHAR(255)",
        "TEXT", "TINYTEXT", "MEDIUMTEXT", "LONGTEXT", "ENUM"};
    
    private String createTableNameS = "";
    private String createTableQueryS = "";
    private String createTableColumnS = "";

    private Preferences prefs;

    public class CreateTableWorker extends SwingWorker<Integer, String> {

        private final JTable table;
        private final String db;
        private final String tbl;
        private final String query;
        private final int progress;

        public CreateTableWorker(final JTable table, final String db, final String tbl, final String query){
            this.table = table;
            this.db = db;
            this.tbl = tbl;
            this.query = query;
            progress = 0;
        }

        @Override   
        protected Integer doInBackground() throws Exception {
            publish("Begin populating the table");
            Statement stmt = conn.createStatement();
            ResultSet rs = null;
            
            System.out.println("Begin creating table");

            if (query == ""){
                rs = stmt.executeQuery("select * from " + db + "." + tbl + ";");
            } else {
                rs = stmt.executeQuery(query);
            }
            
            System.out.println("executed query");

            table.setModel(buildTableModel(rs));
            
            System.out.println("built table model");


            //Adjust Columns
            for (int i = 0; i < table.getColumnCount(); i++){
                try{
                    int stringLength = table.getValueAt(0, i).toString().length();
                    table.getColumnModel().getColumn(i).setPreferredWidth((stringLength + 50) * 2 );
                }catch (NullPointerException ex){
                    System.out.println("null value found");
                }
            }
            return progress;
        }

        private DefaultTableModel buildTableModel(ResultSet rs) throws SQLException {
            
            System.out.println("building table model");

            ResultSetMetaData metaData = rs.getMetaData();

            rs.last();
            int size = rs.getRow() + 1;
            int counter = 0;
            rs.beforeFirst();
            
            if(size == 1){
                return (DefaultTableModel) table.getModel();
            }
            
            
            System.out.println(size);

            // names of columns
            Vector<String> columnNames = new Vector<>();
            int columnCount = metaData.getColumnCount();
            for (int column = 1; column <= columnCount; column++) {
                columnNames.add(metaData.getColumnName(column));
            }

            // data of the table
            final Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> vector = new Vector<>();
                for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                    vector.add(rs.getObject(columnIndex));
                }

                data.add(vector);
                counter++;
                setProgress((counter + 1) * 100 / size);

                if (isCancelled()){
                    break;
                }

                if (getProgress() == 100){
                    publish("Finished creating table");
                    return new DefaultTableModel(data, columnNames){
                        @Override
                        public boolean isCellEditable(int row, int column){
                            return false;
                        }
                    };
                }
            }
            //Did not complete, do not change model
            return (DefaultTableModel) table.getModel();
        }

    }

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

        iWidth = (screenSize.width - queryDialog.getWidth()) / 2;
        iHeight = (screenSize.height - queryDialog.getHeight()) / 2;
        queryDialog.setLocation(iWidth, iHeight);

        iWidth = (screenSize.width - importText.getWidth()) / 2;
        iHeight = (screenSize.height - importText.getHeight()) / 2;
        importText.setLocation(iWidth, iHeight);

        iWidth = (screenSize.width - loadingDialog.getWidth()) / 2;
        iHeight = (screenSize.height - loadingDialog.getHeight()) / 2;
        loadingDialog.setLocation(iWidth, iHeight);

        iWidth = (screenSize.width - fileNamerDialog.getWidth()) / 2;
        iHeight = (screenSize.height - fileNamerDialog.getHeight()) / 2;
        fileNamerDialog.setLocation(iWidth, iHeight);

        iWidth = (screenSize.width - createDatabaseDialog.getWidth()) / 2;
        iHeight = (screenSize.height - createDatabaseDialog.getHeight()) / 2;
        createDatabaseDialog.setLocation(iWidth, iHeight);

        iWidth = (screenSize.width - dropDatabaseDialog.getWidth()) / 2;
        iHeight = (screenSize.height - dropDatabaseDialog.getHeight()) / 2;
        dropDatabaseDialog.setLocation(iWidth, iHeight);

        iWidth = (screenSize.width - dropTableDialog.getWidth()) / 2;
        iHeight = (screenSize.height - dropTableDialog.getHeight()) / 2;
        dropTableDialog.setLocation(iWidth, iHeight);

        iWidth = (screenSize.width - dropTableDialog.getWidth()) / 2;
        iHeight = (screenSize.height - dropTableDialog.getHeight()) / 2;
        dropTableDialog.setLocation(iWidth, iHeight);
                
        iWidth = (screenSize.width - createTableDialog.getWidth()) / 2;
        iHeight = (screenSize.height - createTableDialog.getHeight()) / 2;
        createTableDialog.setLocation(iWidth, iHeight);

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
        }
    }

    public boolean connectToServer() {
        conn = null;
        Statement stmt = null;
        try{
            //STEP 2: Register JDBC driver
            Class.forName("com.mysql.jdbc.Driver");

            //STEP 3: Open a connection
            conn = DriverManager.getConnection(db_url,db_username,db_password);

            //STEP 4: Execute a query
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
        DefaultMutableTreeNode MySQL = new DefaultMutableTreeNode("MySQL");
        DatabaseMetaData md = conn.getMetaData();
        ResultSet db = md.getCatalogs();
        while(db.next()) {
            DefaultMutableTreeNode database = new DefaultMutableTreeNode(db.getString(1));
            MySQL.add(database);
            String[] types = {"TABLE", "VIEW"};
            ResultSet tbl = md.getTables(db.getString(1), null, "%", types);
            boolean emptySet = true;
            while(tbl.next()) {
                emptySet = false;
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
    
    public void exportSQL(String fileName) throws SQLException, FileNotFoundException{
        String db = exportdbCombo.getSelectedItem().toString();
        if (exportBoolean) {
            try {
                String tbl = exporttblCombo.getSelectedItem().toString();
                ResultSet rs;
                PreparedStatement pst;
                pst = (PreparedStatement) conn.prepareStatement("select * from " + db + "." + tbl + ";");
                System.out.println("select * from " + db + "." + tbl + ";");
                rs = pst.executeQuery();

                StringWriter sw = new StringWriter();
                CSVWriter csvWriter = new CSVWriter(sw, ',', '\0');
                csvWriter.setResultService(new ResultSetHelperService());
                csvWriter.writeAll(rs, false);
                String result = sw.toString();

                File file = new File(dir + "/" + fileName);

                // if file doesnt exists, then create it
                if (!file.exists()) {
                        file.createNewFile();
                }

                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(result);
                bw.close();

                try {
                    debugPane.getStyledDocument().insertString(debugPane.getStyledDocument().getLength(), "File '" + fileName + "' exported successfully.\n", null);
                } catch (BadLocationException ex) {
                    Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (IOException ex) {
                try {
                    debugPane.getStyledDocument().insertString(debugPane.getStyledDocument().getLength(), ex + "\n", null);
                    Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
                } catch (BadLocationException ex1) {
                    Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
        else{
                String terminal = "mysqldump " +
                        "-u " + db_username + " -p" + db_password +
                        " -r " + dir + "/" + fileName + " " + db;

                Process runtimeProcess;
            try {
                 runtimeProcess = Runtime.getRuntime().exec(terminal);
                int processComplete = runtimeProcess.waitFor();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                    debugPane.getStyledDocument().insertString(debugPane.getStyledDocument().getLength(), "Database " + db + " dumped successfully.\n", null);
                } catch (BadLocationException ex) {
                    Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
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
                    if(line.toLowerCase().startsWith("select") || line.toLowerCase().startsWith("show")
                            || line.toLowerCase().startsWith("desc")) {
                        pst = (PreparedStatement) conn.prepareStatement(line);
                        rs = pst.executeQuery();
                        if (flag){
                            customQueryTable.setModel(buildTableModel(rs));

                            //Set Width Testing
                            for (int i = 0; i < customQueryTable.getColumnCount(); i++){
//                                customQueryTable.getColumnModel().getColumn(i).setPreferredWidth(130);
                                try{
                                   int stringLength = customQueryTable.getValueAt(0, i).toString().length();
                                   customQueryTable.getColumnModel().getColumn(i).setPreferredWidth((stringLength + 50) * 2 );
                                }catch (NullPointerException ex){
                                    System.out.println("null value found");
                                }
                            }

                        }
                    }else{
                        stmt.execute(line);
                    }
                    jMenu_refreshButton.doClick();
                    long end_time = System.nanoTime();
                    double difference = (end_time - start_time)/1e9;
                    String time = String.format("%.2f", difference);
                    if (flag){
                        customQueryDebugPane.getStyledDocument().insertString(customQueryDebugPane.getStyledDocument().getLength(), "Statement: " + line + "\n", null);
                    }else{
                        debugPane.getStyledDocument().insertString(debugPane.getStyledDocument().getLength(), "Statement: " + line + "\n", null);
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

        // names of columns
        Vector<String> columnNames = new Vector<>();
        int columnCount = metaData.getColumnCount();
        for (int column = 1; column <= columnCount; column++) {
            columnNames.add(metaData.getColumnName(column));
        }

        // data of the table
        Vector<Vector<Object>> data = new Vector<>();
        while (rs.next()) {
            Vector<Object> vector = new Vector<>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                vector.add(rs.getObject(columnIndex));
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
        customQueryPane = new javax.swing.JEditorPane();
        customQuerySubmitButton = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        customQueryDebugPane = new javax.swing.JTextPane();
        loadingDialog = new javax.swing.JDialog();
        jLabel6 = new javax.swing.JLabel();
        tableBuilderProgressBar = new javax.swing.JProgressBar();
        AbortUpdateTableButton = new javax.swing.JButton();
        importText = new javax.swing.JDialog();
        jButton1 = new javax.swing.JButton();
        importTextfilename = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        importTextImportButton = new javax.swing.JButton();
        fieldterm = new javax.swing.JTextField();
        fieldencl = new javax.swing.JTextField();
        fieldesc = new javax.swing.JTextField();
        lineterm = new javax.swing.JTextField();
        linestart = new javax.swing.JTextField();
        databaseCombo = new javax.swing.JComboBox();
        tableCombo = new javax.swing.JComboBox();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        fileNamerDialog = new javax.swing.JDialog();
        jLabel14 = new javax.swing.JLabel();
        fileNamerName = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        selectedDirectory = new javax.swing.JLabel();
        exportButton = new javax.swing.JButton();
        exportdbCombo = new javax.swing.JComboBox();
        directorySelector = new javax.swing.JButton();
        exporttblCombo = new javax.swing.JComboBox();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        fileWarning = new javax.swing.JLabel();
        createDatabaseDialog = new javax.swing.JDialog();
        jLabel18 = new javax.swing.JLabel();
        createDbText = new javax.swing.JTextField();
        createDbButton = new javax.swing.JButton();
        dropDatabaseDialog = new javax.swing.JDialog();
        jLabel19 = new javax.swing.JLabel();
        dropDbCombo = new javax.swing.JComboBox();
        dropDbButton = new javax.swing.JButton();
        dropTableDialog = new javax.swing.JDialog();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        dropTblDbCombo = new javax.swing.JComboBox();
        dropTblCombo = new javax.swing.JComboBox();
        dropTblButton = new javax.swing.JButton();
        createTableDialog = new javax.swing.JDialog();
        jLabel22 = new javax.swing.JLabel();
        createTableName = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        createTableStatement = new javax.swing.JEditorPane();
        jLabel23 = new javax.swing.JLabel();
        createTableExecute = new javax.swing.JButton();
        createTableAddName = new javax.swing.JButton();
        jLabel24 = new javax.swing.JLabel();
        createTableColumnName = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        createTableDataTypes = new javax.swing.JComboBox();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel26 = new javax.swing.JLabel();
        createTableExtra = new javax.swing.JTextField();
        createTableAddColumn = new javax.swing.JButton();
        jLabel27 = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        createTableDebugPane = new javax.swing.JTextPane();
        createTableDbNames = new javax.swing.JComboBox();
        jLabel28 = new javax.swing.JLabel();
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
        exportSQL = new javax.swing.JMenuItem();
        exportTXT = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        customQueryMenuItem = new javax.swing.JMenuItem();
        createDbMenuItem = new javax.swing.JMenuItem();
        createTblMenuItem = new javax.swing.JMenuItem();
        dropDbMenuItem = new javax.swing.JMenuItem();
        dropTblMenuItem = new javax.swing.JMenuItem();

        connectionDialog.setTitle("Connect to MySQL DB");
        connectionDialog.setAlwaysOnTop(true);
        connectionDialog.setMinimumSize(new java.awt.Dimension(418, 215));
        connectionDialog.setResizable(false);
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
                        .addComponent(rememberMeCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 26, Short.MAX_VALUE)
                .addGroup(connectionDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(connectButton)
                    .addComponent(rememberMeCheckBox))
                .addContainerGap())
        );

        fileChooser.setCurrentDirectory(new File("."));

        queryDialog.setMinimumSize(new java.awt.Dimension(571, 500));
        queryDialog.setResizable(false);

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

        jScrollPane3.setViewportView(customQueryPane);

        customQuerySubmitButton.setText("Submit Query");
        customQuerySubmitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customQuerySubmitButtonActionPerformed(evt);
            }
        });

        customQueryDebugPane.setEditable(false);
        jScrollPane4.setViewportView(customQueryDebugPane);

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

        loadingDialog.setAutoRequestFocus(false);
        loadingDialog.setMinimumSize(new java.awt.Dimension(400, 120));
        loadingDialog.setResizable(false);

        jLabel6.setText("Loading Table. Please Wait...");

        AbortUpdateTableButton.setText("Abort");
        AbortUpdateTableButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AbortUpdateTableButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout loadingDialogLayout = new javax.swing.GroupLayout(loadingDialog.getContentPane());
        loadingDialog.getContentPane().setLayout(loadingDialogLayout);
        loadingDialogLayout.setHorizontalGroup(
            loadingDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(loadingDialogLayout.createSequentialGroup()
                .addGap(94, 94, 94)
                .addGroup(loadingDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tableBuilderProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(100, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, loadingDialogLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(AbortUpdateTableButton)
                .addContainerGap())
        );
        loadingDialogLayout.setVerticalGroup(
            loadingDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(loadingDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(tableBuilderProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(AbortUpdateTableButton)
                .addContainerGap(13, Short.MAX_VALUE))
        );

        importText.setMinimumSize(new java.awt.Dimension(600, 330));
        importText.setResizable(false);

        jButton1.setText("File Selector");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel7.setText("Field Terminated By:");

        jLabel8.setText("Field Enclosed By:");

        jLabel9.setText("Field Escaped By:");

        jLabel10.setText("Line Terminated By:");

        jLabel11.setText("Line Starting By:");

        importTextImportButton.setText("Import");
        importTextImportButton.setEnabled(false);
        importTextImportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importTextImportButtonActionPerformed(evt);
            }
        });

        fieldencl.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fieldenclActionPerformed(evt);
            }
        });

        fieldesc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fieldescActionPerformed(evt);
            }
        });

        lineterm.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                linetermActionPerformed(evt);
            }
        });

        databaseCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        databaseCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                databaseComboActionPerformed(evt);
            }
        });

        tableCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        tableCombo.setEnabled(false);
        tableCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tableComboActionPerformed(evt);
            }
        });

        jLabel12.setText("Database:");

        jLabel13.setText("Table:");

        javax.swing.GroupLayout importTextLayout = new javax.swing.GroupLayout(importText.getContentPane());
        importText.getContentPane().setLayout(importTextLayout);
        importTextLayout.setHorizontalGroup(
            importTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(importTextLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(importTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(importTextLayout.createSequentialGroup()
                        .addGroup(importTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel7)
                            .addComponent(jLabel8)
                            .addComponent(jLabel9)
                            .addComponent(jLabel10)
                            .addComponent(jLabel11))
                        .addGap(55, 55, 55)
                        .addGroup(importTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(fieldterm)
                            .addComponent(linestart, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(fieldencl, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(fieldesc, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(lineterm, javax.swing.GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE))
                        .addGroup(importTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(importTextLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(importTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel12)
                                    .addComponent(jLabel13))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(importTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(tableCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(databaseCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 149, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, importTextLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(importTextImportButton)
                                .addContainerGap())))
                    .addGroup(importTextLayout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(importTextfilename)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        importTextLayout.setVerticalGroup(
            importTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(importTextLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(importTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(importTextfilename))
                .addGap(31, 31, 31)
                .addGroup(importTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(fieldterm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(databaseCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12))
                .addGap(20, 20, 20)
                .addGroup(importTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(fieldencl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tableCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13))
                .addGap(22, 22, 22)
                .addGroup(importTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(fieldesc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(23, 23, 23)
                .addGroup(importTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(lineterm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addGroup(importTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(linestart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(importTextImportButton))
                .addContainerGap(40, Short.MAX_VALUE))
        );

        fileNamerDialog.setMinimumSize(new java.awt.Dimension(397, 295));
        fileNamerDialog.setResizable(false);

        jLabel14.setText("Output file name:");

        fileNamerName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileNamerNameActionPerformed(evt);
            }
        });

        jLabel15.setText("Selected directory:");

        exportButton.setText("Export");
        exportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportButtonActionPerformed(evt);
            }
        });

        exportdbCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        exportdbCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportdbComboActionPerformed(evt);
            }
        });

        directorySelector.setText("Select directory");
        directorySelector.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                directorySelectorActionPerformed(evt);
            }
        });

        exporttblCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel16.setText("Database:");

        jLabel17.setText("Table:");

        fileWarning.setForeground(new java.awt.Color(255, 0, 0));
        fileWarning.setText("Provide a file name!");

        javax.swing.GroupLayout fileNamerDialogLayout = new javax.swing.GroupLayout(fileNamerDialog.getContentPane());
        fileNamerDialog.getContentPane().setLayout(fileNamerDialogLayout);
        fileNamerDialogLayout.setHorizontalGroup(
            fileNamerDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(fileNamerName, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fileNamerDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fileWarning)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 183, Short.MAX_VALUE)
                .addComponent(exportButton))
            .addGroup(fileNamerDialogLayout.createSequentialGroup()
                .addComponent(exportdbCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(exporttblCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(fileNamerDialogLayout.createSequentialGroup()
                .addGroup(fileNamerDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(fileNamerDialogLayout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel17))
                    .addGroup(fileNamerDialogLayout.createSequentialGroup()
                        .addGroup(fileNamerDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel14)
                            .addComponent(jLabel15)
                            .addComponent(directorySelector))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(selectedDirectory, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        fileNamerDialogLayout.setVerticalGroup(
            fileNamerDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fileNamerDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fileNamerDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(jLabel17))
                .addGap(4, 4, 4)
                .addGroup(fileNamerDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exportdbCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exporttblCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 45, Short.MAX_VALUE)
                .addComponent(directorySelector)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectedDirectory, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel14)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fileNamerName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(fileNamerDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exportButton)
                    .addComponent(fileWarning))
                .addGap(19, 19, 19))
        );

        createDatabaseDialog.setMinimumSize(new java.awt.Dimension(278, 145));
        createDatabaseDialog.setResizable(false);

        jLabel18.setText("Database Name:");

        createDbButton.setText("Create");
        createDbButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createDbButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout createDatabaseDialogLayout = new javax.swing.GroupLayout(createDatabaseDialog.getContentPane());
        createDatabaseDialog.getContentPane().setLayout(createDatabaseDialogLayout);
        createDatabaseDialogLayout.setHorizontalGroup(
            createDatabaseDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, createDatabaseDialogLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(createDbButton)
                .addContainerGap())
            .addGroup(createDatabaseDialogLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(createDatabaseDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel18)
                    .addComponent(createDbText, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        createDatabaseDialogLayout.setVerticalGroup(
            createDatabaseDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(createDatabaseDialogLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(jLabel18)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(createDbText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(createDbButton)
                .addContainerGap(50, Short.MAX_VALUE))
        );

        dropDatabaseDialog.setMinimumSize(new java.awt.Dimension(278, 145));
        dropDatabaseDialog.setResizable(false);

        jLabel19.setText("Drop Database:");

        dropDbCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        dropDbButton.setText("Drop");
        dropDbButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dropDbButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout dropDatabaseDialogLayout = new javax.swing.GroupLayout(dropDatabaseDialog.getContentPane());
        dropDatabaseDialog.getContentPane().setLayout(dropDatabaseDialogLayout);
        dropDatabaseDialogLayout.setHorizontalGroup(
            dropDatabaseDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dropDatabaseDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dropDatabaseDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel19)
                    .addComponent(dropDbCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(26, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dropDatabaseDialogLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(dropDbButton)
                .addContainerGap())
        );
        dropDatabaseDialogLayout.setVerticalGroup(
            dropDatabaseDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dropDatabaseDialogLayout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(jLabel19)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dropDbCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(dropDbButton)
                .addContainerGap(35, Short.MAX_VALUE))
        );

        dropTableDialog.setMinimumSize(new java.awt.Dimension(444, 209));
        dropTableDialog.setResizable(false);

        jLabel20.setText("Databases:");

        jLabel21.setText("Table to Drop:");

        dropTblDbCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        dropTblDbCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dropTblDbComboActionPerformed(evt);
            }
        });

        dropTblCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        dropTblButton.setText("Drop");
        dropTblButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dropTblButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout dropTableDialogLayout = new javax.swing.GroupLayout(dropTableDialog.getContentPane());
        dropTableDialog.getContentPane().setLayout(dropTableDialogLayout);
        dropTableDialogLayout.setHorizontalGroup(
            dropTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dropTableDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dropTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(dropTableDialogLayout.createSequentialGroup()
                        .addComponent(jLabel20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(dropTblDbCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(dropTableDialogLayout.createSequentialGroup()
                        .addComponent(jLabel21)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(dropTblCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(248, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dropTableDialogLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(dropTblButton)
                .addContainerGap())
        );
        dropTableDialogLayout.setVerticalGroup(
            dropTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dropTableDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dropTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(dropTblDbCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(74, 74, 74)
                .addGroup(dropTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(dropTblCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dropTblButton)
                .addContainerGap(44, Short.MAX_VALUE))
        );

        createTableDialog.setTitle("Create a new table.");
        createTableDialog.setMinimumSize(new java.awt.Dimension(526, 540));
        createTableDialog.setResizable(false);

        jLabel22.setText("Table Name:");

        createTableName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createTableNameActionPerformed(evt);
            }
        });

        jScrollPane2.setViewportView(createTableStatement);

        jLabel23.setText("Statement Builder");

        createTableExecute.setText("Execute");
        createTableExecute.setEnabled(false);
        createTableExecute.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createTableExecuteActionPerformed(evt);
            }
        });

        createTableAddName.setText("Add");
        createTableAddName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createTableAddNameActionPerformed(evt);
            }
        });

        jLabel24.setText("Column Name:");

        jLabel25.setText("Data Type:");

        createTableDataTypes.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        createTableDataTypes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createTableDataTypesActionPerformed(evt);
            }
        });

        jLabel26.setText("Constraints:");

        createTableAddColumn.setText("Add");
        createTableAddColumn.setEnabled(false);
        createTableAddColumn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createTableAddColumnActionPerformed(evt);
            }
        });

        jLabel27.setText("Debug console");

        jScrollPane6.setViewportView(createTableDebugPane);

        createTableDbNames.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel28.setText("Database:");

        javax.swing.GroupLayout createTableDialogLayout = new javax.swing.GroupLayout(createTableDialog.getContentPane());
        createTableDialog.getContentPane().setLayout(createTableDialogLayout);
        createTableDialogLayout.setHorizontalGroup(
            createTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(createTableDialogLayout.createSequentialGroup()
                .addComponent(jLabel23)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(jScrollPane2)
            .addGroup(createTableDialogLayout.createSequentialGroup()
                .addGroup(createTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(createTableDialogLayout.createSequentialGroup()
                        .addComponent(jLabel27)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(createTableExecute))
                    .addGroup(createTableDialogLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(createTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSeparator1)
                            .addGroup(createTableDialogLayout.createSequentialGroup()
                                .addGroup(createTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel24)
                                    .addComponent(jLabel25))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(createTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(createTableDataTypes, javax.swing.GroupLayout.PREFERRED_SIZE, 133, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(createTableColumnName, javax.swing.GroupLayout.PREFERRED_SIZE, 133, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(createTableDialogLayout.createSequentialGroup()
                                .addGroup(createTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel22)
                                    .addComponent(jLabel28))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                                .addGroup(createTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(createTableDbNames, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(createTableName, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(35, 35, 35)
                                .addComponent(createTableAddName)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 168, Short.MAX_VALUE))
                            .addGroup(createTableDialogLayout.createSequentialGroup()
                                .addComponent(jLabel26)
                                .addGap(31, 31, 31)
                                .addComponent(createTableExtra))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, createTableDialogLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(createTableAddColumn)))))
                .addContainerGap())
            .addComponent(jScrollPane6)
        );
        createTableDialogLayout.setVerticalGroup(
            createTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(createTableDialogLayout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(createTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(createTableName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(createTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(createTableDbNames, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel28)
                    .addComponent(createTableAddName))
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(createTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel24)
                    .addComponent(createTableColumnName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(createTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel25)
                    .addComponent(createTableDataTypes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(createTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel26)
                    .addComponent(createTableExtra, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(createTableAddColumn)
                .addGap(5, 5, 5)
                .addComponent(jLabel23)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(createTableDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(createTableExecute, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel27, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SwingBench");
        setMinimumSize(new java.awt.Dimension(700, 635));
        setPreferredSize(new java.awt.Dimension(700, 635));
        setResizable(false);

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

        jMenu2.setText("I/O");

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

        exportSQL.setText("Export .sql");
        exportSQL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportSQLActionPerformed(evt);
            }
        });
        jMenu2.add(exportSQL);

        exportTXT.setText("Export .txt");
        exportTXT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportTXTActionPerformed(evt);
            }
        });
        jMenu2.add(exportTXT);

        mainMenuBar.add(jMenu2);

        jMenu3.setText("Query");
        jMenu3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenu3ActionPerformed(evt);
            }
        });

        customQueryMenuItem.setText("Custom Query");
        customQueryMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customQueryMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(customQueryMenuItem);

        createDbMenuItem.setText("Create DB");
        createDbMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createDbMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(createDbMenuItem);

        createTblMenuItem.setText("Create Table");
        createTblMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createTblMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(createTblMenuItem);

        dropDbMenuItem.setText("Drop DB");
        dropDbMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dropDbMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(dropDbMenuItem);

        dropTblMenuItem.setText("Drop Table");
        dropTblMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dropTblMenuItemActionPerformed(evt);
            }
        });
        jMenu3.add(dropTblMenuItem);

        mainMenuBar.add(jMenu3);

        setJMenuBar(mainMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(schemaScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE))
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
        importText.setVisible(false);
        fileNamerDialog.setVisible(false);
        queryDialog.setVisible(false);
        fileChooser.setVisible(false);
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
                    long start_time = System.nanoTime();
                    String db = selPath.getPath()[1].toString();
                    String tbl = selPath.getPath()[2].toString();
                    
                    System.out.println("Create Table Worker");

                    worker1 = new CreateTableWorker(selectedTable,db,tbl,"");
                    worker1.addPropertyChangeListener((PropertyChangeEvent event) -> {
                        switch (event.getPropertyName()) {
                            case "progress":
                                tableBuilderProgressBar.setIndeterminate(false);
                                tableBuilderProgressBar.setValue((Integer) event.getNewValue());
                                break;
                            case "state":
                                switch ((StateValue) event.getNewValue()) {

                                    case STARTED:
                                    case PENDING:
                                        tableBuilderProgressBar.setIndeterminate(true);
                                        loadingDialog.setVisible(true);
                                        break;
                                    case DONE:
                                        loadingDialog.setVisible(false);
                                        worker1 = null;
//                                        selectedTable.updateUI();
                                        break;
                                }
                                break;
                        }
                    });
                    
                    System.out.println("Executing");
                    worker1.execute();

                    long end_time = System.nanoTime();
                    double difference = (end_time - start_time)/1e9;
                    String time = String.format("%.2f", difference);
                    debugPane.getStyledDocument().insertString(debugPane.getStyledDocument().getLength(), "Statement: SELECT * FROM " + db + "." + tbl + ";\n", null);
                } catch (BadLocationException ex) {
                    Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }//GEN-LAST:event_schemaTreeMouseClicked

    private void importSQLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importSQLActionPerformed
        // TODO add your handling code here:
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
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
        try {
            // TODO add your handling code here:
            importTextfilename.setText("");
            fieldencl.setText("");
            fieldterm.setText("\\t");
            fieldesc.setText("\\\\");
            linestart.setText("");
            lineterm.setText("\\n");
            ArrayList<String> dbNames = new ArrayList<>();
            importTextImportButton.setEnabled(false);

            DatabaseMetaData md = conn.getMetaData();
            ResultSet db = md.getCatalogs();

            while(db.next()) {
                dbNames.add(db.getString(1));
            }

            databaseCombo.setModel(new DefaultComboBoxModel(dbNames.toArray()));
            tableCombo.setEnabled(false);

            importText.setVisible(true);
        } catch (SQLException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
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

    private void createDbMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createDbMenuItemActionPerformed
        // TODO add your handling code here:
        createDatabaseDialog.setVisible(true);
    }//GEN-LAST:event_createDbMenuItemActionPerformed

    private void customQueryMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customQueryMenuItemActionPerformed
        // TODO add your handling code here:
        queryDialog.setVisible(true);
    }//GEN-LAST:event_customQueryMenuItemActionPerformed

    private void customQuerySubmitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customQuerySubmitButtonActionPerformed
        try {
            // TODO add your handling code here:
            importSQL(new Scanner(customQueryPane.getText()),true);
            customQueryPane.setText(null);
        } catch (SQLException | FileNotFoundException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
            try {
                customQueryDebugPane.getStyledDocument().insertString(customQueryDebugPane.getStyledDocument().getLength(), ex + "\n", null);
            } catch (BadLocationException ex1) {
                Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }//GEN-LAST:event_customQuerySubmitButtonActionPerformed

    private void exportSQLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSQLActionPerformed
        try {
            // TODO add your handling code here:
            ArrayList<String> dbNames = new ArrayList<>();
            exportButton.setEnabled(false);
            fileWarning.setVisible(false);
            fileNamerName.setText("");
            selectedDirectory.setText("");

            DatabaseMetaData md = conn.getMetaData();
            ResultSet db = md.getCatalogs();

            while(db.next()) {
                dbNames.add(db.getString(1));
            }

            exportdbCombo.setModel(new DefaultComboBoxModel(dbNames.toArray()));
            exporttblCombo.setEnabled(false);
            exportBoolean = false;
            fileNamerDialog.setVisible(true);
        } catch (SQLException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_exportSQLActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = fileChooser.showOpenDialog(null);


        if (returnVal == JFileChooser.APPROVE_OPTION){
            importFile = fileChooser.getSelectedFile();
            importTextfilename.setText(importFile.getName());
            importTextImportButton.setEnabled(true);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void importTextImportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importTextImportButtonActionPerformed
        // TODO add your handling code here:
        String query = "load data local infile '" +
                importFile.getPath() +
                "' into table " +
                databaseCombo.getSelectedItem().toString() +
                "." +
                tableCombo.getSelectedItem().toString() +
                " fields terminated by '" +
                fieldterm.getText() +
                "' enclosed by '" +
                fieldencl.getText() +
                "' escaped by '" +
                fieldesc.getText() +
                "' lines terminated by '" +
                lineterm.getText() +
                "' starting by '" +
                linestart.getText() +
                "'";
        try {
            Statement stmt = conn.createStatement();
            stmt.execute(query);
            debugPane.getStyledDocument().insertString(debugPane.getStyledDocument().getLength(), "Statement: " + query + "\n", null);
            importText.setVisible(false);
        } catch (SQLException | BadLocationException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
            try {
                debugPane.getStyledDocument().insertString(debugPane.getStyledDocument().getLength(), ex + "\n", null);
            } catch (BadLocationException ex1) {
                Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }//GEN-LAST:event_importTextImportButtonActionPerformed

    private void databaseComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_databaseComboActionPerformed
     try {
            // TODO add your handling code here:
            String dbName2 = databaseCombo.getSelectedItem().toString();
            ArrayList<String> tblNames2 = new ArrayList<>();

            DatabaseMetaData md = conn.getMetaData();
            String[] types = {"TABLE", "VIEW"};
            ResultSet tbl = md.getTables(dbName2, null, "%", types);
            boolean emptySet = false;
            while(tbl.next()) {
                tblNames2.add(tbl.getString("TABLE_NAME"));
                emptySet = true;
            }
            if (!emptySet){
                dropTblButton.setEnabled(false);
            }
            else{
                dropTblButton.setEnabled(true);
            }
            tbl.close();

            tableCombo.setModel(new DefaultComboBoxModel(tblNames2.toArray()));
            tableCombo.setEnabled(emptySet);
        } catch (SQLException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_databaseComboActionPerformed

    private void fieldenclActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fieldenclActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_fieldenclActionPerformed

    private void linetermActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_linetermActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_linetermActionPerformed

    private void fieldescActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fieldescActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_fieldescActionPerformed

    private void exportTXTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportTXTActionPerformed
        // TODO add your handling code here:
        try {
            // TODO add your handling code here:
            ArrayList<String> dbNames = new ArrayList<>();
            exportButton.setEnabled(false);
            fileWarning.setVisible(false);
            fileNamerName.setText("");
            selectedDirectory.setText("");

            DatabaseMetaData md = conn.getMetaData();
            ResultSet db = md.getCatalogs();

            while(db.next()) {
                dbNames.add(db.getString(1));
            }

            exportdbCombo.setModel(new DefaultComboBoxModel(dbNames.toArray()));
            exporttblCombo.setEnabled(false);
            exportBoolean = true;
            fileNamerDialog.setVisible(true);
        } catch (SQLException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_exportTXTActionPerformed

    private void fileNamerNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileNamerNameActionPerformed
        // TODO add your handling code here:
        if (fileNamerName.getText().equals("")){
            exportButton.setEnabled(false);
        }
        else{
            exportButton.setEnabled(true);
        }
    }//GEN-LAST:event_fileNamerNameActionPerformed

    private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportButtonActionPerformed
        // TODO add your handling code here:
        if (fileNamerName.getText().equals("")){
            fileWarning.setVisible(true);
        }
        else
        {
            try {
                exportSQL(fileNamerName.getText());
                fileNamerDialog.setVisible(false);
            } catch (SQLException ex) {
                Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_exportButtonActionPerformed

    private void directorySelectorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_directorySelectorActionPerformed
        // TODO add your handling code here:
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fileChooser.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION){
            dir = fileChooser.getCurrentDirectory().toString();
            dir = dir + "/" + fileChooser.getSelectedFile().getName();
            selectedDirectory.setText(dir);
            exportButton.setEnabled(true);
        }
    }//GEN-LAST:event_directorySelectorActionPerformed

    private void exportdbComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportdbComboActionPerformed
        // TODO add your handling code here:
        try {
            // TODO add your handling code here:
            String dbName2 = exportdbCombo.getSelectedItem().toString();
            ArrayList<String> tblNames2 = new ArrayList<>();

            DatabaseMetaData md = conn.getMetaData();
            String[] types = {"TABLE", "VIEW"};
            ResultSet tbl = md.getTables(dbName2, null, "%", types);
            boolean emptySet = false;
            while(tbl.next()) {
                tblNames2.add(tbl.getString("TABLE_NAME"));
                emptySet = true;
            }
            tbl.close();

            exporttblCombo.setModel(new DefaultComboBoxModel(tblNames2.toArray()));
            if (exportBoolean){
                exporttblCombo.setEnabled(emptySet);
            }
        } catch (SQLException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_exportdbComboActionPerformed

    private void tableComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tableComboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tableComboActionPerformed

    private void AbortUpdateTableButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AbortUpdateTableButtonActionPerformed
        // TODO add your handling code here:
        worker1.cancel(true);
    }//GEN-LAST:event_AbortUpdateTableButtonActionPerformed

    private void createDbButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createDbButtonActionPerformed
        // TODO add your handling code here:
        String createDbName = createDbText.getText();
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE DATABASE " + createDbName + ";");
            debugPane.getStyledDocument().insertString(debugPane.getStyledDocument().getLength(), "Database '" + createDbName + "' created successfully.\n", null);
            createDatabaseDialog.setVisible(false);
            jMenu_refreshButton.doClick();

        } catch (SQLException | BadLocationException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
        }

    }//GEN-LAST:event_createDbButtonActionPerformed

    private void dropDbButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dropDbButtonActionPerformed
        // TODO add your handling code here:
        String createDbName = dropDbCombo.getSelectedItem().toString();
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("DROP DATABASE " + createDbName + ";");
            debugPane.getStyledDocument().insertString(debugPane.getStyledDocument().getLength(), "Database '" + createDbName + "' dropped successfully.\n", null);
            dropDatabaseDialog.setVisible(false);
            jMenu_refreshButton.doClick();

        } catch (SQLException | BadLocationException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_dropDbButtonActionPerformed

    private void dropDbMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dropDbMenuItemActionPerformed
        try {
            // TODO add your handling code here:
            ArrayList<String> dbNames = new ArrayList<>();
            DatabaseMetaData md = conn.getMetaData();
            ResultSet db = md.getCatalogs();

            while(db.next()) {
                dbNames.add(db.getString(1));
            }

            dropDbCombo.setModel(new DefaultComboBoxModel(dbNames.toArray()));

            dropDatabaseDialog.setVisible(true);
        } catch (SQLException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_dropDbMenuItemActionPerformed

    private void dropTblMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dropTblMenuItemActionPerformed
        // TODO add your handling code here:
        try {
            // TODO add your handling code here:
            ArrayList<String> dbNames = new ArrayList<>();
            ArrayList<String> tblNames = new ArrayList<>();
            DatabaseMetaData md = conn.getMetaData();
            ResultSet db = md.getCatalogs();

            while(db.next()) {
                dbNames.add(db.getString(1));
            }

            dropTblDbCombo.setModel(new DefaultComboBoxModel(dbNames.toArray()));
            String[] types = {"TABLE", "VIEW"};
            ResultSet tbl = md.getTables(dropTblDbCombo.getSelectedItem().toString(), null, "%", types);
            boolean emptySet = false;
            while(tbl.next()) {
                tblNames.add(tbl.getString("TABLE_NAME"));
                emptySet = true;
            }
            if (!emptySet){
                dropTblButton.setEnabled(false);
            }
            else{
                dropTblButton.setEnabled(true);
            }
            tbl.close();

            dropTblCombo.setModel(new DefaultComboBoxModel(tblNames.toArray()));

            dropTableDialog.setVisible(true);
        } catch (SQLException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_dropTblMenuItemActionPerformed

    private void dropTblDbComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dropTblDbComboActionPerformed
        // TODO add your handling code here:
        try {
            // TODO add your handling code here:
            ArrayList<String> tblNames = new ArrayList<>();
            DatabaseMetaData md = conn.getMetaData();
            ResultSet db = md.getCatalogs();

            String[] types = {"TABLE", "VIEW"};
            ResultSet tbl = md.getTables(dropTblDbCombo.getSelectedItem().toString(), null, "%", types);
            boolean emptySet = false;
            while(tbl.next()) {
                tblNames.add(tbl.getString("TABLE_NAME"));
                emptySet = true;
            }
            tbl.close();
            if (!emptySet){
                dropTblButton.setEnabled(false);
            }
            else{
                dropTblButton.setEnabled(true);
            }

            dropTblCombo.setModel(new DefaultComboBoxModel(tblNames.toArray()));

            dropTableDialog.setVisible(true);
        } catch (SQLException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_dropTblDbComboActionPerformed

    private void dropTblButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dropTblButtonActionPerformed
        // TODO add your handling code here:
        String dropTblName = dropTblDbCombo.getSelectedItem().toString() + "." +
                dropTblCombo.getSelectedItem().toString();
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("DROP TABLE " + dropTblName + ";");
            debugPane.getStyledDocument().insertString(debugPane.getStyledDocument().getLength(), "Table '" + dropTblName + "' dropped successfully.\n", null);
            dropTableDialog.setVisible(false);
            jMenu_refreshButton.doClick();

        } catch (SQLException | BadLocationException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_dropTblButtonActionPerformed

    private void jMenu3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenu3ActionPerformed

    private void createTableNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createTableNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_createTableNameActionPerformed

    private void createTableExecuteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createTableExecuteActionPerformed
        try {
            // TODO add your handling code here:
            Statement query = conn.createStatement();
            query.execute("USE " + createTableDbNames.getSelectedItem().toString() + ";");
            query.execute(createTableStatement.getText());
            createTableDebugPane.getStyledDocument().insertString(createTableDebugPane.getStyledDocument().getLength(), "Table Created\n", null);
            query.close();
            jMenu_refreshButton.doClick();
            
        } catch (SQLException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
            try {
                createTableDebugPane.getStyledDocument().insertString(createTableDebugPane.getStyledDocument().getLength(), ex + "\n", null);
            } catch (BadLocationException ex1) {
                Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } catch (BadLocationException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_createTableExecuteActionPerformed

    private void createTableAddNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createTableAddNameActionPerformed
        // TODO add your handling code here:
        createTableNameS = "CREATE TABLE " + createTableName.getText() + " (";
        createTableQueryS = createTableNameS; 
        createTableStatement.setText(createTableQueryS);
        createTableAddColumn.setEnabled(true);
    }//GEN-LAST:event_createTableAddNameActionPerformed

    private void createTableAddColumnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createTableAddColumnActionPerformed
        // TODO add your handling code here:
        createTableColumnS = "";
        if (createTableExecute.isEnabled()){
            createTableColumnS += ", ";
            createTableQueryS = createTableQueryS.substring(0, createTableQueryS.length()-2);
        }
        
        createTableColumnS += createTableColumnName.getText() + " " + 
                createTableDataTypes.getSelectedItem().toString() + " " +
                createTableExtra.getText() + ");";
        createTableQueryS += createTableColumnS;
        
        createTableStatement.setText(createTableQueryS);        
        createTableExecute.setEnabled(true);
    }//GEN-LAST:event_createTableAddColumnActionPerformed

    private void createTblMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createTblMenuItemActionPerformed
        try {
            // TODO add your handling code here:
            createTableDataTypes.setModel(new DefaultComboBoxModel(types));
            
            ArrayList<String> dbNames = new ArrayList<>();
            
            DatabaseMetaData md = conn.getMetaData();
            ResultSet db = md.getCatalogs();

            while(db.next()) {
                dbNames.add(db.getString(1));
            }

            createTableDbNames.setModel(new DefaultComboBoxModel(dbNames.toArray()));
            
            createTableAddColumn.setEnabled(false);
            createTableExecute.setEnabled(false);
            createTableNameS = "";
            createTableColumnS = "";
            createTableQueryS = "";
            createTableName.setText("");
            createTableColumnName.setText("");
            createTableExtra.setText("");
            
            createTableDialog.setVisible(true);
        } catch (SQLException ex) {
            Logger.getLogger(MainApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_createTblMenuItemActionPerformed

    private void createTableDataTypesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createTableDataTypesActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_createTableDataTypesActionPerformed

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
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainApplication.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new MainApplication().setVisible(false);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AbortUpdateTableButton;
    private javax.swing.JButton connectButton;
    private javax.swing.JDialog connectionDialog;
    private javax.swing.JDialog createDatabaseDialog;
    private javax.swing.JButton createDbButton;
    private javax.swing.JMenuItem createDbMenuItem;
    private javax.swing.JTextField createDbText;
    private javax.swing.JButton createTableAddColumn;
    private javax.swing.JButton createTableAddName;
    private javax.swing.JTextField createTableColumnName;
    private javax.swing.JComboBox createTableDataTypes;
    private javax.swing.JComboBox createTableDbNames;
    private javax.swing.JTextPane createTableDebugPane;
    private javax.swing.JDialog createTableDialog;
    private javax.swing.JButton createTableExecute;
    private javax.swing.JTextField createTableExtra;
    private javax.swing.JTextField createTableName;
    private javax.swing.JEditorPane createTableStatement;
    private javax.swing.JMenuItem createTblMenuItem;
    private javax.swing.JTextPane customQueryDebugPane;
    private javax.swing.JMenuItem customQueryMenuItem;
    private javax.swing.JEditorPane customQueryPane;
    private javax.swing.JButton customQuerySubmitButton;
    private javax.swing.JTable customQueryTable;
    private javax.swing.JComboBox databaseCombo;
    private javax.swing.JLabel debugConsoleLabel;
    private javax.swing.JTextPane debugPane;
    private javax.swing.JScrollPane debugScrollPane;
    private javax.swing.JLabel dialog_error_label;
    private javax.swing.JPasswordField dialog_password;
    private javax.swing.JTextField dialog_url;
    private javax.swing.JTextField dialog_username;
    private javax.swing.JButton directorySelector;
    private javax.swing.JDialog dropDatabaseDialog;
    private javax.swing.JButton dropDbButton;
    private javax.swing.JComboBox dropDbCombo;
    private javax.swing.JMenuItem dropDbMenuItem;
    private javax.swing.JDialog dropTableDialog;
    private javax.swing.JButton dropTblButton;
    private javax.swing.JComboBox dropTblCombo;
    private javax.swing.JComboBox dropTblDbCombo;
    private javax.swing.JMenuItem dropTblMenuItem;
    private javax.swing.JButton exportButton;
    private javax.swing.JMenuItem exportSQL;
    private javax.swing.JMenuItem exportTXT;
    private javax.swing.JComboBox exportdbCombo;
    private javax.swing.JComboBox exporttblCombo;
    private javax.swing.JTextField fieldencl;
    private javax.swing.JTextField fieldesc;
    private javax.swing.JTextField fieldterm;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JDialog fileNamerDialog;
    private javax.swing.JTextField fileNamerName;
    private javax.swing.JLabel fileWarning;
    private javax.swing.JMenuItem importSQL;
    private javax.swing.JMenuItem importTXT;
    private javax.swing.JDialog importText;
    private javax.swing.JButton importTextImportButton;
    private javax.swing.JLabel importTextfilename;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuItem jMenu_disconnectButton;
    private javax.swing.JMenuItem jMenu_exitButton;
    private javax.swing.JMenuItem jMenu_refreshButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextField linestart;
    private javax.swing.JTextField lineterm;
    private javax.swing.JDialog loadingDialog;
    private javax.swing.JMenuBar mainMenuBar;
    private javax.swing.JDialog queryDialog;
    private javax.swing.JCheckBox rememberMeCheckBox;
    private javax.swing.JScrollPane schemaScrollPane;
    private javax.swing.JTree schemaTree;
    private javax.swing.JLabel selectedDirectory;
    private javax.swing.JTable selectedTable;
    private javax.swing.JProgressBar tableBuilderProgressBar;
    private javax.swing.JComboBox tableCombo;
    private javax.swing.JScrollPane tableScrollPane;
    // End of variables declaration//GEN-END:variables
}
