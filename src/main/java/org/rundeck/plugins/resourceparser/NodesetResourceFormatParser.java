package org.rundeck.plugins.resourceparser;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserException;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.*;

/**
 * NodesetResourceFormatParser is ...
 *
 * @author Greg Schueler <greg@simplifyops.com>
 * @since 2014-05-14
 */
@Plugin(name = NodesetResourceFormatParser.NODESETXML, service = ServiceNameConstants.ResourceFormatParser)
@PluginDescription(title = "Nodeset XML Parser", description = "Parses nodes defined in the nodesetxml format")
public class NodesetResourceFormatParser implements ResourceFormatParser {

    public static final String NODESETXML = "nodesetxml";

    public Set<String> getFileExtensions() {
        HashSet<String> strings = new HashSet<String>();
        strings.add(".nodesetxml");
        return strings;
    }

    public Set<String> getMIMETypes() {
        HashSet<String> strings = new HashSet<String>();
        strings.add("application/rundeck-nodeset,v1+xml");
        return strings;
    }

    public INodeSet parseDocument(File file) throws ResourceFormatParserException {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new ResourceFormatParserException(e);
        }
        try {
            return parseDocument(stream);
        } finally {
            closeQuietly(stream);
        }
    }

    private void closeQuietly(Closeable stream) {
        try {
            stream.close();
        } catch (IOException e) {

        }
    }

    /**
     * load nodes from a document
     *
     * @param doc
     *
     * @return
     */
    private INodeSet loadNodes(Document doc) throws ResourceFormatParserException {

        NodeSetImpl iNodeEntries = new NodeSetImpl();
        List<NodeItem> allnodes = new ArrayList<NodeItem>();
        Element rootElement = doc.getRootElement();
        if (!rootElement.getName().equals("nodeset")) {
            throw new ResourceFormatParserException("nodesetxml format error: root element should be 'nodeset'");
        }

        NodeSet rootSet = parseNodeSetElement(rootElement, allnodes);
        parseSubNodeSets(rootElement, allnodes, rootSet);
        fillNodes(allnodes, iNodeEntries);
        return iNodeEntries;
    }

    /**
     * Fill defined nodes into the NodeSetImpl
     *
     * @param iNodeEntries
     */
    private void fillNodes(List<NodeItem> allnodes, NodeSetImpl iNodeEntries) {
        for (NodeItem allnode : allnodes) {
            String name = allnode.attributes.get("name");
            NodeEntryImpl nodeEntry = new NodeEntryImpl(name);

            //find root then descend tree to set attributes
            ArrayList<NodeItem> parents = new ArrayList<NodeItem>();
            NodeItem parent = allnode.parent;
            while (parent != null) {
                parents.add(0, parent);//unshift
                parent = parent.parent;
            }
            //iterate from top down
            for (NodeItem parentItem : parents) {
                updateNodeEntry(parentItem, nodeEntry);
            }
            updateNodeEntry(allnode, nodeEntry);

            Set tags = nodeEntry.getTags();
            nodeEntry.setTags(tags);
            iNodeEntries.putNode(nodeEntry);
        }
    }

    private void updateNodeEntry(NodeItem item, NodeEntryImpl nodeEntry) {
        nodeEntry.getAttributes().putAll(item.attributes);
        nodeEntry.getTags().addAll(item.tags);
    }

    /**
     * evaluate nodeset elements
     *
     * @param element
     *
     * @return
     */
    private List<NodeSet> parseNodeSets(Element element, List<NodeItem> allnodes) {
        ArrayList<NodeSet> result = new ArrayList<NodeSet>();
        List nodeset = element.selectNodes("nodeset");
        for (Object o : nodeset) {
            Element nodesetelem = (Element) o;
            NodeSet nodeSet = parseNodeSetElement(nodesetelem, allnodes);
            result.add(nodeSet);
        }
        return result;
    }

    /**
     * Parse nodeset element
     *
     * @param nodesetelem
     *
     * @return
     */
    private NodeSet parseNodeSetElement(Element nodesetelem, List<NodeItem> allnodes) {
        NodeSet nodeSet = new NodeSet();
        parseElementNodeAttributes(nodesetelem, nodeSet);

        //now process nodes
        List nodelist = nodesetelem.selectNodes("node");
        for (Object o : nodelist) {
            Element nodelem = (Element) o;
            NodeItem nodeItem = new NodeItem();
            parseElementNodeAttributes(nodelem, nodeItem);
            nodeItem.parent = nodeSet;
            nodeSet.nodes.add(nodeItem);
            allnodes.add(nodeItem);
        }

        //now process sub nodesets
        parseSubNodeSets(nodesetelem, allnodes, nodeSet);
        return nodeSet;
    }

    private void parseSubNodeSets(Element nodesetelem, List<NodeItem> allnodes, NodeSet nodeSet) {
        List<NodeSet> nodeSets = parseNodeSets(nodesetelem, allnodes);
        for (NodeSet set : nodeSets) {
            set.parent = nodeSet;
            nodeSet.subsets.add(set);
        }
    }

    /**
     * Parse xml attributes and embedded attributes/attribute elements for an xml element, and set as rd node attributes
     * on the NodeItem
     *
     * @param nodesetelem
     * @param nodeItem
     */
    private void parseElementNodeAttributes(Element nodesetelem, NodeItem nodeItem) {
        //element's attributes
        for (Object o : nodesetelem.attributes()) {
            Attribute attr = (Attribute) o;
            if (!"tags".equals(attr.getName())) {
                nodeItem.attributes.put(attr.getName(), attr.getValue());
            } else {
                nodeItem.tags.addAll(tagString(attr.getValue()));
            }
        }
        //now parse <attributes><attribute/> elements
        List list = nodesetelem.selectNodes("attributes/attribute");
        for (Object o : list) {
            Element attr = (Element) o;
            Attribute name = attr.attribute("name");
            Attribute value = attr.attribute("value");
            if (null != name && null != value) {
                if (!"tags".equals(name.getValue())) {
                    nodeItem.attributes.put(name.getValue(), value.getValue());
                } else {
                    nodeItem.tags.addAll(tagString(value.getValue()));
                }
            }
        }
    }

    private Set<String> tagString(String tagstring) {
        String[] split = tagstring.split(",\\s*");
        return new HashSet<String>(Arrays.asList(split));
    }

    static class NodeItem {
        NodeItem parent;
        Map<String, String> attributes;
        Set<String> tags;

        NodeItem() {
            attributes = new HashMap<String, String>();
            tags = new HashSet<String>();
        }
    }

    static class NodeSet extends NodeItem {
        List<NodeItem> nodes;
        List<NodeSet> subsets;

        NodeSet() {
            nodes = new ArrayList<NodeItem>();
            subsets = new ArrayList<NodeSet>();
        }
    }

    public INodeSet parseDocument(InputStream inputStream) throws ResourceFormatParserException {
        final SAXReader reader = new SAXReader(false);
        final Document doc;
        try {
            doc = reader.read(inputStream);
        } catch (DocumentException e) {
            throw new ResourceFormatParserException(e);
        }
        return loadNodes(doc);
    }
}
