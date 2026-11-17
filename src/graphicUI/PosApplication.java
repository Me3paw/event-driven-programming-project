package graphicUI;

import connectDB.ConnectDB;

import javax.swing.SwingUtilities;

public final class PosApplication {
    private PosApplication() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new PosFrame().setVisible(true);
                }
            });
            return;
        }
        if (args.length == 1 && "--db-smoke".equals(args[0])) {
            System.exit(ConnectDB.smoke());
        }
        System.err.println("Tham số không hợp lệ. Sử dụng: PosApplication [--db-smoke]");
        System.exit(2);
    }
}
