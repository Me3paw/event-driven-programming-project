package graphicUI;

import connectDB.DBConnection;

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
            System.exit(DBConnection.smoke());
        }
        System.err.println("Usage: PosApplication [--db-smoke]");
        System.exit(2);
    }
}
