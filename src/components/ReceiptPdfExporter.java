package components;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class ReceiptPdfExporter {
    private ReceiptPdfExporter() {
    }

    public static void export(ReceiptPrintable receipt, File file) throws IOException {
        if (receipt == null || file == null)
            throw new IllegalArgumentException("Thiếu dữ liệu xuất hóa đơn");
        int width = 900;
        int height = Math.max(900, receipt.renderedHeight(width));
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            receipt.render(graphics, width, height);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", jpeg);
        writePdf(file, jpeg.toByteArray(), width, height);
    }

    private static void writePdf(File file, byte[] image, int pixelWidth, int pixelHeight)
            throws IOException {
        double pageWidth = 595.0;
        double pageHeight = pageWidth * pixelHeight / pixelWidth;
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        java.util.List<Integer> offsets = new java.util.ArrayList<Integer>();
        write(pdf, "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");
        offsets.add(object(pdf, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"));
        offsets.add(object(pdf, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"));
        offsets.add(object(pdf, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                + pageWidth + " " + pageHeight + "] /Resources << /XObject << /Im0 4 0 R >> "
                + ">> /Contents 5 0 R >>\nendobj\n"));
        String imageHeader = "4 0 obj\n<< /Type /XObject /Subtype /Image /Width " + pixelWidth
                + " /Height " + pixelHeight + " /ColorSpace /DeviceRGB /BitsPerComponent 8 "
                + "/Filter /DCTDecode /Length " + image.length + " >>\nstream\n";
        offsets.add(pdf.size());
        write(pdf, imageHeader);
        pdf.write(image);
        write(pdf, "\nendstream\nendobj\n");
        String content = "q\n" + pageWidth + " 0 0 " + pageHeight + " 0 0 cm\n/Im0 Do\nQ\n";
        offsets.add(object(pdf, "5 0 obj\n<< /Length " + content.length() + " >>\nstream\n"
                + content + "endstream\nendobj\n"));
        int xref = pdf.size();
        write(pdf, "xref\n0 6\n0000000000 65535 f \n");
        for (Integer offset : offsets)
            write(pdf, String.format("%010d 00000 n \n", offset));
        write(pdf, "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs())
            throw new IOException("Không thể tạo thư mục lưu hóa đơn");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(pdf.toByteArray());
        }
    }

    private static int object(ByteArrayOutputStream output, String value) throws IOException {
        int offset = output.size();
        write(output, value);
        return offset;
    }

    private static void write(ByteArrayOutputStream output, String value) throws IOException {
        output.write(value.getBytes("ISO-8859-1"));
    }
}
