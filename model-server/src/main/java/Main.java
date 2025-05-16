import com.sun.net.httpserver.HttpServer;
import modelserver.HttpHandlerImplML;
import org.apache.spark.ml.feature.MinMaxScalerModel;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;

public class Main {
    private static final int port = 6666;
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Missing arguments");
            System.exit(1);
        }

        String modelPath = args[0];
        if (!new File(modelPath).exists()) {
            System.err.println(modelPath + " not found");
            System.exit(1);
        }
        String scalerModelPath = args[1];
        if (!new File(scalerModelPath).exists()) {
            System.err.println(scalerModelPath + " not found");
            System.exit(1);
        }

        SparkSession session = SparkSession
                .builder()
                .appName("ModelServer")
                .master("local")
                .getOrCreate();

        MinMaxScalerModel scalerModel = MinMaxScalerModel.load(scalerModelPath);
        LinearRegressionModel model = LinearRegressionModel.load(modelPath);

        try {
            InetSocketAddress addr = new InetSocketAddress(port);
            HttpServer server = HttpServer.create(addr, 128);
            server.createContext("/ml", new HttpHandlerImplML(model, scalerModel));
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
            server.setExecutor(threadPoolExecutor);
            server.start();
            System.out.println("Model server listening on port " + port);
        } catch (IOException ioException) {
            System.err.println("Couldn't start server");
        }
    }
}