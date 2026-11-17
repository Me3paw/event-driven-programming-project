package components;

import java.awt.Component;
import java.awt.print.PrinterJob;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public final class ReceiptPrinting {
    private ReceiptPrinting() {}
    public static void print(Component parent, ReceiptPrintable receipt) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Hóa đơn " + receipt.invoiceId());
        job.setPrintable(receipt);
        if (!job.printDialog()) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { job.print(); return null; }
            @Override protected void done() {
                try { get(); }
                catch (Exception exception) {
                    JOptionPane.showMessageDialog(parent,
                            "Không in được hóa đơn. Giao dịch thanh toán không thay đổi. Hãy thử in lại từ danh sách hóa đơn.",
                            "Lỗi in hóa đơn", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
