package modelserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.spark.ml.feature.MinMaxScalerModel;
import org.apache.spark.ml.linalg.DenseVector;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.ml.linalg.VectorUDT;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class HttpHandlerImplML implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpHandlerImplML.class);

    private LinearRegressionModel model;
    private MinMaxScalerModel scalerModel;
    
    public HttpHandlerImplML(LinearRegressionModel model, MinMaxScalerModel scalerModel) {
        this.model = model;
        this.scalerModel = scalerModel;
    }
    
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        //System.out.println("Handling");
        String method = httpExchange.getRequestMethod();
        switch (method) {
            case "POST":
                doPost(httpExchange);
                break;
            default:
                httpExchange.sendResponseHeaders(405, -1);
                break;
        }
    }

    /*private void doPost(HttpExchange httpExchange) throws IOException {
        System.out.println("Doing post");
        String parameters = new String(new Scanner(httpExchange.getRequestBody()).nextLine()
                .getBytes(StandardCharsets.UTF_8));
        ParameterParser parser = new ParameterParser(parameters);
        Map<String, Object> parametersMap = parser.getParameters();
        System.out.println(parametersMap);
        if (parametersMap.size() == 5) {
            if(parametersMap.containsKey("close5") && parametersMap.containsKey("close4") &&
                parametersMap.containsKey("close3") && parametersMap.containsKey("close2") &&
                    parametersMap.containsKey("close1")) {
                //double[] params = new double[]{(double) parametersMap.get("open"),
                    //(double) parametersMap.get("low"),
                    //(double) parametersMap.get("actualOpen")};
                double[] params = new double[]{
                        (double) parametersMap.get("close5"),
                        (double) parametersMap.get("close4"),
                        (double) parametersMap.get("close3"),
                        (double) parametersMap.get("close2"),
                        (double) parametersMap.get("close1")
                };
                SparkSession spark = SparkSession
                        .builder()
                        .appName("ModelCreator")
                        .master("local")
                        .getOrCreate();
                DenseVector vector = new DenseVector(params);
                StructType schema = new StructType(new StructField[]{
                        new StructField("features", new VectorUDT(), false, Metadata.empty())
                });
                Row row = RowFactory.create((Vector) vector);
                Dataset<Row> inputDF = spark.createDataFrame(Collections.singletonList(row), schema);
                Dataset<Row> scaledDF = scalerModel.transform(inputDF);
                Vector scaledVector = scaledDF.select("scaledFeatures").first().getAs(0);
                double prediction = model.predict(scaledVector);
                double truncatedPrediction = BigDecimal.valueOf(prediction)
                                .setScale(2, RoundingMode.DOWN).doubleValue();
                System.out.println(prediction);
                JSONObject json = new JSONObject();
                json.put("prediction", truncatedPrediction);
                httpExchange.sendResponseHeaders(200, json.toString().length());
                httpExchange.getResponseHeaders().set("Content-Type", "application/json");
                OutputStream out = httpExchange.getResponseBody();
                out.write(json.toString().getBytes());
                out.flush();
                out.close();
                return;
            }
        }

        httpExchange.sendResponseHeaders(400, -1);
    }*/

    private void doPost(HttpExchange httpExchange) throws IOException {
        InputStream requestBodyStream = httpExchange.getRequestBody();
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        byte[] tmpBuff = new byte[8192];
        int length = 0;
        while ((length = requestBodyStream.read(tmpBuff)) > 0) {
            buff.write(tmpBuff, 0, length);
        }
        String jsonString = new String(buff.toByteArray(), StandardCharsets.UTF_8);
        try {
            JSONObject requestBodyJson = new JSONObject(jsonString);
            JSONArray dataJson = requestBodyJson.getJSONArray("data");
            JSONObject klineJson = dataJson.getJSONObject(0);
            //System.out.println(klineJson);
            double open = klineJson.getDouble("open");
            double high = klineJson.getDouble("high");
            double low = klineJson.getDouble("low");
            double close = klineJson.getDouble("close");
            double[] params = new double[]{
                    open,
                    high,
                    low,
                    close
            };
            SparkSession spark = SparkSession
                    .builder()
                    .appName("ModelCreator")
                    .master("local")
                    .getOrCreate();
            DenseVector vector = new DenseVector(params);
            StructType schema = new StructType(new StructField[]{
                    new StructField("features", new VectorUDT(), false, Metadata.empty())
            });
            Row row = RowFactory.create((Vector) vector);
            Dataset<Row> inputDF = spark.createDataFrame(Collections.singletonList(row), schema);
            Dataset<Row> scaledDF = scalerModel.transform(inputDF);
            Vector scaledVector = scaledDF.select("scaledFeatures").first().getAs(0);
            double prediction = model.predict(scaledVector);
            double truncatedPrediction = BigDecimal.valueOf(prediction)
                    .setScale(2, RoundingMode.DOWN).doubleValue();
            //System.out.println(prediction);
            JSONObject json = new JSONObject();
            json.put("prediction", truncatedPrediction);
            httpExchange.sendResponseHeaders(200, json.toString().length());
            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            OutputStream out = httpExchange.getResponseBody();
            out.write(json.toString().getBytes());
            out.flush();
            out.close();
            MDC.put("open", Double.toString(open));
            MDC.put("high", Double.toString(high));
            MDC.put("low", Double.toString(low));
            MDC.put("close", Double.toString(close));
            MDC.put("prediction", Double.toString(truncatedPrediction));
            HttpHandlerImplML.logger.info("Prediction computed");
            MDC.clear();
        } catch (JSONException jsonException) {
            httpExchange.sendResponseHeaders(400, -1);
        }
    }
}
