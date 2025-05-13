package data;

import data.utils.KLine;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

public class DataPreprocessor {
    public static void main(String[] args) throws Exception {
        String rawDataPathString = args[0];
        Scanner scanner = new Scanner(new File(rawDataPathString));
        if (!scanner.hasNextLine()) {
            System.err.println("Dataset is empty");
            System.exit(1);
        }
        PrintWriter writer = new PrintWriter(args[1]);
        // print header
        writer.println(getHeader());
        KLine curr = KLine.parse(scanner.nextLine());
        while (scanner.hasNextLine()) {
            KLine next = KLine.parse(scanner.nextLine());
            double label = next.getClose();
            String row =
                    curr.getOpen() + "," +
                    curr.getHigh() + "," +
                    curr.getLow() + "," +
                    curr.getClose() + "," +
                    label;
            writer.println(row);
            curr = next;
        }
        scanner.close();
        writer.close();
        System.exit(0);
    }

    private static String getHeader() {
        return "open,high,low,close,label";
    }
}