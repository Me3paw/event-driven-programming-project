package com.me3paw.pos;

import com.me3paw.pos.ui.PosFrame;

import javax.swing.SwingUtilities;

/** Application entry point for the POS visual prototype. */
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
            System.exit(DbSmoke.run());
        }
        System.err.println("Usage: PosApplication [--db-smoke]");
        System.exit(2);
    }
}
