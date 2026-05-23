package com.skybook.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.skybook.model.Ticket;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final Font TITLE_FONT   = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD,   new BaseColor(30, 64, 175));
    private static final Font HEADER_FONT  = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   BaseColor.WHITE);
    private static final Font LABEL_FONT   = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   new BaseColor(75, 85, 99));
    private static final Font VALUE_FONT   = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(17, 24, 39));
    private static final Font SMALL_FONT   = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, new BaseColor(107, 114, 128));
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public byte[] generateTicketPdf(Ticket ticket) {
        try {
            Document doc = new Document(PageSize.A5, 36, 36, 36, 36);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();

            // ── Header bar ──────────────────────────────
            PdfContentByte cb = writer.getDirectContent();
            cb.setColorFill(new BaseColor(30, 64, 175));
            cb.rectangle(36, doc.top() - 55, doc.right() - 36, 55);
            cb.fill();

            // Logo / title
            Paragraph title = new Paragraph("✈  SkyBook  —  BOARDING PASS", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(10);
            doc.add(title);
            doc.add(Chunk.NEWLINE);

            // ── Route row ───────────────────────────────
            PdfPTable routeTable = new PdfPTable(3);
            routeTable.setWidthPercentage(100);
            routeTable.setWidths(new float[]{2f, 1f, 2f});
            routeTable.setSpacingBefore(12);

            PdfPCell fromCell = routeCell(ticket.getFlight().getSource(), "FROM");
            PdfPCell arrowCell = arrowCell("→");
            PdfPCell toCell   = routeCell(ticket.getFlight().getDestination(), "TO");

            routeTable.addCell(fromCell);
            routeTable.addCell(arrowCell);
            routeTable.addCell(toCell);
            doc.add(routeTable);

            // ── Details table ────────────────────────────
            PdfPTable details = new PdfPTable(2);
            details.setWidthPercentage(100);
            details.setSpacingBefore(14);
            details.setSpacingAfter(14);

            addDetailRow(details, "Ticket No.",     ticket.getId());
            addDetailRow(details, "Passenger",      ticket.getPassengerName());
            addDetailRow(details, "Airline",        ticket.getFlight().getAirline());
            addDetailRow(details, "Flight",         ticket.getFlight().getId());
            addDetailRow(details, "Departure",      ticket.getFlight().getDepartureTime().format(FMT));
            addDetailRow(details, "Arrival",        ticket.getFlight().getArrivalTime().format(FMT));
            addDetailRow(details, "Seat",           ticket.getSeatNumber());
            addDetailRow(details, "Status",         ticket.getStatus().name());
            addDetailRow(details, "Price",          "USD " + ticket.getFlight().getPrice());
            doc.add(details);

            // ── Footer ───────────────────────────────────
            Paragraph footer = new Paragraph("Booked on " + ticket.getBookedAt().format(FMT) +
                    "  |  Have a safe flight!", SMALL_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            // ── Barcode ──────────────────────────────────
            Barcode128 barcode = new Barcode128();
            barcode.setCode(ticket.getId() + "-" + ticket.getFlight().getId());
            barcode.setCodeType(Barcode128.CODE128);
            Image bcImg = barcode.createImageWithBarcode(cb, null, null);
            bcImg.setAlignment(Element.ALIGN_CENTER);
            bcImg.scalePercent(80);
            bcImg.setSpacingBefore(10);
            doc.add(bcImg);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    // ── Helpers ─────────────────────────────────────
    private PdfPCell routeCell(String city, String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p = new Paragraph();
        p.add(new Chunk(city + "\n", new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, new BaseColor(30, 64, 175))));
        p.add(new Chunk(label, SMALL_FONT));
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        return cell;
    }

    private PdfPCell arrowCell(String arrow) {
        PdfPCell cell = new PdfPCell(new Phrase(arrow,
                new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, new BaseColor(30, 64, 175))));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private void addDetailRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL_FONT));
        labelCell.setBackgroundColor(new BaseColor(243, 244, 246));
        labelCell.setBorderColor(new BaseColor(229, 231, 235));
        labelCell.setPadding(7);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, VALUE_FONT));
        valueCell.setBorderColor(new BaseColor(229, 231, 235));
        valueCell.setPadding(7);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}
