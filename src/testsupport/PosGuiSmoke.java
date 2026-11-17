package testsupport;

import connectDB.ConnectDB;
import entity.SanPham;
import entity.TaiKhoan;
import graphicUI.PosFrame;
import service.NguoiDung;
import service.DichVuBanHang;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import static testsupport.TestFixtures.*;

/** Real Swing login/workers/dialogs in an isolated X server; no active desktop interaction. */
public final class PosGuiSmoke {
  private static PosFrame frame;
  private PosGuiSmoke() {}
  public static void main(String[] args) {
    try { run(); }
    catch (Throwable exception) { exception.printStackTrace(); System.exit(1); }
  }
  private static void run() throws Exception {
    TestDatabase.requireIsolated();
    Files.createDirectories(Path.of(".dev/audit-fixes/gui"));
    try (TestFixtures fixtures = new TestFixtures(); JdbcFaults faults = new JdbcFaults()) {
      check(faults.jdbcCompliant(), "GUI uses real JDBC with controlled faults");
      TaiKhoan account = fixtures.account("QUAN_LY");
      try {
        edt(() -> { frame = new PosFrame(); frame.setVisible(true); });
        screenshot("login"); login(account.getTenDangNhap());
        edt(() -> {
          try {
            Method show = PosFrame.class.getDeclaredMethod("showPage", String.class, JButton.class);
            show.setAccessible(true); show.invoke(frame, "sale", null);
          } catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
        });
        for (int width : new int[] {1024, 1280}) {
          edt(() -> frame.setSize(width, width == 1024 ? 680 : 780)); Thread.sleep(150);
          visiblePayment("empty sale " + width); screenshot("empty-" + width);
        }
        SanPham first = fixtures.product(10000), recent = fixtures.product(15);
        fixtures.purchase(first, 30, 0, true); fixtures.purchase(recent, 30, 0, true);
        staleQuote(fixtures, first, recent);
        fill(first, 1);
        for (int width : new int[] {1024, 1280}) {
          edt(() -> frame.setSize(width, width == 1024 ? 680 : 780)); Thread.sleep(150);
          visiblePayment("full sale " + width); screenshot("full-" + width);
        }
        edt(() -> {
          for (Component component : all((Container) stateField("panel")))
            if (component.getFont() != null) component.setFont(component.getFont().deriveFont(component.getFont().getSize2D() * 1.3f));
          frame.setSize(1024, 680); frame.validate();
        });
        visiblePayment("130 percent font scale 1024"); screenshot("scaled-1024");
        // Move focus through inputs. Scrollable fields must follow keyboard focus.
        edt(() -> ((JComponent) named("Mã hoặc tên sản phẩm")).requestFocusInWindow());
        Robot robot = new Robot();
        for (int index = 0; index < 12; index++) {
          robot.keyPress(java.awt.event.KeyEvent.VK_TAB); robot.keyRelease(java.awt.event.KeyEvent.VK_TAB); Thread.sleep(40);
        }
        check(value(() -> {
          Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
          return focused instanceof JComponent && !((JComponent) focused).getVisibleRect().isEmpty();
        }), "Tab navigation scrolls focused input into view");
        edt(() -> ((JTextField) named("Điểm thưởng sử dụng")).setText("1.5"));
        Thread.sleep(500);
        check(value(() -> !(Boolean) stateField("quoted") && !((JButton) stateField("btnThanhToan")).isEnabled()),
            "fractional points invalidate quote and disable payment");
        edt(() -> ((JTextField) named("Điểm thưởng sử dụng")).setText("0"));
        edt(() -> ((JComboBox<?>) named("Phương thức thanh toán")).setSelectedItem("QR"));
        awaitQuote();
        JdbcFaults.FAIL_RECEIPT_AFTER_SALE.set(true);
        payAndConfirm(true);
        waitFor(() -> value(() -> cart().getRowCount() == 0), "cart completion before receipt read");
        waitFor(() -> visibleDialog("Thanh toán thành công — lỗi hóa đơn in"), "receipt-specific success dialog");
        check(fixtures.stock(first) == 29, "post-payment receipt failure deducts stock once");
        check(value(() -> !(Boolean) stateField("busy") && stateField("pending") == null), "paid cart cannot be submitted again after receipt failure");
        closeDialogs(); trackInvoices(fixtures, first);
        // Lose the response after a real SQL commit and make both immediate resolution attempts fail.
        fill(first, 1);
        JdbcFaults.LOSE_NEXT_SALE_COMMIT.set(true);
        payAndConfirm(false);
        waitFor(() -> visibleDialog("Cần kiểm tra kết quả thanh toán"), "uncertain checkout warning");
        closeDialogs();
        check(value(() -> cart().getRowCount() == 1 && (Boolean) stateField("locked") && stateField("pending") != null),
            "unknown commit preserves frozen cart and stable request");
        check(fixtures.stock(first) == 28, "uncertain checkout committed one deduction");
        edt(() -> ((JButton) stateField("btnThanhToan")).doClick());
        waitFor(() -> value(() -> cart().getRowCount() == 0), "same-ID retry resolves checkout");
        drainReceiptDialogs(); trackInvoices(fixtures, first);
        check(fixtures.stock(first) == 28, "resolving unknown checkout does not deduct stock again");
        fill(recent, 1);
        Object saved = value(() -> field(frame, "saleState"));
        NguoiDung old = value(() -> (NguoiDung) field(frame, "actor"));
        fixtures.masters.capNhatTrangThaiTaiKhoan(fixtures.manager, account.getTenDangNhap(), "KHOA");
        rejects(service.SessionExpiredException.class, () -> new DichVuBanHang().tonKho(old, first.getMaSanPham()), "GUI actor detects revocation");
        waitFor(() -> value(() -> field(frame, "actor") == null), "revocation returns to login"); closeDialogs();
        fixtures.masters.capNhatTrangThaiTaiKhoan(fixtures.manager, account.getTenDangNhap(), "HOAT_DONG");
        login(account.getTenDangNhap());
        check(value(() -> field(frame, "saleState") == saved && cart().getRowCount() == 1), "reauthentication preserves unsaved cart for its owner");
        System.out.println("Swing look and feel: " + UIManager.getLookAndFeel().getName());
      } finally {
        closeDialogs(); if (frame != null) edt(() -> frame.dispose());
        // Include invoices even if an assertion interrupted a UI response.
        try (Connection connection = ConnectDB.open(); Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT ma_hoa_don FROM HOA_DON WHERE checkout_actor='" + account.getTenDangNhap() + "'")) {
          while (result.next()) fixtures.track("HOA_DON", result.getString(1));
        }
      }
    }
    System.out.println("PosGuiSmoke checks: " + checks);
    System.exit(0);
  }

  private static void login(String username) throws Exception {
    edt(() -> {
      for (Component component : all(frame.getContentPane())) {
        if (component instanceof JPasswordField) ((JPasswordField) component).setText("MatKhau123");
        else if (component instanceof JTextField) ((JTextField) component).setText(username);
      }
      for (Component component : all(frame.getContentPane()))
        if (component instanceof JButton && "Đăng nhập".equals(((JButton) component).getText())) ((JButton) component).doClick();
    });
    waitFor(() -> value(() -> field(frame, "actor") != null && field(frame, "saleState") != null), "real Swing login");
    Thread.sleep(400);
  }
  private static void staleQuote(TestFixtures f, SanPham oldProduct, SanPham recentProduct) throws Exception {
    try (Connection blocker = ConnectDB.open()) {
      int spid;
      try (Statement statement = blocker.createStatement(); ResultSet result = statement.executeQuery("SELECT @@SPID")) { result.next(); spid = result.getInt(1); }
      blocker.setAutoCommit(false);
      try (PreparedStatement statement = blocker.prepareStatement("UPDATE SAN_PHAM SET gia_ban=gia_ban WHERE ma_san_pham=?")) {
        statement.setString(1, oldProduct.getMaSanPham()); statement.executeUpdate();
      }
      setCart(oldProduct, 1);
      waitFor(() -> blockedBy(spid), "older quote blocked in actual SQL Server");
      fill(recentProduct, 1);
      BigDecimal before = value(() -> (BigDecimal) cart().getValueAt(0, 5));
      blocker.rollback(); Thread.sleep(500);
      check(value(() -> recentProduct.getMaSanPham().equals(cart().getValueAt(0, 0))
          && before.compareTo((BigDecimal) cart().getValueAt(0, 5)) == 0 && before.intValueExact() == 15),
          "blocked old quote cannot overwrite newer 15 VND cart");
      screenshot("stale-quote-protected");
    }
  }
  private static boolean blockedBy(int spid) throws Exception {
    try (Connection connection = DriverManager.getConnection(System.getenv("POS_DB_URL"), "sa", System.getenv("POS_TEST_ADMIN_PASSWORD"));
        PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM sys.dm_exec_requests WHERE blocking_session_id=?")) {
      statement.setInt(1, spid);
      try (ResultSet result = statement.executeQuery()) { result.next(); return result.getInt(1) > 0; }
    }
  }
  private static void fill(SanPham product, int quantity) throws Exception { setCart(product, quantity); awaitQuote(); }
  private static void setCart(SanPham product, int quantity) throws Exception {
    edt(() -> {
      DefaultTableModel cart = cart(); cart.setRowCount(0);
      cart.addRow(new Object[] {product.getMaSanPham(), product.getTenSanPham(), quantity,
          product.getGiaBan(), BigDecimal.ZERO, product.getGiaBan().multiply(BigDecimal.valueOf(quantity)), ""});
      ((JComboBox<?>) named("Phương thức thanh toán")).setSelectedItem("QR");
      ((Runnable) stateField("refresh")).run();
    });
  }
  private static void awaitQuote() throws Exception { waitFor(() -> value(() -> (Boolean) stateField("quoted")), "current quote finishes"); }
  private static void payAndConfirm(boolean duplicate) throws Exception {
    if (duplicate) {
      edt(() -> ((JButton) stateField("btnThanhToan")).requestFocusInWindow());
      Thread.sleep(100);
      Robot robot = new Robot(); robot.keyPress(java.awt.event.KeyEvent.VK_ENTER); robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
    } else SwingUtilities.invokeLater(() -> ((JButton) stateField("btnThanhToan")).doClick());
    waitFor(() -> visibleDialog("Thanh toán"), "payment confirmation by keyboard or click");
    if (duplicate) edt(() -> {
      JButton payment = (JButton) stateField("btnThanhToan");
      for (java.awt.event.ActionListener listener : payment.getActionListeners())
        listener.actionPerformed(new java.awt.event.ActionEvent(payment, 0, "duplicate"));
    });
    edt(() -> {
      for (Window window : Window.getWindows()) if (window instanceof JDialog && window.isShowing()
          && "Thanh toán".equals(((JDialog) window).getTitle())) {
        for (Component component : all((Container) window))
          if (component instanceof JButton && ("Yes".equals(((JButton) component).getText()) || "Có".equals(((JButton) component).getText()))) {
            ((JButton) component).doClick(); return;
          }
      }
      throw new IllegalStateException("Confirmation yes button missing");
    });
    waitFor(() -> visibleDialog("Thanh toán") || visibleDialog("Cần kiểm tra kết quả thanh toán"), "payment result dialog");
    if (visibleDialog("Thanh toán")) closeDialogs();
  }
  private static void drainReceiptDialogs() throws Exception {
    for (int index = 0; index < 8; index++) { Thread.sleep(100); closeDialogs(); }
  }
  private static void trackInvoices(TestFixtures f, SanPham product) throws Exception {
    try (Connection connection = ConnectDB.open(); PreparedStatement statement = connection.prepareStatement("SELECT ma_hoa_don FROM CHI_TIET_HOA_DON WHERE ma_san_pham=?")) {
      statement.setString(1, product.getMaSanPham());
      try (ResultSet result = statement.executeQuery()) { while (result.next()) f.track("HOA_DON", result.getString(1)); }
    }
  }
  private static void visiblePayment(String description) throws Exception {
    check(value(() -> {
      JButton button = (JButton) stateField("btnThanhToan"); JLabel total = (JLabel) stateField("lblTongThanhToan");
      Rectangle content = frame.getContentPane().getBounds();
      for (JComponent component : new JComponent[] {button, total}) {
        Rectangle bounds = SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), frame.getContentPane());
        if (!component.isShowing() || !content.contains(bounds)) return false;
      }
      return true;
    }), "payment and total visible: " + description);
  }
  private static DefaultTableModel cart() {
    for (Component component : all((Container) stateField("panel")))
      if (component instanceof JTable && ((JTable) component).getColumnCount() == 7) return (DefaultTableModel) ((JTable) component).getModel();
    throw new IllegalStateException("Sale cart missing");
  }
  private static Component named(String name) {
    for (Component component : all((Container) stateField("panel")))
      if (component instanceof JComponent && name.equals(((JComponent) component).getAccessibleContext().getAccessibleName())) return component;
    throw new IllegalStateException("Missing component " + name);
  }
  private static Object stateField(String name) { return field(field(frame, "saleState"), name); }
  private static Object field(Object target, String name) {
    try { Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target); }
    catch (ReflectiveOperationException exception) { throw new IllegalStateException(exception); }
  }
  private static List<Component> all(Container container) {
    List<Component> result = new ArrayList<Component>();
    for (Component component : container.getComponents()) { result.add(component); if (component instanceof Container) result.addAll(all((Container) component)); }
    return result;
  }
  private static void screenshot(String name) throws Exception {
    edt(() -> {
      try {
        BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); frame.paint(graphics); graphics.dispose();
        ImageIO.write(image, "png", Path.of(".dev/audit-fixes/gui/" + name + ".png").toFile());
      } catch (Exception exception) { throw new IllegalStateException(exception); }
    });
  }
  private static boolean visibleDialog(String title) throws Exception {
    return value(() -> {
      for (Window window : Window.getWindows()) if (window instanceof JDialog && window.isShowing() && title.equals(((JDialog) window).getTitle())) return true;
      return false;
    });
  }
  private static void closeDialogs() throws Exception { edt(() -> { for (Window window : Window.getWindows()) if (window instanceof JDialog && window.isShowing()) window.dispose(); }); }
  private static void edt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }
  private static <T> T value(Callable<T> action) throws Exception {
    FutureTask<T> task = new FutureTask<T>(action); SwingUtilities.invokeAndWait(task); return task.get();
  }
  private interface Condition { boolean ready() throws Exception; }
  private static void waitFor(Condition condition, String description) throws Exception {
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
    while (System.nanoTime() < deadline) { if (condition.ready()) return; Thread.sleep(50); }
    throw new AssertionError("Timeout: " + description);
  }
}
