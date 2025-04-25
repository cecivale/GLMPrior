package glmprior.operator;

import java.util.HashSet;
import java.util.Set;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Operator;
import beast.base.core.Input.Validate;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.util.Randomizer;


/**
 * @author dkuh004
 *         Date: Oct 26, 2011
 *         Time: 2:52:09 PM
 */

@Description("Propose a new set of indicators independent of the previous state")

public class PickIndicatorOperator extends Operator {

    public Input<BooleanParameter> parameter = new Input<BooleanParameter>("parameter", "the parameter to operate a flip on.", Validate.REQUIRED);

    public Input<Integer> minNonZeros = new Input<Integer>("minNonZeros", " minimum number of non zeros (default = 0)", 0);
    public Input<Integer> maxNonZeros = new Input<Integer>("maxNonZeros", " maximum number of non zeros (default = parameter length)");

    public int min;
    public int max;
    public BooleanParameter indicator;
    public int dim;

    @Override
    public void initAndValidate() {

        dim = parameter.get().getDimension();
        min = minNonZeros.get();

        if( maxNonZeros.get() != null ) {
            max = maxNonZeros.get();
        }
        else{
            max = dim;
        }
    }


    @Override
    public double proposal() {

        indicator = parameter.get();
        Set<Integer> s = new HashSet<Integer>();


        int nr_nonzeros = min + Randomizer.nextInt(max-min);  // pick how many nonzeros

        for (int i=0; i <dim; i++)
        {
            indicator.setValue(i, false);
        }


        while (s.size() < nr_nonzeros)
        {
            s.add(Randomizer.nextInt(dim)); // pick the nonzeros randomly
        }

        for(Integer i : s)
        {
            indicator.setValue(i, true);
        }

        return 0;
    }


}
