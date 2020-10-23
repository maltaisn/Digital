/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.core.arithmetic;

import java.util.concurrent.ThreadLocalRandom;

import de.neemann.digital.core.*;
import de.neemann.digital.core.element.Element;
import de.neemann.digital.core.element.ElementAttributes;
import de.neemann.digital.core.element.ElementTypeDescription;
import de.neemann.digital.core.element.Keys;
import de.neemann.digital.core.stats.Countable;

import static de.neemann.digital.core.element.PinInfo.input;

/**
 * The Random element
 */
public class Random extends Node implements Element, Countable {

    /**
     * The element description
     */
    public static final ElementTypeDescription DESCRIPTION =
            new ElementTypeDescription(Random.class, input("C").setClock())
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.BITS);

    private final int bits;
    private final ObservableValue output;
    private ObservableValue clockVal;
    private boolean lastClock;
    private long value;

    /**
     * Creates a new instance
     *
     * @param attributes the attributes
     */
    public Random(ElementAttributes attributes) {
        super(true);
        bits = attributes.getBits();
        output = new ObservableValue("out", bits).setPinDescription(DESCRIPTION);
        value = ThreadLocalRandom.current().nextLong();
    }

    @Override
    public void readInputs() throws NodeException {
        boolean clock = clockVal.getBool();
        if (clock && !lastClock)
            value = ThreadLocalRandom.current().nextLong();
        lastClock = clock;
    }

    @Override
    public void writeOutputs() throws NodeException {
        output.setValue(value);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        clockVal = inputs.get(0).addObserverToValue(this).checkBits(1, this, 0);
    }

    @Override
    public ObservableValues getOutputs() {
        return output.asList();
    }

    @Override
    public int getDataBits() {
        return bits;
    }

    /**
     * @return the clock value
     */
    public ObservableValue getClock() {
        return clockVal;
    }
}
