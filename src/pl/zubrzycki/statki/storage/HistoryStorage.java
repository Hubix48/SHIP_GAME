package pl.zubrzycki.statki.storage;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HistoryStorage {

    private final List<String> entries = new ArrayList<>();

    public void add(String text) {
        entries.add(text);
    }

    public void clear() {
        entries.clear();
    }

    public List<String> getAll() {
        return new ArrayList<>(entries);
    }

    public void exportToFile(String winnerName) throws Exception {
        String fileName = "Statki - zdarzenia gry " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("[dd-MM-yyyy__HH-mm-ss]")) +
                ".txt";

        File file = new File(fileName);
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            String formattedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

            writer.println("Historia gry w statki");
            writer.println("Data rozegrania: " + formattedDate);
            writer.println("Zwycięzca rozgrywki: " + winnerName);
            writer.println();
            writer.println("Przebieg gry:");

            for (String e : entries) writer.println("• " + e);
        }
    }
}