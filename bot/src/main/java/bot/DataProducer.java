package bot;

import binance.BinanceUtil;
import binance.types.KLine;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataProducer extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(DataProducer.class);
    private static Logs prevLogs = null;
    private static final Map<String, DataProducer> instancesMapMain = new ConcurrentHashMap<>();
    private static final Map<String, DataProducer> instancesMapTest = new ConcurrentHashMap<>();
    private final DataObtainer dataObtainer;
    private final String symbol;
    private final String baseUrl;
    private double modelProcessedData;
    
    private DataProducer(String symbol, String baseUrl) {
        this.symbol = symbol;
        this.baseUrl = baseUrl;
        dataObtainer = new DataObtainer(symbol, baseUrl);
    }
    
    public static DataProducer getInstance(String symbol, String baseUrl) throws RuntimeException {
        //Verificare cosa succede con symbol invalido
        if (baseUrl.equals(BinanceUtil.mainnetBaseUrl)) {
            if (!instancesMapMain.containsKey(symbol)) {
                synchronized (DataProducer.class) {
                    if (!instancesMapMain.containsKey(symbol)) {
                        DataProducer dp = new DataProducer(symbol, baseUrl);
                        instancesMapMain.put(symbol, dp);
                        dp.start();
                        System.out.println("Nuova istanza creata");
                    } else {
                        System.out.println("Ritorno istanza gia esistente dal sync");
                    }
                }
            } else {
                System.out.println("Ritorno istanza gia esistente");
            }
            return instancesMapMain.get(symbol);
        }
        if (baseUrl.equals(BinanceUtil.testnetBaseUrl)) {
            if (!instancesMapTest.containsKey(symbol)) {
                synchronized (DataProducer.class) {
                    if (!instancesMapTest.containsKey(symbol)) {
                        DataProducer dp = new DataProducer(symbol, baseUrl);
                        instancesMapTest.put(symbol, dp);
                        dp.start();
                    }
                }
            }
            return instancesMapTest.get(symbol);
        }
        
        throw new RuntimeException("Invalid baseUrl");
    }
    
    public double getModelProcessedData() {
        return modelProcessedData;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    @Override
    public void run() {
        dataObtainer.start();
        while (true) {
            synchronized (dataObtainer) {
                try {
                    dataObtainer.wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
            List<KLine> obtainedData = dataObtainer.getData();
            if (prevLogs != null) {
                MDC.put("sent timestamp", Long.toUnsignedString(prevLogs.sentTime));
                MDC.put("received timestamp", Long.toUnsignedString(prevLogs.receivedTime));
                MDC.put("prediction", Double.toString(prevLogs.prediction));
                MDC.put("actual", Double.toString(dataObtainer.getData().getFirst().getClose()));
                logger.info("Previous prediction");
                MDC.clear();
            }
            JSONObject jsonMain = new JSONObject();
            JSONArray jsonData = new JSONArray();
            jsonData.put(obtainedData.get(1).toJSON());
            jsonMain.put("data", jsonData);
            URL url = null;
            HttpURLConnection conn = null;
            try{
                url = new URL("http://localhost:6666/ml");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.getOutputStream().write(jsonMain.toString().getBytes(StandardCharsets.UTF_8));
                long sentTimestamp = System.currentTimeMillis();
                JSONObject json = new JSONObject(new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                long receivedTimestamp = System.currentTimeMillis();
                modelProcessedData = json.getDouble("prediction");
                prevLogs = new Logs(sentTimestamp, receivedTimestamp, modelProcessedData);
            } catch (Exception e) {
                System.out.println("[DATA PRODUCER] Couldn't connect to model server");
                continue;
            }
            System.out.println("[DATA PRODUCER] Data produced");
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    private static class Logs {
        public long sentTime;
        public long receivedTime;
        public double prediction;

        public Logs(long sentTime, long receivedTime, double prediction) {
            this.sentTime = sentTime;
            this.receivedTime = receivedTime;
            this.prediction = prediction;
        }
    }
}
