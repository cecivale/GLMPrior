package glmprior.parameterization;

import bdmmprime.parameterization.Parameterization;
import bdmmprime.parameterization.SkylineMatrixParameter;
import bdmmprime.parameterization.TimedParameter;
import beast.base.core.Input;

public class GLMCanonicalParameterization extends Parameterization {

    final static double[] EMPTY_TIME_ARRAY = new double[0];
    public final Input<GLMSkylineVectorParameter> birthRateInput = new Input<>("birthRate",
            "Birth rate skyline.", Input.Validate.REQUIRED);
    public final Input<GLMSkylineVectorParameter> deathRateInput = new Input<>("deathRate",
            "Death rate skyline.", Input.Validate.REQUIRED);
    public final Input<GLMSkylineVectorParameter> samplingRateInput = new Input<>("samplingRate",
            "Sampling rate skyline.", Input.Validate.REQUIRED);
    public final Input<TimedParameter> rhoSamplingInput = new Input<>("rhoSampling",
            "Contemporaneous sampling times and probabilities.");
    public final Input<GLMSkylineVectorParameter> removalProbInput = new Input<>("removalProb",
            "Removal prob skyline.", Input.Validate.REQUIRED);
    public final Input<SkylineMatrixParameter> migRateInput = new Input<>("migrationRate",
            "Migration rate skyline.");
    public final Input<SkylineMatrixParameter> crossBirthRateInput = new Input<>("birthRateAmongDemes",
            "Birth rate among demes skyline.");
    double[] ZERO_VALUE_ARRAY;
    double[][] ZERO_VALUE_MATRIX;

    public void initAndValidate() {

        int nTypes = birthRateInput.get().getNTypes();
        ZERO_VALUE_ARRAY = new double[nTypes];
        ZERO_VALUE_MATRIX = new double[nTypes][nTypes];

        super.initAndValidate();
    }

    @Override
    public double[] getMigRateChangeTimes() {
        if (migRateInput.get() == null)
            return EMPTY_TIME_ARRAY;

        return migRateInput.get().getChangeTimes();
    }

    @Override
    public double[] getBirthRateChangeTimes() {
        return birthRateInput.get().getChangeTimes();
    }

    @Override
    public double[] getCrossBirthRateChangeTimes() {
        if (crossBirthRateInput.get() == null)
            return EMPTY_TIME_ARRAY;

        return crossBirthRateInput.get().getChangeTimes();
    }

    @Override
    public double[] getDeathRateChangeTimes() {
        return deathRateInput.get().getChangeTimes();
    }

    @Override
    public double[] getSamplingRateChangeTimes() {
        return samplingRateInput.get().getChangeTimes();
    }

    @Override
    public double[] getRemovalProbChangeTimes() {
        return removalProbInput.get().getChangeTimes();
    }

    @Override
    public double[] getRhoSamplingTimes() {
        if (rhoSamplingInput.get() != null)
            return rhoSamplingInput.get().getTimes();
        else
            return EMPTY_TIME_ARRAY;
    }

    @Override
    protected double[][] getMigRateValues(double time) {
        if (migRateInput.get() == null)
            return ZERO_VALUE_MATRIX;

        return migRateInput.get().getValuesAtTime(time);
    }

    @Override
    protected double[] getBirthRateValues(double time) {
        return birthRateInput.get().getValuesAtTime(time);
    }

    @Override
    protected double[][] getCrossBirthRateValues(double time) {
        if (crossBirthRateInput.get() == null)
            return ZERO_VALUE_MATRIX;

        return crossBirthRateInput.get().getValuesAtTime(time);
    }

    @Override
    protected double[] getDeathRateValues(double time) {
        return deathRateInput.get().getValuesAtTime(time);
    }

    @Override
    protected double[] getSamplingRateValues(double time) {
        return samplingRateInput.get().getValuesAtTime(time);
    }

    @Override
    protected double[] getRemovalProbValues(double time) {
        return removalProbInput.get().getValuesAtTime(time);
    }

    @Override
    protected double[] getRhoValues(double time) {
        if (rhoSamplingInput.get() != null)
            return rhoSamplingInput.get().getValuesAtTime(time);
        else
            return ZERO_VALUE_ARRAY;
    }

    @Override
    protected void validateParameterTypeCounts() {
        if (birthRateInput.get().getNTypes() != getNTypes())
            throw new IllegalArgumentException("Birth rate skyline type count does not match type count of model.");

        if (deathRateInput.get().getNTypes() != getNTypes())
            throw new IllegalArgumentException("Death rate skyline type count does not match type count of model.");

        if (samplingRateInput.get().getNTypes() != getNTypes())
            throw new IllegalArgumentException("Sampling rate skyline type count does not match type count of model.");

        if (migRateInput.get() != null
                && migRateInput.get().getNTypes() != getNTypes())
            throw new IllegalArgumentException("Migration rate skyline type count does not match type count of model.");

        if (crossBirthRateInput.get() != null
                && crossBirthRateInput.get().getNTypes() != getNTypes())
            throw new IllegalArgumentException("Birth rate among demes skyline type count does not match type count of model.");

        if (removalProbInput.get().getNTypes() != getNTypes())
            throw new IllegalArgumentException("Removal prob skyline type count does not match type count of model.");

        if (rhoSamplingInput.get() != null
                && rhoSamplingInput.get().getNTypes() != getNTypes())
            throw new IllegalArgumentException("Rho sampling type count does not match type count of model.");
    }
}
