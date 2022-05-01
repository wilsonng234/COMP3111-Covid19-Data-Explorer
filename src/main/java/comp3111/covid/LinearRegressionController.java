package comp3111.covid;

import covidData.LinearRegression;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;

import static comp3111.covid.DataAnalysis.getFileParser;

public class LinearRegressionController implements Initializable {
    private String dataset = "COVID_Dataset_v1.0.csv";

    @FXML
    private TextField countryTextField;

    @FXML
    private ContextMenu countryContextMenu;

    @FXML
    private ImageView HomeImage;

    @FXML
    private LineChart<Number, Number> lineChart;

    @FXML
    private NumberAxis chartXaxis;

    @FXML
    private NumberAxis chartYaxis;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private ListView<String> xParameterList;

    @FXML
    private ListView<String> yParameterList;

    @FXML
    private Button generateButton;

    private HashSet<String> countries = getCountries(dataset);
    private List<String> sortedCountries;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sortedCountries = new ArrayList<>();
        sortedCountries.addAll(countries);
        Collections.sort(sortedCountries);
        // set countryMenu
        countryTextField.setOnKeyTyped(
                new EventHandler<KeyEvent>() {
                    @Override
                    public void handle(KeyEvent event) {
                        countryContextMenu.getItems().clear();
                        int numberOfItems = 0;
                        for (String country : sortedCountries) {
                            String input = countryTextField.getText().toLowerCase().trim();

                            if (!country.toLowerCase().trim().equals(input) && country.toLowerCase().contains(input)) {
                                MenuItem item = new MenuItem(country);
                                countryContextMenu.getItems().add(item);
                                numberOfItems++;

                                item.setOnAction(
                                        new EventHandler<ActionEvent>() {
                                            @Override
                                            public void handle(ActionEvent event) {
                                                countryTextField.setText(item.getText());
                                            }
                                        }
                                );
                            }
                            if (numberOfItems == 10)
                                break;
                        }
                        countryContextMenu.show(countryTextField,Side.BOTTOM,0,0);
                    }
                }
        );

        // set ParameterList
        List<String> parameterList = new ArrayList<>();
        parameterList.add("Total Confirmed Cases");
        parameterList.add("Confirmed Cases Per Million");
        parameterList.add("Total Deaths");
        parameterList.add("Total Deaths Per Million");
        parameterList.add("Vaccination Rate");
        parameterList.add("Fully Vaccination Rate");

        xParameterList.getItems().addAll(parameterList);
        yParameterList.getItems().addAll(parameterList);

        generateButton.setOnMouseClicked(
                new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        lineChart.getData().clear();
                        String country1 = countryTextField.getText();

                        if (country1 == null) {
                                Alert countryNotChosenAlert = new Alert(Alert.AlertType.WARNING);
                                countryNotChosenAlert.setTitle("COUNTRY NOT CHOSEN");
                                countryNotChosenAlert.setContentText("Please choose at least a country first");

                                countryNotChosenAlert.showAndWait().ifPresent(
                                        new Consumer<ButtonType>() {
                                            @Override
                                            public void accept(ButtonType buttonType) {
                                            }
                                        }
                                        );
                                return;
                        }

                        if (!countries.contains(country1)){
                            Alert countryNotFound = new Alert(Alert.AlertType.WARNING);
                            countryNotFound.setTitle("COUNTRIES NOT FOUND");
                            countryNotFound.setContentText("Countries are not found");

                            countryNotFound.showAndWait().ifPresent(
                                    new Consumer<ButtonType>() {
                                        @Override
                                        public void accept(ButtonType buttonType) {
                                        }
                                    }
                            );

                            return;
                        }

                        String xParameter = xParameterList.getSelectionModel().getSelectedItem();
                        String yParameter = yParameterList.getSelectionModel().getSelectedItem();

                        lineChart.getXAxis().setLabel(xParameter);
                        lineChart.getYAxis().setLabel(yParameter);

                        if (xParameter.equals("Total Confirmed Cases")) {
                            xParameter = "total_cases";
                        } else if (xParameter.equals("Confirmed Cases Per Million")) {
                            xParameter = "total_cases_per_million";
                        } else if (xParameter.equals("Total Deaths")){
                            xParameter = "total_deaths";
                        }
                        else if (xParameter.equals("Total Deaths Per Million")){
                            xParameter = "total_deaths_per_million";
                        }
                        else if (xParameter.equals("Vaccination Rate")) {
                            xParameter = "total_vaccinations_per_hundred";
                        }
                        else if (xParameter.equals("Fully Vaccination Rate")){
                            xParameter = "people_fully_vaccinated_per_hundred";
                        }

                        if (yParameter.equals("Total Confirmed Cases")) {
                            yParameter = "total_cases";
                        } else if (yParameter.equals("Confirmed Cases Per Million")) {
                            yParameter = "total_cases_per_million";
                        } else if (yParameter.equals("Total Deaths")){
                            yParameter = "total_deaths";
                        }
                        else if (yParameter.equals("Total Deaths Per Million")){
                            yParameter = "total_deaths_per_million";
                        }
                        else if (yParameter.equals("Vaccination Rate")) {
                            yParameter = "total_vaccinations_per_hundred";
                        }
                        else if (yParameter.equals("Fully Vaccination Rate")){
                            yParameter = "people_fully_vaccinated_per_hundred";
                        }

                        if (validateDate()) {
                            LocalDate startDate = startDatePicker.getValue();
                            LocalDate endDate = endDatePicker.getValue();

                            try {
                                addCountryLine(countryTextField.getText(), xParameter, yParameter, startDate, endDate);
                            }
                            catch (IllegalArgumentException dateException){
                                Alert alert = new Alert(Alert.AlertType.WARNING);

                                if (dateException.getLocalizedMessage().equals("Please select another start date, data is not found at the start date")) {
                                    alert.setTitle("Start Date Alert");
                                    alert.setContentText("Please select another start date, data is not found at the start date");
                                }
                                else{
                                    alert.setTitle("End Date Alert");
                                    alert.setContentText("Please select another end date, data is not found at the end date");
                                }

                                alert.showAndWait().ifPresent(
                                        new Consumer<ButtonType>() {
                                            @Override
                                            public void accept(ButtonType buttonType) {
                                            }
                                        }
                                );

                                return;
                            }
                        }
                    }
                }
        );

        // set LineChart

        lineChart.setTitle("Linear Regression");
        lineChart.setCreateSymbols(false);

        chartXaxis.setAutoRanging(false);
        chartYaxis.setAutoRanging(false);
    }

    private Boolean validateDate(){
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        Alert invalidDateAlert = new Alert(Alert.AlertType.WARNING);

        if (startDate == null && endDate == null){
            invalidDateAlert.setTitle("BOTH DATE NOT CHOSEN");
            invalidDateAlert.setContentText("Please choose the start date and end date first");

            invalidDateAlert.showAndWait().ifPresent(
                    new Consumer<ButtonType>() {
                        @Override
                        public void accept(ButtonType buttonType) {
                        }
                    }
            );
            return false;
        }
        if (startDate == null){
            invalidDateAlert.setTitle("START DATE NOT CHOSEN");
            invalidDateAlert.setContentText("Please choose the start date first");

            invalidDateAlert.showAndWait().ifPresent(
                    new Consumer<ButtonType>() {
                        @Override
                        public void accept(ButtonType buttonType) {
                        }
                    }
            );
            return false;
        }
        if (endDate == null){
            invalidDateAlert.setTitle("END DATE NOT CHOSEN");
            invalidDateAlert.setContentText("Please choose the end date first");

            invalidDateAlert.showAndWait().ifPresent(
                    new Consumer<ButtonType>() {
                        @Override
                        public void accept(ButtonType buttonType) {
                        }
                    }
            );
            return false;
        }
        if (startDate.isAfter(endDate) || startDate.isEqual(endDate)){
            invalidDateAlert.setTitle("INVALID DATE INPUT");
            invalidDateAlert.setContentText("start date cannot be after or equals to end date!!");

            invalidDateAlert.showAndWait().ifPresent(
                    new Consumer<ButtonType>() {
                        @Override
                        public void accept(ButtonType buttonType) {
                        }
                    }
            );
            return false;
        }

        return true;
    }

    private void addCountryLine(String country,String xParameter,String yParameter,LocalDate startDate,LocalDate endDate){
        List<Double> xList = getData(country, xParameter, startDate, endDate);
        List<Double> yList = getData(country, yParameter, startDate, endDate);

        if (xList.size() < ChronoUnit.DAYS.between(startDate,endDate) + 1){
            throw new IllegalArgumentException("Please select another end date, data is not found at the end date");
        }

        try{
            LinearRegression linearRegression = new LinearRegression(xList, yList);
            double slope = linearRegression.slope();
            double intercept = linearRegression.intercept();

            XYChart.Series<Number, Number> regressionSeries = new XYChart.Series<>();
            regressionSeries.setName("regression line of " + country);

            for (Double xDatum : xList) {
                XYChart.Data<Number, Number> data = new XYChart.Data<Number, Number>(xDatum, linearRegression.predict(xDatum));

                regressionSeries.getData().add(data);
            }

            XYChart.Series<Number, Number> countrySeries = new XYChart.Series<>();
            countrySeries.setName(country);
            for (int i = 0; i < xList.size(); i++) {
                XYChart.Data<Number, Number> data = new XYChart.Data<Number, Number>(xList.get(i), yList.get(i));
                countrySeries.getData().add(data);
            }


            chartXaxis.setLowerBound(xList.get(0) - 1);
            chartXaxis.setUpperBound(xList.get(xList.size() - 1) + 1);

            chartXaxis.setTickUnit((chartXaxis.getUpperBound()-chartXaxis.getLowerBound())/5);

            Double yLowerBound = Math.min(yList.get(0),linearRegression.predict(yList.get(0))) - 1;
            Double yUpperBound = Math.max(yList.get(yList.size()-1),linearRegression.predict(xList.get(xList.size()-1))) + 1;

            chartYaxis.setLowerBound(yLowerBound);
            chartYaxis.setUpperBound(yUpperBound);

            chartYaxis.setTickUnit((chartYaxis.getUpperBound()-chartYaxis.getLowerBound())/5);

            lineChart.getData().add(regressionSeries);
            lineChart.getData().add(countrySeries);
            System.out.println(xList.size());
        }
        catch (IllegalArgumentException lengthNotEqualException){
            System.out.println(lengthNotEqualException.toString());
            System.out.println(country + "\t" + xParameter + " against " + yParameter);
        }
    }

    @FXML
    void switchToHomeScene(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/ui/home.fxml"));
        Stage stage = (Stage)((Node)(event.getSource())).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private HashSet<String> getCountries(String dataset){
        HashSet<String> countries = new HashSet<String>();

        for (CSVRecord csvRecord : getFileParser(dataset)) {
            String country = csvRecord.get("location");

            if (!countries.contains(country)) {
                countries.add(country);
            }
        }

        return countries;
    }

    private List<Double> getData(String country,String parameter,LocalDate startDate,LocalDate endDate){
        List<Double> data = new ArrayList<Double>();

        for (CSVRecord csvRecord : getFileParser(dataset)) {
            String countryName = csvRecord.get("location");
            if (!country.equals(countryName))
                continue;

            String countryISO = csvRecord.get("iso_code");
            String dateRecord = csvRecord.get("date");
            if (dateRecord.equals("")) {
                System.out.println("empty dateRecord??");
                throw new IllegalArgumentException();
                //continue;
            }

            String[] dateRecordInfo = dateRecord.trim().split("/");
            LocalDate recordDate = LocalDate.of(Integer.parseInt(dateRecordInfo[2]),
                    Integer.parseInt(dateRecordInfo[0]),
                    Integer.parseInt(dateRecordInfo[1]));

            String datum = csvRecord.get(parameter);
            if (recordDate.isEqual(startDate) || (recordDate.isAfter(startDate) && recordDate.isBefore(endDate)) || recordDate.isEqual(endDate)) {
                try{
                    data.add(Double.parseDouble(datum));
                    //System.out.println(data);
                }
                catch (NumberFormatException exception){
                    if (data.size() == 0){
                        throw new IllegalArgumentException("Please select another start date, data is not found at the start date");
                    }
                    data.add(data.get(data.size()-1));
                }
            }
        }

        if (data.size() == 0){
            throw  new IllegalArgumentException("Please select another select start date, data is not found at start date");
        }
        return data;
    }
}
