package net.ion.radon.repository;


public class ReferenceNode {

	private Node refNode ;
	private ReferenceNode(Node refNode){
		this.refNode = refNode ;
	}
	
	static ReferenceNode create(Node refNode) {
		return new ReferenceNode(refNode);
	}

	public boolean hasSourceAradonId() {
		return refNode.hasProperty(ReferenceManager.SRC_ARADON);
	}
	
	public boolean hasTargetAradonId() {
		return refNode.hasProperty(ReferenceManager.TARGET_ARADON);
	}

	public AradonId getSourceAradonId() {
		InNode innode = (InNode) refNode.get(ReferenceManager.SRC_ARADON);
		return AradonId.load(innode.getDBObject());

	}

}
