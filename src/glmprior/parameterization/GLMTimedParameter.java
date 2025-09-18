/*
 * Copyright (C) 2019-2024 Tim Vaughan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package glmprior.parameterization;

import bdmmprime.parameterization.TimedParameter;
import beast.base.core.Input;
import glmprior.util.GLMPrior;

import java.io.PrintStream;


/**
 * Parameter in which the value of each element corresponds to a particular time.
 */
public class GLMTimedParameter extends TimedParameter {

    public final Input<Boolean> isGLMInput = new Input<>("isGLM",
            "Should GLM prior be applied", false);

    boolean isGLM;

    @Override
    public void initAndValidate() {
        super.initAndValidate();

        isGLM = isGLMInput.get();
    }

    public void init(PrintStream out) {
        super.init(out);
        if (isGLM && valuesInput.get() instanceof GLMPrior) {
                ((GLMPrior) valuesInput.get()).init(out);
        }
    }

    @Override
    public void log(long sample, PrintStream out) {
        super.log(sample, out);
        if (isGLM && valuesInput.get() instanceof GLMPrior) {
                ((GLMPrior) valuesInput.get()).log(sample, out);
        }
    }

    @Override
    public void close(PrintStream out) {
        super.close(out);
        if (isGLM && valuesInput.get() instanceof GLMPrior) {
                ((GLMPrior) valuesInput.get()).close(out);
        }
    }


}
