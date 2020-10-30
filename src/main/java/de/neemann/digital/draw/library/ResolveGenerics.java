/*
 * Copyright (c) 2019 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.draw.library;

import de.neemann.digital.analyse.SubstituteLibrary;
import de.neemann.digital.core.NodeException;
import de.neemann.digital.core.element.Keys;
import de.neemann.digital.draw.elements.Circuit;
import de.neemann.digital.draw.elements.VisualElement;
import de.neemann.digital.hdl.hgs.*;
import de.neemann.digital.hdl.hgs.function.Function;
import de.neemann.digital.lang.Lang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves a generic circuit and makes it non generic
 */
public class ResolveGenerics {

    private final HashMap<String, Statement> map;

    /**
     * Creates a new instance
     */
    public ResolveGenerics() {
        map = new HashMap<>();
    }

    /**
     * Resolves the generics
     *
     * @param argsCode      the arguments code
     * @param circuit       the circuit to resolve
     * @param library       the library to use
     * @return the resolved circuit
     * @throws NodeException            NodeException
     * @throws ElementNotFoundException ElementNotFoundException
     */
    public CircuitHolder resolveCircuit(String argsCode, Circuit circuit, LibraryInterface library) throws NodeException, ElementNotFoundException {
        final Args args = createArgs(argsCode, circuit);
        return resolveCircuit(args, circuit, library);
    }

    /**
     * Resolves the generics
     *
     * @param visualElement the visual element
     * @param circuit       the circuit to resolve
     * @param library       the library to use
     * @return the resolved circuit
     * @throws NodeException            NodeException
     * @throws ElementNotFoundException ElementNotFoundException
     */
    public CircuitHolder resolveCircuit(VisualElement visualElement, Circuit circuit, LibraryInterface library) throws NodeException, ElementNotFoundException {
        final Args args = createArgs(visualElement, circuit);
        return resolveCircuit(args, circuit, library);
    }

    /**
     * Resolves the generics
     *
     * @param args          the generic arguments
     * @param circuit       the circuit to resolve
     * @param library       the library to use
     * @return the resolved circuit
     * @throws NodeException            NodeException
     * @throws ElementNotFoundException ElementNotFoundException
     */
    private CircuitHolder resolveCircuit(Args args, Circuit circuit, LibraryInterface library) throws NodeException, ElementNotFoundException {
        Circuit c = circuit.createDeepCopy();
        for (VisualElement ve : c.getElements()) {
            String gen = ve.getElementAttributes().get(Keys.GENERIC).trim();
            try {
                if (!gen.isEmpty()) {
                    boolean isCustom = library.getElementType(ve.getElementName(), ve.getElementAttributes()).isCustom();
                    Statement genS = getStatement(gen);
                    if (isCustom) {
                        Context mod = new Context()
                                .declareVar("args", args)
                                .declareFunc("setCircuit", new SetCircuitFunc(ve));
                        genS.execute(mod);
                        ve.setGenericArgs(mod);
                    } else {
                        Context mod = new Context()
                                .declareVar("args", args)
                                .declareVar("this", new SubstituteLibrary.AllowSetAttributes(ve.getElementAttributes()));
                        genS.execute(mod);
                    }
                }
            } catch (HGSEvalException | ParserException | IOException e) {
                final NodeException ex = new NodeException(Lang.get("err_evaluatingGenericsCode_N_N", ve, gen), e);
                ex.setOrigin(circuit.getOrigin());
                throw ex;
            }
        }
        return new CircuitHolder(c, args);
    }

    /**
     * Resolves the generics and return a circuit intended to be viewed
     *
     * @param argsCode      the arguments code
     * @param circuit       the circuit to resolve
     * @param library       the library to use
     * @return the resolved circuit
     * @throws NodeException            NodeException
     * @throws ElementNotFoundException ElementNotFoundException
     */
    public CircuitHolder resolveCircuitForViewing(String argsCode, Circuit circuit, LibraryInterface library) throws NodeException, ElementNotFoundException {
        ResolveGenerics.CircuitHolder circuitHolder = resolveCircuit(argsCode, circuit, library);

        Circuit modCircuit = circuitHolder.getCircuit();
        modCircuit.getAttributes().set(Keys.IS_GENERIC, false);

        // Change generic code on generic subcircuits.
        ArrayList<VisualElement> elements = modCircuit.getElements();
        for (VisualElement ve : elements) {
            if (ve.getGenericArgs() != null) {
                // Element is generic. Clear the code and export all context values instead.
                ve.getElementAttributes().set(Keys.GENERIC, getParameterizedGenericCode(
                        ve.getGenericArgs(), circuitHolder.getArgs().getContext()));
            }
        }

        return circuitHolder;
    }

    private Args createArgs(VisualElement visualElement, Circuit circuit) throws NodeException {
        Context context;
        if (visualElement != null) {
            context = visualElement.getGenericArgs();
            if (context == null) {
                String argsCode = visualElement.getElementAttributes().get(Keys.GENERIC);
                try {
                    Statement s = getStatement(argsCode);
                    context = new Context();
                    s.execute(context);
                } catch (HGSEvalException | ParserException | IOException e) {
                    final NodeException ex = new NodeException(Lang.get("err_evaluatingGenericsCode_N_N", visualElement, argsCode), e);
                    ex.setOrigin(circuit.getOrigin());
                    throw ex;
                }
            }
        } else
            context = new Context();

        return new Args(context);
    }

    private Args createArgs(String argsCode, Circuit circuit) throws NodeException {
        try {
            Statement s = getStatement(argsCode);
            Context context = new Context();
            s.execute(context);
            return new Args(context);
        } catch (HGSEvalException | ParserException | IOException e) {
            final NodeException ex = new NodeException(Lang.get("msg_errParsingGenerics"), e);
            ex.setOrigin(circuit.getOrigin());
            throw ex;
        }
    }

    private String getParameterizedGenericCode(Context context, Context args) {
        StringBuilder sb = new StringBuilder();

        // Get all exported values and all argument values.
        Map<String, Object> exported = new HashMap<>();
        for (Map.Entry<String, Object> entry : context.getMap().entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (!name.equals("args")) {
                exported.put(name, value);
            }
        }
        for (Map.Entry<String, Object> argsEntry : args.getMap().entrySet()) {
            if (!exported.containsKey(argsEntry.getKey())) {
                exported.put(argsEntry.getKey(), argsEntry.getValue());
            }
        }

        // Export all those values.
        for (Map.Entry<String, Object> entry : exported.entrySet()) {
            Object value = entry.getValue();
            String valueStr = null;
            if (value instanceof Long)
                valueStr = value.toString();
            else if (value instanceof Integer)
                valueStr = "int(" + value.toString() + ")";
            else if (value instanceof String)
                valueStr = '"' + getEscapedString((String) value) + '"';
            else if (value instanceof Boolean)
                valueStr = value.toString();
            else if (value instanceof Double)
                valueStr = "float(" + value.toString() + ")";
            if (valueStr != null) {
                sb.append("export ");
                sb.append(entry.getKey());
                sb.append(" := ");
                sb.append(valueStr);
                sb.append(";\n");
            }
        }

        return sb.toString();
    }

    /**
     * Correctly escape a string value.
     * @param s string to escape
     * @return the escaped string.
     */
    private static String getEscapedString(String s) {
        if (s == null)
            return null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            String val = "";
            switch (c) {
                case '\\':
                    val = "\\\\";
                    break;
                case '\n':
                    val = "\\n";
                    break;
                case '\r':
                    val = "\\r";
                    break;
                case '\t':
                    val = "\\t";
                    break;
                case '"':
                    val = "\\\"";
                    break;
                default:
                    val = String.valueOf(c);
            }
            sb.append(val);
        }
        return sb.toString();
    }

    private Statement getStatement(String code) throws IOException, ParserException {
        Statement genS = map.get(code);
        if (genS == null) {
            genS = new Parser(code).parse(false);
            map.put(code, genS);
        }
        return genS;
    }

    /**
     * Holds the args of a circuit.
     * Implements the access to the parents args values.
     */
    public static final class Args implements HGSMap {
        private final Context args;

        private Args(Context args) {
            this.args = args;
        }

        @Override
        public Object hgsMapGet(String key) throws HGSEvalException {
            Object v = args.hgsMapGet(key);
            if (v == null) {
                Object a = args.hgsMapGet("args");
                if (a instanceof HGSMap) {
                    return ((HGSMap) a).hgsMapGet(key);
                }
            }
            return v;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Args that = (Args) o;
            return args.equals(that.args);
        }

        Context getContext() {
            return args;
        }

        @Override
        public int hashCode() {
            return Objects.hash(args);
        }
    }

    /**
     * Holds the circuit and the args that created that circuit.
     */
    public static final class CircuitHolder {
        private final Circuit circuit;
        private final Args args;

        private CircuitHolder(Circuit circuit, Args args) {
            this.circuit = circuit;
            this.args = args;
        }

        /**
         * @return teturns the created circuit
         */
        public Circuit getCircuit() {
            return circuit;
        }

        /**
         * @return the args that created the circuit
         */
        public Args getArgs() {
            return args;
        }
    }

    private static final class SetCircuitFunc extends Function {
        private final VisualElement ve;

        private  SetCircuitFunc(VisualElement ve) {
            super(1);
            this.ve = ve;
        }

        @Override
        protected Object f(Object... args) {
            ve.setElementName(args[0].toString());
            return null;
        }

        // All setCircuit functions are considered identical.
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o != null && getClass() == o.getClass();
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }
}
