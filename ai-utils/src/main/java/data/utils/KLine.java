package data.utils;

import java.util.Scanner;

public class KLine {
    private final long timestamp;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final double volume;
    private final long closeTimestamp;
    private final double quoteAssetVolume;
    private final int numberOfTrades;
    private final double takerBuyBaseAssetVolume;
    private final double takerBuyQuoteAssetVolume;

    public KLine(long timestamp, double open, double high, double low, double close, double volume,
                 long closeTimestamp, double quoteAssetVolume, int numberOfTrades,
                 double takerBuyBaseAssetVolume, double takerBuyQuoteAssetVolume) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.closeTimestamp = closeTimestamp;
        this.quoteAssetVolume = quoteAssetVolume;
        this.numberOfTrades = numberOfTrades;
        this.takerBuyBaseAssetVolume = takerBuyBaseAssetVolume;
        this.takerBuyQuoteAssetVolume = takerBuyQuoteAssetVolume;
    }

    public static KLine parse(String line) {
        Scanner scanner = new Scanner(line);
        scanner.useDelimiter(",");
        return new KLine(scanner.nextLong(), scanner.nextDouble(), scanner.nextDouble(),
                scanner.nextDouble(), scanner.nextDouble(), scanner.nextDouble(),
                scanner.nextLong(), scanner.nextDouble(), scanner.nextInt(),
                scanner.nextDouble(), scanner.nextDouble());
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public double getVolume() {
        return volume;
    }

    public long getCloseTimestamp() {
        return closeTimestamp;
    }

    public double getQuoteAssetVolume() {
        return quoteAssetVolume;
    }

    public int getNumberOfTrades() {
        return numberOfTrades;
    }

    public double getTakerBuyBaseAssetVolume() {
        return takerBuyBaseAssetVolume;
    }

    public double getTakerBuyQuoteAssetVolume() {
        return takerBuyQuoteAssetVolume;
    }

    @Override
    public String toString() {
        return "data.KLine{" +
                "timestamp=" + timestamp +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", close=" + close +
                ", volume=" + volume +
                ", closeTimestamp=" + closeTimestamp +
                ", quoteAssetVolume=" + quoteAssetVolume +
                ", numberOfTrades=" + numberOfTrades +
                ", takerBuyBaseAssetVolume=" + takerBuyBaseAssetVolume +
                ", takerBuyQuoteAssetVolume=" + takerBuyQuoteAssetVolume +
                '}';
    }
}
