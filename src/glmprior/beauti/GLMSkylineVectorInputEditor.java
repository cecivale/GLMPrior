package glmprior.beauti;

import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;
import beastfx.app.inputeditor.BeautiDoc;
import glmprior.parameterization.GLMSkylineVectorParameter;
import glmprior.util.GLMPrior;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DoubleStringConverter;

import java.util.ArrayList;
import java.util.List;

public class GLMSkylineVectorInputEditor extends GLMSkylineInputEditor {

    GLMSkylineVectorParameter skylineVector;


    public GLMSkylineVectorInputEditor(BeautiDoc doc) {
        super(doc);
    }

    @Override
    public Class<?> type() {
        return GLMSkylineVectorParameter.class;
    }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr,
                     ExpandOption isExpandOption, boolean addButtons) {

        skylineVector = (GLMSkylineVectorParameter) input.get();
        isGLM = skylineVector.isGLMInput.get();
        super.init(input, beastObject, itemNr, isExpandOption, addButtons);


        skylineVector.initAndValidate();

        if (!skylineVector.isGLMInput.get())
            updateValuesUI();
        else
            updateGLMUI();

        glmValuesTable.setFixedCellSize(25);
        glmValuesTable.prefHeightProperty().bind(glmValuesTable.fixedCellSizeProperty()
                .multiply(Bindings.size(glmValuesTable.getItems()).add(1.1)));

        valuesTable.setFixedCellSize(25);
        valuesTable.prefHeightProperty().bind(valuesTable.fixedCellSizeProperty()
                .multiply(Bindings.size(valuesTable.getItems()).add(1.1)));

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
                    ((RealParameter) p).setDimension(nTypes * nEpochs); // TODO check, put a warning and then change
                sanitiseRealParameter(((RealParameter) p));
            }
            if (!((GLMPrior) skylineParameter.skylineValuesInput.get()).predictorsInput.get().isEmpty())
                ((GLMPrior) skylineParameter.skylineValuesInput.get()).initAndValidate();

        } else {

            RealParameter valuesParam = (RealParameter) skylineParameter.skylineValuesInput.get();
            if (skylineParameter.isScalarInput.get())
                valuesParam.setDimension(nEpochs);
            else
                valuesParam.setDimension(nTypes * nEpochs);
            sanitiseRealParameter(valuesParam);
        }

        if (skylineParameter.changeTimesInput.get() != null)
            ((RealParameter) skylineParameter.changeTimesInput.get()).initAndValidate();
        skylineParameter.initAndValidate();
    }

    @Override
    void isGLM(boolean isGLM) {
        skylineVector.isGLMInput.set(isGLM);
    }

    @Override
    int getnChanges(int nTypes, int dimension) {
        return dimension / nTypes - 1;
    }

    @Override



    void updateGLMUI() {
        GLMPrior valuesParameter = (GLMPrior) skylineVector.skylineValuesInput.get();
        int nChanges = skylineVector.changeTimesInput.get().getDimension();
        if ( skylineVector.changeTimesInput.get() ==null){
            return;
        }
        int nTypes = skylineVector.typeSetInput.get().getNTypes();
        int nPredictors = valuesParameter.predictorsInput.get().size();


        glmValuesTable.getColumns().clear();
        glmValuesTable.getItems().clear();

        TableColumn<ObservableList<String>, String> predCol = new TableColumn<>("Predictor");
        predCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().get(0))
        );
        glmValuesTable.getColumns().add(predCol);
        if (nTypes > 1 && !skylineParameter.isScalarInput.get()) {
            TableColumn<ObservableList<String>, String> parentCol = null;
            for (int i = 0; i < (nChanges + 1) * nTypes; i++) {
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
                col = new TableColumn<>(skylineParameter.typeSetInput.get().getTypeName(remainder));
                col.setCellValueFactory(cellData ->
                        new SimpleStringProperty(cellData.getValue().get(finalI))
                );
                parentCol.getColumns().add(col);


            }
            glmValuesTable.getColumns().add(parentCol);//add last parent column

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

        int nChanges = skylineVector.getChangeCount();
        int nTypes = skylineVector.getNTypes();

        RealParameter valuesParameter = (RealParameter) skylineVector.skylineValuesInput.get();
        TableColumn<ValuesTableEntry, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(p -> new ObservableValueBase<>() {
            @Override
            public String getValue() {
                int type = ((VectorValuesEntry) p.getValue()).type;
                return type < 0
                        ? "ALL"
                        : skylineVector.typeSetInput.get().getTypeName(type);
            }
        });
        valuesTable.getColumns().add(typeCol);
        for (int i = 0; i < nChanges + 1; i++) {
            TableColumn<ValuesTableEntry, Double> col = new TableColumn<>("Epoch " + (i + 1));
            int epochIdx = i;
            col.setCellValueFactory(p -> new ObservableValueBase<>() {
                @Override
                public Double getValue() {
                    int type = ((VectorValuesEntry) p.getValue()).type;
                    return type < 0
                            ? valuesParameter.getValue(epochIdx)
                            : valuesParameter.getValue(epochIdx * nTypes + type);
                }
            });
            col.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
            col.setOnEditCommit(e -> {
                int type = ((VectorValuesEntry) e.getTableView()
                        .getItems().get(e.getTablePosition().getRow())).type;
                if (type < 0) {
                    valuesParameter.setValue(epochIdx, e.getNewValue());
                } else {
                    valuesParameter.setValue(epochIdx * nTypes + type, e.getNewValue());
                }
                sanitiseRealParameter(valuesParameter);
            });
            valuesTable.getColumns().add(col);
        }

        if (valuesParameter.getDimension() / (nChanges + 1) > 1) {
            for (int type = 0; type < nTypes; type++)
                valuesTable.getItems().add(new VectorValuesEntry(type));
        } else {
            valuesTable.getItems().add(new VectorValuesEntry(-1));
        }
    }

    public static class VectorValuesEntry extends ValuesTableEntry {
        public final int type;

        public VectorValuesEntry(int type) {
            this.type = type;
        }
    }
}
