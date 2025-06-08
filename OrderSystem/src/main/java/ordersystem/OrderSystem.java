package ordersystem;

import org.json.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;



public class OrderSystem extends JFrame {
    private JComboBox<String> storeSelector;
    private JTextArea cartArea;
    private JPanel menuPanel;
    private JButton checkoutButton;
    private JButton couponButton;
    private JButton clearCartButton;
    private String[] storeFiles = {"store1.json", "store2.json", "store3.json"};
    private String currentCoupon = "";
    private double discountRate = 1.0;
    private java.util.List<Item> cart = new ArrayList<>();
    private int orderNumber = 1;
    private String currentStoreName = "丘蒔堂";

    public OrderSystem() {
        Font defaultFont = new Font("微軟正黑體", Font.PLAIN, 16);
    UIManager.put("Label.font", defaultFont);
    UIManager.put("Button.font", defaultFont);
    UIManager.put("TextArea.font", defaultFont);
    UIManager.put("ComboBox.font", defaultFont);
    UIManager.put("TextField.font", defaultFont);
    UIManager.put("Spinner.font", defaultFont);

        setTitle("點餐系統（分類版）");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 235, 220));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(210, 180, 140));
        storeSelector = new JComboBox<>(new String[]{"丘蒔堂", "緒食", "Twins手工蛋餅"});
        storeSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                        boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                return label;
            }
        });

        storeSelector.addActionListener(e -> {
            if (!cart.isEmpty()) {
                ImageIcon warnIcon = new ImageIcon(getClass().getClassLoader().getResource("quemark.jpg"));
Image img = warnIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
ImageIcon resizedIcon = new ImageIcon(img);

int result = JOptionPane.showConfirmDialog(this,
    "切換店家會清購物車，確定要切換嗎？", "提示",
    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, resizedIcon);

                if (result != JOptionPane.YES_OPTION) {
                    storeSelector.setSelectedItem(currentStoreName); // 回復選項
                    return;
                }
                cart.clear();
                updateCartDisplay();
            }
            int index = storeSelector.getSelectedIndex();
            currentStoreName = storeSelector.getSelectedItem().toString();
            loadMenu(storeFiles[index]);
        });
        
        topPanel.add(storeSelector, BorderLayout.WEST);

        couponButton = new JButton("輸入折扣碼");
        couponButton.addActionListener(e -> showCouponDialog());
        topPanel.add(couponButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(new Color(250, 240, 230));
        add(new JScrollPane(menuPanel), BorderLayout.CENTER);

        JPanel cartPanel = new JPanel(new BorderLayout());
        ImageIcon bigIcon = new ImageIcon(getClass().getClassLoader().getResource("Food.jpg")); // 換成你的大圖
        Image bigImg = bigIcon.getImage().getScaledInstance(400, 150, Image.SCALE_SMOOTH); // 可調整大小
        JLabel bigImageLabel = new JLabel(new ImageIcon(bigImg));
        bigImageLabel.setHorizontalAlignment(JLabel.CENTER); // 置中對齊
        cartPanel.setBackground(new Color(250, 240, 230));
        JLabel cartImageLabel = new JLabel();
        ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("cart.jpg"));
        Image scaled = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH); // 可調整大小
        cartImageLabel.setIcon(new ImageIcon(scaled));
        cartImageLabel.setHorizontalAlignment(JLabel.CENTER); // 置中對齊
        cartImageLabel.setOpaque(true); // ❗ 必須設為 true 才會顯示背景色
        cartImageLabel.setBackground(Color.WHITE); // 設為白色背景

        cartPanel.add(cartImageLabel, BorderLayout.NORTH); // ➤ 加在最上面

        cartArea = new JTextArea(20, 20);
        cartArea.setEditable(false);
        cartPanel.add(new JScrollPane(cartArea), BorderLayout.CENTER);

        JPanel cartButtonPanel = new JPanel(new GridLayout(1, 3));
        checkoutButton = new JButton("結帳");
        checkoutButton.addActionListener(e -> {
        if (cart.isEmpty()) {
        if (cart.isEmpty()) {
    ImageIcon emptyCartIcon = new ImageIcon(getClass().getClassLoader().getResource("cart.jpg"));
    Image img = emptyCartIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
    ImageIcon resizedIcon = new ImageIcon(img);

    JOptionPane.showMessageDialog(this, "購物車為空，請先選擇餐點！", "提醒", JOptionPane.WARNING_MESSAGE, resizedIcon);
    return;
}

        return;
        }
        
        showPaymentDialog();

    });

        clearCartButton = new JButton("清除購物車");
        clearCartButton.addActionListener(e -> {
            cart.clear();
            updateCartDisplay();
        });


            JButton removeItemButton = new JButton("刪除單筆餐點");
    removeItemButton.addActionListener(e -> {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "購物車為空！");
            return;
        }

        String[] options = new String[cart.size()];
        for (int i = 0; i < cart.size(); i++) {
            Item item = cart.get(i);
            options[i] = (i + 1) + ". [" + item.category + "] " + item.name +
                        (item.note.isEmpty() ? "" : "（備註: " + item.note + "）");
        }

        String selected = (String) JOptionPane.showInputDialog(
            this,
            "選擇要刪除的項目：",
            "刪除餐點",
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]
        );

        if (selected != null) {
            int index = Integer.parseInt(selected.split("\\.")[0]) - 1;
            cart.remove(index);
            updateCartDisplay();
        }
    });

        cartButtonPanel.add(checkoutButton);
        cartButtonPanel.add(clearCartButton);
        cartButtonPanel.add(removeItemButton); // ✅ 新增這行

        // ⬇ 加在上一步刪除的位置
JPanel cartBottomPanel = new JPanel(new BorderLayout());
cartBottomPanel.setBackground(new Color(250, 240, 230));  // 背景一致
cartBottomPanel.add(bigImageLabel, BorderLayout.CENTER); // 放圖片
cartBottomPanel.add(cartButtonPanel, BorderLayout.SOUTH); // 放按鈕

cartPanel.add(cartBottomPanel, BorderLayout.SOUTH); // ➤ 把整組放入購物車底部

        cartArea.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int offset = cartArea.viewToModel(e.getPoint());
                int line = cartArea.getDocument().getDefaultRootElement().getElementIndex(offset);
                if (line >= 0 && line < cart.size()) {
                    int confirm = JOptionPane.showConfirmDialog(null, "是否刪除此項目？", "刪除確認", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        cart.remove(line);
                        updateCartDisplay();
                    }
                }
            }
        });

        add(cartPanel, BorderLayout.EAST);
        loadMenu(storeFiles[0]);
    }
    
    private boolean sendOrderToServer(String last3Digits, String paymentMethod, int pickupMinutes, JDialog[] progressDialogHolder){
        Socket socket = new Socket();
try {
    socket.connect(new java.net.InetSocketAddress("140.112.249.174", 12345), 30000);
        socket.setSoTimeout(30000);

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        StringBuilder orderBuilder = new StringBuilder();
        orderBuilder.append("訂單內容：\n");
        orderBuilder.append("店家名稱：").append(currentStoreName).append("\n"); 
        for (Item item : cart) {
            orderBuilder.append(item.name).append(" - $").append(item.price);
            if (!item.note.isEmpty()) {
                orderBuilder.append(" (備註: ").append(item.note).append(")");
            }
            orderBuilder.append("\n");
        }
        orderBuilder.append("\n手機末三碼：").append(last3Digits);
        orderBuilder.append("\n付款方式：").append(paymentMethod);
        orderBuilder.append("\n預計 ").append(pickupMinutes).append(" 分鐘後取餐");

        out.println(orderBuilder.toString());
        out.println("<<END>>");
        out.flush();
        // ➤ 顯示等待接單視窗
JDialog waitingDialog = new JDialog(this, "等待接單中", false);
waitingDialog.setSize(200, 100);
waitingDialog.setLayout(new BorderLayout());
waitingDialog.add(new JLabel("等待店家接單中...", SwingConstants.CENTER), BorderLayout.CENTER);
waitingDialog.setLocationRelativeTo(this);
waitingDialog.setVisible(true);
        

        String response;
long start = System.currentTimeMillis();
while ((response = in.readLine()) == null) {
    if (System.currentTimeMillis() - start > 30000) { // 等超過 30 秒
    ImageIcon timeoutIcon = new ImageIcon(getClass().getClassLoader().getResource("sorry.jpg"));
    Image img = timeoutIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
    ImageIcon resizedIcon = new ImageIcon(img);
    JOptionPane.showMessageDialog(this, "店家未回應，訂單失敗！", "超時", JOptionPane.ERROR_MESSAGE, resizedIcon);
    return false;
}
    Thread.sleep(100); // 小延遲避免空轉
}
waitingDialog.dispose(); // ➤ 關掉等待接單視窗（拒單/錯誤）
if (!response.contains("接受") && !response.contains("確認接單")) {
    ImageIcon denyIcon = new ImageIcon(getClass().getClassLoader().getResource("sorry.jpg"));
Image img = denyIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
ImageIcon resizedIcon = new ImageIcon(img);
JOptionPane.showMessageDialog(this, "店家拒絕接單，訂單失敗！", "拒單", JOptionPane.ERROR_MESSAGE, resizedIcon);
    return false;
}


// ✅ 接單成功立即顯示取餐明細與號碼
waitingDialog.dispose(); // ➤ 成功接單也關掉等待接單視窗
SwingUtilities.invokeLater(() -> {
    StringBuilder receipt = new StringBuilder();
    receipt.append("店家：").append(currentStoreName).append("\n");
    for (int i = 0; i < cart.size(); i++) {
        Item item = cart.get(i);
        receipt.append(i + 1).append(". [").append(item.category).append("] ")
              .append(item.name).append(" - $").append(item.price);
        if (!item.note.isEmpty()) receipt.append(" (備註: ").append(item.note).append(")");
        receipt.append("\n");
    }

    double total = cart.stream().mapToDouble(i -> i.price).sum();
    receipt.append("\n總金額：$").append(String.format("%.0f", total));
    if (discountRate < 1.0) {
        receipt.append(" → 折扣後：$").append(String.format("%.0f", total * discountRate));
    }
    receipt.append("\n手機末三碼：").append(last3Digits);

    java.time.LocalTime now = java.time.LocalTime.now();
    java.time.LocalTime finishTime = now.plusMinutes(pickupMinutes);
    String formattedTime = finishTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    receipt.append("\n預計取餐時間：").append(formattedTime);

    receipt.append("\n感謝您的訂購！");

    ImageIcon resizedIcon = new ImageIcon(
        getClass().getClassLoader().getResource("money.jpg")
    );
    Image img = resizedIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
    ImageIcon icon = new ImageIcon(img);

    JOptionPane.showMessageDialog(
        this,
        "訂單成功！取餐號碼：" + (100 + orderNumber++) +
        "\n\n--- 明細 ---\n" + receipt,
        "訊息",
        JOptionPane.INFORMATION_MESSAGE,
        icon
    );
});



        // 顯示備餐進度（包含訂單資訊）
        progressDialogHolder[0] = showPreparationDialog(pickupMinutes);

        // 等待店家傳來「請取餐」
        // ✅ 開啟新執行緒持續監聽「餐點完成」訊息
BufferedReader finalIn = in;
Socket finalSocket = socket;

new Thread(() -> {
    try {
        String line;
        while ((line = finalIn.readLine()) != null) {
            if (line.contains("取餐") || line.contains("完成")) {
                SwingUtilities.invokeLater(() -> {
    ImageIcon completeIcon = new ImageIcon(getClass().getClassLoader().getResource("finish.jpg"));
    Image img = completeIcon.getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH);
    ImageIcon resizedIcon = new ImageIcon(img);

    JOptionPane.showMessageDialog(this,
        "餐點完成，請取餐！",
        "通知",
        JOptionPane.INFORMATION_MESSAGE,
        resizedIcon
    );
    Window[] windows = Window.getWindows();
    for (Window window : windows) {
        if (window instanceof JDialog) {
            JDialog dialog = (JDialog) window;
            if (progressDialogHolder[0] != null) {
    progressDialogHolder[0].dispose();
}
            }
        }
    }
);
                break;
            }
        }
    } catch (Exception ex) {
        ex.printStackTrace();
    } 
}).start();

        

        return true;

    } catch (java.net.SocketTimeoutException e) {
        ImageIcon timeoutIcon = new ImageIcon(getClass().getClassLoader().getResource("sorry.jpg"));
    Image img = timeoutIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
    ImageIcon resizedIcon = new ImageIcon(img);

    JOptionPane.showMessageDialog(this, "連線逾時，店家未回應！", "錯誤", JOptionPane.ERROR_MESSAGE, resizedIcon);
        return false;
    } catch (Exception e) {
        e.printStackTrace();
        ImageIcon failIcon = new ImageIcon(getClass().getClassLoader().getResource("sorry.jpg"));
        Image img = failIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
        ImageIcon resizedIcon = new ImageIcon(img);
        JOptionPane.showMessageDialog(this, "送出訂單失敗！", "錯誤", JOptionPane.ERROR_MESSAGE, resizedIcon);
        return false;
    }
}

    
    

    private void loadMenu(String fileName) {
        cart.clear();
        discountRate = 1.0;
        menuPanel.removeAll();

        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(content);
            currentCoupon = obj.optString("couponCode", "");
            JSONObject menuObject = obj.getJSONObject("menu");

            for (String category : menuObject.keySet()) {
                JLabel categoryLabel = new JLabel("【" + category + "】");
                categoryLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
                categoryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                menuPanel.add(categoryLabel);

                JSONArray items = menuObject.getJSONArray(category);
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String name = item.getString("name");
                    double price = item.getDouble("price");

                    String description = item.optString("description", "");

JPanel itemPanel = new JPanel();
itemPanel.setLayout(new BorderLayout());
itemPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
itemPanel.setBackground(new Color(255, 248, 220));

// 左側：品名 + 價格 + 描述
JPanel textPanel = new JPanel();
textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
textPanel.setOpaque(false);

JLabel nameLabel = new JLabel(name + " - $" + price);
nameLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 5));
nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

JLabel descLabel = new JLabel(description);
descLabel.setFont(new Font("微軟正黑體", Font.PLAIN, 12));
descLabel.setForeground(Color.DARK_GRAY);
descLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 5));
descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

textPanel.add(nameLabel);
textPanel.add(descLabel);

// 右側：加入按鈕
JButton btn = new JButton("加入");
btn.setPreferredSize(new Dimension(60, 25));
btn.setBackground(new Color(222, 184, 135));
btn.setOpaque(true);
btn.setFocusPainted(false);
btn.addMouseListener(new MouseAdapter() {
    public void mouseEntered(MouseEvent e) {
        btn.setBackground(new Color(205, 133, 63));
    }

    public void mouseExited(MouseEvent e) {
        btn.setBackground(new Color(222, 184, 135));
    }
});

btn.addActionListener(e -> {
    JPanel inputPanel = new JPanel(new GridLayout(2, 2));
    JTextField quantityField = new JTextField("1");
    JTextField noteField = new JTextField();

    inputPanel.add(new JLabel("數量："));
    inputPanel.add(quantityField);
    inputPanel.add(new JLabel("備註："));
    inputPanel.add(noteField);

    ImageIcon addIcon = new ImageIcon(getClass().getClassLoader().getResource("add.jpg"));
Image img = addIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
ImageIcon resizedAddIcon = new ImageIcon(img);
int result = JOptionPane.showConfirmDialog(this, inputPanel, "加入餐點", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, resizedAddIcon);

    if (result == JOptionPane.OK_OPTION) {
        int quantity = 1;
        try {
            quantity = Integer.parseInt(quantityField.getText());
            if (quantity < 1) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請輸入正整數數量");
            return;
        }

        String note = noteField.getText();
        for (int w = 0; w < quantity; w++) {
            cart.add(new Item(name, price, note, category));
        }
        updateCartDisplay();
    }
});


itemPanel.add(textPanel, BorderLayout.CENTER);
itemPanel.add(btn, BorderLayout.EAST);
itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

menuPanel.add(itemPanel);

                }
                menuPanel.add(Box.createVerticalStrut(10));
            }
            menuPanel.revalidate();
            menuPanel.repaint();
            updateCartDisplay();
        } catch (Exception e) {
            e.printStackTrace();
            JLabel failLabel = new JLabel("載入菜單失敗！");
            menuPanel.add(failLabel);
        }
    }

    private void updateCartDisplay() {
        double total = 0;
        StringBuilder sb = new StringBuilder("購物車內容：\n");
        for (int i = 0; i < cart.size(); i++) {
            Item item = cart.get(i);
            sb.append(i + 1).append(". [").append(item.category).append("] ")
              .append(item.name).append(" - $").append(item.price);
            if (!item.note.isEmpty()) sb.append(" (備註: ").append(item.note).append(")");
            sb.append("\n");
            total += item.price;
        }
        sb.append("\n總金額：$").append(String.format("%.0f", total));
        if (discountRate < 1.0) {
            sb.append(" → 折扣後：$").append(String.format("%.0f", total * discountRate));
        }
        cartArea.setText(sb.toString());
    }

    private void showCouponDialog() {
        ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("Discount.jpg"));
Image img = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
ImageIcon resizedIcon = new ImageIcon(img);

JTextField inputField = new JTextField();
Object[] message = {
    "請輸入折扣碼：", inputField
};

int option = JOptionPane.showOptionDialog(this, message, "輸入折扣碼",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE,
        resizedIcon, null, null);

if (option != JOptionPane.OK_OPTION) return;
String input = inputField.getText();
        if (input == null) return;
        if (input.equals(currentCoupon)) {
            discountRate = 0.9;
            ImageIcon successIcon = new ImageIcon(getClass().getClassLoader().getResource("coupon.jpg"));
Image img1 = successIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
ImageIcon resizedSuccessIcon = new ImageIcon(img1);

JOptionPane.showMessageDialog(this,
    "✅ 折扣碼正確，九折優惠已套用！",
    "折扣成功",
    JOptionPane.INFORMATION_MESSAGE,
    resizedSuccessIcon
);
        } else {
            discountRate = 1.0;
            ImageIcon failIcon = new ImageIcon(getClass().getClassLoader().getResource("wrongCode.jpg"));
Image img2 = failIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
ImageIcon resizedFailIcon = new ImageIcon(img2);

JOptionPane.showMessageDialog(this,
    "❌ 折扣碼錯誤！",
    "錯誤",
    JOptionPane.ERROR_MESSAGE,
    resizedFailIcon
);
        }
        updateCartDisplay();
    }

    private void showPaymentDialog() {
        if (cart.size() > 30) {
            JOptionPane.showMessageDialog(this, "餐點數量過多，請來電或現場訂餐");
            return;
        }

        JDialog dialog = new JDialog(this, "付款", true);
        ImageIcon moneyIcon = new ImageIcon(getClass().getClassLoader().getResource("money.jpg"));
        Image smallImage = moneyIcon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH); // 🔽 這裡設定為 16x16
        dialog.setIconImage(smallImage);

        dialog.setSize(300, 400);
        dialog.setLayout(new GridLayout(8, 1));
        

        JLabel methodLabel = new JLabel("選擇付款方式：");
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"現金", "信用卡"});
        JTextField phoneField = new JTextField();
        phoneField.setToolTipText("輸入手機末三碼");

        JLabel timeLabel = new JLabel("預計取餐時間（分鐘）：");
        JComboBox<Integer> timeCombo = new JComboBox<>();
        for (int i = 15; i <= 60; i += 5) {
            timeCombo.addItem(i);
        }

        JPanel cardPanel = new JPanel(new BorderLayout());
        JTextField cardField = new JTextField();
        cardPanel.add(new JLabel("信用卡卡號："), BorderLayout.WEST);
        cardPanel.add(cardField, BorderLayout.CENTER);

        methodCombo.addActionListener(e -> {
            cardField.setEnabled("信用卡".equals(methodCombo.getSelectedItem()));
        });
        cardField.setEnabled(false);

        JButton confirmBtn = new JButton("確認付款");
        confirmBtn.addActionListener(e -> {
    String last3 = phoneField.getText();
    if (!last3.matches("\\d{3}")) {
        ImageIcon failIcon = new ImageIcon(getClass().getClassLoader().getResource("xxx.jpg"));
Image img = failIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
ImageIcon resizedIcon = new ImageIcon(img);
JOptionPane.showMessageDialog(this, "請輸入正確手機末三碼", "錯誤", JOptionPane.ERROR_MESSAGE, resizedIcon);

        return;
    }

    if ("信用卡".equals(methodCombo.getSelectedItem())) {
        String card = cardField.getText();
        if (!card.matches("\\d{16}")) {
            ImageIcon failIcon = new ImageIcon(getClass().getClassLoader().getResource("xxx.jpg"));
Image img = failIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
ImageIcon resizedIcon = new ImageIcon(img);
JOptionPane.showMessageDialog(this, "請輸入16位數卡號", "錯誤", JOptionPane.ERROR_MESSAGE, resizedIcon);

            return;
        }
    }

    String payment = methodCombo.getSelectedItem().toString(); // 付款方式
    int selectedTime = (Integer) timeCombo.getSelectedItem(); // 取餐時間

    dialog.dispose(); // 關掉付款視窗

    // ✅ 開啟備餐中進度條（非阻塞）
    

    // ✅ 傳送訂單給店家並等待回應（接單與完成）
    JDialog[] progressDialogHolder = new JDialog[1];  // 用陣列包住以便傳遞

boolean orderAccepted = sendOrderToServer(last3, payment, selectedTime, progressDialogHolder);

if (orderAccepted && progressDialogHolder[0] != null) {
    progressDialogHolder[0].setVisible(true);
}
    if (!orderAccepted) {
        return; // 若店家拒單，備餐視窗會自己關掉
    }

    
});


        dialog.add(methodLabel);
        dialog.add(methodCombo);
        dialog.add(new JLabel("手機末三碼："));
        dialog.add(phoneField);
        dialog.add(timeLabel);
        dialog.add(timeCombo);
        dialog.add(cardPanel);
        dialog.add(confirmBtn);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JDialog showPreparationDialog(int pickupMinutes) {
    JDialog progressDialog = new JDialog(this, "備餐中", false);
    progressDialog.setSize(400, 300);
    progressDialog.setLayout(new BorderLayout());

    JProgressBar bar = new JProgressBar(0, 100);
    bar.setValue(50); // 顯示中間狀態
    bar.setString("等待店家製作中...");
    bar.setStringPainted(true);

    JTextArea detailArea = new JTextArea();
    detailArea.setEditable(false);
    StringBuilder sb = new StringBuilder("餐點明細：\n");
    for (int i = 0; i < cart.size(); i++) {
        Item item = cart.get(i);
        sb.append(i + 1).append(". [").append(item.category).append("] ")
          .append(item.name).append(" - $").append(item.price);
        if (!item.note.isEmpty()) sb.append(" (備註: ").append(item.note).append(")");
        sb.append("\n");
    }
    double total = cart.stream().mapToDouble(i -> i.price).sum();
    sb.append("\n總金額：$").append(String.format("%.0f", total));
    if (discountRate < 1.0) {
        sb.append(" → 折扣後：$").append(String.format("%.0f", total * discountRate));
    }

    // 計算預計完成時間（現實時間格式 HH:mm）
java.time.LocalTime now = java.time.LocalTime.now();
java.time.LocalTime finishTime = now.plusMinutes(pickupMinutes);
String formattedTime = finishTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

sb.append("\n預計取餐時間：").append(formattedTime);


    detailArea.setText(sb.toString());
    detailArea.setBackground(new Color(250, 240, 230));
    detailArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    progressDialog.add(bar, BorderLayout.NORTH);
    progressDialog.add(new JScrollPane(detailArea), BorderLayout.CENTER);

    new Thread(() -> {
        try {
            Thread.sleep(10 * 60 * 1000); // 最多等 10 分鐘自動關閉
        } catch (InterruptedException ignored) {}
        SwingUtilities.invokeLater(() -> progressDialog.dispose());
    }).start();

    progressDialog.setLocationRelativeTo(this);
    progressDialog.setVisible(true);
    return progressDialog;
}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OrderSystem().setVisible(true));
    }

    private static class Item {
        String name;
        double price;
        String note;
        String category;
        Item(String name, double price, String note, String category) {
            this.name = name;
            this.price = price;
            this.note = note == null ? "" : note;
            this.category = category;
        }
    }
}