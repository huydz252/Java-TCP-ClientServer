package chatapp;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.swing.*;
import java.awt.*;

public class ChatServer {
    private static JTextArea chatArea, logArea;
    private static DefaultListModel<String> clientListModel = new DefaultListModel<>();
    private static JList<String> clientList;
    private static Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private static Map<String, ClientHandler> clientMap = Collections.synchronizedMap(new HashMap<>());
    private static JTextField inputField;
    private static JButton sendButton;

    public static void main(String[] args) {
        // ===== GUI =====
        JFrame frame = new JFrame("💬 Server Chat Control Panel");
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(new Color(245, 245, 250));

        // ===== Khu vực chat chính =====
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setBackground(new Color(255, 255, 255));

        // ===== Khu vực log =====
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setForeground(new Color(0, 100, 0));
        logArea.setBackground(new Color(240, 255, 240));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(chatArea), new JScrollPane(logArea));
        splitPane.setDividerLocation(230);
        splitPane.setResizeWeight(0.7);
        splitPane.setBorder(BorderFactory.createTitledBorder("📨 Khu vực tin nhắn / Log hệ thống"));

        frame.add(splitPane, BorderLayout.CENTER);

        // ===== Danh sách client =====
        clientList = new JList<>(clientListModel);
        clientList.setFont(new Font("Segoe UI", Font.BOLD, 13));
        clientList.setBackground(new Color(245, 250, 255));
        clientList.setBorder(BorderFactory.createTitledBorder("🟢 Người dùng trực tuyến"));
        frame.add(new JScrollPane(clientList), BorderLayout.EAST);

        // ===== Ô nhập và nút gửi =====
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 200, 255), 2),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));

        sendButton = new JButton("Gửi");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sendButton.setBackground(new Color(46, 139, 87));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        frame.add(inputPanel, BorderLayout.SOUTH);

        // ===== Cài đặt cửa sổ =====
        frame.setSize(800, 500);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        sendButton.addActionListener(e -> sendServerMessage());
        inputField.addActionListener(e -> sendServerMessage());

        // ===== Lắng nghe client =====
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(6000)) {
                logArea.append("Server đang lắng nghe tại cổng 6000...\n");
                while (true) {
                    Socket socket = serverSocket.accept();
                    String clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
                    PrintWriter tempOut = new PrintWriter(socket.getOutputStream(), true);

                    int option = JOptionPane.showConfirmDialog(
                            frame,
                            "Client " + clientInfo + " muốn kết nối.\nBạn có chấp nhận không?",
                            "Yêu cầu kết nối mới",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                    );

                    if (option == JOptionPane.YES_OPTION) {
                        tempOut.println("CONNECTED");
                        ClientHandler handler = new ClientHandler(socket);
                        clients.add(handler);
                        handler.start();
                        logArea.append("Client " + clientInfo + " được phép kết nối.\n");
                    } else {
                        tempOut.println("REJECTED");
                        logArea.append("Từ chối client: " + clientInfo + "\n");
                        socket.close();
                    }
                }
            } catch (IOException e) {
                logArea.append("Lỗi server: " + e.getMessage() + "\n");
            }
        }).start();
    }

    // ===== Gửi tin nhắn từ server tới tất cả client =====
    private static void sendServerMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            broadcast("Server: " + msg);
            inputField.setText("");
        }
    }

    // ===== Broadcast tới tất cả client =====
    private static void broadcast(String msg) {
        synchronized (clients) {
            for (ClientHandler c : clients) {
                c.sendMessage(msg);
            }
        }
        chatArea.append(msg + "\n");
    }

    // ===== Gửi danh sách online tới tất cả client =====
    private static void updateClientList() {
        StringBuilder sb = new StringBuilder("ONLINE:");
        synchronized (clientMap) {
            for (String name : clientMap.keySet()) {
                sb.append(name).append(",");
            }
        }
        String listMsg = sb.toString();
        synchronized (clients) {
            for (ClientHandler c : clients) {
                c.sendMessage(listMsg);
            }
        }
    }

    // ===== Xử lý client =====
    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;
        private boolean isRegistered = false;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public String getClientName() {
            return clientName;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // ===== Đăng ký username/password =====
                while (!isRegistered) {
                    out.println("ENTER_CREDENTIALS");
                    String msg = in.readLine();
                    if (msg == null) break;

                    if (msg.startsWith("REGISTER:")) {
                        String[] parts = msg.split(":");
                        if (parts.length == 3) {
                            String username = parts[1];
                            String password = parts[2];

                            try (FileWriter fw = new FileWriter("users.txt", true)) {
                                fw.write(LocalDateTime.now() + " | " + username + " | " + password + "\n");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            clientName = username;
                            out.println("ACCEPTED");
                            isRegistered = true;
                            clientListModel.addElement(clientName);
                            clientMap.put(clientName, this);

                            chatArea.append(clientName + " đã tham gia chat!\n");
                            updateClientList();
                        } else {
                            out.println("REJECTED");
                        }
                    } else {
                        out.println("REJECTED");
                    }
                }

                // ===== Nhận tin nhắn và yêu cầu của client =====
                String msg;
                while ((msg = in.readLine()) != null) {
                	out.println("---------------------------------");
                	if (msg.equalsIgnoreCase("PING")) {
                        out.println("PONG");

                    } else if (msg.equalsIgnoreCase("CFG_REQUEST")) {
                        InetAddress local = InetAddress.getLocalHost();
                        out.println("CFG_RESPONSE: Host=" + local.getHostName() +
                                ", IP=" + local.getHostAddress());

                    } else if (msg.equalsIgnoreCase("TIME_REQUEST")) {
                        out.println("TIME_RESPONSE: " + LocalDateTime.now());

                    } else if (msg.equalsIgnoreCase("OS_REQUEST")) {
                        out.println("OS_RESPONSE: " + System.getProperty("os.name"));

                    } else if (msg.startsWith("ECHO ")) {
                        String echoMsg = msg.substring(5);
                        out.println("ECHO_RESPONSE: " + echoMsg);

                    } else if (msg.equalsIgnoreCase("LIST_REQUEST")) {
                        StringBuilder sb = new StringBuilder("Clients: ");
                        synchronized (clients) {
                            for (ClientHandler c : clients) {
                                sb.append(c.getClientName()).append("; ");
                            }
                        }
                        out.println(sb.toString());

                    // --- Các chức năng của labels2 ---
                    } else if (msg.equalsIgnoreCase("NETRESTART")) {
                    	out.println("---------------------------------");
                        try {
                            // Dừng dịch vụ trước
                            Process stopProcess = Runtime.getRuntime().exec("net stop Dhcp");
                            stopProcess.waitFor();

                            // Khởi động lại
                            Process startProcess = Runtime.getRuntime().exec("net start Dhcp");
                            startProcess.waitFor();

                            // Đọc phản hồi lỗi nếu có
                            BufferedReader errReader = new BufferedReader(
                                new InputStreamReader(startProcess.getErrorStream())
                            );
                            StringBuilder errMsg = new StringBuilder();
                            String line;
                            while ((line = errReader.readLine()) != null) {
                                errMsg.append(line).append("\n");
                            }

                            if (errMsg.length() == 0) {
                                out.println("NETRESTART_RESPONSE: DHCP service restarted successfully (Windows)");
                            } else if (errMsg.toString().contains("already been started")) {
                                out.println("NETRESTART_RESPONSE: DHCP service was already running (Windows)");
                            } else {
                                out.println("NETRESTART_RESPONSE: Partial error:\n" + errMsg);
                            }

                        } catch (Exception e) {
                            out.println("NETRESTART_RESPONSE: Failed to restart service: " + e.getMessage());
                        }
                    }
                    else if (msg.equalsIgnoreCase("ROUTE")) {
                        try {
                            Process p = Runtime.getRuntime().exec("route print");
                            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line).append("\n");
                            }
                            out.println("ROUTE_RESPONSE:\n" + sb.toString());
                        } catch (IOException e) {
                            out.println("ROUTE_RESPONSE: Failed to execute command.");
                        }

                    } else if (msg.equalsIgnoreCase("ARP")) {
                        try {
                            Process p = Runtime.getRuntime().exec("arp -a");
                            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                sb.append(line).append("\n");
                            }
                            out.println("ARP_RESPONSE:\n" + sb.toString());
                        } catch (IOException e) {
                            out.println("ARP_RESPONSE: Failed to execute command.");
                        }

                    } else if (msg.equalsIgnoreCase("MEM")) {
                        long totalMem = Runtime.getRuntime().totalMemory();
                        long freeMem = Runtime.getRuntime().freeMemory();
                        out.println("MEM_RESPONSE: Total=" + totalMem / (1024 * 1024) + "MB, Free=" + freeMem / (1024 * 1024) + "MB");

                    } else if (msg.equalsIgnoreCase("CPU")) {
                    	ProcessHandle current = ProcessHandle.current();
                    	ProcessHandle.Info info = current.info();

                    	info.command().ifPresent(cmd ->
                    	    out.println("Command: " + cmd)
                    	);

                    	info.startInstant().ifPresent(start ->
                    	    out.println("Start Time: " + start)
                    	);

                    	info.user().ifPresent(user ->
                    	    out.println("User: " + user)
                    	);

                    	info.totalCpuDuration().ifPresentOrElse(duration -> {
                    		out.println("CPU_RESPONSE: Total process CPU time = " + duration.toMillis() + " ms");
                    	}, () -> {
                    		out.println("CPU_RESPONSE: Unable to read CPU time");
                    	});
                    }

                    else if (msg.equalsIgnoreCase("DISK")) {
                        File root = new File("C:\\");
                        long total = root.getTotalSpace() / (1024 * 1024 * 1024);
                        long free = root.getFreeSpace() / (1024 * 1024 * 1024);
                        out.println("DISK_RESPONSE: Total=" + total + "GB, Free=" + free + "GB");
                    }
                    //message
                    else if (msg.startsWith("/msg ")) {
                        String[] parts = msg.split(" ", 3);
                        if (parts.length >= 3) {
                            String target = parts[1];
                            String content = parts[2];
                            ClientHandler targetHandler = clientMap.get(target);
                            if (targetHandler != null) {
                                targetHandler.sendMessage(clientName + ": " + content);
                                this.sendMessage("Bạn -> " + target + ": " + content);
                            } else {
                                this.sendMessage("Người dùng không tồn tại: " + target);
                            }
                        }
                    } else {
                        chatArea.append(clientName + ": " + msg + "\n");
                    }
                }

            } catch (IOException e) {
                logArea.append("Client ngắt kết nối: " + clientName + "\n");
            } finally {
                try { socket.close(); } catch (IOException e) {}
                clients.remove(this);
                clientMap.remove(clientName);
                clientListModel.removeElement(clientName);

                chatArea.append(clientName + " đã rời chat.\n");
                updateClientList();
            }
        }

        public void sendMessage(String msg) {
            if (out != null) out.println(msg);
        }
    }
}
