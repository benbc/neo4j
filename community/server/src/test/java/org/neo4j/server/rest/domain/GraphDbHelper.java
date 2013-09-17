/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.rest.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.schema.ConstraintCreator;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.server.database.Database;

import static org.neo4j.graphdb.DynamicLabel.label;
import static org.neo4j.helpers.collection.Iterables.count;
import static org.neo4j.helpers.collection.Iterables.single;

public class GraphDbHelper
{
    private final Database database;

    public GraphDbHelper( Database database )
    {
        this.database = database;
    }

    public int getNumberOfNodes()
    {
        return numberOfEntitiesFor( Node.class );
    }

    public int getNumberOfRelationships()
    {
        return numberOfEntitiesFor( Relationship.class );
    }

    @SuppressWarnings("deprecation")
    private int numberOfEntitiesFor( Class<? extends PropertyContainer> type )
    {
        return (int) database.getGraph().getNodeManager().getNumberOfIdsInUse( type );
    }

    public Map<String, Object> getNodeProperties( long nodeId )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            Node node = database.getGraph().getNodeById( nodeId );
            Map<String, Object> allProperties = new HashMap<>();
            for ( String propertyKey : node.getPropertyKeys() )
            {
                allProperties.put( propertyKey, node.getProperty( propertyKey ) );
            }
            tx.success();
            return allProperties;
        }
    }

    public void setNodeProperties( long nodeId, Map<String, Object> properties )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            Node node = database.getGraph().getNodeById( nodeId );
            for ( Map.Entry<String, Object> propertyEntry : properties.entrySet() )
            {
                node.setProperty( propertyEntry.getKey(), propertyEntry.getValue() );
            }
            tx.success();
        }
    }

    public long createNode( Label... labels )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            Node node = database.getGraph().createNode( labels );
            tx.success();
            return node.getId();
        }
    }

    public long createNode( Map<String, Object> properties, Label... labels )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            Node node = database.getGraph().createNode( labels );
            for ( Map.Entry<String, Object> entry : properties.entrySet() )
            {
                node.setProperty( entry.getKey(), entry.getValue() );
            }
            tx.success();
            return node.getId();
        }
    }

    public void deleteNode( long id )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            Node node = database.getGraph().getNodeById( id );
            node.delete();
            tx.success();
        }
    }

    public long createRelationship( String type, long startNodeId, long endNodeId )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            Node startNode = database.getGraph().getNodeById( startNodeId );
            Node endNode = database.getGraph().getNodeById( endNodeId );
            Relationship relationship = startNode.createRelationshipTo( endNode,
                    DynamicRelationshipType.withName( type ) );
            tx.success();
            return relationship.getId();
        }
    }

    public long createRelationship( String type )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            Node startNode = database.getGraph().createNode();
            Node endNode = database.getGraph().createNode();
            Relationship relationship = startNode.createRelationshipTo( endNode,
                    DynamicRelationshipType.withName( type ) );
            tx.success();
            return relationship.getId();
        }
    }

    public void setRelationshipProperties( long relationshipId, Map<String, Object> properties )

    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            Relationship relationship = database.getGraph().getRelationshipById( relationshipId );
            for ( Map.Entry<String, Object> propertyEntry : properties.entrySet() )
            {
                relationship.setProperty( propertyEntry.getKey(), propertyEntry.getValue() );
            }
            tx.success();
        }
    }

    public Map<String, Object> getRelationshipProperties( long relationshipId )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            Relationship relationship = database.getGraph().getRelationshipById( relationshipId );
            Map<String, Object> allProperties = new HashMap<>();
            for ( String propertyKey : relationship.getPropertyKeys() )
            {
                allProperties.put( propertyKey, relationship.getProperty( propertyKey ) );
            }
            tx.success();
            return allProperties;
        }
    }

    public Relationship getRelationship( long relationshipId )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            Relationship relationship = database.getGraph().getRelationshipById( relationshipId );
            tx.success();
            return relationship;
        }
    }

    public void addNodeToIndex( String indexName, String key, Object value, long id )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            database.getNodeIndex( indexName ).add( database.getGraph().getNodeById( id ), key, value );
            tx.success();
        }
    }

    public Collection<Long> queryIndexedNodes( String indexName, String key, Object value )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            Collection<Long> result = new ArrayList<>();
            for ( Node node : database.getNodeIndex( indexName ).query( key, value ) )
            {
                result.add( node.getId() );
            }
            tx.success();
            return result;
        }
    }

    public Collection<Long> getIndexedNodes( String indexName, String key, Object value )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            Collection<Long> result = new ArrayList<>();
            for ( Node node : database.getNodeIndex( indexName ).get( key, value ) )
            {
                result.add( node.getId() );
            }
            tx.success();
            return result;
        }
    }

    public Collection<Long> getIndexedRelationships( String indexName, String key, Object value )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            Collection<Long> result = new ArrayList<>();
            for ( Relationship relationship : database.getRelationshipIndex( indexName ).get( key, value ) )
            {
                result.add( relationship.getId() );
            }
            tx.success();
            return result;
        }
    }

    public void addRelationshipToIndex( String indexName, String key, String value, long relationshipId )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            Index<Relationship> index = database.getRelationshipIndex( indexName );
            index.add( database.getGraph().getRelationshipById( relationshipId ), key, value );
            tx.success();
        }
    }

    public String[] getNodeIndexes()
    {
        try ( Transaction ignored = database.getGraph().beginTx() )
        {
            return database.getIndexManager()
                    .nodeIndexNames();
        }
    }

    public Index<Node> createNodeFullTextIndex( String named )
    {
        try ( Transaction transaction = database.getGraph().beginTx() )
        {
            Index<Node> index = database.getIndexManager()
                    .forNodes( named, MapUtil.stringMap( IndexManager.PROVIDER, "lucene", "type", "fulltext" ) );
            transaction.success();
            return index;
        }
    }

    public Index<Node> createNodeIndex( String named )
    {
        try ( Transaction transaction = database.getGraph().beginTx() )
        {
            Index<Node> nodeIndex = database.getIndexManager()
                    .forNodes( named );
            transaction.success();
            return nodeIndex;
        }
    }

    public String[] getRelationshipIndexes()
    {
        try ( Transaction ignored = database.getGraph().beginTx() )
        {
            return database.getIndexManager()
                    .relationshipIndexNames();
        }
    }

    public long getReferenceNode()
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            @SuppressWarnings("deprecation")
            Node referenceNode = database.getGraph().getReferenceNode();

            tx.success();
            return referenceNode.getId();
        }
    }

    public Index<Relationship> createRelationshipIndex( String named )
    {
        try ( Transaction transaction = database.getGraph().beginTx() )
        {
            RelationshipIndex relationshipIndex = database.getIndexManager()
                    .forRelationships( named );
            transaction.success();
            return relationshipIndex;
        }
    }

    public Iterable<String> getNodeLabels( long node )
    {
        return new IterableWrapper<String, Label>( database.getGraph().getNodeById( node ).getLabels() )
        {
            @Override
            protected String underlyingObjectToObject( Label object )
            {
                return object.name();
            }
        };
    }

    public void addLabelToNode( long node, String labelName )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            database.getGraph().getNodeById( node ).addLabel( label( labelName ) );
            tx.success();
        }
    }

    public Iterable<IndexDefinition> getSchemaIndexes( String labelName )
    {
        return database.getGraph().schema().getIndexes( label( labelName ) );
    }

    public IndexDefinition createSchemaIndex( String labelName, String propertyKey )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            IndexDefinition index = database.getGraph().schema().indexFor( label( labelName ) ).on( propertyKey ).create();
            tx.success();
            return index;
        }
    }

    public Iterable<ConstraintDefinition> getPropertyUniquenessConstraints( String labelName, final String propertyKey )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            Iterable<ConstraintDefinition> definitions = Iterables.filter( new Predicate<ConstraintDefinition>()
            {

                @Override
                public boolean accept( ConstraintDefinition item )
                {
                    if ( item.isConstraintType( ConstraintType.UNIQUENESS ) )
                    {
                        Iterable<String> keys = item.asUniquenessConstraint().getPropertyKeys();
                        return single( keys ).equals( propertyKey );
                    }
                    else
                        return false;

                }
            }, database.getGraph().schema().getConstraints( label( labelName ) ) );
            tx.success();
            return definitions;
        }
    }

    public ConstraintDefinition createPropertyUniquenessConstraint( String labelName, List<String> propertyKeys )
    {
        try ( Transaction tx = database.getGraph().beginTx() )
        {
            ConstraintCreator creator = database.getGraph().schema().constraintFor( label( labelName ) ).unique();
            for ( String propertyKey : propertyKeys )
                creator = creator.on( propertyKey );
            ConstraintDefinition result = creator.create();
            tx.success();
            return result;
        }
    }

    public long getLabelCount( long nodeId )
    {
        try ( Transaction ignored = database.getGraph().beginTx() )
        {
            return count( database.getGraph().getNodeById( nodeId ).getLabels() );
        }
    }
}
