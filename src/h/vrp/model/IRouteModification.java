package h.vrp.model;

/**
 * Represents a modification to a route
 * @author hinton
 */
public interface IRouteModification {
	/**
	 * For accessing the unmodified route
	 * @return the unmodified route to which this is a proposed change
	 */
	public Route unmodifiedRoute();
	
	/**
	 * Allows iteration through the route after a change
	 * @return a vertex iterator which goes through the new route
	 */
//	public IVertexIterator sequence();
	
	/**
	 * An iterator over the vertices which would be added
	 * @return an iterator over new vertices
	 */
	public IVertexIterator addedVertices();
	/**
	 * Like {@link addedVertices} but with the obvious change
	 * @return
	 */
	public IVertexIterator removedVertices();
	
	/**
	 * An iterator over the edges which would be added
	 * @return
	 */
	public IEdgeIterator addedEdges();
	/**
	 * An iterator over the edges which would be removed
	 * @return
	 */
	public IEdgeIterator removedEdges();
	
	
	/**
	 * An iterator over edges which would be entirely deleted from the solution,
	 * rather than just moved to another route
	 * @return
	 */
	public IEdgeIterator deletedEdges();
	
	public interface IVertexIterator {
		public boolean hasNext();
		public int next();
	}
	public interface IEdgeIterator {
		public boolean hasNext();
		public void next();
		public int from();
		public int to();
	}
	
	public static IVertexIterator emptyVertexIterator = new IVertexIterator(){
		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public int next() {
			return 0;
		}};
		
	public static IEdgeIterator emptyEdgeIterator = new IEdgeIterator() {
		@Override
		public int from() {
			return 0;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public void next() {
			
		}

		@Override
		public int to() {
			return 0;
		}};

		/**
		 * Iterator over edges which are entirely new
		 * @return
		 */
	public IEdgeIterator createdEdges();

	
}
