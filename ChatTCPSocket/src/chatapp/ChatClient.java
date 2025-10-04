package chatapp;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;

public class ChatClient {
    private static JTextArea chatArea, logArea;
    private static JTextField inputField;
    private static JButton sendButton;
    private static PrintWriter out;
    private static boolean isAccepted = false;
    private static DefaultListModel<String> onlineListModel = new DefaultListModel<>();
    private static JList<String> onlineList = new JList<>(onlineListModel);
    private static String clientName = "";
    private static JFrame frame;

    public static void main(String[] args) {
        // ===== Giao diện chính =====
        frame = new JFrame("Client Chat");
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(new Color(240, 248, 255)); // xanh nhạt pastel
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ===== Khu vực chat =====
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chatArea.setBackground(new Color(250, 250, 250));
        chatArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        // ===== Khu vực log =====
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setForeground(new Color(70, 70, 70));
        logArea.setBackground(new Color(245, 245, 245));
        logArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(chatArea),
                new JScrollPane(logArea)
        );
        splitPane.setDividerLocation(200);
        splitPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        frame.add(splitPane, BorderLayout.CENTER);

        // ===== Danh sách online =====
        onlineList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        onlineList.setBackground(new Color(250, 250, 255));
        onlineList.setBorder(BorderFactory.createTitledBorder("Đang Online"));
        frame.add(new JScrollPane(onlineList), BorderLayout.EAST);

        // ===== Thanh nhập tin =====
        inputPanel();

        // ===== Thanh chức năng =====
        functionPanel();

        // ===== Hiển thị cửa sổ =====
        frame.setSize(750, 450);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // ===== Kết nối tới server =====
        connectToServer();
    }

    private static void inputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        inputPanel.setBorder(new EmptyBorder(5, 10, 10, 10));
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sendButton = new JButton("Gửi");
        sendButton.setBackground(new Color(100, 149, 237));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);
    }

    private static void functionPanel() {
        JPanel buttonPanel = new JPanel(new GridLayout(2, 6, 8, 8));
        buttonPanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        buttonPanel.setBackground(new Color(240, 248, 255));

        String[] labels1 = { "PING", "CFG", "TIME", "OS", "LIST", "ECHO" };
        String[] labels2 = { "NETSTAT", "ROUTE", "ARP", "MEM", "CPU", "DISK" };

        Color btnColor = new Color(65, 105, 225);

        JButton pingBtn = createButton(labels1[0], btnColor);
        JButton cfgBtn = createButton(labels1[1], btnColor);
        JButton timeBtn = createButton(labels1[2], btnColor);
        JButton osBtn = createButton(labels1[3], btnColor);
        JButton listBtn = createButton(labels1[4], btnColor);
        JButton echoBtn = createButton(labels1[5], btnColor);
        
        JButton netReStartBtn = createButton(labels2[0], btnColor);
        JButton routeBtn = createButton(labels2[1], btnColor);
        JButton arpBtn = createButton(labels2[2], btnColor);
        JButton memBtn = createButton(labels2[3], btnColor);
        JButton cpuBtn = createButton(labels2[4], btnColor);
        JButton diskBtn = createButton(labels2[5], btnColor);

        buttonPanel.add(pingBtn);
        buttonPanel.add(cfgBtn);
        buttonPanel.add(timeBtn);
        buttonPanel.add(osBtn);
        buttonPanel.add(listBtn);
        buttonPanel.add(echoBtn);
        
        buttonPanel.add(netReStartBtn);
        buttonPanel.add(routeBtn);
        buttonPanel.add(arpBtn);
        buttonPanel.add(memBtn);
        buttonPanel.add(cpuBtn);
        buttonPanel.add(diskBtn);
        frame.add(buttonPanel, BorderLayout.NORTH);

        // Sự kiện các nút
        pingBtn.addActionListener(e -> out.println("PING"));
        cfgBtn.addActionListener(e -> out.println("CFG_REQUEST"));
        timeBtn.addActionListener(e -> out.println("TIME_REQUEST"));
        osBtn.addActionListener(e -> out.println("OS_REQUEST"));
        listBtn.addActionListener(e -> out.println("LIST_REQUEST"));
        echoBtn.addActionListener(e -> {
            String content = JOptionPane.showInputDialog("Nhập nội dung để ECHO:");
            if (content != null && !content.trim().isEmpty()) {
                out.println("ECHO " + content);
            }
        });
        
        netReStartBtn.addActionListener(e -> out.println("NETRESTART"));
        routeBtn.addActionListener(e -> out.println("ROUTE"));
        arpBtn.addActionListener(e -> out.println("ARP"));
        memBtn.addActionListener(e -> out.println("MEM"));
        cpuBtn.addActionListener(e -> out.println("CPU"));
        diskBtn.addActionListener(e -> out.println("DISK"));

    }

    private static JButton createButton(String label, Color color) {
        JButton btn = new JButton(label);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return btn;
    }

    private static void connectToServer() {
        try {
            Socket socket = new Socket("127.0.0.1", 6000);
            logArea.append("Đã kết nối tới server!\n");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Nhận dữ liệu từ server
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        if (msg.startsWith("ONLINE:")) {
                            updateOnlineList(msg);
                        } else if (msg.equals("ENTER_CREDENTIALS")) {
                            login();
                        } else if (msg.equals("ACCEPTED")) {
                            isAccepted = true;
                            frame.setTitle("Client Chat - " + clientName);
                            logArea.append("Đăng nhập thành công với tên: " + clientName + "\n");
                        } else if (msg.equals("REJECTED")) {
                            isAccepted = false;
                            logArea.append("Đăng nhập thất bại.\n");
                        } else {
                            chatArea.append(msg + "\n");
                        }
                    }
                } catch (IOException e) {
                    logArea.append("Mất kết nối tới server!\n");
                }
            }).start();

            // Gửi tin
            sendButton.addActionListener(e -> sendMessage());
            inputField.addActionListener(e -> sendMessage());

            // Nhấn chọn user để nhắn riêng
            onlineList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    String target = onlineList.getSelectedValue();
                    if (target != null) {
                        inputField.setText("/msg " + target + " ");
                        inputField.requestFocus();
                    }
                }
            });

        } catch (IOException e) {
            logArea.append("Không thể kết nối tới server: " + e.getMessage() + "\n");
        }
    }

    private static void updateOnlineList(String msg) {
        onlineListModel.clear();
        String[] users = msg.substring(7).split(",");
        for (String u : users) {
            if (!u.isEmpty()) onlineListModel.addElement(u);
        }
    }

    private static void login() {
        String username = JOptionPane.showInputDialog("Nhập tên đăng nhập:");
        String password = JOptionPane.showInputDialog("Nhập mật khẩu:");
        if (username != null && !username.trim().isEmpty()) {
            clientName = username;
            out.println("REGISTER:" + username + ":" + password);
        }
    }

    private static void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            if (isAccepted) {
                out.println(msg);
            } else {
                logArea.append("Bạn chưa được server chấp nhận!\n");
            }
            inputField.setText("");
        }
    }
}
