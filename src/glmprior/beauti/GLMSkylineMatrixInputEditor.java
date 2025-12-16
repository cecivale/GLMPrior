package glmprior.beauti;

import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;
import beastfx.app.inputeditor.BeautiDoc;
import glmprior.parameterization.GLMSkylineMatrixParameter;
import glmprior.util.GLMPrior;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import javafx.util.converter.DoubleStringConverter;

import java.util.ArrayList;
import java.util.List;

public class GLMSkylineMatrixInputEditor extends GLMSkylineInputEditor {

    GLMSkylineMatrixParameter skylineMatrix;


    public GLMSkylineMatrixInputEditor(BeautiDoc doc) {
        super(doc);
    }

    @Override
    public Class<?> type() {
        return GLMSkylineMatrixParameter.class;
    }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr,
                     ExpandOption isExpandOption, boolean addButtons) {

        skylineMatrix = (GLMSkylineMatrixParameter) input.get();
        isGLM = skylineMatrix.isGLMInput.get();
        super.init(input, beastObject, itemNr, isExpandOption, addButtons);


        skylineMatrix.initAndValidate();

        if (skylineMatrix.getNTypes() == 1) {
            mainInputBox.setVisible(false);
            mainInputBox.setManaged(false);
            pane.getChildren().add(new Label("Insufficient types for this parameter."));
        } else {
            if (!skylineMatrix.isGLMInput.get())
                updateValuesUI();
            else
                updateGLMUI();

            glmValuesTable.setFixedCellSize(25);
            glmValuesTable.prefHeightProperty().bind(glmValuesTable.fixedCellSizeProperty()
                    .multiply(Bindings.size(glmValuesTable.getItems()).add(5.0)));

            valuesTable.setFixedCellSize(25);
            valuesTable.prefHeightProperty().bind(valuesTable.fixedCellSizeProperty()
                    .multiply(Bindings.size(valuesTable.getItems()).add(3.0)));
        }

    }

    @Override
    void ensureValuesConsistency() {
        int nTypes = skylineParameter.typeSetInput.get().getNTypes();
        int nEpochs = skylineParameter.changeTimesInput.get() == null
                ? 1
                : skylineParameter.changeTimesInput.get().getDimension() + 1;
        // TODO here is an error when going back and forth between GLM and normal param
        boolean b = skylineParameter.skylineValuesInput.get() instanceof GLMPrior;

        if (b) {
            for (Function p : (((GLMPrior) skylineParameter.skylineValuesInput.get()).predictorsInput.get())) {
                // TODO actually maybe should just put an error if dimensions of table are wrong
                if (skylineParameter.isScalarInput.get())
                    ((RealParameter) p).setDimension(nEpochs); // TODO check, put a warning and then change
                else
                    ((RealParameter) p).setDimension(nTypes * (nTypes - 1) * nEpochs); // TODO check, put a warning and then change
                sanitiseRealParameter(((RealParameter) p));
            }
            if (!((GLMPrior) skylineParameter.skylineValuesInput.get()).predictorsInput.get().isEmpty())
                ((GLMPrior) skylineParameter.skylineValuesInput.get()).initAndValidate();

        } else {

            RealParameter valuesParam = (RealParameter) skylineParameter.skylineValuesInput.get();
            if (skylineParameter.isScalarInput.get())
                valuesParam.setDimension(nEpochs);
            else
                valuesParam.setDimension(nTypes * (nTypes - 1) * nEpochs);
            sanitiseRealParameter(valuesParam);
            ((RealParameter) skylineParameter.skylineValuesInput.get()).initAndValidate();
        }

        if (skylineParameter.changeTimesInput.get() != null)
            ((RealParameter) skylineParameter.changeTimesInput.get()).initAndValidate();
//        if (b && (((GLMLogLinear) skylineParameter.skylineValuesInput.get()).predictorsInput.get()) != null)
//            ((RealParameter)((GLMLogLinear) skylineParameter.skylineValuesInput.get()).predictorsInput.get()).initAndValidate();
        skylineParameter.initAndValidate();
    }

    @Override
    void isGLM(boolean isGLM) {
        skylineMatrix.isGLMInput.set(isGLM);
    }

    @Override
    int getnChanges(int nTypes, int dimension) {
        return dimension / (nTypes * (nTypes - 1)) -1;
    }

    void updateGLMUI() {
        GLMPrior valuesParameter = (GLMPrior) skylineMatrix.skylineValuesInput.get();
        int nChanges = skylineParameter.changeTimesInput.get() == null
                ? 0
                : skylineParameter.changeTimesInput.get().getDimension();
        int nTypes = skylineParameter.typeSetInput.get().getNTypes();
        int nPredictors = valuesParameter.predictorsInput.get().size();


        glmValuesTable.getColumns().clear();
        glmValuesTable.getItems().clear();
        int colCount = 0;

        TableColumn<ObservableList<String>, String> predCol = new TableColumn<>("Predictor");
        int finalColCount1 = colCount;
        predCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().get(finalColCount1))
        );
        glmValuesTable.getColumns().add(predCol);
        colCount++;

        if (nTypes > 1 && !skylineParameter.isScalarInput.get()) {
            TableColumn<ObservableList<String>, String> parentCol = null;
            TableColumn<ObservableList<String>, String> fromCol = null;
            int fromID = 0;
            for (int intervals=0; intervals<(nChanges+1); intervals++){
                parentCol = new TableColumn<>("Epoch " + (intervals + 1));
                for (int from=0; from<nTypes; from++){
                    fromCol = new TableColumn<>("From " + skylineParameter.typeSetInput.get().getTypeName(from));
                    for (int to=0; to<nTypes; to++){
                        if (from==to)
                            continue;
                        TableColumn<ObservableList<String>, String> toCol = new TableColumn<>("To " + skylineParameter.typeSetInput.get().getTypeName(to));
                        int finalColCount2 = colCount;
                        toCol.setCellValueFactory(cellData ->
                                new SimpleStringProperty(cellData.getValue().get(finalColCount2))
                        );
                        colCount++;
                        fromCol.getColumns().add(toCol);
                    }
                    parentCol.getColumns().add(fromCol);
                }
                glmValuesTable.getColumns().add(parentCol);
            }


//            for (int k=0; k<(nChanges+1)*(nTypes)*(nTypes);k++){
//
//                int finalI = k + 1;
//                if (k % ((nTypes)*(nTypes)) == 0) {
//                    if (k > 0){
//                        glmValuesTable.getColumns().add(parentCol);
//                    }
//                    parentCol = new TableColumn<>("Epoch " + (k / ((nTypes)*(nTypes)) + 1));
////                    int finalColCount = colCount;
////                    parentCol.setCellValueFactory(cellData ->
////                            new SimpleStringProperty(cellData.getValue().get(finalColCount))
////                    );
//                }
//
//                if (k % ((nChanges+1)*nTypes) == 0) {
//                    from = new TableColumn<>("From " + skylineParameter.typeSetInput.get().getTypeName(k/((nChanges+1)*nTypes)));
////                    int finalColCount2 = colCount;
////                    from.setCellValueFactory(cellData ->
////                            new SimpleStringProperty(cellData.getValue().get(finalColCount2))
////                    );
//                    fromID = k/((nChanges+1)*nTypes);
//
//                }
//
//                if (k%nTypes !=fromID) {
//                    TableColumn<ObservableList<String>, String> to = new TableColumn<>("To " + skylineParameter.typeSetInput.get().getTypeName(k%nTypes));
//                    int finalColCount3 = colCount;
//                    to.setCellValueFactory(cellData ->
//                            new SimpleStringProperty(cellData.getValue().get(finalColCount3))
//                    );
//                    colCount++;
//
//                    from.getColumns().add(to);
//                }
//
//                if (k % ((nTypes)*(nTypes)) != 0 && (k+1) % nTypes == 0) {
//                    parentCol.getColumns().add(from);
//                }
//
//
//            }
//            glmValuesTable.getColumns().add(parentCol);//add last parent column
        } else {

            for (int i = 0; i < nChanges + 1; i++) {
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

    @Override
    void updateValuesUI() {
        valuesTable.getColumns().clear();
        valuesTable.getItems().clear();

        int nChanges = skylineMatrix.getChangeCount();
        int nTypes = skylineMatrix.getNTypes();

        RealParameter valuesParameter = (RealParameter) skylineMatrix.skylineValuesInput.get();

        TableColumn<ValuesTableEntry, String> typeCol = new TableColumn<>("From Type");
        typeCol.setCellValueFactory(p -> new ObservableValueBase<>() {
            @Override
            public String getValue() {
                int type = ((MatrixValuesEntry) p.getValue()).fromType;
                return type < 0
                        ? "ALL"
                        : skylineMatrix.typeSetInput.get().getTypeName(type);
            }
        });
        valuesTable.getColumns().add(typeCol);

        List<Integer> toTypes = new ArrayList<>();
        if (skylineParameter.isScalarInput.get())
            toTypes.add(-1);
        else {
            for (int t=0; t<nTypes; t++) {
                toTypes.add(t);
            }
        }

        for (int i=0; i<nChanges+1; i++) {
            TableColumn<ValuesTableEntry, Object> epochCol = new TableColumn<>("Epoch " + (i + 1));
            valuesTable.getColumns().add(epochCol);

            int epochIdx = i;

            for (int toType : toTypes) {
                TableColumn<ValuesTableEntry, Double> col = toType == -1
                        ? new TableColumn<>("to ALL")
                        : new TableColumn<>("to " + skylineMatrix.typeSetInput.get().getTypeName(toType));

                col.setCellValueFactory(p -> new ObservableValueBase<>() {
                    @Override
                    public Double getValue() {
                        int from = ((MatrixValuesEntry) p.getValue()).fromType;
                        if (from < 0)
                            return valuesParameter.getValue(epochIdx);

                        if (from == toType)
                            return Double.NaN;

                        return valuesParameter.getValue(
                                epochIdx*nTypes*(nTypes-1)
                                        + (nTypes-1)*from
                                        + (toType<from ? toType : toType-1));
                    }
                });
//                col.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
                col.setCellFactory(new Callback<>() {
                    @Override
                    public TableCell<ValuesTableEntry, Double> call(TableColumn<ValuesTableEntry, Double> param) {
                        return new TextFieldTableCell<>(new DoubleStringConverter()) {
                            @Override
                            public void updateItem(Double item, boolean empty) {
                                super.updateItem(item, empty);

                                if (item == null || empty) {
                                    setText("");
                                    setStyle("");
                                    setEditable(false);
                                } else if (Double.isNaN(item)) {
                                    setText("");
                                    setStyle("-fx-background-color: black");
                                    setEditable(false);
                                } else {
                                    setText(Double.toString(item));
                                    setEditable(true);
                                }
                            }
                        };
                    }
                });
                col.setOnEditCommit(e -> {
                    int from = ((MatrixValuesEntry) e.getTableView()
                            .getItems().get(e.getTablePosition().getRow())).fromType;
                    if (from < 0) {
                        valuesParameter.setValue(epochIdx, e.getNewValue());
                    } else {
                        valuesParameter.setValue(epochIdx*nTypes*(nTypes-1)
                                + (nTypes-1)*from
                                + (toType<from ? toType : toType-1), e.getNewValue());
                    }
                    ensureValuesConsistency();
                    sanitiseRealParameter(valuesParameter);
                });
                epochCol.getColumns().add(col);
            }
        }

        if (!skylineParameter.isScalarInput.get()) {
            for (int from=0; from<nTypes; from++) {
                valuesTable.getItems().add(new MatrixValuesEntry(from));
            }
        } else {
            valuesTable.getItems().add(new MatrixValuesEntry(-1));
        }
    }

    public static class MatrixValuesEntry extends ValuesTableEntry {
        public final int fromType;

        public MatrixValuesEntry(int fromType) {
            this.fromType = fromType;
        }
    }

    private void mergeRows(TableColumn<ObservableList<String>, String> column, int columnIndex, int mergeInterval) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: center; -fx-font-weight: bold;");

                    int rowIndex = getIndex();
                    if (rowIndex > 0 && rowIndex % mergeInterval != 0) { // Merge every N rows
                        String prevItem = getTableView().getItems().get(rowIndex - 1).get(columnIndex);
                        if (item.equals(prevItem)) {
                            setText(""); // Hide duplicate values to simulate merging
                        }
                    }
                }
            }
        });
    }

}
