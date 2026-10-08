package glmprior.util;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.parameter.BooleanParameter;

import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A BooleanParameter that has a specified dimension with only one index set to true,
 * all other indices are false.
 *
 * This is useful for testing or initialization purposes where you want to activate
 * only a single predictor in a GLM model.
 *
 */
@Description("Boolean parameter with specified dimension where only one specified index is true")
public class SingleIndexBooleanParameter extends BooleanParameter {

    public Input<Integer> trueIndexInput = new Input<>(
            "trueIndex",
            "Index that should be set to true (0-based, all others will be false)",
            Input.Validate.REQUIRED);

    public SingleIndexBooleanParameter() {
        super();
    }

    @Override
    public void initAndValidate() {
        super.initAndValidate();

        int dimension = dimensionInput.get();
        int trueIndex = trueIndexInput.get();

        // Validate inputs
        if (dimension <= 0) {
            throw new IllegalArgumentException("Dimension must be positive, got: " + dimension);
        }

        if (trueIndex < 0 || trueIndex >= dimension) {
            throw new IllegalArgumentException(
                    "trueIndex must be in range [0, " + (dimension - 1) + "], got: " + trueIndex);
        }

        // Create array with all false except at trueIndex
        Boolean[] values = new Boolean[dimension];
        for (int i = 0; i < dimension; i++) {
            values[i] = (i == trueIndex);
        }

        // Initialize the parameter with these values
        this.values = values;
        this.m_fUpper = true;
        this.m_fLower = false;
    }

    /**
     * Gets the dimension of this parameter.
     */
    @Override
    public int getDimension() {
        if (dimensionInput.get() != null) {
            return dimensionInput.get();
        }
        return super.getDimension();
    }

    /**
     * Gets the index that is set to true.
     */
    public int getTrueIndex() {
        return trueIndexInput.get();
    }

    /**
     * Returns string representation compatible with fromXML parsing.
     * Format: ID[dimension trueIndex] (lower,upper): value1 value2 ...
     *
     * Note: This extends the standard BooleanParameter format to include trueIndex
     * so that state can be fully restored.
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(getID()).append("[").append(values.length);
        buf.append(" ").append(getTrueIndex());  // Store trueIndex in the minorDimension slot
        buf.append("] ");
        buf.append("(").append(m_fLower).append(",").append(m_fUpper).append("): ");
        for (final Boolean value : values) {
            buf.append(value).append(" ");
        }
        return buf.toString();
    }

    /**
     * Restores parameter state from XML node.
     * Parses the format produced by toString():
     * ID[dimension trueIndex] (lower,upper): value1 value2 ...
     *
     * Also supports legacy format for backward compatibility:
     * SingleIndexBooleanParameter[dimension=X, trueIndex=Y]
     */
    @Override
    public void fromXML(final Node node) {
        final NamedNodeMap atts = node.getAttributes();
        setID(atts.getNamedItem("id").getNodeValue());
        final String str = node.getTextContent();

        // Pattern 1: New format with values - ID[dimension trueIndex] (lower,upper): values
        Pattern pattern = Pattern.compile(".*\\[(\\d+)\\s+(\\d+)].*\\(([^,]+),([^)]+)\\):\\s*(.*)");
        Matcher matcher = pattern.matcher(str);

        if (matcher.matches()) {
            final int dimension = Integer.parseInt(matcher.group(1));
            final int storedTrueIndex = Integer.parseInt(matcher.group(2));
            final String lower = matcher.group(3);
            final String upper = matcher.group(4);
            final String valuesAsString = matcher.group(5).trim();
            final String[] valueStrings = valuesAsString.split("\\s+");

            // Update trueIndex input if it differs
            if (trueIndexInput.get() != storedTrueIndex) {
                trueIndexInput.setValue(storedTrueIndex, this);
            }

            fromXML(dimension, lower, upper, valueStrings);
            return;
        }

        // Pattern 2: Legacy format - SingleIndexBooleanParameter[dimension=X, trueIndex=Y]
        pattern = Pattern.compile(".*\\[dimension=(\\d+),\\s*trueIndex=(\\d+)].*");
        matcher = pattern.matcher(str);

        if (matcher.matches()) {
            final int dimension = Integer.parseInt(matcher.group(1));
            final int storedTrueIndex = Integer.parseInt(matcher.group(2));

            // Update trueIndex input if it differs
            if (trueIndexInput.get() != storedTrueIndex) {
                trueIndexInput.setValue(storedTrueIndex, this);
            }

            // Reconstruct values from dimension and trueIndex
            values = new Boolean[dimension];
            for (int i = 0; i < dimension; i++) {
                values[i] = (i == storedTrueIndex);
            }
            m_fLower = false;
            m_fUpper = true;
            return;
        }

        // Pattern 3: Standard BooleanParameter format - ID[dimension] (lower,upper): values
        pattern = Pattern.compile(".*\\[(\\d+)].*\\(([^,]+),([^)]+)\\):\\s*(.*)");
        matcher = pattern.matcher(str);

        if (matcher.matches()) {
            final int dimension = Integer.parseInt(matcher.group(1));
            final String lower = matcher.group(2);
            final String upper = matcher.group(3);
            final String valuesAsString = matcher.group(4).trim();
            final String[] valueStrings = valuesAsString.split("\\s+");

            fromXML(dimension, lower, upper, valueStrings);
            return;
        }

        throw new RuntimeException("SingleIndexBooleanParameter could not be parsed from: " + str);
    }

    /**
     * Restore values from parsed XML components.
     * Note: This shadows the package-private method in BooleanParameter.
     */
    private void fromXML(int dimension, String lower, String upper, String[] valueStrings) {
        values = new Boolean[dimension];
        for (int i = 0; i < valueStrings.length; i++) {
            values[i] = Boolean.parseBoolean(valueStrings[i]);
        }
        m_fLower = Boolean.parseBoolean(lower);
        m_fUpper = Boolean.parseBoolean(upper);
    }
}