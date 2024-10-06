import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

class MyCanvas extends JPanel {
    public ArrayList<ArrayList<ArrayList<Double>>> line = new ArrayList<>();
    public ArrayList<ArrayList<Double>> plotPoints = new ArrayList<>();

    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        if (!line.isEmpty()) {
            g.setColor(Color.BLACK);
            for (int j = 0; j < line.size(); j++) {
                for (int i = 0; i < line.get(j).size() - 1; i++) {
                    Shape l = new Line2D.Double(line.get(j).get(i).get(0),
                            line.get(j).get(i).get(1),
                            line.get(j).get(i+1).get(0),
                            line.get(j).get(i+1).get(1)
                    );
                    g2.draw(l);

                }
            }
        }
        if (!plotPoints.isEmpty()) {
            double pLat, pLon;
            g.setColor(Color.RED);
            for (int i = 0; i < plotPoints.size(); i++) {
                pLat = plotPoints.get(i).get(0);
                pLon = plotPoints.get(i).get(1);
                Shape l1 = new Line2D.Double(pLat - 5, pLon, pLat + 5, pLon);
                Shape l2 = new Line2D.Double(pLat, pLon - 5, pLat, pLon + 5);
                g2.draw(l1); g2.draw(l2);
            }
        }
    }
}

class Json {
    public final JsonObject json;
    public final int numOfFeatures;

    public Json(String jsonData) {
        this.json = new Gson().fromJson(jsonData, JsonObject.class);
        this.numOfFeatures = this.json.getAsJsonArray("features").size();
    }
    public String getOneFeature() {
        return this.json.getAsJsonArray("features").get(0).getAsJsonObject().get("type").getAsString();
    }
    public int findCountryIndex(String searchName) {
        String name;
        for (int j = 0; j < this.numOfFeatures - 1; j++) {
            name = this.json.getAsJsonArray("features")
                    .get(j)
                    .getAsJsonObject()
                    .get("properties")
                    .getAsJsonObject()
                    .get("sovereignt")
                    .getAsString();
            if (Objects.equals(name, searchName)) {
                return j;
            }
        }
        return -1;
    }
    public JsonArray getCoordinates(int feature) {
        return this.json
                .getAsJsonArray("features")
                .get(feature)
                .getAsJsonObject()
                .get("geometry")
                .getAsJsonObject()
                .getAsJsonArray("coordinates");
    }
}

class mapConvert {
    public static ArrayList<Double> coordinatesToInt(double lat, double lon, int mapW, int mapH) {
        ArrayList<Double> pair = new ArrayList<>();
        double x, y, latRad;
        int intX, intY;

        x = (lon+180) * ((double)mapW /360);
        latRad = lat * Math.PI/180;
        y = Math.log(Math.tan((Math.PI/4) + (latRad/2)));
        y = ((double) mapH /2) - (mapW * y / (2 * Math.PI));
        pair.add(x); pair.add(y);
        return pair;
    }
}

public class Main extends Canvas {
    public static void main(String[] args) throws IOException {
        int mapW = 900;
        int mapH = 720;
        JFrame f = new JFrame("World Map");
        f.setSize(900, 720);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container c = f.getContentPane();

        JTextField latTextField = new JTextField("20.82");
        latTextField.setMaximumSize(new Dimension(150, latTextField.getPreferredSize().height));
        JTextField lonTextField = new JTextField("-157.97");
        lonTextField.setMaximumSize(new Dimension(150, lonTextField.getPreferredSize().height));

        JButton plotButton = new JButton("Plot Point");

        MyCanvas canvas = new MyCanvas();
        canvas.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        File jsonFile = new File("src/low_geo.json");
        String jsonData = Files.readString(jsonFile.toPath());
        Json geoData = new Json(jsonData);

        for (int n = 0; n < geoData.numOfFeatures - 1; n++) {

            JsonArray country = geoData.getCoordinates(n);
            ArrayList<ArrayList<Double>> coordinates = new ArrayList<>();
            ArrayList<Double> pair;
            double lat, lon;
            for (int i = 0; i < country.size(); i++) {
                // System.out.println(country.get(i));
                JsonArray majorFeatures = country.get(i).getAsJsonArray();
                for (int j = 0; j < majorFeatures.size(); j++) {
                    // System.out.println(majorFeatures.get(j));
                    JsonArray minorFeatures = majorFeatures.get(j).getAsJsonArray();
                    if (minorFeatures.size() == 2) {
                        pair = new ArrayList<>();
                        lat = minorFeatures.get(1).getAsDouble();
                        lon = minorFeatures.get(0).getAsDouble();
                        pair = mapConvert.coordinatesToInt(lat, lon, mapW, mapH);
                        coordinates.add(pair);
                    } else {
                        for (int k = 0; k < minorFeatures.size(); k++) {
                            // System.out.println(minorFeatures.get(k));
                            JsonArray subFeatures = minorFeatures.get(k).getAsJsonArray();
                            pair = new ArrayList<>();
                            lat = subFeatures.get(1).getAsDouble();
                            lon = subFeatures.get(0).getAsDouble();
                            pair = mapConvert.coordinatesToInt(lat, lon, mapW, mapH);
                            coordinates.add(pair);
                        }
                        canvas.line.add(coordinates);
                        coordinates = new ArrayList<>();
                    }
                }
                canvas.line.add(coordinates);
                coordinates = new ArrayList<>();
            }
        }
        plotButton.addActionListener(e -> {
            double tfLat, tfLon;
            ArrayList<Double> pair = new ArrayList<>();
            tfLat = Double.parseDouble(latTextField.getText());
            tfLon = Double.parseDouble(lonTextField.getText());
            pair = mapConvert.coordinatesToInt(tfLat, tfLon, mapW, mapH);
            canvas.plotPoints.add(pair);
            f.revalidate();
            f.repaint();
        });

        c.setLayout(new BoxLayout(c, BoxLayout.PAGE_AXIS));
        f.add(latTextField);
        f.add(lonTextField);
        f.add(plotButton);
        f.add(canvas);
        f.revalidate();
        f.repaint();
        f.setVisible(true);
    }
}
