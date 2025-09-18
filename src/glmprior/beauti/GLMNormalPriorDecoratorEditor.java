package glmprior.beauti;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.inference.Distribution;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import beastfx.app.inputeditor.*;
import feast.function.Slice;
import glmprior.util.GLMNormalDistribution;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;
import java.util.Set;

/** Editor for Prior.distr when value is GLMNormalDistribution. Renders GLM inputs + the time-series builder button. */
public class GLMNormalPriorDecoratorEditor extends ParametricDistributionInputEditor {

    public GLMNormalPriorDecoratorEditor(BeautiDoc doc) { super(doc); }

    // Some BEAST builds look for both:
    @Override public Class<?> type()  { return GLMNormalDistribution.class; }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr,
                     ExpandOption isExpandOption, boolean addButtons) {
        super.init(input, beastObject, itemNr, ExpandOption.TRUE, addButtons);

        final Prior prior = (Prior) beastObject;                       // owner of the input
        final GLMNormalDistribution glm = (GLMNormalDistribution) input.get();

        // Create a master VBox to contain both parent's content and our custom UI
        VBox masterContainer = new VBox(8);

        // --- Row 1: our time-series builder UI
        Button build = new Button("Load predictors");
        Label status = new Label();
        HBox row = new HBox(8, build, status);
        row.setPadding(new Insets(8, 0, 4, 10));
        masterContainer.getChildren().add(row);
        List<Distribution> suppressPriorsList = new ArrayList<>();


        build.setOnAction(e -> {
            try {
                FileChooser fc = new FileChooser();
                fc.setTitle("Load time-varying predictors (P × T)");
                fc.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("CSV/TSV", "*.csv", "*.tsv", "*.txt"),
                        new FileChooser.ExtensionFilter("All files", "*.*"));
                File f = fc.showOpenDialog(ownerWindow());
                if (f == null) return;

                Matrix M = parseMatrix(f); // rows=P, cols=T
                final int P = M.rows, T = M.cols;

                // --- Get 'x' (target) from the current Prior via listInputs()
                Input<RealParameter> xIn = (Input<RealParameter>)  prior.getInput("x"); //findInputByName(prior, "x");
                RealParameter x = xIn.get();

                // Ensure x has length T (without setDimension)
                if (x.getDimension() != T) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < T; i++) {
                        if (i > 0) sb.append(' ');
                        sb.append(i < x.getDimension() ? x.getValue(i) : 0.0);
                    }
                    x.valuesInput.setValue(sb.toString(), x);
                }

                // --- Shared GLM parameters from the selected distribution row
                RealParameter alpha = glm.interceptInput.get();
                RealParameter beta  = glm.coefficientsInput.get();
                RealParameter sigma = glm.sigmaInput.get();

                // Make sure beta has length P (without setDimension)
                if (beta.getDimension() != P) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < P; j++) {
                        if (j > 0) sb.append(' ');
                        sb.append(j < beta.getDimension() ? beta.getValue(j) : 0.0);
                    }
                    beta.valuesInput.setValue(sb.toString(), beta);
                }

                // --- Find the *actual* priors list that contains this row, by scanning outputs
                Input<List<Distribution>> priorsListInput =
                        findDistributionListInputFor(prior);

                List<Distribution> priorsList = priorsListInput.get(); // mutate in place
                int baseIdx = Math.max(0, priorsList.indexOf(prior));

                // ===== t = 0 : REUSE the existing row =====
                // predictors X_0 (extract column 0 from P × T matrix)
                RealParameter X0 = new RealParameter();
                X0.setID(x.getID() + ".X_t0");
                X0.isEstimatedInput.setValue(false, X0);
                X0.valuesInput.setValue(joinDoubles(extractColumn(M, 0)), X0);
                doc.registerPlugin(X0);

                // GLM_0 (shares alpha, beta, sigma)
                glmprior.util.GLMNormalDistribution glm0 = new glmprior.util.GLMNormalDistribution();
                glm0.setID("glmNormal_t0");
                glm0.interceptInput.setValue(alpha, glm0);
                glm0.coefficientsInput.setValue(beta, glm0);
                glm0.predictorsInput.setValue(X0, glm0);
                glm0.sigmaInput.setValue(sigma, glm0);
                doc.registerPlugin(glm0);

                // Slice y[0]
                Slice s0 = new Slice();
                s0.setID("slice_" + x.getID() + "_0");

                s0.functionInput.setValue(x, s0);
                s0.startIndexInput.setValue(0, s0);
                s0.countInput.setValue(1, s0);
                doc.registerPlugin(s0);

                // Update existing Prior row’s x & distr (no getInput(), use listInputs)
                prior.m_x.setValue(s0, prior);
                prior.distInput.setValue(glm, prior);

                // ===== t = 1..T-1 : create & INSERT new rows after the reused row =====
                for (int t = 1; t < T; t++) {
                    // predictors X_t (fixed)
                    RealParameter Xt = new RealParameter();
                    Xt.setID(x.getID() + ".X_t" + t);
                    Xt.isEstimatedInput.setValue(false, Xt);
                    Xt.valuesInput.setValue(joinDoubles(extractColumn(M, t)), Xt);
                    doc.registerPlugin(Xt);

                    // GLM_t
                    glmprior.util.GLMNormalDistribution glmt = new glmprior.util.GLMNormalDistribution();
                    glmt.setID("glmNormal_t" + t);
                    glmt.interceptInput.setValue(alpha, glmt);
                    glmt.coefficientsInput.setValue(beta, glmt);
                    glmt.predictorsInput.setValue(Xt, glmt);
                    glmt.sigmaInput.setValue(sigma, glmt);
                    doc.registerPlugin(glmt);

                    // Slice y[t]
                    Slice st = new Slice();
                    st.setID("slice_" + x.getID() + "_" + t);
                    st.functionInput.setValue(x, st);
                    st.startIndexInput.setValue(t, st);
                    st.countInput.setValue(1, st);
                    doc.registerPlugin(st);

                    // New Prior row p(y[t] | GLM_t)
                    Prior pr = new Prior();
                    pr.setID("prior_glm_t" + t);
                    pr.m_x.setValue(st, pr);
                    pr.distInput.setValue(glmt, pr);
                    doc.registerPlugin(pr);


                    // Insert directly into the SAME list the panel uses
                    priorsList.add(baseIdx + t, pr);
                    suppressPriorsList.add(pr);
                    suppressRow(doc, pr);        // but hide it in BEAUti

                }


                this.refreshPanel();

//                // Nudge BEAUti to refresh, if needed
//                try {
//                    doc.fireDocHasChanged();
//                } catch (Throwable ignore) {
//                    // nothing to do
//                }

                status.setText("Built " + T + " priors (P=" + P + ").");

            } catch (Exception ex) {
                status.setText("Failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        });



        // Call this after super.init() but before moving children to master container
//        CheckBox coefficientsEstimateCheckbox = findEstimateCheckboxForParameter("coefficients");
//        if (coefficientsEstimateCheckbox != null) {
//            // You can now control the checkbox
//            coefficientsEstimateCheckbox.setSelected(true);
//        }



        // --- Rows 2+: render the **standard editors** for GLM inputs so users can set priors etc.
        // Order: intercept, coefficients, sigma, predictors (predictors shown read-only if you like)
//        masterContainer.getChildren().add(new javafx.scene.control.Separator());
//        masterContainer.getChildren().add(makeEditorNode(glm.getInput("intercept"), glm));
//        masterContainer.getChildren().add(makeEditorNode(glm.getInput("coefficients"), glm));
//        masterContainer.getChildren().add(makeEditorNode(glm.getInput("sigma"), glm));
//        masterContainer.getChildren().add(makeEditorNode(glm.getInput("predictors"), glm)); // optional: users may still inspect/override

        // Move all existing children (from parent) to the master container
        masterContainer.getChildren().addAll(getChildren());
        getChildren().clear();

        getChildren().add(masterContainer);
        for (Distribution pr : suppressPriorsList){
            suppressRow(doc, pr);        // but hide it in BEAUti
        }
        this.refreshPanel();

    }


    /** Find an input on a BEAST object by its name (works across BEAST 2 builds). */
    private static Input<?> findInputByName(BEASTInterface obj, String name) {
        for (Input<?> in : obj.listInputs()) {
            if (name.equals(in.getName())) return in;
        }
        throw new IllegalArgumentException("Input '" + name + "' not found on " + obj.getClass().getSimpleName() +
                (obj.getID() != null ? " id=" + obj.getID() : ""));
    }

    /** Locate the *list* input that contains the current Prior among its elements. */
    @SuppressWarnings("unchecked")
    private static Input<List<Distribution>> findDistributionListInputFor(Prior prior) {
        // Each output is a BEASTInterface (e.g. a CompoundDistribution)
        for (BEASTInterface outObj : prior.getOutputs()) {
            // look through that object's inputs
            for (Input<?> in : outObj.listInputs()) {
                Object val = in.get();
                if (val instanceof List<?> L) {
                    if (!L.isEmpty() && L.get(0) instanceof Distribution) {
                        if (L.contains(prior) && "distribution".equals(in.getName())) {
                            return (Input<List<Distribution>>) in;
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("Could not find parent distribution list for prior " + prior.getID());
    }

    /** Extract column t from P × T matrix (returns array of length P). */
    private static double[] extractColumn(Matrix matrix, int colIndex) {
        double[] column = new double[matrix.rows];
        for (int p = 0; p < matrix.rows; p++) {
            column[p] = matrix.vals[p][colIndex];
        }
        return column;
    }

    /** Space-delimited string for RealParameter.valuesInput. */
    private static String joinDoubles(double[] row) {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < row.length; j++) {
            if (j > 0) sb.append(' ');
            sb.append(row[j]);
        }
        return sb.toString();
    }


    @SuppressWarnings({"rawtypes","unchecked"})
    private static void suppressRow(BeautiDoc doc, BEASTInterface node) {
        // Try: doc.suppressInput(BEASTInterface)
        try {
            Method m = doc.getClass().getMethod("suppressInput", BEASTInterface.class);
            m.invoke(doc, node);
            return;
        } catch (Throwable ignore) {}

        // Try: doc.suppressInputByID(String)
        try {
            Method m = doc.getClass().getMethod("suppressInputByID", String.class);
            m.invoke(doc, node.getID());
            return;
        } catch (Throwable ignore) {}

        // Try: doc.getSuppressedInputs() -> Set<BEASTInterface>
        try {
            Method m = doc.getClass().getMethod("getSuppressedInputs");
            Object set = m.invoke(doc);
            if (set instanceof Set) {
                ((Set) set).add(node);
                return;
            }
        } catch (Throwable ignore) {}

        // Try: doc.getSuppressedInputIDs() -> Set<String>
        try {
            Method m = doc.getClass().getMethod("getSuppressedInputIDs");
            Object set = m.invoke(doc);
            if (set instanceof Set) {
                ((Set) set).add(node.getID());
            }
        } catch (Throwable ignore) {}



        // If none of the above exist on your build, you can later fall back to the “put them under posterior” approach.
    }



    private Node makeEditorNode(Input<?> childInput, BEASTInterface owner) {
        try {
            InputEditorFactory factory = doc.getInputEditorFactory();
            InputEditor ed = factory.createInputEditor(childInput, owner, doc);
            return (Node) ed; // InputEditor is a Node in FX
        } catch (Exception e) {
            // fall back: show a tiny error label instead of crashing the panel
            return new Label("⛔ " + childInput.getName() + ": " + e.getMessage());
        }
    }

    private Window ownerWindow() { return getScene() != null ? getScene().getWindow() : null; }

    private static Matrix parseMatrix(File f) throws Exception {
        List<double[]> rows = new ArrayList<>();
        boolean hasPredictorNames = false;
        
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String sep = null;
            String line;
            
            // Read all lines and determine separator
            List<String> allLines = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    allLines.add(line.trim());
                    if (sep == null) {
                        sep = line.contains("\t") ? "\t" : ",";
                    }
                }
            }
            
            if (allLines.isEmpty()) throw new IllegalArgumentException("Empty file");
            
            // Find first data row (skip header rows that are all text)
            int dataStartRow = -1;
            for (int i = 0; i < allLines.size(); i++) {
                String[] tok = allLines.get(i).split(sep);
                boolean hasNumericData = false;
                
                // Check if this row has any numeric data
                for (String cell : tok) {
                    try {
                        Double.parseDouble(cell.trim());
                        hasNumericData = true;
                        break;
                    } catch (NumberFormatException e) {
                        // Continue checking other cells
                    }
                }
                
                if (hasNumericData) {
                    dataStartRow = i;
                    break;
                }
            }
            
            if (dataStartRow == -1) {
                throw new IllegalArgumentException("No numeric data found in file");
            }
            
            // Process data rows starting from dataStartRow
            for (int i = dataStartRow; i < allLines.size(); i++) {
                String[] tok = allLines.get(i).split(sep);
                
                // Check if first column contains predictor names (text) - only on first data row
                if (rows.isEmpty()) {
                    try {
                        Double.parseDouble(tok[0].trim());
                        hasPredictorNames = false; // First column is numeric
                    } catch (NumberFormatException e) {
                        hasPredictorNames = true; // First column is text (predictor name)
                    }
                }
                
                int off = hasPredictorNames ? 1 : 0; // Skip predictor name column if present
                
                double[] r = new double[tok.length - off];
                for (int j = off; j < tok.length; j++) {
                    r[j - off] = Double.parseDouble(tok[j].trim());
                }
                rows.add(r);
            }
        }
        
        if (rows.isEmpty()) throw new IllegalArgumentException("No data rows");
        int T = rows.get(0).length; // T = number of time points (columns after skipping name column)
        double[][] vals = new double[rows.size()][T];
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).length != T) throw new IllegalArgumentException("Inconsistent column count at row " + i);
            vals[i] = rows.get(i);
        }
        return new Matrix(rows.size(), T, vals);
    }
    private static class Matrix { final int rows, cols; final double[][] vals; Matrix(int r,int c,double[][]v){rows=r;cols=c;vals=v;} }

    private CheckBox findEstimateCheckboxForParameter(String parameterName) {
        return findEstimateCheckboxRecursive(this, parameterName);
    }

    private CheckBox findEstimateCheckboxRecursive(Pane parentPane, String parameterName) {
        for (javafx.scene.Node child : parentPane.getChildren()) {
            // Check if this is a ParameterInputEditor
            if (child.getClass().getSimpleName().equals("ParameterInputEditor")) {
                // Check if this editor has the correct parameter label
                if (hasParameterLabel(child, parameterName)) {
                    // Found the right parameter editor, now find its estimate checkbox
                    return findCheckboxInParameterEditor(child);
                }
            }
            // If it's a Pane, recurse into it
            else if (child instanceof Pane) {
                CheckBox checkbox = findEstimateCheckboxRecursive((Pane) child, parameterName);
                if (checkbox != null) return checkbox;
            }
        }
        return null;
    }

    private boolean hasParameterLabel(Node parameterEditor, String parameterName) {
        // Look for a Label child that contains the parameter name
        if (parameterEditor instanceof Pane) {
            return findLabelRecursive((Pane) parameterEditor, parameterName);
        }
        return false;
    }

    private boolean findLabelRecursive(Pane pane, String parameterName) {
        for (javafx.scene.Node child : pane.getChildren()) {
            if (child instanceof Label) {
                Label label = (Label) child;
                String labelText = label.getText();
                if (labelText != null &&
                        (labelText.equalsIgnoreCase(parameterName) ||
                                labelText.toLowerCase().contains(parameterName.toLowerCase()))) {
                    return true;
                }
            } else if (child instanceof Pane) {
                if (findLabelRecursive((Pane) child, parameterName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private CheckBox findCheckboxInParameterEditor(Node parameterEditor) {
        if (parameterEditor instanceof Pane) {
            return findCheckboxRecursive((Pane) parameterEditor);
        }
        return null;
    }

    private CheckBox findCheckboxRecursive(Pane pane) {
        for (javafx.scene.Node child : pane.getChildren()) {
            if (child instanceof CheckBox) {
                CheckBox cb = (CheckBox) child;
                // Check if this checkbox has "estimate" text or is near an "estimate" label
                if (cb.getText() != null && cb.getText().toLowerCase().contains("estimate")) {
                    return cb;
                }
                // Also check if there's an "estimate" label nearby (common pattern in BEAUti)
                if (hasEstimateLabelNearby(pane, cb)) {
                    return cb;
                }
            } else if (child instanceof Pane) {
                CheckBox checkbox = findCheckboxRecursive((Pane) child);
                if (checkbox != null) return checkbox;
            }
        }
        return null;
    }

    private boolean hasEstimateLabelNearby(Pane container, CheckBox checkbox) {
        // Look for "estimate" label in the same container as the checkbox
        for (javafx.scene.Node sibling : container.getChildren()) {
            if (sibling instanceof Label) {
                Label label = (Label) sibling;
                if (label.getText() != null &&
                        label.getText().toLowerCase().contains("estimate")) {
                    return true;
                }
            }
        }
        return false;
    }

}
