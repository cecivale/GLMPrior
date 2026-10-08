open module glmprior {
    requires beast.pkgmgmt;
    requires beast.base;
    requires java.xml;
    requires beast.fx;
    requires bdmmprime;
    requires org.apache.commons.statistics.distribution;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;

    exports glmprior.util;
    exports glmprior.operator;
    exports glmprior.parameterization;
    exports glmprior.beauti;

    provides beast.base.core.BEASTInterface with
        glmprior.util.GLMPrior,
        glmprior.util.GLMNormalDistribution,
        glmprior.util.GLMDistribution,
        glmprior.util.MultiGLMDistribution,
        glmprior.util.GLMLogger,
        glmprior.util.FunctionParameter,
        glmprior.util.SingleIndexBooleanParameter,
        glmprior.operator.ExtendedSwapOperator,
        glmprior.operator.RealRandomWalkOperator,
        glmprior.operator.BitFlipBSSVSOperator,
        glmprior.operator.PickIndicatorOperator,
        glmprior.operator.MultiSwapOperator,
        glmprior.operator.SingleBactrianRandomWalkOperator,
        glmprior.operator.GLMJointCoefficientParameterOperator,
        glmprior.operator.GLMJointParameterOperator,
        glmprior.parameterization.GLMSkylineVectorParameter,
        glmprior.parameterization.GLMSkylineMatrixParameter,
        glmprior.parameterization.GLMTimedParameter,
        glmprior.parameterization.GLMCanonicalParameterization,
        glmprior.parameterization.GLMEpiParameterization;

    provides beastfx.app.inputeditor.InputEditor with
        glmprior.beauti.GLMSkylineVectorInputEditor,
        glmprior.beauti.GLMSkylineMatrixInputEditor,
        glmprior.beauti.GLMTimedParameterInputEditor,
        glmprior.beauti.GLMNormalPriorDecoratorEditor;
}
