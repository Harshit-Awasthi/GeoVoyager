import java.awt.*;
import java.io.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.json.JSONArray;
import org.json.JSONObject;

public class GeoVoyager extends JFrame implements ActionListener {

    private JTextField sourceField;
    private JTextField destField;
    private JButton cordsButton;
    private JButton searchButton;
    private JLabel coordsLabel;
    private JLabel travelLabel;
    private JList<String> newsList;
    private String origin;
    private String destination;

    public GeoVoyager() {
        setTitle("Geo Voyager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        sourceField = new JTextField("Source");
        destField = new JTextField("Destination");
        cordsButton = new JButton("Get Coordinates");
        searchButton = new JButton("Get Tavel Info");
        coordsLabel = new JLabel("Coordinates");
        travelLabel = new JLabel("Travel Info");
        newsList = new JList<String>();

        Dimension buttonSize = new Dimension(10, 20);
        cordsButton.setPreferredSize(buttonSize);
        searchButton.setPreferredSize(buttonSize);

        cordsButton.addActionListener(this);
        searchButton.addActionListener(this);

        JPanel inputPanel = new JPanel(new GridLayout(1, 2));
        inputPanel.add(sourceField);
        inputPanel.add(destField);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(cordsButton);
        buttonPanel.add(searchButton);
        buttonPanel.setPreferredSize(new Dimension(300, 50));

        JPanel outputPanel = new JPanel(new GridLayout(3, 1));
        outputPanel.add(coordsLabel);
        outputPanel.add(travelLabel);
        outputPanel.add(newsList);

        getContentPane().add(inputPanel, BorderLayout.NORTH);
        getContentPane().add(buttonPanel, BorderLayout.CENTER);
        getContentPane().add(outputPanel, BorderLayout.SOUTH);

        // Set the size and make the frame visible
        setSize(500, 200);
        setVisible(true);
    }

    // ActionListener implementation
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == cordsButton) {

            // Get coordinates
            String originLocation = sourceField.getText();
            String destLocation = destField.getText();

            origin = fetchCoordinates(originLocation);
            destination = fetchCoordinates(destLocation);
            if (origin != null && destination != null) {
                coordsLabel.setText("Coordinates fetched " + "<" + origin + ">" + "  " + "<" + destination + ">");
            }
        } else if (e.getSource() == searchButton) {
            // Handle distance/time and news
            String searchText = destField.getText();

            fetchNews(searchText);
            String[] value = fetchDistance(origin, destination);
            travelLabel.setText("Distance : " + value[0] + "km     Time : " + value[1]);
        }
    }

    public String fetchCoordinates(String location) {
        try {
            URL url = new URL("https://nominatim.openstreetmap.org/search?q=" + URLEncoder.encode(location, "UTF-8")
                    + "&format=json");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            String json = response.toString();
            int index = json.indexOf("\"lat\":");
            if (index >= 0) {
                String lat = json.substring(index + 6, json.indexOf(",", index));
                index = json.indexOf("\"lon\":");
                if (index >= 0) {
                    String lon = json.substring(index + 6, json.indexOf(",", index));
                    String output = lon.substring(1, lon.length() - 1) + "," + lat.substring(1, lat.length() - 1);

                    return output;
                }
            } else {
                coordsLabel.setText("Error: location not found");
            }
        } catch (IOException ex) {
            coordsLabel.setText("Error: " + ex.getMessage());
        }
        return null;
    }

    public void fetchNews(String searchTerm) {
        if (searchTerm != null && !searchTerm.trim().equals("")) {
            try {
                String url = "https://newsapi.org/v2/everything?q=" + URLEncoder.encode(searchTerm, "UTF-8")
                        + "&apiKey=5c3ce4d17d2e4ca0b10702f2d27c9220";
                HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                con.setRequestMethod("GET");
                con.setDoInput(true);
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray articles = jsonResponse.getJSONArray("articles");
                ArrayList<String> titles = new ArrayList<String>();
                for (int i = 0; i < 5; i++) { // articles.length()
                    JSONObject article = articles.getJSONObject(i);
                    String title = article.getString("title");
                    titles.add(title);
                }
                newsList.setListData(titles.toArray(new String[0]));
                // statusLabel.setText("Search complete");
            } catch (Exception ex) {
                ex.printStackTrace();
                travelLabel.setText("Error: " + ex.getMessage());
            }
        } else {
            travelLabel.setText("Please enter a search term");
        }
    }

    private static String covertTime(double seconds) {
        int hours = (int) seconds / 3600;
        int minutes = (int) (seconds % 3600) / 60;
        String time = hours + " hour " + minutes + " mins";
        return time;
    }

    public String[] fetchDistance(String origin, String destination) {
        final String BASE_URL = "http://router.project-osrm.org/route/v1/driving/";
        try {

            // create URL for OpenStreetMap Distance API
            URL url = new URL(BASE_URL + origin + ";" + destination + "?overview=false");

            // send HTTP request and get response
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            con.disconnect();

            // parse JSON response to get distance in meters
            JSONObject json = new JSONObject(content.toString());
            JSONArray routes = json.getJSONArray("routes");
            JSONObject route = routes.getJSONObject(0);
            double distance = route.getDouble("distance");
            double travelTimeInSeconds = route.getDouble("duration");

            // return distance in kilometers
            String finalDistance = String.format("%.2f", distance / 1000.0);
            String duration = covertTime(travelTimeInSeconds);
            // System.out.println(finalDistance+" " + duration);
            return new String[] { finalDistance, duration };

        } catch (Exception ex) {
            travelLabel.setText("Error calculating distance");
            ex.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        new GeoVoyager();
    }

}
