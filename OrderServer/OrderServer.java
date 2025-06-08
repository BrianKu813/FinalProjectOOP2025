import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;

public class OrderServer {

    // 使用線程池來處理多單
    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);  // 設定最大並行數

    public static void main(String[] args) {
        int port = 12345;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("伺服器啟動，監聽埠號 " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("接收到新客戶端連線：" + clientSocket.getRemoteSocketAddress());

                // 為每個客戶端連線開啟新線程處理
                executorService.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private static final int READ_TIMEOUT_MS = 30000; // 30秒讀取逾時
        private static List<Order> orderHistory = new ArrayList<>(); // 訂單歷史
        private static final String ORDER_HISTORY_FILE = "orders.txt"; // 儲存訂單歷史的檔案

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                StringBuilder orderBuilder = new StringBuilder();
                String line;

                // 讀取直到空行或逾時
                while ((line = in.readLine()) != null) {
                    if (line.trim().isEmpty()) break;
                    orderBuilder.append(line).append("\n");
                }

                String orderContent = orderBuilder.toString();
                System.out.println("收到訂單內容：\n" + orderContent);

                if (orderContent.trim().isEmpty()) {
                    out.println("未收到任何訂單內容！");
                    socket.close();
                    return;
                }

                // 為每個訂單生成唯一的 ID 並加入訂單歷史
                String orderId = "Order-" + System.currentTimeMillis();
                Order order = new Order(orderId, orderContent, "待處理");
                orderHistory.add(order);

                // 儲存訂單歷史到檔案
                saveOrderHistoryToFile();

                // 在 EDT 內彈出確認視窗
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        int option = JOptionPane.showConfirmDialog(null,
                                "收到新訂單：\n" + orderContent + "\n\n是否接受此訂單？",
                                "訂單確認", JOptionPane.YES_NO_OPTION);

                        try {
                            if (option == JOptionPane.YES_OPTION) {
                                order.setStatus("已接受");
                                showOrderDetails(order, out);  // 顯示訂單明細
                                out.println("店家已確認接單！");
                            } else {
                                String reason = JOptionPane.showInputDialog(null, "請輸入拒絕接單的原因：");
                                if (reason == null || reason.trim().isEmpty()) {
                                    reason = "很抱歉，目前無法接單。";
                                }
                                out.println(reason);
                                order.setStatus("已拒絕");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (InterruptedException | InvocationTargetException e) {
                    e.printStackTrace();
                }

                // 改：延後關閉，直到 client 關閉連線或「完成」按下後自動關
                new Thread(() -> {
                    try {
                        // 等待客戶端結束或 10 分鐘後自動關閉
                        Thread.sleep(10 * 60 * 1000);
                        if (!socket.isClosed()) {
                            System.out.println("已自動關閉客戶端連線：" + socket.getRemoteSocketAddress());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 儲存訂單歷史到檔案
        private static void saveOrderHistoryToFile() {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(ORDER_HISTORY_FILE))) {
                for (Order order : orderHistory) {
                    writer.write(order.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 顯示訂單詳細信息
        private static void showOrderDetails(Order order, PrintWriter out) {
            String displayText = order.getContent() + "\n\n訂單狀態: " + order.getStatus();

            JFrame frame = new JFrame("訂單明細");
            frame.setSize(450, 400);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBackground(new java.awt.Color(235, 224, 210));  // 大地色系背景
            mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); // 內邊距

            JLabel titleLabel = new JLabel("訂單明細");
            titleLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 20));
            titleLabel.setForeground(new java.awt.Color(101, 67, 33));  // 深棕色
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            mainPanel.add(titleLabel, BorderLayout.NORTH);

            JTextArea textArea = new JTextArea(displayText);
            textArea.setEditable(false);
            textArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 14));
            textArea.setForeground(new java.awt.Color(84, 59, 29));  // 深棕色
            textArea.setBackground(new java.awt.Color(245, 238, 227));  // 淡米色背景

            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.getViewport().setBackground(new java.awt.Color(245, 238, 227));
            scrollPane.setBorder(BorderFactory.createLineBorder(new java.awt.Color(150, 111, 51)));

            mainPanel.add(scrollPane, BorderLayout.CENTER);

            // Add "訂單歷史" button to display order history
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15)); // 更加規劃的間距
            JButton orderHistoryButton = new JButton("訂單歷史");
            JButton completeOrderButton = new JButton("已完成");
            JButton clearOrdersButton = new JButton("清除訂單");

            orderHistoryButton.addActionListener(e -> displayOrderHistory());
            completeOrderButton.addActionListener(e -> markOrderAsCompleted(order, out)); // 呼叫此方法將訂單標記為已完成並發送給客戶端
            clearOrdersButton.addActionListener(e -> clearOrderHistory());

            // 調整按鈕樣式
            orderHistoryButton.setPreferredSize(new Dimension(120, 40));
            completeOrderButton.setPreferredSize(new Dimension(120, 40));
            clearOrdersButton.setPreferredSize(new Dimension(120, 40));

            buttonPanel.add(orderHistoryButton);
            buttonPanel.add(completeOrderButton);
            buttonPanel.add(clearOrdersButton);

            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            frame.setContentPane(mainPanel);
            frame.setVisible(true);
        }

        // 訂單標記為已完成
        private static void markOrderAsCompleted(Order order, PrintWriter out) {
            order.setStatus("已完成"); // 更新訂單狀態
            saveOrderHistoryToFile();  // 儲存更新後的訂單歷史

            out.println("訂單 " + order.getId() + " 已完成，請取餐！"); // 發送「訂單已完成」的消息給客戶端
            out.flush();
            System.out.println(">>> 送出訊息：訂單 " + order.getId() + " 已完成，請取餐！");

            // 顯示「訂單已完成」訊息
            JOptionPane.showMessageDialog(null, "訂單已標記為已完成！");
        }

        // 顯示訂單歷史
        private static void displayOrderHistory() {
            // 顯示所有訂單內容
            JFrame historyFrame = new JFrame("訂單歷史");
            historyFrame.setSize(450, 400);
            historyFrame.setLocationRelativeTo(null);
            historyFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JTextArea historyTextArea = new JTextArea();
            historyTextArea.setEditable(false);
            for (Order order : orderHistory) {
                historyTextArea.append(order.toString() + "\n\n");
            }

            JScrollPane historyScrollPane = new JScrollPane(historyTextArea);
            historyScrollPane.getViewport().setBackground(new java.awt.Color(245, 238, 227));
            historyScrollPane.setBorder(BorderFactory.createLineBorder(new java.awt.Color(150, 111, 51)));

            historyFrame.add(historyScrollPane);
            historyFrame.setVisible(true);
        }

        // 清除所有訂單歷史
        private static void clearOrderHistory() {
            orderHistory.clear(); // 清空訂單歷史
            saveOrderHistoryToFile(); // 清空後保存空的歷史文件
            JOptionPane.showMessageDialog(null, "所有訂單歷史已清除！");
        }

        // 訂單類別，包含狀態與內容
        static class Order {
            private String id;
            private String content;
            private String status;

            public Order(String id, String content, String status) {
                this.id = id;
                this.content = content;
                this.status = status;
            }

            public String getContent() {
                return content;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            @Override
            public String toString() {
                return "訂單時間: " + new java.util.Date() + "\n" + content + "\n訂單狀態: " + status + "\n====================================\n";
            }

            public String getId() {
                return id;
            }
        }
    }
}