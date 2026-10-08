package glmprior.operator;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Operator;
import beast.base.core.Input.Validate;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.util.Randomizer;

/**
 * From beast-classic, classes from BEAST
 *    f@author dkuh004
 *         Date: Sep 20, 2011
 *         Time: 12:25:39 PM
 */
@Description("Flip one bit in an array of boolean bits and scale the BSSVS parameter accordingly. The hastings ratio is designed so that all subsets of vectors with the" +
        " same number of 'on' bits are equiprobable.")
public class BitFlipBSSVSOperator extends Operator {

    public Input<BooleanParameter> indicator = new Input<BooleanParameter>("indicator", "the parameter to operate a flip on.", Validate.REQUIRED);
    public Input<RealParameter> rateParameter = new Input<RealParameter>("mu", "the rate parameter in the substitution model " +
            "(mutation rate).", Validate.REQUIRED);
    public Input<Double> m_pScaleFactor = new Input<Double>("scaleFactor", "scaling factor: larger means more bold proposals", 1.0);


    private double scaleFactor;

    public void initAndValidate() {

        scaleFactor = m_pScaleFactor.get();
    }

    /**
     * Change the parameter and return the hastings ratio.
     * Flip (Switch a 0 to 1 or 1 to 0) for a random bit in a bit vector.
     * Return the hastings ratio which makes all subsets of vectors with the same number of 1 bits
     * equiprobable, unless usesPriorOnSum = false then all configurations are equiprobable
     */

    @Override
    public double proposal() {

        final BooleanParameter p = indicator.get();

        final int dim = p.getDimension();

        double sum = 0.0;
        for(int i = 0; i < dim; i++) {
            if( p.getValue(i) ) sum += 1;
        }

        final int pos = Randomizer.nextInt(dim);

        final boolean value = p.getValue(pos);

        double rand = 0;
        if (rateParameter != null)
            rand = Randomizer.nextDouble();

        double logq = 0.0;
        if ( ! value ) {
            p.setValue(pos, true);

            logq = -Math.log((dim - sum) / (sum + 1));
//	               rand = 0.5 - rand;
        } else {
            // assert value;

            p.setValue(pos, false);
            logq = -Math.log(sum / (dim - sum + 1));
//	              rand = 0.5 + rand;
            rand *= -1;
        }

        RealParameter rates = rateParameter.get();
        if (rates != null) {
            final double scale = Math.exp((rand) * scaleFactor);
            logq += Math.log(scale);

            final double oldValue = rates.getValue(0);
            final double newValue = scale * oldValue;

            if (outsideBounds(newValue, rates))
                return Double.NEGATIVE_INFINITY;

            rates.setValue(0, newValue);
        }

        return logq;
    }

    private boolean outsideBounds(double value, RealParameter param) {
        final Double l = param.getLower();
        final Double h = param.getUpper();

        return ( value < l || value > h );
        //return (l != null && value < l || h != null && value > h);
    }

}
