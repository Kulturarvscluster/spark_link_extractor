package dk.kb.kac.p003;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.Collectors;

import static org.apache.spark.sql.types.DataTypes.StringType;
import static org.apache.spark.sql.types.DataTypes.createStructField;
import static org.apache.spark.sql.types.DataTypes.createStructType;

public class SparkLinkExtractor {
    
    
    public static void main(String[] args) throws Exception {
        
        
        String inputFile = args[0];
        String outputFile = args[1];
        
        try (SparkSession spark = SparkSession
                                          .builder()
                                          .config(new SparkConf(true))
                                          .appName("Html Link reader")
                                          .getOrCreate();) {
            
            SparkContext sc = spark.sparkContext();
            SQLContext sqlContext = spark.sqlContext();
            
            
            //Read the input parquet file
            Dataset<Row> datafile = spark.read().parquet(inputFile);
            
            
            JavaRDD<Row> linkRDD = datafile.toJavaRDD().map(row -> {
                //Read column Target-URI
                String url = row.getAs("Target-URI");
                
                //Extract hostname from url
                String host = new URL(url).getHost();
                
                //Read column contents
                String contents = row.getAs("contents");
                
                //Parse html contents
                Document doc = Jsoup.parse(contents, url);
                
                //Extracts all links as string
                String links = extractLinks(host, doc);
                
                //Create a new row
                return RowFactory.create(url, links);
            });
            
            //Specify the names and types of the columns in the result
            StructType structure = createStructType(
                    new StructField[]{
                            createStructField("uri", StringType, false),
                            createStructField("links", StringType, true)
                    });
            
            //Write the result
            spark.createDataFrame(linkRDD, structure)
                 .write()
                 .mode(SaveMode.Overwrite)
                 .parquet(outputFile);
        }
    }
    
    //DETTE ER METODEN DER STYRER IMAGE LINK UDTRÆK FRA HTML
    private static String extractLinks(String host, Document doc) {
        return doc.select("img[src]")
                  .stream()
                  .map(src -> cleanSpaces(src.attr("abs:src")))
                  .filter(src -> {
                      URL target_url;
                      try {
                          target_url = new URL(src);
                      } catch (MalformedURLException e) {
                          return false;
                      }
                      return !target_url.getHost().endsWith(host)
                             && !host.endsWith(target_url.getHost());
                  })
                  .collect(Collectors.joining("\n"));
    }
    
    
    private static String cleanSpaces(String s) {
        return s.replaceAll("\\s", " ");
    }
}
