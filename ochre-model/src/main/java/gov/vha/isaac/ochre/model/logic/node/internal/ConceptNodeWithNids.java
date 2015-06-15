package gov.vha.isaac.ochre.model.logic.node.internal;

import gov.vha.isaac.ochre.api.DataTarget;
import gov.vha.isaac.ochre.model.logic.LogicExpressionOchreImpl;
import gov.vha.isaac.ochre.model.logic.Node;
import gov.vha.isaac.ochre.model.logic.NodeSemantic;
import gov.vha.isaac.ochre.model.logic.node.AbstractNode;
import gov.vha.isaac.ochre.model.logic.node.external.ConceptNodeWithUuids;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import gov.vha.isaac.ochre.util.UuidT5Generator;

/**
 * Created by kec on 12/10/14.
 */
public final class ConceptNodeWithNids extends AbstractNode {

    int conceptNid;

    public ConceptNodeWithNids(LogicExpressionOchreImpl logicGraphVersion, DataInputStream dataInputStream) throws IOException {
        super(logicGraphVersion, dataInputStream);
        conceptNid = dataInputStream.readInt();
    }

    public ConceptNodeWithNids(LogicExpressionOchreImpl logicGraphVersion, int conceptNid) {
        super(logicGraphVersion);
        this.conceptNid = conceptNid;

    }

    public ConceptNodeWithNids(ConceptNodeWithUuids externalForm) {
        super(externalForm);
        this.conceptNid = getIdentifierService().getNidForUuids(externalForm.getConceptUuid());

    }

    public int getConceptNid() {
        return conceptNid;
    }

    @Override
    public void writeNodeData(DataOutput dataOutput, DataTarget dataTarget) throws IOException {
        switch (dataTarget) {
            case EXTERNAL:
                ConceptNodeWithUuids externalForm = new ConceptNodeWithUuids(this);
                externalForm.writeNodeData(dataOutput, dataTarget);
                break;
            case INTERNAL:
                super.writeData(dataOutput, dataTarget);
                dataOutput.writeInt(conceptNid);
                break;
            default:
                throw new UnsupportedOperationException("Can't handle dataTarget: " + dataTarget);
        }
    }

    @Override
    public NodeSemantic getNodeSemantic() {
        return NodeSemantic.CONCEPT;
    }

    @Override
    protected UUID initNodeUuid() {
        try {
            return UuidT5Generator.get(getNodeSemantic().getSemanticUuid(),
                    getIdentifierService().getUuidPrimordialForNid(conceptNid).toString());
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public AbstractNode[] getChildren() {
        return new AbstractNode[0];
    }

    @Override
    public final void addChildren(Node... children) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "ConceptNode[" + getNodeIndex() + "]: \"" + getConceptService().getConcept(conceptNid).toUserString() + "\"" + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ConceptNodeWithNids that = (ConceptNodeWithNids) o;

        return conceptNid == that.conceptNid;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + conceptNid;
        return result;
    }

    @Override
    protected int compareFields(Node o) {
        return conceptNid - ((ConceptNodeWithNids) o).getConceptNid();
    }

}
