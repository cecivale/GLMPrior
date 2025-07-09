package glmprior.beauti;

import bdmmprime.beauti.EpochVisualizerPane;
import bdmmprime.distribution.BirthDeathMigrationDistribution;
import bdmmprime.parameterization.SkylineParameter;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.tree.TraitSet;
import beast.base.evolution.tree.Tree;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.util.FXUtils;
import glmprior.util.GLMPrior;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class GLMSkylineInputEditor extends InputEditor.Base {

    SkylineParameter skylineParameter;

    TableView<ValuesTableEntry> valuesTable;
    TableView<ObservableList<String>> glmValuesTable;
    Button loadButton;
    Button showTableButton;

    Label value;

    VBox mainInputBox;

    EpochVisualizerPane epochVisualizer;

    Boolean isGLM = false;

    public GLMSkylineInputEditor(BeautiDoc doc) {
        super(doc);
    }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr,
                     ExpandOption isExpandOption, boolean addButtons) {

        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr = itemNr;
        pane = FXUtils.newHBox();

        skylineParameter = (SkylineParameter) input.get();
        ensureValuesConsistency();

        addInputLabel();

        // Add elements specific to change times

        int nChanges = skylineParameter.getChangeCount();

        mainInputBox = FXUtils.newVBox();
        mainInputBox.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY,
                BorderStrokeStyle.SOLID, null, null)));

        HBox boxHoriz = FXUtils.newHBox();
        Label changePointLabel = new Label("Number of change times:");
        Spinner<Integer> changeCountSpinner = new Spinner<>(0, Integer.MAX_VALUE, nChanges);
        changeCountSpinner.setEditable(true);
        changeCountSpinner.setRepeatDelay(Duration.INDEFINITE); // (Hack around weird race condition I can't solve)
        boxHoriz.getChildren().add(changePointLabel);
        boxHoriz.getChildren().add(changeCountSpinner);

        mainInputBox.getChildren().add(boxHoriz);

        VBox changeTimesBox = FXUtils.newVBox();
        HBox changeTimesEntryRow = FXUtils.newHBox();
        changeTimesBox.getChildren().add(changeTimesEntryRow);
        HBox changeTimesBoxRow = FXUtils.newHBox();
        CheckBox timesAreAgesCheckBox = new CheckBox("Times specified as ages");
        changeTimesBoxRow.getChildren().add(timesAreAgesCheckBox);
        CheckBox estimateTimesCheckBox = new CheckBox("Estimate change times");
        changeTimesBoxRow.getChildren().add(estimateTimesCheckBox);
        changeTimesBox.getChildren().add(changeTimesBoxRow);

        changeTimesBoxRow = FXUtils.newHBox();
        CheckBox timesAreRelativeCheckBox = new CheckBox("Relative to process length");
        changeTimesBoxRow.getChildren().add(timesAreRelativeCheckBox);
        Button distributeChangeTimesButton = new Button("Distribute evenly");
        changeTimesBoxRow.getChildren().add(distributeChangeTimesButton);
        changeTimesBox.getChildren().add(changeTimesBoxRow);

        mainInputBox.getChildren().add(changeTimesBox);

        if (nChanges > 0) {
            updateChangeTimesUI((RealParameter) skylineParameter.changeTimesInput.get(),
                    changeTimesEntryRow);
            timesAreAgesCheckBox.setSelected(skylineParameter.timesAreAgesInput.get());
            timesAreRelativeCheckBox.setSelected(skylineParameter.timesAreRelativeInput.get());

            estimateTimesCheckBox.setSelected(
                    ((RealParameter) skylineParameter.changeTimesInput.get())
                            .isEstimatedInput.get());
            estimateTimesCheckBox.disableProperty().set(isGLM);

            changeCountSpinner.getEditor().setDisable(isGLM); // Disable text editing
            changeCountSpinner.setDisable(isGLM); // Disable the whole spinner
        } else {
            changeTimesBox.setVisible(false);
            changeTimesBox.setManaged(false);
        }
        boxHoriz = FXUtils.newHBox();
        CheckBox glmCheckBox = new CheckBox("Use GLM");
        glmCheckBox.setSelected(isGLM);
        boxHoriz.getChildren().add(glmCheckBox);
        mainInputBox.getChildren().add(boxHoriz);


        // Add GLM-specific elements
        VBox glmBox = FXUtils.newVBox();

        boxHoriz = FXUtils.newHBox();
        glmValuesTable = new TableView<>();
        glmValuesTable.getSelectionModel().setCellSelectionEnabled(true);
        glmValuesTable.setEditable(true);
        loadButton = new Button("Load GLM predictor csv");
        showTableButton = new Button("Show Table"); // New button
        showTableButton.setOnAction(event -> showTablePopup()); // Open pop-up on click

        CheckBox estimateIndicatorsCheckBox = new CheckBox("Estimate indicators");
        CheckBox estimateErrorCheckBox = new CheckBox("Estimate error");
        VBox glmValuesTableBoxCol = FXUtils.newVBox();
        glmValuesTableBoxCol.getChildren().add(loadButton);
        glmValuesTableBoxCol.getChildren().add(showTableButton);
//        glmValuesTableBoxCol.getChildren().add(glmValuesTable);
        glmValuesTableBoxCol.getChildren().add(estimateIndicatorsCheckBox);
        glmValuesTableBoxCol.getChildren().add(estimateErrorCheckBox);

        estimateIndicatorsCheckBox.setSelected(isGLM && ((GLMPrior) skylineParameter.skylineValuesInput.get()).indicatorsInput.get() != null &&
                ((GLMPrior) skylineParameter.skylineValuesInput.get()).indicatorsInput.get().isEstimatedInput.get());
        estimateErrorCheckBox.setSelected(isGLM && ((GLMPrior) skylineParameter.skylineValuesInput.get()).errorInput.get() != null &&
                ((GLMPrior) skylineParameter.skylineValuesInput.get()).errorInput.get().isEstimatedInput.get());
        if (isGLM && !((GLMPrior) skylineParameter.skylineValuesInput.get()).predictorsInput.get().isEmpty()) {
            showTableButton.setManaged(true);
            showTableButton.setVisible(true);
        } else {
            showTableButton.setManaged(false);
            showTableButton.setVisible(false);
        }
        boxHoriz.getChildren().add(glmValuesTableBoxCol);

        glmBox.getChildren().add(boxHoriz);
        mainInputBox.getChildren().add(glmBox);

        glmBox.setManaged(isGLM);
        glmBox.setVisible(isGLM);


        // Add elements specific to values

        boxHoriz = FXUtils.newHBox();
        value = new Label("Value:");
        boxHoriz.getChildren().add(value);
        valuesTable = new TableView<>();
        valuesTable.getSelectionModel().setCellSelectionEnabled(true);
        valuesTable.setEditable(true);
        VBox valuesTableBoxCol = FXUtils.newVBox();
        valuesTableBoxCol.getChildren().add(valuesTable);
        boxHoriz.getChildren().add(valuesTableBoxCol);


        mainInputBox.getChildren().add(boxHoriz);

        boxHoriz = FXUtils.newHBox();
        CheckBox scalarRatesCheckBox = new CheckBox("Scalar values");
        boxHoriz.getChildren().add(scalarRatesCheckBox);
        CheckBox linkValuesCheckBox = new CheckBox("Link identical values");
        linkValuesCheckBox.setSelected(skylineParameter.linkIdenticalValuesInput.get());
        boxHoriz.getChildren().add(linkValuesCheckBox);

        mainInputBox.getChildren().add(boxHoriz);

        boxHoriz = FXUtils.newHBox();
        CheckBox estimateValuesCheckBox = new CheckBox("Estimate values");
        if (skylineParameter.skylineValuesInput.get() instanceof RealParameter) {
            estimateValuesCheckBox.setSelected(((RealParameter) skylineParameter.skylineValuesInput.get()).isEstimatedInput.get());
        }
        boxHoriz.getChildren().add(estimateValuesCheckBox);
        CheckBox visualizerCheckBox = new CheckBox("Display epoch visualization");
        visualizerCheckBox.setSelected(skylineParameter.epochVisualizerDisplayed);
        boxHoriz.getChildren().add(visualizerCheckBox);
        mainInputBox.getChildren().add(boxHoriz);

        epochVisualizer = new EpochVisualizerPane(getTree(), getTypeTraitSet(), skylineParameter);
        epochVisualizer.widthProperty().bind(mainInputBox.widthProperty().subtract(10));
        epochVisualizer.setVisible(skylineParameter.epochVisualizerDisplayed);
        epochVisualizer.setManaged(skylineParameter.epochVisualizerDisplayed);
        mainInputBox.getChildren().add(epochVisualizer);

        int nTypes = skylineParameter.getNTypes();

        if ((skylineParameter.skylineValuesInput.get() instanceof RealParameter &&
                (skylineParameter.skylineValuesInput.get()).getDimension() == (nChanges + 1)) ||
                (skylineParameter.skylineValuesInput.get() instanceof GLMPrior &&
                        ((GLMPrior) skylineParameter.skylineValuesInput.get()).predictorsInput.get().get(0).getDimension() == (nChanges + 1))) {
            if (nTypes > 1) {
                scalarRatesCheckBox.setSelected(true);
                scalarRatesCheckBox.disableProperty().set(false);
                epochVisualizer.setScalar(true);
            } else {
                scalarRatesCheckBox.setSelected(false);
                scalarRatesCheckBox.disableProperty().set(true);
                epochVisualizer.setScalar(false);
            }
        } else {
            scalarRatesCheckBox.setSelected(false);
            epochVisualizer.setScalar(false);
        }

        pane.getChildren().add(mainInputBox);
        getChildren().add(pane);

        glmBox.setManaged(isGLM);
        glmBox.setVisible(isGLM);
        value.setManaged(!isGLM);
        value.setVisible(!isGLM);
        valuesTableBoxCol.setManaged(!isGLM);
        valuesTableBoxCol.setVisible(!isGLM);
        linkValuesCheckBox.setManaged(!isGLM);
        linkValuesCheckBox.setVisible(!isGLM);
        estimateValuesCheckBox.setManaged(!isGLM);
        estimateValuesCheckBox.setVisible(!isGLM);

        // Add event listeners:
        changeCountSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println(oldValue + " -> " + newValue);

            if (newValue > 0) {
                RealParameter param = (RealParameter) skylineParameter.changeTimesInput.get();
                if (param == null) {
                    if (!doc.pluginmap.containsKey(getChangeTimesParameterID())) {
                        param = new RealParameter("0.0");
                        param.setID(getChangeTimesParameterID());
                    } else {
                        param = (RealParameter) doc.pluginmap.get(getChangeTimesParameterID());
                    }
                    skylineParameter.changeTimesInput.setValue(param, skylineParameter);
                }
                param.setDimension(newValue);
                sanitiseRealParameter(param);
            } else {
                skylineParameter.changeTimesInput.setValue(null, skylineParameter);
            }

            ensureValuesConsistency();

            if (newValue > 0) {
                updateChangeTimesUI((RealParameter) skylineParameter.changeTimesInput.get(),
                        changeTimesEntryRow);
                timesAreAgesCheckBox.setSelected(skylineParameter.timesAreAgesInput.get());

                estimateTimesCheckBox.setSelected(
                        ((RealParameter) skylineParameter.changeTimesInput.get())
                                .isEstimatedInput.get());

                changeTimesBox.setManaged(true);
                changeTimesBox.setVisible(true);
            } else {
                changeTimesBox.setManaged(false);
                changeTimesBox.setVisible(false);
            }

            System.out.println(skylineParameter);
            System.out.println(scalarRatesCheckBox.isSelected());

            if (!isGLM) {
                if (!(skylineParameter.skylineValuesInput.get() instanceof RealParameter)) {
                    RealParameter valuesParameter = new RealParameter("1.0");
                    valuesParameter.setDimension(nChanges + 1);

                    valuesParameter.setID(getGLMValuesParameterID());
                    skylineParameter.skylineValuesInput.setValue(valuesParameter, skylineParameter);
                }
                updateValuesUI();
            } else {
                if (!(skylineParameter.skylineValuesInput.get() instanceof GLMPrior)) {
                    GLMPrior glmValue = new GLMPrior();
                    glmValue.setID(getGLMValuesParameterID());
                    skylineParameter.skylineValuesInput.setValue(glmValue, skylineParameter);
                }
                updateGLMUI();
            }
        });

        // Set up the button action
        loadButton.setOnAction(event -> {
            File file = FXUtils.getLoadFile("Select predictor file", null, null, "csv");

            if (file != null) {
                loadCSVFile(file, glmValuesTable);
            }

            int dim = glmValuesTable.getColumns().size() - 1;
            if (nTypes > 1 && !scalarRatesCheckBox.isSelected() && dim % nTypes != 0) {
                showInvalidInputError(dim, nTypes);
                return;
            }

            GLMPrior glmValue;
            if (skylineParameter.skylineValuesInput.get() instanceof GLMPrior) {
                glmValue = (GLMPrior) skylineParameter.skylineValuesInput.get();
            } else {
                glmValue = new GLMPrior();
            }

            glmValue.setID(getGLMValuesParameterID());
            glmValuesTable.getItems().forEach(row -> {
                RealParameter predictor = new RealParameter(processRow(row));
                predictor.setID(getPredictorParameterID(row.get(0)));
                sanitiseRealParameter(predictor);
                glmValue.predictorsInput.get().add(predictor);

            });
            RealParameter coefficients = glmValue.coefficientsInput.get();
            if (coefficients == null) {
                if (!doc.pluginmap.containsKey(getGLMCoefficientParameterID())) {
                    coefficients = new RealParameter("1.0");
                    coefficients.setID(getGLMCoefficientParameterID());
                } else {
                    coefficients = (RealParameter) doc.pluginmap.get(getGLMCoefficientParameterID());
                }
                glmValue.coefficientsInput.setValue(coefficients, glmValue);
            }
            coefficients.setDimension(glmValue.predictorsInput.get().size());
            coefficients.isEstimatedInput.setValue(true, coefficients);
            sanitiseRealParameter(coefficients);
            BooleanParameter indicators = glmValue.indicatorsInput.get();
            if (indicators == null){
                if (!doc.pluginmap.containsKey(getGLMIndicatorParameterID())) {
                    indicators = new BooleanParameter("1");
                    indicators.setID(getGLMIndicatorParameterID());
                } else {
                    indicators = (BooleanParameter) doc.pluginmap.get(getGLMIndicatorParameterID());
                }
                glmValue.indicatorsInput.setValue(indicators, glmValue);
            }
            indicators.setDimension(glmValue.predictorsInput.get().size());



            RealParameter error = glmValue.errorInput.get();
            if (error == null && estimateErrorCheckBox.isSelected()) {
                if (!doc.pluginmap.containsKey(getGLMErrorParameterID())) {
                    error = new RealParameter("0.001");
                    error.setID(getGLMErrorParameterID());
                } else {
                    error = (RealParameter) doc.pluginmap.get(getGLMErrorParameterID());
                }
                glmValue.errorInput.setValue(error, glmValue);
            }

            skylineParameter.skylineValuesInput.setValue(glmValue, skylineParameter);
            changeCountSpinner.getValueFactory().setValue(getnChanges(nTypes, dim));
            showTableButton.setManaged(true);
            showTableButton.setVisible(true);
            sync();
        });


        timesAreAgesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            skylineParameter.timesAreAgesInput.setValue(newValue, skylineParameter);
            skylineParameter.initAndValidate();
            System.out.println(skylineParameter);
            epochVisualizer.repaintCanvas();
        });

        estimateTimesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            RealParameter changeTimes = (RealParameter) skylineParameter.changeTimesInput.get();
            changeTimes.isEstimatedInput.setValue(newValue, changeTimes);
            sync();
        });

        estimateIndicatorsCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            GLMPrior glmValue = (GLMPrior) skylineParameter.skylineValuesInput.get();
            BooleanParameter indicators = glmValue.indicatorsInput.get();
            indicators.isEstimatedInput.setValue(newValue, indicators);
            sync();
        });

        estimateErrorCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            GLMPrior glmValue = (GLMPrior) skylineParameter.skylineValuesInput.get();
            RealParameter error = glmValue.errorInput.get();
            if (error == null && newValue){
                if (!doc.pluginmap.containsKey(getGLMErrorParameterID())) {
                    error = new RealParameter("0.001");
                    error.setID(getGLMErrorParameterID());
                    error.isEstimatedInput.setValue(newValue, error);
                } else {
                    error = (RealParameter) doc.pluginmap.get(getGLMErrorParameterID());
                    error.isEstimatedInput.setValue(newValue, error);
                }
                glmValue.errorInput.setValue(error, glmValue);
            }

            sync();
        });

        timesAreRelativeCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            skylineParameter.timesAreRelativeInput.setValue(newValue, skylineParameter);
            skylineParameter.initAndValidate();
            System.out.println(skylineParameter);
            epochVisualizer.repaintCanvas();
        });

        distributeChangeTimesButton.setOnAction(e -> {

            RealParameter changeTimesParam = (RealParameter) skylineParameter.changeTimesInput.get();
            int nTimes = changeTimesParam.getDimension();

            if (skylineParameter.timesAreRelativeInput.get()) {
                for (int i = 0; i < nTimes; i++) {
                    changeTimesParam.setValue(i, ((double) (i + 1)) / (nTimes + 1));
                }
            } else {
                if (nTimes > 1) {
                    for (int i = 0; i < nTimes - 1; i++) {
                        changeTimesParam.setValue(i,
                                (changeTimesParam.getArrayValue(nTimes - 1) * (i + 1)) / (nTimes + 1));
                    }
                }
            }

            sanitiseRealParameter(changeTimesParam);
            sync();
        });

        scalarRatesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            skylineParameter.isScalarInput.setValue(newValue, skylineParameter);

            ensureValuesConsistency();
            if (skylineParameter.skylineValuesInput.get() instanceof RealParameter) {
                sanitiseRealParameter((RealParameter) skylineParameter.skylineValuesInput.get());
                updateValuesUI();
            } else {
                updateGLMUI();
            }
            System.out.println(skylineParameter);
            epochVisualizer.setScalar(newValue);
        });

        glmCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {

            if (!valuesTable.getItems().isEmpty() || !glmValuesTable.getItems().isEmpty()) {
                String msg = newValue ? "Switching to GLM will discard the current values. Continue?" :
                        "Switching to scalar values will discard the current GLM predictors. Continue?";

                ButtonType result = beastfx.app.util.Alert.showConfirmDialog(null,
                        msg,
                        "Continue?", beastfx.app.util.Alert.YES_NO_OPTION);
                if (result == beastfx.app.util.Alert.NO_OPTION) {
                    return;
                }
            }

            glmBox.setManaged(newValue);
            glmBox.setVisible(newValue);
            value.setManaged(!newValue);
            value.setVisible(!newValue);
            valuesTableBoxCol.setManaged(!newValue);
            valuesTableBoxCol.setVisible(!newValue);
            linkValuesCheckBox.setManaged(!newValue);
            linkValuesCheckBox.setVisible(!newValue);
            estimateValuesCheckBox.setManaged(!newValue);
            estimateValuesCheckBox.setVisible(!newValue);
            estimateTimesCheckBox.disableProperty().set(newValue);
            isGLM = newValue;

            isGLM(newValue);
            if (!newValue) {
                if (!(skylineParameter.skylineValuesInput.get() instanceof RealParameter)) {
                    RealParameter valuesParameter = new RealParameter("0.0");
                    valuesParameter.setDimension(nChanges + 1);
                    valuesParameter.setID(getGLMValuesParameterID());
                    skylineParameter.skylineValuesInput.setValue(valuesParameter, skylineParameter);
                }
                updateValuesUI();
            } else {
                if (!(skylineParameter.skylineValuesInput.get() instanceof GLMPrior)) {
                    GLMPrior glmValue = new GLMPrior();
                    glmValue.setID(getGLMValuesParameterID());
                    skylineParameter.skylineValuesInput.setValue(glmValue, skylineParameter);
                }
                updateGLMUI();
            }
        });

        linkValuesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            skylineParameter.linkIdenticalValuesInput.setValue(newValue, beastObject);
            sync();
        });

        estimateValuesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (skylineParameter.skylineValuesInput.get() instanceof RealParameter valuesParameter) {
                valuesParameter.isEstimatedInput.setValue(newValue, valuesParameter);
            }
            sync();
        });

        visualizerCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            skylineParameter.epochVisualizerDisplayed = newValue;
            epochVisualizer.setVisible(newValue);
            epochVisualizer.setManaged(newValue);
        });
    }


    private void loadCSVFile(File file, TableView<ObservableList<String>> glmValuesTable) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            // Clear existing columns and rows
            glmValuesTable.getColumns().clear();
            glmValuesTable.getItems().clear();

            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",", -1);

                if (isFirstLine) {
                    // Create columns based on the header row
                    for (int i = 0; i < fields.length; i++) {
                        final int colIndex = i;
                        TableColumn<ObservableList<String>, String> column = new TableColumn<>(fields[i]);
                        column.setCellValueFactory(cellData ->
                                new SimpleStringProperty(cellData.getValue().get(colIndex))
                        );
                        glmValuesTable.getColumns().add(column);
                    }
                    isFirstLine = false;
                }
                if (isHeaderRow(fields)) continue; // Skip rows with non-numeric values
                // Add rows to the table
                ObservableList<String> row = FXCollections.observableArrayList(fields);
                glmValuesTable.getItems().add(row);

            }
        } catch (IOException e) {
            showErrorDialog();
        }
    }

    /**
     * Determines if a row is a header row (contains only non-numeric values).
     */
    private boolean isHeaderRow(String[] fields) {
        for (String value : fields) {
            if (isNumeric(value)) {
                return false; // If any value is numeric, this is NOT a header row
            }
        }
        return true; // All values are non-numeric → this is a header row
    }

    /**
     * Checks if a given string is numeric.
     */
    private boolean isNumeric(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        try {
            Double.parseDouble(str.trim()); // Attempt to parse as a number
            return true; // If successful, it's numeric
        } catch (NumberFormatException e) {
            return false; // Parsing failed → it's not numeric
        }
    }

    private void showTablePopup() {
        Stage popupStage = new Stage();
        popupStage.setTitle("GLM Values Table");

        // Set it as a modal window
        popupStage.initModality(Modality.APPLICATION_MODAL);

        // Create a layout and add the table
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        glmValuesTable.refresh();
        layout.getChildren().add(glmValuesTable);

        // Create scene and show the pop-up
        Scene popupScene = new Scene(layout, 600, 400);
        popupStage.setScene(popupScene);
        popupStage.show();
    }


    Double[] processRow(ObservableList<String> row) {
        return row.stream()
                .map(value -> {
                    try {
                        return value == null || value.trim().isEmpty()
                                ? null
                                : Double.valueOf(value.trim());
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Skipping invalid numeric value: " + value);
                        return null;  // Skip non-numeric values
                    }
                })
                .filter(Objects::nonNull)  // Remove null values from the list
                .toArray(Double[]::new);
    }

    private void showErrorDialog() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error loading file");
        alert.setHeaderText(null);
        alert.setContentText("Could not read the file.");
        alert.showAndWait();
    }

    private void showInvalidInputError(int dim, int nTypes) {
        String errorMsg = "Invalid input: number of columns in GLM table (" + dim
                + ") must be [number of types]x[number of epochs] and therefore be evenly divisible by the number of types (" +
                nTypes + "). To correct you should adjust one or more: " +
                "\n 1) the GLM table" +
                "\n 2) number of types" +
                "\n 3) select scalar values.";


        beastfx.app.util.Alert.showMessageDialog(
                this,
                errorMsg,
                "Error: Invalid input",
                beastfx.app.util.Alert.ERROR_MESSAGE
        );

        glmValuesTable.getItems().clear();
    }

    /**
     * Configure inputs for change times.
     *
     * @param parameter           change times parameter
     * @param changeTimesEntryRow HBox containing time inputs
     */
    void updateChangeTimesUI(RealParameter parameter,
                             HBox changeTimesEntryRow) {
        changeTimesEntryRow.getChildren().clear();
        changeTimesEntryRow.getChildren().add(new Label("Change times:"));
        for (int i = 0; i < parameter.getDimension(); i++) {
            changeTimesEntryRow.getChildren().add(new Label("Epoch " + (i + 1) + "->" + (i + 2) + ": "));
            TextField textField = new TextField(parameter.getValue(i).toString());

            textField.setPrefWidth(50);
            textField.setPadding(new Insets(0));
            HBox.setMargin(textField, new Insets(0, 10, 0, 0));

            int index = i;
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                parameter.setValue(index, Double.valueOf(newValue));
                sanitiseRealParameter(parameter);
                skylineParameter.initAndValidate();
                System.out.println(skylineParameter);
                epochVisualizer.repaintCanvas();
            });

            changeTimesEntryRow.getChildren().add(textField);
        }
    }

    void sanitiseRealParameter(RealParameter parameter) {
        parameter.valuesInput.setValue(
                Arrays.stream(parameter.getDoubleValues())
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(" ")),
                parameter);
        parameter.initAndValidate();
    }


    String getChangeTimesParameterID() {
        int idx = skylineParameter.getID().indexOf("SP");
        String prefix = skylineParameter.getID().substring(0, idx);
        String suffix = skylineParameter.getID().substring(idx + 2);

        return prefix + "ChangeTimes" + suffix;
    }

    String getGLMValuesParameterID() {
        int idx = skylineParameter.getID().indexOf("SP");
        String prefix = skylineParameter.getID().substring(0, idx);
        String suffix = skylineParameter.getID().substring(idx + 2);

        return prefix + suffix;
    }

    String getGLMCoefficientParameterID() {
        int idx = skylineParameter.getID().indexOf("SP");
        String prefix = skylineParameter.getID().substring(0, idx);
        String suffix = skylineParameter.getID().substring(idx + 2);

        return prefix + "Coefficients" + suffix;
    }

    String getGLMIndicatorParameterID() {
        int idx = skylineParameter.getID().indexOf("SP");
        String prefix = skylineParameter.getID().substring(0, idx);
        String suffix = skylineParameter.getID().substring(idx + 2);

        return prefix + "Indicators" + suffix;
    }

    String getPredictorParameterID(String predName) {
        int idx = skylineParameter.getID().indexOf("SP");
        String prefix = skylineParameter.getID().substring(0, idx);
        String suffix = skylineParameter.getID().substring(idx + 2);

        return prefix + "_" + predName + "_" + suffix;
    }

    String getGLMErrorParameterID() {
        int idx = skylineParameter.getID().indexOf("SP");
        String prefix = skylineParameter.getID().substring(0, idx);
        String suffix = skylineParameter.getID().substring(idx + 2);

        return prefix + "Errors" + suffix;
    }

    private String getPartitionID() {
        return skylineParameter.getID().split("\\.t:")[1];
    }

    protected Tree getTree() {
        return (Tree) doc.pluginmap.get("Tree.t:" + getPartitionID());
    }

    protected TraitSet getTypeTraitSet() {
        BirthDeathMigrationDistribution bdmmPrimeDistrib =
                (BirthDeathMigrationDistribution) doc.pluginmap.get("BDMMPrime.t:" + getPartitionID());

        return bdmmPrimeDistrib.typeTraitSetInput.get();
    }

    /**
     * Called on initialisation to ensure values parameter is
     * up-to-date with the number of types.  This is necessary because
     * the type count is affected by the TypeTraitSetInputEditor, which
     * calls a sync() when this value changes.
     */
    abstract void ensureValuesConsistency();

    abstract void updateValuesUI();

    abstract void updateGLMUI();

    abstract void isGLM(boolean isGLM);

    abstract int getnChanges(int nTypes, int dimension);

    public abstract static class ValuesTableEntry {
    }
}
