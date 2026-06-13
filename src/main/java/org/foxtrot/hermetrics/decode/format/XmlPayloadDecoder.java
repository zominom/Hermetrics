package org.foxtrot.hermetrics.decode.format;

import org.foxtrot.hermetrics.decode.DecodeException;
import org.foxtrot.hermetrics.decode.PayloadDecoder;
import org.foxtrot.hermetrics.decode.RawMessage;

import org.foxtrot.hermetrics.canonical.value.CanonicalArray;
import org.foxtrot.hermetrics.canonical.value.CanonicalObject;
import org.foxtrot.hermetrics.canonical.value.CanonicalString;
import org.foxtrot.hermetrics.canonical.value.CanonicalValue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class XmlPayloadDecoder implements PayloadDecoder {

    @Override
    public String formatId() {
        return "xml";
    }

    @Override
    public CanonicalValue decode(RawMessage message) {
        try {
            Document document = hardenedParser().parse(new ByteArrayInputStream(message.value()));
            Element root = document.getDocumentElement();
            TreeMap<String, CanonicalValue> wrapper = new TreeMap<>();
            wrapper.put(root.getTagName(), convertElement(root));
            return new CanonicalObject(wrapper);
        } catch (Exception e) {
            throw new DecodeException("invalid XML on topic " + message.topic(), e);
        }
    }

    private static DocumentBuilder hardenedParser() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder();
    }

    private static CanonicalValue convertElement(Element element) {
        TreeMap<String, CanonicalValue> fields = attributeFields(element);
        Map<String, List<CanonicalValue>> children = new LinkedHashMap<>();
        String text = collectChildren(element, children);

        if (fields.isEmpty() && children.isEmpty()) {
            return new CanonicalString(text);
        }
        if (!text.isEmpty()) {
            fields.put("#text", new CanonicalString(text));
        }
        for (var entry : children.entrySet()) {
            List<CanonicalValue> values = entry.getValue();
            fields.put(entry.getKey(), values.size() == 1 ? values.get(0) : new CanonicalArray(values));
        }
        return new CanonicalObject(fields);
    }

    private static TreeMap<String, CanonicalValue> attributeFields(Element element) {
        TreeMap<String, CanonicalValue> fields = new TreeMap<>();
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            fields.put("@" + attribute.getNodeName(), new CanonicalString(attribute.getNodeValue()));
        }
        return fields;
    }

    private static String collectChildren(Element element, Map<String, List<CanonicalValue>> children) {
        StringBuilder text = new StringBuilder();
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            switch (node.getNodeType()) {
                case Node.ELEMENT_NODE -> children
                        .computeIfAbsent(((Element) node).getTagName(), tag -> new ArrayList<>())
                        .add(convertElement((Element) node));
                case Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> {
                    String piece = node.getNodeValue().trim();
                    if (!piece.isEmpty()) {
                        text.append(piece);
                    }
                }
                default -> {
                }
            }
        }
        return text.toString();
    }
}
