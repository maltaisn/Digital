/*
 * Copyright (c) 2020 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.gui.components.modification;

import de.neemann.digital.draw.elements.Circuit;
import de.neemann.digital.draw.elements.VisualElement;
import de.neemann.digital.lang.Lang;
import de.neemann.digital.undo.ModifyException;

/**
 * Modifies the name of a visual element.
 */
public class ModifyName extends ModificationOfVisualElement {

    private final String name;

    /**
     * Creates a new instance
     *
     * @param ve   the visual element to modify
     * @param name the new element name
     */
    public ModifyName(VisualElement ve, String name) {
        super(ve, Lang.get("mod_setName_N0_to_element_N1", name, getToolTipName(ve)));
        this.name = name;
    }

    @Override
    public void modify(Circuit circuit) throws ModifyException {
        VisualElement ve = getVisualElement(circuit);
        ve.setElementName(name);
    }
}
