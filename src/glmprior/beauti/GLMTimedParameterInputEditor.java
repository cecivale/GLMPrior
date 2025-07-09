package glmprior.beauti;

import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.util.FXUtils;
import glmprior.parameterization.GLMTimedParameter;
import glmprior.util.GLMPrior;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GLMTimedParameterInputEditor extends InputEditor.Base {

    GLMTimedParameter timedParameter;

    TableView<TimedParamValuesTableEntry> valuesTable;

    TableView<ObservableList<String>> glmValuesTable;
    Button loadButton;
    Button showTableButton;
    Boolean isGLM = false;
    Label value;
    Boolean isScalar = false;

    public GLMTimedParameterInputEditor(BeautiDoc doc) {
        super(doc);
    }

    @Override
    public Class<?> type() {
        return GLMTimedParameter.class;
    }


    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr,
                     ExpandOption isExpandOption, boolean addButtons) {

        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr = itemNr;
        pane = FXUtils.newHBox();

        timedParameter = (GLMTimedParameter)input.get();
        isGLM = timedParameter.isGLMInput.get();

        addInputLabel();

        ensureValuesConsistency(true);

        // Add elements specific to times

        int timeCount = timedParameter.getTimeCount();

        VBox mainInputBox = FXUtils.newVBox();
        mainInputBox.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY,
                BorderStrokeStyle.SOLID, null, null)));

        HBox boxHoriz = FXUtils.newHBox();
        Label changePointLabel = new Label("Number of times:");
        Spinner<Integer> timeCountSpinner = new Spinner<>(0, Integer.MAX_VALUE, timeCount);
        timeCountSpinner.setEditable(true);
        boxHoriz.getChildren().add(changePointLabel);
        boxHoriz.getChildren().add(timeCountSpinner);

        mainInputBox.getChildren().add(boxHoriz);

        VBox timesAndValuesBox = FXUtils.newVBox();
        HBox timesEntryRow = FXUtils.newHBox();
        timesAndValuesBox.getChildren().add(timesEntryRow);
        HBox timesBoxRow = FXUtils.newHBox();
        CheckBox timesAreAgesCheckBox = new CheckBox("Times specified as ages");
        timesBoxRow.getChildren().add(timesAreAgesCheckBox);
        CheckBox estimateTimesCheckBox = new CheckBox("Estimate times");
        timesBoxRow.getChildren().add(estimateTimesCheckBox);
        timesAndValuesBox.getChildren().add(timesBoxRow);


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

        estimateIndicatorsCheckBox.setSelected(isGLM && ((GLMPrior) timedParameter.valuesInput.get()).indicatorsInput.get().isEstimatedInput.get());
        estimateErrorCheckBox.setSelected(isGLM && ((GLMPrior) timedParameter.valuesInput.get()).errorInput.get().isEstimatedInput.get());
        if (isGLM && !((GLMPrior) timedParameter.valuesInput.get()).predictorsInput.get().isEmpty()) {
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
        value = new Label("Values:");
        boxHoriz.getChildren().add(value);
        valuesTable = new TableView<>();
        valuesTable.getSelectionModel().setCellSelectionEnabled(true);
        valuesTable.setEditable(true);
        valuesTable.setFixedCellSize(25);
        valuesTable.prefHeightProperty().bind(valuesTable.fixedCellSizeProperty()
                .multiply(Bindings.size(valuesTable.getItems()).add(1.1)));
        VBox valuesTableBoxCol = FXUtils.newVBox();
        valuesTableBoxCol.getChildren().add(valuesTable);
        boxHoriz.getChildren().add(valuesTableBoxCol);

        timesAndValuesBox.getChildren().add(boxHoriz);

        boxHoriz = FXUtils.newHBox();
        CheckBox scalarValues = new CheckBox("Scalar values");
        boxHoriz.getChildren().add(scalarValues);
        CheckBox estimateValuesCheckBox = new CheckBox("Estimate values");
        boxHoriz.getChildren().add(estimateValuesCheckBox);

        int nTypes = timedParameter.getNTypes();
        if (nTypes > 1) {
            scalarValues.setSelected(true);
            scalarValues.disableProperty().set(false);
            isScalar = true;
        } else {
            scalarValues.setSelected(false);
            scalarValues.disableProperty().set(true);
            isScalar = false;
        }

        timesAndValuesBox.getChildren().add(boxHoriz);
        mainInputBox.getChildren().add(timesAndValuesBox);

        if (timeCount > 0) {
            updateTimesUI(timesEntryRow);
            if (!isGLM && !(timedParameter.valuesInput.get() instanceof GLMPrior))
                updateValuesUI();
            else
                updateGLMUI();

            timesAreAgesCheckBox.setSelected(timedParameter.timesAreAgesInput.get());

            estimateTimesCheckBox.setSelected(
                    ((RealParameter) timedParameter.timesInput.get())
                            .isEstimatedInput.get());
            estimateTimesCheckBox.disableProperty().set(isGLM);
            timeCountSpinner.getEditor().setDisable(isGLM); // Disable text editing
            timeCountSpinner.setDisable(isGLM); // Disable the whole spinner

            if (timedParameter.valuesInput.get() instanceof RealParameter) {
                estimateValuesCheckBox.setSelected(((RealParameter)
                        timedParameter.valuesInput.get()).isEstimatedInput.get());
            }
            estimateValuesCheckBox.disableProperty().set(isGLM);
        } else {
            timesAndValuesBox.setVisible(false);
            timesAndValuesBox.setManaged(false);
        }

        pane.getChildren().add(mainInputBox);
        getChildren().add(pane);


        glmBox.setManaged(isGLM);
        glmBox.setVisible(isGLM);
        value.setManaged(!isGLM);
        value.setVisible(!isGLM);
        valuesTableBoxCol.setManaged(!isGLM);
        valuesTableBoxCol.setVisible(!isGLM);
        estimateValuesCheckBox.setManaged(!isGLM);
        estimateValuesCheckBox.setVisible(!isGLM);

        // Add event listeners:
        timeCountSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println(oldValue + " -> " + newValue);

            if (newValue > 0) {
//                if (!isGLM){
                    RealParameter times = getTimesParam();
                    times.setDimension(newValue);
                    sanitiseRealParameter(times);
                    timedParameter.timesInput.setValue(times, timedParameter);
//                } else {
//                    GLMLogLinear glmTimesValue = new GLMLogLinear();
//                    glmTimesValue.setID(getTimesParameterID());
//                    timedParameter.timesInput.setValue(glmTimesValue, timedParameter);
//                }
            } else {
                if (estimateTimesCheckBox.isSelected())
                    estimateTimesCheckBox.fire();

                if (estimateValuesCheckBox.isSelected())
                    estimateValuesCheckBox.fire();
                timedParameter.timesInput.setValue(null, timedParameter);
            }

            ensureValuesConsistency(scalarValues.isSelected());

            if (newValue > 0) {
                updateTimesUI(timesEntryRow);
                if (!isGLM)
                    updateValuesUI();
                else
                    updateGLMUI();


                timesAreAgesCheckBox.setSelected(timedParameter.timesAreAgesInput.get());

                estimateTimesCheckBox.setSelected(getTimesParam().isEstimatedInput.get());

                timesAndValuesBox.setManaged(true);
                timesAndValuesBox.setVisible(true);


            } else {
                timesAndValuesBox.setManaged(false);
                timesAndValuesBox.setVisible(false);
            }

            System.out.println(timedParameter);
            System.out.println(scalarValues.isSelected());
//                    valuesParameter.setDimension(newValue);
//
//                    valuesParameter.setID(getGLMValuesParameterID());
//                    timedParameter.timesInput.setValue(valuesParameter, timedParameter);
//                }
//                updateValuesUI();
//            } else {
//                if (!(timedParameter.timesInput.get() instanceof GLMLogLinear)) {
//                    GLMLogLinear glmValue = new GLMLogLinear();
//                    glmValue.setID(getGLMValuesParameterID());
//                    timedParameter.timesInput.setValue(glmValue, timedParameter);
//                }
//                updateGLMUI();
//            }
        });

        loadButton.setOnAction(event -> {
//            );
            File file = FXUtils.getLoadFile("Select predictor file", null, null, "csv");

            if (file != null) {
                loadCSVFile(file, glmValuesTable);
            }

            int dim = glmValuesTable.getColumns().size() - 1;
            if (nTypes > 1 && !scalarValues.isSelected() && dim % nTypes != 0) {
                showInvalidInputError(dim, nTypes);
                return;
            }

            GLMPrior glmValue;
            if (timedParameter.valuesInput.get() instanceof GLMPrior) {
                glmValue = (GLMPrior) timedParameter.valuesInput.get();
            } else {
                glmValue = new GLMPrior();
            }

            glmValue.setID(getTimesParameterID());
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
            if (indicators == null) {
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
            if (error == null) {
                if (!doc.pluginmap.containsKey(getGLMErrorParameterID())) {
                    error = new RealParameter("0.001");
                    error.setID(getGLMErrorParameterID());
                } else {
                    error = (RealParameter) doc.pluginmap.get(getGLMErrorParameterID());
                }
                glmValue.errorInput.setValue(error, glmValue);
            }

            timedParameter.valuesInput.setValue(glmValue, timedParameter);
            timeCountSpinner.getValueFactory().setValue(getNTimes(nTypes, dim));
            showTableButton.setManaged(true);
            showTableButton.setVisible(true);
            sync();
        });

        timesAreAgesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            timedParameter.timesAreAgesInput.setValue(newValue, timedParameter);
            timedParameter.initAndValidate();
            System.out.println(timedParameter);
        });

        estimateTimesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            RealParameter changeTimes = (RealParameter) timedParameter.timesInput.get();
            changeTimes.isEstimatedInput.setValue(newValue, changeTimes);
            sync();
        });

        scalarValues.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue)
                getValuesParam().setDimension(timedParameter.getTimeCount());
            else
                getValuesParam().setDimension(timedParameter.getNTypes() * timedParameter.getTimeCount());

            sanitiseRealParameter(getValuesParam());
            ensureValuesConsistency(newValue);
            updateValuesUI();
            System.out.println(timedParameter);
        });

        estimateValuesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            RealParameter valuesParam = getValuesParam();
            valuesParam.isEstimatedInput.setValue(newValue, valuesParam);
            hardSync();
        });

        glmCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {

            glmBox.setManaged(newValue);
            glmBox.setVisible(newValue);
            value.setManaged(!newValue);
            value.setVisible(!newValue);
            valuesTableBoxCol.setManaged(!newValue);
            valuesTableBoxCol.setVisible(!newValue);
            estimateValuesCheckBox.setManaged(!newValue);
            estimateValuesCheckBox.setVisible(!newValue);
            estimateTimesCheckBox.disableProperty().set(newValue);
            isGLM = newValue;


            timedParameter.isGLMInput.set(newValue);
            if (!newValue) {
                if (!(timedParameter.valuesInput.get() instanceof RealParameter)) {
                    RealParameter valuesParameter = new RealParameter("0.0");
                    valuesParameter.setDimension(timedParameter.getNTypes() * timedParameter.getTimeCount());
                    valuesParameter.setID(getGLMValuesParameterID());
                    timedParameter.valuesInput.setValue(valuesParameter, timedParameter);
                }
                updateValuesUI();
            } else {
                if (!(timedParameter.valuesInput.get() instanceof GLMPrior)) {
                    GLMPrior glmTimesValue = new GLMPrior();
                    glmTimesValue.setID(getGLMValuesParameterID());
                    timedParameter.valuesInput.setValue(glmTimesValue, timedParameter);
                }
                updateGLMUI();
            }
        });


    }

    /**
     * Configure inputs for times.
     *
     * @param changeTimesEntryRow HBox containing time inputs
     */
    void updateTimesUI(HBox changeTimesEntryRow) {
        RealParameter parameter = getTimesParam();
        changeTimesEntryRow.getChildren().clear();
        changeTimesEntryRow.getChildren().add(new Label("Times:"));
        for (int i=0; i<parameter.getDimension(); i++) {
            changeTimesEntryRow.getChildren().add(new Label("Time " + (i+1) + ": "));
            TextField textField = new TextField(parameter.getValue(i).toString());

            textField.setPrefWidth(50);
            textField.setPadding(new Insets(0));
            HBox.setMargin(textField, new Insets(0, 10, 0, 0));

            int index = i;
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                parameter.setValue(index, Double.valueOf(newValue));
                sanitiseRealParameter(parameter);
                timedParameter.initAndValidate();
                System.out.println(timedParameter);
            });

            changeTimesEntryRow.getChildren().add(textField);
        }
    }

    void updateValuesUI() {
        valuesTable.getColumns().clear();
        valuesTable.getItems().clear();

        int nTimes = timedParameter.getTimeCount();
        int nTypes = timedParameter.getNTypes();

        RealParameter valuesParameter = getValuesParam();
        TableColumn<TimedParamValuesTableEntry, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(p -> new ObservableValueBase<>() {
            @Override
            public String getValue() {
                int type = p.getValue().type;
                return type < 0
                        ? "ALL"
                        : timedParameter.typeSetInput.get().getTypeName(type);
            }
        });
        valuesTable.getColumns().add(typeCol);
        for (int i=0; i<nTimes; i++) {
            TableColumn<TimedParamValuesTableEntry, Double> col = new TableColumn<>("Epoch " + (i+1));
            int epochIdx = i;
            col.setCellValueFactory(p -> new ObservableValueBase<>() {
                @Override
                public Double getValue() {
                    int type = p.getValue().type;
                    return type<0
                            ? valuesParameter.getValue(epochIdx)
                            : valuesParameter.getValue(epochIdx*nTypes + type);
                }
            });
            col.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
            col.setOnEditCommit(e -> {
                int type = e.getTableView()
                        .getItems().get(e.getTablePosition().getRow()).type;
                if (type < 0) {
                    valuesParameter.setValue(epochIdx, e.getNewValue());
                } else {
                    valuesParameter.setValue(epochIdx*nTypes + type, e.getNewValue());
                }
                sanitiseRealParameter(valuesParameter);
                System.out.println(timedParameter);
            });
            valuesTable.getColumns().add(col);
        }

        if (valuesParameter.getDimension() / nTimes > 1) {
            for (int type=0; type<nTypes; type++)
                valuesTable.getItems().add(new TimedParamValuesTableEntry(type));
        } else {
            valuesTable.getItems().add(new TimedParamValuesTableEntry(-1));
        }
    }

    void updateGLMUI() {
        GLMPrior valuesParameter = (GLMPrior) timedParameter.valuesInput.get();
        int nTimes = timedParameter.getTimeCount();
        int nTypes = timedParameter.typeSetInput.get().getNTypes();
        int nPredictors = valuesParameter.predictorsInput.get().size();


        glmValuesTable.getColumns().clear();
        glmValuesTable.getItems().clear();

        TableColumn<ObservableList<String>, String> predCol = new TableColumn<>("Predictor");
        predCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().get(0))
        );
        glmValuesTable.getColumns().add(predCol);
        if (nTypes > 1 && !isScalar) {
            TableColumn<ObservableList<String>, String> parentCol = null;
            for (int i = 0; i < nTimes * nTypes; i++) {
                TableColumn<ObservableList<String>, String> col;
                int finalI = i + 1;
                if (i % nTypes == 0) {
                    if (i > 0)
                        glmValuesTable.getColumns().add(parentCol);
                    parentCol = new TableColumn<>("Epoch " + (i / nTypes + 1));
                    parentCol.setCellValueFactory(cellData ->
                            new SimpleStringProperty(cellData.getValue().get(finalI))
                    );
                }
                int remainder = i % nTypes;
                col = new TableColumn<>(timedParameter.typeSetInput.get().getTypeName(remainder));
                col.setCellValueFactory(cellData ->
                        new SimpleStringProperty(cellData.getValue().get(finalI))
                );
                parentCol.getColumns().add(col);


            }
            glmValuesTable.getColumns().add(parentCol);//add last parent column
        } else {
            for (int i = 0; i < nTimes; i++) {
                TableColumn<ObservableList<String>, String> col = new TableColumn<>("Epoch " + (i + 1));
                int finalI = i + 1;
                col.setCellValueFactory(cellData ->
                        new SimpleStringProperty(cellData.getValue().get(finalI))
                );
                glmValuesTable.getColumns().add(col);
            }
        }
        for (int i = 0; i < nPredictors; i++) {
            RealParameter pred = (RealParameter) valuesParameter.predictorsInput.get().get(i);
            List<String> stringList = new ArrayList<>(pred.valuesInput.get().stream()
                    .map(String::valueOf)
                    .toList());
            stringList.add(0, getPredictorName(pred));
            ObservableList<String> row = FXCollections.observableArrayList(stringList);
            glmValuesTable.getItems().add(row);
        }

    }

    String getPredictorName(RealParameter predictor) {
        return predictor.getID().split("_")[1];
    }

    void ensureValuesConsistency(boolean scalar) {
        int nTypes = timedParameter.typeSetInput.get().getNTypes();
        int nEpochs = timedParameter.timesInput.get() == null
                ? 0
                : timedParameter.timesInput.get().getDimension();

        System.out.println("Number of epochs: " + nEpochs);

        boolean isParamGLM = timedParameter.valuesInput.get() instanceof GLMPrior;



        if (nEpochs > 0) {

            if (isParamGLM) {
                for (Function p : (((GLMPrior) timedParameter.valuesInput.get()).predictorsInput.get())) {
                    // TODO actually maybe should just put an error if dimensions of table are wrong
                    if (scalar)
                        ((RealParameter) p).setDimension(nEpochs); // TODO check, put a warning and then change
                    else
                        ((RealParameter) p).setDimension(nTypes * nEpochs); // TODO check, put a warning and then change
                    sanitiseRealParameter(((RealParameter) p));
                }
                if (!((GLMPrior) timedParameter.valuesInput.get()).predictorsInput.get().isEmpty())
                    ((GLMPrior) timedParameter.valuesInput.get()).initAndValidate();

            } else {
                RealParameter valuesParam = getValuesParam();

                if (scalar)
                    valuesParam.setDimension(nEpochs);
                else
                    valuesParam.setDimension(nTypes * nEpochs);

                sanitiseRealParameter(valuesParam);
                timedParameter.valuesInput.setValue(valuesParam, timedParameter);
            }
        } else
            timedParameter.valuesInput.setValue(null, timedParameter);

        timedParameter.initAndValidate();
    }

    void sanitiseRealParameter(RealParameter parameter) {
        parameter.valuesInput.setValue(
                Arrays.stream(parameter.getDoubleValues())
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(" ")),
                parameter);
        parameter.initAndValidate();
    }

    RealParameter getTimesParam() {
        RealParameter timesParam = (RealParameter)timedParameter.timesInput.get();
        if (timesParam == null) {

            int idx = timedParameter.getID().indexOf("SP");
            String prefix = timedParameter.getID().substring(0, idx);
            String suffix = timedParameter.getID().substring(idx+2);
            String paramID = prefix + "Times" + suffix;

            timesParam = (RealParameter) doc.pluginmap.get(paramID);
            if (timesParam == null) {
                timesParam = new RealParameter("0.0");
                timesParam.setID(paramID);
            }
        }
        return timesParam;
    }

    RealParameter getValuesParam() {
        RealParameter valuesParam = (RealParameter)timedParameter.valuesInput.get();
        if (valuesParam == null) {

            int idx = timedParameter.getID().indexOf("SP");
            String prefix = timedParameter.getID().substring(0, idx);
            String suffix = timedParameter.getID().substring(idx+2);
            String paramID = prefix + suffix;

            valuesParam = (RealParameter) doc.pluginmap.get(paramID);
            if (valuesParam == null) {
                valuesParam = new RealParameter("0.0");
                valuesParam.isEstimatedInput.setValue(true, valuesParam);
                valuesParam.setID(paramID);
            }
        }
        return valuesParam;
    }

    public static class TimedParamValuesTableEntry {
        final int type;

        public TimedParamValuesTableEntry(int type) {
            this.type = type;
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

    String getTimesParameterID() {
        int idx = timedParameter.getID().indexOf("SP");
        String prefix = timedParameter.getID().substring(0, idx);
        String suffix = timedParameter.getID().substring(idx + 2);

        return prefix + "Times" + suffix;
    }

    String getGLMValuesParameterID() {
        int idx = timedParameter.getID().indexOf("SP");
        String prefix = timedParameter.getID().substring(0, idx);
        String suffix = timedParameter.getID().substring(idx + 2);

        return prefix + suffix;
    }

    String getGLMCoefficientParameterID() {
        int idx = timedParameter.getID().indexOf("SP");
        String prefix = timedParameter.getID().substring(0, idx);
        String suffix = timedParameter.getID().substring(idx + 2);

        return prefix + "Coefficients" + suffix;
    }

    String getGLMIndicatorParameterID() {
        int idx = timedParameter.getID().indexOf("SP");
        String prefix = timedParameter.getID().substring(0, idx);
        String suffix = timedParameter.getID().substring(idx + 2);

        return prefix + "Indicators" + suffix;
    }

    String getPredictorParameterID(String predName) {
        int idx = timedParameter.getID().indexOf("SP");
        String prefix = timedParameter.getID().substring(0, idx);
        String suffix = timedParameter.getID().substring(idx + 2);

        return prefix + "_" + predName + "_" + suffix;
    }

    String getGLMErrorParameterID() {
        int idx = timedParameter.getID().indexOf("SP");
        String prefix = timedParameter.getID().substring(0, idx);
        String suffix = timedParameter.getID().substring(idx + 2);

        return prefix + "Errors" + suffix;
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
            showFileErrorDialog();
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

    private void showFileErrorDialog() {
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

    int getNTimes(int nTypes, int dimension) {
        return dimension / nTypes;
    }
}
