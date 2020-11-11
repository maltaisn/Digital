/*
 * Copyright (c) 2017 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.gui.components.tree;

import de.neemann.digital.draw.library.ElementLibrary;
import de.neemann.digital.draw.library.LibraryListener;
import de.neemann.digital.draw.library.LibraryNode;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * TreeModel based on a {@link ElementLibrary}
 */
public class LibraryTreeModel implements TreeModel, LibraryListener {
    private final LibraryNode base;
    private LibraryNode root;
    private final ArrayList<TreeModelListener> listeners = new ArrayList<>();
    private String query;
    private final HashSet<TreePath> expandedPaths;
    private final HashSet<TreePath> tempExpandedPaths;

    /**
     * Creates a new library tree model
     *
     * @param library the library
     */
    public LibraryTreeModel(ElementLibrary library) {
        base = library.getRoot();
        expandedPaths = new HashSet<>();
        tempExpandedPaths = new HashSet<>();
        filter("");
        library.addListener(this);
    }

    /**
     * Filter the tree model with a search query.
     * @param query search query. An empty string resets the tree to the original library.
     */
    public void filter(String query) {
        this.query = query;
        root = base.filter(query.trim().toLowerCase());

        final TreeModelEvent treeModelEvent;
        if (root == null) {
            treeModelEvent = new TreeModelEvent(this, (TreePath) null);
        } else {
            tempExpandedPaths.clear();
            HashSet<LibraryNode> expandedNodes = new HashSet<>();
            for (TreePath path : expandedPaths) {
                expandedNodes.add((LibraryNode) path.getLastPathComponent());
            }
            if (!isAnyPathExpanded(expandedNodes, root)) {
                // Temporarily expand until a leaf is reached if there are no expanded nodes.
                // These nodes are collapsed afterwards to prevent expanding the whole tree while searching.
                LibraryNode node = root;
                while (!node.isLeaf()) {
                    tempExpandedPaths.add(new TreePath(node.getPath()));
                    node = node.getChild(0);
                }
            }
            treeModelEvent = new TreeModelEvent(this, root.getPath());
        }

        for (TreeModelListener l : listeners)
            l.treeStructureChanged(treeModelEvent);
    }

    /**
     * Check if any path is expanded.
     *
     * @param expandedNodes All currently expanded nodes.
     * @param node Node to check in.
     * @return whether this node or any of its children is expanded.
     */
    private boolean isAnyPathExpanded(HashSet<LibraryNode> expandedNodes, LibraryNode node) {
        if (node.isLeaf())
            return false;
        if (expandedNodes.contains(node)) {
            return true;
        }
        for (LibraryNode child : node) {
            if (isAnyPathExpanded(expandedNodes, child)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public Object getChild(Object o, int i) {
        return ((LibraryNode) o).getChild(i);
    }

    @Override
    public int getChildCount(Object o) {
        return ((LibraryNode) o).size();
    }

    @Override
    public boolean isLeaf(Object o) {
        return ((LibraryNode) o).isLeaf();
    }

    @Override
    public void valueForPathChanged(TreePath treePath, Object o) {
    }

    @Override
    public int getIndexOfChild(Object o, Object o1) {
        return ((LibraryNode) o).indexOf((LibraryNode) o1);
    }

    @Override
    public void addTreeModelListener(TreeModelListener treeModelListener) {
        listeners.add(treeModelListener);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener treeModelListener) {
        listeners.remove(treeModelListener);
    }

    @Override
    public void libraryChanged(LibraryNode node) {
        // Library changed, refilter.
        filter(query);
    }

    public void setExpanded(TreePath path, boolean expanded) {
        if (!tempExpandedPaths.contains(path)) {
            if (expanded)
                expandedPaths.add(path);
            else
                expandedPaths.remove(path);
        }
    }

    public HashSet<TreePath> getExpandedPaths() {
        return expandedPaths;
    }

    public HashSet<TreePath> getTempExpandedPaths() {
        return tempExpandedPaths;
    }

    /**
     * Same as getRoot() but returns the typed root element
     *
     * @return the root LibraryNode
     */
    public LibraryNode getTypedRoot() {
        return root;
    }

}
