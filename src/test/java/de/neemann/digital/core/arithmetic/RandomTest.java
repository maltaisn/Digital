/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.core.arithmetic;

import junit.framework.TestCase;

import de.neemann.digital.core.Model;
import de.neemann.digital.core.ObservableValue;
import de.neemann.digital.core.element.ElementAttributes;

public class RandomTest extends TestCase {

    public void testRandom() throws Exception {
        // Test isn't fully deterministic, but chance of failing is basically nil.
        ObservableValue clk = new ObservableValue("clk", 1);

        Model model = new Model();
        Random out = model.add(new Random(new ElementAttributes().setBits(32)));
        out.setInputs(clk.asList());
        model.init(false);

        long lastVal = 0;
        for (int i = 0; i < 10; i++) {
            long val = out.getOutputs().get(0).getValue();
            assertTrue(val != lastVal);
            lastVal = val;

            clk.setBool(false);
            model.doStep();
            clk.setBool(true);
            model.doStep();
        }
    }

}
