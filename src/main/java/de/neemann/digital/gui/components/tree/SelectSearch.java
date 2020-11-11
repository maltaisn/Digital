/*
 * Copyright (c) 2020 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */

package de.neemann.digital.gui.components.tree;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.neemann.digital.lang.Lang;

/**
 * Search bar to filter components in tree.
 */
public class SelectSearch extends JTextField {

    private static final int SEARCH_DEBOUNCE_DELAY = 50;

    public SelectSearch(LibraryTreeModel model) {
        super();

        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        Timer timer = new Timer(SEARCH_DEBOUNCE_DELAY, actionEvent -> {
            model.filter(getText());
        });
        timer.setRepeats(false);
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filter();
            }

            private void filter() {
                timer.restart();
            }
        });

        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (getText().isEmpty() && !hasFocus()) {
            g.setColor(Color.GRAY);
            g.drawString(Lang.get("key_search"), 5, (getHeight() + getFont().getSize()) / 2);
        }
    }
}
